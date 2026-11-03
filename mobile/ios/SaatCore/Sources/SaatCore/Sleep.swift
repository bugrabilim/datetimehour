import Foundation

/// Uyku planı (30 Ekim 2026, rakip analizi #17): 90 dakikalık uyku döngüleri (sitedeki alarm sayfalarıyla aynı kural:
/// uykuya dalmak için 15 dk + döngüler). Uyku takibi (mikrofon, hareket) bilerek yok. Android'deki Sleep'in aynısı.
public enum Sleep {
    public static let fallAsleep = 15
    public static let cycle = 90
    public static let cycles = [6, 5, 4]

    /// Günün dakikası (0–1439)
    public static func mod(_ m: Int) -> Int { ((m % 1440) + 1440) % 1440 }

    /// Kalkış saatine göre yatma saatleri (6, 5, 4 döngü)
    public static func bedtimes(wake: Int) -> [(cycles: Int, minute: Int)] {
        cycles.map { ($0, mod(wake - fallAsleep - $0 * cycle)) }
    }

    /// Şimdi yatılırsa kalkış saatleri (6, 5, 4 döngü)
    public static func wakeTimes(now: Int) -> [(cycles: Int, minute: Int)] {
        cycles.map { ($0, mod(now + fallAsleep + $0 * cycle)) }
    }

    /// Planın yatma saati: kalkıştan uyku süresi kadar önce
    public static func bedtime(wake: Int, duration: Int) -> Int { mod(wake - duration) }
}

/// Gün doğumu ışığı: alarmdan `window` dakika önce başlar, alarm anında tam aydınlık olur, 15 dk sonra söner.
/// Ekran yalnız tam ekran saat açıkken aydınlanır (telefon uygulama kapalıyken ekranı açamaz).
public enum SunriseLight {
    public static let after: TimeInterval = 15 * 60

    /// 0 (karanlık) – 1 (tam aydınlık); yumuşak başlangıçlı
    public static func level(now: Date, alarmAt: Date, window: Int) -> Double {
        guard window > 0 else { return 0 }
        let start = alarmAt.addingTimeInterval(-Double(window) * 60)
        if now < start || now > alarmAt.addingTimeInterval(after) { return 0 }
        if now >= alarmAt { return 1 }
        let x = now.timeIntervalSince(start) / alarmAt.timeIntervalSince(start)
        return x * x * (3 - 2 * x)
    }

    private static let stops: [(Double, [Double])] = [
        (0, [0, 0, 0]),
        (0.25, [70, 10, 6]),
        (0.55, [196, 72, 22]),
        (0.8, [255, 160, 72]),
        (1, [255, 236, 204]),
    ]

    /// Işığın rengi (koyu kırmızıdan turuncuya, sonra sıcak beyaza); 0–255 RGB
    public static func rgb(_ level: Double) -> (r: Int, g: Int, b: Int) {
        let l = max(0, min(1, level))
        let i = min(stops.lastIndex { $0.0 <= l } ?? 0, stops.count - 2)
        let (a, ca) = stops[i], (b, cb) = stops[i + 1]
        let k = (l - a) / (b - a)
        let c = (0..<3).map { Int((ca[$0] + (cb[$0] - ca[$0]) * k).rounded()) }
        return (c[0], c[1], c[2])
    }

    /// Ekran parlaklığı (0,05 – 1)
    public static func brightness(_ level: Double) -> Double { 0.05 + 0.95 * max(0, min(1, level)) }
}

/// Akıllı alarm (31 Ekim 2026, sahibin kararı: uyku takibinden yalnız akıllı alarm). Telefon yataktayken ivmeölçerin
/// büyüklüğü (g) yavaş izlenen tabana göre `threshold`'tan fazla saparsa o örnek "hareket" sayılır; bir dakikada en az
/// `minSamples` hareketli örnek varsa o dakika hareketlidir. Alarm penceresinde son `span` dakikanın en az `need`'i
/// hareketliyse erken çalınır. Kayıt yok; yalnız bellekte son 10 dakika. Android :core'daki SmartWake ile aynı kural.
public struct SmartWake {
    public static let epochMs: Int64 = 60_000
    public static let afterMs: Int64 = 2 * 60_000
    let threshold: Double
    let minSamples: Int
    let need: Int
    let span: Int
    private var base = Double.nan
    private var counts: [Int64: Int] = [:]
    private var lastCounted = Int64.min
    /// Pencere içinde hareketli bulunan dakika sayısı (ekranda)
    public private(set) var movingMinutes = 0

    public init(threshold: Double = 0.025, minSamples: Int = 3, need: Int = 2, span: Int = 3) {
        self.threshold = threshold
        self.minSamples = minSamples
        self.need = need
        self.span = span
    }

    private static func epoch(_ t: Int64) -> Int64 { t >= 0 ? t / epochMs : (t - epochMs + 1) / epochMs }

