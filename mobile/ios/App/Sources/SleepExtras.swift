import AVFoundation
import EventKit
import EventKitUI
import SaatCore
import SwiftUI

/// Tatil modu (2. analiz #44; Android'deki AlarmPause ile aynı anahtarlar): `alarm.pause.from` / `.to` (ISO, ikisi de
/// dahil) arasında tekrarlayan alarmlar çalmaz; bir kezlik alarmlar etkilenmez.
enum AlarmPause {
    static let fromKey = "alarm.pause.from", toKey = "alarm.pause.to"

    static var range: (CalendarDay, CalendarDay)? {
        let d = UserDefaults.standard
        guard let a = CalendarDay(iso: d.string(forKey: fromKey) ?? ""), let b = CalendarDay(iso: d.string(forKey: toKey) ?? ""), a <= b else { return nil }
        return (a, b)
    }

    static func contains(_ t: Date) -> Bool {
        guard let (a, b) = range else { return false }
        let day = CalendarDay(t)
        return day >= a && day <= b
    }

    static var active: Bool { range.map { CalendarDay(Date()) <= $0.1 } ?? false }

    static func save(_ from: CalendarDay?, _ to: CalendarDay?) {
        UserDefaults.standard.set(from?.iso ?? "", forKey: fromKey)
        UserDefaults.standard.set(to?.iso ?? "", forKey: toKey)
    }
}

/// Uyku düzeni kaydı (2. analiz #47): iPhone'da kalkış, alarm çaldıktan sonra uygulama açılınca alarmın çaldığı an
/// (MorningCheck) ya da alarm görevi bitince; yatış uyku seslerinin ya da akıllı alarmın başlatıldığı an (18–04 arası).
/// Kayıt `sleep.log` (SleepStats.Entry), bekleyen yatış `sleep.bedAt` (1970'ten sn). Android'deki SleepLog ile aynı.
enum SleepLogStore {
    static let key = "sleep.log", bedKey = "sleep.bedAt"

    static func load() -> [SleepStats.Entry] {
        guard let s = UserDefaults.standard.string(forKey: key), let d = s.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([SleepStats.Entry].self, from: d)) ?? []
    }

    static func save(_ l: [SleepStats.Entry]) {
        if let d = try? JSONEncoder().encode(l) { UserDefaults.standard.set(String(data: d, encoding: .utf8), forKey: key) }
    }

    static func clear() {
        UserDefaults.standard.removeObject(forKey: key)
        UserDefaults.standard.removeObject(forKey: bedKey)
    }

    static func markBed(_ at: Date = Date()) {
        let h = Calendar.current.component(.hour, from: at)
        if h >= 18 || h < 4 { UserDefaults.standard.set(at.timeIntervalSince1970, forKey: bedKey) }
    }

    static func recordWake(_ at: Date = Date()) {
        let c = Calendar.current.dateComponents([.hour, .minute], from: at)
        let h = c.hour ?? 0
        guard h >= 3 && h < 13 else { return }
        let bedAt = UserDefaults.standard.double(forKey: bedKey)
        var b = -1
        if bedAt > 0, at.timeIntervalSince1970 - bedAt > 0, at.timeIntervalSince1970 - bedAt < 16 * 3600 {
            let bc = Calendar.current.dateComponents([.hour, .minute], from: Date(timeIntervalSince1970: bedAt))
            b = (bc.hour ?? 0) * 60 + (bc.minute ?? 0)
        }
        save(SleepStats.record(load(), SleepStats.Entry(d: CalendarDay(at).iso, w: h * 60 + (c.minute ?? 0), b: b)))
        UserDefaults.standard.removeObject(forKey: bedKey)
    }
}

/// El feneri (2. analiz #41): iPhone'da alarm sistemin ekranında çaldığı için yalnız uygulamadaki alarm görevi ekranında
@MainActor
enum Torch {
    static var available: Bool { AVCaptureDevice.default(for: .video)?.hasTorch == true }

    static func set(_ on: Bool) {
        guard let d = AVCaptureDevice.default(for: .video), d.hasTorch, (try? d.lockForConfiguration()) != nil else { return }
        d.torchMode = on ? .on : .off
        d.unlockForConfiguration()
    }
}

struct TorchButton: View {
    @State private var on = false
    var body: some View {
        if Torch.available {
            Button {
                on.toggle()
                Torch.set(on)
            } label: {
                Label(L("alarm.torch"), systemImage: on ? "flashlight.on.fill" : "flashlight.off.fill")
            }
            .buttonStyle(.bordered)
            .tint(on ? Theme.accent : Theme.muted)
            .onDisappear { Torch.set(false) }
        }
    }
}

