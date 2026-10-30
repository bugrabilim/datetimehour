import SwiftUI
import SaatCore

/*
 * Vakitler ekleri (30 Ekim 2026, rakip analizi listesi #65–#67, #73):
 * - Hesap yöntemi ve Hanefî ikindisi (sitedeki methodParams ile aynı; `prayer.method`, `prayer.hanafi`).
 * - Güneş/Ay yayı: gündüz Güneş'in, gece Ay'ın gökteki yolu, vakit işaretleriyle.
 * - Vakitte gerçek alarm (iOS 26+ AlarmKit): seçili vakitler için önümüzdeki 3 gün (`prayer.alarm`).
 * - Hilal görünürlüğü kartı (Yallop; Ay takvimi ekranı).
 * Android'deki PrayerExtras.kt ile aynı anahtarlar.
 */

// MARK: - Hesap yöntemi

enum PrayerMethod {
    static let methodKey = "prayer.method"
    static let hanafiKey = "prayer.hanafi"
    static var id: String { UserDefaults.standard.string(forKey: methodKey) ?? "diyanet" }
    static var hanafi: Bool { UserDefaults.standard.bool(forKey: hanafiKey) }
    static var parameters: PrayerTimes.Parameters { .method(id, hanafi: hanafi) }
    static var isCustom: Bool { id != "diyanet" || hanafi }
    static func name(_ id: String) -> String { L("prayer.method.names.\(id)") }
}

struct PrayerMethodView: View {
    @AppStorage(PrayerMethod.methodKey) private var method = "diyanet"
    @AppStorage(PrayerMethod.hanafiKey) private var hanafi = false

    var body: some View {
        Form {
            Section {
                Picker(L("prayer.method.label"), selection: $method) {
                    ForEach(PrayerTimes.methodIDs, id: \.self) { Text(PrayerMethod.name($0)).tag($0) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            } header: { Text(L("prayer.method.label")) }
            Section {
                Picker(L("prayer.method.asrLabel"), selection: $hanafi) {
                    Text(L("prayer.method.asrStd")).tag(false)
                    Text(L("prayer.method.asrHanafi")).tag(true)
                }
                .pickerStyle(.inline)
                .labelsHidden()
            } header: { Text(L("prayer.method.asrLabel")) } footer: { Text(L("prayer.method.appNote")) }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("prayer.method.app"))
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: method) { refresh() }
        .onChange(of: hanafi) { refresh() }
    }

    private func refresh() {
        Task {
            await PrayerNotifier.reschedule()
            await PrayerAlarms.sync()
            LiveActivities.sync()
        }
    }
}

// MARK: - Güneş/Ay yayı

struct SunMoonArc: View {
    let now: Date
    let place: AppPlace

    private struct Span { let start: Date; let end: Date; let day: Bool }

    private func span(_ schedule: PrayerSchedule) -> (Span, [(Date, String)])? {
        func sun(_ d: Date) -> (rise: Date, set: Date)? {
            let e = schedule.entries(on: d)
            guard let r = e.first(where: { $0.prayer == .sunrise })?.at, let s = e.first(where: { $0.prayer == .maghrib })?.at else { return nil }
            return (r, s)
        }
        guard let today = sun(now) else { return nil }
        if now >= today.rise && now < today.set {
            let marks = schedule.entries(on: now).filter { [.dhuhr, .asr].contains($0.prayer) }.map { ($0.at, $0.prayer.localizedName) }
            return (Span(start: today.rise, end: today.set, day: true), marks)
        }
        if now < today.rise, let y = sun(now.addingTimeInterval(-86400)) {
            return (Span(start: y.set, end: today.rise, day: false), [])
        }
        if let t = sun(now.addingTimeInterval(86400)) {
            let isha = schedule.entries(on: now).first { $0.prayer == .isha }.map { [($0.at, $0.prayer.localizedName)] } ?? []
            return (Span(start: today.set, end: t.rise, day: false), isha + schedule.entries(on: t.rise).filter { $0.prayer == .fajr }.map { ($0.at, $0.prayer.localizedName) })
        }
        return nil
    }

