import SwiftUI
import SaatCore

/* Uyku (30 Ekim 2026, rakip analizi #17): uyku planı ve yatma hatırlatması, 90 dakikalık döngülere göre saat önerileri,
   uyku sesleri (telefonda üretilen gürültü, kapanma süresiyle) ve gün doğumu ışığı (alarm ayarı; tam ekran saatte).
   Uyku takibi bilerek yok: mikrofon ve hareket algılayıcısı kullanılmaz. Kayıtlar yalnız telefonda (sleep.*). */

enum SleepPlan {
    static let onKey = "sleep.on", wakeKey = "sleep.wake", durationKey = "sleep.dur", daysKey = "sleep.days", remindKey = "sleep.remind"
    static let remindChoices = [0, 15, 30, 60]

    private static var d: UserDefaults { .standard }
    static var on: Bool { d.bool(forKey: onKey) }
    /// Kalkış (gece yarısından dakika; varsayılan 07.00)
    static var wake: Int { d.object(forKey: wakeKey) as? Int ?? 420 }
    /// Uyku süresi (dakika; varsayılan 8 saat)
    static var duration: Int { d.object(forKey: durationKey) as? Int ?? 480 }
    /// Kalkış günleri (ISO 1 pazartesi … 7 pazar; varsayılan her gün)
    static var days: [Int] { parse(d.string(forKey: daysKey)) }
    /// Hatırlatma yatma saatinden kaç dakika önce (varsayılan 30)
    static var remind: Int { d.object(forKey: remindKey) as? Int ?? 30 }

    static func parse(_ raw: String?) -> [Int] {
        (raw ?? "1,2,3,4,5,6,7").split(separator: ",").compactMap { Int($0) }.filter { (1...7).contains($0) }
    }

    /// Günün dakikası → "23:00" (telefonun saat biçimiyle)
    static func hm(_ minute: Int) -> String {
        let m = Sleep.mod(minute)
        return Fmt.time(Calendar.current.date(bySettingHour: m / 60, minute: m % 60, second: 0, of: Date()) ?? Date())
    }

    /// Döngü süresi saat olarak: "7,5" (Arapçada da Latin rakamı)
    static func hours(_ minutes: Int) -> String {
        return (Double(minutes) / 60).formatted(.number.precision(.fractionLength(0...1)).locale(AppLocale.numberLocale))
    }

    /// "8 sa 30 dk"
    static func durationText(_ minutes: Int) -> String {
        T("sleep.durationFmt", minutes / 60, minutes % 60)
    }

    /// Yatma hatırlatmaları: kalkış günü D için yatma = D'nin kalkış saati − uyku süresi, hatırlatma ondan `remind` dk önce
    @MainActor static func reminders(now: Date, days count: Int) -> [PlannedNotification] {
        guard on, !days.isEmpty else { return [] }
        let cal = Calendar.current
        let start = cal.startOfDay(for: now)
        return (0...count).compactMap { i -> PlannedNotification? in
            guard let day = cal.date(byAdding: .day, value: i, to: start),
                  days.contains(AlarmItem.isoWeekday(day)),
                  let wakeAt = cal.date(bySettingHour: wake / 60, minute: wake % 60, second: 0, of: day) else { return nil }
            let bed = wakeAt.addingTimeInterval(-Double(duration) * 60)
            let fire = bed.addingTimeInterval(-Double(remind) * 60)
            guard fire > now else { return nil }
            return PlannedNotification(id: "\(FeatureNotifier.featurePrefix)bed-\(BadgeStore.day(day))", fireAt: fire,
                                       title: L(remind == 0 ? "sleep.n.now" : "sleep.n.soon"),
                                       body: T("sleep.n.body", hm(wake - duration), hm(wake), durationText(duration)))
        }
    }
}

/// Uyku seslerinin durumu (ekranda kalan süre için)
enum SleepSounds {
    static let kindKey = "sleep.noise", volumeKey = "sleep.vol", timerKey = "sleep.timer"
    static let timerChoices = [15, 30, 45, 60, 90, 0]

