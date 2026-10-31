import SwiftUI
import SaatCore

/* Mac menü çubuğu, Apple TV ve Vision Pro uygulamalarının ortak görünümü (rakip analizi listesi #95–#97): saat, tarih
   (miladi + hicri), sıradaki vakit ve kalan süre, bugünün vakitleri. Hesap telefondakiyle aynı çekirdekten (SaatCore),
   internet gerekmez; yer 81 il ya da cihazın konumu (en yakın il; yurt dışında tam konum). Kayıt `desk.place`. */

enum DeskPlace {
    static let key = "desk.place"

    static func resolve(_ sel: String, auto: Place?) -> Place {
        if sel == "auto" { return auto ?? Place(province: Province.find("istanbul")) }
        return Place(province: Province.find(sel))
    }

    /// Ramazan'da akşam "İftar", imsak "Sahur" (Ramazan günleri hicri tablodan: 9. ay)
    static func label(_ e: PrayerSchedule.Entry, zone: TimeZone) -> String {
        let ramadan = Hijri.date(e.at, timeZone: zone)?.month == 9
        if ramadan && e.prayer == .maghrib { return L("live.iftar") }
        if ramadan && e.prayer == .fajr { return L("live.sahur") }
        return e.prayer.localizedName
    }

    /// "1:05" (saat:dakika) ya da "12 dk"
    static func remaining(_ seconds: TimeInterval) -> String {
        let m = max(0, Int(seconds) / 60)
        return m >= 60 ? String(format: "%d:%02d", m / 60, m % 60) : T("desk.minutes", m)
    }

    /// "30 Ekim Cuma" (her bölgede Gregoryen takvimle)
    static func dayStyle(_ zone: TimeZone) -> Date.FormatStyle {
        var s = Date.FormatStyle.dateTime.day().month(.wide).year().weekday(.wide)
        s.calendar = Calendar(identifier: .gregorian)
        s.timeZone = zone
        return s
    }

    /// Saniyeli saat (her dilde Latin rakamı; 24/12 ayara göre)
    static func clock(_ date: Date, zone: TimeZone, seconds: Bool) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = zone
        let h12 = UserDefaults.standard.bool(forKey: "h12")
        f.dateFormat = (h12 ? "h:mm" : "HH:mm") + (seconds ? ":ss" : "") + (h12 ? " a" : "")
        return f.string(from: date)
    }
}

/// Mac, Apple TV ve Vision Pro'da T(): yer tutuculu metin ("{n} dk" → %1$@)
func T(_ key: String, _ args: CustomStringConvertible...) -> String {
    String(format: L(key), arguments: args.map { "\($0)" as CVarArg })
}

/// Büyük saat, tarih, sıradaki vakit ve bugünün vakitleri; `scale` ekran boyuna göre (TV büyük, Mac küçük)
struct DeskClock: View {
    let place: Place
    var scale: CGFloat = 1
    var seconds = true

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            let zone = place.schedule.timeZone
            let next = place.schedule.next(after: now)
            VStack(spacing: 14 * scale) {
                Text(DeskPlace.clock(now, zone: zone, seconds: seconds))
                    .font(.system(size: 92 * scale, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                VStack(spacing: 4 * scale) {
                    Text(now.formatted(DeskPlace.dayStyle(zone)))
                        .font(.system(size: 22 * scale))
                    if let h = Hijri.date(now, timeZone: zone) {
                        Text(h.text).font(.system(size: 18 * scale)).foregroundStyle(.orange)
                    }
                }
                if let next {
                    HStack(spacing: 10 * scale) {
                        Text("\(DeskPlace.label(next, zone: zone)) \(place.schedule.clock(next.at))")
                            .font(.system(size: 24 * scale, weight: .semibold))
                        Text(timerInterval: now...max(now, next.at), countsDown: true)
                            .font(.system(size: 24 * scale, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(.orange)
                    }
                }
                HStack(spacing: 22 * scale) {
                    ForEach(place.schedule.entries(on: now)) { e in
                        VStack(spacing: 2 * scale) {
                            Text(e.prayer.localizedName).font(.system(size: 15 * scale))
                            Text(place.schedule.clock(e.at)).font(.system(size: 20 * scale, weight: .medium)).monospacedDigit()
                        }
                        .foregroundStyle(e.at == next?.at ? Color.orange : Color.primary)
                    }
                }
                Text(place.name).font(.system(size: 15 * scale)).foregroundStyle(.secondary)
            }
            .environment(\.layoutDirection, .leftToRight)
            .multilineTextAlignment(.center)
        }
    }
}

/// Yer seçimi: cihazın konumu ya da 81 il
struct DeskPlaceList: View {
    @Binding var sel: String
    var onPick: () -> Void = {}

    var body: some View {
        List {
            row("auto", L("widget.province.auto"))
            ForEach(Province.all) { p in row(p.slug, p.displayName) }
        }
        .navigationTitle(L("wear.place"))
    }

    private func row(_ id: String, _ title: String) -> some View {
        Button {
            sel = id
            onPick()
        } label: {
            HStack {
                Text(title)
                Spacer()
                if sel == id { Image(systemName: "checkmark").foregroundStyle(.orange) }
            }
        }
    }
}
