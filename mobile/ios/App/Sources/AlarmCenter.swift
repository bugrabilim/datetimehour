import Foundation
import SwiftUI
import UserNotifications
#if canImport(AlarmKit)
import AlarmKit
#endif

/// Alarm: saat, etiket, tekrar (bir kez / her gün / hafta içi)
struct AlarmItem: Codable, Identifiable, Hashable {
    enum Repeat: String, Codable, CaseIterable { case once, every, weekdays }
    var id = UUID()
    var hour: Int
    var minute: Int
    var label: String = ""
    var repeats: Repeat = .once
    var enabled = true

    var title: String { label.isEmpty ? L("tools.alarm.defaultLabel") : label }
    var timeText: String {
        var c = DateComponents()
        c.hour = hour
        c.minute = minute
        return Fmt.time(Calendar.current.date(from: c) ?? Date())
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
            let weekdays: [Int?] = item.repeats == .weekdays ? [2, 3, 4, 5, 6] : [nil] // Calendar: pazar = 1
            for wd in weekdays {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                c.weekday = wd
                let trigger = UNCalendarNotificationTrigger(dateMatching: c, repeats: item.repeats != .once)
                try? await center.add(UNNotificationRequest(identifier: "alarm-\(item.id)-\(wd ?? 0)", content: content, trigger: trigger))
            }
        }
    }

    /// Zamanlayıcı ya da pomodoro bitişi: verilen anda çalar
    static func schedule(id: UUID, at date: Date, title: String) async {
        if #available(iOS 26.0, *), await KitAlarms.schedule(id: id, at: date, title: title) { return }
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

    private static func attributes(title: String) -> AlarmAttributes<SaatAlarmData> {
        let stop = AlarmButton(text: LocalizedStringResource(stringLiteral: L("tools.alarm.stop")), textColor: .white, systemImageName: "stop.circle")
        let alert = AlarmPresentation.Alert(title: LocalizedStringResource(stringLiteral: title), stopButton: stop)
        return AlarmAttributes(presentation: AlarmPresentation(alert: alert), metadata: SaatAlarmData(), tintColor: Theme.accent)
    }

    static func sync(_ items: [AlarmItem]) async {
        let manager = AlarmManager.shared
        let known = Set((UserDefaults.standard.stringArray(forKey: idsKey) ?? []).compactMap(UUID.init(uuidString:)))
        for id in known { try? manager.cancel(id: id) }
        guard await requestAccess() else { return }
        var scheduled: [String] = []
        for item in items where item.enabled {
            let time = Alarm.Schedule.Relative.Time(hour: item.hour, minute: item.minute)
            let recurrence: Alarm.Schedule.Relative.Recurrence
            switch item.repeats {
            case .once: recurrence = .never
            case .every: recurrence = .weekly([.monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday])
            case .weekdays: recurrence = .weekly([.monday, .tuesday, .wednesday, .thursday, .friday])
            }
            let schedule = Alarm.Schedule.relative(.init(time: time, repeats: recurrence))
            let config = AlarmManager.AlarmConfiguration<SaatAlarmData>(schedule: schedule, attributes: attributes(title: item.title))
            if (try? await manager.schedule(id: item.id, configuration: config)) != nil { scheduled.append(item.id.uuidString) }
        }
        UserDefaults.standard.set(scheduled, forKey: idsKey)
    }

    static func schedule(id: UUID, at date: Date, title: String) async -> Bool {
        guard await requestAccess() else { return false }
        let config = AlarmManager.AlarmConfiguration<SaatAlarmData>(schedule: .fixed(date), attributes: attributes(title: title))
        return (try? await AlarmManager.shared.schedule(id: id, configuration: config)) != nil
    }

    static func cancel(id: UUID) {
        try? AlarmManager.shared.cancel(id: id)
    }
}
#endif
