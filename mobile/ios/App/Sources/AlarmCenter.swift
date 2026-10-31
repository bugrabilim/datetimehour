import Foundation
import SwiftUI
import UserNotifications
import SaatCore
import AppIntents
#if canImport(AlarmKit)
import ActivityKit
import AlarmKit
#endif

/// Alarm: saat, etiket, günler, erteleme süresi ve sınırı, "sonraki alarmı atla", kademeli ses (30 Ekim 2026; eski
/// kayıtlardaki `repeats` [bir kez / her gün / hafta içi] okunurken günlere çevrilir). Günler ISO: 1 pazartesi … 7 pazar.
struct AlarmItem: Codable, Identifiable, Hashable {
    var id = UUID()
    var hour: Int
    var minute: Int
    var label: String = ""
    /// Çalacağı günler; boşsa bir kez
    var days: [Int] = []
    var enabled = true
    /// Erteleme süresi (dakika; 0 = ertelemesiz)
    var snooze: Int = 5
    /// Bu andan önceki çalmalar atlanır
    var skipUntil: Date?
    /// En çok kaç kez ertelenebilir (0 = sınırsız)
    var snoozeMax: Int = 0
    /// Kademeli ses: AlarmKit sesi yükseltemediği için uygulamayla gelen yükselen melodi (alarm-rise.caf) çalar
    var rise = false
    /// Resmî tatillerde çalmaz (rakip analizi #4; yalnız tekrarlayan alarmda; yarım gün arifelerde çalar)
    var skipHolidays = false
    /// Gün doğumu ışığı: alarmdan bu kadar dakika önce tam ekran saat yavaşça aydınlanır (0 = kapalı; rakip analizi #17)
    var sunrise = 0
    /// Kapatma görevi (rakip analizi #13): none, math, shake, steps, code; zorluk (matematik 1–3); kayıtlı kod (QR/barkod)
    var task = "none"
    var taskLevel = 2
    var code = ""
    /// Uyanma denetimi: kapattıktan 5 dk sonra "Uyanık mısınız?"; cevap gelmezse yeniden çalar
    var wakeCheck = false
    /// Çalmadan bu kadar dakika önce sessiz "yaklaşan alarm" bildirimi, içinden atlanabilir (0 = kapalı; rakip analizi #19).
    /// Yeni alarmda 30, önceki sürümden gelen alarmda kapalı.
    var preNotify = 30
    /// Çalarken saati sesli söyler (rakip analizi #20; iOS'ta saati söyleyen ses dosyası alarm sesi olur)
    var speak = false
    /// Esnek tekrar (rakip analizi listesi #74): bir kezlik alarmda belirli tarih (ISO; boşsa ilk uygun gün), tekrarlayanda
    /// kaç haftada bir (1 ya da 2; başlangıç haftası `anchor`, ISO pazartesi) ve bitiş tarihi (ISO, o gün dahil; boşsa yok)
    var date = ""
    var every = 1
    var anchor = ""
    var until = ""
    /// Müzikle alarm (rakip analizi listesi #94): seçilen şarkıdan kesit anahtarı (Library/Sounds/alarm-music-<anahtar>.caf;
    /// boşsa yok) ve şarkının adı. Yalnız iPhone'da; Android'de alarm sesi sistemin zil sesi seçicisinden seçilir.
    var music = ""
    var musicName = ""

    static let snoozeChoices = [0, 5, 10, 15, 20, 30]
    static let snoozeMaxChoices = [0, 1, 2, 3, 5]
    static let sunriseChoices = [0, 10, 20, 30]
    static let preNotifyChoices = [0, 15, 30, 60]
    static let riseSound = "alarm-rise.caf"
    static let weekdays = [1, 2, 3, 4, 5]

    init(hour: Int, minute: Int) {
        self.hour = hour
        self.minute = minute
    }

