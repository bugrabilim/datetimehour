import AVFoundation
import SwiftUI
import UIKit

/// Saniye tık sesi (2. analiz #10; sitedeki `sth-prefs.tick` gibi): saat ekranı ve tam ekran açıkken her saniye
/// kısa bir tık. Ses telefonda üretilir (dosya yok); oturum "ambient" olduğu için telefon sessizdeyken çalmaz ve
/// çalan müziği kesmez. Seçim `tickSound` (varsayılan kapalı).
@MainActor
final class TickSound {
    static let shared = TickSound()
    static let key = "tickSound"
    private var engine: AVAudioEngine?
    private var player: AVAudioPlayerNode?
    private var buffer: AVAudioPCMBuffer?
    private var last = 0

    func tick(_ second: Int) {
        guard UserDefaults.standard.bool(forKey: Self.key), second != last,
              UIApplication.shared.applicationState == .active else { return }
        last = second
        if engine == nil { setup() }
        guard let engine, let player, let buffer else { return }
        if !engine.isRunning { try? engine.start() }
        player.scheduleBuffer(buffer, at: nil, options: .interrupts)
        if !player.isPlaying { player.play() }
    }

    func stop() {
        player?.stop()
        engine?.stop()
        engine = nil
        player = nil
        buffer = nil
    }

    private func setup() {
        let session = AVAudioSession.sharedInstance()
        // Pomodoro/uyku sesi ya da aralık antrenmanı çalıyorsa oturuma dokunulmaz
        if session.category != .playback {
            try? session.setCategory(.ambient, options: [.mixWithOthers])
            try? session.setActive(true)
        }
        let e = AVAudioEngine()
        let p = AVAudioPlayerNode()
        guard let format = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 1) else { return }
        e.attach(p)
        e.connect(p, to: e.mainMixerNode, format: format)
        let n = 1323 // 30 ms
        guard let b = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(n)),
              let ch = b.floatChannelData?[0] else { return }
        b.frameLength = AVAudioFrameCount(n)
        var seed: UInt32 = 12345
        for i in 0..<n {
            let t = Double(i) / 44100
            seed = seed &* 1664525 &+ 1013904223
            let noise = Double(Int32(bitPattern: seed)) / Double(Int32.max)
            let env = exp(-t / 0.004)
            ch[i] = Float((0.6 * sin(2 * .pi * 2200 * t) + 0.4 * noise) * env * 0.35)
        }
        engine = e
        player = p
        buffer = b
    }
}

/// Tabela metni (2. analiz #22): sözcükler bölünmesin diye satırlar sözcük sınırından seçilir; 1–3 satır arasından
/// yazının en büyük çıkacağı bölünme alınır (harf genişliği ~0,6 em varsayımıyla). `aspect` = genişlik / yükseklik.
func bannerLines(_ text: String, aspect: Double) -> String {
    let words = text.split(whereSeparator: \.isWhitespace).map(String.init)
    guard words.count > 1 else { return text.trimmingCharacters(in: .whitespaces) }
    var best = words.joined(separator: " "), bestSize = 0.0
    let n = words.count
    var splits: [[Int]] = [[n]]
    for a in 1..<n { splits.append([a, n]) }
    if n >= 3 { for a in 1..<(n - 1) { for b in (a + 1)..<n { splits.append([a, b, n]) } } }
    for sp in splits {
        var from = 0
        let lines = sp.map { to -> String in defer { from = to }; return words[from..<to].joined(separator: " ") }
        let longest = Double(lines.map(\.count).max() ?? 1)
        let size = min(aspect / (0.6 * longest), 1 / (1.05 * Double(lines.count)))
        if size > bestSize { bestSize = size; best = lines.joined(separator: "\n") }
    }
    return best
}

/// İkili saat diliminin ikinci şehri (2. analiz #12): `dualCity` (cities.json anahtarı); yoksa Londra
/// (burası Londra ise New York)
enum DualZone {
    static let key = "dualCity"

