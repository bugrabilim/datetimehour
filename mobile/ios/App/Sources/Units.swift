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

    /// Basınç birimi (2. analiz #106; Ayarlar, `units.pressure`): auto (birime göre), hPa, mmHg, inHg
    static var pressureUnit: String { UserDefaults.standard.string(forKey: "units.pressure") ?? "auto" }

    static func pressure(_ p: Measurement<UnitPressure>) -> String {
        let u = pressureUnit == "auto" ? (imperial ? "inHg" : "hPa") : pressureUnit
        switch u {
        case "inHg": return String(format: "%.2f inHg", p.converted(to: .inchesOfMercury).value)
        case "mmHg": return "\(Int(p.converted(to: .millimetersOfMercury).value.rounded())) mmHg"
        default: return "\(Int(p.converted(to: .hectopascals).value.rounded())) hPa"
        }
    }

    private static func decimal(_ v: Double) -> String {
        String(format: "%.1f", v).replacingOccurrences(of: ".", with: AppLocale.numberLocale.decimalSeparator ?? ".")
    }

    /// Görüş: 10 km'den uzaksa tam sayı, yakınsa bir ondalık (Amerikan biriminde mil)
    static func distance(_ m: Measurement<UnitLength>) -> String {
        let v = imperial ? m.converted(to: .miles).value : m.converted(to: .kilometers).value
        return (v >= 10 ? "\(Int(v.rounded()))" : decimal(v)) + (imperial ? " mi" : " km")
    }

    /// Yağış miktarı (mm ya da inç)
    static func precip(_ m: Measurement<UnitLength>) -> String {
        if imperial { return String(format: "%.2f", m.converted(to: .inches).value).replacingOccurrences(of: ".", with: AppLocale.numberLocale.decimalSeparator ?? ".") + " in" }
        return decimal(m.converted(to: .millimeters).value) + " mm"
    }

    /// Derece farkı (yarın uyarısı; Amerikan biriminde °F farkı)
    static func delta(_ c: Int) -> Int { imperial ? Int((Double(c) * 1.8).rounded()) : c }
}
