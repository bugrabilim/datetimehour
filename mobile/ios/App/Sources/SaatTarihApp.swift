import SwiftUI

@main
struct SaatTarihApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            HomeView()
        }
        .onChange(of: scenePhase) { _, phase in
            // Her açılışta bildirim listesi yeniden kurulur (iOS'ta en çok 64 bekleyen bildirim)
            if phase == .active { Task { await PrayerNotifier.reschedule() } }
        }
    }
}
