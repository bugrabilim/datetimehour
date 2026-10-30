import Foundation
import BackgroundTasks
import UserNotifications
import WeatherKit
import SaatCore

/// Özellik bildirimleri (30 Ekim 2026, sahibin isteği): her özelliğin bildirimi varsayılan açık, kullanıcı
/// Ayarlar › Bildirimler'den istemediğini kapatır. Namaz vakitleri ayrıca (NotificationPrefs). Bildirimler telefonda
/// kurulur; yalnız hava özeti için hava servisine (WeatherKit) sorulur. iOS en çok 64 bekleyen bildirim tuttuğu için
/// liste PrayerNotifier'da vakitlerle birlikte zamana göre sıralanıp kesilir; arka plan yenilemesi listeyi ileri taşır.
enum NotifyFeature: String, CaseIterable, Identifiable {
    case calendar, seasons, moon, sun, weather, hourly, dst

    var id: String { rawValue }
    var key: String { "notify.\(rawValue)" }
    var title: String { L("notify.cat.\(rawValue)") }
    var note: String { L("notify.cat.\(rawValue).note") }

    var icon: String {
        switch self {
        case .calendar: "calendar"
        case .seasons: "leaf"
        case .moon: "moon.circle"
        case .sun: "sunrise"
        case .weather: "cloud.sun"
        case .hourly: "clock"
        case .dst: "clock.arrow.2.circlepath"
        }
    }

    /// Kullanıcı kapatmadıkça açık
    var isOn: Bool { UserDefaults.standard.object(forKey: key) as? Bool ?? true }

    /// Hava özetinin saati (gece yarısından dakika; varsayılan 07.30)
    static let weatherTimeKey = "notify.weatherTime"
    /// Saat başı bildiriminin ilk ve son saati (varsayılan 09–21)
    static let hourlyFromKey = "notify.hourlyFrom"
    static let hourlyToKey = "notify.hourlyTo"
    static var weatherMinutes: Int { UserDefaults.standard.object(forKey: weatherTimeKey) as? Int ?? 450 }
    static var hourlyFrom: Int { UserDefaults.standard.object(forKey: hourlyFromKey) as? Int ?? 9 }
    static var hourlyTo: Int { UserDefaults.standard.object(forKey: hourlyToKey) as? Int ?? 21 }
}

/// Kurulacak tek seferlik bildirim (vakitler ve özellikler aynı listede zamana göre sıralanır)
struct PlannedNotification {
    let id: String
    let fireAt: Date
    let title: String
    let body: String
    var thread = "feature"
    var sound: UNNotificationSound? = .default
}

@MainActor
enum FeatureNotifier {
    static let featurePrefix = "feat-"
    static let hourlyPrefix = "hourly-"

    /// `now` sonrasındaki özellik bildirimleri (`days` gün; mevsim ve Ay olayları daha ileriye bakar)
    static func planned(place: AppPlace, now: Date, days: Int = 14) async -> [PlannedNotification] {
        var out: [PlannedNotification] = []
        if NotifyFeature.calendar.isOn { out += calendar(now: now, days: days) }
        if NotifyFeature.seasons.isOn { out += seasons(place: place, now: now) }
        if NotifyFeature.moon.isOn { out += moon(place: place, now: now) }
        if NotifyFeature.sun.isOn { out += sun(place: place, now: now, days: days) }
        if NotifyFeature.dst.isOn { out += dst(now: now) }
        if NotifyFeature.weather.isOn { out += await weather(place: place, now: now) }
        return out.filter { $0.fireAt > now }
    }

    /// Özel günler: o gün sabah 09.00'da (telefonun saatiyle), günün bütün olayları tek bildirimde
    private static func calendar(now: Date, days: Int) -> [PlannedNotification] {
        let today = CalendarDay(now)
        return (0..<days).compactMap { i in
            let day = today.adding(days: i)
            var seen = Set<String>()
            let names = CalendarData.shared.events(on: day).map(\.name).filter { seen.insert($0).inserted }
            guard !names.isEmpty else { return nil }
            let fire = day.noon(in: .current).addingTimeInterval(-3 * 3600)
            return PlannedNotification(id: "\(featurePrefix)cal-\(day.iso)", fireAt: fire,
                                       title: names.joined(separator: " · "), body: T("notify.n.today", Fmt.day(day)))
        }
    }

    /// Ekinoks ve gündönümü anında; güney yarımkürede mevsim adları yer değiştirir
    private static func seasons(place: AppPlace, now: Date) -> [PlannedNotification] {
        let names = place.lat < 0 ? ["autumn", "winter", "spring", "summer"] : ["spring", "summer", "autumn", "winter"]
        let year = Calendar(identifier: .gregorian).component(.year, from: now)
        return [year, year + 1].flatMap(SeasonsView.instants).compactMap { index, at in
            guard at > now, at.timeIntervalSince(now) < 400 * 86400 else { return nil }
            return PlannedNotification(id: "\(featurePrefix)season-\(Int(at.timeIntervalSince1970))", fireAt: at,
                                       title: T("notify.n.season", L("nature.seasons.\(names[index])")),
                                       body: when(at, place))
        }
    }

    /// Dolunay ve yeni ay anında (yaklaşık iki ay ileri)
    private static func moon(place: AppPlace, now: Date) -> [PlannedNotification] {
        var out: [PlannedNotification] = []
        var t = now
        for _ in 0..<3 {
            let m = Nature.moon(at: t)
            for (ms, key) in [(m.nextNewMoon, "new"), (m.nextFullMoon, "full")] {
                let at = Date(unixMilliseconds: ms)
                let id = "\(featurePrefix)moon-\(key)-\(ms / 60000)"
                guard !out.contains(where: { $0.id == id }) else { continue }
                out.append(PlannedNotification(id: id, fireAt: at, title: L("nature.phases.\(key)"), body: when(at, place)))
            }
            t = Date(unixMilliseconds: min(m.nextNewMoon, m.nextFullMoon)).addingTimeInterval(3600)
        }
        return out
    }

