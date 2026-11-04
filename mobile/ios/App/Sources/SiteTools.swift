import AVFoundation
import SaatCore
import SwiftUI

/*
 * Sitedeki dokuz araç uygulamada (4 Kasım 2026, sahibin isteği: "satranç saati vardı, şimdi bulamadım"): satranç saati,
 * metronom, konuşan saat, burç hesaplama, saat hesaplama, köprü tatil, emeklilik (yalnız Türkçe), sınav zamanlayıcısı ve
 * sınıf araçları. Hesaplar sitedeki betiklerle aynı (src/js/more-tools.js, src/js/retire.js, lib/bridges.mjs); metinler
 * sitedeki anahtarlarla. Android'deki SiteTools.kt ile aynı kayıt anahtarları ("exam", "classroom", "metro").
 */

// MARK: Hesaplar

/// Saat hesaplama (sitedeki timecalc)
enum TimeCalc {
    /// İki saat arası (dakika) ve gece yarısı geçildi mi
    static func between(_ a: Int, _ b: Int) -> (Int, Bool) {
        let d = b - a
        return d < 0 ? (d + 1440, true) : (d, false)
    }

    /// Toplam saniye ve okunamayan satırlar (1'den); satır: 45 (dk), 1:30, 2:15:30, başında − ya da +
    static func sum(_ text: String) -> (Int, [Int]) {
        var total = 0
        var bad: [Int] = []
        for (i, line) in text.components(separatedBy: "\n").enumerated() {
            var l = line.filter { !$0.isWhitespace }
            if l.isEmpty { continue }
            var sign = 1
            if let f = l.first, "-−–".contains(f) { sign = -1; l.removeFirst() } else if l.first == "+" { l.removeFirst() }
            let p = l.split(separator: ":", omittingEmptySubsequences: false).flatMap { $0.split(separator: ".", omittingEmptySubsequences: false) }
            let n = p.compactMap { s -> Int? in s.allSatisfy(\.isASCII) && s.allSatisfy(\.isNumber) && !s.isEmpty ? Int(s) : nil }
            if p.count > 3 || n.count != p.count { bad.append(i + 1); continue }
            total += sign * (n.count == 1 ? n[0] * 60 : n.count == 2 ? n[0] * 3600 + n[1] * 60 : n[0] * 3600 + n[1] * 60 + n[2])
        }
        return (total, bad)
    }

    /// Dakikayı günün saati olarak (uygulamanın 24/12 ayarıyla)
    static func clock(_ min: Int) -> String {
        let x = ((min % 1440) + 1440) % 1440
        if UserDefaults.standard.bool(forKey: "h12") {
            let h = x / 60 % 12 == 0 ? 12 : x / 60 % 12
            return String(format: "%d:%02d %@", h, x % 60, x < 720 ? "AM" : "PM")
        }
        return String(format: "%02d:%02d", x / 60, x % 60)
    }

    static func decimal(_ v: Double) -> String {
        let f = NumberFormatter()
        f.locale = AppLocale.numberLocale
        f.maximumFractionDigits = 2
        f.minimumFractionDigits = 0
        return f.string(from: NSNumber(value: v)) ?? String(v)
    }
}

/// Burç (sitedeki zodiac): Güneş'in ve Ay'ın tutulum boylamı, tropikal zodyak
enum Zodiac {
    static let symbols = ["♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"]

