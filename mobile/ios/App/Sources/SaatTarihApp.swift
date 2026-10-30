import SwiftUI
import Combine
import SaatCore

@main
struct SaatTarihApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Özellik aç/kapa: ilk iş, önceki kullanıcıda hepsi açık kalsın (Features.migrate)
        Features.migrate()
        // Apple Watch'a seçili yer (WatchConnectivity)
        WatchSync.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .onChange(of: scenePhase) { _, phase in
            // Her açılışta bildirim listesi yeniden kurulur (iOS'ta en çok 64 bekleyen bildirim)
            // Otomatik konumdaysa konum sessizce yenilenir (değişirse bildirimler de yeniden kurulur)
            if phase == .active {
                BadgeStore.shared.appOpened()
                Task {
                    await PlaceModel.shared.refresh()
                    await PrayerNotifier.askOnce()
                    await PrayerNotifier.reschedule()
                    await AlarmCenter.refreshSkips()
                }
                // Aralık antrenmanı sürüyorsa sesli duyurular (uygulama yeniden açılmış olabilir)
                HiitRunner.shared.sync()
                // Pomodoro arka plan sesi (süre uygulama kapalıyken bitmiş olabilir)
                PomoNoise.sync()
                // Kilit ekranı / Dynamic Island etkinlikleri (sıradaki vakit ileri taşınır, bitenler kapanır)
                LiveActivities.sync()
            } else if phase == .background {
                BackgroundRefresh.schedule()
            }
        }
        .backgroundTask(.appRefresh(BackgroundRefresh.id)) {
            await BackgroundRefresh.run()
        }
    }
}

/// Sekmeler (sahibin sırası, 18 Ekim 2026): Saat, Hava, Takvim, Dünya saatleri, Mevsimler, Vakitler, Araçlar.
/// iOS'un TabView'i beşten fazlasını "Diğer" altına koyduğu için alt bar uygulamanın kendisidir.
enum AppTab: Int, CaseIterable {
    case clock, weather, calendar, world, seasons, prayer, tools

    var title: String {
        switch self {
        case .clock: L("tab.clock")
        case .weather: L("tab.weather")
        case .calendar: L("tab.calendar")
        case .world: L("tab.world")
        case .seasons: L("tab.seasons")
        case .prayer: L("tab.prayer")
        case .tools: L("tab.tools")
        }
    }

    /// Kayıtlardaki ad (`features` anahtarı; Android'deki Tab.key'in "tab." sonrası)
    var key: String {
        switch self {
        case .clock: "clock"
        case .weather: "weather"
        case .calendar: "calendar"
        case .world: "world"
        case .seasons: "seasons"
        case .prayer: "prayer"
        case .tools: "tools"
        }
    }

    var icon: String {
        switch self {
        case .clock: "clock"
        case .weather: "cloud.sun"
        case .calendar: "calendar"
        case .world: "globe.europe.africa"
        case .seasons: "leaf"
        case .prayer: "moon.stars"
        case .tools: "timer"
        }
    }
}

/// Araçlar sekmesindeki sayfalar
enum ToolRoute: Hashable {
    case alarm, timer, stopwatch, pomodoro, sleep, datecalc, settings, about, badges
}

/// Sekme ve Araçlar gezintisi; saat ekranındaki alanlar dokununca ilgili sayfayı açar.
@MainActor @Observable
final class Router {
    static let shared = Router()

    /// Ekran görüntüsü için: -startTab 2 ile o sekmede açılır
    var tab = AppTab(rawValue: UserDefaults.standard.integer(forKey: "startTab")) ?? .clock
    var toolsPath: [ToolRoute] = []
    /// Takvim sekmesi bu günü açar (saat ekranından)
    var calendarDay: CalendarDay?
    /// Saat ekranındaki dişliden açılan ayarlar (Araçlar kapalıyken de ulaşılsın)
    var sheet: SettingsSheet?
    /// Siri / Kısayollar "Tam ekran saati aç": saat sekmesi tam ekran açılır
    var wantsFullscreen = false

    func openFullscreen() {
        sheet = nil
        tab = .clock
        wantsFullscreen = true
    }

