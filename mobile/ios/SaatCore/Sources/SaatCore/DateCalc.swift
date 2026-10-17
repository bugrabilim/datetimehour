import Foundation

/// Takvim günü (saat dilimi yok). Tarih hesaplama aracı, takvim ve iş günü sayımı bunu kullanır;
/// sitedeki `src/js/dates.js` ile aynı kurallar.
public struct CalendarDay: Hashable, Comparable, Sendable, CustomStringConvertible {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(_ year: Int, _ month: Int, _ day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    /// "2026-10-17"
    public init?(iso: String) {
        let p = iso.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3, (1...12).contains(p[1]), (1...CalendarDay.daysIn(p[0], p[1])).contains(p[2]) else { return nil }
        self.init(p[0], p[1], p[2])
    }

    /// Verilen saat dilimindeki gün
    public init(_ date: Date, timeZone: TimeZone = .current) {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: date)
        self.init(c.year!, c.month!, c.day!)
    }

    public var iso: String { String(format: "%04d-%02d-%02d", year, month, day) }
    public var description: String { iso }

    public static func daysIn(_ y: Int, _ m: Int) -> Int {
        switch m {
        case 2: return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 ? 29 : 28
        case 4, 6, 9, 11: return 30
        default: return 31
        }
    }

    /// 1970-01-01'den bu yana gün (proleptik Gregoryen)
    public var dayNumber: Int {
        let a = (14 - month) / 12, y = year + 4800 - a, m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045 - 2440588
    }

    public init(dayNumber n: Int) {
        let a = n + 2440588 + 32044, b = (4 * a + 3) / 146097, c = a - 146097 * b / 4
        let d = (4 * c + 3) / 1461, e = c - 1461 * d / 4, m = (5 * e + 2) / 153
        self.init(100 * b + d - 4800 + m / 10, m + 3 - 12 * (m / 10), e - (153 * m + 2) / 5 + 1)
    }

    /// Pazartesi = 0 … Pazar = 6
    public var weekday: Int { ((dayNumber + 3) % 7 + 7) % 7 }
    public var isWeekend: Bool { weekday >= 5 }

    public func adding(days n: Int) -> CalendarDay { CalendarDay(dayNumber: dayNumber + n) }

    /// Ay ekler; hedef ayda o gün yoksa ayın son günü
    public func adding(months n: Int) -> CalendarDay {
        let t = year * 12 + (month - 1) + n
        let y = Int((Double(t) / 12).rounded(.down)), m = ((t % 12) + 12) % 12 + 1
        return CalendarDay(y, m, min(day, CalendarDay.daysIn(y, m)))
    }

    /// Yılın kaçıncı günü (1 Ocak = 1)
    public var dayOfYear: Int { dayNumber - CalendarDay(year, 1, 1).dayNumber + 1 }

    /// ISO 8601 hafta numarası ve hafta yılı
    public var isoWeek: (week: Int, year: Int) {
        let thursday = adding(days: 3 - weekday)
        return ((thursday.dayOfYear - 1) / 7 + 1, thursday.year)
    }

    public static func < (a: CalendarDay, b: CalendarDay) -> Bool { a.dayNumber < b.dayNumber }
}

public enum DateCalc {
    /// b − a gün
    public static func days(from a: CalendarDay, to b: CalendarDay) -> Int { b.dayNumber - a.dayNumber }

    /// a'dan b'ye tamamlanan yıl, ay, gün (a ≤ b): önce a'ya sığan en çok ay eklenir (ay sonu kısaltılır), kalan gündür.
    /// Örnek: 31 Ocak → 1 Mart = 1 ay 1 gün (31 Ocak + 1 ay = 28 Şubat).
    public static func ymd(from a: CalendarDay, to b: CalendarDay) -> (years: Int, months: Int, days: Int) {
        var total = (b.year - a.year) * 12 + (b.month - a.month)
        if a.adding(months: total) > b { total -= 1 }
        let d = days(from: a.adding(months: total), to: b)
        return (total / 12, total % 12, d)
    }

    /// İş günü ağırlığı: hafta sonu 0, resmî tatil 0, arife (yarım gün) 0,5, diğer 1.
    /// `offDays`: ISO tarih → ağırlık (tatil 0, yarım gün 0,5)
    public static func workWeight(_ day: CalendarDay, offDays: [String: Double]) -> Double {
        day.isWeekend ? 0 : (offDays[day.iso] ?? 1)
    }

    /// [a, b) arasındaki iş günü (başlangıç dahil, bitiş hariç)
    public static func workdays(from a: CalendarDay, to b: CalendarDay, offDays: [String: Double]) -> Double {
        var w = 0.0
        var n = a.dayNumber
        while n < b.dayNumber {
            w += workWeight(CalendarDay(dayNumber: n), offDays: offDays)
            n += 1
        }
        return w
    }

    /// Tarihe n iş günü ekler (negatifse çıkarır); ağırlığı sıfır olmayan her gün bir sayılır
    public static func adding(workdays n: Int, to a: CalendarDay, offDays: [String: Double]) -> CalendarDay {
        var cur = a, left = abs(n)
        let step = n < 0 ? -1 : 1
        while left > 0 {
            cur = cur.adding(days: step)
            if workWeight(cur, offDays: offDays) > 0 { left -= 1 }
        }
        return cur
    }

    /// Sonraki doğum günü (bugün doğum günüyse bugün); 29 Şubat artık olmayan yılda 28 Şubat
    public static func nextBirthday(_ birth: CalendarDay, today: CalendarDay) -> CalendarDay {
        let thisYear = CalendarDay(today.year, birth.month, min(birth.day, CalendarDay.daysIn(today.year, birth.month)))
        if thisYear >= today { return thisYear }
        return CalendarDay(today.year + 1, birth.month, min(birth.day, CalendarDay.daysIn(today.year + 1, birth.month)))
    }
}

public extension CalendarDay {
    /// Bu günün verilen saat dilimindeki öğlesi (gün hesabında saat dilimi kayması olmasın diye)
    func noon(in timeZone: TimeZone) -> Date {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        return cal.date(from: DateComponents(year: year, month: month, day: day, hour: 12)) ?? Date()
    }

    /// Ayın ilk günü
    var firstOfMonth: CalendarDay { CalendarDay(year, month, 1) }
}