    static func city(_ key: String) -> City? {
        if let c = City.all.first(where: { $0.key == key }) { return c }
        let fallback = TimeZone.current.identifier == "Europe/London" ? "new-york" : "london"
        return City.all.first { $0.key == fallback } ?? City.all.first
    }
}

/// 21. saat modeli: iki analog kadran; solda burası, sağda seçilen şehir (kadranın içinde kısa kod ve fark).
/// Şehrin adına dokununca şehir seçilir.
struct DualClockPart: View {
    let now: Date
    let width: CGFloat
    let height: CGFloat
    @AppStorage(DualZone.key) private var cityKey = ""
    @AppStorage("secA") private var seconds = true
    @AppStorage(SecondsMotion.key) private var motionRaw = SecondsMotion.tick.rawValue
    @State private var picking = false

    var body: some View {
        let city = DualZone.city(cityKey)
        let zone = city?.zone ?? .current
        let d = min(width * 0.46, height * 0.72)
        let motion = SecondsMotion(rawValue: motionRaw) ?? .tick
        HStack(alignment: .top, spacing: width * 0.04) {
            column(date: now, zone: .current, size: d, title: PlaceModel.shared.place.name.isEmpty ? L("dual.here") : PlaceModel.shared.place.name,
                   caption: nil, motion: motion)
            column(date: now, zone: zone, size: d, title: city?.displayName ?? "",
                   caption: city.map { ZoneCaption.code($0.key) + " " + ZoneCaption.diff(zone, from: .current, at: now) }, motion: motion)
                .onTapGesture { picking = true }
        }
        .sheet(isPresented: $picking) {
            NavigationStack {
                List(City.all) { c in
                    Button {
                        cityKey = c.key
                        picking = false
                    } label: {
                        HStack {
                            Text(c.displayName).foregroundStyle(Theme.text)
                            Spacer()
                            Text(ZoneCaption.diff(c.zone, from: .current, at: Date())).foregroundStyle(Theme.muted).monospacedDigit()
                        }
                    }
                }
                .themedBackground()
                .navigationTitle(L("dual.city"))
                .navigationBarTitleDisplayMode(.large)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { picking = false } } }
            }
        }
    }

    private func column(date: Date, zone: TimeZone, size: CGFloat, title: String, caption: String?, motion: SecondsMotion) -> some View {
        VStack(spacing: size * 0.06) {
            Group {
                if seconds && motion != .tick {
                    TimelineView(.animation(minimumInterval: motion.interval)) { ctx in
                        AnalogClock(date: ctx.date, showSeconds: true, motion: motion, timeZone: zone, caption: caption)
                    }
                } else {
                    AnalogClock(date: date, showSeconds: seconds, timeZone: zone, caption: caption)
                }
            }
            .frame(width: size, height: size)
            .onTapGesture {
                seconds.toggle()
                BadgeStore.shared.hit(.saniye)
            }
            Text(title).font(.system(size: size * 0.09, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
            Text(Fmt.time(date, tz: zone) + (Fmt.ampm(date, tz: zone).isEmpty ? "" : " " + Fmt.ampm(date, tz: zone)))
                .font(.system(size: size * 0.085, weight: .medium, design: .rounded)).monospacedDigit()
                .foregroundStyle(Theme.muted)
                .environment(\.layoutDirection, .leftToRight)
        }
        .frame(width: size)
    }
}

/// Tam ekranda saat modellerinin iki yanındaki araç sayfaları (2. analiz #26): ilk saatten geriye pomodoro,
/// son saatten sonra kronometre. Durum araçların kendi kayıtlarıdır (`pomo.*`, `sw.*`), araç sayfasıyla aynıdır.
enum FullscreenTool: String {
    case pomodoro = "tool.pomo", stopwatch = "tool.sw"
}

struct FullscreenToolPage: View {
    let tool: FullscreenTool
    @AppStorage("pomo.phase") private var phase = "work"
    @AppStorage("pomo.round") private var round = 1
    @AppStorage("pomo.rounds") private var rounds = 4
    @AppStorage("pomo.end") private var end = 0.0
    @AppStorage("pomo.left") private var left = 0.0
    @AppStorage("sw.start") private var swStart = 0.0
    @AppStorage("sw.acc") private var swAcc = 0.0

    var body: some View {
        GeometryReader { g in
            let big = min(g.size.width * 0.22, g.size.height * 0.32)
            TimelineView(.animation(minimumInterval: tool == .stopwatch ? 0.05 : 0.5)) { ctx in
                VStack(spacing: big * 0.25) {
                    switch tool {
                    case .pomodoro: pomodoro(ctx.date, big: big)
                    case .stopwatch: stopwatch(ctx.date, big: big)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    @ViewBuilder private func pomodoro(_ now: Date, big: CGFloat) -> some View {
        let remaining = PomoEngine.remaining(at: now)
        Text(L("tools.pomodoro.\(phase)")).font(.system(size: big * 0.28, weight: .semibold))
            .foregroundStyle(phase == "work" ? Theme.accent : CalendarData.color("religious"))
        Text(clock(Int(remaining.rounded(.up)))).font(ClockFont.current.font(big)).monospacedDigit()
            .lineLimit(1).minimumScaleFactor(0.4).environment(\.layoutDirection, .leftToRight)
        Text(T("tools.pomodoro.round", round, rounds)).font(.system(size: big * 0.2)).foregroundStyle(Theme.muted)
        HStack(spacing: 28) {
            Button { PomoEngine.advance() } label: { Image(systemName: "forward.end.fill") }
                .accessibilityLabel(L("tools.pomodoro.skip"))
            Button {
                if end > 0 { PomoEngine.pause(at: now) } else { PomoEngine.start(seconds: left > 0 ? left : PomoEngine.phaseSeconds) }
            } label: {
                Image(systemName: end > 0 ? "pause.circle.fill" : "play.circle.fill").font(.system(size: big * 0.5))
            }
            .accessibilityLabel(L(end > 0 ? "tools.pomodoro.pause" : "tools.pomodoro.start"))
        }
        .font(.system(size: big * 0.3))
        .foregroundStyle(Theme.accent)
        .onChange(of: end > 0 && remaining <= 0) { _, finished in
            if finished { PomoEngine.advance(completed: true) }
        }
    }

    @ViewBuilder private func stopwatch(_ now: Date, big: CGFloat) -> some View {
        let t = swAcc + (swStart > 0 ? now.timeIntervalSinceReferenceDate - swStart : 0)
        let cs = Int((t * 100).rounded(.down))
        Text(L("tools.hub.cards.stopwatch.name")).font(.system(size: big * 0.28, weight: .semibold)).foregroundStyle(Theme.accent)
        Text(clock(cs / 100) + String(format: ".%02d", cs % 100)).font(ClockFont.current.font(big)).monospacedDigit()
            .lineLimit(1).minimumScaleFactor(0.4).environment(\.layoutDirection, .leftToRight)
        HStack(spacing: 28) {
            Button {
                swAcc = 0
                swStart = swStart > 0 ? Date().timeIntervalSinceReferenceDate : 0
            } label: { Image(systemName: "arrow.counterclockwise") }
                .accessibilityLabel(L("tools.stopwatch.reset"))
            Button {
                if swStart > 0 {
                    swAcc = t
                    swStart = 0
                } else {
                    swStart = Date().timeIntervalSinceReferenceDate
                    BadgeStore.shared.hit(.kronometre)
                }
                LiveActivities.sync()
            } label: {
                Image(systemName: swStart > 0 ? "pause.circle.fill" : "play.circle.fill").font(.system(size: big * 0.5))
            }
            .accessibilityLabel(L(swStart > 0 ? "tools.stopwatch.stop" : "tools.stopwatch.start"))
        }
        .font(.system(size: big * 0.3))
        .foregroundStyle(Theme.accent)
    }
}