    /// Her gün Güneş doğarken ve batarken (vakit düzeltmeleri olmadan; Mevsimler'deki saatler)
    private static func sun(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        (0..<days).flatMap { i -> [PlannedNotification] in
            let date = now.addingTimeInterval(Double(i) * 86400)
            guard let s = SeasonsView.sun(place: place, now: date) else { return [] }
            return [
                PlannedNotification(id: "\(featurePrefix)sunrise-\(Int(s.rise.timeIntervalSince1970) / 60)", fireAt: s.rise,
                                    title: L("nature.sunrise"), body: "\(place.name) · \(place.hm(s.rise))"),
                PlannedNotification(id: "\(featurePrefix)sunset-\(Int(s.set.timeIntervalSince1970) / 60)", fireAt: s.set,
                                    title: L("nature.sunset"), body: "\(place.name) · \(place.hm(s.set))"),
            ]
        }
    }

    /// Telefonun saat diliminde saatler değişmeden önceki akşam 20.00'de (60 gün içindeyse)
    private static func dst(now: Date) -> [PlannedNotification] {
        let tz = TimeZone.current
        guard let t = tz.nextDaylightSavingTimeTransition(after: now), t.timeIntervalSince(now) < 60 * 86400 else { return [] }
        let delta = tz.secondsFromGMT(for: t.addingTimeInterval(3600)) - tz.secondsFromGMT(for: t.addingTimeInterval(-3600))
        guard delta != 0 else { return [] }
        let cal = Calendar.current
        guard let dayBefore = cal.date(byAdding: .day, value: -1, to: t),
              let fire = cal.date(bySettingHour: 20, minute: 0, second: 0, of: dayBefore) else { return [] }
        let amount = abs(delta) == 3600 ? L("notify.n.oneHour") : T("notify.n.minutes", abs(delta) / 60)
        return [PlannedNotification(id: "\(featurePrefix)dst-\(Int(t.timeIntervalSince1970))", fireAt: fire,
                                    title: T(delta > 0 ? "notify.n.dstForward" : "notify.n.dstBack", amount),
                                    body: Fmt.longDate(t))]
    }

    /// Her sabah günün tahmini (önümüzdeki üç gün; liste her açılışta ve arka planda tazelenir).
    /// WeatherKit şartı: Apple Hava Durumu adı bildirimde de yazar.
    private static func weather(place: AppPlace, now: Date) async -> [PlannedNotification] {
        guard let w = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) else { return [] }
        let percent = FloatingPointFormatStyle<Double>.Percent(locale: AppLocale.locale).precision(.fractionLength(0))
        let minutes = NotifyFeature.weatherMinutes
        return w.dailyForecast.prefix(3).compactMap { day in
            let d = CalendarDay(day.date, timeZone: place.zone)
            let fire = d.noon(in: place.zone).addingTimeInterval(Double(minutes * 60 - 12 * 3600))
            guard fire > now else { return nil }
            let body = T("notify.n.weatherBody", day.condition.description, Units.temp(day.highTemperature),
                         Units.temp(day.lowTemperature), day.precipitationChance.formatted(percent))
            return PlannedNotification(id: "\(featurePrefix)weather-\(d.iso)", fireAt: fire,
                                       title: T("notify.n.weather", place.name), body: body + "\n\u{F8FF} Weather")
        }
    }

    /// Saat başı: her saat için günlük yinelenen tek bildirim (iOS sınırında az yer tutsun diye)
    static func hourlyRequests() -> [UNNotificationRequest] {
        guard NotifyFeature.hourly.isOn else { return [] }
        let from = NotifyFeature.hourlyFrom, to = NotifyFeature.hourlyTo
        let hours = from <= to ? Array(from...to) : Array(from...23) + Array(0...to)
        return hours.map { h in
            let at = Calendar.current.date(bySettingHour: h, minute: 0, second: 0, of: Date()) ?? Date()
            let content = UNMutableNotificationContent()
            content.title = T("notify.n.hourly", [Fmt.time(at), Fmt.ampm(at)].filter { !$0.isEmpty }.joined(separator: " "))
            content.sound = .default
            content.threadIdentifier = "hourly"
            return UNNotificationRequest(identifier: "\(hourlyPrefix)\(h)", content: content,
                                         trigger: UNCalendarNotificationTrigger(dateMatching: DateComponents(hour: h, minute: 0),
                                                                                repeats: true))
        }
    }

    /// "30 Ekim 2026 Cuma · 14:25" (yerin saatiyle)
    private static func when(_ date: Date, _ place: AppPlace) -> String {
        "\(Fmt.longDate(date, tz: place.zone)) · \(place.hm(date))"
    }
}

/// Arka plan yenilemesi: bildirim listesi (vakitler, hava özeti) uygulama açılmasa da ileri taşınır.
/// iOS ne zaman çalıştıracağına kendisi karar verir; en erken 6 saat sonrası istenir.
enum BackgroundRefresh {
    static let id = "tr.bumba.time.refresh"

    static func schedule() {
        let r = BGAppRefreshTaskRequest(identifier: id)
        r.earliestBeginDate = Date().addingTimeInterval(6 * 3600)
        try? BGTaskScheduler.shared.submit(r)
    }

    static func run() async {
        schedule()
        await PrayerNotifier.reschedule()
    }
}
