import CoreLocation
import MapKit
import SwiftUI
import UserNotifications

/// Konuma bağlı hatırlatıcı (rakip analizi listesi #93): bir yere varınca ya da oradan ayrılınca sesli bildirim.
/// iOS bölge bildirimi (UNLocationNotificationTrigger) kullanılır: konum takibini sistem yapar, uygulama arka planda
/// çalışmaz ve konum hiçbir yere gönderilmez. "Uygulamayı kullanırken" konum izni ve "Kesin konum" gerekir; iOS en çok
/// 20 bölge izler. Kayıt `geoAlarms` (JSON; Android'le aynı alanlar).
struct GeoAlarm: Codable, Identifiable, Equatable {
    var id = UUID().uuidString
    var label = ""
    var lat: Double
    var lon: Double
    /// Yarıçap (m)
    var r = 200
    /// "arrive" (varınca) ya da "leave" (ayrılınca)
    var on = "arrive"
    /// Her seferinde mi, bir kez mi
    var repeats = true
    var enabled = true

    static let radii = [100, 200, 300, 500, 1000]

    var title: String { label.isEmpty ? L("geo.defaultLabel") : label }
    var coordinate: CLLocationCoordinate2D { CLLocationCoordinate2D(latitude: lat, longitude: lon) }
}

@MainActor
enum GeoAlarms {
    static let key = "geoAlarms"
    static let prefix = "geo-"
    static let limit = 20

    static func load() -> [GeoAlarm] {
        guard let raw = UserDefaults.standard.string(forKey: key), let d = raw.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([GeoAlarm].self, from: d)) ?? []
    }

    static func save(_ list: [GeoAlarm]) {
        if let d = try? JSONEncoder().encode(Array(list.prefix(limit))) { UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: key) }
    }

    /// Bekleyen bildirim yerinden düşülecek sayı (PrayerNotifier 64 sınırını paylaştırır)
    static var activeCount: Int { load().filter(\.enabled).count }

    /// Uygulama öne gelince: bir kezlik hatırlatıcı çaldıysa (bekleyen bildirimi kalmadıysa) kapatılır
    static func refresh() async {
        let list = load()
        guard list.contains(where: { $0.enabled && !$0.repeats }),
              await PrayerNotifier.isAllowed(PrayerNotifier.authorizationStatus()) else { return }
        let pending = Set(await UNUserNotificationCenter.current().pendingNotificationRequests().map(\.identifier))
        var changed = false
        let updated = list.map { g -> GeoAlarm in
            guard g.enabled, !g.repeats, !pending.contains(prefix + g.id) else { return g }
            var off = g
            off.enabled = false
            changed = true
            return off
        }
        if changed { save(updated) }
    }

    /// Bölge bildirimlerini baştan kurar
    static func sync() async {
        let center = UNUserNotificationCenter.current()
        let old = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix(prefix) }
        center.removePendingNotificationRequests(withIdentifiers: old)
        let list = load().filter(\.enabled)
        guard !list.isEmpty else { return }
        _ = try? await center.requestAuthorization(options: [.alert, .sound])
        for g in list {
            let region = CLCircularRegion(center: g.coordinate, radius: CLLocationDistance(g.r), identifier: prefix + g.id)
            region.notifyOnEntry = g.on == "arrive"
            region.notifyOnExit = g.on == "leave"
            let content = UNMutableNotificationContent()
            content.title = g.title
            content.body = L(g.on == "arrive" ? "geo.arrived" : "geo.left")
            content.sound = .default
            content.interruptionLevel = .active
            content.threadIdentifier = "geo"
            let trigger = UNLocationNotificationTrigger(region: region, repeats: g.repeats)
            try? await center.add(UNNotificationRequest(identifier: prefix + g.id, content: content, trigger: trigger))
        }
    }
}

/// Konum izni durumu ve "Uygulamayı kullanırken" izni isteme
@MainActor
final class GeoPermission: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager: CLLocationManager
    @Published var status: CLAuthorizationStatus
    @Published var precise: Bool

    override init() {
        let m = CLLocationManager()
        manager = m
        status = m.authorizationStatus
        precise = m.accuracyAuthorization == .fullAccuracy
        super.init()
        m.delegate = self
    }

    var allowed: Bool { status == .authorizedWhenInUse || status == .authorizedAlways }

    func request() { manager.requestWhenInUseAuthorization() }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        MainActor.assumeIsolated {
            status = manager.authorizationStatus
            precise = manager.accuracyAuthorization == .fullAccuracy
        }
    }
}

struct GeoAlarmsView: View {
    @State private var list = GeoAlarms.load()
    @State private var editing: GeoAlarm?
    @StateObject private var permission = GeoPermission()

