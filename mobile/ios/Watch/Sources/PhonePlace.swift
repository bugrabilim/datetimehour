import Foundation
import SaatCore
import WatchConnectivity

/// iPhone'daki yer (WatchConnectivity uygulama bağlamı: ad, enlem, boylam, saat dilimi). iPhone uygulaması yer değişince
/// ve öne gelince gönderir (App/Sources/WatchSync.swift); son gelen saatte saklanır, iPhone yanında olmasa da kullanılır.
final class PhonePlace: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = PhonePlace()
    private let storeKey = "phone.place"

    @Published private(set) var place: Place?

    override init() {
        super.init()
        place = Self.decode(UserDefaults.standard.dictionary(forKey: storeKey) ?? [:])
    }

    func activate() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    private static func decode(_ c: [String: Any]) -> Place? {
        guard let name = c["name"] as? String, let lat = c["lat"] as? Double, let lon = c["lon"] as? Double else { return nil }
        let zone = (c["tz"] as? String).flatMap(TimeZone.init(identifier:)) ?? .current
        return Place(name: name, schedule: PrayerSchedule(latitude: lat, longitude: lon, timeZone: zone))
    }

    private func apply(_ c: [String: Any]) {
        guard !c.isEmpty, let p = Self.decode(c) else { return }
        UserDefaults.standard.set(c, forKey: storeKey)
        DispatchQueue.main.async { self.place = p }
    }

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        apply(session.receivedApplicationContext)
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        apply(applicationContext)
    }
}
