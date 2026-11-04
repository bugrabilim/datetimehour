import SwiftUI
import UIKit

/// Gerçekçi 3D simgeler (4 Kasım 2026, sahibin seçimi: "çizgi simge istemiyorum, bulut varsa gerçekçi bir bulut olsun").
/// Görseller Microsoft Fluent Emoji 3D (MIT): mobile/shared/icons3d/gen-icons3d.py üretir, Icons3D.xcassets
/// (`e3d-<ad>`), uygulama ve widget hedefinde. Adlar çizgi simgelerle aynı (icons.json); hava için ek adlar
/// sun_small_cloud, sun_cloud, sun_rain. Android'deki Icons3D (LineIcons.kt) ile aynı adlar.
enum Icons3D {
    private static var cache: [String: Bool] = [:]

    /// Bu adla 3D görsel var mı
    static func has(_ name: String) -> Bool {
        if let v = cache[name] { return v }
        let v = UIImage(named: "e3d-\(name)") != nil
        cache[name] = v
        return v
    }

    /// WeatherKit'in SF Symbol adından 3D hava simgesi (Android'deki WeatherCode.line ile aynı aile)
    static func weather(symbol s: String) -> String {
        if s.contains("bolt") { return "storm" }
        if s.contains("snow") || s.contains("sleet") || s.contains("hail") { return "snow" }
        if s.contains("rain") || s.contains("drizzle") { return s.contains("sun") ? "sun_rain" : "rain" }
        if s.contains("fog") || s.contains("haze") || s.contains("smoke") || s.contains("dust") { return "fog" }
        if s.contains("wind") || s.contains("tornado") || s.contains("hurricane") || s.contains("tropicalstorm") { return "wind" }
        if s.hasPrefix("cloud.sun") { return "sun_cloud" }
        if s.hasPrefix("cloud") { return "cloud" }
        if s.hasPrefix("moon") { return "moon" }
        if s.hasPrefix("sun") { return "sun" }
        if s.hasPrefix("thermometer") { return "thermometer" }
        return "sun_cloud"
    }
}

/// 3D simge görseli; `dim`: gri ve soluk (seçili olmayan sekme, bitmemiş pomodoro turu)
struct Icon3D: View {
    let name: String
    var size: CGFloat = 28
    var dim = false

    var body: some View {
        Image("e3d-\(name)")
            .resizable()
            .interpolation(.high)
            .scaledToFit()
            .frame(width: size, height: size)
            .saturation(dim ? 0 : 1)
            .opacity(dim ? 0.45 : 1)
            .accessibilityHidden(true)
    }
}

/// Hava simgesi: WeatherKit sembol adından 3D görsel (bulut, güneş, yağmur…)
struct WeatherIcon3D: View {
    let symbol: String
    var size: CGFloat = 28

    var body: some View {
        Icon3D(name: Icons3D.weather(symbol: symbol), size: size)
    }
}
