import Foundation
import SaatCore
import WatchConnectivity

/// iPhone'daki yer (WatchConnectivity uygulama bağlamı: ad, enlem, boylam, saat dilimi). iPhone uygulaması yer değişince
/// ve öne gelince gönderir (App/Sources/WatchSync.swift); son gelen saatte saklanır, iPhone yanında olmasa da kullanılır.
final class PhonePlace: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = PhonePlace()
    private let storeKey = "phone.place"

    @Published private(set) var place: Place?
    /// iPhone'daki dünya saatleri (ad, saat dilimi); gelmediyse boş
    @Published private(set) var world: [(name: String, zone: TimeZone)] = []

    override init() {
        super.init()
        let c = UserDefaults.standard.dictionary(forKey: storeKey) ?? [:]
        place = Self.decode(c)
        world = Self.decodeWorld(c)
    }

    private static func decodeWorld(_ c: [String: Any]) -> [(name: String, zone: TimeZone)] {
        ((c["world"] as? [String]) ?? []).compactMap { row in
            let parts = row.split(separator: "|", maxSplits: 1).map(String.init)
            guard parts.count == 2, let z = TimeZone(identifier: parts[1]) else { return nil }
            return (name: parts[0], zone: z)
        }
    }

    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    private static func decode(_ c: [String: Any]) -> Place? {
        guard let name = c["name"] as? String, let lat = c["lat"] as? Double, let lon = c["lon"] as? Double else { return nil }
        let zone = (c["tz"] as? String).flatMap(TimeZone.init(identifier:)) ?? .current
        // Telefondaki vakit yöntemi; eski bağlamda yoksa yere göre varsayılan
        if let m = c["method"] as? String {
            return Place(name: name, schedule: PrayerSchedule(latitude: lat, longitude: lon, timeZone: zone,
                                                               parameters: .method(m, hanafi: c["hanafi"] as? Bool ?? false)))
        }
        return Place(name: name, schedule: SharedPrayerMethod.schedule(latitude: lat, longitude: lon, timeZone: zone))
    }

    private func apply(_ c: [String: Any]) {
        guard !c.isEmpty, let p = Self.decode(c) else { return }
        UserDefaults.standard.set(c, forKey: storeKey)
        let w = Self.decodeWorld(c)
        DispatchQueue.main.async { self.place = p; self.world = w }
    }

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        apply(session.receivedApplicationContext)
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        apply(applicationContext)
    }
}