    private enum CodingKeys: String, CodingKey { case id, hour, minute, label, days, enabled, snooze, skipUntil, snoozeMax, rise, skipHolidays, sunrise, task, taskLevel, code, wakeCheck, preNotify, speak, date, every, anchor, until, music, musicName }
    private enum LegacyKeys: String, CodingKey { case repeats }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(UUID.self, forKey: .id) ?? UUID()
        hour = try c.decode(Int.self, forKey: .hour)
        minute = try c.decode(Int.self, forKey: .minute)
        label = try c.decodeIfPresent(String.self, forKey: .label) ?? ""
        enabled = try c.decodeIfPresent(Bool.self, forKey: .enabled) ?? true
        snooze = try c.decodeIfPresent(Int.self, forKey: .snooze) ?? 5
        skipUntil = try c.decodeIfPresent(Date.self, forKey: .skipUntil)
        snoozeMax = try c.decodeIfPresent(Int.self, forKey: .snoozeMax) ?? 0
        rise = try c.decodeIfPresent(Bool.self, forKey: .rise) ?? false
        skipHolidays = try c.decodeIfPresent(Bool.self, forKey: .skipHolidays) ?? false
        sunrise = try c.decodeIfPresent(Int.self, forKey: .sunrise) ?? 0
        task = try c.decodeIfPresent(String.self, forKey: .task) ?? "none"
        taskLevel = try c.decodeIfPresent(Int.self, forKey: .taskLevel) ?? 2
        code = try c.decodeIfPresent(String.self, forKey: .code) ?? ""
        wakeCheck = try c.decodeIfPresent(Bool.self, forKey: .wakeCheck) ?? false
        preNotify = try c.decodeIfPresent(Int.self, forKey: .preNotify) ?? 0
        speak = try c.decodeIfPresent(Bool.self, forKey: .speak) ?? false
        date = try c.decodeIfPresent(String.self, forKey: .date) ?? ""
        every = try c.decodeIfPresent(Int.self, forKey: .every) ?? 1
        anchor = try c.decodeIfPresent(String.self, forKey: .anchor) ?? ""
        until = try c.decodeIfPresent(String.self, forKey: .until) ?? ""
        music = try c.decodeIfPresent(String.self, forKey: .music) ?? ""
        musicName = try c.decodeIfPresent(String.self, forKey: .musicName) ?? ""
        if let d = try c.decodeIfPresent([Int].self, forKey: .days) {
            days = d
        } else {
            let legacy = try decoder.container(keyedBy: LegacyKeys.self)
            switch try legacy.decodeIfPresent(String.self, forKey: .repeats) {
            case "every": days = Array(1...7)
            case "weekdays": days = Self.weekdays
            default: days = []
            }
        }
    }

    var title: String { label.isEmpty ? L("tools.alarm.defaultLabel") : label }

    /// Kaydı eksik görev matematiğe düşer: kod okutmada kod, fotoğrafta özet ("ph:…") yoksa
    static func effectiveTask(_ task: String, code: String) -> String {
        if task == "code" && (code.isEmpty || code.hasPrefix(PhotoHash.prefix)) { return "math" }
        if task == "photo" && !code.hasPrefix(PhotoHash.prefix) { return "math" }
        return task
    }

    /// Durdurulunca istenecek görev ve denetim (görev ya da denetim yoksa nil)
    var wakeTask: PendingWakeTask? {
        guard task != "none" || wakeCheck else { return nil }
        let t = Self.effectiveTask(task, code: code)
        return PendingWakeTask(task: t, level: taskLevel, code: code, wakeCheck: wakeCheck, title: title, rise: rise, rings: 0, repeatID: nil)
    }
    var timeText: String {
        var c = DateComponents()
        c.hour = hour
        c.minute = minute
        return Fmt.time(Calendar.current.date(from: c) ?? Date())
    }

    /// "Bir kez", "Her gün", "Hafta içi" ya da gün kısaltmaları; belirli tarih, iki haftada bir ve bitiş tarihi eklenir
    var repeatText: String {
        let d = Set(days)
        var text: String
        if d.isEmpty { text = CalendarDay(iso: date).map { Fmt.dayMonth($0.noon(in: .current)) } ?? L("tools.alarm.once") }
        else if d.count == 7 { text = L("alarm.everyDay") }
        else if d == Set(Self.weekdays) { text = L("tools.alarm.weekdays") }
        else { text = days.sorted().map { L("tools.alarm.dayNames.\($0 - 1)") }.joined(separator: " ") }
        if !d.isEmpty && every == 2 { text += " · " + L("alarm.biweekly") }
        if !d.isEmpty, let u = CalendarDay(iso: until) { text += " · " + T("alarm.untilShort", Fmt.dayMonth(u.noon(in: .current))) }
        return text
    }

    /// Esnek tekrar kuralı: bu gün çalar mı (gün, iki haftada bir, belirli tarih, bitiş)
    func allows(_ day: CalendarDay) -> Bool {
        if let u = CalendarDay(iso: until), day > u { return false }
        if days.isEmpty { return CalendarDay(iso: date).map { $0 == day } ?? true }
        guard days.contains(day.weekday + 1) else { return false }
        if every > 1, let a = CalendarDay(iso: anchor) {
            let weeks = (day.adding(days: -day.weekday).dayNumber - a.adding(days: -a.weekday).dayNumber) / 7
            return ((weeks % every) + every) % every == 0
        }
        return true
    }
    /// Haftalık tekrar AlarmKit'in yinelemesiyle kurulamıyorsa (iki haftada bir ya da bitiş tarihi): tek tek kurulur
    var needsPlan: Bool { !days.isEmpty && (every > 1 || !until.isEmpty) }
    /// Bitiş tarihi geçti ya da belirli tarih geçmişte kaldı: bir daha çalmaz
    func expired(now: Date = Date()) -> Bool { next(after: now) == nil }

    /// ISO gün (1 pazartesi … 7 pazar)
    static func isoWeekday(_ date: Date) -> Int {
        let w = Calendar.current.component(.weekday, from: date) // pazar = 1
        return (w + 5) % 7 + 1
    }

    private func at(_ day: Date) -> Date? {
        Calendar.current.date(bySettingHour: hour, minute: minute, second: 0, of: day)
    }

    /// `date` sonrasındaki ilk çalma (atlama hesaba katılmaz)
    func occurrence(after date: Date) -> Date? {
        let cal = Calendar.current
        // Belirli tarih uzakta olabilir: o güne doğrudan bakılır
        if days.isEmpty, let d = CalendarDay(iso: self.date) {
            guard let t = at(d.noon(in: .current)), t > date, allows(d) else { return nil }
            return t
        }
        for add in 0...15 {
            guard let day = cal.date(byAdding: .day, value: add, to: date), let t = at(day), t > date else { continue }
            if allows(CalendarDay(t)) { return t }
        }
        return nil
    }

    /// `date` öncesindeki son çalma (günaydın kartı için)
    func occurrence(before date: Date) -> Date? {
        let cal = Calendar.current
        for back in 0...15 {
            guard let day = cal.date(byAdding: .day, value: -back, to: date), let t = at(day), t <= date else { continue }
            if (days.isEmpty && self.date.isEmpty) || allows(CalendarDay(t)) { return t }
        }
        return nil
    }

    /// Türkiye resmî tatili mi (calendar.json, tam gün tatil; arife yarım gün sayılmaz)
    static var isHoliday: (Date) -> Bool = { date in
        CalendarData.shared.events(on: CalendarDay(date)).contains { $0.type == "holiday" }
    }

    /// Bu çalma atlanır mı: "sonraki alarmı atla" ya da (tekrarlayan alarmda) resmî tatil
    func blocked(_ t: Date, now: Date) -> Bool {
        if let s = skipUntil, s > now, t < s { return true }
        return skipHolidays && !days.isEmpty && Self.isHoliday(t)
    }

    /// Sıradaki çalma (atlanan çalma ve tatiller geçilir)
    func next(after now: Date = Date()) -> Date? {
        var t = occurrence(after: now)
        for _ in 0..<60 {
            guard let c = t else { return nil }
            if !blocked(c, now: now) { return c }
            t = occurrence(after: c)
        }
        return t
    }

    /// Elle atlama olmadan (atlanacak çalmayı bulmak için)
    var unskipped: AlarmItem { var c = self; c.skipUntil = nil; return c }

    /// Sıradaki çalma bir resmî tatil yüzünden kaydırıldı mı (listede not için)
    func holidayShifted(now: Date = Date()) -> Bool {
        guard skipHolidays, !days.isEmpty else { return false }
        var plain = self; plain.skipHolidays = false
        return next(after: now) != plain.next(after: now)
    }

    /// Tekrarlayan alarmın kurulum planı: haftalık günler + tek seferlik çalmalar. Önümüzdeki 7 günde atlanan ya da
    /// tatile denk gelen çalmanın günü haftalık tekrardan çıkarılır, o günün sonraki 4 haftası tek seferlik kurulur
    /// (tatil olanlar hariç). Uygulama açılınca ya da arka plan yenilemesinde plan baştan kurulur.
    func plan(now: Date = Date()) -> (days: [Int], fixed: [Date]) {
        // İki haftada bir ya da bitiş tarihli: haftalık yineleme yok, sonraki 8 çalma tek tek
        if needsPlan {
            var fixed: [Date] = [], t = next(after: now)
            while let c = t, fixed.count < 8 { fixed.append(c); t = next(after: c) }
            return ([], fixed)
        }
        var weekly = days, fixed: [Date] = []
        var t = occurrence(after: now)
        let end = now.addingTimeInterval(7 * 86_400)
        while let c = t, c <= end {
            if blocked(c, now: now) {
                let wd = Self.isoWeekday(c)
                weekly.removeAll { $0 == wd }
                for k in 1...4 {
                    if let later = Calendar.current.date(byAdding: .day, value: 7 * k, to: c), !blocked(later, now: now) { fixed.append(later) }
                }
            }
            t = occurrence(after: c)
        }
        return (weekly, fixed)
    }

    /// Gün doğumu ışığı seçili sıradaki alarm (tam ekran saat için): an ve pencere (dakika)
    static func nextSunrise(now: Date = Date()) -> (at: Date, window: Int)? {
        // Alarm anından 15 dk sonrasına kadar ışık yanık kalır: biraz geriden bakılır
        let from = now.addingTimeInterval(-SunriseLight.after)
        return load().filter { $0.enabled && $0.sunrise > 0 }
            .compactMap { a in a.next(after: from).map { ($0, a.sunrise) } }
            .min { $0.0 < $1.0 }
    }

    /// Yaklaşan alarm bildirimleri (sessiz, "Bu alarmı atla" düğmeli): her alarmın sıradaki üç çalması için; özellik
    /// bildirimleriyle aynı listede kurulur (PrayerNotifier.reschedule)
    @MainActor static func preNotifications(now: Date) -> [PlannedNotification] {
        load().filter { $0.enabled && $0.preNotify > 0 }.flatMap { item -> [PlannedNotification] in
            var out: [PlannedNotification] = []
            var t = item.next(after: now)
            while let at = t, out.count < 3 {
                let fire = at.addingTimeInterval(-Double(item.preNotify * 60))
                if fire > now {
                    out.append(PlannedNotification(
                        id: "\(FeatureNotifier.featurePrefix)alarmpre-\(item.id.uuidString)-\(Int(at.timeIntervalSince1970) / 60)",
                        fireAt: fire, title: T("alarm.preTitle", Fmt.time(at)), body: item.title + " · " + L("alarm.preBody"),
                        thread: "alarm", sound: nil, category: AlarmPreNotify.category,
                        passive: true, info: ["alarm": item.id.uuidString, "at": String(at.timeIntervalSince1970)]))
                }
                t = item.next(after: at.addingTimeInterval(60))
            }
            return out
        }
    }

    static let storeKey = "alarms"
    static func load() -> [AlarmItem] {
        guard let data = UserDefaults.standard.data(forKey: storeKey) else { return [] }
        return (try? JSONDecoder().decode([AlarmItem].self, from: data)) ?? []
    }
    static func save(_ items: [AlarmItem]) {
        UserDefaults.standard.set(try? JSONEncoder().encode(items), forKey: storeKey)
        AlarmMusic.cleanup(keeping: items)
    }
}

