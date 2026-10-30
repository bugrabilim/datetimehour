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

/// Zamanlayıcı geçmişi (rakip analizi listesi #82): son başlatılan süreler, aynı ad + süre bir kez (en yenisi üstte, en çok 12);
/// kayıt `timers.history` (JSON [{n, t}], t ms; Android'le aynı)
enum TimerHistory {
    struct Entry: Codable, Equatable, Hashable { var n: String; var t: Int64 }
    static let key = "timers.history"
    static func load() -> [Entry] {
        UserDefaults.standard.data(forKey: key).flatMap { try? JSONDecoder().decode([Entry].self, from: $0) } ?? []
    }
    static func record(_ name: String, _ ms: Int64) {
        let e = Entry(n: name, t: ms)
        let list = [e] + load().filter { $0 != e }
        UserDefaults.standard.set(try? JSONEncoder().encode(Array(list.prefix(12))), forKey: key)
    }
    static func clear() { UserDefaults.standard.removeObject(forKey: key) }
}

/// Aralık antrenmanı: ısınma, çalışma, dinlenme (sn) ve tur sayısı
struct HiitConfig: Equatable {
    var warmup = 10, work = 30, rest = 15, rounds = 8
    /// Çok adımlı rutin (rakip analizi listesi #83): adlı adımlar her turda sırayla; boşsa klasik aralık
    var name = ""
    var steps: [Step] = []

    /// Rutin adımı: ad, süre (sn), dinlenme mi (renk ve sesli duyuru için)
    struct Step: Equatable, Identifiable {
        var id = UUID()
        var n: String
        var s: Int
        var r = false
        static func == (a: Step, b: Step) -> Bool { a.n == b.n && a.s == b.s && a.r == b.r }
    }

    var isRoutine: Bool { !steps.isEmpty }
    var totalSeconds: Int { isRoutine ? rounds * steps.reduce(0) { $0 + $1.s } : warmup + rounds * work + (rounds - 1) * rest }
    var raw: String { "\(warmup),\(work),\(rest),\(rounds)" }

    struct Phase: Equatable {
        let kind: String; let round: Int; let left: Int; let index: Int
        var name = ""
    }

