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
            if phase == .active { Task { await PrayerNotifier.reschedule() } }
        }
    }
}

/// Beş sekme: Saat, Vakitler, Takvim, Araçlar, Diğer (dünya saatleri, hava, mevsimler, ayarlar)
struct RootView: View {
    @AppStorage("appearance") private var appearance = "system"
    /// Ekran görüntüsü için: -startTab 2 ile o sekmede açılır
    @State private var tab = UserDefaults.standard.integer(forKey: "startTab")

    var body: some View {
        TabView(selection: $tab) {
            ClockTab()
                .tabItem { Label(L("tab.clock"), systemImage: "clock") }
                .tag(0)
            PrayerTab()
                .tabItem { Label(L("tab.prayer"), systemImage: "moon.stars") }
                .tag(1)
            CalendarTab()
                .tabItem { Label(L("tab.calendar"), systemImage: "calendar") }
                .tag(2)
            ToolsTab()
                .tabItem { Label(L("tab.tools"), systemImage: "timer") }
                .tag(3)
            MoreTab()
                .tabItem { Label(L("tab.more"), systemImage: "ellipsis.circle") }
                .tag(4)
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
    case monthly, imsakiye, alarm, timer, stopwatch, pomodoro, datecalc, world, weather, seasons, settings, homeItems
    var id: String { rawValue }

    @MainActor @ViewBuilder var view: some View {
        let province = Province.find(UserDefaults.standard.string(forKey: "province") ?? "istanbul")
        switch self {
        case .monthly: MonthlyPrayerView(province: province)
        case .imsakiye:
            if let r = CalendarData.shared.nextRamadan(from: CalendarDay(Date())) { ImsakiyeView(province: province, ramadan: r) }
        case .alarm: AlarmsView()
        case .timer: TimerView()
        case .stopwatch: StopwatchView()
        case .pomodoro: PomodoroView()
        case .datecalc: DateCalcView()
        case .world: WorldClockView()
        case .weather: WeatherView()
        case .seasons: SeasonsView()
        case .settings: SettingsView()
        case .homeItems: NavigationStack { HomeItemsView() }
        }
    }
}
