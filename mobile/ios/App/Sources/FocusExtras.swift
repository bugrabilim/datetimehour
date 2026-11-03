import CoreMotion
import SaatCore
import SwiftUI
import UIKit

// MARK: Odak engelinin zaman çizelgesi (2. analiz #27)

/// Pomodoro dışında da belirli gün ve saatlerde engel (ör. hafta içi 08–17). Kayıt `focus.sched` (açık mı),
/// `focus.sched.days` (ISO 1–7, virgülle; varsayılan hafta içi), `focus.sched.from` / `.to` (gece yarısından dakika).
/// Android'deki anahtarlarla aynı. iPhone'da kalkan uygulama açılınca ve arka plan yenilemesinde güncellenir.
enum FocusScheduleStore {
    static let onKey = "focus.sched", daysKey = "focus.sched.days", fromKey = "focus.sched.from", toKey = "focus.sched.to"

    static var on: Bool { UserDefaults.standard.bool(forKey: onKey) }
    static var days: Set<Int> {
        Set((UserDefaults.standard.string(forKey: daysKey) ?? "1,2,3,4,5").split(separator: ",").compactMap { Int($0) })
    }
    static var from: Int { (UserDefaults.standard.object(forKey: fromKey) as? Int) ?? 480 }
    static var to: Int { (UserDefaults.standard.object(forKey: toKey) as? Int) ?? 1020 }

    static func active(_ now: Date = Date()) -> Bool {
        guard on else { return false }
        let c = Calendar(identifier: .gregorian).dateComponents([.weekday, .hour, .minute], from: now)
        let iso = ((c.weekday ?? 1) + 5) % 7 + 1 // Pazar 1 → 7, Pazartesi 2 → 1
        return FocusSchedule.active(days: days, from: from, to: to, weekday: iso, minute: (c.hour ?? 0) * 60 + (c.minute ?? 0))
    }
}

/// Odak kartının içindeki çizelge bölümü: aç/kapat, günler, başlangıç ve bitiş
struct FocusScheduleSection: View {
    @AppStorage(FocusScheduleStore.onKey) private var on = false
    @AppStorage(FocusScheduleStore.daysKey) private var daysRaw = "1,2,3,4,5"
    @AppStorage(FocusScheduleStore.fromKey) private var from = 480
    @AppStorage(FocusScheduleStore.toKey) private var to = 1020

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Toggle(L("focus.sched"), isOn: $on).tint(Theme.accent)
            if on {
                let days = Set(daysRaw.split(separator: ",").compactMap { Int($0) })
                HStack(spacing: 6) {
                    ForEach(1...7, id: \.self) { d in
                        let sel = days.contains(d)
                        Button {
                            var s = days
                            if sel { s.remove(d) } else { s.insert(d) }
                            daysRaw = s.sorted().map(String.init).joined(separator: ",")
                        } label: {
                            Text(L("calendar.weekdaysShort.\(d - 1)")).font(.caption.weight(.semibold))
                                .frame(maxWidth: .infinity, minHeight: 32)
                                .background(sel ? Theme.accent : Theme.surface2, in: RoundedRectangle(cornerRadius: 8))
                                .foregroundStyle(sel ? Color.white : Theme.text)
                        }
                        .buttonStyle(.plain)
                    }
                }
                DatePicker(L("focus.schedFrom"), selection: minuteBinding($from), displayedComponents: .hourAndMinute)
                DatePicker(L("focus.schedTo"), selection: minuteBinding($to), displayedComponents: .hourAndMinute)
                if FocusScheduleStore.active() {
                    Label(L("focus.schedOn"), systemImage: "lock.fill").font(.caption.weight(.semibold)).foregroundStyle(Theme.accent)
                }
                Text(L("focus.schedNoteIOS")).font(.caption).foregroundStyle(Theme.muted)
            }
        }
        .onChange(of: on) { FocusBlock.sync() }
        .onChange(of: daysRaw) { FocusBlock.sync() }
        .onChange(of: from) { FocusBlock.sync() }
        .onChange(of: to) { FocusBlock.sync() }
    }

    private func minuteBinding(_ b: Binding<Int>) -> Binding<Date> {
        Binding(get: {
            Calendar.current.date(bySettingHour: b.wrappedValue / 60, minute: b.wrappedValue % 60, second: 0, of: Date()) ?? Date()
        }, set: { d in
            let c = Calendar.current.dateComponents([.hour, .minute], from: d)
            b.wrappedValue = (c.hour ?? 0) * 60 + (c.minute ?? 0)
        })
    }
}

// MARK: Nefes egzersizi (2. analiz #28)

/// Araçlar › Nefes egzersizi: 4-7-8, kutu, sakin, dengeli kalıplar; büyüyüp küçülen çember, adım adı ve kalan
/// saniye, adım değişince hafif titreşim. Süre 1–10 dk. Hiçbir şey kaydedilmez (yalnız seçilen kalıp ve süre).
struct BreathView: View {
    @AppStorage("breath.pattern") private var pattern = "478"
    @AppStorage("breath.minutes") private var minutes = 3
    @State private var start: Date?
    @State private var lastStep = -1
    @State private var finished: Int?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var steps: [Double] { Breath.patterns[pattern] ?? [4, 7, 8, 0] }

