import Foundation

/// Hicri tarih (rakip analizi #9). Ay başları Diyanet'e hizalı tablodan (hijri.json; sitedeki lib/hijri.mjs üretir:
/// Diyanet vakit tablolarındaki hicri tarihler ve dini günler, dayanağı olmayan aylarda Umm al-Qura). Platformun
/// Umm al-Qura takvimi bazı aylarda Diyanet'ten bir gün ayrıldığı için kullanılmaz. Gün gece yarısı değişir.
/// Tablonun dışında (şu an 21 Aralık 2025 – 13 Ocak 2032) tarih yoktur.
struct HijriDate: Equatable {
    let year: Int
    let month: Int
    let day: Int

    var monthName: String { L("hijri.month.\(month)") }
    /// "19 Cemaziyelevvel 1448"
    var text: String { String(format: L("hijri.fmt"), "\(day)", monthName, "\(year)") }
}

enum Hijri {
    /// [(ay başının gün numarası, hicri yıl, ay)]; son satır tablonun sonu
    private static let table: [(start: Int, year: Int, month: Int)] = {
        guard let url = Bundle.main.url(forResource: "hijri", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let rows = try? JSONSerialization.jsonObject(with: data) as? [[Any]] else { return [] }
        return rows.compactMap { r in
            guard r.count == 3, let iso = r[0] as? String, let y = r[1] as? Int, let m = r[2] as? Int else { return nil }
            let p = iso.split(separator: "-").compactMap { Int($0) }
            guard p.count == 3 else { return nil }
            return (jdn(p[0], p[1], p[2]), y, m)
        }
    }()

    /// Gregoryen tarihin Jülyen gün numarası (gün farkı için; Android'de LocalDate.toEpochDay)
    static func jdn(_ y: Int, _ m: Int, _ d: Int) -> Int {
        let a = (14 - m) / 12, yy = y + 4800 - a, mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    /// Takvim günü (yıl, ay, gün) için hicri tarih; tablo dışında nil
    static func date(year: Int, month: Int, day: Int) -> HijriDate? {
        let n = jdn(year, month, day)
        guard table.count >= 2 else { return nil }
        for i in stride(from: table.count - 2, through: 0, by: -1) where n >= table[i].start {
            return n < table[i + 1].start ? HijriDate(year: table[i].year, month: table[i].month, day: n - table[i].start + 1) : nil
        }
        return nil
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

    /// Hicri çevirici (rakip analizi #13): tabloda Diyanet, dışında Umm al-Qura (`outside`)
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
        if table.count >= 2 {
            for i in 0..<(table.count - 1) where table[i].year == year && table[i].month == month {
                let len = table[i + 1].start - table[i].start
                if day > len { return .tooLong(len) }
                let g = fromJdn(table[i].start + day - 1)
                return .date(year: g.0, month: g.1, day: g.2, outside: false)
            }
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