/// Yaklaşan alarm bildiriminin "Bu alarmı atla" düğmesi (AppDelegate didReceive)
@MainActor
enum AlarmPreNotify {
    static let category = "alarmpre"
    static let skipAction = "skip"

    static var categories: Set<UNNotificationCategory> {
        [UNNotificationCategory(identifier: category,
                                actions: [UNNotificationAction(identifier: skipAction, title: L("alarm.preSkip"), options: [])],
                                intentIdentifiers: [])]
    }

    /// Bildirimdeki alarmın o çalmasını atlar
    static func skip(alarmID: String, at: TimeInterval) async {
        var items = AlarmItem.load()
        guard let i = items.firstIndex(where: { $0.id.uuidString == alarmID }) else { return }
        items[i].skipUntil = Date(timeIntervalSince1970: at + 60)
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
        await PrayerNotifier.reschedule()
    }
}

/// Alarmda saatin sesli söylenmesi (rakip analizi #20): AlarmKit çalarken uygulama kod çalıştıramadığı için zil ve
/// "Saat 07:00" konuşması telefonda bir kez ses dosyasına yazılır (Library/Sounds) ve alarm sesi olarak verilir.
@MainActor
enum AlarmSpeech {
    static let prefix = "alarm-say-"
    static func fileName(_ item: AlarmItem) -> String {
        "\(prefix)\(AppLocale.code)-\(String(format: "%02d%02d", item.hour, item.minute)).caf"
    }
    static func sound(for item: AlarmItem) async -> String? {
        guard item.speak else { return nil }
        let name = fileName(item)
        return await NotificationSounds.make(name: name, text: T("alarm.speakText", item.timeText)) ? name : nil
    }
}

