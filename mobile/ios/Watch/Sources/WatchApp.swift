import SwiftUI
import SaatCore

/// Apple Watch uygulaması (rakip analizi #6): saat ve tarih (miladi + hicri), sıradaki vakit ve kalan süre, bugünün
/// vakitleri, yer seçimi (iPhone'daki yer, saatin konumu ya da 81 il). Hesap telefondakiyle aynı çekirdekten (SaatCore),
/// internet gerekmez. Komplikasyonlar WatchWidget hedefinde.
@main
struct SaatTarihWatchApp: App {
    init() { PhonePlace.shared.activate() }

    var body: some Scene {
        WindowGroup { WatchHome() }
    }
}

/// Saatteki yer seçimi: "phone" (iPhone'daki yer; WatchConnectivity ile gelir), "auto" (saatin konumu) ya da il
enum WatchPlace {
    static let key = "watch.place"

    static func resolve(_ sel: String, auto: Place?) -> Place {
        switch sel {
        case "phone": return PhonePlace.shared.place ?? Place(province: Province.find("istanbul"))
        case "auto": return auto ?? Place(province: Province.find("istanbul"))
        default: return Place(province: Province.find(sel))
        }
    }

    /// Ramazan'da akşam "İftar", imsak "Sahur" (Ramazan günleri hicri tablodan: 9. ay)
    static func label(_ e: PrayerSchedule.Entry, zone: TimeZone) -> String {
        let ramadan = Hijri.date(e.at, timeZone: zone)?.month == 9
        if ramadan && e.prayer == .maghrib { return L("live.iftar") }
        if ramadan && e.prayer == .fajr { return L("live.sahur") }
        return e.prayer.localizedName
    }
}

struct WatchHome: View {
    @AppStorage(WatchPlace.key) private var sel = "phone"
    @ObservedObject private var phone = PhonePlace.shared
    @State private var autoPlace: Place?

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = WatchPlace.resolve(sel, auto: autoPlace)
                let zone = place.schedule.timeZone
                let next = place.schedule.next(after: now)
                List {
                    VStack(spacing: 2) {
                        Text(place.schedule.clock(now))
                            .font(.system(size: 40, weight: .semibold, design: .rounded))
                            .monospacedDigit()
                        Text(now.formatted(Self.dayStyle(zone)))
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                        if let h = Hijri.date(now, timeZone: zone) {
                            Text(h.text).font(.footnote).foregroundStyle(.orange)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                    if let next {
                        VStack(spacing: 2) {
                            Text("\(WatchPlace.label(next, zone: zone)) \(place.schedule.clock(next.at))")
                                .font(.headline)
                            Text(timerInterval: now...max(now, next.at), countsDown: true)
                                .font(.system(size: 28, weight: .bold, design: .rounded))
                                .monospacedDigit()
                                .foregroundStyle(.orange)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    Section(L("wear.today")) {
                        ForEach(place.schedule.entries(on: now)) { e in
                            HStack {
                                Text(e.prayer.localizedName)
                                Spacer()
                                Text(place.schedule.clock(e.at)).monospacedDigit()
                            }
                            .foregroundStyle(e.at == next?.at ? Color.orange : Color.primary)
                        }
                    }
                    NavigationLink {
                        PlaceList(sel: $sel)
                    } label: {
                        VStack(alignment: .leading) {
                            Text(place.name)
                            Text(L("wear.place")).font(.caption2).foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .task(id: sel) {
            guard sel == "auto" else { return }
            if let loc = await OneShotLocation().fetch(requestPermission: true) { autoPlace = Place(location: loc) }
        }
    }

    /// "30 Ekim Cuma" (her bölgede Gregoryen takvimle)
    private static func dayStyle(_ zone: TimeZone) -> Date.FormatStyle {
        var s = Date.FormatStyle.dateTime.day().month(.wide).weekday(.wide)
        s.calendar = Calendar(identifier: .gregorian)
        s.timeZone = zone
        return s
    }
}

/// Yer seçimi: iPhone'daki yer, saatin konumu ya da 81 il
private struct PlaceList: View {
    @Binding var sel: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            row("phone", L("wear.phonePlace"))
            row("auto", L("widget.province.auto"))
            ForEach(Province.all) { p in row(p.slug, p.displayName) }
        }
        .navigationTitle(L("wear.place"))
    }

    private func row(_ id: String, _ title: String) -> some View {
        Button {
            sel = id
            dismiss()
        } label: {
            HStack {
                Text(title)
                Spacer()
                if sel == id { Image(systemName: "checkmark").foregroundStyle(.orange) }
            }
        }
    }
}
