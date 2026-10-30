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

    static let snoozeChoices = [0, 5, 10, 15, 20, 30]
    static let snoozeMaxChoices = [0, 1, 2, 3, 5]
    static let sunriseChoices = [0, 10, 20, 30]
    static let riseSound = "alarm-rise.caf"
    static let weekdays = [1, 2, 3, 4, 5]

    init(hour: Int, minute: Int) {
        self.hour = hour
        self.minute = minute
    }

    private enum CodingKeys: String, CodingKey { case id, hour, minute, label, days, enabled, snooze, skipUntil, snoozeMax, rise, skipHolidays, sunrise, task, taskLevel, code, wakeCheck }
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

    /// Durdurulunca istenecek görev ve denetim (görev ya da denetim yoksa nil)
    var wakeTask: PendingWakeTask? {
        guard task != "none" || wakeCheck else { return nil }
        let t = task == "code" && code.isEmpty ? "math" : task
        return PendingWakeTask(task: t, level: taskLevel, code: code, wakeCheck: wakeCheck, title: title, rise: rise, rings: 0, repeatID: nil)
    }
    var timeText: String {
        var c = DateComponents()
        c.hour = hour
        c.minute = minute
        return Fmt.time(Calendar.current.date(from: c) ?? Date())
    }

    /// "Bir kez", "Her gün", "Hafta içi" ya da gün kısaltmaları
    var repeatText: String {
        let d = Set(days)
        if d.isEmpty { return L("tools.alarm.once") }
        if d.count == 7 { return L("alarm.everyDay") }
        if d == Set(Self.weekdays) { return L("tools.alarm.weekdays") }
        return days.sorted().map { L("tools.alarm.dayNames.\($0 - 1)") }.joined(separator: " ")
    }

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
        for add in 0...8 {
            guard let day = cal.date(byAdding: .day, value: add, to: date), let t = at(day), t > date else { continue }
            if days.isEmpty || days.contains(Self.isoWeekday(t)) { return t }
        }
        return nil
    }

    /// `date` öncesindeki son çalma (günaydın kartı için)
    func occurrence(before date: Date) -> Date? {
        let cal = Calendar.current
        for back in 0...8 {
            guard let day = cal.date(byAdding: .day, value: -back, to: date), let t = at(day), t <= date else { continue }
            if days.isEmpty || days.contains(Self.isoWeekday(t)) { return t }
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

    static let storeKey = "alarms"
    static func load() -> [AlarmItem] {
        guard let data = UserDefaults.standard.data(forKey: storeKey) else { return [] }
        return (try? JSONDecoder().decode([AlarmItem].self, from: data)) ?? []
    }
    static func save(_ items: [AlarmItem]) {
        UserDefaults.standard.set(try? JSONEncoder().encode(items), forKey: storeKey)
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
            content.sound = item.rise ? UNNotificationSound(named: UNNotificationSoundName(AlarmItem.riseSound)) : .default
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
        let holidayPlan = items.contains { $0.enabled && $0.skipHolidays && !$0.days.isEmpty }
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

        init(snooze: Int = 0, remaining: Int = -1, rise: Bool = false) {
            self.snooze = snooze
            self.remaining = remaining
            self.rise = rise
        }

        init(_ item: AlarmItem) {
            self.init(snooze: item.snooze, remaining: item.snoozeMax == 0 ? -1 : item.snoozeMax, rise: item.rise)
            wake = item.wakeTask
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
                ? SnoozeAlarmIntent(minutes: options.snooze, label: title, remaining: options.remaining, rise: options.rise, wake: options.wake) : nil,
            sound: options.rise ? .named(AlarmItem.riseSound) : .default)
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
            let time = Alarm.Schedule.Relative.Time(hour: item.hour, minute: item.minute)
            let skipping = (item.skipUntil ?? .distantPast) > now
            if item.days.isEmpty {
                // Bir kez: atlandıysa sonraki gün
                let schedule: Alarm.Schedule = skipping
                    ? .fixed(item.next(after: now) ?? now)
                    : .relative(.init(time: time, repeats: .never))
                if (try? await manager.schedule(id: item.id, configuration: config(schedule, id: item.id, title: item.title, options: Options(item)))) != nil {
                    scheduled.append(item.id.uuidString)
                }
                continue
            }
            // Haftalık; atlanan ya da tatile denk gelen çalmanın günü haftalık tekrardan çıkar, sonraki haftaları tek
            // seferlik kurulur (uygulama açılışında ya da arka plan yenilemesinde plan yeniden kurulur)
            let plan = item.plan(now: now)
            for later in plan.fixed {
                let extra = UUID()
                if (try? await manager.schedule(id: extra, configuration: config(.fixed(later), id: extra, title: item.title, options: Options(item)))) != nil {
                    scheduled.append(extra.uuidString)
                }
            }
            let days = plan.days
            guard !days.isEmpty else { continue }
            let schedule = Alarm.Schedule.relative(.init(time: time, repeats: .weekly(days.map(weekday))))
            if (try? await manager.schedule(id: item.id, configuration: config(schedule, id: item.id, title: item.title, options: Options(item)))) != nil {
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

    init() {}

    init(minutes: Int, label: String, remaining: Int, rise: Bool, wake: PendingWakeTask?) {
        self.minutes = minutes
        self.label = label
        self.remaining = remaining
        self.rise = rise
        self.wake = wake.flatMap { try? JSONEncoder().encode($0) }.map { String(decoding: $0, as: UTF8.self) } ?? ""
    }

    func perform() async throws -> some IntentResult {
        let left = remaining < 0 ? -1 : max(0, remaining - 1)
        var options = KitAlarms.Options(snooze: minutes, remaining: left, rise: rise)
        options.wake = try? JSONDecoder().decode(PendingWakeTask.self, from: Data(wake.utf8))
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
        if let id = UUID(uuidString: alarmID) { try? AlarmManager.shared.stop(id: id) }
        if let p = try? JSONDecoder().decode(PendingWakeTask.self, from: Data(wake.utf8)) { await WakeTasks.stopped(p) }
        return .result()
    }
}
#endif
