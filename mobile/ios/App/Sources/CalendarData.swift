import Foundation
import SwiftUI
import SaatCore

/// Türkiye takvimi (calendar.json; sitedeki takvimle aynı kaynak, gen-strings.mjs üretir)
final class CalendarData {
    static let shared = CalendarData()

    struct Event: Decodable, Hashable {
        let date: String
        let key: String
        let type: String
        /// Dünya tatillerinde (2. analiz #122) ad hazır gelir
        var label: String?

        var day: CalendarDay? { CalendarDay(iso: date) }
        var name: String { label ?? L("calendar.events.\(key)") }
        var color: Color { CalendarData.color(type) }
    }

    struct Span: Decodable, Hashable {
        let from: String
        let to: String?
    }

    struct Year: Decodable {
        let events: [Event]
        let schoolRanges: [Span]
        let projected: Bool
    }

    struct Ramadan: Hashable {
        let from: CalendarDay
        let to: CalendarDay
        var days: [CalendarDay] { (0...DateCalc.days(from: from, to: to)).map { from.adding(days: $0) } }
    }

    private struct File: Decodable {
        let years: [String: Year]
        let ramadans: [Span]
    }

    /// calendar.json (Diyanet'e göre); dini günler ve Ramazan seçili hicri takvime göre kaydırılmış hâlleriyle okunur
    let years: [Int: Year]
    private let baseRamadans: [Ramadan]
    private let lock = NSLock()
    private var cacheKey = ""
    private var cacheByDate: [String: [Event]] = [:]
    private var cacheByYear: [Int: [Event]] = [:]
    private var cacheRamadans: [Ramadan] = []

    private init() {
        let file = Bundle.main.url(forResource: "calendar", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? JSONDecoder().decode(File.self, from: $0) }
        years = Dictionary(uniqueKeysWithValues: (file?.years ?? [:]).compactMap { k, v in Int(k).map { ($0, v) } })
        baseRamadans = (file?.ramadans ?? []).compactMap { r in
            guard let from = CalendarDay(iso: r.from) else { return nil }
            // Bitişi bilinmeyen Ramazan 30 gün sayılır
            return Ramadan(from: from, to: r.to.flatMap { CalendarDay(iso: $0) } ?? from.adding(days: 29))
        }
    }

    /// Diyanet'e göre bir günün seçili hicri takvimdeki karşılığı (Endonezya takvimi ya da ±1 gün; 10 Kasım 2026)
    static func shift(_ d: CalendarDay) -> CalendarDay {
        let s = Hijri.shift(year: d.year, month: d.month, day: d.day)
        return CalendarDay(s.0, s.1, s.2)
    }

    /// Dini günün seçili takvimdeki günü (Regaib: Recep'in ilk cumasından önceki perşembe; arife: bayramdan önceki gün)
    static func shiftReligious(_ e: Event) -> Event {
        guard !Hijri.isDefault, let d = e.day else { return e }
        let n: CalendarDay
        switch e.key {
        case "regaib":
            let fri = d.adding(days: 1)
            if let h = Hijri.baseDate(year: fri.year, month: fri.month, day: fri.day), let m = Hijri.month(year: h.year, month: 7) {
                let first = CalendarDay(m.first.0, m.first.1, m.first.2)
                n = first.adding(days: (4 - first.weekday + 7) % 7 - 1)
            } else { n = d }
        case "rbEve", "kbEve": n = shift(d.adding(days: 1)).adding(days: -1)
        default: n = shift(d)
        }
        return n == d ? e : Event(date: n.iso, key: e.key, type: e.type, label: e.label)
    }

    private func refresh() {
        lock.lock(); defer { lock.unlock() }
        let k = Hijri.key
        guard k != cacheKey else { return }
        var byYear: [Int: [Event]] = [:]
        for y in years.keys.sorted() {
            for e in years[y]!.events {
                let s = e.type == "religious" ? Self.shiftReligious(e) : e
                byYear[s.day?.year ?? y, default: []].append(s)
            }
        }
        cacheByYear = byYear
        cacheByDate = Dictionary(grouping: byYear.keys.sorted().flatMap { byYear[$0]! }, by: \.date)
        cacheRamadans = Hijri.isDefault ? baseRamadans : baseRamadans.map { r in
            Ramadan(from: Self.shift(r.from), to: Self.shift(r.to.adding(days: 1)).adding(days: -1))
        }
        cacheKey = k
    }

    /// Ramazanlar (seçili hicri takvime göre)
    var ramadans: [Ramadan] { refresh(); return lock.withLock { cacheRamadans } }
    /// Bütün olaylar yıl sırasıyla (dini günler seçili hicri takvime göre)
    var allEvents: [Event] { refresh(); return lock.withLock { cacheByYear.keys.sorted().flatMap { cacheByYear[$0]! } } }

    var yearRange: ClosedRange<Int> { (years.keys.min() ?? 2026)...(years.keys.max() ?? 2026) }

    func events(on day: CalendarDay) -> [Event] { refresh(); return lock.withLock { cacheByDate[day.iso] ?? [] } }

    func events(year: Int, month: Int) -> [Event] {
        refresh()
        return lock.withLock { (cacheByYear[year] ?? []).filter { $0.day?.month == month } }
    }

    func isRamadan(_ day: CalendarDay) -> Bool { ramadans.contains { $0.from <= day && day <= $0.to } }

    func isSchoolBreak(_ day: CalendarDay) -> Bool {
        (years[day.year]?.schoolRanges ?? []).contains { s in
            guard let a = CalendarDay(iso: s.from) else { return false }
            let b = s.to.flatMap { CalendarDay(iso: $0) } ?? a
            return a <= day && day <= b
        }
    }

    /// Şu an süren ya da sıradaki Ramazan
    func nextRamadan(from today: CalendarDay) -> Ramadan? { ramadans.first { $0.to >= today } }

    /// İş günü hesabı için tatiller: tam gün 0, arife 0,5
    lazy var offDays: [String: Double] = {
        var out: [String: Double] = [:]
        for e in years.values.flatMap(\.events) {
            if e.type == "holiday" { out[e.date] = 0 } else if e.type == "half", out[e.date] == nil { out[e.date] = 0.5 }
        }
        return out
    }()

    /// Bugünden sonraki günler (bugün dahil), en çok `limit`
    func upcoming(from today: CalendarDay, limit: Int) -> [Event] {
        allEvents
            .filter { ($0.day ?? today) >= today && $0.type != "school" }
            .prefix(limit).map { $0 }
    }

    static func color(_ type: String) -> Color {
        // Kapsayıcı takvimlerde (`cal:<id>`) takvimin rengi
        if type.hasPrefix("cal:") { return ExtraCals.color(String(type.dropFirst(4))) }
        switch type {
        case "holiday": return Color(red: 0.80, green: 0.20, blue: 0.18)
        case "half", "leave": return Color(red: 0.90, green: 0.50, blue: 0.10)
        case "religious": return Color(red: 0.13, green: 0.55, blue: 0.35)
        case "school": return Color(red: 0.45, green: 0.33, blue: 0.75)
        default: return Color(red: 0.20, green: 0.45, blue: 0.80)
        }
    }
}