    func open(_ item: HomeItem) {
        switch item {
        case .date, .week, .hijri:
            calendarDay = CalendarDay(Date())
            tab = .calendar
        case .upcoming:
            let today = CalendarDay(Date(), timeZone: PrayerSchedule.zone)
            calendarDay = CalendarData.shared.upcoming(from: today, limit: 1).first?.day ?? today
            tab = .calendar
        case .next, .prayers:
            tab = .prayer
        case .weather:
            tab = .weather
        case .world, .globe:
            tab = .world
        case .sun:
            tab = .seasons
        }
    }
}

struct RootView: View {
    @AppStorage("appearance") private var appearance = "system"
    /// Uygulama rengi (Ayarlar); değişince sekmeler yeni renkle yeniden çizilir
    @AppStorage("accent") private var accent = AccentPalette.defaultID
    /// Açık özellikler: alt barda yalnız bunlar (ve geçici olarak şu anki sekme); yalnız Saat kalırsa bar yok
    @AppStorage(Features.key) private var features = ""
    @State private var router = Router.shared
    /// Klavye açıkken alt bar gizlenir (sistemin sekme çubuğu gibi; klavyenin üstüne binmez)
    @State private var keyboard = false
    /// Yan çevrilince (iPhone yatayda yüksekliği "compact") alt bar, durum çubuğu ve ana ekran çizgisi gizlenir:
    /// içerik sıkışmasın (30 Ekim 2026, sahibin isteği). Sekme değiştirmek için telefon dik tutulur.
    @Environment(\.verticalSizeClass) private var vsc
    @Environment(\.horizontalSizeClass) private var hsc
    private var landscape: Bool { vsc == .compact }
    /// iPad'de (geniş pencere) sayfalar okunur genişlikte ortalanır; kenarlar temanın zemin rengiyle dolar
    private var readable: CGFloat { UIDevice.current.userInterfaceIdiom == .pad && hsc == .regular ? 900 : .infinity }
    /// Alarm son iki saatte çaldıysa açılışta günaydın kartı (MorningCheck)
    @State private var morning = false
    /// Görevli alarm durduruldu: görev ekranı (çözülmeden kapanmaz; WakeTasks)
    @State private var wakeTask: PendingWakeTask?

    var body: some View {
        // Bar sayfanın altında ayrı bir satır: sayfanın son satırları barın altında kalmaz
        // (safeAreaInset NavigationStack içindeki listelere geçmiyordu; yazılar barın altına giriyordu)
        VStack(spacing: 0) {
            content
                .id(router.tab == .tools ? "tools" : "\(router.tab.rawValue)-\(accent)")
                .frame(maxWidth: readable, maxHeight: .infinity)
                .frame(maxWidth: .infinity)
                .background(Theme.bg.ignoresSafeArea())
            let tabs = Features.barTabs(raw: features, current: router.tab)
            if !keyboard && !landscape && tabs.count > 1 { TabBar(tabs: tabs, selection: $router.tab) }
        }
        .statusBarHidden(landscape)
        .persistentSystemOverlays(landscape ? .hidden : .automatic)
        .overlay(alignment: .top) { BadgeToast() }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in keyboard = true }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in keyboard = false }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            if WakeTasks.onActive() {
                wakeTask = WakeTasks.pending
            } else if MorningCheck.due() {
                morning = true
            }
        }
        .sheet(isPresented: $morning) { MorningCard().preferredColorScheme(scheme) }
        .fullScreenCover(item: $wakeTask) { t in
            WakeTaskView(task: t) {
                Task {
                    await WakeTasks.solved()
                    wakeTask = nil
                    if MorningCheck.due() { morning = true }
                }
            }
            .preferredColorScheme(scheme)
        }
        .tint(Theme.accent)
        .preferredColorScheme(scheme)
        .task {
            // CI ekran görüntüsü: -landscape ile uygulama yatay açılır
            guard ProcessInfo.processInfo.arguments.contains("-landscape"),
                  let scene = UIApplication.shared.connectedScenes.first(where: { $0 is UIWindowScene }) as? UIWindowScene
            else { return }
            scene.requestGeometryUpdate(.iOS(interfaceOrientations: .landscapeRight))
        }
        .fullScreenCover(item: $screen) { s in
            NavigationStack { s.view }
                .preferredColorScheme(scheme)
        }
        .sheet(item: $router.sheet) { s in
            NavigationStack {
                s.view
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { router.sheet = nil } }
                    }
            }
            .tint(Theme.accent)
            .preferredColorScheme(scheme)
        }
    }

    @ViewBuilder private var content: some View {
        switch router.tab {
        case .clock: ClockTab()
        case .weather: NavigationStack { WeatherView() }
        case .calendar: CalendarTab()
        case .world: NavigationStack { WorldClockView() }
        case .seasons: NavigationStack { SeasonsView() }
        case .prayer: PrayerTab()
        case .tools: ToolsTab()
        }
    }

    private var scheme: ColorScheme? { appearance == "light" ? .light : appearance == "dark" ? .dark : nil }

    /// Ekran görüntüsü için: -openScreen weather gibi bir argümanla alt ekran doğrudan açılır (CI)
    @State private var screen = UserDefaults.standard.string(forKey: "openScreen").flatMap(DebugScreen.init(rawValue:))
}