    static func sunLon(_ ms: Double) -> Double {
        let d = ms / 86_400_000 + 2440587.5 - 2451545
        let g = (357.529 + 0.98560028 * d) * .pi / 180
        let q = 280.459 + 0.98564736 * d
        return ((q + 1.915 * sin(g) + 0.020 * sin(2 * g)).truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
    }

    static func moonLon(_ ms: Double) -> Double {
        let p = Nature.moonPosition(atMilliseconds: ms)
        let d = ms / 86_400_000 + 2440587.5 - 2451545
        let e = (23.439 - 0.00000036 * d) * .pi / 180
        let a = p.rightAscension * .pi / 180, dec = p.declination * .pi / 180
        let lon = atan2(sin(a) * cos(e) + tan(dec) * sin(e), cos(a)) * 180 / .pi
        return (lon.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
    }

    static func sign(_ lon: Double) -> Int { Int(floor(lon / 30)) % 12 }
    static func label(_ i: Int) -> String { symbols[i] + "\u{FE0E} " + L("zodiac.signs.\(i)") }
}

/// Emeklilik (sitedeki retire.js; yalnız 4/a, Türkiye mevzuatı). Tarihler UTC gece yarısı, JS Date.UTC taşma kuralıyla.
enum Retire {
    struct Cond { let kind: String; let n: Int; let at: Date? }
    struct Route { let key: String; let conds: [Cond]; let at: Date? }
    struct Result { let group: String; let routes: [Route]; let best: Route? }

    static var cal: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        return c
    }()

    static func day(_ y: Int, _ m: Int, _ d: Int) -> Date { cal.date(from: DateComponents(year: y, month: m, day: d))! }

    /// JS Date.UTC(y + dy, m + dm, d + dd) gibi taşmalı ekleme
    static func addYMD(_ t: Date, _ y: Int, _ mo: Int = 0, _ d: Int = 0) -> Date {
        let c = cal.dateComponents([.year, .month, .day], from: t)
        let jan = day(c.year! + y, 1, 1)
        let m = cal.date(byAdding: .month, value: c.month! - 1 + mo, to: jan)!
        return cal.date(byAdding: .day, value: c.day! - 1 + d, to: m)!
    }

    private static let bWomen: [[Int]] = [[18, 0, 0, 5000], [17, 0, 0, 5000], [16, 0, 0, 5075], [15, 0, 0, 5150], [14, 0, 0, 5225], [13, 0, 0, 5300],
        [12, 0, 0, 5375], [11, 0, 0, 5450], [10, 0, 0, 5525], [9, 0, 0, 5600], [8, 0, 0, 5675], [7, 0, 0, 5750], [6, 0, 0, 5825], [5, 0, 0, 5900],
        [4, 0, 0, 5975], [3, 0, 0, 5975], [2, 8, 15, 5975]]
    private static let bMen: [[Int]] = [[23, 0, 0, 5000], [21, 6, 0, 5000], [20, 0, 0, 5075], [18, 6, 0, 5150], [17, 0, 0, 5225], [15, 6, 0, 5300],
        [14, 0, 0, 5375], [12, 6, 0, 5450], [11, 0, 0, 5525], [9, 6, 0, 5600], [8, 0, 0, 5675], [6, 6, 0, 5750], [5, 0, 0, 5825], [3, 6, 0, 5900],
        [2, 8, 15, 5975]]

    private static func postAge(_ t: Date, female: Bool) -> Int {
        let y = cal.component(.year, from: t)
        for r in [[2036, 58, 60], [2038, 59, 61], [2040, 60, 62], [2042, 61, 63], [2044, 62, 64], [2046, 63, 65], [2048, 64, 65], [99999, 65, 65]]
        where y < r[0] { return female ? r[1] : r[2] }
        return 65
    }

    static func calc(female f: Bool, birth: Date, start: Date, days: Int, rate: Int, today: Date) -> Result {
        let law1999 = day(1999, 9, 8), law2008 = day(2008, 5, 1), refB = day(2002, 5, 23)
        func dayDate(_ need: Int) -> Date? {
            if days >= need { return today }
            if rate <= 0 { return nil }
            return cal.date(byAdding: .day, value: Int(ceil(Double(need - days) * 365.25 / Double(rate))), to: today)
        }
        func route(_ key: String, _ conds: [Cond]) -> Route {
            Route(key: key, conds: conds, at: conds.contains { $0.at == nil } ? nil : conds.compactMap(\.at).max())
        }
        func cDays(_ n: Int) -> Cond { Cond(kind: "days", n: n, at: dayDate(n)) }
        func cYears(_ n: Int) -> Cond { Cond(kind: "years", n: n, at: addYMD(start, n)) }
        func cAge(_ n: Int) -> Cond { Cond(kind: "age", n: n, at: addYMD(birth, n)) }
        var routes: [Route] = []
        let group: String
        if start < law1999 {
            let old = start <= addYMD(law1999, f ? -18 : -23)
            var need = 5975
            if old { need = 5000 } else {
                for r in (f ? bWomen : bMen) where start <= addYMD(refB, -r[0], -r[1], -r[2]) { need = r[3]; break }
            }
            group = old ? "a" : "b"
            routes.append(route("full", [cDays(need), cYears(f ? 20 : 25), Cond(kind: "noAge", n: 0, at: start)]))
            routes.append(route("partialPre", [cDays(3600), cYears(15), cAge(old ? (f ? 50 : 55) : f ? 58 : 60)]))
        } else if start < law2008 {
            group = "mid"
            routes.append(route("mid1", [cDays(7000), cAge(f ? 58 : 60)]))
            routes.append(route("mid2", [cDays(4500), cYears(25), cAge(f ? 58 : 60)]))
        } else {
            group = "post"
            let d72 = dayDate(7200), d54 = dayDate(5400)
            let a1 = d72.map { postAge($0, female: f) } ?? (f ? 58 : 60)
            let a2 = min(65, (d54.map { postAge($0, female: f) } ?? (f ? 58 : 60)) + 3)
            routes.append(route("full", [cDays(7200), cAge(a1)]))
            routes.append(route("partialPost", [cDays(5400), cAge(a2)]))
        }
        let best = routes.filter { $0.at != nil }.min { $0.at! < $1.at! }
        return Result(group: group, routes: routes, best: best)
    }

    static func diffText(_ a: Date, _ b: Date) -> String {
        let x = cal.dateComponents([.year, .month, .day], from: a), y = cal.dateComponents([.year, .month, .day], from: b)
        var m = (y.year! - x.year!) * 12 + (y.month! - x.month!)
        if y.day! < x.day! { m -= 1 }
        var parts: [String] = []
        if m >= 12 { parts.append(T("retire.years", m / 12)) }
        if m % 12 != 0 { parts.append(T("retire.months", m % 12)) }
        return parts.isEmpty ? T("retire.months", 0) : parts.joined(separator: " ")
    }

    static func ageAt(_ birth: Date, _ at: Date) -> Int {
        var age = cal.component(.year, from: at) - cal.component(.year, from: birth)
        if addYMD(birth, age) > at { age -= 1 }
        return age
    }

    static func show(_ d: Date) -> String { Fmt.longDate(d, tz: TimeZone(identifier: "UTC")!) }
}

/// Köprü tatil verisi (gen-strings; sitedeki /kopru-tatil/ ile aynı hesap)
enum Bridges {
    struct Item: Decodable, Hashable {
        let g: [String]
        let from: String
        let to: String
        let days: Int
        let leave: Double
        let leaveDays: [String]
        let halfDays: [String]
        let best: Bool
    }
    struct Year: Decodable { let projected: Bool; let list: [Item] }

    static let years: [Int: Year] = {
        guard let url = Bundle.main.url(forResource: "bridges", withExtension: "json"), let data = try? Data(contentsOf: url),
              let raw = try? JSONDecoder().decode([String: Year].self, from: data) else { return [:] }
        return Dictionary(uniqueKeysWithValues: raw.compactMap { k, v in Int(k).map { ($0, v) } })
    }()

    static func groupName(_ g: String) -> String { g == "rb" || g == "kb" ? L("bridge.groups.\(g)") : L("calendar.events.\(g)") }
    static func date(_ iso: String) -> String { CalendarDay(iso: iso).map { Fmt.dayMonth($0.noon(in: .current)) + " " + String($0.year) } ?? iso }
}

/// Sınıf araçlarının adları: satır, virgül ya da noktalı virgülle; tekrarsız, en çok 300
enum Classroom {
    static let key = "classroom"
    static func names(_ text: String) -> [String] {
        var seen = Set<String>(), out: [String] = []
        for part in text.components(separatedBy: CharacterSet(charactersIn: "\n,،、，;")) {
            let x = String(part.trimmingCharacters(in: .whitespaces).prefix(60))
            if !x.isEmpty && seen.insert(x).inserted { out.append(x) }
        }
        return Array(out.prefix(300))
    }
}

// MARK: Ses

/// Bip sesleri ve metronom: AVAudioEngine'de üretilen kısa tamponlar (dosya yok); metronom bir ölçüyü döngüde çalar
final class ToolAudio {
    static let shared = ToolAudio()
    private let engine = AVAudioEngine()
    private let beepNode = AVAudioPlayerNode()
    private let loopNode = AVAudioPlayerNode()
    private let format = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 1)!
    private var started = false
    private var loopFrames: AVAudioFramePosition = 1
    private let rate = 44100.0