    /// Geçen süreye (sn) göre evre (Android'deki HiitConfig.phase ile aynı)
    func phase(_ elapsed: Int) -> Phase {
        var t = elapsed
        if isRoutine {
            for r in 1...max(1, rounds) {
                for (i, st) in steps.enumerated() {
                    if t < st.s { return Phase(kind: st.r ? "rest" : "work", round: r, left: st.s - t, index: (r - 1) * steps.count + i, name: st.n) }
                    t -= st.s
                }
            }
            return Phase(kind: "done", round: rounds, left: 0, index: rounds * steps.count)
        }
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

    /// Evrenin sesli adı: "Isın", "Çalış" (son turda "Son tur. Çalış"), "Dinlen", "Bitti"; rutinde adımın adı
    func word(_ p: Phase) -> String {
        if p.kind == "done" { return L("hiit.say.done") }
        if isRoutine {
            let last = p.round == rounds && rounds > 1 && p.index == (rounds - 1) * steps.count
            return (last ? L("hiit.say.last") + ". " : "") + p.name
        }
        switch p.kind {
        case "warmup": return L("hiit.say.warmup")
        case "work": return p.round == rounds && rounds > 1 ? L("hiit.say.last") + ". " + L("hiit.say.work") : L("hiit.say.work")
        default: return L("hiit.say.rest")
        }
    }

    /// Ekrandaki ad
    func title(_ p: Phase) -> String {
        if !p.name.isEmpty { return p.name }
        switch p.kind {
        case "warmup": return L("hiit.warmup")
        case "work": return L("hiit.work")
        case "rest": return L("hiit.rest")
        default: return L("hiit.done")
        }
    }

    /// Sıradaki adımın adı (rutinde; son adımda nil)
    func next(_ p: Phase) -> String? {
        guard isRoutine, p.kind != "done", p.index + 1 < rounds * steps.count else { return nil }
        return steps[(p.index + 1) % steps.count].n
    }

    /// Rutin JSON'u (Android'deki routineJson ile aynı alanlar: n, rounds, steps[{n, s, r}])
    var routineJSON: [String: Any] { ["n": name, "rounds": rounds, "steps": steps.map { ["n": $0.n, "s": $0.s, "r": $0.r] }] }

    static func fromRoutine(_ o: [String: Any]) -> HiitConfig {
        let steps = (o["steps"] as? [[String: Any]] ?? []).compactMap { d -> Step? in
            guard let n = d["n"] as? String, let s = (d["s"] as? NSNumber)?.intValue else { return nil }
            return Step(n: n, s: s, r: (d["r"] as? Bool) ?? false)
        }
        return HiitConfig(rounds: max(1, (o["rounds"] as? NSNumber)?.intValue ?? 1), name: o["n"] as? String ?? "", steps: steps)
    }

    /// Etkin ayar: seçili rutin (hiit.routine) ya da klasik aralık (hiit.cfg)
    static func load() -> HiitConfig {
        if let raw = UserDefaults.standard.string(forKey: "hiit.routine"), let d = raw.data(using: .utf8),
           let o = try? JSONSerialization.jsonObject(with: d) as? [String: Any] {
            let c = fromRoutine(o)
            if c.isRoutine { return c }
        }
        let p = (UserDefaults.standard.string(forKey: "hiit.cfg") ?? "").split(separator: ",").compactMap { Int($0) }
        guard p.count == 4 else { return HiitConfig() }
        return HiitConfig(warmup: p[0], work: p[1], rest: p[2], rounds: p[3])
    }
}

/// Rutin şablonları ve kullanıcının rutinleri (Android'deki Routines ile aynı kayıt: routines, hiit.routine)
enum Routines {
    private static let key = "routines"
    private static func ex(_ k: String) -> String { L("routine.ex.\(k)") }
    private static func work(_ k: String, _ s: Int) -> HiitConfig.Step { .init(n: ex(k), s: s) }
    private static func rest(_ s: Int) -> HiitConfig.Step { .init(n: L("hiit.rest"), s: s, r: true) }

    /// Hazır şablonlar (kimlik, yapı)
    static func templates() -> [(String, HiitConfig)] {
        let seven = ["jacks", "wallsit", "pushup", "crunch", "stepup", "squat", "dips", "plank", "highknees", "lunge", "pushrot", "sideplank"]
        return [
            ("tabata", HiitConfig(rounds: 8, name: L("routine.tabata"), steps: [.init(n: L("hiit.work"), s: 20), rest(10)])),
            ("seven", HiitConfig(rounds: 1, name: L("routine.seven"), steps: seven.enumerated().flatMap { i, k in i < 11 ? [work(k, 30), rest(10)] : [work(k, 30)] })),
            ("emom", HiitConfig(rounds: 10, name: L("routine.emom"), steps: [.init(n: L("routine.minute"), s: 60)])),
            ("warmup", HiitConfig(rounds: 1, name: L("routine.warmup"), steps: ["march", "arms", "hips", "knees", "jacks"].map { work($0, 45) })),
            ("stretch", HiitConfig(rounds: 1, name: L("routine.stretch"), steps: ["neck", "shoulders", "hamstring", "quads", "back"].map { work($0, 40) })),
        ]
    }

    static func mine() -> [(String, HiitConfig)] {
        guard let d = UserDefaults.standard.string(forKey: key)?.data(using: .utf8),
              let a = try? JSONSerialization.jsonObject(with: d) as? [[String: Any]] else { return [] }
        return a.compactMap { o in (o["id"] as? String).map { ($0, HiitConfig.fromRoutine(o)) } }
    }

