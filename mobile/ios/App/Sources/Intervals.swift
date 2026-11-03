import AVFoundation
import SwiftUI
import SaatCore
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
    /// 2. analiz #133: renk (0 = yok; geri sayım kartlarıyla aynı palet) ve bitince kendiliğinden yeniden başlama
    var color: Int?
    var repeats: Bool?

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

    /// Evrenin toplam süresi (sn)
    func duration(_ p: Phase) -> Int {
        if p.kind == "done" { return 0 }
        if isRoutine { return steps[p.index % steps.count].s }
        switch p.kind {
        case "warmup": return warmup
        case "work": return work
        default: return rest
        }
    }

    /// Antrenmanın bölümleri (süresi 0 olanlar yok): başlangıç (sn) ve süre; sıradakiler listesi ve adım atlama için
    struct Seg: Equatable { let phase: Phase; let start: Int; let dur: Int }

    func segments() -> [Seg] {
        var out: [Seg] = []
        var t = 0
        while t < totalSeconds {
            let p = phase(t)
            if p.kind == "done" || p.left <= 0 { break }
            out.append(Seg(phase: p, start: t, dur: p.left))
            t += p.left
        }
        return out
    }

    /// İsteğe bağlı duyurular (2. analiz #135): dinlenmede sıradaki adım, evrenin yarısı, son 10 saniye
    struct Cues {
        var next = false, half = false, ten = false
        static func load() -> Cues {
            let d = UserDefaults.standard
            return Cues(next: d.object(forKey: "hiit.cue.next") as? Bool ?? true, half: d.bool(forKey: "hiit.cue.half"), ten: d.bool(forKey: "hiit.cue.ten"))
        }
    }

    /// Söylenecek: evre değişince (ya da ilk bakışta) evrenin adı (dinlenme ve ısınmada istenirse sıradaki adım), evre içinde
    /// son 3 saniyede sayı, istenirse son 10 saniye ve evrenin yarısı (yalnız 20 sn ve uzun evrelerde); yoksa nil.
    /// Android'deki HiitConfig.cue ile aynı kural.
    func cue(last: Phase?, _ p: Phase, cues: Cues = Cues()) -> String? {
        guard let last, p.index == last.index else {
            var w = word(p)
            if cues.next, p.kind == "rest" || p.kind == "warmup", let nx = segments().first(where: { $0.phase.index > p.index }) {
                w += ". " + T("routine.next", title(nx.phase))
            }
            return w
        }
        if p.kind == "done" { return nil }
        if (1...3).contains(p.left), p.left != last.left { return "\(p.left)" }
        let d = duration(p)
        if cues.ten, d >= 20, last.left > 10, p.left <= 10 { return L("hiit.say.ten") }
        if cues.half, d >= 20, last.left > d / 2, p.left <= d / 2 { return L("hiit.say.half") }
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
    var routineJSON: [String: Any] { ["n": name, "rounds": rounds, "steps": steps.map { ["n": $0.n, "s": $0.s, "r": $0.r] as [String: Any] }] }

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
        if let text = cfg.cue(last: last, p, cues: .load()) { say(text) }
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
    /// Kaldırılanı geri almak için (2. analiz #133)
    @State private var removed: TimerItem?
    /// Süre girişi: tekerlek ya da tuş takımı (2. analiz #134)
    @AppStorage("timers.keypad") private var keypad = false
    @State private var digits = ""
    @State private var h = 0
    @State private var m = 5
    @State private var s = 0
    @State private var name = ""
    @State private var history = TimerHistory.load()
    /// Görsel (rakamsız) zamanlayıcı tam ekranı (2. analiz #140)
    @State private var visualID: String?

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
                    if let r = removed {
                        HStack {
                            Text(T("timers.removed", r.name.isEmpty ? clock(Int(r.total / 1000)) : r.name)).font(.footnote).foregroundStyle(Theme.muted)
                            Spacer()
                            Button(L("timers.undo")) {
                                items.append(r)
                                TimerItem.save(items)
                                if r.running, let id = UUID(uuidString: r.id) {
                                    let at = Date(timeIntervalSince1970: Double(r.end) / 1000), title = r.doneTitle
                                    Task { await AlarmCenter.schedule(id: id, at: at, title: title) }
                                }
                                removed = nil
                            }
                            .font(.footnote.weight(.semibold))
                        }
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
                    Picker("", selection: $keypad) {
                        Text(L("timers.input.wheel")).tag(false)
                        Text(L("timers.input.keypad")).tag(true)
                    }
                    .pickerStyle(.segmented)
                    if keypad {
                        DurationKeypad(digits: $digits) { hh, mm, ss in h = hh; m = mm; s = ss }
                    } else {
                        HStack(spacing: 0) {
                            wheel($h, 0..<24, L("tools.countdown.hoursShort"))
                            wheel($m, 0..<60, L("tools.countdown.minutesShort"))
                            wheel($s, 0..<60, L("tools.countdown.secondsShort"))
                        }
                        .frame(height: 150)
                        .environment(\.layoutDirection, .leftToRight)
                    }
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
                    // Kendiliğinden tekrar (2. analiz #133; uygulama açıkken)
                    if t.repeats == true { update(t.id) { start($0, $0.total) } }
                }
            }
        }
        .onAppear { items = TimerItem.load() }
        // Kilit ekranı / Dynamic Island: süren zamanlayıcılar
        .onChange(of: items) { LiveActivities.sync() }
        .fullScreenCover(isPresented: Binding(get: { visualID != nil }, set: { if !$0 { visualID = nil } })) {
            if let t = items.first(where: { $0.id == visualID }) { VisualTimerView(item: t) }
        }
    }

    private func row(_ t: TimerItem, now: Int64) -> some View {
        let rem = t.remaining(now)
        let done = t.running && rem <= 0
        let tint = (t.color ?? 0) > 0 && (t.color ?? 0) < CountdownItem.palette.count ? CountdownItem.palette[t.color ?? 0] : Theme.accent
        return HStack {
            // Kalan süre halkası (2. analiz #101)
            ZStack {
                Circle().stroke(Theme.surface2, lineWidth: 5)
                Circle().trim(from: 0, to: t.total > 0 ? CGFloat(Double(rem) / Double(t.total)) : 0)
                    .stroke(tint, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                if t.repeats == true { Image(systemName: "repeat").font(.caption2).foregroundStyle(tint) }
            }
            .frame(width: 40, height: 40)
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 0) {
                Text(t.name.isEmpty ? clock(Int(t.total / 1000)) : t.name).font(.footnote).foregroundStyle(Theme.muted).lineLimit(1)
                Text(done ? L("tools.countdown.finished") : clock(Int((rem + 999) / 1000)))
                    .font(.system(size: 32, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(done ? tint : Theme.text)
                    .environment(\.layoutDirection, .leftToRight)
                // Bitiş saati (2. analiz #133)
                if t.running && !done {
                    Text("🕓 " + Fmt.time(Date(timeIntervalSince1970: Double(t.end) / 1000))).font(.caption).foregroundStyle(Theme.muted)
                }
            }
            Spacer()
            // ±10 dk, +1 sa, tekrar, renk (2. analiz #133)
            Menu {
                Button("+10 " + L("tools.countdown.minutesShort")) { adjust(t, rem, 600_000) }
                Button("+1 " + L("tools.countdown.hoursShort")) { adjust(t, rem, 3_600_000) }
                Button("−1 " + L("tools.countdown.minutesShort")) { adjust(t, rem, -60_000) }
                Button("−10 " + L("tools.countdown.minutesShort")) { adjust(t, rem, -600_000) }
                Button(L("timers.visual")) { visualID = t.id }
                Toggle(L("timers.repeat"), isOn: Binding(get: { t.repeats ?? false }, set: { v in update(t.id) { var x = $0; x.repeats = v; return x } }))
                Picker(L("countdowns.color"), selection: Binding(get: { t.color ?? 0 }, set: { v in update(t.id) { var x = $0; x.color = v; return x } })) {
                    ForEach(0..<CountdownItem.palette.count, id: \.self) { i in
                        Label("\(i)", systemImage: i == 0 ? "circle.slash" : "circle.fill").tag(i)
                    }
                }
            } label: { Image(systemName: "ellipsis.circle") }
                .foregroundStyle(Theme.accent)
                .accessibilityLabel(L("timers.more"))
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
                removed = t
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

    /// Süreyi uzatır ya da kısaltır (sayarken bitiş, duraklatılmışken kalan; en az 1 sn)
    private func adjust(_ t: TimerItem, _ rem: Int64, _ delta: Int64) {
        if t.running && rem > 0 {
            cancel(t)
            update(t.id) { var x = $0; x.total = max(1000, x.total + delta); return start(x, max(1000, rem + delta)) }
        } else if !t.running {
            update(t.id) { var x = $0; x.total = max(1000, x.total + delta); x.left = max(1000, x.left + delta); return x }
        }
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
    @State private var locked = false
    @State private var reminded = ""

    private var active: Bool { start > 0 }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { context in
            let now = context.date.timeIntervalSince1970 * 1000
            let elapsed = active ? Int(((paused > 0 ? paused : now) - start) / 1000) : 0
            let phase = cfg.phase(elapsed)
            ScrollView {
                VStack(spacing: 16) {
                    if !active { settings; HiitHistoryCard() } else { running(phase, elapsed: elapsed, now: now) }
                    HiitCueOptions()
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

    @ViewBuilder private func running(_ phase: HiitConfig.Phase, elapsed: Int, now: Double) -> some View {
        if phase.kind == "done" {
            // Bitiş özeti (2. analiz #135)
            PhasePanel(kind: "done", progress: 1, height: 220) {
                VStack(spacing: 6) {
                    Text("🏁").font(.system(size: 44))
                    Text(L("hiit.done")).font(.title.bold())
                    if cfg.isRoutine { Text(cfg.name) }
                    Text(T("hiit.total", clock(cfg.totalSeconds)) + " · " + T("hiit.roundCount", cfg.rounds)).font(.subheadline)
                }
                .padding(.horizontal, 16)
            }
            if reminded.isEmpty {
                RoundButton(title: L("hiit.remindTomorrow")) { Task { await remindTomorrow() } }
            } else {
                Text(reminded).font(.subheadline).foregroundStyle(Theme.muted)
            }
            RoundButton(title: L("tools.countdown.reset"), prominent: true) {
                cancelEnd()
                start = 0
                paused = 0
                reminded = ""
            }
        } else {
            // Evre renkli pano: zemin evrenin rengi, alttan evrenin ilerlemesiyle dolar (2. analiz #101)
            let dur = max(1, cfg.duration(phase))
            let segs = cfg.segments()
            PhasePanel(kind: phase.kind, progress: 1 - Double(phase.left) / Double(dur)) {
                VStack(spacing: 4) {
                    Text(cfg.title(phase)).font(.largeTitle.bold()).lineLimit(1).minimumScaleFactor(0.5)
                    Text(clock(phase.left))
                        .font(.system(size: 84, weight: .light, design: .rounded)).monospacedDigit()
                        .lineLimit(1).minimumScaleFactor(0.5)
                        .environment(\.layoutDirection, .leftToRight)
                    if (phase.kind == "work" || phase.kind == "rest") && cfg.rounds > 1 { Text(T("hiit.round", phase.round, cfg.rounds)).font(.headline) }
                }
                .padding(.horizontal, 16)
            }
            ProgressView(value: min(1, Double(elapsed) / Double(max(1, cfg.totalSeconds)))).tint(PhaseColors.of(phase.kind))
            if locked {
                HoldToUnlock { locked = false }
            } else {
                // Adım atlama: ⏮ evrenin başına (ilk 3 sn içindeyse bir öncekine), ⏭ sıradakine
                let cur = segs.firstIndex { $0.phase.index == phase.index }
                HStack(spacing: 12) {
                    Button {
                        if let cur { jump(elapsed - segs[cur].start >= 3 || cur == 0 ? segs[cur].start : segs[cur - 1].start) }
                    } label: { Image(systemName: "backward.end.fill").font(.title2) }
                    .accessibilityLabel(L("hiit.prev"))
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
                    Button {
                        let nx = cur.flatMap { segs.indices.contains($0 + 1) ? segs[$0 + 1].start : nil }
                        jump(nx ?? cfg.totalSeconds)
                    } label: { Image(systemName: "forward.end.fill").font(.title2) }
                    .accessibilityLabel(L("hiit.skip"))
                }
                HStack(spacing: 12) {
                    RoundButton(title: L("tools.countdown.reset")) {
                        cancelEnd()
                        start = 0
                        paused = 0
                    }
                    RoundButton(title: "🔒 " + L("tools.lock")) { locked = true }
                }
            }
            // Sıradakiler ve bitiş saatleri (2. analiz #135)
            let upcoming = Array(segs.filter { $0.phase.index > phase.index }.prefix(4))
            if !upcoming.isEmpty {
                let ref = paused > 0 ? paused : now
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("hiit.upcoming")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
                    ForEach(upcoming, id: \.phase.index) { sg in
                        HStack(spacing: 8) {
                            Circle().fill(PhaseColors.of(sg.phase.kind)).frame(width: 10, height: 10)
                            Text(cfg.title(sg.phase)).lineLimit(1)
                            Spacer()
                            Text(clock(sg.dur)).monospacedDigit().foregroundStyle(Theme.muted)
                                .environment(\.layoutDirection, .leftToRight)
                            Text("🕓 " + Fmt.time(Date(timeIntervalSince1970: (ref + Double(sg.start + sg.dur - elapsed) * 1000) / 1000)))
                                .font(.footnote).foregroundStyle(Theme.muted)
                        }
                        .font(.subheadline)
                    }
                }
                .padding(16)
                .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            }
        }
    }

    /// Belirli saniyeye atlar (başlangıç anı kaydırılır; sürüyorsa bitiş alarmı yeniden kurulur)
    private func jump(_ sec: Int) {
        let ref = paused > 0 ? paused : Date().timeIntervalSince1970 * 1000
        cancelEnd()
        start = ref - Double(sec) * 1000
        if paused == 0 { scheduleEnd() }
    }

    /// Yarın aynı saatte bir kezlik alarm (antrenmanın adıyla)
    private func remindTomorrow() async {
        let begin = Date(timeIntervalSince1970: start / 1000)
        let at = Calendar.current.date(byAdding: .day, value: 1, to: begin) ?? begin.addingTimeInterval(86_400)
        let c = Calendar.current.dateComponents([.hour, .minute], from: at)
        var a = AlarmItem(hour: c.hour ?? 9, minute: c.minute ?? 0)
        a.label = String((cfg.isRoutine ? cfg.name : L("hiit.tab")).prefix(40))
        a.date = CalendarDay(at).iso
        var items = AlarmItem.load()
        items.append(a)
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
        reminded = T("alarm.quickSet", Fmt.time(at))
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

/// Görsel zamanlayıcı (2. analiz #140): rakamsız pasta ya da kum saati; çocuk modunda düğmeler gizli, basılı tutunca açılır
struct VisualTimerView: View {
    @Environment(\.dismiss) private var dismiss
    let item: TimerItem
    @AppStorage("timers.visual") private var style = "pie"
    @State private var kids = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.5)) { ctx in
            let rem = item.remaining(Int64(ctx.date.timeIntervalSince1970 * 1000))
            let f = item.total > 0 ? Double(rem) / Double(item.total) : 0
            VStack(spacing: 20) {
                Text(item.name.isEmpty ? clock(Int(item.total / 1000)) : item.name)
                    .font(.title2.weight(.semibold)).foregroundStyle(Color(white: 0.13))
                Spacer()
                if style == "sand" { Hourglass(left: f).frame(maxWidth: 260) } else { PieTimer(left: f).padding(.horizontal, 8) }
                Spacer()
                if kids {
                    HoldToUnlock { kids = false }
                } else {
                    Picker("", selection: $style) {
                        Text(L("timers.style.pie")).tag("pie")
                        Text(L("timers.style.sand")).tag("sand")
                    }
                    .pickerStyle(.segmented)
                    HStack {
                        Button("🧒 " + L("timers.kids")) { kids = true }
                        Spacer()
                        Button(L("common.done")) { dismiss() }
                    }
                    .foregroundStyle(Color(white: 0.13))
                }
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
            .environment(\.colorScheme, .light)
        }
        .interactiveDismissDisabled(kids)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }
}

/// Biten antrenmanlar (2. analiz #135): `hiit.log` [{d: yerel gün, n: ad, s: süre sn, r: tur}], en çok 200; yalnız cihazda
/// (Android'deki HiitLog ile aynı biçim)
enum HiitLog {
    struct Entry: Codable, Equatable { let d: String; let n: String; let s: Int; let r: Int }
    static let key = "hiit.log"

    static func load() -> [Entry] {
        guard let data = UserDefaults.standard.string(forKey: key)?.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([Entry].self, from: data)) ?? []
    }

    static func add(_ cfg: HiitConfig, start: Double) {
        let d = CalendarDay(Date(timeIntervalSince1970: start / 1000)).iso
        let list = Array(([Entry(d: d, n: cfg.isRoutine ? cfg.name : "", s: cfg.totalSeconds, r: cfg.rounds)] + load()).prefix(200))
        if let data = try? JSONEncoder().encode(list) { UserDefaults.standard.set(String(decoding: data, as: UTF8.self), forKey: key) }
    }

    static func clear() { UserDefaults.standard.removeObject(forKey: key) }

    /// Bu hafta (pazartesiden) biten antrenman sayısı
    static func thisWeek(_ today: Date = Date()) -> Int {
        let cal = Calendar(identifier: .gregorian)
        let back = (cal.component(.weekday, from: today) + 5) % 7
        let mon = CalendarDay(cal.date(byAdding: .day, value: -back, to: today) ?? today).iso
        return load().filter { $0.d >= mon }.count
    }
}

private struct HiitHistoryCard: View {
    @State private var log = HiitLog.load()
    @State private var confirm = false

    var body: some View {
        if !log.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(L("hiit.history")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
                    Spacer()
                    Button(L("hiit.historyClear")) { confirm = true }.font(.footnote)
                }
                Text(T("hiit.week", HiitLog.thisWeek()))
                ForEach(Array(log.prefix(6).enumerated()), id: \.offset) { _, e in
                    HStack {
                        Text(CalendarDay(iso: e.d).map { Fmt.dayMonth($0.noon(in: .current)) } ?? e.d).foregroundStyle(Theme.muted).frame(width: 96, alignment: .leading)
                        Text(e.n.isEmpty ? L("hiit.tab") : e.n).lineLimit(1)
                        Spacer()
                        Text(clock(e.s)).monospacedDigit().foregroundStyle(Theme.muted).environment(\.layoutDirection, .leftToRight)
                    }
                    .font(.subheadline)
                }
            }
            .padding(16)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            .confirmationDialog(L("hiit.historyClear"), isPresented: $confirm, titleVisibility: .visible) {
                Button(L("hiit.historyClear"), role: .destructive) { HiitLog.clear(); log = [] }
            }
        }
    }
}

/// Duyuru seçenekleri (2. analiz #135)
private struct HiitCueOptions: View {
    @AppStorage("hiit.cue.next") private var next = true
    @AppStorage("hiit.cue.half") private var half = false
    @AppStorage("hiit.cue.ten") private var ten = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("hiit.cues")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
            Toggle(L("hiit.cue.nextLabel"), isOn: $next)
            Toggle(L("hiit.cue.halfLabel"), isOn: $half)
            Toggle(L("hiit.cue.tenLabel"), isOn: $ten)
        }
        .tint(Theme.accent)
        .padding(16)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
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
            .navigationBarTitleDisplayMode(.large)
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

/// Süre girişi tuş takımı (2. analiz #134): rakamlar sağdan dolar (SS:DD:ss), ⌫ siler, 00 iki sıfır ekler
struct DurationKeypad: View {
    @Binding var digits: String
    let onChange: (Int, Int, Int) -> Void

    private var parts: (Int, Int, Int) {
        let d = String(repeating: "0", count: max(0, 6 - digits.count)) + digits.suffix(6)
        let a = Array(d)
        return (Int(String(a[0...1])) ?? 0, Int(String(a[2...3])) ?? 0, Int(String(a[4...5])) ?? 0)
    }

    var body: some View {
        let p = parts
        VStack(spacing: 10) {
            Text(String(format: "%02d:%02d:%02d", p.0, p.1, p.2))
                .font(.system(size: 44, weight: .light, design: .rounded)).monospacedDigit()
                .foregroundStyle(digits.isEmpty ? Theme.muted : Theme.text)
                .environment(\.layoutDirection, .leftToRight)
            Grid(horizontalSpacing: 10, verticalSpacing: 10) {
                ForEach([["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], ["00", "0", "⌫"]], id: \.self) { row in
                    GridRow {
                        ForEach(row, id: \.self) { k in
                            Button { press(k) } label: {
                                Text(k).font(.title2.weight(.medium)).frame(maxWidth: .infinity, minHeight: 48)
                                    .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 12))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(k == "⌫" ? L("timers.input.delete") : k)
                        }
                    }
                }
            }
            .environment(\.layoutDirection, .leftToRight)
        }
    }

    private func press(_ k: String) {
        if k == "⌫" { if !digits.isEmpty { digits.removeLast() } } else if digits.count < 6 { digits = String((digits + k).drop(while: { $0 == "0" }).prefix(6)) }
        let p = parts
        // Dakika ve saniye 59'u aşarsa taşar (90 sn → 1:30)
        let total = min(23 * 3600 + 59 * 60 + 59, p.0 * 3600 + p.1 * 60 + p.2)
        onChange(total / 3600, total % 3600 / 60, total % 60)
    }
}
