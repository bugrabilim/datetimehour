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

    init(district: District) {
        self.init(id: district.id, name: district.label, lat: district.lat, lon: district.lon, zone: PrayerSchedule.zone)
    }

    /// Kayıtlı kimlikten yer: il, ilçe ("d:il:ilçe") ya da otomatik
    static func byID(_ id: String) -> AppPlace {
        if id == autoID { return stored() }
        if let d = District.find(id) { return AppPlace(district: d) }
        return AppPlace(province: Province.find(id.hasPrefix("d:") ? String(id.split(separator: ":")[1]) : id))
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
        if slug.hasPrefix("d:"), let district = District.find(slug) { return AppPlace(district: district) }
        guard slug == autoID else { return AppPlace(province: Province.find(slug.hasPrefix("d:") ? String(slug.split(separator: ":")[1]) : slug)) }
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
        // Apple Watch "iPhone'daki yer" ve kilit ekranındaki sıradaki vakit
        WatchSync.shared.send(new)
        LiveActivities.sync()
    }
}

/// Konum seçici: "Konumumu kullan (otomatik)", kayıtlı yerler, 81 il ve 973 ilçe (arama ya da ile dokunup ilçeler).
/// Vakitler, hava, dünya saatleri ve ayarlarda.
struct PlacePicker: View {
    var font: Font = .headline
    @State private var model = PlaceModel.shared
    @State private var open = false

    var body: some View {
        let place = model.place
        Button { open = true } label: {
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
        .buttonStyle(.plain)
        .sheet(isPresented: $open) { PlaceSheet(model: model) }
        .alert(L("place.locationFailed"), isPresented: $model.failed) {
            Button(L("settings.openSettings")) {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            }
            Button(L("common.done"), role: .cancel) {}
        }
    }
}

private struct PlaceSheet: View {
    @Environment(\.dismiss) private var dismiss
    let model: PlaceModel
    @State private var query = ""
    @State private var saved = SavedPlaces.ids

    var body: some View {
        let place = model.place
        let q = District.fold(query.trimmingCharacters(in: .whitespaces))
        NavigationStack {
            List {
                if q.isEmpty {
                    Button {
                        dismiss()
                        Task { await model.useLocation() }
                    } label: { Label(L("place.useLocation"), systemImage: place.isAuto ? "location.fill" : "location") }
                    if !saved.isEmpty || !place.isAuto {
                        Section(L("place.saved")) {
                            ForEach(saved, id: \.self) { id in
                                row("★ " + AppPlace.byID(id).name, selected: id == place.id) { pick(id) }
                                    .swipeActions { Button(L("place.remove"), role: .destructive) { remove(id) } }
                                    .contextMenu { Button(L("place.remove"), role: .destructive) { remove(id) } }
                            }
                            if !place.isAuto && !saved.contains(place.id) {
                                Button("☆ " + L("place.save") + " (" + place.name + ")") {
                                    saved.append(place.id)
                                    SavedPlaces.set(saved)
                                }
                            }
                        }
                    }
                    Section(L("prayer.province")) {
                        ForEach(Province.all) { p in
                            NavigationLink {
                                DistrictList(province: p, current: place.id) { pick($0) }
                            } label: {
                                Text(p.displayName)
                                    .foregroundStyle(place.id == p.slug || place.id.hasPrefix("d:\(p.slug):") ? Theme.accent : Theme.text)
                            }
                        }
                    }
                } else {
                    ForEach(Province.all.filter { District.fold($0.displayName).contains(q) || District.fold($0.name).contains(q) }) { p in
                        row(p.displayName, selected: place.id == p.slug) { pick(p.slug) }
                    }
                    ForEach(Province.all.flatMap { District.all($0.slug) }.filter { !$0.isCentre && District.fold($0.name).contains(q) }.prefix(60)) { d in
                        row(d.label, selected: d.id == place.id) { pick(d.id) }
                    }
                }
                Text(L("place.districtSource")).font(.caption2).foregroundStyle(Theme.muted)
            }
            .searchable(text: $query, prompt: L("place.search"))
            .themedBackground()
            .tint(Theme.accent)
            .navigationTitle(L("settings.location"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } } }
        }
    }

    private func row(_ text: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(text).foregroundStyle(selected ? Theme.accent : Theme.text)
                Spacer()
                if selected { Image(systemName: "checkmark").foregroundStyle(Theme.accent) }
            }
        }
    }

    private func pick(_ id: String) {
        model.select(id)
        dismiss()
    }

    private func remove(_ id: String) {
        saved.removeAll { $0 == id }
        SavedPlaces.set(saved)
    }
}

/// Bir ilin ilçeleri; en üstte il merkezi
private struct DistrictList: View {
    let province: Province
    let current: String
    let onPick: (String) -> Void

    var body: some View {
        List {
            Button { onPick(province.slug) } label: {
                Text(T("place.wholeProvince", province.displayName)).foregroundStyle(current == province.slug ? Theme.accent : Theme.text)
            }
            Section(L("place.districts")) {
                ForEach(District.all(province.slug).filter { !$0.isCentre }) { d in
                    Button { onPick(d.id) } label: {
                        Text(d.name).foregroundStyle(current == d.id ? Theme.accent : Theme.text)
                    }
                }
            }
        }
        .themedBackground()
        .navigationTitle(province.displayName)
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Kayıtlı yerler arasında hızlı geçiş (Vakitler); kayıtlı yer yoksa görünmez
struct SavedPlaceChips: View {
    @State private var model = PlaceModel.shared
    @AppStorage(SavedPlaces.key) private var raw = ""

    var body: some View {
        let ids = raw.split(separator: "|").map(String.init)
        if !ids.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(ids, id: \.self) { id in
                        let on = model.place.id == id
                        Button { model.select(id) } label: {
                            Text(AppPlace.byID(id).name)
                                .font(.subheadline.weight(on ? .semibold : .regular))
                                .padding(.horizontal, 14).padding(.vertical, 8)
                                .background(on ? Theme.accentSoft : Theme.surface2, in: Capsule())
                                .foregroundStyle(on ? Theme.accent : Theme.text)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}
