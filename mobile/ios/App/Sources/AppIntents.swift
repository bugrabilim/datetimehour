import AppIntents
import Foundation
import SaatCore

/* Siri ve Kısayollar (rakip analizi #7): zamanlayıcı başlat, alarm kur, tam ekran saati aç (isteğe bağlı gece modu;
   "şarja takınca" otomasyonu için), sıradaki namaz vakti ve bugünün tarihi (miladi + hicri). Metinler Localizable'da
   (intent.*), Siri ifadeleri AppShortcuts.xcstrings'te (gen-strings.mjs "siri.phrase.*"; buradaki İngilizce ifadeler
   app-strings.json'daki "en" değerleriyle birebir aynı olmalı, gen-strings denetler). */

struct StartTimerIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.timer.title"
    static var description = IntentDescription("intent.timer.desc")

    @Parameter(title: "intent.timer.minutes", requestValueDialog: IntentDialog("intent.timer.ask"))
    var minutes: Int

    @Parameter(title: "intent.timer.name")
    var name: String?

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let minutes = max(1, min(1440, self.minutes))
        let ms = Int64(minutes) * 60_000
        var t = TimerItem(name: (name ?? "").trimmingCharacters(in: .whitespaces), total: ms)
        t.end = TimerItem.nowMs + ms
        var items = TimerItem.load()
        items.append(t)
        TimerItem.save(items)
        if let id = UUID(uuidString: t.id) {
            await AlarmCenter.schedule(id: id, at: Date(timeIntervalSince1970: Double(t.end) / 1000), title: t.doneTitle)
        }
        BadgeStore.shared.hit(.zamanlayici)
        return .result(dialog: IntentDialog(LocalizedStringResource(stringLiteral: T("intent.timer.done", minutes))))
    }
}

struct SetAlarmIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.alarm.title"
    static var description = IntentDescription("intent.alarm.desc")

    @Parameter(title: "intent.alarm.time", requestValueDialog: IntentDialog("intent.alarm.ask"))
    var time: Date

    @Parameter(title: "intent.alarm.label")
    var label: String?

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let c = Calendar.current.dateComponents([.hour, .minute], from: time)
        var item = AlarmItem(hour: c.hour ?? 7, minute: c.minute ?? 0)
        item.label = (label ?? "").trimmingCharacters(in: .whitespaces)
        var items = AlarmItem.load()
        items.append(item)
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
        BadgeStore.shared.hit(.alarm)
        return .result(dialog: IntentDialog(LocalizedStringResource(stringLiteral: T("intent.alarm.done", item.timeText))))
    }
}

/// Uygulamayı tam ekran saatle açar (Kısayollar › Otomasyon › Şarj cihazı bağlandığında gibi)
struct OpenFullscreenClockIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.full.title"
    static var description = IntentDescription("intent.full.desc")
    static var openAppWhenRun: Bool = true

    @Parameter(title: "intent.full.night", default: false)
    var night: Bool

    @MainActor
    func perform() async throws -> some IntentResult {
        if night { UserDefaults.standard.set("on", forKey: NightMode.key) }
        Router.shared.openFullscreen()
        return .result()
    }
}

struct NextPrayerIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.prayer.title"
    static var description = IntentDescription("intent.prayer.desc")

    @MainActor
    func perform() async throws -> some IntentResult & ReturnsValue<String> & ProvidesDialog {
        let place = AppPlace.stored(), now = Date()
        guard let next = place.schedule.next(after: now) else {
            return .result(value: place.name, dialog: IntentDialog(LocalizedStringResource(stringLiteral: place.name)))
        }
        let left = Int(next.at.timeIntervalSince(now).rounded(.up))
        let text = T("intent.prayer.answer", place.name, next.prayer.localizedName, place.hm(next.at), Fmt.hm(seconds: left))
        return .result(value: text, dialog: IntentDialog(LocalizedStringResource(stringLiteral: text)))
    }
}

struct TodayDateIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.date.title"
    static var description = IntentDescription("intent.date.desc")

    @MainActor
    func perform() async throws -> some IntentResult & ReturnsValue<String> & ProvidesDialog {
        let now = Date()
        let text = [Fmt.longDate(now), Hijri.date(now)?.text].compactMap { $0 }.joined(separator: " · ")
        return .result(value: text, dialog: IntentDialog(LocalizedStringResource(stringLiteral: text)))
    }
}

struct SaatTarihShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(intent: StartTimerIntent(), phrases: [
            "Start a timer in \(.applicationName)",
            "Set a \(.applicationName) timer",
        ], shortTitle: "siri.short.timer", systemImageName: "timer")
        AppShortcut(intent: SetAlarmIntent(), phrases: [
            "Set an alarm in \(.applicationName)",
        ], shortTitle: "siri.short.alarm", systemImageName: "alarm")
        AppShortcut(intent: OpenFullscreenClockIntent(), phrases: [
            "Show the clock in \(.applicationName)",
            "Open the full screen clock in \(.applicationName)",
        ], shortTitle: "siri.short.full", systemImageName: "clock")
        AppShortcut(intent: NextPrayerIntent(), phrases: [
            "When is the next prayer in \(.applicationName)",
            "Next prayer time in \(.applicationName)",
        ], shortTitle: "siri.short.prayer", systemImageName: "moon.stars")
        AppShortcut(intent: TodayDateIntent(), phrases: [
            "What is the date in \(.applicationName)",
            "What is the Hijri date in \(.applicationName)",
        ], shortTitle: "siri.short.date", systemImageName: "calendar")
    }
}
