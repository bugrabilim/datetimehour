import SwiftUI
import UIKit
import SaatCore

/*
 * Vakit ve ibadet ekleri (3 Kasım 2026, 2. analiz #57, #61, #63, #64, #65, #66; Android'deki WorshipExtras.kt ile aynı
 * kayıt anahtarları): dini günler listesi ve yaklaşan dini gün satırı, Vakitler'de "kıldım" işareti, kalan süreyi sözle
 * yazma, vakit simgeleri, 24 saat kadranı, vakte göre gökyüzü zemini, ibadet istatistikleri, ömür boyu kaza hesabı,
 * oruç kazasında sebep ve hayız/nifas döneminde takibi duraklatma. Kayıtlar yalnız telefonda; seri ve rozet bilerek yok.
 */

// MARK: - Dini günler (#57)

struct RelDay: Identifiable, Hashable {
    let day: CalendarDay
    let key: String
    var id: String { day.iso + key }
    var name: String { L("calendar.events.\(key)") }
}

enum RelDays {
    static func isReligious(_ e: CalendarData.Event) -> Bool {
        e.type == "religious" || ["rb1", "kb1", "rbEve", "kbEve"].contains(e.key)
    }

    static let all: [RelDay] = CalendarData.shared.years.values.flatMap(\.events)
        .filter(isReligious)
        .compactMap { e in CalendarDay(iso: e.date).map { RelDay(day: $0, key: e.key) } }
        .sorted { $0.day < $1.day }

    static func next(_ today: CalendarDay) -> RelDay? { all.first { $0.day >= today } }
    static func nextOf(_ key: String, _ today: CalendarDay) -> RelDay? { all.first { $0.key == key && $0.day >= today } }
    static var years: [Int] { Array(Set(all.map(\.day.year))).sorted() }

    static func days(_ a: CalendarDay, _ b: CalendarDay) -> Int { b.dayNumber - a.dayNumber }

    /// "Bugün", "Yarın", "3 gün kaldı", "Geçti"
    static func whenText(_ d: CalendarDay, _ today: CalendarDay) -> String {
        let n = days(today, d)
        if n == 0 { return L("client.today") }
        if n == 1 { return L("client.tomorrow") }
        if n < 0 { return L("client.passed") }
        return T("client.daysLeft", n)
    }

    /// Bayram namazı vakti (bayramın ilk günü) seçili yerde
    static func bayramAt(_ d: CalendarDay, _ place: AppPlace) -> Date? {
        let pd = PrayerTimes.times(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon, timeZone: place.zone)
        return PrayerTimes.bayram(pd).map { localInstant(d, $0, place.zone) }
    }
}

/// Vakitler'de: "Yaklaşan: Arefe · 3 gün kaldı"; dokununca dini günler listesi
struct UpcomingReligiousLine: View {
    let today: CalendarDay
    var body: some View {
        if let e = RelDays.next(today) {
            NavigationLink { ReligiousDaysView() } label: {
                Text(T("relday.upcoming", e.name, RelDays.whenText(e.day, today)))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.accent)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

struct ReligiousDaysView: View {
    @State private var year = CalendarDay(Date()).year

    var body: some View {
        let place = PlaceModel.shared.place
        let today = place.today
        ScrollView {
            VStack(spacing: 12) {
                if let e = RelDays.next(today) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(L("relday.next")).font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
                        Text(e.name).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                        Text(Fmt.day(e.day) + (Hijri.date(e.day.noon(in: .current)).map { " · " + $0.text } ?? ""))
                            .font(.subheadline).foregroundStyle(Theme.muted)
                        Text(RelDays.whenText(e.day, today)).font(.headline).foregroundStyle(Theme.accent)
                        if e.key == "rb1" || e.key == "kb1", let at = RelDays.bayramAt(e.day, place) {
                            Text(T("relday.bayramPrayerAt", place.hm(at))).foregroundStyle(Theme.text)
                        }
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 14))
                }
                let three = RelDays.nextOf("ucAylar", today), ram = RelDays.nextOf("ramadanStart", today)
                if three != nil || ram != nil {
                    VStack(alignment: .leading, spacing: 6) {
                        if let three { Text(T("relday.threeMonthsIn", RelDays.days(today, three.day))) }
                        if let ram { Text(T("relday.ramadanIn", RelDays.days(today, ram.day))) }
                    }
                    .foregroundStyle(Theme.text)
                    .card()
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(RelDays.years, id: \.self) { y in
                            Button { year = y } label: {
                                Text(verbatim: String(y)).monospacedDigit()
                                    .foregroundStyle(y == year ? Color.white : Theme.text)
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .background(y == year ? Theme.accent : Theme.surface2, in: Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .environment(\.layoutDirection, .leftToRight)
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(RelDays.all.filter { $0.day.year == year }) { e in
                        let past = e.day < today
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(e.name).fontWeight(.medium).foregroundStyle(past ? Theme.muted : Theme.text)
                                Text(Fmt.day(e.day)).font(.caption).foregroundStyle(Theme.muted)
                            }
                            Spacer()
                            Text(RelDays.whenText(e.day, today)).font(.footnote).foregroundStyle(past ? Theme.muted : Theme.accent)
                        }
                    }
                    if CalendarData.shared.years[year]?.projected == true {
                        Text(L("calendar.projected")).font(.caption).foregroundStyle(Theme.muted)
                    }
                }
                .card()
            }
            .padding(16)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("relday.title"))
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            let ys = RelDays.years
            if let f = ys.first, let l = ys.last { year = min(max(year, f), l) }
        }
    }
}

