import Foundation

/// Alarm kapatma görevleri (30 Ekim 2026, rakip analizi #13): matematik, sallama, adım, kod (QR/barkod) okutma; ve
/// uyanma denetimi. Burada yalnız hesap var; sensör ve kamera uygulamada. Android'deki WakeTask'ın aynısı.
public enum WakeTask {
    public static let kinds = ["none", "math", "shake", "steps", "code", "memory", "typing", "photo"]
    /// Matematik: üç işlem; sallama 30 kez; adım 20
    public static let mathCount = 3
    public static let shakes = 30
    public static let steps = 20
    /// Uyanma denetimi: kapattıktan sonra sorulur, cevap gelmezse alarm yeniden çalar
    public static let checkAfterMin = 5
    public static let checkGraceMin = 2
    /// Görev çözülmeden kapatılırsa alarm bir dakika sonra yeniden çalar; en çok 10 kez
    public static let repeatMin = 1
    public static let repeatMax = 10

    public struct Problem: Equatable, Sendable {
        public let text: String
        public let answer: Int
    }

    /// Hafıza görevi (rakip analizi listesi #75): 3×3 ızgarada yanıp sönen karelerin sırası; uzunluk 4/5/6 (zorluk)
    public static func memorySequence<R: RandomNumberGenerator>(level: Int, using rng: inout R) -> [Int] {
        let n = 3 + min(3, max(1, level))
        var out: [Int] = []
        while out.count < n { let c = Int.random(in: 0..<9, using: &rng); if out.last != c { out.append(c) } }
        return out
    }

    /// Yazma görevi: büyük/küçük harf, baştaki/sondaki boşluk ve noktalama farkı sayılmaz
    public static func typed(_ target: String, _ input: String) -> Bool {
        func norm(_ s: String) -> String {
            s.lowercased().filter { $0.isLetter || $0.isNumber || $0 == " " }.split(separator: " ").joined(separator: " ")
        }
        return !norm(target).isEmpty && norm(target) == norm(input)
    }

    /// Fotoğraf görevi: 9×8 gri görüntüden 64 bit fark özeti (dHash); kayıtlı yerle aynı sahne mi diye karşılaştırılır
    public static func dHash(_ gray: [Int]) -> UInt64 {
        precondition(gray.count == 72)
        var h: UInt64 = 0
        for y in 0..<8 { for x in 0..<8 { h = (h << 1) | (gray[y * 9 + x] > gray[y * 9 + x + 1] ? 1 : 0) } }
        return h
    }

    /// Aynı sahne sayılan en büyük fark (64 bitten); ışık ve açı değişimine pay bırakır
    public static let photoMaxDistance = 18
    public static func samePhoto(_ a: UInt64, _ b: UInt64) -> Bool { (a ^ b).nonzeroBitCount <= photoMaxDistance }

    /// level: 1 kolay (iki basamaklı toplama), 2 orta (çarpma + toplama), 3 zor (iki basamaklı çarpma + toplama)
    public static func problem<R: RandomNumberGenerator>(level: Int, using rng: inout R) -> Problem {
        switch level {
        case 1:
            let a = Int.random(in: 12..<90, using: &rng), b = Int.random(in: 12..<90, using: &rng)
            return Problem(text: "\(a) + \(b)", answer: a + b)
        case 2:
            let a = Int.random(in: 12..<30, using: &rng), b = Int.random(in: 3..<10, using: &rng), c = Int.random(in: 10..<100, using: &rng)
            return Problem(text: "\(a) × \(b) + \(c)", answer: a * b + c)
        default:
            let a = Int.random(in: 13..<50, using: &rng), b = Int.random(in: 12..<30, using: &rng), c = Int.random(in: 100..<1000, using: &rng)
            return Problem(text: "\(a) × \(b) + \(c)", answer: a * b + c)
        }
    }
}

/// Tohumlu rastgele sayı (testler için)
public struct SplitMix64: RandomNumberGenerator {
    private var state: UInt64
    public init(seed: UInt64) { state = seed }
    public mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}

/// İvmeölçerden sayma (izin istemez): büyüklük (g) `high` eşiğini yukarı kestiğinde bir sayılır, yeniden sayılması için
/// önce `low` altına inmeli ve en az `gap` saniye geçmeli. Sallama: 2 g; adım: 1,18 g.
public struct MotionCounter: Sendable {
    let high: Double, low: Double, gap: TimeInterval
    private var armed = true
    private var lastAt: TimeInterval = -.greatestFiniteMagnitude
    public private(set) var count = 0

    public init(high: Double, low: Double, gap: TimeInterval) {
        self.high = high; self.low = low; self.gap = gap
    }

    public static func shake() -> MotionCounter { MotionCounter(high: 2.0, low: 1.3, gap: 0.15) }
    public static func steps() -> MotionCounter { MotionCounter(high: 1.18, low: 1.04, gap: 0.28) }

    /// x, y, z: g biriminde (yer çekimi dahil); sayıldıysa true
    @discardableResult
    public mutating func feed(t: TimeInterval, x: Double, y: Double, z: Double) -> Bool {
        let m = (x * x + y * y + z * z).squareRoot()
        if m < low { armed = true }
        if armed, m > high, t - lastAt >= gap {
            armed = false
            lastAt = t
            count += 1
            return true
        }
        return false
    }
}
