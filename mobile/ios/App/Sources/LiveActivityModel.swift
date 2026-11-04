import ActivityKit
import Foundation

/// Live Activity / Dynamic Island (rakip analizi #2): zamanlayıcı, pomodoro, kronometre ve sıradaki namaz vakti
/// (Ramazan'da iftar ve sahur). Uygulama ve widget hedefinde ortak; görünüm Widget/Sources/LiveActivityWidget.swift,
/// kurulum App/Sources/LiveActivities.swift. Yazılar uygulamada (uygulamanın dilinde) hazırlanıp duruma konur.
struct SaatActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        /// Geri sayımın bittiği an; kronometrede saymaya başladığı an (duraklamalar düşülmüş)
        var date: Date
        /// İlerleme çubuğunun başı (geri sayımda)
        var start: Date
        var title: String
        var subtitle: String
    }

    /// "timer", "pomodoro", "stopwatch", "prayer", "clock", "sun"
    var kind: String
    /// Etkinliğin kimliği (zamanlayıcıda zamanlayıcının kimliği, diğerlerinde tür adı)
    var id: String

    /// Kronometre ve canlı saat ileri sayar (canlı saatte gece yarısından geçen süre = saat; 2. analiz #19)
    var countsUp: Bool { kind == "stopwatch" || kind == "clock" }

    var symbol: String {
        switch kind {
        case "pomodoro": return "brain.head.profile"
        case "stopwatch": return "stopwatch"
        case "prayer": return "moon.stars"
        case "clock": return "clock"
        case "sun": return "sun.horizon"
        default: return "timer"
        }
    }
}