    static func save(_ list: [(String, HiitConfig)]) {
        let a = list.map { id, c in c.routineJSON.merging(["id": id]) { $1 } }
        if let d = try? JSONSerialization.data(withJSONObject: a), let s = String(data: d, encoding: .utf8) {
            UserDefaults.standard.set(s, forKey: key)
        }
    }

    /// Etkin rutini seç (nil: klasik aralığa dön)
    static func select(_ c: HiitConfig?) {
        guard let c, let d = try? JSONSerialization.data(withJSONObject: c.routineJSON) else {
            UserDefaults.standard.removeObject(forKey: "hiit.routine")
            return
        }
        UserDefaults.standard.set(String(data: d, encoding: .utf8), forKey: "hiit.routine")
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
        HealthLog.recordHiitIfDone()
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
    @State private var history = TimerHistory.load()

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
                    if !history.isEmpty {
                        HStack {
                            Text(L("timers.history.heading")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
                            Spacer()
                            Button(L("timers.history.clear")) {
                                TimerHistory.clear()
                                history = []
                            }
                            .font(.footnote)
                            .foregroundStyle(Theme.accent)
                        }
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(history, id: \.self) { e in
                                    Button(e.n.isEmpty ? clock(Int(e.t / 1000)) : e.n + " · " + clock(Int(e.t / 1000))) { add(e.t, name: e.n) }
                                        .buttonStyle(.bordered)
                                        .tint(Theme.accent)
                                }
                            }
                        }
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
        // Kilit ekranı / Dynamic Island: süren zamanlayıcılar
        .onChange(of: items) { LiveActivities.sync() }
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
            // +1 dk (rakip analizi #26): sayarken bitişi, duraklatılmışken kalanı uzatır; bittiyse 1 dakikalık yeniden başlar
            Button(L("tools.countdown.addMinute")) {
                if done {
                    update(t.id) { var x = $0; x.total = 60_000; return start(x, 60_000) }
                } else if t.running {
                    cancel(t)
                    update(t.id) { var x = $0; x.total += 60_000; return start(x, rem + 60_000) }
                } else {
                    update(t.id) { var x = $0; x.left += 60_000; x.total += 60_000; return x }
                }
            }
            .font(.footnote.weight(.semibold))
            .foregroundStyle(Theme.accent)
            .buttonStyle(.borderless)
            .padding(.leading, 10)
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

    private func add(_ ms: Int64, name preset: String? = nil) {
        guard ms > 0 else { return }
        BadgeStore.shared.hit(.zamanlayici)
        let n = (preset ?? name).trimmingCharacters(in: .whitespaces)
        items.append(start(TimerItem(name: n, total: ms), ms))
        TimerItem.save(items)
        TimerHistory.record(n, ms)
        history = TimerHistory.load()
        if preset == nil { name = "" }
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
                    HealthToggle()
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
            HealthLog.recordHiitIfDone()
        }
    }

