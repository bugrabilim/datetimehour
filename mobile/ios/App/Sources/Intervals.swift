import AVFoundation
import SwiftUI
import UIKit

/* Birden çok adlı zamanlayıcı ve aralık (HIIT) antrenmanı (30 Ekim 2026, rakip analizi #12); Android'deki Intervals.kt ile
   aynı kayıtlar: "timers" (JSON, zamanlar Unix ms) ve "hiit.cfg" / "hiit.start" / "hiit.paused". Sesli duyuru iPhone'un
   konuşma sesiyle (dosya ya da internet yok). Aralık antrenmanının duyuruları HiitRunner'da (ekran kilitliyken de);
   zamanlayıcının adı ekran açıkken söylenir. Bitişte her zamanlayıcı kendi alarmını çalar. */

struct TimerItem: Codable, Identifiable, Equatable {
    var id = UUID().uuidString
    var name = ""
    /// Toplam süre (ms)
    var total: Int64
    /// Çalışırken bitiş anı (Unix ms; 0 = durmuş)
    var end: Int64 = 0
    /// Duraklatılmışken kalan (ms)
    var left: Int64 = 0

    var running: Bool { end > 0 }
    func remaining(_ now: Int64) -> Int64 { running ? max(0, end - now) : left }
    var doneTitle: String { name.trimmingCharacters(in: .whitespaces).isEmpty ? L("tools.countdown.finished") : T("timers.doneNamed", name) }

    static var nowMs: Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    static func load() -> [TimerItem] {
        let d = UserDefaults.standard
        var list = (d.data(forKey: "timers")).flatMap { try? JSONDecoder().decode([TimerItem].self, from: $0) } ?? []
        // Eski tek zamanlayıcı (tm.end / tm.left, referans tarihinden saniye) bir kez listeye taşınır
        let oldEnd = d.double(forKey: "tm.end"), oldLeft = d.double(forKey: "tm.left")
        if oldEnd > 0 || oldLeft > 0 {
            let endMs = oldEnd > 0 ? Int64(Date(timeIntervalSinceReferenceDate: oldEnd).timeIntervalSince1970 * 1000) : 0
            let leftMs = Int64(oldLeft * 1000)
            let id = d.string(forKey: "tm.id").flatMap { $0.isEmpty ? nil : $0 } ?? UUID().uuidString
            list.append(TimerItem(id: id, total: max(leftMs, endMs - nowMs, 1000), end: endMs, left: leftMs))
            d.removeObject(forKey: "tm.end"); d.removeObject(forKey: "tm.left"); d.removeObject(forKey: "tm.id")
            save(list)
        }
        return list
    }

    static func save(_ items: [TimerItem]) {
        UserDefaults.standard.set(try? JSONEncoder().encode(items), forKey: "timers")
    }
}

/// Aralık antrenmanı: ısınma, çalışma, dinlenme (sn) ve tur sayısı
struct HiitConfig: Equatable {
    var warmup = 10, work = 30, rest = 15, rounds = 8
    var totalSeconds: Int { warmup + rounds * work + (rounds - 1) * rest }
    var raw: String { "\(warmup),\(work),\(rest),\(rounds)" }

    struct Phase: Equatable { let kind: String; let round: Int; let left: Int; let index: Int }

    /// Geçen süreye (sn) göre evre (Android'deki HiitConfig.phase ile aynı)
    func phase(_ elapsed: Int) -> Phase {
        var t = elapsed
        if t < warmup { return Phase(kind: "warmup", round: 0, left: warmup - t, index: 0) }
        t -= warmup
        for r in 1...max(1, rounds) {
            if t < work { return Phase(kind: "work", round: r, left: work - t, index: r * 2 - 1) }
            t -= work
            if r == rounds { break }
            if t < rest { return Phase(kind: "rest", round: r, left: rest - t, index: r * 2) }
            t -= rest
        }
        return Phase(kind: "done", round: rounds, left: 0, index: rounds * 2 + 1)
    }

    /// Söylenecek: evre değişince (ya da ilk bakışta) evrenin adı, evre içinde son 3 saniyede sayı; yoksa nil
    func cue(last: Phase?, _ p: Phase) -> String? {
        guard let last, p.index == last.index else { return word(p) }
        if p.kind != "done", (1...3).contains(p.left), p.left != last.left { return "\(p.left)" }
        return nil
    }

