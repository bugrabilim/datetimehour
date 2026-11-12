import SwiftUI
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
    case calendar, seasons, moon, sun, weather, hourly, dst, bedtime, habits, friday, daily, sky, photo
    /// Rashdul Qiblat ve sünnet oruçlar (10 Kasım 2026): yalnız Vakitler açıkken; oruçlar varsayılan kapalı
    case qibla, fasting

    var id: String { rawValue }
    var key: String { "notify.\(rawValue)" }
    /// Fotoğraf ışığının metinleri kendi `photoApp.*` anahtarlarında (9 Kasım 2026)
    var title: String {
        switch self {
        case .photo: L("photoApp.title")
        case .qibla: L("qibla.rashdul")
        default: L("notify.cat.\(rawValue)")
        }
    }
    var note: String { self == .photo ? L("photoApp.notify.note") : L("notify.cat.\(rawValue).note") }

    var icon: String {
        switch self {
        case .calendar: "calendar"
        case .seasons: "leaf"
        case .moon: "moon.circle"
        case .sun: "sunrise"
        case .weather: "cloud.sun"
        case .hourly: "clock"
        case .dst: "clock.arrow.2.circlepath"
        case .bedtime: "bed.double"
        case .habits: "cup.and.saucer"
        case .friday: "building.columns"
        case .daily: "quote.bubble"
        case .sky: "sparkles"
        case .photo: "camera.aperture"
        case .qibla: "location.north.circle"
        case .fasting: "moon.stars"
        }
    }

    /// İki tonlu simgenin adı (8 Kasım 2026, DUOTONE.md; Ayarlar › Bildirimler satırları)
    var duo: String {
        switch self {
        case .calendar: "calendar"
        case .seasons: "leaf"
        case .moon: "moon"
        case .sun: "sunrise"
        case .weather: "sun_cloud"
        case .hourly: "clock"
        case .dst: "dst"
        case .bedtime: "bed"
        case .habits: "coffee"
        case .friday: "mosque"
        case .daily: "speech"
        case .sky: "telescope"
        case .photo: "aperture"
        case .qibla: "kaaba"
        case .fasting: "star_crescent"
        }
    }

    /// Konu rengi: takvim ve günün sözü adaçayı, gök ve hava mavi, uyku/alışkanlık/saat başı/yaz saati kiremit, Cuma altın
    var tint: Color {
        switch self {
        case .calendar, .daily: Theme.sage
        case .seasons, .moon, .sun, .weather, .sky, .photo: Theme.sky
        case .hourly, .dst, .bedtime, .habits: Theme.terra
        case .friday, .qibla, .fasting: Theme.gold
        }
    }

    /// Varsayılan: saat başı kapalı, diğerleri açık (30 Ekim 2026, sahibin kararı)
    var defaultOn: Bool { self != .hourly && self != .fasting }
    var isOn: Bool { UserDefaults.standard.object(forKey: key) as? Bool ?? defaultOn }

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
    /// Bildirim düğmeleri (ör. "Bu alarmı atla"), sessiz düzey ve ek bilgi
    var category: String? = nil
    var passive = false
    var info: [String: String] = [:]
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
        if NotifyFeature.dst.isOn { out += dst(now: now) + worldDST(now: now) }
        if NotifyFeature.weather.isOn { out += await weather(place: place, now: now) }
        out += CountdownItem.reminders(now: now) // kişisel geri sayımların hatırlatmaları (her biri kendi ayarıyla)
        if NotifyFeature.bedtime.isOn { out += SleepPlan.reminders(now: now, days: days) } // uyku planı kuruluysa
        out += AlarmItem.preNotifications(now: now) // yaklaşan alarm (her alarm kendi ayarıyla)
        out += AlarmItem.preAlarms(now: now) // sakin ön alarm (2. analiz #42)
        if NotifyFeature.habits.isOn { out += SleepPlan.habits(now: now, days: min(days, 7)) } // uyku alışkanlıkları (#49)
        if NotifyFeature.friday.isOn { out += friday(place: place, now: now, days: days) } // Cuma hatırlatması (#58)
        if NotifyFeature.daily.isOn { out += daily(now: now, days: min(days, 7)) } // günün sözü (#16)
        if NotifyFeature.sky.isOn { out += sky(place: place, now: now, days: days) } // gökyüzü olayları (#154)
        // Fotoğraf ışığı: ekran bir kez açıldıysa, en çok 7 gün (64 bekleyen bildirim sınırı)
        if NotifyFeature.photo.isOn && PhotoPrefs.used { out += photo(place: place, now: now, days: min(days, 7)) }
        // Vakitler'e bağlı olanlar (10 Kasım 2026): Rashdul Qiblat, sünnet oruçlar, Kemenag takviminde isbat akşamı
        if Features.isOn(.prayer) {
            if NotifyFeature.qibla.isOn { out += rashdul(place: place, now: now) }
            if NotifyFeature.fasting.isOn { out += fasting(place: place, now: now, days: days) }
            if NotifyFeature.calendar.isOn { out += isbat(place: place, now: now, days: days) }
        }
        return out.filter { $0.fireAt > now }
    }

    /// Fotoğraf ışığı (9 Kasım 2026): seçilen ışığın (sabah/akşam mavi ve altın saat) başlangıcından `lead` dakika önce;
    /// önbellekte bulut tahmini varsa o günün gün doğumu/batımı tahmini gövdede yazar (bildirim için ağa çıkılmaz)
    static func photo(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        let ev = PhotoPrefs.events, lead = PhotoPrefs.lead
        let samples = PhotoSky.cached(lat: place.lat, lon: place.lon)
        let today = CalendarDay(now, timeZone: place.zone)
        return (0..<days).flatMap { i -> [PlannedNotification] in
            let p = PhotoDay(today.adding(days: i), place)
            return PhotoPrefs.eventIDs.filter { ev.contains($0) }.compactMap { e -> PlannedNotification? in
                guard let at = p.start(e) else { return nil }
                let fire = at.addingTimeInterval(Double(-lead * 60))
                guard fire > now else { return nil }
                let ref = e.hasPrefix("am") ? p.sunrise : p.sunset
                var body = "\(place.name) · \(p.clock(at))"
                if let ref, let samples, let q = PhotoLight.quality(at: Int64(ref.timeIntervalSince1970 * 1000), in: samples) {
                    body += " · " + T("photoApp.n.q", L("photoApp.q.\(q.rawValue)"))
                }
                return PlannedNotification(id: "\(featurePrefix)photo-\(e)-\(Int(at.timeIntervalSince1970 / 60))", fireAt: fire,
                                           title: T("photoApp.n.\(e)", lead), body: body)
            }
        }
    }

    /// Rashdul Qiblat: Güneş Kâbe'nin üstüne gelmeden 30 dk önce; yalnız Güneş o anda seçili yerde ufkun üstündeyse
    static func rashdul(place: AppPlace, now: Date) -> [PlannedNotification] {
        let y = CalendarDay(now, timeZone: place.zone).year
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        return (Qibla.rashdul(year: y) + Qibla.rashdul(year: y + 1)).compactMap { ms -> PlannedNotification? in
            let at = Date(timeIntervalSince1970: (ms / 60000).rounded() * 60)
            let c = utc.dateComponents([.year, .month, .day, .hour, .minute], from: at)
            let sun = Qibla.sun(year: c.year ?? 2026, month: c.month ?? 1, day: c.day ?? 1, ut: Double(c.hour ?? 0) + Double(c.minute ?? 0) / 60,
                                latitude: place.lat, longitude: place.lon)
            let fire = at.addingTimeInterval(-30 * 60)
            guard sun.alt > 0, fire > now, fire < now.addingTimeInterval(400 * 86_400) else { return nil }
            return PlannedNotification(id: "\(featurePrefix)rashdul-\(Int(at.timeIntervalSince1970 / 60))", fireAt: fire,
                                       title: L("qibla.rashdul"), body: T("notify.n.rashdul", place.hm(at)))
        }
    }

    /// Sünnet oruç: oruç gününden önceki akşam 20.00'de (yerin saatiyle); gövdede hicri tarih ve sahurun bittiği vakit
    /// (Kemenag'da ihtiyat imsakı)
    static func fasting(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        let kinds = SunnahFasts.kinds(place: place)
        let today = CalendarDay(now, timeZone: place.zone)
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        return (0...days).compactMap { i -> PlannedNotification? in
            let day = today.adding(days: i), prev = day.adding(days: -1)
            let names = SunnahFasts.reasons(day, kinds: kinds)
            guard !names.isEmpty, let h = Hijri.date(year: day.year, month: day.month, day: day.day),
                  let noon = cal.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: 12)),
                  let fire = cal.date(from: DateComponents(year: prev.year, month: prev.month, day: prev.day, hour: 20)), fire > now else { return nil }
            let body: String
            if let imsak = place.extraRows(on: noon)?.imsak {
                body = T("fast.n.body", h.text, L("prayer.method.rowImsak"), place.hm(imsak))
            } else if let f = place.schedule.entries(on: noon).first(where: { $0.prayer == .fajr }) {
                body = T("fast.n.body", h.text, Prayer.fajr.localizedName, place.hm(f.at))
            } else {
                return nil
            }
            return PlannedNotification(id: "\(featurePrefix)fast-\(day.iso)", fireAt: fire,
                                       title: T("fast.n.title", names.map { L($0) }.joined(separator: " · ")), body: body)
        }
    }

    /// Sidang isbat (Endonezya): Kemenag takvimi kullanılıyorsa Şaban, Ramazan ve Zilkade'nin 29'unda 16.00'da; gövdede
    /// takvime göre beklenen sonuç (sıradaki ayın 1'i). Karar farklı çıkarsa hicri tarih Hesap yöntemi'nden kaydırılır.
    static func isbat(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        guard Hijri.calendar == "kemenag" else { return [] }
        let today = CalendarDay(now, timeZone: place.zone)
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        return (0...days).compactMap { i -> PlannedNotification? in
            let day = today.adding(days: i)
            guard let h = Hijri.date(year: day.year, month: day.month, day: day.day), h.day == 29, [8, 9, 11].contains(h.month),
                  let m = Hijri.month(year: h.year, month: h.month + 1),
                  let fire = cal.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: 16)), fire > now else { return nil }
            let first = CalendarDay(m.first.0, m.first.1, m.first.2)
            return PlannedNotification(id: "\(featurePrefix)isbat-\(day.iso)", fireAt: fire, title: L("notify.n.isbat"),
                                       body: T("notify.n.isbatBody", L("hijri.month.\(h.month + 1)"), Fmt.day(first)))
        }
    }

    /// Gökyüzü olayları (2. analiz #154): o gecenin akşamı 20.00'de (yerel öğleden önceki olay bir önceki akşama düşer)
    private static func sky(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        let lo = now.addingTimeInterval(-86_400), hi = now.addingTimeInterval(Double(days + 1) * 86_400)
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        return SkyEvent.all.filter { $0.date > lo && $0.date < hi }.compactMap { e in
            let hour = cal.component(.hour, from: e.date)
            let dayStart = cal.startOfDay(for: e.date)
            let evening = cal.date(byAdding: .hour, value: hour < 12 ? -4 : 20, to: dayStart) ?? e.date
            let body: String = e.detail ?? Fmt.shortDateTime(e.date, tz: place.zone)
            return PlannedNotification(id: "\(featurePrefix)sky-\(e.t)-\(e.k)", fireAt: evening, title: e.title, body: body, sound: nil, passive: true)
        }
    }

    /// Günün sözü (2. analiz #16): her gün 08.00'de (telefonun saatiyle) takvim yaprağındaki söz
    private static func daily(now: Date, days: Int) -> [PlannedNotification] {
        (0..<days).compactMap { i in
            let day = CalendarDay(now).adding(days: i)
            guard let text = Sayings.today(day) else { return nil }
            let at = day.noon(in: .current).addingTimeInterval(-4 * 3600)
            return PlannedNotification(id: "\(featurePrefix)daily-\(day.iso)", fireAt: at, title: L("notify.n.daily"), body: text,
                                       sound: nil, passive: true)
        }
    }

    /// Cuma hatırlatması (2. analiz #58): Cuma günü öğle vaktinden 45 dakika önce
    private static func friday(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        return (0..<days).compactMap { i in
            let d = now.addingTimeInterval(Double(i) * 86_400)
            guard cal.component(.weekday, from: d) == 6,
                  let dh = place.schedule.entries(on: d).first(where: { $0.prayer == .dhuhr }) else { return nil }
            return PlannedNotification(id: "\(featurePrefix)fri-\(Int(dh.at.timeIntervalSince1970 / 60))", fireAt: dh.at.addingTimeInterval(-45 * 60),
                                       title: L("notify.n.friday"), body: T("notify.n.fridayBody", place.hm(dh.at), place.name))
        }
    }

    /// Özel günler: o gün sabah 09.00'da (telefonun saatiyle), günün bütün olayları tek bildirimde
    private static func calendar(now: Date, days: Int) -> [PlannedNotification] {
        let today = CalendarDay(now)
        return (0..<days).compactMap { i in
            let day = today.adding(days: i)
            var seen = Set<String>()
            // Kapsayıcı takvimlerin çok günlü olayları yalnız ilk gününde (her gün yinelenmesin)
            let names = calEvents(day).filter { !$0.type.hasPrefix("cal:") || $0.date == day.iso }.map(\.name).filter { seen.insert($0).inserted }
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
    /// Ay olayları (2. analiz #152; `SunMoonPrefs`): seçilen olaylar (yeni ay, dolunay, dördünler, süper Ay, tutulmalar),
    /// istenirse olaydan önce
    private static func moon(place: AppPlace, now: Date) -> [PlannedNotification] {
        let ev = SunMoonPrefs.moonEvents
        let lead = Double(SunMoonPrefs.moonLead * 60)
        var out: [PlannedNotification] = []
        func add(_ key: String, _ ms: Int64, _ title: String) {
            let at = Date(unixMilliseconds: ms)
            let fire = at.addingTimeInterval(-lead)
            let id = "\(featurePrefix)moon-\(key)-\(ms / 60000)"
            guard fire > now, !out.contains(where: { $0.id == id }) else { return }
            out.append(PlannedNotification(id: id, fireAt: fire, title: title, body: when(at, place)))
        }
        var t = now
        for _ in 0..<3 {
            let m = Nature.moon(at: t)
            for (ms, key) in [(m.nextNewMoon, "new"), (m.nextFullMoon, "full")] where ev.contains(key) {
                add(key, ms, L("nature.phases.\(key)"))
            }
            t = Date(unixMilliseconds: min(m.nextNewMoon, m.nextFullMoon)).addingTimeInterval(3600)
        }
        let nowMs = now.timeIntervalSince1970 * 1000
        if ev.contains("quarter") {
            let q = MoonDetails.quarters(nowMs)
            add("fq", Int64(q.0), L("nature.phases.firstQuarter"))
            add("lq", Int64(q.1), L("nature.phases.lastQuarter"))
        }
        if ev.contains("super"), let sup = MoonDetails.superMoon(nowMs, perigee: MoonDetails.perigeeApogee(nowMs).0) {
            add("super", Int64(sup), L("moon.ev.super"))
        }
        if ev.contains("eclipse") {
            for e in Eclipse.all where e.date > now && e.date < now.addingTimeInterval(60 * 86400) { add("ecl", e.ut, e.title) }
        }
        return out
    }

    /// Güneş (2. analiz #152): seçilen olaylar (doğuş, batış, sabah ve akşam altın saat başlangıcı), seçilen günler,
    /// istenirse olaydan önce (vakit düzeltmeleri olmadan; Mevsimler'deki saatler)
    private static func sun(place: AppPlace, now: Date, days: Int) -> [PlannedNotification] {
        let ev = SunMoonPrefs.sunEvents
        let lead = Double(SunMoonPrefs.sunLead * 60)
        let dayset = SunMoonPrefs.sunDays
        return (0..<days).flatMap { i -> [PlannedNotification] in
            let date = now.addingTimeInterval(Double(i) * 86400)
            let d = CalendarDay(date, timeZone: place.zone)
            guard dayset.contains(d.weekday + 1) else { return [] }
            var items: [(String, Date, String)] = []
            if let s = SeasonsView.sun(place: place, now: date) {
                if ev.contains("sunrise") { items.append(("sunrise", s.rise, L("nature.sunrise"))) }
                if ev.contains("sunset") { items.append(("sunset", s.set, L("nature.sunset"))) }
            }
            if ev.contains("goldenAm") || ev.contains("goldenPm") {
                let off = Double(place.zone.secondsFromGMT(for: d.noon(in: place.zone))) / 3600
                let l = PrayerTimes.lightHours(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon, utcOffset: off)
                let start = place.startOfDay(d)
                if ev.contains("goldenAm"), let h = l.goldenAm.0 { items.append(("goldenam", start.addingTimeInterval(h * 3600), L("nature.goldenAm"))) }
                if ev.contains("goldenPm"), let h = l.goldenPm.0 { items.append(("goldenpm", start.addingTimeInterval(h * 3600), L("nature.goldenPm"))) }
            }
            return items.compactMap { key, at, title in
                let fire = at.addingTimeInterval(-lead)
                guard fire > now else { return nil }
                return PlannedNotification(id: "\(featurePrefix)\(key)-\(Int(at.timeIntervalSince1970) / 60)", fireAt: fire,
                                           title: title, body: "\(place.name) · \(place.hm(at))")
            }
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

    /// Dünya saatlerindeki şehirlerde saat değişmeden önceki akşam 20.00'de (telefonun saatiyle; rakip analizi #30).
    /// Telefonun kendi değişimiyle aynı an ve aynı farkta olan şehir atlanır (o bildirim zaten var).
    private static func worldDST(now: Date) -> [PlannedNotification] {
        let keys = (UserDefaults.standard.string(forKey: "world") ?? City.defaultKeys).split(separator: ",").map(String.init)
        let mine = TimeZone.current.nextDaylightSavingTimeTransition(after: now)
        let cal = Calendar.current
        return keys.compactMap { WorldEntry.of($0) }.compactMap { city in
            guard let d = city.nextDST(after: now, days: 30), d.at != mine,
                  let dayBefore = cal.date(byAdding: .day, value: -1, to: d.at),
                  let fire = cal.date(bySettingHour: 20, minute: 0, second: 0, of: dayBefore) else { return nil }
            return PlannedNotification(id: "\(featurePrefix)wdst-\(city.key)-\(Int(d.at.timeIntervalSince1970))", fireAt: fire,
                                       title: T("world.dstTitle", city.displayName), body: city.dstNote(now: now, days: 30) ?? "")
        }
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
        } + (UserDefaults.standard.object(forKey: "notify.weather.alerts") as? Bool ?? true ? weatherAlerts(w, place: place, now: now) : [])
    }

    /// Hava uyarıları (2. analiz #110; Android'deki weatherAlerts ile aynı): akşam 19.00'da "yarın belirgin şekilde
    /// soğuk/sıcak" (en yüksek 5° ve fazla fark) ve önümüzdeki 24 saatte yağış ihtimali %60'ı geçen ilk saatten 15 dk önce
    private static func weatherAlerts(_ w: Weather, place: AppPlace, now: Date) -> [PlannedNotification] {
        var out: [PlannedNotification] = []
        let title = T("notify.n.weather", place.name)
        let days = Array(w.dailyForecast.prefix(3))
        if days.count > 1 {
            for i in 0..<(days.count - 1) {
                let a = days[i].highTemperature.converted(to: .celsius).value, b = days[i + 1].highTemperature.converted(to: .celsius).value
                guard let s = WeatherWords.tomorrowAlert(todayMax: a, tomorrowMax: b) else { continue }
                let d = CalendarDay(days[i].date, timeZone: place.zone)
                let fire = d.noon(in: place.zone).addingTimeInterval(7 * 3600)
                guard fire > now else { continue }
                out.append(PlannedNotification(id: "\(featurePrefix)weather-alert-\(CalendarDay(days[i + 1].date, timeZone: place.zone).iso)", fireAt: fire,
                                               title: title, body: T(s.key, Units.delta(s.n)) + "\n\u{F8FF} Weather"))
            }
        }
        if let h = w.hourlyForecast.first(where: { $0.date > now.addingTimeInterval(1800) && $0.date < now.addingTimeInterval(86_400) && $0.precipitationChance >= 0.6 }) {
            out.append(PlannedNotification(id: "\(featurePrefix)weather-rain-\(Int(h.date.timeIntervalSince1970))", fireAt: h.date.addingTimeInterval(-900),
                                           title: title, body: T("weather.alert.rainSoon", Fmt.time(h.date, tz: place.zone)) + "\n\u{F8FF} Weather"))
        }
        return out
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
        await AlarmCenter.refreshSkips()
        await PrayerAlarms.sync()
        await MainActor.run {
            LiveActivities.sync()
            FocusBlock.sync() // zaman çizelgeli odak engeli (2. analiz #27)
        }
        // Kilit ekranı / Dynamic Island hava etkinliği (açıksa)
        await LiveActivities.refreshWeather()
    }
}

/// Güneş ve Ay bildirimlerinin ayrıntısı (2. analiz #152; Android'deki SunMoonPrefs ile aynı anahtarlar)
enum SunMoonPrefs {
    static let moonEventsAll = ["new", "full", "quarter", "super", "eclipse"]
    static let sunEventsAll = ["sunrise", "sunset", "goldenAm", "goldenPm"]
    static let moonLeads = [0, 60, 180, 1440]
    static let sunLeads = [0, 5, 10, 15, 30, 60]

    private static func set(_ key: String, _ def: String) -> Set<String> {
        Set((UserDefaults.standard.string(forKey: key) ?? def).split(separator: ",").map(String.init))
    }

    static var moonEvents: Set<String> {
        get { set("notify.moon.events", "new,full") }
        set { UserDefaults.standard.set(moonEventsAll.filter { newValue.contains($0) }.joined(separator: ","), forKey: "notify.moon.events") }
    }
    static var sunEvents: Set<String> {
        get { set("notify.sun.events", "sunrise,sunset") }
        set { UserDefaults.standard.set(sunEventsAll.filter { newValue.contains($0) }.joined(separator: ","), forKey: "notify.sun.events") }
    }
    static var moonLead: Int { UserDefaults.standard.integer(forKey: "notify.moon.lead") }
    static var sunLead: Int { UserDefaults.standard.integer(forKey: "notify.sun.lead") }
    /// ISO hafta günleri (1 pazartesi … 7 pazar)
    static var sunDays: Set<Int> {
        get { Set(set("notify.sun.days", "1,2,3,4,5,6,7").compactMap { Int($0) }) }
        set { UserDefaults.standard.set((1...7).filter { newValue.contains($0) }.map(String.init).joined(separator: ","), forKey: "notify.sun.days") }
    }

    static func leadLabel(_ m: Int) -> String {
        m == 0 ? L("notify.lead.now") : m % 1440 == 0 ? T("notify.lead.days", m / 1440) : m % 60 == 0 ? T("notify.lead.hours", m / 60) : T("notify.lead.minutes", m)
    }

    static func label(_ e: String) -> String {
        switch e {
        case "new", "full": return L("nature.phases.\(e)")
        case "quarter": return L("moon.ev.quarters")
        case "super": return L("moon.ev.super")
        case "eclipse": return L("moon.ev.eclipse")
        case "sunrise": return L("nature.sunrise")
        case "sunset": return L("nature.sunset")
        case "goldenAm": return L("nature.goldenAm")
        default: return L("nature.goldenPm")
        }
    }
}

/// Güneş/Ay bildirimi ayrıntısı (olaylar, önceden haber verme, Güneş için günler)
struct SunMoonOptionsView: View {
    let moon: Bool
    let changed: () -> Void
    @State private var events: Set<String> = []
    @State private var days: Set<Int> = []
    @AppStorage("notify.moon.lead") private var moonLead = 0
    @AppStorage("notify.sun.lead") private var sunLead = 0

    var body: some View {
        let all = moon ? SunMoonPrefs.moonEventsAll : SunMoonPrefs.sunEventsAll
        Group {
            ForEach(all, id: \.self) { e in
                Toggle(SunMoonPrefs.label(e), isOn: Binding(get: { events.contains(e) }, set: { on in
                    if on { events.insert(e) } else { events.remove(e) }
                    if moon { SunMoonPrefs.moonEvents = events } else { SunMoonPrefs.sunEvents = events }
                    changed()
                }))
                .padding(.leading, 12)
            }
            Picker(L("notify.lead"), selection: moon ? $moonLead : $sunLead) {
                ForEach(moon ? SunMoonPrefs.moonLeads : SunMoonPrefs.sunLeads, id: \.self) { Text(SunMoonPrefs.leadLabel($0)).tag($0) }
            }
            .padding(.leading, 12)
            .onChange(of: moon ? moonLead : sunLead) { changed() }
            if !moon {
                HStack(spacing: 4) {
                    ForEach(1...7, id: \.self) { d in
                        let on = days.contains(d)
                        Button {
                            if on { days.remove(d) } else { days.insert(d) }
                            SunMoonPrefs.sunDays = days
                            changed()
                        } label: {
                            Text(L("tools.alarm.dayNames.\(d - 1)")).font(.caption.weight(.semibold))
                                .frame(maxWidth: .infinity, minHeight: 30)
                                .background(on ? Theme.accent : Theme.surface2, in: Capsule())
                                .foregroundStyle(on ? Color.white : Theme.text)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.leading, 12)
            }
        }
        .onAppear {
            events = moon ? SunMoonPrefs.moonEvents : SunMoonPrefs.sunEvents
            days = SunMoonPrefs.sunDays
        }
    }
}

/// Sünnet oruçlar (10 Kasım 2026, Endonezya araştırması #8; Android'deki SunnahFasts ile aynı kural): pazartesi-perşembe,
/// eyyâm-ı bîd (13–15), arefe (9 Zilhicce), Tasua ve Aşure (9–10 Muharrem), Şevval'in altı günü (bayramın ertesi günü bir kez).
/// Hicri günler uygulamanın hicri takvimiyle; Ramazan, Ramazan Bayramı, Kurban Bayramı ve teşrik günleri (10–13 Zilhicce)
/// atlanır. Seçim `notify.fasting.kinds` (virgülle); kayıt yoksa hepsi, Caferî ve Tahran yönteminde Aşure hariç.
enum SunnahFasts {
    static let kindsKey = "notify.fasting.kinds"
    static let all = ["monthu", "bidh", "arafah", "ashura", "shawwal"]

    static func kinds(place: AppPlace) -> Set<String> {
        if let s = UserDefaults.standard.string(forKey: kindsKey) {
            return Set(s.split(separator: ",").map(String.init).filter { all.contains($0) })
        }
        let shia = ["jafari", "tehran"].contains(PrayerMethod.id(for: place))
        return Set(all.filter { !(shia && $0 == "ashura") })
    }

    static func setKinds(_ k: Set<String>) {
        UserDefaults.standard.set(all.filter { k.contains($0) }.joined(separator: ","), forKey: kindsKey)
    }

    /// O günün sünnet oruç adları (metin anahtarı); yasak günde ve Ramazan'da boş
    static func reasons(_ day: CalendarDay, kinds: Set<String>) -> [String] {
        guard let h = Hijri.date(year: day.year, month: day.month, day: day.day) else { return [] }
        let m = h.month, d = h.day
        if m == 9 || (m == 10 && d == 1) || (m == 12 && (10...13).contains(d)) { return [] }
        var out: [String] = []
        if kinds.contains("arafah") && m == 12 && d == 9 { out.append("fast.name.arafah") }
        if kinds.contains("ashura") && m == 1 && d == 9 { out.append("fast.name.tasua") }
        if kinds.contains("ashura") && m == 1 && d == 10 { out.append("fast.name.ashura") }
        if kinds.contains("shawwal") && m == 10 && d == 2 { out.append("fast.name.shawwal") }
        if kinds.contains("bidh") && (13...15).contains(d) { out.append("fast.name.bidh") }
        if kinds.contains("monthu") {
            if day.weekday == 0 { out.append("fast.name.monday") } else if day.weekday == 3 { out.append("fast.name.thursday") }
        }
        return out
    }
}
