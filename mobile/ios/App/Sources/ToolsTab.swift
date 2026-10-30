import SwiftUI
import Combine
import SaatCore

/// Araçlar sekmesi: alarm, zamanlayıcı, kronometre, pomodoro, tarih hesaplama;
/// altta ayarlar ve hakkında (gizlilik ve e-posta listesi Hakkında sayfasında) (dünya saatleri ve mevsimler alt barda, 18 Ekim 2026)
struct ToolsTab: View {
    @State private var router = Router.shared

    var body: some View {
        NavigationStack(path: $router.toolsPath) {
            List {
                Section {
                    row(.alarm, L("tools.hub.cards.alarm.name"), "alarm")
                    row(.timer, L("tools.timer"), "timer")
                    row(.stopwatch, L("tools.hub.cards.stopwatch.name"), "stopwatch")
                    row(.pomodoro, L("tools.hub.cards.pomodoro.name"), "leaf")
                    row(.sleep, L("sleep.title"), "bed.double")
                    row(.datecalc, L("tools.hub.cards.datecalc.name"), "calendar.badge.clock")
                }
                Section {
                    // Rozetler: girişin yanında bir satırlık not (Bumba standardı madde 13)
                    NavigationLink(value: ToolRoute.badges) {
                        VStack(alignment: .leading, spacing: 3) {
                            Label(L("badges.h1"), systemImage: "rosette")
                            Text(L("badges.note")).font(.caption).foregroundStyle(Theme.muted)
                        }
                    }
                }
                Section {
                    row(.settings, L("settings.title"), "gearshape")
                    row(.about, L("settings.about"), "info.circle")
                }
            }
            .themedBackground()
            .navigationTitle(L("tab.tools"))
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: ToolRoute.self) { route in
                switch route {
                case .alarm: AlarmsView()
                case .timer: TimerView()
                case .stopwatch: StopwatchView()
                case .pomodoro: PomodoroView()
                case .sleep: SleepView()
                case .datecalc: DateCalcView()
                case .settings: SettingsView()
                case .about: AboutView()
                case .badges: BadgesView()
                }
            }
        }
    }

    private func row(_ route: ToolRoute, _ title: String, _ icon: String) -> some View {
        NavigationLink(value: route) { Label(title, systemImage: icon) }
    }
}

// MARK: Ortak

/// Büyük süre göstergesi
struct BigTime: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 64, weight: .semibold, design: .rounded))
            .monospacedDigit()
            .minimumScaleFactor(0.4)
            .lineLimit(1)
            .foregroundStyle(Theme.text)
            .environment(\.layoutDirection, .leftToRight)
            .frame(maxWidth: .infinity)
    }
}

struct RoundButton: View {
    let title: String
    var prominent = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 50)
        }
        .buttonStyle(.borderedProminent)
        .tint(prominent ? Theme.accent : Theme.muted.opacity(0.35))
        .foregroundStyle(prominent ? .white : Theme.text)
    }
}

