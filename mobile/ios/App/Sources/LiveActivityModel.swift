import ActivityKit
import Foundation

/// Live Activity / Dynamic Island (rakip analizi #2): zamanlayıcı, pomodoro, kronometre, sıradaki namaz vakti
/// (Ramazan'da iftar ve sahur), gün doğumu/batımı ve hava durumu (4 Kasım 2026; canlı saatin yerine). Uygulama ve
/// widget hedefinde ortak; görünüm Widget/Sources/LiveActivityWidget.swift, kurulum App/Sources/LiveActivities.swift. Yazılar uygulamada (uygulamanın dilinde) hazırlanıp duruma konur.
struct SaatActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        /// Geri sayımın bittiği an; kronometrede saymaya başladığı an (duraklamalar düşülmüş)
        var date: Date
        /// İlerleme çubuğunun başı (geri sayımda)
        var start: Date
        var title: String
        var subtitle: String
        /// Hava durumunda sağdaki değer ("18°") ve WeatherKit simgesinin adı; diğer türlerde boş
        var value: String? = nil
        var symbol: String? = nil
    }

    /// "timer", "pomodoro", "stopwatch", "prayer", "sun", "weather"
    var kind: String
    /// Etkinliğin kimliği (zamanlayıcıda zamanlayıcının kimliği, diğerlerinde tür adı)
    var id: String

    /// Kronometre ileri sayar
    var countsUp: Bool { kind == "stopwatch" }

    /// Süre yerine sabit değer gösterir (hava durumu)
    var isWeather: Bool { kind == "weather" }

    var symbol: String {
        switch kind {
        case "pomodoro": return "brain.head.profile"
        case "stopwatch": return "stopwatch"
        case "prayer": return "moon.stars"
        case "weather": return "cloud.sun.fill"
        case "sun": return "sun.horizon"
        default: return "timer"
        }
    }
}
