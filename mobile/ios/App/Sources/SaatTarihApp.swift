import SwiftUI
import Combine
import SaatCore

@main
struct SaatTarihApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

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
                }
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
    case alarm, timer, stopwatch, pomodoro, datecalc, settings, about, badges
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

    func open(_ item: HomeItem) {
        switch item {
        case .date, .week:
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
    @State private var router = Router.shared
    /// Klavye açıkken alt bar gizlenir (sistemin sekme çubuğu gibi; klavyenin üstüne binmez)
    @State private var keyboard = false
    /// Yan çevrilince (iPhone yatayda yüksekliği "compact") alt bar, durum çubuğu ve ana ekran çizgisi gizlenir:
    /// içerik sıkışmasın (30 Ekim 2026, sahibin isteği). Sekme değiştirmek için telefon dik tutulur.
    @Environment(\.verticalSizeClass) private var vsc
    private var landscape: Bool { vsc == .compact }

    var body: some View {
        // Bar sayfanın altında ayrı bir satır: sayfanın son satırları barın altında kalmaz
        // (safeAreaInset NavigationStack içindeki listelere geçmiyordu; yazılar barın altına giriyordu)
        VStack(spacing: 0) {
            content
                .id(router.tab == .tools ? "tools" : "\(router.tab.rawValue)-\(accent)")
                .frame(maxHeight: .infinity)
            if !keyboard && !landscape { TabBar(selection: $router.tab) }
        }
        .statusBarHidden(landscape)
        .persistentSystemOverlays(landscape ? .hidden : .automatic)
        .overlay(alignment: .top) { BadgeToast() }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in keyboard = true }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in keyboard = false }
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

/// Yedi düğmeli alt bar (simge + kısa ad; seçili olan uygulama renginde)
private struct TabBar: View {
    @Binding var selection: AppTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases, id: \.self) { tab in
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
         privacy, about, list, globe, google, badges, notifications
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
        case .globe:
            // App Store önizleme videosu için: tam ekran dönen dünya
            GlobeView(fullscreen: true)
                .ignoresSafeArea()
                .toolbar(.hidden, for: .navigationBar)
                .statusBarHidden()
        }
    }
}