/// Uyku planının kalkış alarmı (2. analiz #52): tek alarm, `sleepPlan` işaretli; plan değişince saati ve günleri güncellenir
@MainActor
enum SleepPlanAlarm {
    static func set(wake: Int, days: [Int]) async {
        var items = AlarmItem.load()
        if let i = items.firstIndex(where: \.sleepPlan) {
            items[i].hour = wake / 60
            items[i].minute = wake % 60
            items[i].days = days
            items[i].enabled = true
        } else {
            var a = AlarmItem(hour: wake / 60, minute: wake % 60)
            a.label = L("sleep.alarmLabel")
            a.days = days
            a.sleepPlan = true
            items.append(a)
        }
        items.sort { ($0.hour, $0.minute) < ($1.hour, $1.minute) }
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
    }

    static func update(wake: Int, days: [Int]) async {
        guard let a = AlarmItem.load().first(where: \.sleepPlan), a.hour * 60 + a.minute != wake || a.days != days else { return }
        await set(wake: wake, days: days)
    }
}

extension SleepPlan {
    /// Uyku alışkanlıkları (2. analiz #49): yatmadan 8 sa önce kafein, 1 sa önce ekran, 30 dk önce akşam rutini
    @MainActor static func habits(now: Date, days count: Int) -> [PlannedNotification] {
        guard on, !days.isEmpty else { return [] }
        let cal = Calendar.current
        let start = cal.startOfDay(for: now)
        return (0...count).flatMap { i -> [PlannedNotification] in
            guard let day = cal.date(byAdding: .day, value: i, to: start), days.contains(AlarmItem.isoWeekday(day)),
                  let wakeAt = cal.date(bySettingHour: wake / 60, minute: wake % 60, second: 0, of: day) else { return [] }
            let bed = wakeAt.addingTimeInterval(-Double(duration) * 60)
            return [("caf", 480, "habit.caffeine"), ("scr", 60, "habit.screen"), ("rou", 30, "habit.routine")].compactMap { k, before, key in
                let at = bed.addingTimeInterval(-Double(before) * 60)
                guard at > now else { return nil }
                return PlannedNotification(id: "\(FeatureNotifier.featurePrefix)habits-\(k)-\(BadgeStore.day(day))", fireAt: at,
                                           title: L(key), body: L("notify.cat.habits"))
            }
        }
    }
}

/// Uyku düzeni kartı (2. analiz #47)
struct SleepStatsCard: View {
    @State private var log = SleepLogStore.load()

