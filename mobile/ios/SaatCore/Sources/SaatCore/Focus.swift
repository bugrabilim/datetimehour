import Foundation

/// Pomodoro odak geçmişi (30 Ekim 2026, rakip analizi #16): sitedeki `sth-pomo-log` ile aynı kayıt
/// ({d: yerel gün "YYYY-MM-DD", m: dakika, t: görev}, en çok 1000). Kayıt yalnız telefonda durur.
public enum FocusLog {
    public static let max = 1000

    public struct Entry: Codable, Equatable, Sendable {
        public var d: String
        public var m: Int
        public var t: String
        /// Etiket (rakip analizi #27): work, study, personal, other; yoksa nil
        public var g: String?
        public init(d: String, m: Int, t: String, g: String? = nil) { self.d = d; self.m = m; self.t = t; self.g = g }
    }

    /// Üst üste çalışılan gün sayısı: seri bugün ya da dün biten günlerden geriye sayılır (sitedeki tools.js ile aynı)
    public static func streak(_ log: [Entry], today: CalendarDay) -> Int {
        let have = Set(log.map(\.d))
        var d = have.contains(today.iso) ? today : today.adding(days: -1)
        var n = 0
        while have.contains(d.iso) { n += 1; d = d.adding(days: -1) }
        return n
    }

    /// `since` gününden bu yana etiketlere göre dakika (etiketsizler "none"), çoktan aza; eşitlikte ilk görülen önce
    public static func tags(_ log: [Entry], since: String) -> [(tag: String, minutes: Int)] {
        var order: [String] = []
        var m: [String: Int] = [:]
        for e in log where e.d >= since {
            let k = e.g ?? "none"
            if m[k] == nil { order.append(k) }
            m[k, default: 0] += e.m
        }
        return order.enumerated().sorted { a, b in
            m[a.element]! != m[b.element]! ? m[a.element]! > m[b.element]! : a.offset < b.offset
        }.map { (tag: $0.element, minutes: m[$0.element]!) }
    }

    public struct Sum: Equatable, Sendable {
        public var n: Int
        public var m: Int
    }

    public struct Stats: Equatable, Sendable {
        public var today: Sum
        public var week: Sum
        public var total: Sum
        public var perDay: [Int]
        public var top: [(name: String, minutes: Int)]

        public static func == (a: Stats, b: Stats) -> Bool {
            a.today == b.today && a.week == b.week && a.total == b.total && a.perDay == b.perDay
                && a.top.map(\.name) == b.top.map(\.name) && a.top.map(\.minutes) == b.top.map(\.minutes)
        }
    }

    /// Yeni kaydı ekler, en eski kayıtları atar (en çok 1000); görev adı en çok 60 karakter
    public static func add(_ log: [Entry], _ e: Entry) -> [Entry] {
        var e = e
        e.t = String(e.t.trimmingCharacters(in: .whitespacesAndNewlines).prefix(60))
        return Array((log + [e]).suffix(max))
    }

    /// days: son 7 günün anahtarları, eskiden yeniye (sonuncusu bugün). Adsız görevler `noTask` adıyla toplanır.
    public static func stats(_ log: [Entry], days: [String], noTask: String) -> Stats {
        precondition(days.count == 7)
        func sum(_ f: (Entry) -> Bool) -> Sum {
            let l = log.filter(f)
            return Sum(n: l.count, m: l.reduce(0) { $0 + $1.m })
        }
        let first = days[0]
        var order: [String] = []
        var tasks: [String: Int] = [:]
        for e in log where e.d >= first {
            let k = e.t.trimmingCharacters(in: .whitespaces).isEmpty ? noTask : e.t
            if tasks[k] == nil { order.append(k) }
            tasks[k, default: 0] += e.m
        }
        // Eşit dakikada ilk görülen önce (Kotlin'deki kararlı sıralamayla aynı)
        let top = order.enumerated().sorted { a, b in
            tasks[a.element]! != tasks[b.element]! ? tasks[a.element]! > tasks[b.element]! : a.offset < b.offset
        }.prefix(5).map { (name: $0.element, minutes: tasks[$0.element]!) }
        return Stats(
            today: sum { $0.d == days[6] },
            week: sum { $0.d >= first },
            total: sum { _ in true },
            perDay: days.map { d in log.filter { $0.d == d }.reduce(0) { $0 + $1.m } },
            top: Array(top))
    }
}

