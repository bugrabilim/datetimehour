import Foundation

/* Derece tabanlı trigonometri ve sitedeki JavaScript ile aynı yuvarlama.
   İşlem sırası JS'tekiyle aynı tutulur: sonuçlar ortak test verisiyle bit düzeyine yakın karşılaştırılır. */

let deg2rad = Double.pi / 180
let rad2deg = 180 / Double.pi

@inline(__always) func dsin(_ d: Double) -> Double { sin(d * deg2rad) }
@inline(__always) func dcos(_ d: Double) -> Double { cos(d * deg2rad) }
@inline(__always) func dtan(_ d: Double) -> Double { tan(d * deg2rad) }
@inline(__always) func dasin(_ x: Double) -> Double { asin(x) * rad2deg }
@inline(__always) func dacos(_ x: Double) -> Double { acos(x) * rad2deg }
@inline(__always) func dacot(_ x: Double) -> Double { atan(1 / x) * rad2deg }
@inline(__always) func datan2(_ y: Double, _ x: Double) -> Double { atan2(y, x) * rad2deg }

/// 0..360 aralığına indirger (JS: a - 360 * Math.floor(a / 360))
@inline(__always) func fixAngle(_ a: Double) -> Double { a - 360 * floor(a / 360) }
/// 0..24 aralığına indirger
@inline(__always) func fixHour(_ h: Double) -> Double { h - 24 * floor(h / 24) }

/// JavaScript Math.round: en yakın tam sayı, yarımda yukarı (+∞ yönüne)
@inline(__always) func jsRound(_ x: Double) -> Double { floor(x + 0.5) }
