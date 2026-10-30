import Foundation
import SwiftUI
import UserNotifications
import AppIntents
#if canImport(AlarmKit)
import AlarmKit
#endif

/// Alarm: saat, etiket, günler, erteleme süresi, "sonraki alarmı atla" (30 Ekim 2026; eski kayıtlardaki
/// `repeats` [bir kez / her gün / hafta içi] okunurken günlere çevrilir). Günler ISO: 1 pazartesi … 7 pazar.
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

    static let snoozeChoices = [0, 5, 10, 15, 20, 30]
    static let weekdays = [1, 2, 3, 4, 5]

    init(hour: Int, minute: Int) {
        self.hour = hour
        self.minute = minute
    }

    private enum CodingKeys: String, CodingKey { case id, hour, minute, label, days, enabled, snooze, skipUntil }
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

    /// Sıradaki çalma (atlanan çalma varsa ondan sonraki)
    func next(after now: Date = Date()) -> Date? {
        occurrence(after: max(now, skipUntil ?? .distantPast))
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
            content.sound = .default
            if item.days.isEmpty || item.days.count == 7 {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: !item.days.isEmpty)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-0", content: content, trigger: trigger))
                continue
            }
            for d in item.days {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                c.weekday = d % 7 + 1 // Calendar: pazar = 1
                let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: true)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-\(d)", content: content, trigger: trigger))
            }
        }
    }

    /// Süresi geçmiş "sonraki alarmı atla" işaretlerini temizler, alarmları normal düzenine döndürür
    static func refreshSkips(now: Date = Date()) async {
        var items = AlarmItem.load()
        guard items.contains(where: { ($0.skipUntil ?? .distantFuture) <= now }) else { return }
        for i in items.indices where (items[i].skipUntil ?? .distantFuture) <= now { items[i].skipUntil = nil }
        AlarmItem.save(items)
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

    private static func attributes(title: String, snooze: Int) -> AlarmAttributes<SaatAlarmData> {
        let stop = AlarmButton(text: LocalizedStringResource(stringLiteral: L("tools.alarm.stop")), textColor: .white, systemImageName: "stop.circle")
        let alert: AlarmPresentation.Alert
        if snooze > 0 {
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

    private static func config(_ schedule: Alarm.Schedule, title: String, snooze: Int) -> AlarmManager.AlarmConfiguration<SaatAlarmData> {
        AlarmManager.AlarmConfiguration<SaatAlarmData>(
            schedule: schedule, attributes: attributes(title: title, snooze: snooze),
            secondaryIntent: snooze > 0 ? SnoozeAlarmIntent(minutes: snooze, label: title) : nil)
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
                if (try? await manager.schedule(id: item.id, configuration: config(schedule, title: item.title, snooze: item.snooze))) != nil {
                    scheduled.append(item.id.uuidString)
                }
                continue
            }
            // Haftalık; atlanan çalmanın günü bu hafta dışarıda kalır, bir hafta sonrası tek seferlik eklenir
            // (atlama geçince uygulama açılışında ya da arka plan yenilemesinde normal hâline döner)
            var days = item.days
            if skipping, let skipped = item.occurrence(after: now), skipped < item.skipUntil! {
                let wd = AlarmItem.isoWeekday(skipped)
                days.removeAll { $0 == wd }
                if let later = Calendar.current.date(byAdding: .day, value: 7, to: skipped) {
                    let extra = UUID()
                    if (try? await manager.schedule(id: extra, configuration: config(.fixed(later), title: item.title, snooze: item.snooze))) != nil {
                        scheduled.append(extra.uuidString)
                    }
                }
            }
            guard !days.isEmpty else { continue }
            let schedule = Alarm.Schedule.relative(.init(time: time, repeats: .weekly(days.map(weekday))))
            if (try? await manager.schedule(id: item.id, configuration: config(schedule, title: item.title, snooze: item.snooze))) != nil {
                scheduled.append(item.id.uuidString)
            }
        }
        UserDefaults.standard.set(scheduled, forKey: idsKey)
    }

    static func schedule(id: UUID, at date: Date, title: String, snooze: Int = 0) async -> Bool {
        guard await requestAccess() else { return false }
        return (try? await AlarmManager.shared.schedule(id: id, configuration: config(.fixed(date), title: title, snooze: snooze))) != nil
    }

    static func cancel(id: UUID) {
        try? AlarmManager.shared.cancel(id: id)
    }
}
/// Alarm ekranındaki "ertele" düğmesi: alarm durur, aynı adla `minutes` dakika sonrasına yeni alarm kurulur
@available(iOS 26.0, *)
struct SnoozeAlarmIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "Snooze"
    static let isDiscoverable = false

    @Parameter(title: "Minutes") var minutes: Int
    @Parameter(title: "Label") var label: String

    init() {}

    init(minutes: Int, label: String) {
        self.minutes = minutes
        self.label = label
    }

    func perform() async throws -> some IntentResult {
        _ = await KitAlarms.schedule(id: UUID(), at: Date().addingTimeInterval(Double(max(1, minutes) * 60)), title: label, snooze: minutes)
        return .result()
    }
}
#endif
