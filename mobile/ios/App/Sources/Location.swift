import CoreLocation
import SaatCore

/// Tek seferlik konum. Konum yalnız telefonda vakit hesabı için kullanılır, hiçbir yere gönderilmez.
@MainActor
final class OneShotLocation: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var authWait: CheckedContinuation<Void, Never>?
    private var locWait: CheckedContinuation<CLLocation?, Never>?

    init(accuracy: CLLocationAccuracy = kCLLocationAccuracyKilometer) {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = accuracy
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
        #if os(macOS)
        guard status == .authorizedAlways else { return nil }
        #elseif os(visionOS)
        guard status == .authorizedWhenInUse else { return nil }
        #else
        guard status == .authorizedWhenInUse || status == .authorizedAlways else { return nil }
        #endif
        #if os(iOS)
        if forWidget && !manager.isAuthorizedForWidgetUpdates { return nil }
        #endif
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

/// Vakit yöntemi (widget, saat ve masaüstü uygulamaları dahil; 10 Kasım 2026): uygulamada seçildiyse App Group'taki seçim
/// (`PrayerMethod.publish`), yoksa yerin saat dilimine göre varsayılan (Endonezya Kemenag, Güney Asya Karaçi + Hanefî,
/// Türkiye ve diğerleri Diyanet).
enum SharedPrayerMethod {
    static func parameters(latitude: Double, longitude: Double, timeZone: TimeZone) -> PrayerTimes.Parameters {
        let g = UserDefaults(suiteName: "group.tr.bumba.time")
        if let id = g?.string(forKey: "prayer.method") {
            return .method(id, hanafi: g?.bool(forKey: "prayer.hanafi") ?? false)
        }
        let d = PrayerTimes.defaultMethod(timeZone: timeZone.identifier, latitude: latitude, longitude: longitude)
        return .method(d.id, hanafi: d.hanafi)
    }

    static func schedule(latitude: Double, longitude: Double, timeZone: TimeZone) -> PrayerSchedule {
        PrayerSchedule(latitude: latitude, longitude: longitude, timeZone: timeZone,
                       parameters: parameters(latitude: latitude, longitude: longitude, timeZone: timeZone))
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
        self.init(name: province.displayName,
                  schedule: SharedPrayerMethod.schedule(latitude: province.lat, longitude: province.lon, timeZone: PrayerSchedule.zone))
    }

    /// En yakın il; en yakın il merkezi 150 km'den uzaksa (yurt dışı) tam konum ve cihazın saat dilimi.
    init(location: CLLocation) {
        let (province, km) = Province.nearest(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
        if km <= 150 {
            self.init(province: province)
        } else {
            self.init(name: L("place.myLocation"),
                      schedule: SharedPrayerMethod.schedule(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
                                                            timeZone: .current))
        }
    }
}

/// Konum yokken varsayılan yer (Global düşün, 15 Kasım 2026): telefonun saat dilimi Türkiye'ninse İstanbul ili (Diyanet),
/// değilse o dilimin şehri ya da en kalabalık yeri (`zone-places.json`, gen-strings üretir; uygulamadaki DefaultPlace
/// kuralı), dilim tabloda yoksa aynı bölgede (Europe/, America/…) şu an aynı UTC farkındaki en kalabalık yer. Saat
/// uygulamaları, widget'lar ve Mac/TV/Vision kullanır.
enum ZoneDefault {
    struct Entry { let lat: Double, lon: Double, name: String }
    private struct Row { let zone: String, lat: Double, lon: Double, pop: Double, names: [String] }
    private static let turkey: Set<String> = ["Europe/Istanbul", "Asia/Istanbul", "Turkey"]
    /// Verideki eski adlar (telefon yeni adı verebilir)
    private static let aliases = [
        "Europe/Kyiv": "Europe/Kiev", "Asia/Calcutta": "Asia/Kolkata", "Asia/Saigon": "Asia/Ho_Chi_Minh",
        "Asia/Katmandu": "Asia/Kathmandu", "Asia/Rangoon": "Asia/Yangon", "Asia/Yangon": "Asia/Rangoon",
        "America/Buenos_Aires": "America/Argentina/Buenos_Aires", "Asia/Dacca": "Asia/Dhaka",
    ]
    private static let table: (langs: [String], rows: [Row]) = {
        guard let url = Bundle.main.url(forResource: "zone-places", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let langs = obj["l"] as? [String], let z = obj["z"] as? [[Any]] else { return ([], []) }
        let rows = z.compactMap { r -> Row? in
            guard r.count == 5, let zone = r[0] as? String, let lat = (r[1] as? NSNumber)?.doubleValue,
                  let lon = (r[2] as? NSNumber)?.doubleValue, let pop = (r[3] as? NSNumber)?.doubleValue,
                  let names = r[4] as? [String] else { return nil }
            return Row(zone: zone, lat: lat, lon: lon, pop: pop, names: names)
        }
        return (langs, rows)
    }()

    static func inTurkey(_ zone: TimeZone = .current) -> Bool { turkey.contains(zone.identifier) }

    static func find(_ zone: TimeZone = .current) -> Entry? {
        guard !inTurkey(zone) else { return nil }
        let ids = Set([zone.identifier, aliases[zone.identifier]].compactMap { $0 })
        let rows = table.rows
        var row = rows.first { ids.contains($0.zone) }
        if row == nil, let slash = zone.identifier.firstIndex(of: "/") {
            let region = String(zone.identifier[..<slash]) + "/", now = Date(), off = zone.secondsFromGMT(for: now)
            row = rows.filter { $0.zone.hasPrefix(region) && TimeZone(identifier: $0.zone)?.secondsFromGMT(for: now) == off }
                .max { $0.pop < $1.pop }
        }
        guard let r = row else { return nil }
        let pref = Bundle.main.preferredLocalizations.first ?? "en"
        let lang = pref.hasPrefix("zh") ? "zh" : pref
        let i = table.langs.firstIndex(of: lang) ?? table.langs.firstIndex(of: "en") ?? 0
        let name = i < r.names.count && !r.names[i].isEmpty ? r.names[i] : (r.names.first ?? "")
        return Entry(lat: r.lat, lon: r.lon, name: name)
    }
}

extension Place {
    /// Konum yoksa (izin verilmedi ya da bulunamadı): saat diliminin yeri; Türkiye'de ya da veride yoksa İstanbul ili
    static func fallback(_ zone: TimeZone = .current) -> Place {
        if let e = ZoneDefault.find(zone) {
            return Place(name: e.name, schedule: SharedPrayerMethod.schedule(latitude: e.lat, longitude: e.lon, timeZone: zone))
        }
        return Place(province: Province.find("istanbul"))
    }
}
