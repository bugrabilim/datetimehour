import ActivityKit
import Foundation
import SaatCore

/// Live Activity kurulumu: kayıtlı durumdan (zamanlayıcılar, pomodoro, kronometre, "kilit ekranında sıradaki vakit")
/// istenen etkinlikler çıkarılır, telefondakilerle eşitlenir (yeni başlar, değişen güncellenir, bitenler kapanır).
/// Uygulama öne geldiğinde, arka plan yenilemesinde ve araçlarda durum değişince çağrılır. Etkinlik ancak uygulama
/// öndeyken başlatılabilir; sıradaki vakit uygulama açıldıkça ve arka plan yenilemesinde ileri taşınır.
@MainActor
enum LiveActivities {
    static let prayerKey = "live.prayer"

    private typealias Want = (attributes: SaatActivityAttributes, state: SaatActivityAttributes.ContentState, stale: Date?)

    static func sync(now: Date = Date()) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        var wanted: [String: Want] = [:]
        for w in timers(now) + [stopwatch(), pomodoro(now), prayer(now)].compactMap({ $0 }) { wanted[w.attributes.id] = w }
        for a in Activity<SaatActivityAttributes>.activities where a.activityState == .active || a.activityState == .stale {
            if let w = wanted.removeValue(forKey: a.attributes.id), w.attributes.kind == a.attributes.kind {
                if a.content.state != w.state || a.activityState == .stale {
                    Task { await a.update(ActivityContent(state: w.state, staleDate: w.stale)) }
                }
            } else {
                Task { await a.end(nil, dismissalPolicy: .immediate) }
            }
        }
        for w in wanted.values {
            _ = try? Activity.request(attributes: w.attributes, content: ActivityContent(state: w.state, staleDate: w.stale), pushType: nil)
        }
    }

    /// Süren zamanlayıcılar (duraklatılan ve biten kapanır)
    private static func timers(_ now: Date) -> [Want] {
        let ms = Int64(now.timeIntervalSince1970 * 1000)
        return TimerItem.load().filter { $0.running && $0.end > ms }.map { t in
            let end = Date(timeIntervalSince1970: Double(t.end) / 1000)
            let name = t.name.trimmingCharacters(in: .whitespaces)
            return (SaatActivityAttributes(kind: "timer", id: t.id),
                    .init(date: end, start: end.addingTimeInterval(-Double(t.total) / 1000),
                          title: name.isEmpty ? L("tools.timer") : name, subtitle: Fmt.hm(seconds: Int(t.total / 1000))),
                    end)
        }
    }

    /// Çalışan kronometre (duraklatılınca kapanır)
    private static func stopwatch() -> Want? {
        let d = UserDefaults.standard
        let start = d.double(forKey: "sw.start"), acc = d.double(forKey: "sw.acc")
        guard start > 0 else { return nil }
        let from = Date(timeIntervalSinceReferenceDate: start - acc)
        return (SaatActivityAttributes(kind: "stopwatch", id: "stopwatch"),
                .init(date: from, start: from, title: L("tools.hub.cards.stopwatch.name"), subtitle: ""), nil)
    }

    /// Çalışan pomodoro aşaması
    private static func pomodoro(_ now: Date) -> Want? {
        let d = UserDefaults.standard
        let end = d.double(forKey: "pomo.end")
        guard end > now.timeIntervalSinceReferenceDate else { return nil }
        let phase = d.string(forKey: "pomo.phase") ?? "work"
        func val(_ k: String, _ def: Int) -> Int { d.object(forKey: k) == nil ? def : d.integer(forKey: k) }
        let minutes = phase == "work" ? val("pomo.work", 25) : phase == "short" ? val("pomo.short", 5) : val("pomo.long", 15)
        let at = Date(timeIntervalSinceReferenceDate: end)
        return (SaatActivityAttributes(kind: "pomodoro", id: "pomodoro"),
                .init(date: at, start: at.addingTimeInterval(-Double(minutes * 60)), title: L("tools.pomodoro.\(phase)"),
                      subtitle: T("tools.pomodoro.round", val("pomo.round", 1), val("pomo.rounds", 4))),
                at)
    }

    /// Sıradaki vakit (Ayarlar: Vakitler › "Kilit ekranında sıradaki vakit"); Ramazan'da akşam iftar, imsak sahur
    private static func prayer(_ now: Date) -> Want? {
        guard UserDefaults.standard.bool(forKey: prayerKey) else { return nil }
        let place = AppPlace.stored()
        guard let next = place.schedule.next(after: now) else { return nil }
        let yesterday = now.addingTimeInterval(-86_400)
        let prev = (place.schedule.entries(on: yesterday) + place.schedule.entries(on: now)).map(\.at).filter { $0 <= now }.max()
            ?? now.addingTimeInterval(-3600)
        let ramadan = CalendarData.shared.isRamadan(CalendarDay(next.at, timeZone: place.zone))
        let name = ramadan && next.prayer == .maghrib ? L("live.iftar")
            : ramadan && next.prayer == .fajr ? L("live.sahur") : next.prayer.localizedName
        return (SaatActivityAttributes(kind: "prayer", id: "prayer"),
                .init(date: next.at, start: prev, title: "\(name) \(place.hm(next.at))", subtitle: place.name),
                next.at.addingTimeInterval(60))
    }
}
