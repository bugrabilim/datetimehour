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
private struct BigTime: View {
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

private struct RoundButton: View {
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
private func clock(_ seconds: Int) -> String {
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
    @State private var mode = 0
    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $mode) {
                Text(L("tools.countdown.tabTimer")).tag(0)
                Text(L("tools.countdown.tabDate")).tag(1)
            }
            .pickerStyle(.segmented)
            .padding(16)
            if mode == 0 { DurationTimer() } else { DateCountdown() }
            Spacer(minLength: 0)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("tools.timer"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct DurationTimer: View {
    @AppStorage("tm.end") private var end = 0.0      // çalışırken bitiş anı
    @AppStorage("tm.left") private var left = 0.0    // duraklatılmışken kalan
    @AppStorage("tm.id") private var alarmID = ""
    @State private var h = 0
    @State private var m = 5
    @State private var s = 0

    private var running: Bool { end > 0 }
    private var paused: Bool { end == 0 && left > 0 }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.5)) { context in
            let remaining = running ? max(0, end - context.date.timeIntervalSinceReferenceDate) : left
            VStack(spacing: 20) {
                if running || paused {
                    BigTime(text: remaining <= 0 ? L("tools.countdown.finished") : clock(Int(remaining.rounded(.up))))
                        .padding(.top, 30)
                } else {
                    HStack(spacing: 0) {
                        wheel($h, 0..<24, L("tools.countdown.hoursShort"))
                        wheel($m, 0..<60, L("tools.countdown.minutesShort"))
                        wheel($s, 0..<60, L("tools.countdown.secondsShort"))
                    }
                    .frame(height: 180)
                    .environment(\.layoutDirection, .leftToRight)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach([1, 3, 5, 10, 15, 25, 30, 45, 60], id: \.self) { n in
                                Button(T("tools.countdown.presetMin", n)) { h = n / 60; m = n % 60; s = 0; start(seconds: Double(n * 60)) }
                                    .buttonStyle(.bordered)
                                    .tint(Theme.accent)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                HStack(spacing: 12) {
                    if running {
                        RoundButton(title: L("tools.countdown.reset")) { reset() }
                        RoundButton(title: L("tools.countdown.pause"), prominent: true) {
                            left = remaining
                            end = 0
                            cancelAlarm()
                        }
                    } else if paused {
                        RoundButton(title: L("tools.countdown.reset")) { reset() }
                        RoundButton(title: L("tools.countdown.resume"), prominent: true) { start(seconds: left) }
                    } else {
                        RoundButton(title: L("tools.countdown.start"), prominent: true) { start(seconds: Double(h * 3600 + m * 60 + s)) }
                            .disabled(h + m + s == 0)
                    }
                }
                .padding(.horizontal, 20)
                if running && remaining <= 0 {
                    Text(L("tools.countdown.finished")).font(.headline).foregroundStyle(Theme.accent)
                }
                AlarmNote()
            }
        }
    }

    private func wheel(_ value: Binding<Int>, _ range: Range<Int>, _ unit: String) -> some View {
        Picker(unit, selection: value) {
            ForEach(range, id: \.self) { Text("\($0) \(unit)").tag($0) }
        }
        .pickerStyle(.wheel)
        .frame(maxWidth: .infinity)
    }

    private func start(seconds: Double) {
        guard seconds > 0 else { return }
        BadgeStore.shared.hit(.zamanlayici)
        let id = UUID()
        alarmID = id.uuidString
        end = Date().timeIntervalSinceReferenceDate + seconds
        left = 0
        let at = Date(timeIntervalSinceReferenceDate: end)
        Task { await AlarmCenter.schedule(id: id, at: at, title: L("tools.countdown.finished")) }
    }

    private func cancelAlarm() {
        if let id = UUID(uuidString: alarmID) { AlarmCenter.cancel(id: id) }
        alarmID = ""
    }

    private func reset() {
        cancelAlarm()
        end = 0
        left = 0
    }
}

private struct DateCountdown: View {
    @AppStorage("cd.target") private var target = 0.0
    @AppStorage("cd.title") private var title = ""
    @State private var picked = Date().addingTimeInterval(86400)