// MARK: - Takip ekleri (#61, #63, #64)

enum TrackX {
    private static var d: UserDefaults { .standard }

    private static func obj(_ key: String) -> [String: Any] {
        d.string(forKey: key).flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
    }

    private static func put(_ key: String, _ o: Any) {
        if let data = try? JSONSerialization.data(withJSONObject: o, options: [.sortedKeys]) {
            d.set(String(decoding: data, as: UTF8.self), forKey: key)
        }
    }

    static let inlineKey = "track.inline"
    /// Vakitler ekranında "kıldım" işareti (varsayılan açık)
    static var inline: Bool { d.object(forKey: inlineKey) as? Bool ?? true }

    static func bit(_ p: Prayer) -> Int? {
        switch p {
        case .fajr: 0
        case .dhuhr: 1
        case .asr: 2
        case .maghrib: 3
        case .isha: 4
        case .sunrise: nil
        }
    }

    /// Vakit girdikten kaç dakika sonra işaretlendi (yalnız aynı gün; -1 bilinmiyor)
    static func whenMinutes(_ day: CalendarDay) -> [Int] {
        let a = (obj("track.when")[day.iso] as? [NSNumber])?.map(\.intValue) ?? []
        return (0..<5).map { $0 < a.count ? a[$0] : -1 }
    }

    static func recordWhen(_ day: CalendarDay, _ i: Int, _ minutes: Int?) {
        var o = obj("track.when")
        var cur = whenMinutes(day)
        cur[i] = minutes ?? -1
        if cur.allSatisfy({ $0 < 0 }) { o.removeValue(forKey: day.iso) } else { o[day.iso] = cur }
        let cut = day.adding(days: -400).iso
        for k in o.keys where k < cut { o.removeValue(forKey: k) }
        put("track.when", o)
    }

    /// "Kıldım" işaretini değiştir; vakit içinde işaretlenirse kaç dakika sonra kılındığı da yazılır
    static func toggle(_ day: CalendarDay, _ p: Prayer, vakit: Date?, now: Date = Date()) {
        guard let i = bit(p) else { return }
        let mask = Tracker.dayMask(day)
        let on = mask & (1 << i) == 0
        Tracker.setDayMask(day, mask ^ (1 << i))
        var mins: Int?
        if on, let vakit, now >= vakit { mins = min(24 * 60, Int(now.timeIntervalSince(vakit) / 60)) }
        recordWhen(day, i, mins)
    }

    // Hayız/nifas duraklatması
    static var pausedFrom: CalendarDay? {
        get { d.string(forKey: "track.pausedFrom").flatMap(CalendarDay.init(iso:)) }
        set {
            if let v = newValue {
                d.set(v.iso, forKey: "track.pausedFrom")
            } else {
                if let old = pausedFrom {
                    let list = (pauses() + [(old, CalendarDay(Date()))]).suffix(200).map { [$0.0.iso, $0.1.iso] }
                    put("track.pauses", Array(list))
                }
                d.removeObject(forKey: "track.pausedFrom")
            }
        }
    }