/// Alarm ve zamanlayıcı kurulumu. iOS 26+ AlarmKit: telefon sessizdeyken ve uygulama kapalıyken de gerçek alarm gibi çalar.
/// Daha eski iOS'ta yerel bildirim (sessiz modda çalmaz; ekranda yazar).
@MainActor
enum AlarmCenter {
    static var hasRealAlarms: Bool {
        if #available(iOS 26.0, *) { return true }
        return false
    }

    /// İzin: AlarmKit ya da bildirim
    static func requestAccess() async -> Bool {
        if #available(iOS 26.0, *) { return await KitAlarms.requestAccess() }
        return (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
    }

    /// Alarm listesini telefona kurar (öncekiler silinir)
    static func sync(_ items: [AlarmItem]) async {
        if #available(iOS 26.0, *) {
            await KitAlarms.sync(items)
            return
        }
        let center = UNUserNotificationCenter.current()
        let old = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix("alarm-") }
        center.removePendingNotificationRequests(withIdentifiers: old)
        guard await requestAccess() else { return }
        for item in items where item.enabled {
            let content = UNMutableNotificationContent()
            content.title = item.title
            content.body = item.timeText
            if let say = await AlarmSpeech.sound(for: item) ?? AlarmMusic.sound(for: item) {
                content.sound = UNNotificationSound(named: UNNotificationSoundName(say))
            } else {
                content.sound = item.rise ? UNNotificationSound(named: UNNotificationSoundName(AlarmItem.riseSound)) : .default
            }
            if item.days.isEmpty, !item.date.isEmpty {
                guard let at = item.next() else { continue }
                let c = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: at)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-0", content: content, trigger: UNCalendarNotificationTrigger(dateMatching: c, repeats: false)))
                continue
            }
            if item.days.isEmpty || (item.days.count == 7 && item.plan().days.count == 7) {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: !item.days.isEmpty)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-0", content: content, trigger: trigger))
                continue
            }
            let plan = item.plan()
            for d in plan.days {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                c.weekday = d % 7 + 1 // Calendar: pazar = 1
                let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: true)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-\(d)", content: content, trigger: trigger))
            }
            for (i, date) in plan.fixed.enumerated() {
                let c = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
                let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: false)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-f\(i)", content: content, trigger: trigger))
            }
        }
    }

    /// Süresi geçmiş "sonraki alarmı atla" işaretlerini temizler, alarmları normal düzenine döndürür; tatilde
    /// çalmayan alarm varsa planı günde bir yeniler (önümüzdeki tatiller haftalık tekrardan çıkarılır)
    static func refreshSkips(now: Date = Date()) async {
        var items = AlarmItem.load()
        let today = CalendarDay(now).iso
        let holidayPlan = items.contains { $0.enabled && (($0.skipHolidays && !$0.days.isEmpty) || $0.needsPlan) }
            && UserDefaults.standard.string(forKey: "alarm.planDay") != today
        guard holidayPlan || items.contains(where: { ($0.skipUntil ?? .distantFuture) <= now }) else { return }
        for i in items.indices where (items[i].skipUntil ?? .distantFuture) <= now { items[i].skipUntil = nil }
        AlarmItem.save(items)
        UserDefaults.standard.set(today, forKey: "alarm.planDay")
        await sync(items)
    }

    /// Zamanlayıcı ya da pomodoro bitişi: verilen anda çalar
    static func schedule(id: UUID, at date: Date, title: String) async {
        if #available(iOS 26.0, *) {
            if await KitAlarms.schedule(id: id, at: date, title: title) { return }
        }
        let content = UNMutableNotificationContent()
        content.title = title
        content.sound = .default
        let interval = max(1, date.timeIntervalSinceNow)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        try? await UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: "timer-\(id)", content: content, trigger: trigger))
    }

    static func cancel(id: UUID) {
        if #available(iOS 26.0, *) { KitAlarms.cancel(id: id) }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["timer-\(id)"])
    }
}