    /// Evrenin sesli adı: "Isın", "Çalış" (son turda "Son tur. Çalış"), "Dinlen", "Bitti"
    func word(_ p: Phase) -> String {
        switch p.kind {
        case "warmup": L("hiit.say.warmup")
        case "work": p.round == rounds && rounds > 1 ? L("hiit.say.last") + ". " + L("hiit.say.work") : L("hiit.say.work")
        case "rest": L("hiit.say.rest")
        default: L("hiit.say.done")
        }
    }

    /// Ekrandaki ad
    func title(_ p: Phase) -> String {
        switch p.kind {
        case "warmup": L("hiit.warmup")
        case "work": L("hiit.work")
        case "rest": L("hiit.rest")
        default: L("hiit.done")
        }
    }

    static func load() -> HiitConfig {
        let p = (UserDefaults.standard.string(forKey: "hiit.cfg") ?? "").split(separator: ",").compactMap { Int($0) }
        guard p.count == 4 else { return HiitConfig() }
        return HiitConfig(warmup: p[0], work: p[1], rest: p[2], rounds: p[3])
    }
}

/// Sesli duyuru (bildirim seslerindeki en iyi ses; Azericede Türkçe)
@MainActor
enum Speaker {
    private static let synth = AVSpeechSynthesizer()
    static var on: Bool { UserDefaults.standard.object(forKey: "tools.voice") as? Bool ?? true }

    static func say(_ text: String) {
        guard on else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers, .mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        synth.stopSpeaking(at: .immediate)
        let u = AVSpeechUtterance(string: text)
        u.voice = NotificationSounds.bestVoice
        synth.speak(u)
    }
}

/// Aralık antrenmanı arka planda (30 Ekim 2026, sahibin isteği: "uygulama kapalıyken de çalışması şart"): antrenman
/// sürerken ses oturumu açık tutulur (arka plan ses modu, Info.plist UIBackgroundModes: audio; sessiz bir ses döngüsü
/// çalar), böylece ekran kilitliyken ya da başka uygulamadayken de evre geçişleri ve son 3 saniye telefonun sesiyle
/// söylenir. Müzik çalıyorsa duyurular üstüne söylenir (mixWithOthers; müzik kısılmaz). Durum hiit.start / hiit.paused /
/// hiit.cfg kayıtlarıdır; duraklatınca, sıfırlayınca ya da bitince ses oturumu kapanır. Bitişte ayrıca alarm çalar
/// (AlarmKit). Uygulama kaydırılarak kapatılırsa iOS onu çalıştırmaz; o zaman yalnız bitiş alarmı çalar.
@MainActor
final class HiitRunner {
    static let shared = HiitRunner()
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let synth = AVSpeechSynthesizer()
    private var timer: Timer?
    private var last: HiitConfig.Phase?
    private var observers: [NSObjectProtocol] = []

    private var start: Double { UserDefaults.standard.double(forKey: "hiit.start") }
    private var paused: Double { UserDefaults.standard.double(forKey: "hiit.paused") }
    private var elapsed: Int { Int((Date().timeIntervalSince1970 * 1000 - start) / 1000) }