    var body: some View {
        if let (sp, marks) = span(place.schedule) {
            let total = sp.end.timeIntervalSince(sp.start)
            let f = max(0, min(1, now.timeIntervalSince(sp.start) / total))
            let moon = Nature.moon(at: now)
            VStack(alignment: .leading, spacing: 8) {
                Text(L("prayer.arc.title")).font(.headline).foregroundStyle(Theme.text)
                Canvas { ctx, size in
                    let left: CGFloat = 18, right = size.width - 18, base = size.height - 22, h = size.height - 40
                    func point(_ x: Double) -> CGPoint { CGPoint(x: left + (right - left) * x, y: base - h * sin(.pi * x)) }
                    var horizon = Path(); horizon.move(to: CGPoint(x: 4, y: base)); horizon.addLine(to: CGPoint(x: size.width - 4, y: base))
                    ctx.stroke(horizon, with: .color(Theme.muted.opacity(0.5)), lineWidth: 1)
                    var arc = Path()
                    for i in 0...60 { let p = point(Double(i) / 60); if i == 0 { arc.move(to: p) } else { arc.addLine(to: p) } }
                    ctx.stroke(arc, with: .color(Theme.muted.opacity(0.6)), style: StrokeStyle(lineWidth: 1.5, dash: [4, 4]))
                    var done = Path()
                    for i in 0...60 where Double(i) / 60 <= f { let p = point(Double(i) / 60); if i == 0 { done.move(to: p) } else { done.addLine(to: p) } }
                    ctx.stroke(done, with: .color(sp.day ? Color.orange : Theme.accent), lineWidth: 3)
                    for (at, _) in marks {
                        let x = at.timeIntervalSince(sp.start) / total
                        guard x > 0, x < 1 else { continue }
                        let p = point(x)
                        ctx.fill(Path(ellipseIn: CGRect(x: p.x - 3, y: p.y - 3, width: 6, height: 6)), with: .color(Theme.muted))
                    }
                    let c = point(f)
                    if sp.day {
                        ctx.fill(Path(ellipseIn: CGRect(x: c.x - 11, y: c.y - 11, width: 22, height: 22)), with: .color(.orange))
                    } else {
                        // Ay: aydınlık disk, karanlık kısım evreye göre kaydırılmış daire
                        let r: CGFloat = 10, rect = CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)
                        ctx.fill(Path(ellipseIn: rect), with: .color(Color(white: 0.92)))
                        let shift = CGFloat(moon.fraction < 0.5 ? -1 : 1) * 2 * r * CGFloat(moon.illumination)
                        var shade = ctx
                        shade.clip(to: Path(ellipseIn: rect))
                        shade.fill(Path(ellipseIn: rect.offsetBy(dx: shift, dy: 0)), with: .color(Color(white: 0.25)))
                    }
                }
                .frame(height: 130)
                .accessibilityHidden(true)
                HStack {
                    Text(place.hm(sp.start)).monospacedDigit()
                    Spacer()
                    Text(T(sp.day ? "prayer.arc.day" : "prayer.arc.night", Fmt.hm(seconds: Int(sp.end.timeIntervalSince(now))))).font(.footnote)
                    Spacer()
                    Text(place.hm(sp.end)).monospacedDigit()
                }
                .font(.subheadline)
                .foregroundStyle(Theme.muted)
                .environment(\.layoutDirection, .leftToRight)
            }
            .card()
        }
    }
}

// MARK: - Vakitte alarm (iOS 26+ AlarmKit)

enum PrayerAlarms {
    static let key = "prayer.alarm"
    private static let idsKey = "prayer.alarm.ids"
    static let days = 3

