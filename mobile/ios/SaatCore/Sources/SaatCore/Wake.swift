import Foundation

/// Alarm kapatma görevleri (30 Ekim 2026, rakip analizi #13): matematik, sallama, adım, kod (QR/barkod) okutma; ve
/// uyanma denetimi. Burada yalnız hesap var; sensör ve kamera uygulamada. Android'deki WakeTask'ın aynısı.
public enum WakeTask {
    public static let kinds = ["none", "math", "shake", "steps", "code"]
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