    var body: some View {
        ScrollView {
            VStack(spacing: 22) {
                TimelineView(.animation(minimumInterval: 1.0 / 30, paused: start == nil)) { ctx in
                    let elapsed = start.map { ctx.date.timeIntervalSince($0) } ?? 0
                    let ph = Breath.phase(steps, elapsed: elapsed)
                    let scale = start == nil ? 0.35 : 0.35 + 0.65 * Breath.scale(ph)
                    ZStack {
                        Circle().fill(Theme.accent.opacity(0.12)).frame(width: 260, height: 260)
                        Circle().fill(Theme.accent.opacity(0.55))
                            .frame(width: 260 * (reduceMotion ? 0.8 : scale), height: 260 * (reduceMotion ? 0.8 : scale))
                        VStack(spacing: 6) {
                            Text(start == nil ? L("breath.title") : L(["breath.in", "breath.hold", "breath.out", "breath.hold"][ph.step]))
                                .font(.title2.weight(.semibold))
                            if start != nil {
                                Text(verbatim: "\(Int(ph.left.rounded(.up)))").font(.system(size: 44, weight: .bold, design: .rounded)).monospacedDigit()
                            }
                        }
                        .foregroundStyle(Theme.text)
                    }
                    .frame(height: 280)
                    .onChange(of: ph.step) { _, s in
                        guard start != nil, s != lastStep else { return }
                        lastStep = s
                        UIImpactFeedbackGenerator(style: .soft).impactOccurred()
                    }
                    .onChange(of: elapsed >= Double(minutes * 60)) { _, done in
                        guard done, start != nil else { return }
                        finished = Breath.phase(steps, elapsed: elapsed).cycle
                        start = nil
                    }
                }
                if let finished { Text(T("breath.done", finished)).font(.headline).foregroundStyle(Theme.accent) }
                RoundButton(title: L(start == nil ? "breath.start" : "breath.stop"), prominent: true) {
                    if start == nil { start = Date(); finished = nil; lastStep = -1 } else { start = nil }
                    UIApplication.shared.isIdleTimerDisabled = start != nil
                }
                VStack(alignment: .leading, spacing: 10) {
                    Picker(L("breath.title"), selection: $pattern) {
                        ForEach(Breath.order, id: \.self) { Text(L("breath.pattern.\($0)")).tag($0) }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                    Stepper("\(L("breath.minutes")): \(minutes) \(L("client.minuteShort"))", value: $minutes, in: 1...10)
                }
                .disabled(start != nil)
                .card()
                Text(L("breath.note")).font(.caption).foregroundStyle(Theme.muted)
            }
            .padding(20)
        }
        .background(Theme.bg.ignoresSafeArea())
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .navigationTitle(L("breath.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Telefon yüzüstüyken odak (2. analiz #29)

/// Pomodoro sayfası açıkken ivmeölçerle telefonun yüzüstü olup olmadığına bakar: çalışma sırası beklerken telefon
/// 2 sn yüzüstü kalırsa pomodoro başlar; çalışırken 3 sn kaldırılırsa durur ve uyarır. İzin istemez, hiçbir şey
/// kaydedilmez. Seçim `pomo.flip`.
@MainActor @Observable
final class FlipFocus {
    static let key = "pomo.flip"
    private let motion = CMMotionManager()
    private(set) var faceDown = false
    private var since = Date()
    var pausedNote = false

    func start() {
        guard motion.isAccelerometerAvailable, !motion.isAccelerometerActive else { return }
        motion.accelerometerUpdateInterval = 0.25
        motion.startAccelerometerUpdates(to: .main) { [weak self] data, _ in
            guard let self, let a = data?.acceleration else { return }
            // Ekran aşağı bakarken yerçekimi z ekseninde +1 g
            let down = a.z > 0.8
            MainActor.assumeIsolated { self.update(down: down) }
        }
    }

    func stop() { motion.stopAccelerometerUpdates() }

    private func update(down: Bool) {
        if down != faceDown { faceDown = down; since = Date() }
        guard UserDefaults.standard.bool(forKey: Self.key), PomoEngine.phase == "work" else { return }
        let held = Date().timeIntervalSince(since)
        let running = UserDefaults.standard.double(forKey: "pomo.end") > 0
        if faceDown, !running, held >= 2 {
            PomoEngine.start(seconds: PomoEngine.remaining(at: Date()))
            pausedNote = false
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        } else if !faceDown, running, held >= 3 {
            PomoEngine.pause(at: Date())
            pausedNote = true
            UINotificationFeedbackGenerator().notificationOccurred(.warning)
        }
    }
}

struct FlipFocusCard: View {
    @AppStorage(FlipFocus.key) private var on = false
    @AppStorage("pomo.end") private var end = 0.0
    @State private var flip = FlipFocus()

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Toggle(L("flip.toggle"), isOn: $on).tint(Theme.accent)
            if on {
                if flip.pausedNote && end == 0 {
                    Label(L("flip.paused"), systemImage: "exclamationmark.triangle").font(.caption.weight(.semibold)).foregroundStyle(.orange)
                } else if end == 0 {
                    Label(L("flip.waiting"), systemImage: "iphone.gen3").font(.caption).foregroundStyle(Theme.accent)
                }
            }
            Text(L("flip.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
        .onAppear { if on { flip.start(); UIApplication.shared.isIdleTimerDisabled = true } }
        .onChange(of: on) { _, v in
            if v { flip.start() } else { flip.stop() }
            UIApplication.shared.isIdleTimerDisabled = v
        }
        .onDisappear { flip.stop(); UIApplication.shared.isIdleTimerDisabled = false }
    }
}

// MARK: Odak istatistiği: 7 gün / 30 gün / 12 ay ve odak ormanı (2. analiz #31)

enum FocusRange: String, CaseIterable, Identifiable {
    case week, month, year
    var id: String { rawValue }

    /// Çubukların etiketleri ve dakikaları (eskiden yeniye)
    func series(_ log: [FocusLog.Entry], now: Date = Date()) -> [(label: String, minutes: Int)] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = .current
        switch self {
        case .week, .month:
            let n = self == .week ? 7 : 30
            var sums: [String: Int] = [:]
            for e in log { sums[e.d, default: 0] += e.m }
            return (0..<n).reversed().map { i in
                let d = cal.date(byAdding: .day, value: -i, to: now) ?? now
                let key = BadgeStore.day(d)
                let label = self == .week ? L("calendar.weekdaysShort.\((cal.component(.weekday, from: d) + 5) % 7)") : String(cal.component(.day, from: d))
                return (label, sums[key] ?? 0)
            }
        case .year:
            var sums: [String: Int] = [:]
            for e in log { sums[String(e.d.prefix(7)), default: 0] += e.m }
            return (0..<12).reversed().map { i in
                let d = cal.date(byAdding: .month, value: -i, to: now) ?? now
                let c = cal.dateComponents([.year, .month], from: d)
                let key = String(format: "%04d-%02d", c.year ?? 0, c.month ?? 1)
                return (String(L("calendar.months.\((c.month ?? 1) - 1)").prefix(3)), sums[key] ?? 0)
            }
        }
    }
}

struct FocusRangeChart: View {
    let log: [FocusLog.Entry]
    @AppStorage("pomo.statsRange") private var rangeRaw = FocusRange.week.rawValue

    var body: some View {
        let range = FocusRange(rawValue: rangeRaw) ?? .week
        let s = range.series(log)
        let top = max(1, s.map(\.minutes).max() ?? 1)
        VStack(alignment: .leading, spacing: 8) {
            Picker("", selection: $rangeRaw) {
                ForEach(FocusRange.allCases) { Text(L("stats.range.\($0.rawValue)")).tag($0.rawValue) }
            }
            .pickerStyle(.segmented)
            HStack(alignment: .bottom, spacing: range == .month ? 2 : 6) {
                ForEach(Array(s.enumerated()), id: \.offset) { i, v in
                    VStack(spacing: 3) {
                        Spacer(minLength: 0)
                        RoundedRectangle(cornerRadius: range == .month ? 2 : 4)
                            .fill(i == s.count - 1 ? Theme.accent : Theme.accent.opacity(0.55))
                            .frame(height: max(2, 90 * CGFloat(v.minutes) / CGFloat(top)))
                        if range != .month || i % 5 == 4 {
                            Text(v.label).font(.system(size: 9)).foregroundStyle(Theme.muted).lineLimit(1).fixedSize()
                        } else {
                            Text(" ").font(.system(size: 9))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(v.label): \(v.minutes) \(L("tools.pomodoro.minUnit"))")
                }
            }
            .frame(height: 112)
        }
    }
}

/// Bu ayın odak ormanı: biten her çalışma turu bir ağaç (25 dk altı fidan, 50 dk'ya kadar ağaç, daha uzunu ulu ağaç)
struct FocusForest: View {
    let log: [FocusLog.Entry]

    var body: some View {
        let month = String(BadgeStore.day(Date()).prefix(7))
        let trees = log.filter { $0.d.hasPrefix(month) }.map { $0.m < 25 ? "🌱" : $0.m <= 50 ? "🌳" : "🌲" }
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("forest.title"))
            if trees.isEmpty {
                Text(L("forest.empty")).font(.subheadline).foregroundStyle(Theme.muted)
            } else {
                Text(T("forest.count", trees.count)).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accent)
                Text(trees.prefix(120).joined()).font(.title2).lineSpacing(2)
                    .accessibilityHidden(true)
            }
            Text(L("forest.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }
}
