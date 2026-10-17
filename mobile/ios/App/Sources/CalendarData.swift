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

        var day: CalendarDay? { CalendarDay(iso: date) }
        var name: String { L("calendar.events.\(key)") }
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

    let years: [Int: Year]
    let ramadans: [Ramadan]
    private let byDate: [String: [Event]]

    private init() {
        let file = Bundle.main.url(forResource: "calendar", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? JSONDecoder().decode(File.self, from: $0) }
        years = Dictionary(uniqueKeysWithValues: (file?.years ?? [:]).compactMap { k, v in Int(k).map { ($0, v) } })
        ramadans = (file?.ramadans ?? []).compactMap { r in
            guard let from = CalendarDay(iso: r.from) else { return nil }
            // Bitişi bilinmeyen Ramazan 30 gün sayılır
            return Ramadan(from: from, to: r.to.flatMap { CalendarDay(iso: $0) } ?? from.adding(days: 29))
        }
        byDate = Dictionary(grouping: years.values.flatMap(\.events), by: \.date)
    }

    var yearRange: ClosedRange<Int> { (years.keys.min() ?? 2026)...(years.keys.max() ?? 2026) }

    func events(on day: CalendarDay) -> [Event] { byDate[day.iso] ?? [] }

    func events(year: Int, month: Int) -> [Event] {
        (years[year]?.events ?? []).filter { $0.day?.month == month }
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
        years.keys.sorted().flatMap { years[$0]!.events }
            .filter { ($0.day ?? today) >= today && $0.type != "school" }
            .prefix(limit).map { $0 }
    }

    static func color(_ type: String) -> Color {
        switch type {
        case "holiday": return Color(red: 0.80, green: 0.20, blue: 0.18)
        case "half": return Color(red: 0.90, green: 0.50, blue: 0.10)
        case "religious": return Color(red: 0.13, green: 0.55, blue: 0.35)
        case "school": return Color(red: 0.45, green: 0.33, blue: 0.75)
        default: return Color(red: 0.20, green: 0.45, blue: 0.80)
        }
    }
}
