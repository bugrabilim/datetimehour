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
        // Varsayılan açık (30 Ekim 2026, sahibin isteği: her özelliğin bildirimi açık gelir, kullanıcı kapatır)
        return .init(enabled: d.object(forKey: enabledKey) as? Bool ?? true,
                     prayers: decode(d.string(forKey: prayersKey) ?? defaultPrayers),
                     leadMinutes: d.integer(forKey: leadKey))
    }
}

/// Vakit ve özellik bildirimlerini (FeatureNotifier) telefonda kurar. iOS en çok 64 bekleyen yerel bildirim tutar:
/// saat başı bildirimleri saat başına bir yinelenen kayıt, geri kalan yer zamana göre sıralı tek seferlik bildirimlere
/// verilir; vakitler açıksa sonunda "uygulamayı açın" hatırlatması. Uygulama her açıldığında, arka plan yenilemesinde,
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

    /// İlk açılışta bildirim izni bir kez sorulur (bildirimler varsayılan açık); sonra yalnız Ayarlar'dan
    static func askOnce() async {
        let d = UserDefaults.standard
        guard !d.bool(forKey: "notify.asked") else { return }
        d.set(true, forKey: "notify.asked")
        if await authorizationStatus() == .notDetermined { _ = await requestAuthorization() }
    }

    /// Bu planın kurduğu bildirimler (alarm ve zamanlayıcı bildirimleri ayrı, onlara dokunulmaz)
    private static func isPlanned(_ id: String) -> Bool {
        id.hasPrefix("prayer-") || id == "reopen" || id.hasPrefix(FeatureNotifier.featurePrefix)
            || id.hasPrefix(FeatureNotifier.hourlyPrefix)
    }

    static func reschedule() async {
        generation += 1
        let mine = generation
        let center = UNUserNotificationCenter.current()
        let old = await center.pendingNotificationRequests().map(\.identifier).filter(isPlanned)
        center.removePendingNotificationRequests(withIdentifiers: old)
        guard mine == generation else { return }
        let status = await authorizationStatus()
        guard isAllowed(status), mine == generation else { return }

        let settings = NotificationPrefs.current
        let place = AppPlace.stored()
        let now = Date()
        var planned: [PlannedNotification] = []
        if settings.enabled {
            let alerts = PrayerAlerts.plan(schedule: place.schedule, settings: settings, now: now)
            // Her vakit için ses bir kez üretilir (sesli duyuru vakit adını söyler)
            var sounds: [Prayer: UNNotificationSound] = [:]
            NotificationSounds.removeStale(keeping: Set(Prayer.allCases.map { NotificationSounds.fileName($0, lead: settings.leadMinutes) }))
            for prayer in Set(alerts.map(\.prayer)) {
                sounds[prayer] = await NotificationSounds.sound(for: prayer, lead: settings.leadMinutes)
                guard mine == generation else { return }
            }
            planned += alerts.map { a in
                let name = a.prayer.localizedName
                return PlannedNotification(
                    id: "prayer-\(Int(a.prayerAt.timeIntervalSince1970))-\(a.prayer.rawValue)", fireAt: a.fireAt,
                    title: settings.leadMinutes > 0
                        ? String(format: L("notify.title.before"), name, String(settings.leadMinutes))
                        : String(format: L("notify.title.now"), name),
                    body: "\(place.name) · \(place.hm(a.prayerAt))", thread: "prayer", sound: sounds[a.prayer] ?? .default)
            }
        }
        planned += await FeatureNotifier.planned(place: place, now: now)
        guard mine == generation else { return }

        let hourly = FeatureNotifier.hourlyRequests()
        let room = max(0, 63 - hourly.count)
        let kept = Array(planned.sorted { $0.fireAt < $1.fireAt }.prefix(room))
        var requests = hourly + kept.map { p -> UNNotificationRequest in
            let content = UNMutableNotificationContent()
            content.title = p.title
            content.body = p.body
            content.sound = p.sound
            content.threadIdentifier = p.thread
            return UNNotificationRequest(identifier: p.id, content: content, trigger: trigger(at: p.fireAt))
        }
        if settings.enabled, let last = kept.last {
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

    /// Tam ekranda yön kilidi (OrientationLock)
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        MainActor.assumeIsolated { OrientationLock.mask }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }
}