    var body: some View {
        let s = SleepStats.summary(log, today: CalendarDay(Date()).iso, goal: SleepPlan.duration)
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("sleepstats.title"))
            if s.weekWake == nil && s.spread == nil {
                Text(L("sleepstats.empty")).font(.subheadline).foregroundStyle(Theme.muted)
            } else {
                if let w = s.weekWake {
                    HStack {
                        Text(L("sleepstats.avgWake")).foregroundStyle(Theme.muted)
                        Spacer()
                        Text(String(format: "%02d:%02d", w / 60, w % 60)).monospacedDigit()
                    }
                    if let p = s.prevWake {
                        let d = w - p
                        Text(d < 0 ? T("sleepstats.earlier", -d) : d > 0 ? T("sleepstats.later", d) : L("sleepstats.same"))
                            .font(.caption).foregroundStyle(Theme.muted)
                    }
                }
                if let sp = s.spread { Text(T("sleepstats.spread", sp)).font(.subheadline) }
                if let debt = s.debt { Text(debt > 0 ? T("sleepstats.debt", Fmt.hm(seconds: debt * 60)) : L("sleepstats.noDebt")).font(.subheadline) }
                Button(L("sleepstats.clear"), role: .destructive) { SleepLogStore.clear(); log = [] }.font(.footnote)
            }
            Text(L("sleepstats.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }
}

/// Kronotip testi kartı (2. analiz #48); girdiler kaydedilmez
struct ChronotypeCard: View {
    @State private var times = [1380, 420, 30, 540]

    var body: some View {
        let m = Chronotype.msfsc(workBed: times[0], workWake: times[1], freeBed: times[2], freeWake: times[3])
        let mm = (m + 1440) % 1440
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("chrono.title"))
            ForEach(Array(["chrono.workBed", "chrono.workWake", "chrono.freeBed", "chrono.freeWake"].enumerated()), id: \.offset) { i, k in
                DatePicker(L(k), selection: binding(i), displayedComponents: .hourAndMinute)
            }
            Text(L("chrono.c\(Chronotype.category(m))")).font(.title3.weight(.bold)).foregroundStyle(Theme.accent)
            Text(T("chrono.result", String(format: "%02d:%02d", mm / 60, mm % 60))).font(.subheadline)
            Text(L("chrono.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }

    private func binding(_ i: Int) -> Binding<Date> {
        Binding(get: {
            Calendar.current.date(bySettingHour: times[i] / 60, minute: times[i] % 60, second: 0, of: Date()) ?? Date()
        }, set: { d in
            let c = Calendar.current.dateComponents([.hour, .minute], from: d)
            times[i] = (c.hour ?? 0) * 60 + (c.minute ?? 0)
        })
    }
}

/// Enerji blokları (2. analiz #49): kalkıştan sonra yaklaşık sabah zirvesi, öğleden sonra düşüş ve akşam zirvesi;
/// "Takvime ekle" sistemin etkinlik düzenleyicisini açar (izin gerekmez; kaydetmek kullanıcının elinde)
struct EnergyBlocksCard: View {
    @State private var draft: EnergyDraft?

    struct EnergyDraft: Identifiable {
        let id = UUID()
        let title: String
        let start: Date
        let end: Date
    }

    var body: some View {
        let wake = SleepPlan.wake
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("energy.title"))
            ForEach([("energy.morning", 150, 300), ("energy.dip", 420, 540), ("energy.evening", 600, 720)], id: \.0) { k, a, b in
                let from = (wake + a) % 1440, to = (wake + b) % 1440
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L(k)).font(.subheadline.weight(.semibold))
                        Text(String(format: "%02d:%02d – %02d:%02d", from / 60, from % 60, to / 60, to % 60)).font(.caption).monospacedDigit()
                            .foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Button(L("energy.add")) {
                        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Calendar.current.startOfDay(for: Date())) ?? Date()
                        let s = tomorrow.addingTimeInterval(Double(from) * 60)
                        draft = EnergyDraft(title: L(k), start: s, end: s.addingTimeInterval(Double(b - a) * 60))
                    }
                    .font(.footnote)
                }
            }
            Text(L("energy.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
        .sheet(item: $draft) { d in
            EnergyEventEditor(title: d.title, start: d.start, end: d.end)
        }
    }
}

/// Sistemin etkinlik düzenleyicisi (izin istemez; kaydedilen etkinlik kullanıcının seçtiği takvime gider)
private struct EnergyEventEditor: UIViewControllerRepresentable {
    let title: String
    let start: Date
    let end: Date

    func makeUIViewController(context: Context) -> EKEventEditViewController {
        let store = EKEventStore()
        let vc = EKEventEditViewController()
        vc.eventStore = store
        let e = EKEvent(eventStore: store)
        e.title = title
        e.startDate = start
        e.endDate = end
        vc.event = e
        vc.editViewDelegate = context.coordinator
        return vc
    }

    func updateUIViewController(_ vc: EKEventEditViewController, context: Context) {}
    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, EKEventEditViewDelegate {
        func eventEditViewController(_ controller: EKEventEditViewController, didCompleteWith action: EKEventEditViewAction) {
            controller.dismiss(animated: true)
        }
    }
}

/// Listede büyük "sıradaki alarm X sa Y dk sonra" kartı (2. analiz #51)
struct AlarmListHeadCard: View {
    let items: [AlarmItem]

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { ctx in
            if let next = items.filter(\.enabled).compactMap({ $0.next(after: ctx.date) }).min() {
                VStack(alignment: .leading, spacing: 4) {
                    let mins = Int((next.timeIntervalSince(ctx.date) / 60).rounded(.up))
                    Text(T("alarm.nextIn", Fmt.hm(seconds: mins * 60))).font(AppFonts.cardTitle).foregroundStyle(Theme.text)
                    Text(Fmt.time(next) + " · " + Fmt.longDate(next)).font(.subheadline).foregroundStyle(Theme.muted)
                }
            }
        }
    }
}

/// Tatil modu bölümü (2. analiz #44)
struct VacationSection: View {
    @State private var on = AlarmPause.active
    @State private var from = AlarmPause.range?.0.noon(in: .current) ?? Date()
    @State private var to = AlarmPause.range?.1.noon(in: .current) ?? Date().addingTimeInterval(6 * 86400)
    let onChange: () -> Void

    var body: some View {
        Section {
            Toggle(L("alarm.pause"), isOn: $on).tint(Theme.accent)
            if on {
                DatePicker(L("focus.schedFrom"), selection: $from, displayedComponents: .date)
                DatePicker(L("focus.schedTo"), selection: $to, in: from..., displayedComponents: .date)
                Text(T("alarm.pausedRange", Fmt.dayMonth(from), Fmt.dayMonth(to))).font(.caption.weight(.semibold)).foregroundStyle(Theme.accent)
            }
        } footer: {
            Text(L("alarm.pauseNote"))
        }
        .onChange(of: on) { save() }
        .onChange(of: from) { save() }
        .onChange(of: to) { save() }
    }

    private func save() {
        if on { AlarmPause.save(CalendarDay(from), CalendarDay(max(from, to))) } else { AlarmPause.save(nil, nil) }
        onChange()
    }
}

/// Nottaki etiketler (2. analiz #50): "#kahve" (harf ve rakam), küçük harfe çevrilmiş
enum JournalTags {
    static func parse(_ note: String) -> Set<String> {
        guard let re = try? NSRegularExpression(pattern: "#([\\p{L}\\p{N}_]{1,30})") else { return [] }
        let ns = note as NSString
        return Set(re.matches(in: note, range: NSRange(location: 0, length: ns.length)).map {
            ns.substring(with: $0.range(at: 1)).lowercased(with: AppLocale.locale)
        })
    }
}