    private func ensure() {
        if !started {
            engine.attach(beepNode)
            engine.attach(loopNode)
            engine.connect(beepNode, to: engine.mainMixerNode, format: format)
            engine.connect(loopNode, to: engine.mainMixerNode, format: format)
            started = true
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        if !engine.isRunning { try? engine.start() }
    }

    /// Sesler: (frekans, başlangıç ms, süre ms, düzey)
    func play(_ tones: [(Double, Int, Int, Float)]) {
        ensure()
        let total = tones.map { Int(rate * Double($0.1 + $0.2) / 1000) }.max() ?? 0
        guard total > 0, let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(total)) else { return }
        buf.frameLength = AVAudioFrameCount(total)
        let p = buf.floatChannelData![0]
        for i in 0..<total { p[i] = 0 }
        for (freq, startMs, durMs, vol) in tones {
            let s = Int(rate * Double(startMs) / 1000), n = Int(rate * Double(durMs) / 1000)
            for i in 0..<n where s + i < total {
                let env = min(1, Double(i) / 200) * min(1, Double(n - i) / 400)
                p[s + i] += Float(sin(2 * .pi * freq * Double(i) / rate) * env) * vol
            }
        }
        beepNode.scheduleBuffer(buf, at: nil, options: [], completionHandler: nil)
        if !beepNode.isPlaying { beepNode.play() }
    }

    func beep(_ freq: Double, ms: Int, vol: Float = 0.35, times: Int = 1, gapMs: Int = 120) {
        play((0..<times).map { (freq, $0 * (ms + gapMs), ms, vol) })
    }

    func chime() { play([(880, 0, 250, 0.3), (1318.5, 180, 400, 0.25)]) }

    /// Metronom: bir ölçü (vuruş × bölme) tek tampona yazılır ve döngüde çalar (örnek hassasiyetinde)
    func startMetronome(bpm: Int, beats: Int, sub: Int, accent: Bool, vol: Int) {
        ensure()
        let per = rate * 60 / Double(bpm) / Double(sub), n = beats * sub
        let frames = Int((per * Double(n)).rounded())
        guard let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(frames)) else { return }
        buf.frameLength = AVAudioFrameCount(frames)
        let p = buf.floatChannelData![0]
        for i in 0..<frames { p[i] = 0 }
        let v = Double(vol) / 100 * 0.6
        for idx in 0..<n {
            let main = idx % sub == 0, beat = idx / sub
            let acc = main && beat == 0 && accent && beats > 1
            let freq = !main ? 784.0 : acc ? 1568.0 : 1046.5
            let amp = !main ? v * 0.45 : acc ? v : v * 0.8
            let len = Int(rate * (main ? 0.05 : 0.03)), s = Int((per * Double(idx)).rounded())
            var phase = 0.0
            for i in 0..<len where s + i < frames {
                phase += freq / rate
                let sq: Double = phase.truncatingRemainder(dividingBy: 1) < 0.5 ? 1 : -1
                p[s + i] = Float(sq * amp * min(1, Double(len - i) / 300) * 0.5)
            }
        }
        loopNode.stop()
        loopNode.scheduleBuffer(buf, at: nil, options: .loops, completionHandler: nil)
        loopFrames = AVAudioFramePosition(frames)
        loopNode.play()
    }

    func stopMetronome() { loopNode.stop() }

    /// Çalan vuruş (−1: çalmıyor)
    func currentBeat(bpm: Int) -> Int {
        guard loopNode.isPlaying, let nt = loopNode.lastRenderTime, let pt = loopNode.playerTime(forNodeTime: nt), pt.sampleTime >= 0 else { return -1 }
        let pos = Double(pt.sampleTime % loopFrames)
        return Int(pos / (rate * 60 / Double(bpm)))
    }
}

// MARK: Ortak görünümler

private struct Pill: View {
    let title: String
    let on: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title).font(.subheadline)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(on ? Theme.accent : Theme.surface2, in: Capsule())
                .foregroundStyle(on ? Color.white : Theme.text)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

private struct Note: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View { Text(text).font(.caption).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading) }
}

/// JSON kaydı (Android ile aynı biçim)
private enum Store {
    static func load(_ key: String) -> [String: Any] {
        guard let s = UserDefaults.standard.string(forKey: key), let d = s.data(using: .utf8),
              let o = try? JSONSerialization.jsonObject(with: d) as? [String: Any] else { return [:] }
        return o
    }
    static func save(_ key: String, _ o: [String: Any]) {
        if let d = try? JSONSerialization.data(withJSONObject: o), let s = String(data: d, encoding: .utf8) { UserDefaults.standard.set(s, forKey: key) }
    }
}

// MARK: Satranç saati

@MainActor @Observable
final class ChessModel {
    var mins = 5
    var inc = 0
    var time: [Double] = [300, 300] // 0 beyaz, 1 siyah (sn)
    var moves = [0, 0]
    var turn = -1
    var running = false
    var flag = -1
    private var last = Date()

    func setup() {
        time = [Double(mins * 60), Double(mins * 60)]; moves = [0, 0]; turn = -1; running = false; flag = -1
    }

    func step() {
        guard running, turn >= 0 else { return }
        let now = Date()
        time[turn] -= now.timeIntervalSince(last)
        last = now
        if time[turn] <= 0 {
            time[turn] = 0; flag = turn; running = false
            ToolAudio.shared.beep(988, ms: 250, times: 3, gapMs: 100)
        }
    }

    func press(_ side: Int) {
        guard flag < 0 else { return }
        if turn < 0 { turn = 1 - side; running = true; last = Date(); ToolAudio.shared.beep(1200, ms: 50, vol: 0.2); return }
        guard running, turn == side else { return }
        step()
        guard flag < 0 else { return }
        time[side] += Double(inc); moves[side] += 1; turn = 1 - side; last = Date()
        ToolAudio.shared.beep(1200, ms: 50, vol: 0.2)
    }

    func pauseResume() {
        guard turn >= 0, flag < 0 else { return }
        if running { step(); running = false } else { last = Date(); running = true }
    }

    static func fmt(_ t: Double) -> String {
        let v = max(0, t)
        if v < 10 { return String(format: "%.1f", floor(v * 10) / 10) }
        let s = Int(ceil(v)), h = s / 3600, m = (s % 3600) / 60
        return (h > 0 ? String(format: "%d:%02d", h, m) : String(format: "%02d", m)) + String(format: ":%02d", s % 60)
    }
}

struct ChessClockView: View {
    @State private var m = ChessModel()
    @State private var custom = false
    private let presets = [(1, 0), (3, 0), (3, 2), (5, 0), (5, 3), (10, 0), (15, 10), (30, 0)]
    private let tick = Timer.publish(every: 0.05, on: .main, in: .common).autoconnect()

    private func side(_ s: Int) -> some View {
        let active = m.turn == s && m.flag < 0
        let bg: Color = m.flag == s ? Color(red: 0.79, green: 0.16, blue: 0.16) : active ? Theme.accent : Theme.surface2
        let fg: Color = active || m.flag == s ? .white : Theme.text
        let name = L(s == 0 ? "chess.white" : "chess.black")
        return Button { m.press(s) } label: {
            VStack(spacing: 4) {
                Text(name).font(.subheadline)
                Text(verbatim: ChessModel.fmt(m.time[s])).font(.system(size: 64, weight: .light)).monospacedDigit()
                Text(m.moves[s] > 0 ? T("chess.moves", m.moves[s]) : " ").font(.footnote).opacity(0.8)
                if m.flag == s { Text(T("chess.flag", name)).font(.headline) }
            }
            .foregroundStyle(fg)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(bg, in: RoundedRectangle(cornerRadius: 24))
        }
        .buttonStyle(.plain)
    }

