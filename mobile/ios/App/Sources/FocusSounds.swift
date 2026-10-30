import AVFoundation
import SwiftUI
import SaatCore

/* Pomodoro istatistikleri ve arka plan sesi (30 Ekim 2026, rakip analizi #16; sitedeki pomodoroyla aynı tasarım) ve
   uyku sesleri (#17) için ortak ses çalar. Kayıt "pomo.log" (JSON, sitedeki sth-pomo-log biçimi), yalnız telefonda. */

enum PomoLog {
    static let key = "pomo.log"

    static func load() -> [FocusLog.Entry] {
        guard let s = UserDefaults.standard.string(forKey: key), let d = s.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([FocusLog.Entry].self, from: d)) ?? []
    }

    static func save(_ log: [FocusLog.Entry]) {
        if let d = try? JSONEncoder().encode(log) { UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: key) }
    }

    /// Tamamlanan çalışma turu (çalışmanın bittiği yerel güne yazılır)
    static func record(minutes: Int, task: String, at: Date) {
        save(FocusLog.add(load(), .init(d: BadgeStore.day(at), m: minutes, t: task)))
    }

    /// Son 7 günün anahtarları, eskiden yeniye
    static func lastDays(_ now: Date = Date()) -> [String] {
        (0..<7).reversed().map { BadgeStore.day(Calendar.current.date(byAdding: .day, value: -$0, to: now) ?? now) }
    }
}

/// Gürültü türünün adı
func noiseName(_ kind: String) -> String {
    switch kind {
    case "white": L("tools.pomodoro.noiseWhite")
    case "pink": L("tools.pomodoro.noisePink")
    case "brown": L("tools.pomodoro.noiseBrown")
    case "rain": L("tools.pomodoro.noiseRain")
    case "waves": L("noise.waves")
    default: L("tools.pomodoro.noiseOff")
    }
}

/// Telefonda üretilen gürültüyü döngüde çalar (dosya yok). Ekran kilitliyken de sürer (arka plan ses modu, Info.plist);
/// `until` gelince durur, uyku seslerinde son 30 sn'de kısılarak. Aynı anda tek kaynak: pomodoro ya da uyku.
@MainActor
final class NoisePlayer {
    static let shared = NoisePlayer()
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private var buffers: [String: AVAudioPCMBuffer] = [:]
    private var timer: Timer?
    private(set) var kind = ""
    private(set) var source = ""
    private(set) var until: Date?
    private var fade = false
    private var gain: Float = 0.4
    private var observers: [NSObjectProtocol] = []

    var playing: Bool { !kind.isEmpty }

    private init() {
        engine.attach(player)
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { note in
            let ended = (note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt) == AVAudioSession.InterruptionType.ended.rawValue
            if ended { MainActor.assumeIsolated { NoisePlayer.shared.restart() } }
        })
        observers.append(center.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: .main) { _ in
            MainActor.assumeIsolated { NoisePlayer.shared.restart() }
        })
    }

    /// Çalar ya da çalanı günceller (aynı tür yeniden başlamaz)
    func play(kind: String, volume: Int, until: Date?, source: String, fadeOut: Bool = false) {
        guard NoiseGen.kinds.contains(kind) else { stop(); return }
        if let until, until <= Date() { stop(source: source); return }
        self.until = until
        self.source = source
        self.fade = fadeOut
        gain = NoiseGen.gain(volume)
        if self.kind == kind, engine.isRunning {
            applyVolume()
            return
        }
        self.kind = kind
        startEngine()
        if timer == nil {
            let t = Timer(timeInterval: 0.5, repeats: true) { _ in MainActor.assumeIsolated { NoisePlayer.shared.tick() } }
            RunLoop.main.add(t, forMode: .common)
            timer = t
        }
    }

    func setVolume(_ volume: Int) {
        gain = NoiseGen.gain(volume)
        applyVolume()
    }

    /// Durdurur; kaynak verilirse yalnız o kaynak çalıyorsa
    func stop(source: String? = nil) {
        if let source, source != self.source { return }
        timer?.invalidate()
        timer = nil
        kind = ""
        self.source = ""
        until = nil
        player.stop()
        engine.stop()
        // Aralık antrenmanının duyuruları sürüyorsa ses oturumu açık kalır
        let hiit = UserDefaults.standard.double(forKey: "hiit.start") > 0 && UserDefaults.standard.double(forKey: "hiit.paused") == 0
        if !hiit { try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation) }
    }

    private func tick() {
        guard let until else { return }
        if Date() >= until { stop(); return }
        applyVolume()
    }

    private func applyVolume() {
        var v = gain
        if fade, let until {
            let left = until.timeIntervalSinceNow
            if left < 30 { v *= Float(max(0, left) / 30) }
        }
        player.volume = v
    }

    private func buffer(_ kind: String) -> AVAudioPCMBuffer? {
        if let b = buffers[kind] { return b }
        let samples = NoiseGen.samples(kind)
        guard let format = AVAudioFormat(standardFormatWithSampleRate: Double(NoiseGen.rate), channels: 1),
              let b = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)),
              let data = b.floatChannelData else { return nil }
        b.frameLength = b.frameCapacity
        samples.withUnsafeBufferPointer { data[0].update(from: $0.baseAddress!, count: samples.count) }
        buffers = [kind: b] // bellekte tek arabellek
        return b
    }

    private func startEngine() {
        guard let b = buffer(kind) else { return }
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
        player.stop()
        engine.stop()
        engine.connect(player, to: engine.mainMixerNode, format: b.format)
        player.scheduleBuffer(b, at: nil, options: .loops)
        applyVolume()
        engine.prepare()
        do {
            try engine.start()
            player.play()
        } catch {
            kind = ""
        }
    }

    private func restart() {
        guard playing else { return }
        startEngine()
    }
}

