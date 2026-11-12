import Foundation
import SaatCore

/// Hicri tarih (rakip analizi #9). Ay başları Diyanet'e hizalı tablodan (hijri.json; sitedeki lib/hijri.mjs üretir:
/// Diyanet vakit tablolarındaki hicri tarihler ve dini günler, dayanağı olmayan aylarda Umm al-Qura). Platformun
/// Umm al-Qura takvimi bazı aylarda Diyanet'ten bir gün ayrıldığı için kullanılmaz. Gün gece yarısı değişir.
/// Tablonun dışında (şu an 21 Aralık 2025 – 13 Ocak 2032) tarih yoktur.
///
/// Endonezya takvimi (10 Kasım 2026; hijri-id.json, lib/hijri.mjs hijriTableId: Kemenag takvimi, isbat kararları, dışında
/// MABIMS hesabı): `hijri.cal` "auto" (varsayılan; yer Endonezya'daysa Endonezya, değilse Diyanet), "diyanet" ya da "kemenag".
/// `hijri.offset` −1/0/+1: ay başı bir gün önce/sonra. Android'deki Hijri.kt ile aynı kural.
struct HijriDate: Equatable {
    let year: Int
    let month: Int
    let day: Int

    var monthName: String { L("hijri.month.\(month)") }
    /// "19 Cemaziyelevvel 1448"
    var text: String { String(format: L("hijri.fmt"), "\(day)", monthName, "\(year)") }
}

enum Hijri {
    typealias Row = (start: Int, year: Int, month: Int)
    static let calKey = "hijri.cal", offsetKey = "hijri.offset", zoneKey = "hijri.zone"
    private static let groupID = "group.tr.bumba.time"

    /// [(ay başının gün numarası, hicri yıl, ay)]; son satır tablonun sonu
    private static func load(_ name: String) -> [Row] {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let rows = try? JSONSerialization.jsonObject(with: data) as? [[Any]] else { return [] }
        return rows.compactMap { r in
            guard r.count == 3, let iso = r[0] as? String, let y = r[1] as? Int, let m = r[2] as? Int else { return nil }
            let p = iso.split(separator: "-").compactMap { Int($0) }
            guard p.count == 3 else { return nil }
            return (jdn(p[0], p[1], p[2]), y, m)
        }
    }
    private static let diyanet = load("hijri")
    private static let kemenag = load("hijri-id")

    /// Ayarlar: uygulamada kendi kayıtları; widget'ta App Group (uygulama `publish` ile yazar); saat ve Mac/TV/Vision'da
    /// kendi kayıtları (ayar yok: otomatik, cihazın saat dilimiyle)
    private static var store: UserDefaults {
        #if WIDGET_EXTENSION
        return UserDefaults(suiteName: groupID) ?? .standard
        #else
        return .standard
        #endif
    }
    /// Ayardaki seçim: "auto", "diyanet", "kemenag"
    static var setting: String { store.string(forKey: calKey) ?? "auto" }
    /// Ay başı düzeltmesi (gün): −1, 0, +1
    static var offset: Int { max(-1, min(1, store.integer(forKey: offsetKey))) }
    /// Seçili yerin saat dilimi (otomatik seçim için; uygulama yer değişince yazar), yoksa cihazınki
    static var zone: String { store.string(forKey: zoneKey) ?? TimeZone.current.identifier }
    static var hasKemenag: Bool { !kemenag.isEmpty }
    /// Kullanılan takvim: "diyanet" ya da "kemenag"
    static var calendar: String {
        switch setting {
        case "kemenag": return hasKemenag ? "kemenag" : "diyanet"
        case "diyanet": return "diyanet"
        default: return hasKemenag && PrayerTimes.region(timeZone: zone) == "ID" ? "kemenag" : "diyanet"
        }
    }
    /// Takvim ve düzeltme ("diyanet:0" varsayılan); önbellekler bununla yenilenir
    static var key: String { "\(calendar):\(offset)" }
    static var isDefault: Bool { key == "diyanet:0" }

    /// Uygulama: yerin saat dilimini ve ayarları widget'a (App Group) yayımlar
    static func publish(zone tz: TimeZone) {
        UserDefaults.standard.set(tz.identifier, forKey: zoneKey)
        guard let g = UserDefaults(suiteName: groupID) else { return }
        g.set(tz.identifier, forKey: zoneKey)
        g.set(UserDefaults.standard.string(forKey: calKey) ?? "auto", forKey: calKey)
        g.set(UserDefaults.standard.integer(forKey: offsetKey), forKey: offsetKey)
    }

    /// Kullanılan tablo: seçili takvim, ay başları düzeltme kadar kaydırılmış
    private static var table: [Row] {
        let base = calendar == "kemenag" ? kemenag : diyanet
        let off = offset
        return off == 0 ? base : base.map { (start: $0.start + off, year: $0.year, month: $0.month) }
    }

