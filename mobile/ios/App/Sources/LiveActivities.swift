import ActivityKit
import Foundation
import SaatCore
import SwiftUI
import UIKit
import WeatherKit
import WidgetKit

/// Live Activity kurulumu: kayıtlı durumdan (zamanlayıcılar, pomodoro, kronometre, "kilit ekranında sıradaki vakit",
/// gün doğumu/batımı, hava durumu)
/// istenen etkinlikler çıkarılır, telefondakilerle eşitlenir (yeni başlar, değişen güncellenir, bitenler kapanır).
/// Uygulama öne geldiğinde, arka plan yenilemesinde ve araçlarda durum değişince çağrılır. Etkinlik ancak uygulama
/// öndeyken başlatılabilir; sıradaki vakit uygulama açıldıkça ve arka plan yenilemesinde ileri taşınır.
@MainActor
enum LiveActivities {
    static let prayerKey = "live.prayer"
    /// Güneş sayacı (2. analiz #150): sıradaki gün doğumu ya da batımına kalan süre
    static let sunKey = "live.sun"

    private typealias Want = (attributes: SaatActivityAttributes, state: SaatActivityAttributes.ContentState, stale: Date?)

    static func sync(now: Date = Date()) {
        // Dynamic Island'daki canlı saat kaldırıldı (4 Kasım 2026, sahibin isteği: saat telefonda zaten var); açık olan
        // eski etkinlik aşağıda istenmediği için kapanır
        UserDefaults.standard.removeObject(forKey: "live.clock")
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        var wanted: [String: Want] = [:]
        for w in timers(now) + [stopwatch(), pomodoro(now), prayer(now), weather(now), sun(now)].compactMap({ $0 }) { wanted[w.attributes.id] = w }
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

    /// Hava durumu (LiveWeather): son özet; 12 saatten eskiyse gösterilmez (uygulama açılınca yenilenir)
    private static func weather(_ now: Date) -> Want? {
        guard LiveWeather.isOn, let s = LiveWeather.snapshot(), now.timeIntervalSince(s.at) < 12 * 3600 else { return nil }
        return (LiveWeather.attributes, LiveWeather.state(s), LiveWeather.staleDate(s))
    }

    /// Hava etkinliği açıksa seçili yerin güncel havası WeatherKit'ten alınır (WeatherCache: aynı yer 15 dk), özet
    /// uygulamanın dilinde yazılır ve etkinlik eşitlenir. Uygulama öne gelince, arka plan yenilemesinde ve aç/kapa
    /// düğmelerinde çağrılır.
    static func refreshWeather() async {
        guard LiveWeather.isOn else { sync(); return }
        let place = AppPlace.stored()
        if let w = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) {
            let c = w.currentWeather
            var detail = place.name
            if let today = w.dailyForecast.first {
                detail += " · ↑\(Units.temp(today.highTemperature)) ↓\(Units.temp(today.lowTemperature))"
            }
            LiveWeather.save(.init(value: Units.temp(c.temperature),
                                   symbol: c.symbolName,
                                   title: c.condition.description, detail: detail, at: Date()))
        }
        sync()
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

    /// Sıradaki gün doğumu ya da batımı (bugün ve yarın; vakit düzeltmesi olmadan)
    private static func sun(_ now: Date) -> Want? {
        guard UserDefaults.standard.bool(forKey: sunKey) else { return nil }
        let place = AppPlace.stored()
        let days = [now, now.addingTimeInterval(86_400)].compactMap { SeasonsView.sun(place: place, now: $0) }
        let events = days.flatMap { [($0.rise, true), ($0.set, false)] }.filter { $0.0 > now }.sorted { $0.0 < $1.0 }
        guard let next = events.first else { return nil }
        let prev = days.flatMap { [$0.rise, $0.set] }.filter { $0 <= now }.max() ?? now.addingTimeInterval(-3600)
        return (SaatActivityAttributes(kind: "sun", id: "sun"),
                .init(date: next.0, start: prev, title: L(next.1 ? "nature.sunrise" : "nature.sunset") + " " + place.hm(next.0), subtitle: place.name),
                next.0.addingTimeInterval(60))
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
        // Sıradaki 3 vakit (2. analiz #86): ilki başlıkta ve sayaçta, sonraki ikisi alt satırda
        let tomorrow = now.addingTimeInterval(86_400)
        let after = (place.schedule.entries(on: now) + place.schedule.entries(on: tomorrow)).filter { $0.at > next.at }.prefix(2)
        let more = after.map { "\($0.prayer.localizedName) \(place.hm($0.at))" }
        return (SaatActivityAttributes(kind: "prayer", id: "prayer"),
                .init(date: next.at, start: prev, title: "\(name) \(place.hm(next.at))", subtitle: ([place.name] + more).joined(separator: " · ")),
                next.at.addingTimeInterval(60))
    }
}

/// Kilit ekranı / Dynamic Island hava etkinliğinin aç/kapa anahtarı (Hava sekmesinde ve Saat görünümünde; ayrıca
/// etkinliğin üstündeki × ve Denetim Merkezi düğmesi). Açınca güncel hava alınıp etkinlik başlar.
struct LiveWeatherToggle: View {
    var showNote = true
    @State private var on = LiveWeather.isOn

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Toggle(isOn: $on) {
                Label(L("live.weather.toggle"), systemImage: "lock.iphone")
            }
            .tint(Theme.accent)
            if showNote {
                Text(L("live.weather.note")).font(.footnote).foregroundStyle(Theme.muted)
            }
        }
        .onAppear { on = LiveWeather.isOn }
        .onChange(of: on) { _, value in
            guard value != LiveWeather.isOn else { return }
            Task {
                await LiveWeather.apply(value)
                if #available(iOS 18.0, *) { ControlCenter.shared.reloadControls(ofKind: LiveWeather.controlKind) }
            }
        }
    }
}