#if canImport(AlarmKit)
@available(iOS 26.0, *)
private struct SaatAlarmData: AlarmMetadata {}

@available(iOS 26.0, *)
@MainActor
enum KitAlarms {
    /// Bu uygulamanın kurduğu alarm kimlikleri (listeden silinenleri iptal etmek için)
    private static let idsKey = "alarmkit.ids"

    static func requestAccess() async -> Bool {
        let manager = AlarmManager.shared
        switch manager.authorizationState {
        case .authorized: return true
        case .denied: return false
        default: return ((try? await manager.requestAuthorization()) ?? .denied) == .authorized
        }
    }

    /// Erteleme ayarı: süre (dk; 0 = yok), kalan erteleme hakkı (-1 = sınırsız) ve kademeli ses
    struct Options {
        var snooze = 0
        var remaining = -1
        var rise = false
        /// Kapatma görevi ve uyanma denetimi: Durdur uygulamayı açar (AlarmStopIntent)
        var wake: PendingWakeTask?
        /// Saati söyleyen ses dosyası (Library/Sounds; AlarmSpeech)
        var soundName: String?

        init(snooze: Int = 0, remaining: Int = -1, rise: Bool = false) {
            self.snooze = snooze
            self.remaining = remaining
            self.rise = rise
        }