    static var selected: Set<Prayer> {
        Set((UserDefaults.standard.string(forKey: key) ?? "").split(separator: ",").compactMap { Prayer(rawValue: String($0)) })
    }
    static func setSelected(_ s: Set<Prayer>) {
        UserDefaults.standard.set(Prayer.allCases.filter(s.contains).map(\.rawValue).joined(separator: ","), forKey: key)
    }
    static var supported: Bool { if #available(iOS 26.0, *) { return true } else { return false } }

    /// Önümüzdeki 3 günün seçili vakitleri (geçmiş olanlar hariç)
    static func plan(now: Date = Date()) -> [(Prayer, Date)] {
        let sel = selected
        guard !sel.isEmpty else { return [] }
        let schedule = AppPlace.stored().schedule
        return (0..<days).flatMap { d in
            schedule.entries(on: now.addingTimeInterval(Double(d) * 86400)).filter { sel.contains($0.prayer) && $0.at > now }.map { ($0.prayer, $0.at) }
        }
    }

    /// Kurulu vakit alarmlarını siler, seçime göre yeniden kurar (açılış, arka plan yenilemesi, ayar değişimi)
    static func sync() async {
        #if canImport(AlarmKit)
        if #available(iOS 26.0, *) {
            for id in (UserDefaults.standard.stringArray(forKey: idsKey) ?? []).compactMap(UUID.init(uuidString:)) { KitAlarms.cancel(id: id) }
            var ids: [String] = []
            for (prayer, at) in plan() {
                let id = UUID()
                if await KitAlarms.schedule(id: id, at: at, title: T("prayer.alarm.title", prayer.localizedName)) { ids.append(id.uuidString) }
            }
            UserDefaults.standard.set(ids, forKey: idsKey)
        }
        #endif
    }
}

struct PrayerAlarmSection: View {
    @State private var sel = PrayerAlarms.selected
    var body: some View {
        Section {
            if PrayerAlarms.supported {
                ForEach(Prayer.allCases, id: \.self) { p in
                    Toggle(p.localizedName, isOn: Binding(get: { sel.contains(p) }, set: { on in
                        if on { sel.insert(p) } else { sel.remove(p) }
                        PrayerAlarms.setSelected(sel)
                        Task { await PrayerAlarms.sync() }
                    }))
                }
            }
        } header: { Text(L("prayer.alarm.heading")) } footer: {
            Text(L(PrayerAlarms.supported ? "prayer.alarm.note" : "prayer.alarm.unsupported"))
        }
    }
}

// MARK: - Hilal görünürlüğü

struct CrescentSection: View {
    let place: AppPlace
    let now: Date

    var body: some View {
        let m = Nature.moon(at: now)
        let conj: Int64 = m.age < 3 ? Int64((now.timeIntervalSince1970 - m.age * 86400) * 1000) : m.nextNewMoon
        let evenings = Nature.crescent(conjunction: conj, latitude: place.lat, longitude: place.lon)
        Section {
            ForEach(evenings, id: \.sunset) { e in
                VStack(alignment: .leading, spacing: 3) {
                    Text(Fmt.dayMonth(Date(unixMilliseconds: e.sunset), tz: place.zone)).font(.headline)
                    Text(L("nature.crescentCats.\(e.category)"))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(["A", "B"].contains(e.category) ? Theme.accent : Theme.muted)
                    Text(detail(e)).font(.caption).foregroundStyle(Theme.muted)
                }
            }
        } header: { Text(L("nature.crescentHeading")) } footer: {
            VStack(alignment: .leading, spacing: 4) {
                Text(T("nature.crescentLead", place.name, Fmt.shortDateTime(Date(unixMilliseconds: conj), tz: place.zone)))
                Text(L("nature.crescentNote"))
            }
        }
    }

    private func detail(_ e: Nature.CrescentEvening) -> String {
        var parts = ["☀︎ " + place.hm(Date(unixMilliseconds: e.sunset))]
        if let ms = e.moonset {
            let lag = e.lag ?? 0
            parts.append("☾ " + place.hm(Date(unixMilliseconds: ms)) + " (" + (lag > 0 ? T("nature.crescentLag", Int(lag.rounded())) : L("nature.crescentBefore")) + ")")
        }
        if e.age > 0 { parts.append(T("nature.crescentAge", Int(e.age.rounded()))) }
        return parts.joined(separator: " · ")
    }
}
