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

    /// Cihazın saat dilimindeki takvim günü için hicri tarih
    static func date(_ d: Date, timeZone: TimeZone = .current) -> HijriDate? {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: d)
        return date(year: c.year!, month: c.month!, day: c.day!)
    }
}