        init(_ item: AlarmItem) {
            self.init(snooze: item.snooze, remaining: item.snoozeMax == 0 ? -1 : item.snoozeMax, rise: item.rise)
            wake = item.wakeTask
            soundName = AlarmMusic.sound(for: item)
        }

        /// Erteleme düğmesi gösterilir mi
        var canSnooze: Bool { snooze > 0 && remaining != 0 }
    }

    private static func attributes(title: String, options: Options) -> AlarmAttributes<SaatAlarmData> {
        let snooze = options.snooze
        let stop = AlarmButton(text: LocalizedStringResource(stringLiteral: L("tools.alarm.stop")), textColor: .white, systemImageName: "stop.circle")
        let alert: AlarmPresentation.Alert
        if options.canSnooze {
            // Ertele: alarm durur, uygulama aynı adla `snooze` dakika sonrasına tek seferlik alarm kurar (SnoozeAlarmIntent)
            let again = AlarmButton(text: LocalizedStringResource(stringLiteral: T("alarm.snoozeFor", snooze)), textColor: .white,
                                    systemImageName: "zzz")
            alert = AlarmPresentation.Alert(title: LocalizedStringResource(stringLiteral: title), stopButton: stop,
                                            secondaryButton: again, secondaryButtonBehavior: .custom)
        } else {
            alert = AlarmPresentation.Alert(title: LocalizedStringResource(stringLiteral: title), stopButton: stop)
        }
        return AlarmAttributes(presentation: AlarmPresentation(alert: alert), metadata: SaatAlarmData(), tintColor: Theme.accent)
    }