/// Pomodoro gürültüsü: çalışma süresi işlerken ve tür seçiliyse çalar, süre bitince durur
enum PomoNoise {
    @MainActor
    static func sync() {
        let d = UserDefaults.standard
        let end = d.double(forKey: "pomo.end")
        let kind = d.string(forKey: "pomo.noise") ?? "off"
        let phase = d.string(forKey: "pomo.phase") ?? "work"
        let endDate = Date(timeIntervalSinceReferenceDate: end)
        if end > 0, endDate > Date(), phase == "work", kind != "off" {
            let vol = d.object(forKey: "pomo.vol") == nil ? 40 : d.integer(forKey: "pomo.vol")
            NoisePlayer.shared.play(kind: kind, volume: vol, until: endDate, source: "pomo")
        } else {
            NoisePlayer.shared.stop(source: "pomo")
        }
    }
}

/// Gürültü türü seçici ve ses düzeyi (pomodoro ve uyku sesleri)
struct NoiseControls: View {
    @Binding var kind: String
    @Binding var volume: Int
    var allowOff = true

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach((allowOff ? ["off"] : []) + NoiseGen.kinds, id: \.self) { k in
                        Button { kind = k } label: {
                            Text(noiseName(k))
                                .font(.subheadline.weight(kind == k ? .semibold : .regular))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .background(kind == k ? Theme.accent.opacity(0.22) : Theme.surface2, in: Capsule())
                                .foregroundStyle(kind == k ? Theme.accent : Theme.text)
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(kind == k ? .isSelected : [])
                    }
                }
            }
            HStack {
                Image(systemName: "speaker.wave.1").foregroundStyle(Theme.muted)
                Slider(value: Binding(get: { Double(volume) }, set: { volume = Int($0.rounded()) }), in: 0...100)
                    .tint(Theme.accent)
                    .accessibilityLabel(L("tools.pomodoro.volume"))
                Image(systemName: "speaker.wave.3").foregroundStyle(Theme.muted)
            }
        }
    }
}

/// Pomodoro istatistikleri: bugün, son 7 gün, toplam; 7 günlük çubuk grafik; en çok çalışılan 5 görev; geçmişi sil
struct FocusStatsCard: View {
    let log: [FocusLog.Entry]
    let onClear: () -> Void
    @State private var confirm = false

    var body: some View {
        let days = PomoLog.lastDays()
        let s = FocusLog.stats(log, days: days, noTask: L("tools.pomodoro.noTask"))
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(text: L("tools.pomodoro.statsHeading"))
            if log.isEmpty {
                Text(L("tools.pomodoro.empty")).font(.subheadline).foregroundStyle(Theme.muted)
            } else {
                stat(L("tools.pomodoro.today"), s.today)
                stat(L("tools.pomodoro.week"), s.week)
                stat(L("tools.pomodoro.total"), s.total)
                Text(L("tools.pomodoro.chartCaption")).font(.caption).foregroundStyle(Theme.muted)
                bars(days, s.perDay)
                if !s.top.isEmpty {
                    Text(L("tools.pomodoro.topTasks")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
                    ForEach(Array(s.top.enumerated()), id: \.offset) { _, t in
                        HStack {
                            Text(t.name).lineLimit(1)
                            Spacer()
                            Text(verbatim: "\(t.minutes) \(L("tools.pomodoro.minUnit"))").monospacedDigit().foregroundStyle(Theme.muted)
                        }
                        .font(.subheadline)
                    }
                }
                Button(L("tools.pomodoro.clear"), role: .destructive) { confirm = true }
                    .font(.subheadline)
            }
        }
        .card()
        .confirmationDialog(L("tools.pomodoro.clearConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("tools.pomodoro.clear"), role: .destructive, action: onClear)
        }
    }

    private func stat(_ title: String, _ sum: FocusLog.Sum) -> some View {
        HStack {
            Text(title).foregroundStyle(Theme.muted)
            Spacer()
            Text(T("tools.pomodoro.statLine", sum.n, sum.m)).monospacedDigit().foregroundStyle(Theme.text)
        }
        .font(.subheadline)
    }

    private func bars(_ days: [String], _ minutes: [Int]) -> some View {
        let top = max(1, minutes.max() ?? 1)
        return HStack(alignment: .bottom, spacing: 8) {
            ForEach(0..<7, id: \.self) { i in
                VStack(spacing: 4) {
                    Spacer(minLength: 0)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(i == 6 ? Theme.accent : Theme.accent.opacity(0.55))
                        .frame(height: max(3, 90 * CGFloat(minutes[i]) / CGFloat(top)))
                    Text(weekday(days[i])).font(.caption2).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(weekday(days[i])): \(minutes[i]) \(L("tools.pomodoro.minUnit"))")
            }
        }
        .frame(height: 112)
    }

    /// "2026-10-30" → "Cum"
    private func weekday(_ key: String) -> String {
        let p = key.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3, let d = Calendar(identifier: .gregorian).date(from: DateComponents(year: p[0], month: p[1], day: p[2])) else { return key }
        let w = Calendar(identifier: .gregorian).component(.weekday, from: d) // 1 = pazar
        return L("calendar.weekdaysShort.\((w + 5) % 7)")
    }
}
