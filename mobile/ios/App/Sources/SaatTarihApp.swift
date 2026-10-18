import SwiftUI
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
                Task {
                    await PlaceModel.shared.refresh()
                    await PrayerNotifier.reschedule()
                }
            }
        }
    }
}

/// Sekmeler (sahibin sırası, 18 Ekim 2026): Saat, Hava durumu, Takvim, Vakitler, Araçlar
enum AppTab: Int {
    case clock, weather, calendar, prayer, tools
}

/// Araçlar sekmesindeki sayfalar
enum ToolRoute: Hashable {
    case alarm, timer, stopwatch, pomodoro, datecalc, world, seasons, settings, privacy, about
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
        case .world, .sun:
            toolsPath = [item == .world ? .world : .seasons]
            tab = .tools
        }
    }
}

struct RootView: View {
    @AppStorage("appearance") private var appearance = "system"
    @State private var router = Router.shared

    var body: some View {
        TabView(selection: $router.tab) {
            ClockTab()
                .tabItem { Label(L("tab.clock"), systemImage: "clock") }
                .tag(AppTab.clock)
            NavigationStack { WeatherView() }
                .tabItem { Label(L("weather.h1"), systemImage: "cloud.sun") }
                .tag(AppTab.weather)
            CalendarTab()
                .tabItem { Label(L("tab.calendar"), systemImage: "calendar") }
                .tag(AppTab.calendar)
            PrayerTab()
                .tabItem { Label(L("tab.prayer"), systemImage: "moon.stars") }
                .tag(AppTab.prayer)
            ToolsTab()
                .tabItem { Label(L("tab.tools"), systemImage: "timer") }
                .tag(AppTab.tools)
        }
        .tint(Theme.accent)
        .preferredColorScheme(appearance == "light" ? .light : appearance == "dark" ? .dark : nil)
        .fullScreenCover(item: $screen) { s in
            NavigationStack { s.view }
                .preferredColorScheme(appearance == "light" ? .light : appearance == "dark" ? .dark : nil)
        }
    }

    /// Ekran görüntüsü için: -openScreen weather gibi bir argümanla alt ekran doğrudan açılır (CI)
    @State private var screen = UserDefaults.standard.string(forKey: "openScreen").flatMap(DebugScreen.init(rawValue:))
}

enum DebugScreen: String, Identifiable {
    case monthly, imsakiye, alarm, timer, stopwatch, pomodoro, datecalc, world, weather, seasons, settings, homeItems,
         privacy, about, list, globe
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
        case .globe:
            // App Store önizleme videosu için: tam ekran dönen dünya
            GlobeView(fullscreen: true)
                .ignoresSafeArea()
                .toolbar(.hidden, for: .navigationBar)
                .statusBarHidden()
        }
    }
}