    private static func config(_ schedule: Alarm.Schedule, id: UUID, title: String, options: Options) -> AlarmManager.AlarmConfiguration<SaatAlarmData> {
        AlarmManager.AlarmConfiguration<SaatAlarmData>(
            schedule: schedule, attributes: attributes(title: title, options: options),
            stopIntent: options.wake.map { AlarmStopIntent(alarmID: id.uuidString, wake: $0) },
            secondaryIntent: options.canSnooze
                ? SnoozeAlarmIntent(minutes: options.snooze, label: title, remaining: options.remaining, rise: options.rise, wake: options.wake,
                                    sound: options.soundName) : nil,
            sound: options.soundName.map { .named($0) } ?? (options.rise ? .named(AlarmItem.riseSound) : .default))
    }

    private static func weekday(_ iso: Int) -> Locale.Weekday {
        [.monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday][max(1, min(7, iso)) - 1]
    }

    static func sync(_ items: [AlarmItem]) async {
        let manager = AlarmManager.shared
        let known = Set((UserDefaults.standard.stringArray(forKey: idsKey) ?? []).compactMap(UUID.init(uuidString:)))
        for id in known { try? manager.cancel(id: id) }
        guard await requestAccess() else { return }
        var scheduled: [String] = []
        let now = Date()
        for item in items where item.enabled {
            var options = Options(item)
            if let say = await AlarmSpeech.sound(for: item) { options.soundName = say }
            let time = Alarm.Schedule.Relative.Time(hour: item.hour, minute: item.minute)
            let skipping = (item.skipUntil ?? .distantPast) > now
            if item.days.isEmpty {
                // Bir kez: atlandıysa sonraki gün
                if !item.date.isEmpty && item.next(after: now) == nil { continue }
                let schedule: Alarm.Schedule = skipping || !item.date.isEmpty
                    ? .fixed(item.next(after: now) ?? now)
                    : .relative(.init(time: time, repeats: .never))
                if (try? await manager.schedule(id: item.id, configuration: config(schedule, id: item.id, title: item.title, options: options))) != nil {
                    scheduled.append(item.id.uuidString)
                }
                continue
            }
            // Haftalık; atlanan ya da tatile denk gelen çalmanın günü haftalık tekrardan çıkar, sonraki haftaları tek
            // seferlik kurulur (uygulama açılışında ya da arka plan yenilemesinde plan yeniden kurulur)
            let plan = item.plan(now: now)
            for later in plan.fixed {
                let extra = UUID()
                if (try? await manager.schedule(id: extra, configuration: config(.fixed(later), id: extra, title: item.title, options: options))) != nil {
                    scheduled.append(extra.uuidString)
                }
            }
            let days = plan.days
            guard !days.isEmpty else { continue }
            let schedule = Alarm.Schedule.relative(.init(time: time, repeats: .weekly(days.map(weekday))))
            if (try? await manager.schedule(id: item.id, configuration: config(schedule, id: item.id, title: item.title, options: options))) != nil {
                scheduled.append(item.id.uuidString)
            }
        }
        UserDefaults.standard.set(scheduled, forKey: idsKey)
    }