    static func pauses() -> [(CalendarDay, CalendarDay)] {
        let raw = d.string(forKey: "track.pauses").flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [[String]] } ?? []
        return raw.compactMap { r in
            guard r.count == 2, let a = CalendarDay(iso: r[0]), let b = CalendarDay(iso: r[1]) else { return nil }
            return (a, b)
        }
    }

    static func isPaused(_ day: CalendarDay) -> Bool {
        if let f = pausedFrom, day >= f { return true }
        return pauses().contains { day >= $0.0 && day <= $0.1 }
    }

    // Kaza kılındıkça günlük sayaç (hız hesabı için)
    static func logKazaPrayed(_ day: CalendarDay = CalendarDay(Date())) {
        var o = obj("track.kazaLog")
        o[day.iso] = ((o[day.iso] as? NSNumber)?.intValue ?? 0) + 1
        let cut = day.adding(days: -60).iso
        for k in o.keys where k < cut { o.removeValue(forKey: k) }
        put("track.kazaLog", o)
    }

    static func kazaPerDay(_ today: CalendarDay = CalendarDay(Date())) -> Double {
        let o = obj("track.kazaLog")
        return Double((0..<30).reduce(0) { $0 + ((o[today.adding(days: -$1).iso] as? NSNumber)?.intValue ?? 0) }) / 30
    }

    static let fastReasons = ["illness", "travel", "period", "other"]

    /// Eski tek oruç kazası sayacı "Diğer" sebebine taşınır
    static func migrate() {
        let old = Tracker.kaza("fast")
        if old > 0 { Tracker.setKaza("fast.other", Tracker.kaza("fast.other") + old); Tracker.setKaza("fast", 0) }
    }

    static var fastTotal: Int { fastReasons.reduce(0) { $0 + Tracker.kaza("fast.\($1)") } }
    static var prayerKazaTotal: Int { Tracker.kaza.reduce(0) { $0 + Tracker.kaza($1) } }

    /// Gün sayısını yıl/ay/gün olarak
    static func span(_ days: Double) -> String {
        if days >= 365 {
            let y = days / 365.2425
            let v = y < 10 ? (y * 10).rounded() / 10 : y.rounded()
            let s = v == v.rounded() ? String(Int(v)) : String(v).replacingOccurrences(of: ".", with: AppLocale.numberLocale.decimalSeparator ?? ".")
            return T("track.unit.years", s)
        }
        if days >= 60 { return T("track.unit.months", Int((days / 30.44).rounded())) }
        return T("track.unit.days", max(1, Int(days.rounded())))
    }

    /// Ömür boyu kaza: ergenlikten düzenli kılmaya başlanan yıla kadar her vakit için gün sayısı
    static func lifetime(birth: Int, puberty: Int, start: Int, female: Bool, periodDays: Int) -> Int {
        let years = max(0, start - (birth + puberty))
        var days = Double(years) * 365.2425
        if female { days -= Double(years) * 12 * Double(min(15, max(0, periodDays))) }
        return max(0, Int(days.rounded()))
    }

    static func clearExtras() {
        for k in ["track.when", "track.pauses", "track.pausedFrom", "track.kazaLog", "track.life"] { d.removeObject(forKey: k) }
    }
}