    /// x, y, z: g biriminde (yer çekimi dahil)
    public mutating func add(tMs: Int64, x: Double, y: Double, z: Double) {
        let m = (x * x + y * y + z * z).squareRoot()
        if base.isNaN { base = m; return }
        let d = abs(m - base)
        base += 0.05 * (m - base)
        guard d > threshold else { return }
        let e = Self.epoch(tMs)
        let n = (counts[e] ?? 0) + 1
        counts[e] = n
        if n == minSamples && e != lastCounted { movingMinutes += 1; lastCounted = e }
        for k in counts.keys where k < e - 10 { counts.removeValue(forKey: k) }
    }

    /// Son `span` dakikadan (şimdiki dahil) kaçı hareketli
    public func movingEpochs(nowMs: Int64) -> Int {
        let e = Self.epoch(nowMs)
        return (0..<Int64(span)).filter { (counts[e - $0] ?? 0) >= minSamples }.count
    }

    /// Pencerede (alarmdan `windowMin` dk önce – alarm) ve yeterli hareket varsa true
    public func shouldWake(nowMs: Int64, alarmMs: Int64, windowMin: Int) -> Bool {
        windowMin > 0 && nowMs >= alarmMs - Int64(windowMin) * 60_000 && nowMs < alarmMs && movingEpochs(nowMs: nowMs) >= need
    }
}

/// Uyku düzeni (2. analiz #47; uyku takibi olmadan): Android'deki SleepStats ile aynı kayıt ve kurallar
/// (`sleep.log`: [{d, w, b}], en çok 120).
public enum SleepStats {
    public struct Entry: Codable, Equatable, Sendable {
        public var d: String
        public var w: Int
        public var b: Int
        public init(d: String, w: Int, b: Int = -1) { self.d = d; self.w = w; self.b = b }
    }

    public struct Summary: Equatable, Sendable {
        public let weekWake: Int?, prevWake: Int?, spread: Int?, debt: Int?, nights: Int
    }

    public static let max = 120

    public static func record(_ log: [Entry], _ e: Entry) -> [Entry] {
        let old = log.first { $0.d == e.d }
        let merged = old.map { Entry(d: e.d, w: Swift.min($0.w, e.w), b: $0.b >= 0 ? $0.b : e.b) } ?? e
        return Array((log.filter { $0.d != e.d } + [merged]).sorted { $0.d < $1.d }.suffix(max))
    }

    public static func duration(_ e: Entry) -> Int? { e.b < 0 ? nil : ((e.w - e.b) % 1440 + 1440) % 1440 }

    public static func summary(_ log: [Entry], today: String, goal: Int) -> Summary {
        guard let t = CalendarDay(iso: today) else { return Summary(weekWake: nil, prevWake: nil, spread: nil, debt: nil, nights: 0) }
        let w0 = t.adding(days: -7).iso, w1 = t.adding(days: -14).iso
        let week = log.filter { $0.d > w0 && $0.d <= today }
        let prev = log.filter { $0.d > w1 && $0.d <= w0 }
        func avg(_ l: [Entry]) -> Int? { l.isEmpty ? nil : Int((Double(l.map(\.w).reduce(0, +)) / Double(l.count)).rounded()) }
        let recent = log.filter { $0.d > w1 && $0.d <= today }
        var spread: Int? = nil
        if recent.count >= 3 {
            let m = Double(recent.map(\.w).reduce(0, +)) / Double(recent.count)
            spread = Int((recent.map { (Double($0.w) - m) * (Double($0.w) - m) }.reduce(0, +) / Double(recent.count)).squareRoot().rounded())
        }
        let durs = week.compactMap(duration)
        let debt = durs.isEmpty ? nil : durs.map { Swift.max(0, goal - $0) }.reduce(0, +)
        return Summary(weekWake: avg(week), prevWake: avg(prev), spread: spread, debt: debt, nights: week.count)
    }
}

/// Kronotip tahmini (2. analiz #48; Android'deki Chronotype ile aynı): MCTQ'daki düzeltilmiş serbest gün uyku ortası fikri
public enum Chronotype {
    private static func dur(_ bed: Int, _ wake: Int) -> Int { ((wake - bed) % 1440 + 1440) % 1440 }

    public static func msfsc(workBed: Int, workWake: Int, freeBed: Int, freeWake: Int, workDays: Int = 5) -> Int {
        let sdw = dur(workBed, workWake), sdf = dur(freeBed, freeWake)
        let msf = Double(freeBed) + Double(sdf) / 2
        let week = Double(sdw * workDays + sdf * (7 - workDays)) / 7
        let m = sdf > sdw ? msf - (Double(sdf) - week) / 2 : msf
        let r = ((Int(m.rounded()) % 1440) + 1440) % 1440
        return r >= 720 ? r - 1440 : r
    }

    public static func category(_ msfsc: Int) -> Int {
        switch msfsc {
        case ..<150: 0
        case ..<210: 1
        case ..<270: 2
        case ..<330: 3
        default: 4
        }
    }
}