    static func schedule(id: UUID, at date: Date, title: String, options: Options = Options()) async -> Bool {
        guard await requestAccess() else { return false }
        return (try? await AlarmManager.shared.schedule(id: id, configuration: config(.fixed(date), id: id, title: title, options: options))) != nil
    }

    static func cancel(id: UUID) {
        try? AlarmManager.shared.cancel(id: id)
    }
}
/// Alarm ekranındaki "ertele" düğmesi: alarm durur, aynı adla `minutes` dakika sonrasına yeni alarm kurulur.
/// `remaining` kalan erteleme hakkıdır (-1 = sınırsız); hak bitince yeni alarmda erteleme düğmesi olmaz.
@available(iOS 26.0, *)
struct SnoozeAlarmIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "Snooze"
    static let isDiscoverable = false

    @Parameter(title: "Minutes") var minutes: Int
    @Parameter(title: "Label") var label: String
    @Parameter(title: "Remaining") var remaining: Int
    @Parameter(title: "Rise") var rise: Bool
    /// Kapatma görevi ve uyanma denetimi ertelenen alarmda da sürer (JSON, PendingWakeTask; boşsa yok)
    @Parameter(title: "Wake") var wake: String
    /// Alarmın kendi ses dosyası (müzik kesiti ya da saati söyleyen ses; yoksa nil), ertelenen alarmda da çalar
    @Parameter(title: "Sound") var sound: String?

    init() {}

    init(minutes: Int, label: String, remaining: Int, rise: Bool, wake: PendingWakeTask?, sound: String? = nil) {
        self.minutes = minutes
        self.label = label
        self.remaining = remaining
        self.rise = rise
        self.wake = wake.flatMap { try? JSONEncoder().encode($0) }.map { String(decoding: $0, as: UTF8.self) } ?? ""
        self.sound = sound
    }

    func perform() async throws -> some IntentResult {
        let left = remaining < 0 ? -1 : max(0, remaining - 1)
        var options = KitAlarms.Options(snooze: minutes, remaining: left, rise: rise)
        options.wake = try? JSONDecoder().decode(PendingWakeTask.self, from: Data(wake.utf8))
        options.soundName = sound
        _ = await KitAlarms.schedule(id: UUID(), at: Date().addingTimeInterval(Double(max(1, minutes) * 60)), title: label, options: options)
        return .result()
    }
}

/// Görevli ya da denetimli alarmda "Durdur": alarm susar, uygulama açılır ve görev istenir (WakeTasks). Görev yapılmazsa
/// alarm 1 dakika sonra yeniden çalar (en çok 10 kez); yalnız denetim varsa 5 dk sonra "Uyanık mısınız?" sorulur.
@available(iOS 26.0, *)
struct AlarmStopIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "Stop"
    static let isDiscoverable = false
    static var openAppWhenRun: Bool = true

    @Parameter(title: "Alarm") var alarmID: String
    @Parameter(title: "Wake") var wake: String

    init() {}

    init(alarmID: String, wake: PendingWakeTask) {
        self.alarmID = alarmID
        self.wake = (try? JSONEncoder().encode(wake)).map { String(decoding: $0, as: UTF8.self) } ?? ""
    }

    func perform() async throws -> some IntentResult {
        let id = UUID(uuidString: alarmID)
        let p = try? JSONDecoder().decode(PendingWakeTask.self, from: Data(wake.utf8))
        await MainActor.run {
            if let id { try? AlarmManager.shared.stop(id: id) }
        }
        if let p { await WakeTasks.stopped(p) }
        return .result()
    }
}
#endif