/// "2/5" halkası
struct PrayedRing: View {
    let n: Int
    let total: Int
    var size: CGFloat = 34
    var body: some View {
        ZStack {
            Circle().stroke(Theme.surface2, lineWidth: size * 0.12)
            Circle().trim(from: 0, to: total > 0 ? CGFloat(n) / CGFloat(total) : 0)
                .stroke(Theme.accent, style: StrokeStyle(lineWidth: size * 0.12, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(n)").font(.system(size: size * 0.36, weight: .semibold)).monospacedDigit().foregroundStyle(Theme.text)
        }
        .frame(width: size, height: size)
    }
}

/// Vakit simgesi (#65)
/// Vakit satırlarındaki çizgi simge (doğal tasarım madde 4): imsak şafak, güneş doğuş, öğle tepe, ikindi alçak güneş,
/// akşam batış, yatsı gece. Android'deki LineIconMap.prayer ile aynı.
func prayerLineIcon(_ p: Prayer) -> String {
    switch p {
    case .fajr: "dawn"
    case .sunrise: "sunrise"
    case .dhuhr: "noon"
    case .asr: "afternoon"
    case .maghrib: "sunset"
    case .isha: "night"
    }
}

func prayerSymbol(_ p: Prayer) -> String {
    switch p {
    case .fajr: "sun.horizon"
    case .sunrise: "sunrise"
    case .dhuhr: "sun.max"
    case .asr: "sun.min"
    case .maghrib: "sunset"
    case .isha: "moon.stars"
    }
}

/// Vakte göre gökyüzü zemini (#65; doğal tasarım 4 Kasım 2026, docs/dogal-tasarim bölüm 3.1): gece, şafak, sabah,
/// öğle, ikindi ve akşam gökleri; yazı rengi gökyüzünün parlaklığından (krem ya da mürekkep). Koyu temada gündüz gökleri
/// karartılır, yazı krem kalır. Android'deki skyColors ile aynı renkler.
struct Sky {
    let colors: [Color]
    let text: Color
}

func skyColors(_ entries: [PrayerSchedule.Entry], _ now: Date, dark: Bool = false) -> Sky {
    func at(_ p: Prayer) -> Date? { entries.first { $0.prayer == p }?.at }
    func after(_ x: Date?) -> Bool { x.map { now >= $0 } ?? false }
    func c(_ hex: UInt32) -> Color { Color(red: Double(hex >> 16 & 0xFF) / 255, green: Double(hex >> 8 & 0xFF) / 255, blue: Double(hex & 0xFF) / 255) }
    let cream = c(0xFFF8EC), ink = c(0x2B2520)
    func sky(_ top: UInt32, _ bottom: UInt32, _ text: Color) -> Sky { Sky(colors: [c(top), c(bottom)], text: text) }
    if after(at(.isha)) || !after(at(.fajr)) { return sky(0x1F2240, 0x3B3762, cream) }
    if after(at(.maghrib)) { return sky(0x46305A, 0x8E4B4E, cream) }
    if after(at(.asr)) { return dark ? sky(0x4F3A22, 0x6E5638, cream) : sky(0xE0B577, 0xF5E3BF, ink) }
    if after(at(.dhuhr)) { return dark ? sky(0x284866, 0x47627A, cream) : sky(0x9CC4E0, 0xE3EEF2, ink) }
    if after(at(.sunrise)) { return dark ? sky(0x2D4A63, 0x4F5E6E, cream) : sky(0xA9C9DF, 0xF2E6CC, ink) }
    return sky(0x2E2C55, 0x8A5A78, cream)
}

/// Kalan süre sözle: "1 sa 42 dk sonra İkindi" (#66)
func countdownWords(_ seconds: Int, _ name: String) -> String {
    let mins = max(1, (seconds + 59) / 60)
    let t = mins >= 60 ? T("prayer.wordsHM", mins / 60, mins % 60) : T("prayer.wordsM", mins)
    return T("prayer.inWords", t, name)
}

/// Gökyüzü zeminli kalan süre bloğu (#65, #66): dokununca sözle/saat olarak
struct SkyCountdown: View {
    let entries: [PrayerSchedule.Entry]
    let next: PrayerSchedule.Entry
    let now: Date
    @AppStorage("prayer.words") private var words = false
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let left = Int(next.at.timeIntervalSince(now).rounded(.up))
        let sky = skyColors(entries, now, dark: scheme == .dark)
        Button { words.toggle() } label: {
            VStack(spacing: 6) {
                LineIcon(name: prayerLineIcon(next.prayer), size: 30, c1: sky.text, c2: sky.text)
                if words {
                    Text(countdownWords(left, next.prayer.localizedName))
                        .font(AppFonts.serif ? .custom("Literata-Medium", size: 28, relativeTo: .title) : .system(size: 28, weight: .semibold))
                        .multilineTextAlignment(.center)
                } else {
                    Text(String(format: String(localized: "prayer.nextIn"), next.prayer.localizedName))
                        .font(.subheadline).opacity(0.88)
                    // Büyük sayaç: ince (300) ve tablo rakamlı (doğal tasarım bölüm 3.2)
                    Text(Fmt.countdown(left))
                        .font(.system(size: 52, weight: .light))
                        .monospacedDigit()
                        .lineLimit(1).minimumScaleFactor(0.6)
                        .environment(\.layoutDirection, .leftToRight)
                }
            }
            .foregroundStyle(sky.text)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16).padding(.horizontal, 12)
            .background(LinearGradient(colors: sky.colors, startPoint: .top, endPoint: .bottom),
                        in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(countdownWords(left, next.prayer.localizedName))
        .accessibilityHint(L("prayer.wordsHint"))
    }
}

/// Vakitler'de "kıldım" satır sonu düğmesi
struct PrayedMark: View {
    let day: CalendarDay
    let entry: PrayerSchedule.Entry
    let now: Date
    @Binding var version: Int

    var body: some View {
        let _ = version
        if let i = TrackX.bit(entry.prayer), entry.at <= now, !TrackX.isPaused(day) {
            let on = Tracker.dayMask(day) & (1 << i) != 0
            Button {
                TrackX.toggle(day, entry.prayer, vakit: entry.at, now: now); version += 1
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } label: {
                Image(systemName: on ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(on ? Theme.accent : Theme.muted)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(T("track.markPrayed", entry.prayer.localizedName))
            .accessibilityAddTraits(on ? .isSelected : [])
        } else {
            Color.clear.frame(width: 32, height: 32)
        }
    }
}

/// Bugünün "2/5 kılındı" özeti ve "Hepsini işaretle"
struct PrayedSummary: View {
    let day: CalendarDay
    let entries: [PrayerSchedule.Entry]
    let now: Date
    @Binding var version: Int

    var body: some View {
        let _ = version
        if TrackX.isPaused(day) {
            if let f = TrackX.pausedFrom {
                Text(T("track.pausedSince", Fmt.day(f))).font(.caption).foregroundStyle(Theme.muted)
            }
        } else {
            let mask = Tracker.dayMask(day)
            let started = entries.filter { TrackX.bit($0.prayer) != nil && $0.at <= now }
            HStack(spacing: 10) {
                PrayedRing(n: mask.nonzeroBitCount, total: 5, size: 30)
                Text(T("track.prayedOf", mask.nonzeroBitCount, 5)).font(.subheadline).foregroundStyle(Theme.text)
                Spacer()
                if started.contains(where: { mask & (1 << TrackX.bit($0.prayer)!) == 0 }) {
                    Button(L("track.markAll")) {
                        for e in started where Tracker.dayMask(day) & (1 << TrackX.bit(e.prayer)!) == 0 {
                            TrackX.toggle(day, e.prayer, vakit: e.at, now: now)
                        }
                        version += 1
                    }
                    .font(.subheadline.weight(.semibold))
                    .tint(Theme.accent)
                }
            }
            .padding(.top, 8)
        }
    }
}

/// 24 saat kadranı (#65): gece yarısı üstte, vakit dilimleri renkli yay, noktalar ve şu anki saat ibresi
struct PrayerDial24: View {
    let entries: [PrayerSchedule.Entry]
    let place: AppPlace
    let now: Date

    private var start: Date {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        return cal.startOfDay(for: now)
    }

    var body: some View {
        let start = self.start
        let frac = { (d: Date) -> Double in d.timeIntervalSince(start) / 86400 }
        let colors: [Color] = [Color(red: 0.42, green: 0.36, blue: 0.66), Color(red: 0.95, green: 0.71, blue: 0.35), Color(red: 0.35, green: 0.66, blue: 0.90),
                               Color(red: 0.89, green: 0.60, blue: 0.23), Color(red: 0.75, green: 0.35, blue: 0.25), Color(red: 0.15, green: 0.14, blue: 0.35)]
        GeometryReader { g in
            let s = min(g.size.width, g.size.height)
            let r = s / 2 * 0.74
            let c = CGPoint(x: g.size.width / 2, y: g.size.height / 2)
            let sw = r * 0.16
            let pt = { (f: Double, rad: CGFloat) -> CGPoint in
                let a = (f * 360 - 90) * .pi / 180
                return CGPoint(x: c.x + rad * CGFloat(cos(a)), y: c.y + rad * CGFloat(sin(a)))
            }
            ZStack {
                Circle().stroke(Theme.surface2, lineWidth: sw).frame(width: 2 * r, height: 2 * r).position(c)
                ForEach(Array(entries.enumerated()), id: \.offset) { k, e in
                    let from = frac(e.at)
                    let to = k + 1 < entries.count ? frac(entries[k + 1].at) : frac(entries[0].at) + 1
                    Path { p in
                        p.addArc(center: c, radius: r, startAngle: .degrees(from * 360 - 90), endAngle: .degrees(to * 360 - 90), clockwise: false)
                    }
                    .stroke(colors[k % colors.count].opacity(0.85), lineWidth: sw)
                }
                ForEach(0..<24, id: \.self) { h in
                    Path { p in
                        p.move(to: pt(Double(h) / 24, r + sw * 0.75))
                        p.addLine(to: pt(Double(h) / 24, r + sw * 0.75 + (h % 6 == 0 ? sw * 0.6 : sw * 0.3)))
                    }
                    .stroke(Theme.muted, lineWidth: 1)
                    if h % 6 == 0 {
                        Text(String(format: "%02d", h)).font(.caption2).foregroundStyle(Theme.muted)
                            .position(pt(Double(h) / 24, r + sw * 1.35 + 10))
                    }
                }
                ForEach(Array(entries.enumerated()), id: \.offset) { _, e in
                    Circle().fill(.white).frame(width: sw * 0.64, height: sw * 0.64).position(pt(frac(e.at), r))
                    Text(e.prayer.localizedName).font(.caption2.weight(.medium)).foregroundStyle(Theme.text)
                        .position(pt(frac(e.at), r - sw * 1.6))
                }
                Path { p in p.move(to: c); p.addLine(to: pt(frac(now), r + sw * 0.5)) }
                    .stroke(Theme.accent, style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                Circle().fill(Theme.accent).frame(width: 10, height: 10).position(c)
                Text(place.hm(now)).font(.title3.weight(.bold)).monospacedDigit().foregroundStyle(Theme.text)
                    .position(x: c.x, y: c.y + 22)
            }
            .environment(\.layoutDirection, .leftToRight)
        }
        .aspectRatio(1, contentMode: .fit)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(entries.map { $0.prayer.localizedName + " " + place.hm($0.at) }.joined(separator: ", "))
    }
}

/// Güneş/Ay yayı ile 24 saat kadranı arasında dokunarak geçiş (seçim `prayer.visual`)
struct PrayerVisual: View {
    let now: Date
    let place: AppPlace
    @AppStorage("prayer.visual") private var visual = "arc"

    var body: some View {
        Button { visual = visual == "dial" ? "arc" : "dial" } label: {
            if visual == "dial" {
                PrayerDial24(entries: place.schedule.entries(on: now), place: place, now: now).card()
            } else {
                SunMoonArc(now: now, place: place)
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - İstatistik (#61)

struct TrackerStatsCard: View {
    let today: CalendarDay
    let version: Int

    var body: some View {
        let _ = version
        VStack(alignment: .leading, spacing: 10) {
            Text(L("track.stats")).font(.headline).foregroundStyle(Theme.text)
            Text(L("track.last7")).font(.caption).foregroundStyle(Theme.muted)
            HStack {
                ForEach((0...6).reversed(), id: \.self) { i in
                    let d = today.adding(days: -i)
                    VStack(spacing: 4) {
                        if TrackX.isPaused(d) {
                            Circle().fill(Theme.surface2).frame(width: 34, height: 34)
                        } else {
                            PrayedRing(n: Tracker.dayMask(d).nonzeroBitCount, total: 5)
                        }
                        Text(L("calendar.weekdaysShort.\(d.weekday)")).font(.caption2).foregroundStyle(Theme.muted)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            // Son 12 ay: kılınan / kılınabilecek
            Text(L("track.monthly")).font(.caption).foregroundStyle(Theme.muted).padding(.top, 4)
            let months: [(Int, Int)] = (0..<12).reversed().map { k in
                let m = today.month - k
                let y = today.year + Int((Double(m - 1) / 12).rounded(.down))
                return (y, ((m - 1) % 12 + 12) % 12 + 1)
            }
            HStack(alignment: .bottom, spacing: 4) {
                ForEach(Array(months.enumerated()), id: \.offset) { _, ym in
                    let days = (1...CalendarDay.daysIn(ym.0, ym.1)).map { CalendarDay(ym.0, ym.1, $0) }
                        .filter { $0 <= today && !TrackX.isPaused($0) }
                    let ratio = days.isEmpty ? 0 : Double(days.reduce(0) { $0 + Tracker.dayMask($1).nonzeroBitCount }) / Double(days.count * 5)
                    VStack(spacing: 2) {
                        RoundedRectangle(cornerRadius: 4).fill(ratio > 0 ? Theme.accent : Theme.surface2)
                            .frame(height: 4 + 66 * ratio)
                        Text(String(L("calendar.months.\(ym.1 - 1)").prefix(1))).font(.system(size: 10)).foregroundStyle(Theme.muted)
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(L("calendar.months.\(ym.1 - 1)") + " " + Fmt.percent(Int((ratio * 100).rounded())))
                }
            }
            .frame(height: 90, alignment: .bottom)
            .environment(\.layoutDirection, .leftToRight)
            // Son bir yıl ısı haritası (haftalar sütun, pazartesiden pazara satır)
            Text(L("track.heatmap")).font(.caption).foregroundStyle(Theme.muted).padding(.top, 4)
            let monday = today.adding(days: -today.weekday - 52 * 7)
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 2) {
                        ForEach(0...52, id: \.self) { w in
                            VStack(spacing: 2) {
                                ForEach(0..<7, id: \.self) { dow in
                                    let d = monday.adding(days: w * 7 + dow)
                                    let n = d > today ? -1 : Tracker.dayMask(d).nonzeroBitCount
                                    RoundedRectangle(cornerRadius: 2)
                                        .fill(n < 0 ? Color.clear : n == 0 ? Theme.surface2 : Theme.accent.opacity(0.25 + 0.15 * Double(n)))
                                        .frame(width: 10, height: 10)
                                }
                            }
                            .id(w)
                        }
                    }
                }
                .onAppear { proxy.scrollTo(52, anchor: .trailing) }
            }
            .environment(\.layoutDirection, .leftToRight)
            HStack(spacing: 3) {
                Text(L("track.less")).font(.caption2).foregroundStyle(Theme.muted)
                ForEach(0...5, id: \.self) { n in
                    RoundedRectangle(cornerRadius: 2).fill(n == 0 ? Theme.surface2 : Theme.accent.opacity(0.25 + 0.15 * Double(n)))
                        .frame(width: 10, height: 10)
                }
                Text(L("track.more")).font(.caption2).foregroundStyle(Theme.muted)
            }
            // Ne zaman kılındı (ortalama)
            let avg: [Int?] = (0..<5).map { i in
                let v = (0..<90).compactMap { k -> Int? in let m = TrackX.whenMinutes(today.adding(days: -k))[i]; return m >= 0 ? m : nil }
                return v.isEmpty ? nil : Int((Double(v.reduce(0, +)) / Double(v.count)).rounded())
            }
            if avg.contains(where: { $0 != nil }) {
                Text(L("track.whenTitle")).font(.caption).foregroundStyle(Theme.muted).padding(.top, 4)
                ForEach(Array(Tracker.prayers.enumerated()), id: \.offset) { i, p in
                    if let m = avg[i] {
                        HStack {
                            Text(L("track.prayer.\(p)")).foregroundStyle(Theme.text)
                            Spacer()
                            Text(T("track.avgAfter", m)).foregroundStyle(Theme.text)
                        }
                        .font(.subheadline)
                    }
                }
                Text(L("track.whenNote")).font(.caption2).foregroundStyle(Theme.muted)
            }
        }
        .card()
    }
}

// MARK: - Ömür boyu kaza (#63)

struct LifetimeKazaCard: View {
    var onApplied: () -> Void
    @State private var open = false
    @State private var birth = ""
    @State private var puberty = 15
    @State private var start = ""
    @State private var female = false
    @State private var period = 6
    @State private var loaded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button { open.toggle() } label: {
                HStack {
                    Text(L("track.life.title")).font(.headline).foregroundStyle(Theme.text)
                    Spacer()
                    Image(systemName: open ? "chevron.up" : "chevron.down").foregroundStyle(Theme.muted)
                }
            }
            .buttonStyle(.plain)
            if open {
                TextField(L("track.life.birth"), text: $birth).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                Picker(L("track.life.puberty"), selection: $puberty) {
                    ForEach(9...15, id: \.self) { Text("\($0)").tag($0) }
                }
                .pickerStyle(.menu).tint(Theme.accent)
                TextField(L("track.life.start"), text: $start).keyboardType(.numberPad).textFieldStyle(.roundedBorder)
                Toggle(L("track.life.female"), isOn: $female).tint(Theme.accent)
                if female {
                    Picker(L("track.life.period"), selection: $period) {
                        ForEach(3...10, id: \.self) { Text("\($0)").tag($0) }
                    }
                    .pickerStyle(.menu).tint(Theme.accent)
                }
                if let b = Int(birth), let s = Int(start), (1900...2100).contains(b), (1900...2100).contains(s) {
                    let n = TrackX.lifetime(birth: b, puberty: puberty, start: s, female: female, periodDays: period)
                    Text(T("track.life.result", n)).font(.headline).foregroundStyle(Theme.accent)
                    if n > 0 {
                        Button(L("track.life.apply")) {
                            for k in Tracker.kaza { Tracker.setKaza(k, Tracker.kaza(k) + n) }
                            onApplied()
                        }
                        .buttonStyle(.borderedProminent).tint(Theme.accent)
                    }
                }
                Text(L("track.life.note")).font(.caption).foregroundStyle(Theme.muted)
            }
        }
        .card()
        .onAppear(perform: load)
        .onChange(of: birth) { save() }
        .onChange(of: puberty) { save() }
        .onChange(of: start) { save() }
        .onChange(of: female) { save() }
        .onChange(of: period) { save() }
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        let o = UserDefaults.standard.string(forKey: "track.life").flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
        if let b = (o["b"] as? NSNumber)?.intValue, b > 0 { birth = String(b) }
        if let p = (o["p"] as? NSNumber)?.intValue { puberty = p }
        if let s = (o["s"] as? NSNumber)?.intValue, s > 0 { start = String(s) }
        female = (o["f"] as? NSNumber)?.boolValue ?? false
        if let d = (o["d"] as? NSNumber)?.intValue { period = d }
    }

    private func save() {
        guard loaded else { return }
        let o: [String: Any] = ["b": Int(birth) ?? 0, "p": puberty, "s": Int(start) ?? 0, "f": female, "d": period]
        if let data = try? JSONSerialization.data(withJSONObject: o, options: [.sortedKeys]) {
            UserDefaults.standard.set(String(decoding: data, as: UTF8.self), forKey: "track.life")
        }
    }
}

/// Kaza bitiş süresi: son 30 günün hızıyla ve günde 5 kazayla
struct KazaPace: View {
    var body: some View {
        let remaining = TrackX.prayerKazaTotal
        if remaining > 0 {
            let rate = TrackX.kazaPerDay()
            if rate > 0 {
                Text(T("track.pace", TrackX.span(Double(remaining) / rate))).font(.subheadline).foregroundStyle(Theme.text)
            }
            Text(T("track.paceFive", TrackX.span(Double(remaining) / 5))).font(.footnote).foregroundStyle(Theme.muted)
        }
    }
}

/// Oruç kazası sebeplere göre (#63)
struct FastKazaRows: View {
    @Binding var version: Int
    var body: some View {
        let _ = version
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(L("track.kazaFast")).foregroundStyle(Theme.text)
                Spacer()
                Text("\(TrackX.fastTotal)").font(.body.weight(.semibold)).monospacedDigit().frame(minWidth: 44)
                    .environment(\.layoutDirection, .leftToRight)
                Menu {
                    ForEach(TrackX.fastReasons, id: \.self) { r in
                        Button(L("track.reason.\(r)")) { Tracker.setKaza("fast.\(r)", Tracker.kaza("fast.\(r)") + 1); version += 1 }
                    }
                } label: {
                    Image(systemName: "plus.circle").font(.title3)
                }
                .accessibilityLabel(L("track.fastReason"))
            }
            ForEach(TrackX.fastReasons, id: \.self) { r in
                let n = Tracker.kaza("fast.\(r)")
                if n > 0 {
                    HStack {
                        Text(L("track.reason.\(r)")).font(.subheadline).foregroundStyle(Theme.muted).padding(.leading, 12)
                        Spacer()
                        Button { Tracker.setKaza("fast.\(r)", n - 1); version += 1 } label: { Image(systemName: "minus.circle") }
                            .accessibilityLabel(L("track.kazaPrayed"))
                        Text("\(n)").monospacedDigit().frame(minWidth: 36).environment(\.layoutDirection, .leftToRight)
                    }
                    .buttonStyle(.borderless)
                }
            }
        }
        .tint(Theme.accent)
    }
}

/// Hayız/nifas duraklatması (#63)
struct PauseRow: View {
    @Binding var version: Int
    var body: some View {
        let _ = version
        VStack(alignment: .leading, spacing: 4) {
            Toggle(L("track.pause"), isOn: Binding(get: { TrackX.pausedFrom != nil }, set: {
                TrackX.pausedFrom = $0 ? CalendarDay(Date()) : nil; version += 1
            }))
            .tint(Theme.accent)
            if let f = TrackX.pausedFrom {
                Text(T("track.pausedSince", Fmt.day(f))).font(.caption).foregroundStyle(Theme.muted)
            }
        }
    }
}
