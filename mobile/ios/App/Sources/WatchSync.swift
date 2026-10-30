import Foundation
import WatchConnectivity

/// Apple Watch'a seçili yeri gönderir (WatchConnectivity uygulama bağlamı: ad, enlem, boylam, saat dilimi); saat
/// uygulamasında "iPhone'daki yer" seçiliyken vakitler bu yerle hesaplanır. Saat eşlenmemişse ya da saat uygulaması
/// kurulu değilse hiçbir şey gönderilmez; veri yalnız telefon ile saat arasında gider.
final class WatchSync: NSObject, WCSessionDelegate {
    static let shared = WatchSync()

    func start() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func send(_ place: AppPlace) {
        guard WCSession.isSupported() else { return }
        let s = WCSession.default
        guard s.activationState == .activated, s.isPaired, s.isWatchAppInstalled else { return }
        try? s.updateApplicationContext(["name": place.name, "lat": place.lat, "lon": place.lon, "tz": place.zone.identifier])
    }

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        DispatchQueue.main.async { WatchSync.shared.send(AppPlace.stored()) }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    func sessionDidDeactivate(_ session: WCSession) { session.activate() }
}