    var body: some View {
        List {
            if list.isEmpty { Text(L("geo.none")).foregroundStyle(Theme.muted) }
            ForEach($list) { $g in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(g.title).font(.headline)
                        Text(L(g.on == "arrive" ? "geo.onArrive" : "geo.onLeave") + " · " + T("geo.radiusM", g.r)
                             + (g.repeats ? "" : " · " + L("geo.once")))
                            .font(.subheadline).foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Toggle(L("tools.alarm.enable"), isOn: $g.enabled).labelsHidden()
                }
                .contentShape(Rectangle())
                .onTapGesture { editing = g }
            }
            .onDelete { list.remove(atOffsets: $0) }
            Section {
                if !permission.allowed {
                    Button(L("geo.allow")) { permission.request() }
                    Text(L("geo.denied")).font(.footnote).foregroundStyle(.red)
                } else if !permission.precise {
                    Text(L("geo.notPrecise")).font(.footnote).foregroundStyle(.red)
                }
                Text(L("geo.note")).font(.footnote).foregroundStyle(Theme.muted)
            }
        }
        .themedBackground()
        .navigationTitle(L("geo.title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    let p = AppPlace.stored()
                    editing = GeoAlarm(lat: p.lat, lon: p.lon)
                } label: { Image(systemName: "plus") }
                .disabled(list.count >= GeoAlarms.limit)
                .accessibilityLabel(L("geo.add"))
            }
        }
        .sheet(item: $editing) { g in
            GeoAlarmEditor(alarm: g) { saved in
                if let i = list.firstIndex(where: { $0.id == saved.id }) { list[i] = saved } else { list.append(saved) }
                if !permission.allowed { permission.request() }
            }
        }
        .onChange(of: list) {
            GeoAlarms.save(list)
            Task {
                await GeoAlarms.sync()
                await PrayerNotifier.reschedule()
            }
        }
    }
}

/// Haritada yer seçimi (dokununca iğne taşınır), yarıçap, varınca/ayrılınca, bir kez/her seferinde
private struct GeoAlarmEditor: View {
    @Environment(\.dismiss) private var dismiss
    @State var alarm: GeoAlarm
    let onSave: (GeoAlarm) -> Void
    @State private var camera: MapCameraPosition = .automatic
    @State private var locating = false

    var body: some View {
        NavigationStack {
            Form {
                TextField(L("geo.labelPlaceholder"), text: $alarm.label)
                Section {
                    MapReader { proxy in
                        Map(position: $camera) {
                            Marker(alarm.title, coordinate: alarm.coordinate).tint(Theme.accent)
                            MapCircle(center: alarm.coordinate, radius: CLLocationDistance(alarm.r))
                                .foregroundStyle(Theme.accent.opacity(0.18))
                                .stroke(Theme.accent, lineWidth: 1.5)
                        }
                        .onTapGesture { point in
                            if let c = proxy.convert(point, from: .local) {
                                alarm.lat = c.latitude
                                alarm.lon = c.longitude
                            }
                        }
                    }
                    .frame(height: 260)
                    .listRowInsets(EdgeInsets())
                    Button {
                        locate()
                    } label: {
                        Label(L(locating ? "geo.locating" : "geo.useMine"), systemImage: "location")
                    }
                    .disabled(locating)
                } footer: {
                    Text(L("geo.mapHint"))
                }
                Section {
                    Picker(L("geo.when"), selection: $alarm.on) {
                        Text(L("geo.onArrive")).tag("arrive")
                        Text(L("geo.onLeave")).tag("leave")
                    }
                    .pickerStyle(.segmented)
                    Picker(L("geo.radius"), selection: $alarm.r) {
                        ForEach(GeoAlarm.radii, id: \.self) { Text(T("geo.radiusM", $0)).tag($0) }
                    }
                    Toggle(L("geo.every"), isOn: $alarm.repeats).tint(Theme.accent)
                }
            }
            .themedBackground()
            .navigationTitle(L("geo.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("alarm.cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("alarm.music.save")) {
                        onSave(alarm)
                        dismiss()
                    }
                }
            }
            .onAppear { center() }
        }
    }

    private func center() {
        camera = .region(MKCoordinateRegion(center: alarm.coordinate, latitudinalMeters: Double(alarm.r) * 6, longitudinalMeters: Double(alarm.r) * 6))
    }

    private func locate() {
        locating = true
        Task {
            if let loc = await OneShotLocation(accuracy: kCLLocationAccuracyNearestTenMeters).fetch(requestPermission: true) {
                alarm.lat = loc.coordinate.latitude
                alarm.lon = loc.coordinate.longitude
                center()
            }
            locating = false
        }
    }
}

/// Takvimdeki bir etkinliğe alarm (rakip analizi listesi #93): etkinlik seçilir, başlangıcından seçilen süre önce
/// bir kezlik alarm kurulur (tüm gün etkinlikte o gün 09.00). Yalnız o anki etkinlik bilgisi kullanılır.
enum CalendarAlarm {
    static let leadKey = "alarm.calLead"
    static let leads = [0, 5, 10, 15, 30, 60]

    static func item(title: String, start: Date, allDay: Bool, lead: Int) -> AlarmItem? {
        let cal = Calendar.current
        let at = allDay ? cal.date(bySettingHour: 9, minute: 0, second: 0, of: start) ?? start : start.addingTimeInterval(Double(-lead * 60))
        guard at > Date() else { return nil }
        let c = cal.dateComponents([.hour, .minute], from: at)
        var item = AlarmItem(hour: c.hour ?? 9, minute: c.minute ?? 0)
        item.label = String(title.prefix(40))
        item.date = CalendarDay(at).iso
        return item
    }
}