    @MainActor static var playing: Bool { NoisePlayer.shared.source == "sleep" }

    @MainActor
    static func play() {
        let d = UserDefaults.standard
        let kind = d.string(forKey: kindKey) ?? "rain"
        let vol = d.object(forKey: volumeKey) as? Int ?? 35
        let minutes = d.object(forKey: timerKey) as? Int ?? 30
        NoisePlayer.shared.play(kind: kind, volume: vol, until: minutes > 0 ? Date().addingTimeInterval(Double(minutes) * 60) : nil,
                                source: "sleep", fadeOut: true)
    }
}

struct SleepView: View {
    @AppStorage(SleepPlan.onKey) private var on = false
    @AppStorage(SleepPlan.wakeKey) private var wake = 420
    @AppStorage(SleepPlan.durationKey) private var duration = 480
    @AppStorage(SleepPlan.daysKey) private var daysRaw = "1,2,3,4,5,6,7"
    @AppStorage(SleepPlan.remindKey) private var remind = 30
    @AppStorage(NotifyFeature.bedtime.key) private var bedtimeNotify = true
    @AppStorage(SleepSounds.kindKey) private var noise = "rain"
    @AppStorage(SleepSounds.volumeKey) private var volume = 35
    @AppStorage(SleepSounds.timerKey) private var timer = 30
    @State private var alarmAdded = false