    var body: some View {
        let today = CalendarDay(Date())
        let quick = quickTargets(today)
        TimelineView(.periodic(from: .now, by: 1)) { context in
            ScrollView {
                VStack(spacing: 18) {
                    if target > 0 {
                        let left = Int((Date(timeIntervalSinceReferenceDate: target).timeIntervalSince(context.date)).rounded(.up))
                        VStack(spacing: 6) {
                            if !title.isEmpty { Text(title).font(.headline).foregroundStyle(Theme.accent) }
                            BigTime(text: left <= 0 ? L("tools.countdown.finishedDate") : Fmt.countdown(left))
                            Text(Fmt.longDate(Date(timeIntervalSinceReferenceDate: target)) + " " + Fmt.time(Date(timeIntervalSinceReferenceDate: target)))
                                .font(.subheadline)
                                .foregroundStyle(Theme.muted)
                        }
                        .padding(.top, 20)
                    }
                    DatePicker(L("tools.countdown.targetLabel"), selection: $picked, in: Date()...)
                    Button(L("tools.countdown.targetSet")) {
                        target = picked.timeIntervalSinceReferenceDate
                        title = ""
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    VStack(alignment: .leading, spacing: 8) {
                        SectionTitle(text: L("tools.countdown.quickHeading"))
                        ForEach(Array(quick.enumerated()), id: \.offset) { _, pair in
                            Button {
                                target = pair.1.noon(in: .current).addingTimeInterval(-12 * 3600).timeIntervalSinceReferenceDate
                                title = pair.0
                            } label: {
                                HStack {
                                    Text(pair.0).foregroundStyle(Theme.text)
                                    Spacer()
                                    Text(relative(pair.1, today)).font(.caption).foregroundStyle(Theme.muted)
                                }
                                .card()
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    /// Yeni yıl, Ramazan, bayramlar, 23 Nisan, 19 Mayıs, 30 Ağustos, 29 Ekim: her birinin sıradaki tarihi
    private func quickTargets(_ today: CalendarDay) -> [(String, CalendarDay)] {
        let keys = ["newYear", "ramadanStart", "rb1", "kb1", "apr23", "may19", "aug30", "oct29"]
        let events = CalendarData.shared.years.keys.sorted().flatMap { CalendarData.shared.years[$0]!.events }
        var out: [(String, CalendarDay)] = []
        for key in keys {
            if let e = events.first(where: { $0.key == key && ($0.day ?? today) > today }), let d = e.day {
                out.append((key == "newYear" ? L("tools.countdown.quickNewYear") : e.name, d))
            }
        }
        return out.sorted { $0.1 < $1.1 }
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
                        Text(item.title + " · " + L("tools.alarm.\(item.repeats.rawValue)"))
                            .font(.subheadline)
                            .foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Toggle(L("tools.alarm.enable"), isOn: $item.enabled).labelsHidden()
                }
                .contentShape(Rectangle())
                .onTapGesture { editing = item }
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

private struct AlarmEditor: View {
    @Environment(\.dismiss) private var dismiss
    @State var item: AlarmItem
    let onSave: (AlarmItem) -> Void
    @State private var time = Date()

    var body: some View {
        NavigationStack {
            Form {
                DatePicker(L("tools.alarm.timeLabel"), selection: $time, displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .frame(maxWidth: .infinity)
                TextField(L("tools.alarm.labelPlaceholder"), text: $item.label)
                Picker(L("tools.alarm.repeatLabel"), selection: $item.repeats) {
                    ForEach(AlarmItem.Repeat.allCases, id: \.self) { Text(L("tools.alarm.\($0.rawValue)")).tag($0) }
                }
            }
            .themedBackground()
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
