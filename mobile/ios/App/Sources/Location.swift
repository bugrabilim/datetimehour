import CoreLocation
import SaatCore

/// Tek seferlik konum. Konum yalnız telefonda vakit hesabı için kullanılır, hiçbir yere gönderilmez.
@MainActor
final class OneShotLocation: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var authWait: CheckedContinuation<Void, Never>?
    private var locWait: CheckedContinuation<CLLocation?, Never>?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    /// - Parameter requestPermission: izin sorulabilir mi (yalnız uygulamada; widget izin isteyemez)
    /// - Parameter forWidget: widget'ta, kullanıcı widget'lar için konuma izin verdi mi diye de bakılır
    func fetch(requestPermission: Bool, forWidget: Bool = false) async -> CLLocation? {
        if manager.authorizationStatus == .notDetermined {
            guard requestPermission else { return nil }
            await withCheckedContinuation { c in
                authWait = c
                manager.requestWhenInUseAuthorization()
            }
        }
        let status = manager.authorizationStatus
        guard status == .authorizedWhenInUse || status == .authorizedAlways else { return nil }
        if forWidget && !manager.isAuthorizedForWidgetUpdates { return nil }
        if let last = manager.location, -last.timestamp.timeIntervalSinceNow < 30 * 60 { return last }
        return await withCheckedContinuation { c in
            locWait = c
            manager.requestLocation()
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        MainActor.assumeIsolated {
            guard manager.authorizationStatus != .notDetermined, let c = authWait else { return }
            authWait = nil
            c.resume()
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        MainActor.assumeIsolated {
            locWait?.resume(returning: locations.last)
            locWait = nil
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        MainActor.assumeIsolated {
            locWait?.resume(returning: nil)
            locWait = nil
        }
    }
}

/// Vakitlerin hesaplanacağı yer: bir il ya da (yurt dışında) tam konum.
struct Place {
    let name: String
    let schedule: PrayerSchedule

    init(name: String, schedule: PrayerSchedule) {
        self.name = name
        self.schedule = schedule
    }

    init(province: Province) {
        self.init(name: province.displayName, schedule: PrayerSchedule(province: province))
    }

    /// En yakın il; en yakın il merkezi 150 km'den uzaksa (yurt dışı) tam konum ve cihazın saat dilimi.
    init(location: CLLocation) {
        let (province, km) = Province.nearest(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
        if km <= 150 {
            self.init(province: province)
        } else {
            self.init(name: L("place.myLocation"),
                      schedule: PrayerSchedule(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
                                               timeZone: .current))
        }
    }
}
