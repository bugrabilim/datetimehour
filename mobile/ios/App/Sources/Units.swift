import Foundation
import WeatherKit

/// Ölçü birimleri (Ayarlar › Birimler, `units` anahtarı): metrik (°C, km/sa, hPa) ya da Amerikan (°F, mph, inHg).
/// Seçim yoksa cihazın bölgesine göre.
enum Units {
    static var imperial: Bool { (UserDefaults.standard.string(forKey: "units") ?? systemDefault) == "imperial" }
    static var systemDefault: String { Locale.current.measurementSystem == .us ? "imperial" : "metric" }

    static func temp(_ t: Measurement<UnitTemperature>) -> String {
        let v = t.converted(to: imperial ? .fahrenheit : .celsius).value
        return "\(Int(v.rounded()))°"
    }

    static func wind(_ s: Measurement<UnitSpeed>) -> String {
        if imperial { return "\(Int(s.converted(to: .milesPerHour).value.rounded())) mph" }
        return T("weather.km", Int(s.converted(to: .kilometersPerHour).value.rounded()))
    }

    static func pressure(_ p: Measurement<UnitPressure>) -> String {
        if imperial { return String(format: "%.2f inHg", p.converted(to: .inchesOfMercury).value) }
        return "\(Int(p.converted(to: .hectopascals).value.rounded())) hPa"
    }
}