    var body: some View {
        VStack(spacing: 10) {
            // Karşıdaki oyuncu için siyahın saati ters döner
            side(1).rotationEffect(.degrees(180))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(0..<presets.count, id: \.self) { i in
                        let p = presets[i]
                        Pill(title: "\(p.0) + \(p.1)", on: !custom && m.mins == p.0 && m.inc == p.1) { custom = false; m.mins = p.0; m.inc = p.1; m.setup() }
                    }
                    Pill(title: L("chess.custom"), on: custom) { custom = true }
                }
            }
            if custom {
                HStack {
                    Stepper("\(L("chess.minutes")): \(m.mins)", value: Binding(get: { m.mins }, set: { m.mins = $0; m.setup() }), in: 1...180)
                    Stepper("\(L("chess.increment")): \(m.inc)", value: Binding(get: { m.inc }, set: { m.inc = $0 }), in: 0...60)
                }
                .font(.footnote)
            }
            HStack(spacing: 10) {
                RoundButton(title: L(m.running ? "chess.pause" : "chess.resume")) { m.pauseResume() }
                RoundButton(title: L("chess.reset")) { m.setup() }
            }
            side(0)
            if m.turn < 0 { Note(L("chess.appHint")) }
        }
        .padding(12)
        .background(AppBackdrop().ignoresSafeArea())
        .onReceive(tick) { _ in m.step() }
        .onChange(of: m.running) { _, r in UIApplication.shared.isIdleTimerDisabled = r }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .navigationTitle(L("chess.h1"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Metronom

struct MetronomeView: View {
    @State private var bpm = 100
    @State private var beats = 4
    @State private var sub = 1
    @State private var accent = true
    @State private var vol = 70.0
    @State private var playing = false
    @State private var taps: [Date] = []
    private let names: [(Int, String)] = [(40, "Grave"), (60, "Largo"), (66, "Larghetto"), (76, "Adagio"), (108, "Andante"), (120, "Moderato"),
                                          (156, "Allegro"), (176, "Vivace"), (200, "Presto"), (999, "Prestissimo")]

    private func persist() {
        Store.save("metro", ["bpm": bpm, "beats": beats, "sub": sub, "accent": accent, "vol": Int(vol)])
        if playing { ToolAudio.shared.startMetronome(bpm: bpm, beats: beats, sub: sub, accent: accent, vol: Int(vol)) }
    }

    private func setBpm(_ v: Int) { bpm = min(250, max(30, v)); persist() }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                TimelineView(.animation(minimumInterval: 1.0 / 30, paused: !playing)) { _ in
                    let lit = playing ? ToolAudio.shared.currentBeat(bpm: bpm) : -1
                    HStack(spacing: 10) {
                        ForEach(0..<beats, id: \.self) { i in
                            Circle().fill(lit == i ? Theme.accent : i == 0 && accent ? Theme.accent.opacity(0.35) : Theme.surface2)
                                .frame(width: beats > 8 ? 18 : 24, height: beats > 8 ? 18 : 24)
                        }
                    }
                }
                Text(verbatim: "\(bpm)").font(.system(size: 72, weight: .light)).monospacedDigit()
                Text(verbatim: names.first { bpm < $0.0 }?.1 ?? "").foregroundStyle(Theme.muted)
                HStack(spacing: 10) {
                    ForEach([-5, -1, 1, 5], id: \.self) { d in
                        Button { setBpm(bpm + d) } label: {
                            Text(verbatim: d < 0 ? "−\(-d)" : "+\(d)").font(.headline).frame(width: 56, height: 56)
                                .background(Theme.surface2, in: Circle()).foregroundStyle(Theme.text)
                        }
                        .buttonStyle(.plain)
                    }
                }
                Slider(value: Binding(get: { Double(bpm) }, set: { setBpm(Int($0)) }), in: 30...250).tint(Theme.accent)
                RoundButton(title: L("metronome.tap")) {
                    let now = Date()
                    if let l = taps.last, now.timeIntervalSince(l) > 2 { taps = [] }
                    taps.append(now)
                    if taps.count > 6 { taps.removeFirst() }
                    if taps.count >= 2, let f = taps.first, let l = taps.last {
                        setBpm(Int((60 / (l.timeIntervalSince(f) / Double(taps.count - 1))).rounded()))
                    }
                }
                VStack(alignment: .leading, spacing: 10) {
                    Text(L("metronome.beats")).font(.footnote).foregroundStyle(Theme.muted)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) { ForEach(1...12, id: \.self) { n in Pill(title: "\(n)", on: beats == n) { beats = n; persist() } } }
                    }
                    Text(L("metronome.sub")).font(.footnote).foregroundStyle(Theme.muted)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) { ForEach(1...4, id: \.self) { k in Pill(title: L("metronome.subs.\(k)"), on: sub == k) { sub = k; persist() } } }
                    }
                    Toggle(L("metronome.accent"), isOn: Binding(get: { accent }, set: { accent = $0; persist() })).tint(Theme.accent)
                    Text(L("metronome.volume")).font(.footnote).foregroundStyle(Theme.muted)
                    Slider(value: $vol, in: 0...100, onEditingChanged: { editing in if !editing { persist() } }).tint(Theme.accent)
                }
                .card()
                Note(L("metronome.tempoNote"))
            }
            .padding(20)
        }
        .safeAreaInset(edge: .bottom) {
            ToolPrimaryBar(primary: L(playing ? "metronome.stop" : "metronome.start")) {
                playing.toggle()
                if playing { ToolAudio.shared.startMetronome(bpm: bpm, beats: beats, sub: sub, accent: accent, vol: Int(vol)) } else { ToolAudio.shared.stopMetronome() }
                UIApplication.shared.isIdleTimerDisabled = playing
            }
        }
        .background(AppBackdrop().ignoresSafeArea())
        .onAppear {
            let o = Store.load("metro")
            bpm = o["bpm"] as? Int ?? 100; beats = o["beats"] as? Int ?? 4; sub = o["sub"] as? Int ?? 1
            accent = o["accent"] as? Bool ?? true; vol = Double(o["vol"] as? Int ?? 70)
        }
        .onDisappear { ToolAudio.shared.stopMetronome(); playing = false; UIApplication.shared.isIdleTimerDisabled = false }
        .navigationTitle(L("metronome.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Konuşan saat

struct TalkingClockView: View {
    @AppStorage("talking.every") private var every = "off"
    @AppStorage("talking.chime") private var chime = true
    @State private var synth = AVSpeechSynthesizer()
    @State private var lastKey = ""

    private func sayNow() {
        let c = Calendar.current.dateComponents([.hour, .minute], from: Date())
        let text = T("alarm.speakText", TimeCalc.clock((c.hour ?? 0) * 60 + (c.minute ?? 0)))
        let u = AVSpeechUtterance(string: text)
        u.voice = NotificationSounds.bestVoice
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        if chime {
            ToolAudio.shared.chime()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) { synth.speak(u) }
        } else { synth.speak(u) }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    VStack(spacing: 6) {
                        Text(verbatim: Fmt.time(ctx.date, seconds: true)).font(.system(size: 64, weight: .light)).monospacedDigit()
                        Text(Fmt.longDate(ctx.date)).foregroundStyle(Theme.muted)
                    }
                    .onChange(of: Calendar.current.component(.minute, from: ctx.date)) { _, minute in
                        guard every != "off", let n = Int(every) else { return }
                        let h = Calendar.current.component(.hour, from: ctx.date)
                        let key = "\(h):\(minute)"
                        guard key != lastKey, n == 60 ? minute == 0 : minute % n == 0 else { return }
                        lastKey = key
                        sayNow()
                    }
                }
                .padding(.top, 20)
                VStack(alignment: .leading, spacing: 10) {
                    Text(L("talking.auto")).font(.footnote).foregroundStyle(Theme.muted)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(["off", "1", "5", "15", "30", "60"], id: \.self) { k in Pill(title: L("talking.every.\(k)"), on: every == k) { every = k } }
                        }
                    }
                    Toggle(L("talking.chime"), isOn: $chime).tint(Theme.accent)
                }
                .card()
                Note(L("talking.appNote"))
            }
            .padding(20)
        }
        .safeAreaInset(edge: .bottom) { ToolPrimaryBar(primary: L("talking.say")) { sayNow() } }
        .background(AppBackdrop().ignoresSafeArea())
        .onAppear { UIApplication.shared.isIdleTimerDisabled = every != "off" }
        .onChange(of: every) { _, v in UIApplication.shared.isIdleTimerDisabled = v != "off" }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false; synth.stopSpeaking(at: .immediate) }
        .navigationTitle(L("talking.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Burç

struct ZodiacView: View {
    @State private var date = Calendar.current.date(from: DateComponents(year: 1990, month: 1, day: 1)) ?? Date()
    @State private var picked = false
    @State private var useTime = false
    @State private var time = Calendar.current.date(from: DateComponents(year: 2000, month: 1, day: 1, hour: 12)) ?? Date()

    private func ms(_ day: Date, _ h: Int, _ m: Int) -> Double {
        var c = Calendar.current.dateComponents([.year, .month, .day], from: day)
        c.hour = h; c.minute = m
        return (Calendar.current.date(from: c) ?? day).timeIntervalSince1970 * 1000
    }

    private let starts = [(3, 21), (4, 20), (5, 21), (6, 21), (7, 23), (8, 23), (9, 23), (10, 23), (11, 22), (12, 22), (1, 20), (2, 19)]

    var body: some View {
        List {
            Section {
                DatePicker(L("zodiac.dateLabel"), selection: Binding(get: { date }, set: { date = $0; picked = true }), displayedComponents: .date)
                Toggle(L("zodiac.timeLabel"), isOn: $useTime).tint(Theme.accent)
                if useTime { DatePicker(L("zodiac.timeLabel"), selection: $time, displayedComponents: .hourAndMinute) }
            }
            if picked {
                Section {
                    let tc = Calendar.current.dateComponents([.hour, .minute], from: time)
                    let at = ms(date, useTime ? tc.hour ?? 12 : 12, useTime ? tc.minute ?? 0 : 0)
                    LabeledContent(L("zodiac.sun"), value: Zodiac.label(Zodiac.sign(Zodiac.sunLon(at)))).font(.headline)
                    LabeledContent(L("zodiac.moon"), value: Zodiac.label(Zodiac.sign(Zodiac.moonLon(at)))).font(.headline)
                    if Zodiac.sign(Zodiac.sunLon(ms(date, 0, 0))) != Zodiac.sign(Zodiac.sunLon(ms(date, 23, 59))) { Note(L("zodiac.cusp")) }
                    if !useTime { Note(L("zodiac.noTime")) }
                    Note(L("zodiac.tzNote"))
                }
            }
            Section(L("zodiac.tableHeading")) {
                ForEach(0..<12, id: \.self) { i in
                    let s = starts[i], e = starts[(i + 1) % 12]
                    let from = Calendar.current.date(from: DateComponents(year: 2025, month: s.0, day: s.1)) ?? Date()
                    let to = Calendar.current.date(from: DateComponents(year: 2025, month: e.0, day: e.1 - 1)) ?? Date()
                    LabeledContent(Zodiac.label(i), value: "\(Fmt.dayMonth(from)) – \(Fmt.dayMonth(to))")
                }
            }
            Section { Note(L("zodiac.fun")) }
        }
        .scrollContentBackground(.hidden)
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("zodiac.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Saat hesaplama

struct TimeCalcView: View {
    @State private var s = 9 * 60
    @State private var e = 17 * 60 + 30
    @State private var t = 8 * 60
    @State private var sign = 1
    @State private var h = 2
    @State private var m = 30
    @State private var sum = "1:30\n45\n2:15:30"

    private func timeBinding(_ v: Binding<Int>) -> Binding<Date> {
        Binding(get: { Calendar.current.date(from: DateComponents(year: 2000, month: 1, day: 1, hour: v.wrappedValue / 60, minute: v.wrappedValue % 60)) ?? Date() },
                set: { let c = Calendar.current.dateComponents([.hour, .minute], from: $0); v.wrappedValue = (c.hour ?? 0) * 60 + (c.minute ?? 0) })
    }

    var body: some View {
        List {
            Section(L("timeCalc.betweenHeading")) {
                DatePicker(L("timeCalc.startLabel"), selection: timeBinding($s), displayedComponents: .hourAndMinute)
                DatePicker(L("timeCalc.endLabel"), selection: timeBinding($e), displayedComponents: .hourAndMinute)
                let bt = TimeCalc.between(s, e)
                Text(T("timeCalc.duration", bt.0 / 60, bt.0 % 60) + " · " + T("timeCalc.decimal", TimeCalc.decimal(Double(bt.0) / 60))).font(.headline)
                Note(bt.1 ? L("timeCalc.overnight") : T("timeCalc.minutesTotal", bt.0))
            }
            Section(L("timeCalc.addHeading")) {
                DatePicker(L("timeCalc.timeLabel"), selection: timeBinding($t), displayedComponents: .hourAndMinute)
                Picker("", selection: $sign) {
                    Text(L("timeCalc.opAdd")).tag(1)
                    Text(L("timeCalc.opSub")).tag(-1)
                }
                .pickerStyle(.segmented)
                Stepper("\(L("timeCalc.hoursLabel")): \(h)", value: $h, in: 0...999)
                Stepper("\(L("timeCalc.minutesLabel")): \(m)", value: $m, in: 0...999, step: 5)
                let tot = t + (h * 60 + m) * sign
                let days = Int(floor(Double(tot) / 1440))
                let extra = days == 1 ? L("timeCalc.nextDay") : days == -1 ? L("timeCalc.prevDay") : days > 1 ? T("timeCalc.daysLater", days) : days < -1 ? T("timeCalc.daysEarlier", -days) : ""
                Text(T("timeCalc.resultTime", TimeCalc.clock(tot)) + (extra.isEmpty ? "" : " (\(extra))")).font(.headline)
            }
            Section(L("timeCalc.sumHeading")) {
                TextEditor(text: $sum).frame(minHeight: 120).font(.body.monospacedDigit())
                Note(L("timeCalc.sumHint"))
                let sr = TimeCalc.sum(sum)
                let total = sr.0, bad = sr.1
                let a = abs(total), neg = total < 0 ? "−" : ""
                let txt = neg + "\(a / 3600):" + String(format: "%02d", (a % 3600) / 60) + (a % 60 != 0 ? String(format: ":%02d", a % 60) : "")
                Text(T("timeCalc.sumResult", txt) + " · " + T("timeCalc.decimal", neg + TimeCalc.decimal(Double(a) / 3600))).font(.headline)
                if !bad.isEmpty { Note(T("timeCalc.sumBad", bad.map(String.init).joined(separator: ", "))) }
            }
        }
        .scrollContentBackground(.hidden)
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("timeCalc.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Köprü tatil

struct BridgeView: View {
    @State private var year = Calendar.current.component(.year, from: Date())
    @State private var maxLeave = 9.0

    private func num(_ x: Double) -> String { x == x.rounded() ? String(Int(x)) : TimeCalc.decimal(x) }

    var body: some View {
        let years = Bridges.years.keys.sorted()
        let y = years.contains(year) ? year : years.first ?? year
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(L("bridge.yearLabel")).font(.footnote).foregroundStyle(Theme.muted)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) { ForEach(years, id: \.self) { v in Pill(title: String(v), on: v == y) { year = v } } }
                }
                Text(L("bridge.maxLabel")).font(.footnote).foregroundStyle(Theme.muted)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        Pill(title: L("bridge.maxAll"), on: maxLeave == 9) { maxLeave = 9 }
                        ForEach(1...4, id: \.self) { n in Pill(title: T("bridge.maxN", n), on: maxLeave == Double(n)) { maxLeave = Double(n) } }
                    }
                }
                if Bridges.years[y]?.projected == true { Note(L("bridge.projected")) }
                ForEach(Bridges.years[y]?.list.filter { $0.leave <= maxLeave } ?? [], id: \.self) { b in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(b.g.map(Bridges.groupName).joined(separator: " + ")).font(.headline)
                            Spacer()
                            if b.best {
                                Text(L("bridge.best")).font(.caption.bold()).padding(.horizontal, 8).padding(.vertical, 2)
                                    .background(Theme.accent, in: Capsule()).foregroundStyle(.white)
                            }
                        }
                        Text("\(Bridges.date(b.from)) – \(Bridges.date(b.to))").font(.footnote).foregroundStyle(Theme.muted)
                        HStack {
                            Text(T("bridge.totalN", b.days)).font(.title3.bold())
                            Spacer()
                            Text(b.leave > 0 ? T("bridge.leaveN", num(b.leave)) : L("bridge.noLeave")).font(.headline).foregroundStyle(Theme.accent)
                        }
                        let days = (b.leaveDays.map { ($0, Bridges.date($0)) } + b.halfDays.map { ($0, T("bridge.halfNote", Bridges.date($0))) })
                            .sorted { $0.0 < $1.0 }.map { $0.1 }
                        Text(days.isEmpty ? L("bridge.none") : days.joined(separator: ", ")).font(.footnote).foregroundStyle(Theme.muted)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(b.best ? Theme.accentSoft : Theme.surface, in: RoundedRectangle(cornerRadius: 14))
                }
                Note(L("bridge.note"))
            }
            .padding(16)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("bridge.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Emeklilik (yalnız Türkçe)

struct RetireView: View {
    @State private var female = true
    @State private var birth = Calendar.current.date(from: DateComponents(year: 1980, month: 1, day: 1)) ?? Date()
    @State private var start = Calendar.current.date(from: DateComponents(year: 2005, month: 1, day: 1)) ?? Date()
    @State private var days = ""
    @State private var rate = 360
    @State private var result: Retire.Result?
    @State private var birthUTC = Date()

    /// Seçilen yerel günü UTC gece yarısına çevirir
    private func utc(_ d: Date) -> Date {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: d)
        return Retire.day(c.year!, c.month!, c.day!)
    }

    private var today: Date {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "Europe/Istanbul")!
        let x = c.dateComponents([.year, .month, .day], from: Date())
        return Retire.day(x.year!, x.month!, x.day!)
    }

    private func label(_ c: Retire.Cond) -> String {
        switch c.kind {
        case "days":
            let f = NumberFormatter(); f.locale = Locale(identifier: "tr_TR"); f.numberStyle = .decimal
            return T("retire.condDays", f.string(from: NSNumber(value: c.n)) ?? String(c.n))
        case "years": return T("retire.condYears", c.n)
        case "age": return T("retire.condAge", c.n)
        default: return L("retire.condNoAge")
        }
    }

    var body: some View {
        List {
            Section {
                Picker(L("retire.gender"), selection: $female) {
                    Text(L("retire.women")).tag(true)
                    Text(L("retire.men")).tag(false)
                }
                .pickerStyle(.segmented)
                DatePicker(L("retire.birth"), selection: $birth, displayedComponents: .date)
                DatePicker(L("retire.start"), selection: $start, displayedComponents: .date)
                TextField(L("retire.days"), text: $days).keyboardType(.numberPad)
                Note(L("retire.daysHint"))
                Picker(L("retire.rate"), selection: $rate) {
                    Text(L("retire.rateFull")).tag(360)
                    Text(L("retire.rateHalf")).tag(180)
                    Text(L("retire.rateNone")).tag(0)
                }
            }
            if let r = result {
                Section {
                    if let best = r.best, let at = best.at {
                        Text(T("retire.result", Retire.show(at))).font(.headline)
                        Text(T("retire.resultAge", Retire.ageAt(birthUTC, at)) + " " +
                             (at <= today ? L("retire.resultPast") : T("retire.resultLeft", Retire.diffText(today, at))))
                    } else {
                        Text(L("retire.resultNever")).font(.headline)
                    }
                    Note(T("retire.group", L("retire.groups.\(r.group)")))
                }
                ForEach(r.routes, id: \.key) { rt in
                    Section(L("retire.routes.\(rt.key)")) {
                        ForEach(rt.conds.indices, id: \.self) { i in
                            let c = rt.conds[i]
                            LabeledContent(label(c), value: c.at.map { $0 <= today ? L("retire.done") : Retire.show($0) } ?? L("retire.never"))
                        }
                        LabeledContent("=", value: rt.at.map { Retire.show(max($0, today)) } ?? L("retire.never")).font(.headline)
                    }
                }
            }
            Section {
                Note(L("retire.noteScope"))
                Note(L("retire.noteLaw"))
                Note(L("tools.localOnly"))
            }
        }
        .scrollContentBackground(.hidden)
        .background(AppBackdrop().ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            ToolPrimaryBar(primary: L("retire.calc")) {
                let b = utc(birth), s = utc(start)
                guard let d = Int(days), d >= 0, s >= b else { return }
                birthUTC = b
                result = Retire.calc(female: female, birth: b, start: s, days: d, rate: rate, today: today)
            }
        }
        .onChange(of: female) { _, _ in result = nil }
        .navigationTitle(L("retire.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Sınav zamanlayıcısı

struct ExamTimerView: View {
    @State private var name = ""
    @State private var room = ""
    @State private var read = 10
    @State private var dur = 40
    @State private var start = 0.0 // Unix ms
    @State private var pauseAt = 0.0
    @State private var lastPhase = ""

    private func save() {
        Store.save("exam", ["name": name, "room": room, "read": read, "dur": dur, "start": Int64(start), "pauseAt": Int64(pauseAt)])
    }

    /// Evre (okuma, sınav, bitti), kalan ms, ilerleme ve uyarı (15/5 dk)
    private static func state(start: Double, pauseAt: Double, now: Double, r: Double, d: Double) -> (phase: String, rem: Double, frac: Double, warn: Int) {
        if start == 0 { return ("", r + d, 0, 0) }
        let e = (pauseAt > 0 ? pauseAt : now) - start
        if e < r { return ("read", r - e, e / r, 0) }
        if e < r + d {
            let rem = r + d - e
            return ("exam", rem, (e - r) / d, rem <= 5 * 60_000 ? 5 : rem <= 15 * 60_000 ? 15 : 0)
        }
        return ("done", 0, 1, 0)
    }

    private static func mmss(_ ms: Double) -> String {
        let s = max(0, Int(ceil(ms / 1000))), h = s / 3600, m = (s % 3600) / 60
        return (h > 0 ? String(format: "%d:%02d", h, m) : String(format: "%02d", m)) + String(format: ":%02d", s % 60)
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { ctx in
            let now = ctx.date.timeIntervalSince1970 * 1000
            let r = Double(min(60, max(0, read))) * 60_000, d = Double(min(600, max(1, dur))) * 60_000
            let st = Self.state(start: start, pauseAt: pauseAt, now: now, r: r, d: d)
            let ph = st.phase, rem = st.rem, frac = st.frac, warn = st.warn
            let color: Color = ph == "done" ? Color(red: 0.79, green: 0.16, blue: 0.16) : warn == 5 ? Color.orange : warn == 15 ? Color.yellow : Theme.text
            let phaseText: String = ph == "read" ? L("examTimer.phaseRead") : ph == "exam" ? L("examTimer.phaseExam") : ph == "done" ? L("examTimer.phaseDone")
                : r > 0 ? L("examTimer.phaseRead") : L("examTimer.phaseExam")
            ScrollView {
                VStack(spacing: 14) {
                    if !name.isEmpty { Text(name).font(.title2.weight(.semibold)).multilineTextAlignment(.center) }
                    if !room.isEmpty { Text(room).foregroundStyle(Theme.muted) }
                    Text(phaseText).foregroundStyle(color)
                    Text(verbatim: Self.mmss(start > 0 ? rem : d)).font(.system(size: 76, weight: .light)).monospacedDigit()
                        .foregroundStyle(color)
                    ProgressView(value: min(1, max(0, frac))).tint(color)
                    if warn > 0 { Text(L("examTimer.warn\(warn)")).font(.headline).foregroundStyle(color) }
                    if start > 0 && pauseAt == 0 && ph != "done" {
                        let end = Date(timeIntervalSince1970: (start + r + d) / 1000)
                        let c = Calendar.current.dateComponents([.hour, .minute], from: end)
                        Text(T("examTimer.endsAt", TimeCalc.clock((c.hour ?? 0) * 60 + (c.minute ?? 0)))).foregroundStyle(Theme.muted)
                    }
                    if start == 0 {
                        VStack(spacing: 10) {
                            TextField(L("examTimer.namePh"), text: $name).textFieldStyle(.roundedBorder)
                            TextField(L("examTimer.roomPh"), text: $room).textFieldStyle(.roundedBorder)
                            Stepper("\(L("examTimer.readLabel")): \(read)", value: $read, in: 0...60)
                            Stepper("\(L("examTimer.durLabel")): \(dur)", value: $dur, in: 1...600, step: 5)
                        }
                        .card()
                    }
                    Note(L("examTimer.appNote"))
                }
                .padding(20)
            }
            .onChange(of: ph) { old, new in
                if start > 0 && !old.isEmpty && !new.isEmpty && old != new { ToolAudio.shared.chime() }
                UIApplication.shared.isIdleTimerDisabled = start > 0 && pauseAt == 0 && new != "done"
            }
            .safeAreaInset(edge: .bottom) {
                let primary = start == 0 ? L("examTimer.start") : ph == "done" ? L("examTimer.reset") : pauseAt > 0 ? L("examTimer.resume") : L("examTimer.pause")
                ToolPrimaryBar(primary: primary, secondary: start > 0 && ph != "done" ? L("examTimer.reset") : nil,
                               onSecondary: { start = 0; pauseAt = 0; save(); UIApplication.shared.isIdleTimerDisabled = false }) {
                    let t = Date().timeIntervalSince1970 * 1000
                    if start == 0 { start = t; pauseAt = 0 }
                    else if ph == "done" { start = 0; pauseAt = 0 }
                    else if pauseAt > 0 { start += t - pauseAt; pauseAt = 0 }
                    else { pauseAt = t }
                    save()
                    UIApplication.shared.isIdleTimerDisabled = start > 0 && pauseAt == 0
                }
            }
        }
        .background(AppBackdrop().ignoresSafeArea())
        .onAppear {
            let o = Store.load("exam")
            name = o["name"] as? String ?? ""; room = o["room"] as? String ?? ""
            read = o["read"] as? Int ?? 10; dur = o["dur"] as? Int ?? 40
            start = (o["start"] as? NSNumber)?.doubleValue ?? 0; pauseAt = (o["pauseAt"] as? NSNumber)?.doubleValue ?? 0
        }
        .onChange(of: name) { _, _ in save() }
        .onChange(of: room) { _, _ in save() }
        .onChange(of: read) { _, _ in save() }
        .onChange(of: dur) { _, _ in save() }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .navigationTitle(L("examTimer.h1"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Sınıf araçları

struct ClassroomView: View {
    @State private var tab = "picker"
    @State private var names = ""
    @State private var remove = true
    @State private var pool: [String]?
    @State private var picked = ""
    @State private var teams = 2
    @State private var teamList: [[String]] = []
    @State private var counters: [(String, Int)] = []
    @State private var cname = ""
    @State private var empty = false
    @State private var loaded = false

    private func save() {
        var o: [String: Any] = ["tab": tab, "names": names, "remove": remove, "picked": picked, "teams": teams, "teamList": teamList,
                                "counters": counters.map { ["n": $0.0, "v": $0.1] }]
        if let pool { o["pool"] = pool }
        Store.save(Classroom.key, o)
    }

    var body: some View {
        let all = Classroom.names(names)
        let left = pool?.filter { all.contains($0) } ?? all
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Picker("", selection: $tab) {
                    Text(L("classroom.tabPicker")).tag("picker")
                    Text(L("classroom.tabTeams")).tag("teams")
                    Text(L("classroom.tabCounter")).tag("counter")
                }
                .pickerStyle(.segmented)
                if tab != "counter" {
                    Text(L("classroom.namesLabel")).font(.footnote).foregroundStyle(Theme.muted)
                    TextEditor(text: $names).frame(minHeight: 120).padding(6)
                        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 12))
                        .scrollContentBackground(.hidden)
                    if empty { Text(L("classroom.empty")).foregroundStyle(Theme.err) }
                }
                switch tab {
                case "picker":
                    Toggle(L("classroom.removePicked"), isOn: $remove).tint(Theme.accent)
                    VStack(spacing: 6) {
                        Text(L("classroom.picked")).font(.footnote).foregroundStyle(Theme.muted)
                        Text(picked.isEmpty ? "–" : picked).font(.system(size: 34, weight: .bold, design: .rounded)).multilineTextAlignment(.center)
                        if remove && !all.isEmpty { Text(T("classroom.left", left.count)).font(.caption).foregroundStyle(Theme.muted) }
                    }
                    .frame(maxWidth: .infinity)
                    .card()
                    RoundButton(title: L("classroom.pick"), prominent: true) {
                        guard !all.isEmpty else { empty = true; return }
                        var list = remove ? left : all
                        if list.isEmpty { list = all }
                        guard let choice = list.randomElement() else { return }
                        if remove { pool = list.filter { $0 != choice } }
                        picked = choice
                        save()
                        ToolAudio.shared.beep(988, ms: 150, vol: 0.2)
                    }
                    RoundButton(title: L("classroom.restore")) { pool = nil; picked = ""; save() }
                case "teams":
                    Stepper("\(L("classroom.teamCount")): \(teams)", value: $teams, in: 2...12)
                    RoundButton(title: L("classroom.makeTeams"), prominent: true) {
                        guard !all.isEmpty else { empty = true; return }
                        let k = max(2, min(teams, all.count))
                        let sh = all.shuffled()
                        teamList = (0..<k).map { t in sh.enumerated().filter { $0.offset % k == t }.map(\.element) }
                        save()
                    }
                    ForEach(teamList.indices, id: \.self) { i in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(T("classroom.team", i + 1)).font(.headline).foregroundStyle(Theme.accent)
                            ForEach(teamList[i].indices, id: \.self) { j in Text("\(j + 1). \(teamList[i][j])") }
                        }
                        .card()
                    }
                default:
                    HStack {
                        TextField(L("classroom.counterName"), text: $cname).textFieldStyle(.roundedBorder)
                        Button(L("classroom.addCounter")) {
                            guard counters.count < 24 else { return }
                            let n = cname.trimmingCharacters(in: .whitespaces)
                            counters.append((n.isEmpty ? "\(L("classroom.counterDefault")) \(counters.count + 1)" : String(n.prefix(30)), 0))
                            cname = ""
                            save()
                        }
                        .buttonStyle(.borderedProminent).tint(Theme.accent)
                    }
                    ForEach(counters.indices, id: \.self) { i in
                        HStack(spacing: 10) {
                            Text(counters[i].0).font(.headline)
                            Spacer()
                            Button { counters[i].1 -= 1; save() } label: { Image(systemName: "minus.circle.fill").font(.title) }
                            Text(verbatim: "\(counters[i].1)").font(.title2.bold()).monospacedDigit().frame(minWidth: 48)
                            Button { counters[i].1 += 1; save() } label: { Image(systemName: "plus.circle.fill").font(.title) }
                            Button { counters.remove(at: i); save() } label: { Image(systemName: "xmark").foregroundStyle(Theme.muted) }
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(Theme.accent)
                        .card()
                    }
                    if !counters.isEmpty {
                        RoundButton(title: L("classroom.reset")) { counters = counters.map { ($0.0, 0) }; save() }
                    }
                }
                Note(L("classroom.appNote"))
            }
            .padding(16)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .onAppear {
            guard !loaded else { return }
            loaded = true
            let o = Store.load(Classroom.key)
            tab = o["tab"] as? String ?? "picker"; names = o["names"] as? String ?? ""; remove = o["remove"] as? Bool ?? true
            pool = o["pool"] as? [String]; picked = o["picked"] as? String ?? ""; teams = o["teams"] as? Int ?? 2
            teamList = o["teamList"] as? [[String]] ?? []
            counters = (o["counters"] as? [[String: Any]] ?? []).map { ($0["n"] as? String ?? "", $0["v"] as? Int ?? 0) }
        }
        .onChange(of: names) { _, _ in if loaded { pool = nil; empty = false; save() } }
        .onChange(of: remove) { _, _ in if loaded { pool = nil; save() } }
        .onChange(of: tab) { _, _ in if loaded { empty = false; save() } }
        .onChange(of: teams) { _, _ in if loaded { save() } }
        .navigationTitle(L("classroom.h1"))
        .navigationBarTitleDisplayMode(.large)
    }
}