    private init() {
        engine.attach(player)
        // Telefon görüşmesi ya da kulaklık değişince ses motoru durur: antrenman sürüyorsa yeniden başlatılır
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { note in
            let ended = (note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt) == AVAudioSession.InterruptionType.ended.rawValue
            if ended { MainActor.assumeIsolated { HiitRunner.shared.restartAudio() } }
        })
        observers.append(center.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: .main) { _ in
            MainActor.assumeIsolated { HiitRunner.shared.restartAudio() }
        })
    }

    /// Antrenman sürüyorsa duyuruları başlatır, sürmüyorsa durdurur (ekrandaki düğmelerden ve uygulama öne gelince)
    func sync() {
        let running = start > 0 && paused == 0 && HiitConfig.load().phase(elapsed).kind != "done"
        if running { begin() } else { end() }
    }

    private func begin() {
        guard timer == nil else { return }
        last = nil // sürdürünce o anki evre yeniden söylenir
        startAudio()
        let t = Timer(timeInterval: 0.2, repeats: true) { _ in
            MainActor.assumeIsolated { HiitRunner.shared.tick() }
        }
        RunLoop.main.add(t, forMode: .common)
        timer = t
        tick()
    }

    private func end() {
        timer?.invalidate()
        timer = nil
        last = nil
        synth.stopSpeaking(at: .immediate)
        stopAudio()
    }

    private func tick() {
        guard start > 0, paused == 0 else { end(); return }
        let cfg = HiitConfig.load()
        let p = cfg.phase(elapsed)
        if let text = cfg.cue(last: last, p) { say(text) }
        last = p
        guard p.kind == "done" else { return }
        // "Bitti" söylensin, sonra ses oturumu kapanır (bitiş alarmı ayrıca çalar)
        timer?.invalidate()
        timer = nil
        Task {
            try? await Task.sleep(for: .seconds(4))
            if timer == nil { stopAudio() }
        }
    }

    private func say(_ text: String) {
        guard Speaker.on else { return }
        synth.stopSpeaking(at: .immediate)
        let u = AVSpeechUtterance(string: text)
        u.voice = NotificationSounds.bestVoice
        synth.speak(u)
    }

    /// Sessiz döngü: ses oturumu açık kalsın, uygulama arka planda askıya alınmasın
    private func startAudio() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
        guard !engine.isRunning,
              let format = AVAudioFormat(standardFormatWithSampleRate: 22050, channels: 1),
              let silence = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 22050) else { return }
        silence.frameLength = silence.frameCapacity
        if let data = silence.floatChannelData { data[0].update(repeating: 0, count: Int(silence.frameLength)) }
        engine.connect(player, to: engine.mainMixerNode, format: format)
        player.scheduleBuffer(silence, at: nil, options: .loops)
        engine.prepare()
        do {
            try engine.start()
            player.play()
        } catch {
            // Ses motoru açılamazsa duyurular yine ekran açıkken çalışır
        }
    }

    private func restartAudio() {
        guard timer != nil else { return }
        player.stop()
        engine.stop()
        startAudio()
    }

    private func stopAudio() {
        player.stop()
        engine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

private struct VoiceToggle: View {
    @AppStorage("tools.voice") private var on = true
    var body: some View {
        Toggle(isOn: $on) {
            VStack(alignment: .leading, spacing: 2) {
                Text(L("tools.voice"))
                Text(L("tools.voiceNote")).font(.caption).foregroundStyle(Theme.muted)
            }
        }
        .tint(Theme.accent)
    }
}

// MARK: Zamanlayıcılar

struct TimersView: View {
    @State private var items = TimerItem.load()
    @State private var announced = Set<String>()
    @State private var h = 0
    @State private var m = 5
    @State private var s = 0
    @State private var name = ""

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.5)) { context in
            let now = Int64(context.date.timeIntervalSince1970 * 1000)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if !items.isEmpty {
                        Text(L("timers.heading")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
                        VStack(spacing: 0) {
                            ForEach(items) { t in
                                row(t, now: now)
                                if t.id != items.last?.id { Divider() }
                            }
                        }
                        .padding(.horizontal, 12)
                        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
                    }
                    Text(L("timers.new")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
                    TextField(L("timers.namePlaceholder"), text: $name)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityLabel(L("timers.name"))
                    HStack(spacing: 0) {
                        wheel($h, 0..<24, L("tools.countdown.hoursShort"))
                        wheel($m, 0..<60, L("tools.countdown.minutesShort"))
                        wheel($s, 0..<60, L("tools.countdown.secondsShort"))
                    }
                    .frame(height: 150)
                    .environment(\.layoutDirection, .leftToRight)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach([1, 3, 5, 10, 15, 25, 30, 45, 60], id: \.self) { n in
                                Button(T("tools.countdown.presetMin", n)) { add(Int64(n) * 60_000) }
                                    .buttonStyle(.bordered)
                                    .tint(Theme.accent)
                            }
                        }
                    }
                    RoundButton(title: L("tools.countdown.start"), prominent: true) { add(Int64(h * 3600 + m * 60 + s) * 1000) }
                        .disabled(h + m + s == 0)
                    VoiceToggle()
                    Text(L("timers.multiNote")).font(.footnote).foregroundStyle(Theme.muted)
                    Text(L(AlarmCenter.hasRealAlarms ? "alarm.realNote" : "alarm.fallbackNote")).font(.footnote).foregroundStyle(Theme.muted)
                }
                .padding(16)
            }
            .onChange(of: now / 500) {
                // Ekran açıkken biten zamanlayıcı sesli duyurulur (bir kez)
                for t in items where t.running && t.end <= now && !announced.contains(t.id + "\(t.end)") {
                    announced.insert(t.id + "\(t.end)")
                    Speaker.say(t.doneTitle)
                }
            }
        }
        .onAppear { items = TimerItem.load() }
    }

    private func row(_ t: TimerItem, now: Int64) -> some View {
        let rem = t.remaining(now)
        let done = t.running && rem <= 0
        return HStack {
            VStack(alignment: .leading, spacing: 0) {
                Text(t.name.isEmpty ? clock(Int(t.total / 1000)) : t.name).font(.footnote).foregroundStyle(Theme.muted).lineLimit(1)
                Text(done ? L("tools.countdown.finished") : clock(Int((rem + 999) / 1000)))
                    .font(.system(size: 32, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(done ? Theme.accent : Theme.text)
                    .environment(\.layoutDirection, .leftToRight)
            }
            Spacer()
            Group {
                if done {
                    Button { update(t.id) { start($0, $0.total) } } label: { Image(systemName: "arrow.clockwise") }
                        .accessibilityLabel(L("timers.restart"))
                } else if t.running {
                    Button {
                        cancel(t)
                        update(t.id) { var x = $0; x.left = rem; x.end = 0; return x }
                    } label: { Image(systemName: "pause.fill") }
                        .accessibilityLabel(L("tools.countdown.pause"))
                } else {
                    Button { update(t.id) { start($0, $0.left) } } label: { Image(systemName: "play.fill") }
                        .accessibilityLabel(L("tools.countdown.resume"))
                }
            }
            .font(.title3)
            .foregroundStyle(Theme.accent)
            .buttonStyle(.borderless)
            Button {
                cancel(t)
                items.removeAll { $0.id == t.id }
                TimerItem.save(items)
            } label: { Image(systemName: "xmark") }
                .buttonStyle(.borderless)
                .foregroundStyle(Theme.muted)
                .padding(.leading, 12)
                .accessibilityLabel(L("timers.remove"))
        }
        .padding(.vertical, 8)
    }

    private func wheel(_ value: Binding<Int>, _ range: Range<Int>, _ unit: String) -> some View {
        Picker(unit, selection: value) {
            ForEach(range, id: \.self) { Text("\($0) \(unit)").tag($0) }
        }
        .pickerStyle(.wheel)
        .frame(maxWidth: .infinity)
    }

    private func update(_ id: String, _ change: (TimerItem) -> TimerItem) {
        items = items.map { $0.id == id ? change($0) : $0 }
        TimerItem.save(items)
    }

    private func start(_ t: TimerItem, _ ms: Int64) -> TimerItem {
        var x = t
        x.end = TimerItem.nowMs + ms
        x.left = 0
        if let id = UUID(uuidString: x.id) {
            let at = Date(timeIntervalSince1970: Double(x.end) / 1000), title = x.doneTitle
            Task { await AlarmCenter.schedule(id: id, at: at, title: title) }
        }
        return x
    }

    private func cancel(_ t: TimerItem) {
        if let id = UUID(uuidString: t.id) { AlarmCenter.cancel(id: id) }
    }

    private func add(_ ms: Int64) {
        guard ms > 0 else { return }
        BadgeStore.shared.hit(.zamanlayici)
        items.append(start(TimerItem(name: name.trimmingCharacters(in: .whitespaces), total: ms), ms))
        TimerItem.save(items)
        name = ""
    }
}

// MARK: Aralık (HIIT)

struct HiitView: View {
    @State private var cfg = HiitConfig.load()
    /// Başlangıç (Unix ms; duraklatılan süre kadar ileri kayar) ve duraklatıldığı an (0 = sürüyor)
    @AppStorage("hiit.start") private var start = 0.0
    @AppStorage("hiit.paused") private var paused = 0.0
    @AppStorage("hiit.alarm") private var alarmID = ""

    private var active: Bool { start > 0 }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { context in
            let now = context.date.timeIntervalSince1970 * 1000
            let elapsed = active ? Int(((paused > 0 ? paused : now) - start) / 1000) : 0
            let phase = cfg.phase(elapsed)
            ScrollView {
                VStack(spacing: 16) {
                    if !active { settings } else { running(phase, elapsed: elapsed) }
                    VoiceToggle()
                    Text(L("hiit.note")).font(.footnote).foregroundStyle(Theme.muted)
                }
                .padding(16)
            }
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = active && paused == 0 }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .onChange(of: [start, paused]) {
            UIApplication.shared.isIdleTimerDisabled = active && paused == 0
            // Sesli duyurular HiitRunner'da (ekran kilitliyken de)
            HiitRunner.shared.sync()
        }
    }

    private var settings: some View {
        VStack(spacing: 12) {
            VStack(spacing: 4) {
                stepper(L("hiit.warmup"), $cfg.warmup, 0...600, 5, seconds: true)
                stepper(L("hiit.work"), $cfg.work, 5...1800, 5, seconds: true)
                stepper(L("hiit.rest"), $cfg.rest, 0...900, 5, seconds: true)
                stepper(L("hiit.rounds"), $cfg.rounds, 1...50, 1, seconds: false)
                Text(T("hiit.total", clock(cfg.totalSeconds))).font(.footnote).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            RoundButton(title: L("tools.countdown.start"), prominent: true) {
                UserDefaults.standard.set(cfg.raw, forKey: "hiit.cfg")
                start = Date().timeIntervalSince1970 * 1000
                paused = 0
                scheduleEnd()
            }
        }
    }

    @ViewBuilder private func running(_ phase: HiitConfig.Phase, elapsed: Int) -> some View {
        let color: Color = phase.kind == "work" ? Theme.accent : phase.kind == "rest" ? .green : Theme.muted
        Text(cfg.title(phase))
            .font(.largeTitle.bold())
            .foregroundStyle(color)
            .padding(.top, 10)
        if phase.kind != "done" { BigTime(text: clock(phase.left)) }
        if phase.kind == "work" || phase.kind == "rest" { Text(T("hiit.round", phase.round, cfg.rounds)).font(.headline) }
        ProgressView(value: min(1, Double(elapsed) / Double(max(1, cfg.totalSeconds)))).tint(color)
        HStack(spacing: 12) {
            RoundButton(title: L("tools.countdown.reset")) {
                cancelEnd()
                start = 0
                paused = 0
            }
            if phase.kind != "done" {
                if paused > 0 {
                    RoundButton(title: L("tools.countdown.resume"), prominent: true) {
                        start += Date().timeIntervalSince1970 * 1000 - paused
                        paused = 0
                        scheduleEnd()
                    }
                } else {
                    RoundButton(title: L("tools.countdown.pause"), prominent: true) {
                        cancelEnd()
                        paused = Date().timeIntervalSince1970 * 1000
                    }
                }
            }
        }
    }

    private func stepper(_ title: String, _ value: Binding<Int>, _ range: ClosedRange<Int>, _ step: Int, seconds: Bool) -> some View {
        Stepper(value: value, in: range, step: step) {
            HStack {
                Text(title)
                Spacer()
                Text(seconds ? clock(value.wrappedValue) : "\(value.wrappedValue)")
                    .monospacedDigit()
                    .fontWeight(.semibold)
                    .environment(\.layoutDirection, .leftToRight)
            }
        }
    }

        Text(cfg.title(phase))
            .font(.largeTitle.bold())    private func scheduleEnd() {
        let id = UUID()
        alarmID = id.uuidString
        let at = Date(timeIntervalSince1970: start / 1000 + Double(cfg.totalSeconds))
        Task { await AlarmCenter.schedule(id: id, at: at, title: L("hiit.done")) }
    }

    private func cancelEnd() {
        if let id = UUID(uuidString: alarmID) { AlarmCenter.cancel(id: id) }
        alarmID = ""
    }
}