    private var settings: some View {
        VStack(spacing: 12) {
            VStack(spacing: 4) {
                if cfg.isRoutine {
                    Text(cfg.name).font(.headline).frame(maxWidth: .infinity, alignment: .leading)
                    ForEach(cfg.steps) { st in
                        HStack {
                            Text(st.n).foregroundStyle(st.r ? Theme.muted : Theme.ink)
                            Spacer()
                            Text(clock(st.s)).monospacedDigit().foregroundStyle(Theme.muted)
                                .environment(\.layoutDirection, .leftToRight)
                        }
                        .font(.subheadline)
                    }
                    stepper(L("hiit.rounds"), $cfg.rounds, 1...50, 1, seconds: false)
                } else {
                    stepper(L("hiit.warmup"), $cfg.warmup, 0...600, 5, seconds: true)
                    stepper(L("hiit.work"), $cfg.work, 5...1800, 5, seconds: true)
                    stepper(L("hiit.rest"), $cfg.rest, 0...900, 5, seconds: true)
                    stepper(L("hiit.rounds"), $cfg.rounds, 1...50, 1, seconds: false)
                }
                Text(T("hiit.total", clock(cfg.totalSeconds))).font(.footnote).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if cfg.isRoutine {
                    Button(L("routine.classic")) {
                        Routines.select(nil)
                        cfg = HiitConfig.load()
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 4)
                }
            }
            .padding(16)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            RoutinePicker { c in
                cfg = c
                Routines.select(c)
            }
            RoundButton(title: L("tools.countdown.start"), prominent: true) {
                if cfg.isRoutine { Routines.select(cfg) } else { UserDefaults.standard.set(cfg.raw, forKey: "hiit.cfg") }
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
        if (phase.kind == "work" || phase.kind == "rest") && cfg.rounds > 1 { Text(T("hiit.round", phase.round, cfg.rounds)).font(.headline) }
        if let next = cfg.next(phase) { Text(T("routine.next", next)).font(.subheadline).foregroundStyle(Theme.muted) }
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

    private func scheduleEnd() {
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

/// Rutin seçimi: hazır şablonlar ve kullanıcının rutinleri; yeni rutin oluşturma
struct RoutinePicker: View {
    let onPick: (HiitConfig) -> Void
    @State private var mine = Routines.mine()
    @State private var editing = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("routine.heading")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
            ForEach(Array((Routines.templates() + mine).enumerated()), id: \.offset) { _, item in
                HStack {
                    Button { onPick(item.1) } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.1.name).foregroundStyle(Theme.ink)
                            Text(T("hiit.total", clock(item.1.totalSeconds))).font(.caption).foregroundStyle(Theme.muted)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    if mine.contains(where: { $0.0 == item.0 }) {
                        Button {
                            mine.removeAll { $0.0 == item.0 }
                            Routines.save(mine)
                        } label: { Image(systemName: "xmark").foregroundStyle(Theme.muted) }
                        .buttonStyle(.plain)
                        .accessibilityLabel(L("timers.remove"))
                    }
                }
                .padding(.vertical, 4)
            }
            Button(L("routine.new")) { editing = true }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
        .sheet(isPresented: $editing) {
            RoutineEditor { c in
                mine.append((UUID().uuidString, c))
                Routines.save(mine)
                editing = false
                onPick(c)
            }
        }
    }
}

/// Yeni rutin: ad, adımlar (ad, süre, dinlenme), tur
struct RoutineEditor: View {
    let onSave: (HiitConfig) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var rounds = 1
    @State private var steps: [HiitConfig.Step] = [.init(n: L("hiit.work"), s: 30), .init(n: L("hiit.rest"), s: 10, r: true)]

    var body: some View {
        NavigationStack {
            Form {
                TextField(L("routine.name"), text: $name)
                ForEach($steps) { $st in
                    Section {
                        TextField(L("routine.name"), text: $st.n)
                        Stepper(value: $st.s, in: 5...1800, step: 5) {
                            HStack {
                                Text(L("routine.seconds"))
                                Spacer()
                                Text(clock(st.s)).monospacedDigit().environment(\.layoutDirection, .leftToRight)
                            }
                        }
                        Toggle(L("hiit.rest"), isOn: $st.r)
                    }
                }
                .onDelete { idx in if steps.count > 1 { steps.remove(atOffsets: idx) } }
                Section {
                    Button(L("routine.addStep")) { if steps.count < 30 { steps.append(.init(n: L("hiit.work"), s: 30)) } }
                    Stepper(value: $rounds, in: 1...50) { HStack { Text(L("hiit.rounds")); Spacer(); Text("\(rounds)").monospacedDigit() } }
                }
            }
            .themedBackground()
            .navigationTitle(L("routine.new"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("alarm.cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("alarm.save")) {
                        let n = name.trimmingCharacters(in: .whitespaces)
                        onSave(HiitConfig(rounds: rounds, name: n.isEmpty ? L("routine.untitled") : n, steps: steps.filter { $0.s > 0 }))
                    }
                }
            }
        }
    }
}