    var body: some View {
        Form {
            Section {
                Toggle(L("sleep.plan.use"), isOn: $on).tint(Theme.accent)
                if on {
                    DatePicker(L("sleep.wake"), selection: wakeDate, displayedComponents: .hourAndMinute)
                    Stepper(value: $duration, in: 300...660, step: 15) {
                        HStack {
                            Text(L("sleep.duration"))
                            Spacer()
                            Text(SleepPlan.durationText(duration)).monospacedDigit().foregroundStyle(Theme.muted)
                        }
                    }
                    dayButtons
                    HStack {
                        Text(L("sleep.bedtime")).font(.headline)
                        Spacer()
                        Text(SleepPlan.hm(wake - duration)).font(.title3.weight(.semibold)).monospacedDigit().foregroundStyle(Theme.accent)
                    }
                    Toggle(L("sleep.remind"), isOn: $bedtimeNotify).tint(Theme.accent)
                    if bedtimeNotify {
                        Picker(L("sleep.remindWhen"), selection: $remind) {
                            ForEach(SleepPlan.remindChoices, id: \.self) { m in
                                Text(m == 0 ? L("sleep.remindAt") : T("sleep.remindBefore", m)).tag(m)
                            }
                        }
                    }
                    Button(L("sleep.addAlarm")) { addAlarm() }
                    if alarmAdded {
                        Label(L("sleep.alarmAdded"), systemImage: "checkmark.circle").foregroundStyle(Theme.muted).font(.footnote)
                    }
                }
            } header: {
                Text(L("sleep.plan"))
            } footer: {
                Text(L("sleep.plan.note"))
            }

            Section {
                let now = Calendar.current.dateComponents([.hour, .minute], from: Date())
                Text(L("sleep.ifNow")).font(.subheadline.weight(.semibold))
                ForEach(Array(Sleep.wakeTimes(now: (now.hour ?? 0) * 60 + (now.minute ?? 0)).enumerated()), id: \.offset) { _, c in
                    cycleRow(SleepPlan.hm(c.minute), c.cycles)
                }
                Text(T("sleep.forWake", SleepPlan.hm(wake))).font(.subheadline.weight(.semibold))
                ForEach(Array(Sleep.bedtimes(wake: wake).enumerated()), id: \.offset) { _, c in
                    cycleRow(SleepPlan.hm(c.minute), c.cycles)
                }
            } header: {
                Text(L("sleep.cycles"))
            } footer: {
                Text(L("sleep.cycles.note"))
            }

            // Akıllı alarm (SmartSleep): pencereli sıradaki alarm için uyku oturumu
            SmartSleepSection()

            Section {
                NoiseControls(kind: $noise, volume: $volume, allowOff: false)
                Picker(L("sleep.timer"), selection: $timer) {
                    ForEach(SleepSounds.timerChoices, id: \.self) { m in
                        Text(m == 0 ? L("sleep.timerOff") : T("alarm.snoozeMin", m)).tag(m)
                    }
                }
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    let playing = SleepSounds.playing
                    Button {
                        if playing { NoisePlayer.shared.stop(source: "sleep") } else { SleepSounds.play() }
                    } label: {
                        HStack {
                            Label(L(playing ? "sleep.stop" : "sleep.play"), systemImage: playing ? "stop.fill" : "play.fill")
                            Spacer()
                            if playing, let until = NoisePlayer.shared.until {
                                Text(clock(Int(until.timeIntervalSince(ctx.date).rounded(.up))))
                                    .monospacedDigit().foregroundStyle(Theme.muted)
                            }
                        }
                    }
                }
            } header: {
                Text(L("sleep.sounds"))
            } footer: {
                Text(L("sleep.sounds.note"))
            }

            Section {
                Text(L("sleep.sunriseInfo")).font(.subheadline)
            } header: {
                Text(L("alarm.sunrise"))
            }

            Section {
                Text(L("sleep.privacy")).font(.footnote).foregroundStyle(Theme.muted)
            }
        }
        .themedBackground()
        .navigationTitle(L("sleep.title"))
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: [on, bedtimeNotify]) { reschedule() }
        .onChange(of: [wake, duration, remind]) { reschedule() }
        .onChange(of: daysRaw) { reschedule() }
        .onChange(of: noise) { if SleepSounds.playing { SleepSounds.play() } }
        .onChange(of: volume) { if SleepSounds.playing { NoisePlayer.shared.setVolume(volume) } }
    }

    private var wakeDate: Binding<Date> {
        Binding {
            Calendar.current.date(bySettingHour: wake / 60, minute: wake % 60, second: 0, of: Date()) ?? Date()
        } set: { date in
            let c = Calendar.current.dateComponents([.hour, .minute], from: date)
            wake = (c.hour ?? 7) * 60 + (c.minute ?? 0)
        }
    }

    private var dayButtons: some View {
        let days = SleepPlan.parse(daysRaw)
        return HStack(spacing: 6) {
            ForEach(1...7, id: \.self) { d in
                let sel = days.contains(d)
                Button {
                    let next = sel ? days.filter { $0 != d } : (days + [d]).sorted()
                    daysRaw = next.map(String.init).joined(separator: ",")
                } label: {
                    Text(L("tools.alarm.dayNames.\(d - 1)"))
                        .font(.footnote.weight(.semibold))
                        .frame(maxWidth: .infinity, minHeight: 36)
                        .background(sel ? Theme.accent : Theme.surface2, in: Capsule())
                        .foregroundStyle(sel ? Color.white : Theme.text)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(sel ? .isSelected : [])
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    private func cycleRow(_ time: String, _ cycles: Int) -> some View {
        HStack {
            Text(time).font(.title3.weight(.semibold)).monospacedDigit()
            Spacer()
            Text(T("sleep.cycleRow", cycles, SleepPlan.hours(cycles * Sleep.cycle)))
                .font(.subheadline).foregroundStyle(Theme.muted)
        }
    }

    private func addAlarm() {
        var item = AlarmItem(hour: wake / 60, minute: wake % 60)
        item.label = L("sleep.alarmLabel")
        item.days = SleepPlan.parse(daysRaw)
        var items = AlarmItem.load()
        items.append(item)
        AlarmItem.save(items)
        alarmAdded = true
        Task { await AlarmCenter.sync(items) }
    }

    private func reschedule() {
        Task {
            if on, bedtimeNotify, await PrayerNotifier.authorizationStatus() == .notDetermined {
                _ = await PrayerNotifier.requestAuthorization()
            }
            await PrayerNotifier.reschedule()
        }
    }
}