/// Arka plan sesi (pomodoro ve uyku sesleri): sitedeki Web Audio gürültüsünün (tools.js noiseBuffer) karşılığı, telefonda
/// üretilir, dosya yok. Beyaz, pembe (Paul Kellet), kahverengi, yağmur (pembe gürültü 900 Hz yüksek geçiren + 7 kHz alçak
/// geçiren), dalga (pembe gürültü, 10 sn'lik yavaş kabarma). Arabellek döngüde çalınır; ucu başına yarım saniyede
/// karıştırıldığı için döngü yerinde tık sesi olmaz. Ses düzeyi türler arasında eşit (RMS 0,2). Android'deki NoiseGen'in aynısı.
public enum NoiseGen {
    public static let kinds = ["white", "pink", "brown", "rain", "waves"]
    public static let rate = 44100
    static let rms = 0.2

    public static func seconds(_ kind: String) -> Int { kind == "waves" ? 10 : 8 }

    public static func samples(_ kind: String, rate: Int = NoiseGen.rate, seed: UInt32 = 0x5A17) -> [Float] {
        let n = seconds(kind) * rate
        let fade = rate / 2
        var raw = [Double](repeating: 0, count: n + fade)
        var s: UInt32 = seed == 0 ? 1 : seed
        var b0 = 0.0, b1 = 0.0, b2 = 0.0, b3 = 0.0, b4 = 0.0, b5 = 0.0, b6 = 0.0, last = 0.0
        for i in raw.indices {
            // xorshift32: tekrarlanabilir
            s ^= s << 13; s ^= s >> 17; s ^= s << 5
            let w = Double(s) / 4294967296.0 * 2 - 1
            switch kind {
            case "white": raw[i] = w * 0.5
            case "brown":
                last = (last + 0.02 * w) / 1.02
                raw[i] = last * 3.5
            default:
                b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.969 * b2 + w * 0.153852
                b3 = 0.8665 * b3 + w * 0.3104856; b4 = 0.55 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.016898
                raw[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11
                b6 = w * 0.115926
            }
        }
        switch kind {
        case "rain":
            biquad(&raw, rate: rate, f: 900, high: true)
            biquad(&raw, rate: rate, f: 7000, high: false)
        case "waves":
            biquad(&raw, rate: rate, f: 1200, high: false)
        default: break
        }
        // Döngü yeri: sondaki yarım saniye baştakiyle karıştırılır
        var out = [Double](repeating: 0, count: n)
        for i in 0..<n {
            if i < fade {
                let k = Double(i) / Double(fade)
                out[i] = raw[i] * k + raw[n + i] * (1 - k)
            } else {
                out[i] = raw[i]
            }
        }
        if kind == "waves" {
            for i in 0..<n {
                let e = 0.5 - 0.5 * cos(2 * Double.pi * Double(i) / Double(n))
                out[i] *= 0.25 + 0.75 * pow(e, 1.5)
            }
        }
        let sq = out.reduce(0) { $0 + $1 * $1 }
        let g = rms / Swift.max(1e-9, (sq / Double(n)).squareRoot())
        return out.map { Float(Swift.max(-1, Swift.min(1, $0 * g))) }
    }

    /// RBJ ikinci derece süzgeç (Q = 0,7071), yerinde
    static func biquad(_ x: inout [Double], rate: Int, f: Double, high: Bool) {
        let w0 = 2 * Double.pi * Swift.min(f, Double(rate) * 0.45) / Double(rate)
        let c = cos(w0)
        let alpha = sin(w0) / (2 * 0.7071)
        let a0 = 1 + alpha
        let b0 = (high ? (1 + c) / 2 : (1 - c) / 2) / a0
        let b1 = (high ? -(1 + c) : 1 - c) / a0
        let b2 = b0
        let a1 = -2 * c / a0
        let a2 = (1 - alpha) / a0
        var x1 = 0.0, x2 = 0.0, y1 = 0.0, y2 = 0.0
        for i in x.indices {
            let y = b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x[i]; y2 = y1; y1 = y
            x[i] = y
        }
    }

    /// Ses düzeyi (0–100) → kazanç
    public static func gain(_ volume: Int) -> Float { Float(Swift.max(0, Swift.min(100, volume))) / 100 }
}
