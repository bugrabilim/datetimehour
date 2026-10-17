import SwiftUI
import CoreLocation
import WidgetKit
import SaatCore

/// Uygulamanın konumu: bir il ya da "Otomatik" (telefonun konumundan; en yakın il 150 km içindeyse o il,
/// değilse tam konum ve cihazın saat dilimi). "province" anahtarında il ya da "auto"; son bulunan konum "auto.*".
struct AppPlace: Equatable {
    static let autoID = "auto"

    let id: String
    let name: String
    let lat: Double
    let lon: Double
    let zone: TimeZone

    var isAuto: Bool { id == Self.autoID }
    /// Konum değişince yeniden yükleme için (hava durumu)
    var key: String { String(format: "%.2f,%.2f", lat, lon) }
    var schedule: PrayerSchedule { PrayerSchedule(latitude: lat, longitude: lon, timeZone: zone) }
    var today: CalendarDay { CalendarDay(Date(), timeZone: zone) }

    init(id: String, name: String, lat: Double, lon: Double, zone: TimeZone) {
        self.id = id
        self.name = name
        self.lat = lat
        self.lon = lon
        self.zone = zone
    }

    init(province: Province, id: String? = nil) {
        self.init(id: id ?? province.slug, name: province.displayName, lat: province.lat, lon: province.lon, zone: PrayerSchedule.zone)
    }

    /// Bu yerin saatiyle "HH:mm" (her dilde Latin rakamı)
    func hm(_ date: Date) -> String {
        if zone == PrayerSchedule.zone { return PrayerSchedule.hm.string(from: date) }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        f.timeZone = zone
        return f.string(from: date)
    }

    /// Kayıtlı seçim
    static func stored(_ d: UserDefaults = .standard) -> AppPlace {
        let slug = d.string(forKey: "province") ?? "istanbul"
        guard slug == autoID else { return AppPlace(province: Province.find(slug)) }
        guard d.object(forKey: "auto.lat") != nil else { return AppPlace(province: Province.find("istanbul"), id: autoID) }
        let lat = d.double(forKey: "auto.lat"), lon = d.double(forKey: "auto.lon")
        let ref = d.string(forKey: "auto.ref") ?? ""
        if ref.hasPrefix("p:") {
            return AppPlace(province: Province.find(String(ref.dropFirst(2))), id: autoID)
        }
        let zone = d.string(forKey: "auto.tz").flatMap(TimeZone.init(identifier:)) ?? .current
        let city = ref.hasPrefix("c:") ? City.all.first { $0.key == ref.dropFirst(2) } : nil
        return AppPlace(id: autoID, name: city?.displayName ?? L("place.myLocation"), lat: lat, lon: lon, zone: zone)
    }

    /// Konumu kaydeder: en yakın il (150 km), değilse yakındaki dünya şehrinin adı (100 km) ya da "Konumum"
    static func save(_ loc: CLLocation, _ d: UserDefaults = .standard) {
        let lat = loc.coordinate.latitude, lon = loc.coordinate.longitude
        let (p, km) = Province.nearest(latitude: lat, longitude: lon)
        var ref = ""
        if km <= 150 {
            ref = "p:" + p.slug
        } else if let c = City.all.min(by: { distance($0.lat, $0.lon, lat, lon) < distance($1.lat, $1.lon, lat, lon) }),
                  distance(c.lat, c.lon, lat, lon) <= 100 {
            ref = "c:" + c.key
        }
        // 2 ondalık (~1 km): vakit için yeterli
        d.set((lat * 100).rounded() / 100, forKey: "auto.lat")
        d.set((lon * 100).rounded() / 100, forKey: "auto.lon")
        d.set(TimeZone.current.identifier, forKey: "auto.tz")
        d.set(ref, forKey: "auto.ref")
    }

    private static func distance(_ aLat: Double, _ aLon: Double, _ bLat: Double, _ bLon: Double) -> Double {
        CLLocation(latitude: aLat, longitude: aLon).distance(from: CLLocation(latitude: bLat, longitude: bLon)) / 1000
    }
}

/// Seçili konum; değişince vakit bildirimleri ve widget'lar yenilenir.
@MainActor @Observable
final class PlaceModel {
    static let shared = PlaceModel()

    private(set) var place = AppPlace.stored()
    private(set) var locating = false
    /// Konum izni verilmedi ya da konum alınamadı
    var failed = false

    func select(_ slug: String) {
        UserDefaults.standard.set(slug, forKey: "province")
        apply()
    }

    /// "Konumumu kullan": izin ister, konumu bulur, Otomatik'e geçer
    func useLocation() async {
        locating = true
        defer { locating = false }
        guard let loc = await OneShotLocation().fetch(requestPermission: true) else {
            failed = true
            return
        }
        AppPlace.save(loc)
        UserDefaults.standard.set(AppPlace.autoID, forKey: "province")
        apply()
    }

    /// Uygulama öne gelince: Otomatik'teyse konum sessizce yenilenir (izin sorulmaz)
    func refresh() async {
        guard place.isAuto, let loc = await OneShotLocation().fetch(requestPermission: false) else { return }
        AppPlace.save(loc)
        if AppPlace.stored() != place { apply() }
    }

    private func apply() {
        let new = AppPlace.stored()
        guard new != place else { return }
        place = new
        Task { await PrayerNotifier.reschedule() }
        WidgetCenter.shared.reloadAllTimelines()
    }
}

/// Konum seçici: "Konumumu kullan (otomatik)" ve 81 il. Vakitler, dünya saatleri ve ayarlarda.
struct PlacePicker: View {
    var font: Font = .headline
    @State private var model = PlaceModel.shared

    var body: some View {
        let place = model.place
        Menu {
            Button {
                Task { await model.useLocation() }
            } label: {
                Label(L("place.useLocation"), systemImage: place.isAuto ? "location.fill" : "location")
            }
            Picker(L("prayer.province"), selection: Binding(
                get: { place.isAuto ? "" : place.id },
                set: { model.select($0) }
            )) {
                ForEach(Province.all) { Text($0.displayName).tag($0.slug) }
            }
        } label: {
            HStack(spacing: 6) {
                if model.locating {
                    ProgressView()
                } else {
                    Image(systemName: place.isAuto ? "location.fill" : "mappin.and.ellipse")
                }
                Text(place.name)
                Image(systemName: "chevron.up.chevron.down").font(.caption2)
            }
            .font(font)
            .foregroundStyle(Theme.accent)
        }
        .alert(L("place.locationFailed"), isPresented: $model.failed) {
            Button(L("settings.openSettings")) {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            }
            Button(L("common.done"), role: .cancel) {}
        }
    }
}
