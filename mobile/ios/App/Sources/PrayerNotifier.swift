import Foundation
import UIKit
import UserNotifications
import SaatCore

/// Bildirim tercihleri (UserDefaults; ekranlar @AppStorage ile aynı anahtarları kullanır).
enum NotificationPrefs {
    static let enabledKey = "notify.enabled"
    static let prayersKey = "notify.prayers"
    static let leadKey = "notify.lead"
    static let defaultPrayers = encode(PrayerAlerts.Settings.defaultPrayers)

    static func decode(_ raw: String) -> Set<Prayer> {
        Set(raw.split(separator: ",").compactMap { Prayer(rawValue: String($0)) })
    }

    static func encode(_ set: Set<Prayer>) -> String {
        Prayer.allCases.filter(set.contains).map(\.rawValue).joined(separator: ",")
    }

    static var current: PrayerAlerts.Settings {
        let d = UserDefaults.standard
        return .init(enabled: d.bool(forKey: enabledKey),
                     prayers: decode(d.string(forKey: prayersKey) ?? defaultPrayers),
                     leadMinutes: d.integer(forKey: leadKey))
    }
}

/// Vakit bildirimlerini telefonda kurar. iOS en çok 64 bekleyen yerel bildirim tutar: plan 63 vakit
/// (5 vakitle yaklaşık 12 gün) ve sonunda "uygulamayı açın" hatırlatması. Uygulama her açıldığında,
/// il ya da tercih değişince liste baştan kurulur.
@MainActor
enum PrayerNotifier {
    private static var generation = 0

    static func authorizationStatus() async -> UNAuthorizationStatus {
        await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }

    static func isAllowed(_ status: UNAuthorizationStatus) -> Bool {
        status == .authorized || status == .provisional || status == .ephemeral
    }

    static func requestAuthorization() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    static func reschedule() async {
        generation += 1
        let mine = generation
        let center = UNUserNotificationCenter.current()
        // Yalnız vakit bildirimleri silinir (alarm ve zamanlayıcı bildirimleri kalır)
        let old = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix("prayer-") || $0 == "reopen" }
        center.removePendingNotificationRequests(withIdentifiers: old)
        guard mine == generation else { return }
        let settings = NotificationPrefs.current
        guard settings.enabled else { return }
        let status = await authorizationStatus()
        guard isAllowed(status), mine == generation else { return }

        let province = Province.find(UserDefaults.standard.string(forKey: "province") ?? "istanbul")
        let alerts = PrayerAlerts.plan(schedule: PrayerSchedule(province: province), settings: settings, now: Date())
        // Her vakit için ses bir kez üretilir (sesli duyuru vakit adını söyler)
        var sounds: [Prayer: UNNotificationSound] = [:]
        NotificationSounds.removeStale(keeping: Set(Prayer.allCases.map { NotificationSounds.fileName($0, lead: settings.leadMinutes) }))
        for prayer in Set(alerts.map(\.prayer)) {
            sounds[prayer] = await NotificationSounds.sound(for: prayer, lead: settings.leadMinutes)
            guard mine == generation else { return }
        }
        var requests = alerts.map { a -> UNNotificationRequest in
            let content = UNMutableNotificationContent()
            let name = a.prayer.localizedName
            content.title = settings.leadMinutes > 0
                ? String(format: L("notify.title.before"), name, String(settings.leadMinutes))
                : String(format: L("notify.title.now"), name)
            content.body = "\(province.displayName) · \(PrayerSchedule.hm.string(from: a.prayerAt))"
            content.sound = sounds[a.prayer] ?? .default
            content.threadIdentifier = "prayer"
            return UNNotificationRequest(identifier: "prayer-\(Int(a.prayerAt.timeIntervalSince1970))-\(a.prayer.rawValue)",
                                         content: content, trigger: trigger(at: a.fireAt))
        }
        if let last = alerts.last {
            let content = UNMutableNotificationContent()
            content.title = L("notify.heading")
            content.body = L("notify.reopen")
            requests.append(UNNotificationRequest(identifier: "reopen", content: content,
                                                  trigger: trigger(at: last.fireAt.addingTimeInterval(30 * 60))))
        }
        for r in requests {
            guard mine == generation else { return } // daha yeni bir kurulum başladı
            try? await center.add(r)
        }
    }

    /// Anı Türkiye saatiyle tam tarih olarak verir: cihazın saat dilimi değişse de vakit kaymaz.
    private static func trigger(at date: Date) -> UNCalendarNotificationTrigger {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = PrayerSchedule.zone
        var c = cal.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        c.calendar = cal
        c.timeZone = PrayerSchedule.zone
        return UNCalendarNotificationTrigger(dateMatching: c, repeats: false)
    }
}

/// Uygulama açıkken gelen bildirim de afiş ve sesle gösterilir.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }
}