    private static func date(in t: [Row], _ n: Int) -> HijriDate? {
        guard t.count >= 2 else { return nil }
        for i in stride(from: t.count - 2, through: 0, by: -1) where n >= t[i].start {
            return n < t[i + 1].start ? HijriDate(year: t[i].year, month: t[i].month, day: n - t[i].start + 1) : nil
        }
        return nil
    }

    private static func start(in t: [Row], year: Int, month: Int) -> (start: Int, length: Int)? {
        guard t.count >= 2 else { return nil }
        for i in 0..<(t.count - 1) where t[i].year == year && t[i].month == month {
            return (t[i].start, t[i + 1].start - t[i].start)
        }
        return nil
    }

    /// Diyanet tablosundaki hicri tarih (calendar.json'daki günlerin dayanağı)
    static func baseDate(year: Int, month: Int, day: Int) -> HijriDate? { date(in: diyanet, jdn(year, month, day)) }

    /// Diyanet'e göre verilmiş bir günün (dini günler, Ramazan) seçili takvimdeki karşılığı: aynı hicri gün. Varsayılanda
    /// ya da tablo dışında aynı gün.
    static func shift(year: Int, month: Int, day: Int) -> (Int, Int, Int) {
        guard !isDefault, let h = date(in: diyanet, jdn(year, month, day)), let m = start(in: table, year: h.year, month: h.month) else {
            return (year, month, day)
        }
        return fromJdn(m.start + min(h.day, m.length) - 1)
    }

    /// Gregoryen tarihin Jülyen gün numarası (gün farkı için; Android'de LocalDate.toEpochDay)
    static func jdn(_ y: Int, _ m: Int, _ d: Int) -> Int {
        let a = (14 - m) / 12, yy = y + 4800 - a, mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    /// Takvim günü (yıl, ay, gün) için hicri tarih; tablo dışında nil
    static func date(year: Int, month: Int, day: Int) -> HijriDate? { date(in: table, jdn(year, month, day)) }

    /// Hicri ayın ilk günü (Gregoryen yıl, ay, gün) ve uzunluğu; tablo dışında nil (takvimi hicri aylarla gezmek için; 2. analiz #119)
    static func month(year: Int, month: Int) -> (first: (Int, Int, Int), length: Int)? {
        start(in: table, year: year, month: month).map { (fromJdn($0.start), $0.length) }
    }

    /// Jülyen gün numarasından Gregoryen (yıl, ay, gün)
    static func fromJdn(_ n: Int) -> (Int, Int, Int) {
        let a = n + 32044, b = (4 * a + 3) / 146097, c = a - 146097 * b / 4
        let d = (4 * c + 3) / 1461, e = c - 1461 * d / 4, m = (5 * e + 2) / 153
        return (100 * b + d - 4800 + m / 10, m + 3 - 12 * (m / 10), e - (153 * m + 2) / 5 + 1)
    }

    private static let utc = TimeZone(identifier: "UTC")!
    private static var ummAlQura: Calendar { var c = Calendar(identifier: .islamicUmmAlQura); c.timeZone = utc; return c }
    private static var gregorian: Calendar { var c = Calendar(identifier: .gregorian); c.timeZone = utc; return c }

    /// Hicri çevirici (rakip analizi #13): tabloda seçili takvim, dışında Umm al-Qura (`outside`)
    static func convert(year: Int, month: Int, day: Int) -> (date: HijriDate, outside: Bool)? {
        if let h = date(year: year, month: month, day: day) { return (h, false) }
        guard let g = gregorian.date(from: DateComponents(year: year, month: month, day: day, hour: 12)) else { return nil }
        let c = ummAlQura.dateComponents([.year, .month, .day], from: g)
        guard let y = c.year, let m = c.month, let d = c.day else { return nil }
        return (HijriDate(year: y, month: m, day: d), true)
    }

    enum Reverse { case date(year: Int, month: Int, day: Int, outside: Bool), tooLong(Int), none }

    /// Hicri (yıl, ay, gün) → Gregoryen; ay o kadar gün çekmiyorsa ayın gün sayısı
    static func gregorian(year: Int, month: Int, day: Int) -> Reverse {
        if let m = start(in: table, year: year, month: month) {
            if day > m.length { return .tooLong(m.length) }
            let g = fromJdn(m.start + day - 1)
            return .date(year: g.0, month: g.1, day: g.2, outside: false)
        }
        let uq = ummAlQura
        guard let first = uq.date(from: DateComponents(year: year, month: month, day: 1, hour: 12)) else { return .none }
        let len = uq.range(of: .day, in: .month, for: first)?.count ?? 30
        if day > len { return .tooLong(len) }
        let at = first.addingTimeInterval(Double(day - 1) * 86400)
        let c = gregorian.dateComponents([.year, .month, .day], from: at)
        guard let y = c.year, let m = c.month, let d = c.day else { return .none }
        return .date(year: y, month: m, day: d, outside: true)
    }

    /// Cihazın saat dilimindeki takvim günü için hicri tarih
    static func date(_ d: Date, timeZone: TimeZone = .current) -> HijriDate? {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: d)
        return date(year: c.year!, month: c.month!, day: c.day!)
    }
}
