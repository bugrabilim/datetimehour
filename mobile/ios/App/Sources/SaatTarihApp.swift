import SwiftUI

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
    }
}