/// Saniyelik süre: "1:02:03" ya da "02:03"
func clock(_ seconds: Int) -> String {
    let s = max(0, seconds)
    return s >= 3600 ? String(format: "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60) : String(format: "%02d:%02d", s / 60, s % 60)
}

// MARK: Kronometre

struct StopwatchView: View {
    /// Çalışırken başlangıç anı (0 = durdu); toplam ve turlar kalıcı (uygulama kapansa da süre kaybolmaz)
    @AppStorage("sw.start") private var start = 0.0
    @AppStorage("sw.acc") private var acc = 0.0
    @AppStorage("sw.laps") private var lapsRaw = ""

    private var laps: [Double] { lapsRaw.split(separator: ",").compactMap { Double($0) } }
    private func elapsed(_ now: Date) -> Double { acc + (start > 0 ? now.timeIntervalSinceReferenceDate - start : 0) }

    var body: some View {
        TimelineView(.animation(minimumInterval: 0.05)) { context in
            let t = elapsed(context.date)
            VStack(spacing: 20) {
                BigTime(text: format(t))
                    .padding(.top, 30)
                HStack(spacing: 12) {
                    if start > 0 {
                        RoundButton(title: L("tools.stopwatch.lap")) { lapsRaw = (laps + [t]).map { String($0) }.joined(separator: ",") }
                        RoundButton(title: L("tools.stopwatch.stop"), prominent: true) {
                            acc = t
                            start = 0
                        }
                    } else {
                        RoundButton(title: L("tools.stopwatch.reset")) {
                            acc = 0
                            lapsRaw = ""
                        }
                        .disabled(t == 0)
                        RoundButton(title: L(t > 0 ? "tools.stopwatch.resume" : "tools.stopwatch.start"), prominent: true) {
                            start = Date().timeIntervalSinceReferenceDate
                            BadgeStore.shared.hit(.kronometre)
                        }
                    }
                }
                lapList(current: t)
            }
            .padding(.horizontal, 20)
        }
        .background(Theme.bg.ignoresSafeArea())
        // Kilit ekranı / Dynamic Island: çalışan kronometre
        .onChange(of: start) { LiveActivities.sync() }
        .navigationTitle(L("tools.hub.cards.stopwatch.name"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func format(_ t: Double) -> String {
        let cs = Int((t * 100).rounded(.down))
        let s = cs / 100
        return clock(s) + String(format: ".%02d", cs % 100)
    }

    @ViewBuilder
    private func lapList(current: Double) -> some View {
        let totals = laps
        let lapTimes = totals.enumerated().map { i, v in v - (i > 0 ? totals[i - 1] : 0) }
        let best = lapTimes.count > 1 ? lapTimes.min() : nil
        let worst = lapTimes.count > 1 ? lapTimes.max() : nil
        List {
            if lapTimes.isEmpty {
                Text(L("tools.stopwatch.empty")).foregroundStyle(Theme.muted)
            }
            ForEach(Array(lapTimes.enumerated()).reversed(), id: \.offset) { i, lap in
                HStack {
                    Text(verbatim: "\(L("tools.stopwatch.lapNo")) \(i + 1)")
                    if lap == best { Text(L("tools.stopwatch.best")).font(.caption).foregroundStyle(.green) }
                    if lap == worst { Text(L("tools.stopwatch.worst")).font(.caption).foregroundStyle(.red) }
                    Spacer()
                    Text(format(lap)).monospacedDigit()
                    Text(format(totals[i])).monospacedDigit().foregroundStyle(Theme.muted)
                }
                .environment(\.layoutDirection, .leftToRight)
            }
        }
        .themedBackground()
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
}

// MARK: Zamanlayıcı ve tarihe geri sayım

struct TimerView: View {
    /// Aralık antrenmanı sürüyorsa o sekme açılır
    @State private var mode = UserDefaults.standard.double(forKey: "hiit.start") > 0 ? 1 : 0
    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $mode) {
                Text(L("tools.countdown.tabTimer")).tag(0)
                Text(L("hiit.tab")).tag(1)
                Text(L("tools.countdown.tabDate")).tag(2)
            }
            .pickerStyle(.segmented)
            .padding(16)
            switch mode {
            case 0: TimersView()
            case 1: HiitView()
            default: CountdownsView()
            }
            Spacer(minLength: 0)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("tools.timer"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Alarm

struct AlarmsView: View {
    @State private var items = AlarmItem.load()
    @State private var editing: AlarmItem?
    @State private var denied = false

    var body: some View {
        List {
            if items.isEmpty {
                Text(L("tools.alarm.none")).foregroundStyle(Theme.muted)
            }
            ForEach($items) { $item in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.timeText)
                            .font(.system(size: 40, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                        Text(item.title + " · " + item.repeatText)
                            .font(.subheadline)
                            .foregroundStyle(Theme.muted)
                        if let skip = item.skipUntil, skip > Date(), let at = item.unskipped.next() {
                            Text(T("alarm.skipped", Fmt.longDate(at)))
                                .font(.caption)
                                .foregroundStyle(Theme.accent)
                        }
                        if item.enabled, item.holidayShifted(), let at = item.next() {
                            Text(T("alarm.holidayNext", Fmt.longDate(at)))
                                .font(.caption)
                                .foregroundStyle(Theme.muted)
                        }
                    }
                    Spacer()
                    Toggle(L("tools.alarm.enable"), isOn: $item.enabled).labelsHidden()
                }
                .contentShape(Rectangle())
                .onTapGesture { editing = item }
                .swipeActions(edge: .leading) {
                    if item.enabled { skipButton($item) }
                }
                .contextMenu {
                    if item.enabled { skipButton($item) }
                }
            }
            .onDelete { items.remove(atOffsets: $0) }
            Section {
                if denied { Text(L("alarm.denied")).foregroundStyle(.red) }
                AlarmNote()
            }
        }
        .themedBackground()
        .navigationTitle(L("tools.hub.cards.alarm.name"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    let now = Calendar.current.dateComponents([.hour, .minute], from: Date())
                    editing = AlarmItem(hour: now.hour ?? 7, minute: now.minute ?? 0)
                } label: { Image(systemName: "plus") }
                .accessibilityLabel(L("tools.alarm.add"))
            }
        }
        .sheet(item: $editing) { item in
            AlarmEditor(item: item) { saved in
                if let i = items.firstIndex(where: { $0.id == saved.id }) { items[i] = saved } else { items.append(saved) }
                BadgeStore.shared.hit(.alarm)
                items.sort { ($0.hour, $0.minute) < ($1.hour, $1.minute) }
            }
        }
        .onChange(of: items) {
            AlarmItem.save(items)
            Task {
                await AlarmCenter.sync(items)
                let allowed = await AlarmCenter.requestAccess()
                denied = !allowed && items.contains { $0.enabled }
            }
        }
    }
}

extension AlarmsView {
    /// "Sonraki alarmı atla" ya da geri al
    @ViewBuilder fileprivate func skipButton(_ item: Binding<AlarmItem>) -> some View {
        let now = Date()
        if let skip = item.wrappedValue.skipUntil, skip > now {
            Button { item.wrappedValue.skipUntil = nil } label: { Label(L("alarm.unskip"), systemImage: "arrow.uturn.backward") }
        } else if let next = item.wrappedValue.unskipped.next(after: now) {
            Button { item.wrappedValue.skipUntil = next.addingTimeInterval(60) } label: { Label(L("alarm.skip"), systemImage: "forward.end") }
                .tint(Theme.accent)
        }
    }
}

private struct AlarmEditor: View {
    @Environment(\.dismiss) private var dismiss
    @State var item: AlarmItem
    let onSave: (AlarmItem) -> Void
    @State private var time = Date()
    @State private var scanning = false

    var body: some View {
        NavigationStack {
            Form {
                DatePicker(L("tools.alarm.timeLabel"), selection: $time, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .frame(maxWidth: .infinity)
                TextField(L("tools.alarm.labelPlaceholder"), text: $item.label)
                Section(L("alarm.days")) {
                    // Pazartesiden pazara yedi düğme; hiçbiri seçili değilse bir kez çalar
                    HStack(spacing: 6) {
                        ForEach(1...7, id: \.self) { d in
                            let on = item.days.contains(d)
                            Button {
                                if on { item.days.removeAll { $0 == d } } else { item.days.append(d); item.days.sort() }
                            } label: {
                                Text(L("tools.alarm.dayNames.\(d - 1)"))
                                    .font(.footnote.weight(.semibold))
                                    .frame(maxWidth: .infinity, minHeight: 36)
                                    .background(on ? Theme.accent : Theme.surface2, in: Capsule())
                                    .foregroundStyle(on ? Color.white : Theme.text)
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(on ? .isSelected : [])
                        }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                    Text(item.repeatText).font(.footnote).foregroundStyle(Theme.muted)
                }
                Section {
                    Picker(L("alarm.snooze"), selection: $item.snooze) {
                        ForEach(AlarmItem.snoozeChoices, id: \.self) { m in
                            Text(m == 0 ? L("alarm.snoozeOff") : T("alarm.snoozeMin", m)).tag(m)
                        }
                    }
                    if item.snooze > 0 {
                        Picker(L("alarm.snoozeMax"), selection: $item.snoozeMax) {
                            ForEach(AlarmItem.snoozeMaxChoices, id: \.self) { n in
                                Text(n == 0 ? L("alarm.snoozeMax.none") : T("alarm.snoozeMax.n", n)).tag(n)
                            }
                        }
                    }
                }
                Section {
                    Toggle(L("alarm.rise"), isOn: $item.rise)
                        .tint(Theme.accent)
                } footer: {
                    Text(L("alarm.riseNoteIOS"))
                }
                // Gün doğumu ışığı (rakip analizi #17): tam ekran saat açıkken alarmdan önce ekran yavaşça aydınlanır
                Section {
                    Picker(L("alarm.sunrise"), selection: $item.sunrise) {
                        ForEach(AlarmItem.sunriseChoices, id: \.self) { m in
                            Text(m == 0 ? L("alarm.snoozeOff") : T("alarm.sunriseMin", m)).tag(m)
                        }
                    }
                } footer: {
                    Text(L("alarm.sunriseNote"))
                }
                // Kapatma görevi ve uyanma denetimi (rakip analizi #13)
                Section {
                    Picker(L("alarm.task"), selection: $item.task) {
                        ForEach(WakeTask.kinds, id: \.self) { k in Text(L("alarm.task.\(k)")).tag(k) }
                    }
                    if item.task == "math" {
                        Picker(L("alarm.task.level"), selection: $item.taskLevel) {
                            ForEach(1...3, id: \.self) { n in Text(L("alarm.task.level.\(n)")).tag(n) }
                        }
                    }
                    if item.task == "code" {
                        if CodeScannerView.available {
                            Button(L(item.code.isEmpty ? "alarm.code.save" : "alarm.code.change")) { scanning = true }
                        } else {
                            Text(L("wake.noCamera")).font(.footnote).foregroundStyle(Theme.muted)
                        }
                        Text(L(item.code.isEmpty ? "alarm.code.missing" : "alarm.code.saved")).font(.footnote).foregroundStyle(Theme.muted)
                        Text(L("alarm.code.hint")).font(.footnote).foregroundStyle(Theme.muted)
                    }
                    Toggle(L("alarm.wakeCheck"), isOn: $item.wakeCheck).tint(Theme.accent)
                } header: {
                    Text(L("alarm.task.heading"))
                } footer: {
                    Text(L(AlarmCenter.hasRealAlarms ? "alarm.task.noteIOS" : "alarm.task.oldIOS") + " " + L("alarm.wakeCheck.note"))
                }
                // Yalnız tekrarlayan alarmda: Türkiye'deki tam gün resmî tatillerde çalmaz
                if !item.days.isEmpty {
                    Section {
                        Toggle(L("alarm.skipHolidays"), isOn: $item.skipHolidays)
                            .tint(Theme.accent)
                    } footer: {
                        Text(L("alarm.skipHolidaysNote"))
                    }
                }
            }
            .themedBackground()
            .sheet(isPresented: $scanning) {
                CodeScannerView { value in
                    item.code = value
                    scanning = false
                }
                .ignoresSafeArea()
            }
            .navigationTitle(L("alarm.new"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("alarm.cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("alarm.save")) {
                        let c = Calendar.current.dateComponents([.hour, .minute], from: time)
                        item.hour = c.hour ?? 0
                        item.minute = c.minute ?? 0
                        item.enabled = true
                        item.skipUntil = nil
                        if item.days.isEmpty { item.skipHolidays = false }
                        onSave(item)
                        dismiss()
                    }
                }
            }
            .onAppear {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                time = Calendar.current.date(from: c) ?? Date()
            }
        }
    }
}

/// Alarmın nasıl çalacağı (iOS sürümüne göre)
private struct AlarmNote: View {
    var body: some View {
        Text(L(AlarmCenter.hasRealAlarms ? "alarm.realNote" : "alarm.fallbackNote"))
            .font(.footnote)
            .foregroundStyle(Theme.muted)
            .padding(.horizontal, 4)
    }
}

// MARK: Pomodoro

struct PomodoroView: View {
    @AppStorage("pomo.work") private var workMin = 25
    @AppStorage("pomo.short") private var shortMin = 5
    @AppStorage("pomo.long") private var longMin = 15
    @AppStorage("pomo.rounds") private var rounds = 4
    @AppStorage("pomo.phase") private var phase = "work"
    @AppStorage("pomo.round") private var round = 1
    @AppStorage("pomo.end") private var end = 0.0
    @AppStorage("pomo.left") private var left = 0.0
    @AppStorage("pomo.done") private var done = 0
    @AppStorage("pomo.id") private var alarmID = ""
    // Görev adı, istatistik ve arka plan sesi (rakip analizi #16; sitedeki pomodoroyla aynı)
    @AppStorage("pomo.task") private var task = ""
    @AppStorage("pomo.noise") private var noise = "off"
    @AppStorage("pomo.vol") private var volume = 40
    @State private var log = PomoLog.load()

    private var phaseSeconds: Double { Double((phase == "work" ? workMin : phase == "short" ? shortMin : longMin) * 60) }
    private var running: Bool { end > 0 }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let remaining = running ? max(0, end - context.date.timeIntervalSinceReferenceDate) : (left > 0 ? left : phaseSeconds)
            ScrollView {
                VStack(spacing: 18) {
                    Text(L("tools.pomodoro.\(phase)"))
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(phase == "work" ? Theme.accent : CalendarData.color("religious"))
                        .padding(.top, 24)
                    BigTime(text: clock(Int(remaining.rounded(.up))))
                    Text(T("tools.pomodoro.round", round, rounds)).foregroundStyle(Theme.muted)
                    HStack(spacing: 12) {
                        RoundButton(title: L("tools.pomodoro.skip")) { advance() }
                        if running {
                            RoundButton(title: L("tools.pomodoro.pause"), prominent: true) {
                                left = remaining
                                end = 0
                                cancelAlarm()
                            }
                        } else {
                            RoundButton(title: L(left > 0 ? "tools.pomodoro.resume" : "tools.pomodoro.start"), prominent: true) {
                                start(seconds: left > 0 ? left : phaseSeconds)
                            }
                        }
                    }
                    Text(T("tools.pomodoro.completed", done)).font(.footnote).foregroundStyle(Theme.muted)
                    Button(L("tools.pomodoro.reset")) {
                        cancelAlarm()
                        end = 0; left = 0; phase = "work"; round = 1; done = 0
                    }
                    .tint(Theme.accent)
                    VStack(alignment: .leading, spacing: 8) {
                        Text(L("tools.pomodoro.taskLabel")).font(.subheadline).foregroundStyle(Theme.muted)
                        TextField(L("tools.pomodoro.taskPlaceholder"), text: Binding(get: { task }, set: { task = String($0.prefix(60)) }))
                            .textFieldStyle(.roundedBorder)
                            .submitLabel(.done)
                    }
                    PomoTodoCard(current: $task).id(log.count) // tur bitince sayaçlar yeniden okunur
                    VStack(alignment: .leading, spacing: 8) {
                        SectionTitle(text: L("tools.pomodoro.noise"))
                        NoiseControls(kind: $noise, volume: $volume)
                        Text(L("pomo.noiseNote")).font(.caption).foregroundStyle(Theme.muted)
                    }
                    .card()
                    FocusStatsCard(log: log) {
                        PomoLog.save([])
                        log = []
                    }
                    Form {
                        Section(L("tools.pomodoro.settings")) {
                            Stepper("\(L("tools.pomodoro.workMin")): \(workMin)", value: $workMin, in: 5...90, step: 5)
                            Stepper("\(L("tools.pomodoro.shortMin")): \(shortMin)", value: $shortMin, in: 1...30)
                            Stepper("\(L("tools.pomodoro.longMin")): \(longMin)", value: $longMin, in: 5...60, step: 5)
                            Stepper("\(L("tools.pomodoro.roundsLabel")): \(rounds)", value: $rounds, in: 2...8)
                        }
                    }
                    .themedBackground()
                    .frame(height: 280)
                    .scrollDisabled(true)
                }
                .padding(.horizontal, 20)
            }
            .onChange(of: running && remaining <= 0) { _, finished in
                if finished { advance(completed: true) }
            }
        }
        .background(Theme.bg.ignoresSafeArea())
        // Kilit ekranı / Dynamic Island: çalışan pomodoro aşaması; arka plan sesi çalışma süresince
        .onChange(of: end) { LiveActivities.sync(); PomoNoise.sync() }
        .onChange(of: phase) { PomoNoise.sync() }
        .onChange(of: noise) { PomoNoise.sync() }
        .onChange(of: volume) { if NoisePlayer.shared.source == "pomo" { NoisePlayer.shared.setVolume(volume) } }
        .onAppear { log = PomoLog.load(); PomoNoise.sync() }
        .navigationTitle(L("tools.hub.cards.pomodoro.name"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func start(seconds: Double) {
        let id = UUID()
        alarmID = id.uuidString
        end = Date().timeIntervalSinceReferenceDate + seconds
        left = 0
        let title = T("tools.pomodoro.phaseDone", L("tools.pomodoro.\(phase)"))
        let at = Date(timeIntervalSinceReferenceDate: end)
        Task { await AlarmCenter.schedule(id: id, at: at, title: title) }
    }

    private func cancelAlarm() {
        if let id = UUID(uuidString: alarmID) { AlarmCenter.cancel(id: id) }
        alarmID = ""
    }

    /// Sonraki aşamaya geçer (duraklatılmış başlar)
    private func advance(completed: Bool = false) {
        if !completed { cancelAlarm() }
        if phase == "work" {
            if completed {
                done += 1
                BadgeStore.shared.hit(.pomodoro) // çalışma süresi sonuna kadar tamamlandı
                // Odak geçmişi: atlanan tur sayılmaz (sitedeki gibi)
                PomoLog.record(minutes: workMin, task: task, at: end > 0 ? Date(timeIntervalSinceReferenceDate: end) : Date())
                PomoTodo.save(PomoTodo.credit(PomoTodo.load(), task: task))
                log = PomoLog.load()
            }
            phase = round >= rounds ? "long" : "short"
        } else {
            if phase == "long" { round = 1 } else { round += 1 }
            phase = "work"
        }
        end = 0
        left = 0
    }
}

// MARK: Tarih hesaplama

struct DateCalcView: View {
    @State private var start = Date()
    @State private var finish = Date().addingTimeInterval(30 * 86400)
    @State private var base = Date()
    @State private var amount = 30
    @State private var unit = "days"
    @State private var birth = Calendar.current.date(byAdding: .year, value: -30, to: Date()) ?? Date()

    private let off = CalendarData.shared.offDays

    var body: some View {
        let today = CalendarDay(Date())
        Form {
            Section(L("datecalc.diffHeading")) {
                DatePicker(L("datecalc.startLabel"), selection: $start, displayedComponents: .date)
                DatePicker(L("datecalc.endLabel"), selection: $finish, displayedComponents: .date)
                let a = min(CalendarDay(start), CalendarDay(finish))
                let b = max(CalendarDay(start), CalendarDay(finish))
                let n = DateCalc.days(from: a, to: b)
                let r = DateCalc.ymd(from: a, to: b)
                Text(T("datecalc.diffDays", num(n))).font(.headline)
                Text(T("datecalc.diffWeeks", n / 7, n % 7))
                Text(T("datecalc.diffYmd", r.years, r.months, r.days))
                Text(T("datecalc.diffWork", workText(DateCalc.workdays(from: a, to: b, offDays: off))))
                Text(L("datecalc.diffNote")).font(.footnote).foregroundStyle(Theme.muted)
            }
            Section(L("datecalc.addHeading")) {
                DatePicker(L("datecalc.startLabel"), selection: $base, displayedComponents: .date)
                Stepper("\(L("datecalc.amountLabel")): \(amount)", value: $amount, in: -3650...3650)
                Picker(L("datecalc.unitLabel"), selection: $unit) {
                    ForEach(["days", "weeks", "months", "years", "workdays"], id: \.self) { Text(L("datecalc.units.\($0)")).tag($0) }
                }
                Text(T("datecalc.addResult", Fmt.day(added(CalendarDay(base))))).font(.headline)
                Text(L("datecalc.addHint")).font(.footnote).foregroundStyle(Theme.muted)
            }
            Section(L("datecalc.ageHeading")) {
                DatePicker(L("datecalc.birthLabel"), selection: $birth, in: ...Date(), displayedComponents: .date)
                let b = CalendarDay(birth)
                let r = DateCalc.ymd(from: b, to: today)
                let next = DateCalc.nextBirthday(b, today: today)
                Text(T("datecalc.ageResult", r.years, r.months, r.days)).font(.headline)
                Text(T("datecalc.ageDays", num(DateCalc.days(from: b, to: today))))
                if next == today {
                    Text(L("datecalc.ageToday"))
                } else {
                    Text(T("datecalc.ageNext", Fmt.day(next), DateCalc.days(from: today, to: next)))
                }
            }
            Section {
                Text(T("datecalc.range", CalendarData.shared.yearRange.lowerBound, CalendarData.shared.yearRange.upperBound))
                    .font(.footnote)
                    .foregroundStyle(Theme.muted)
            }
        }
        .themedBackground()
        .navigationTitle(L("tools.hub.cards.datecalc.name"))
        .navigationBarTitleDisplayMode(.inline)
        // Rozet: kullanıcı bir hesap girdisini değiştirince
        .onChange(of: [start, finish, base, birth]) { BadgeStore.shared.hit(.tarihHesap) }
        .onChange(of: amount) { BadgeStore.shared.hit(.tarihHesap) }
        .onChange(of: unit) { BadgeStore.shared.hit(.tarihHesap) }
    }

    private func workText(_ w: Double) -> String {
        w == w.rounded() ? num(Int(w)) : String(format: "%.1f", w).replacingOccurrences(of: ".", with: AppLocale.code == "en" ? "." : ",")
    }

    private func added(_ d: CalendarDay) -> CalendarDay {
        switch unit {
        case "weeks": return d.adding(days: amount * 7)
        case "months": return d.adding(months: amount)
        case "years": return d.adding(months: amount * 12)
        case "workdays": return DateCalc.adding(workdays: amount, to: d, offDays: off)
        default: return d.adding(days: amount)
        }
    }
}