/// Alt bar: açık özelliklerin düğmeleri (simge + kısa ad; seçili olan uygulama renginde)
private struct TabBar: View {
    let tabs: [AppTab]
    @Binding var selection: AppTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tabs, id: \.self) { tab in
                let on = tab == selection
                Button {
                    // Seçili sekmeye yeniden dokununca Araçlar'da açık sayfadan listeye dönülür
                    if tab == selection, tab == .tools { Router.shared.toolsPath = [] }
                    selection = tab
                } label: {
                    VStack(spacing: 3) {
                        Image(systemName: tab.icon)
                            .symbolVariant(on ? .fill : .none)
                            .font(.system(size: 19))
                            .frame(height: 24)
                        Text(tab.title)
                            .font(.system(size: 10, weight: on ? .semibold : .regular))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 7)
                    .padding(.bottom, 3)
                    .contentShape(Rectangle())
                    .foregroundStyle(on ? Theme.accent : Theme.muted)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tab.title)
                .accessibilityAddTraits(on ? [.isSelected, .isButton] : .isButton)
            }
        }
        .padding(.horizontal, 2)
        .background(.bar)
        .overlay(alignment: .top) { Divider() }
    }
}

enum DebugScreen: String, Identifiable {
    case monthly, imsakiye, alarm, timer, stopwatch, pomodoro, datecalc, world, weather, seasons, settings, homeItems,
         privacy, about, list, globe, google, badges, notifications, morning, moon, look, features, qibla, mosques, night, sleep,
         wake
    var id: String { rawValue }

    @MainActor @ViewBuilder var view: some View {
        let place = AppPlace.stored()
        switch self {
        case .monthly: MonthlyPrayerView(place: place)
        case .imsakiye:
            if let r = CalendarData.shared.nextRamadan(from: CalendarDay(Date())) { ImsakiyeView(place: place, ramadan: r) }
        case .alarm: AlarmsView()
        case .timer: TimerView()
        case .stopwatch: StopwatchView()
        case .pomodoro: PomodoroView()
        case .sleep: SleepView()
        case .wake:
            // Görev ekranı örneği (CI ekran görüntüsü): orta zorlukta matematik
            WakeTaskView(task: PendingWakeTask(task: "math", level: 2, code: "", wakeCheck: true, title: L("sleep.alarmLabel"),
                                               rise: false, rings: 0, repeatID: nil)) {}
        case .datecalc: DateCalcView()
        case .world: WorldClockView()
        case .weather: WeatherView()
        case .seasons: SeasonsView()
        case .settings: SettingsView()
        case .homeItems: HomeItemsView()
        case .privacy: PrivacyView()
        case .about: AboutView()
        case .list: ListSignupView()
        case .google: GoogleCalendarGuide()
        case .badges: BadgesView()
        case .notifications: NotificationsView()
        case .morning: MorningCard()
        case .moon: MoonView()
        case .look: AppearanceView()
        case .features: FeaturesView()
        case .qibla: QiblaView()
        case .mosques: NearbyMosquesView(place: place)
        case .night: FullscreenClock() // -nightMode on ile gece modu
        case .globe:
            // App Store önizleme videosu için: tam ekran dönen dünya
            GlobeView(fullscreen: true)
                .ignoresSafeArea()
                .toolbar(.hidden, for: .navigationBar)
                .statusBarHidden()
        }
    }
}
