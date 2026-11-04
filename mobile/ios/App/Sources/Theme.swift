import SwiftUI
import UIKit

/// Sitedeki renk token'ları (src/styles.css): açık "kâğıt" ve koyu "gece" paleti. 4 Kasım 2026'dan beri doğal tasarımın
/// sıcak taban renkleri (docs/dogal-tasarim, bölüm 3.1; sahibin onayı): krem kâğıt, sıcak gece, kahve tonlu yazı.
/// Uygulama teması kullanıcı seçimidir (Ayarlar › Uygulama rengi, `accent` anahtarı): her tema bir ana renk,
/// bir ikinci renk ve bir ton gücüyle gelir; zemin, kartlar, saat kadranı ve ibreler buna göre boyanır.
/// Varsayılan temada ton yoktur (sitedeki renkler). Widget'lar varsayılanı kullanır.
enum Theme {
    /// Zemin: atmosfer seçiliyse renk geçişinin orta tonu (düz renk isteyen yerler için; sayfa zemini AppBackdrop)
    static var bg: Color { Atmosphere.current?.mid ?? tinted(0xF6F0E4, 0x14151C, light: 0.09, dark: 0.11) }
    static var surface: Color { tinted(0xFFFBF4, 0x1D1E27, light: 0.04, dark: 0.07) }
    /// Saat kadranı ve ikincil yüzeyler
    static var surface2: Color { tinted(0xEDE4D3, 0x272834, light: 0.18, dark: 0.20) }
    static let text = dynamic(light: 0x2B2520, dark: 0xF1EADF)
    static let muted = dynamic(light: 0x6A5F55, dark: 0xB4AA9B)
    /// Süs rengi (ayraç noktaları, soluk çizimler): yazıda kullanılmaz
    static let faint = dynamic(light: 0xA89A88, dark: 0x6E675E)
    static var border: Color { tinted(0xE2D6C1, 0x343542, light: 0.35, dark: 0.35) }
    /// İnce ayraç çizgisi (doğal tasarımdaki `line`; kartlarda kenarlık yok)
    static var line: Color { border }
    static var accent: Color { AccentPalette.current.color }
    /// Ana rengin yumuşak düz tonu (saydam değil; üstündeki yazı ≥4,5:1)
    static var accentSoft: Color {
        let p = AccentPalette.current
        return dynamic(light: mix(0xFFFBF4, p.light, 0.15), dark: mix(0x1D1E27, p.dark, 0.16))
    }
    /// Ana renkli düğmenin yazısı
    static let onAccent = dynamic(light: 0xFFF8EF, dark: 0x1A1410)
    /// İkinci renk: saniye ibresi, saat ortası, vurgu ayrıntıları
    static var accent2: Color { AccentPalette.current.secondColor }
    /// Saat rakamları ve akrep: yazı rengi, temanın ana rengine doğru çekilmiş
    static var ink: Color { tinted(0x2B2520, 0xF1EADF, light: 0.45, dark: 0.30) }
    /// Hata ve başarı renkleri: sitedeki --err/--ok token'ları (2. analiz #100; iki temada ≥4,5:1)
    static let err = dynamic(light: 0xA3311B, dark: 0xFF9F8A)
    static let errBg = dynamic(light: 0xFBE7E7, dark: 0x3A1A1A)
    static let ok = dynamic(light: 0x2E6A39, dark: 0x8FD3A0)
    static let okBg = dynamic(light: 0xE2F3E8, dark: 0x16301F)
    /// Doğa renkleri (doğal tasarım, docs/dogal-tasarim; 4 Kasım 2026): Tümü kategorileri, tanıtım çizimleri
    static let terra = dynamic(light: 0xB4532A, dark: 0xF2A65A)
    static let sage = dynamic(light: 0x4A6B47, dark: 0x9CC294)
    static let sky = dynamic(light: 0x3A6889, dark: 0x8DB8DA)
    static let gold = dynamic(light: 0x835A0D, dark: 0xE2B65C)
    static let dusk = dynamic(light: 0x7A4760, dark: 0xD69BB5)

    fileprivate static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(rgb: dark) : UIColor(rgb: light) })
    }

    /// Temel rengi seçili temanın ana rengine doğru karıştırır (oran × temanın ton gücü)
    private static func tinted(_ light: UInt32, _ dark: UInt32, light lt: Double, dark dt: Double) -> Color {
        let p = AccentPalette.current
        let k = p.tint
        return Color(UIColor { tc in
            tc.userInterfaceStyle == .dark
                ? UIColor(rgb: mix(dark, p.dark, dt * k))
                : UIColor(rgb: mix(light, p.light, lt * k))
        })
    }

    static func mix(_ a: UInt32, _ b: UInt32, _ t: Double) -> UInt32 {
        func ch(_ v: UInt32, _ s: UInt32) -> Double { Double((v >> s) & 0xFF) }
        func m(_ s: UInt32) -> UInt32 { UInt32((ch(a, s) * (1 - t) + ch(b, s) * t).rounded()) << s }
        return m(16) | m(8) | m(0)
    }
}

/// Uygulama temaları: dört grup, her grupta altı tema. Ana ve ikinci renk açık temada koyu, koyu temada açık
/// tonla verilir (iki temada da okunur); `tint` zeminin ve kadranın ne kadar boyanacağı.
struct AccentPalette: Identifiable {
    enum Group: String, CaseIterable { case general, kids, elegant, business }

    let id: String
    let group: Group
    let light: UInt32
    let dark: UInt32
    let second: (light: UInt32, dark: UInt32)
    let tint: Double

    var color: Color { Theme.dynamic(light: light, dark: dark) }
    var secondColor: Color { Theme.dynamic(light: second.light, dark: second.dark) }
    /// Bu temada saat kadranının rengi (seçicideki örnek için)
    var faceColor: Color {
        Theme.dynamic(light: Theme.mix(0xEDE4D3, light, 0.18 * tint), dark: Theme.mix(0x272834, dark, 0.20 * tint))
    }

    static let defaultID = "amber"

    private init(_ id: String, _ group: Group, _ light: UInt32, _ dark: UInt32, second: (UInt32, UInt32), tint: Double) {
        self.id = id
        self.group = group
        self.light = light
        self.dark = dark
        self.second = second
        self.tint = tint
    }

    static let all: [AccentPalette] = [
        // Genel (varsayılan: sitedeki renkler, ton yok)
        .init("amber", .general, 0x9A3F00, 0xF2A65A, second: (0x9A3F00, 0xF2A65A), tint: 0),
        .init("blue", .general, 0x0A66C2, 0x5AA9FF, second: (0xC2410A, 0xFF9E5A), tint: 0.8),
        .init("green", .general, 0x1B7A43, 0x5BD38A, second: (0xA15C00, 0xF5C04A), tint: 0.8),
        .init("indigo", .general, 0x3949AB, 0x9FA8DA, second: (0xC2185B, 0xF48FB1), tint: 0.8),
        .init("orange", .general, 0xB85400, 0xFFB74D, second: (0x00695C, 0x4DB6AC), tint: 0.8),
        .init("cyan", .general, 0x00708A, 0x4DD0E1, second: (0xAD1457, 0xF06292), tint: 0.8),
        // Çocuklar için: canlı ve iki renkli
        .init("kid-red", .kids, 0xC62828, 0xFF6B6B, second: (0x1565C0, 0x64B5F6), tint: 1),
        .init("kid-orange", .kids, 0xD84315, 0xFF9E5E, second: (0x6A1B9A, 0xCE93D8), tint: 1),
        .init("kid-yellow", .kids, 0x8A6D00, 0xFFD54F, second: (0xC62828, 0xFF6B6B), tint: 1),
        .init("kid-green", .kids, 0x2E7D32, 0x7BD88F, second: (0xD84315, 0xFF9E5E), tint: 1),
        .init("kid-blue", .kids, 0x1565C0, 0x64B5F6, second: (0x8A6D00, 0xFFD54F), tint: 1),
        .init("kid-purple", .kids, 0x7B1FA2, 0xCE93D8, second: (0x2E7D32, 0x7BD88F), tint: 1),
        // Zarif: gül, mercan, leylak tonları
        .init("rose", .elegant, 0xAD1457, 0xF48FB1, second: (0x8D6E63, 0xD7B8A8), tint: 0.9),
        .init("coral", .elegant, 0xB4442E, 0xFF8A75, second: (0x00796B, 0x80CBC4), tint: 0.9),
        .init("mauve", .elegant, 0x8E4A6E, 0xE0A3C4, second: (0x5C6BC0, 0xB3BCF5), tint: 0.9),
        .init("lavender", .elegant, 0x6A4FA3, 0xB9A5F0, second: (0xAD1457, 0xF48FB1), tint: 0.9),
        .init("plum", .elegant, 0x6D2E5B, 0xD69ACB, second: (0xA1887F, 0xE6C9B8), tint: 0.9),
        .init("rosegold", .elegant, 0x9C5A4A, 0xE8B4A0, second: (0x6D4C41, 0xD7B8A8), tint: 0.9),
        // İş: sakin, koyu tonlar
        .init("navy", .business, 0x1F3A68, 0x8AB4F8, second: (0xA67C00, 0xE6C35C), tint: 0.6),
        .init("slate", .business, 0x3E4C5E, 0xA9B7C8, second: (0x0A66C2, 0x5AA9FF), tint: 0.6),
        .init("graphite", .business, 0x333333, 0xD0D0D0, second: (0xC62828, 0xFF6B6B), tint: 0.5),
        .init("teal", .business, 0x00695C, 0x4DB6AC, second: (0x37474F, 0xB0BEC5), tint: 0.6),
        .init("burgundy", .business, 0x7A1F2B, 0xE08A95, second: (0x8D6E63, 0xD7B8A8), tint: 0.6),
        .init("forest", .business, 0x2F5233, 0x8FC79A, second: (0x8A6D00, 0xE6C35C), tint: 0.6),
    ]

    static func find(_ id: String?) -> AccentPalette {
        all.first { $0.id == id } ?? all[0]
    }

    static var current: AccentPalette { find(UserDefaults.standard.string(forKey: "accent")) }
}

/// Adlı atmosfer temaları (2. analiz #78; sahibin ekran görüntüsü): bütün uygulamanın zemini yukarıdan aşağı bir renk
/// geçişiyle boyanır (açık temada soluk, koyu temada derin tonlar; yazılar iki temada da okunur). Uygulama rengi
/// (`accent`) ayrı seçilir. Seçim `atmo` anahtarında (boş = düz); Android'deki Atmosphere ile aynı renkler.
struct Atmosphere: Identifiable {
    static let key = "atmo"
    let id: String
    let light: (UInt32, UInt32)
    let dark: (UInt32, UInt32)

    var top: Color { Theme.dynamic(light: light.0, dark: dark.0) }
    var bottom: Color { Theme.dynamic(light: light.1, dark: dark.1) }
    var mid: Color { Theme.dynamic(light: Theme.mix(light.0, light.1, 0.5), dark: Theme.mix(dark.0, dark.1, 0.5)) }

    static let all: [Atmosphere] = [
        .init(id: "dawn", light: (0xFDEFE3, 0xF6D9E2), dark: (0x2B1C2F, 0x41243A)),
        .init(id: "lavender", light: (0xF2ECFB, 0xE1D6F5), dark: (0x1F1A33, 0x31254F)),
        .init(id: "ocean", light: (0xE5F3F8, 0xCFE5F1), dark: (0x0D1F2C, 0x113653)),
        .init(id: "forest", light: (0xEAF5EA, 0xD5EAD8), dark: (0x0F2119, 0x1A382A)),
        .init(id: "desert", light: (0xFCF2E2, 0xF1DBBA), dark: (0x2A2016, 0x40301B)),
        .init(id: "aurora", light: (0xE6F7F1, 0xE3DFF8), dark: (0x0D2B2A, 0x2B1F47)),
        .init(id: "sunset", light: (0xFEEBDD, 0xF8D3D6), dark: (0x301A11, 0x3D1729)),
        .init(id: "night", light: (0xE7ECF7, 0xD5DCEF), dark: (0x0A0F1F, 0x1A2342)),
    ]

    /// "Gün ışığı" (doğal tasarım madde 7, 4 Kasım 2026; yeni kurulumda varsayılan): seçili yerde Güneş'in yüksekliğine
    /// ve yükselip alçalmasına göre şafak, sabah/öğle, ikindi (altın saat), akşam ya da gece tonu; sayfanın üstünden
    /// zemine iner. Uygulamada Güneş hesabı `sun` ile verilir; saat widget'ları App Group'taki konumla aynı tonu çizer
    /// (`widgetDaylight`, doğal tasarım madde 19).
    static let daylightID = "daylight"
    /// Uygulama açılırken kurulur: seçili yerde Güneş'in yüksekliği (derece) ve yükselip yükselmediği
    static var sun: ((Date) -> (alt: Double, rising: Bool))?

    static func daylight(at date: Date = Date()) -> Atmosphere {
        let s = sun?(date) ?? (alt: 30, rising: true)
        return daylightTone(alt: s.alt, rising: s.rising)
    }

    /// Güneş'in yüksekliğine (derece) ve yükselip alçalmasına göre ton (Android'deki Atmosphere.daylightTone ile aynı eşikler)
    static func daylightTone(alt: Double, rising: Bool) -> Atmosphere {
        let tone: (UInt32, UInt32)
        if alt < -6 { tone = (0xE3E4EE, 0x12131C) }
        else if alt < 6 { tone = rising ? (0xF8E4D6, 0x2A1E2C) : (0xEEDDE4, 0x271B2C) }
        else if !rising && alt < 25 { tone = (0xF6E3C4, 0x2A2118) }
        else { tone = (0xE9EFF0, 0x172230) }
        return Atmosphere(id: daylightID, light: (tone.0, 0xF6F0E4), dark: (tone.1, 0x14151C))
    }

    /// App Group'taki küçük paylaşım (uygulama yazar, widget okur): atmosfer seçimi ve seçili yerin konumu
    static let groupID = "group.tr.bumba.time"

    /// Uygulama öne gelince ve arka plana geçerken yazar (saat widget'larının gün ışığı zemini için)
    static func publish(atmo: String, lat: Double, lon: Double) {
        guard let d = UserDefaults(suiteName: groupID) else { return }
        d.set(atmo, forKey: key)
        d.set(lat, forKey: "place.lat")
        d.set(lon, forKey: "place.lon")
    }

    /// Saat widget'larında gün ışığı tonu: uygulamada "Gün ışığı" seçiliyse seçili yerde o andaki ton, değilse nil
    static func widgetDaylight(at date: Date) -> Atmosphere? {
        guard let d = UserDefaults(suiteName: groupID), d.string(forKey: key) == daylightID,
              d.object(forKey: "place.lat") != nil else { return nil }
        let lat = d.double(forKey: "place.lat"), lon = d.double(forKey: "place.lon")
        let alt = WorldMath.sunAltitude(lat: lat, lon: lon, at: date)
        let rising = WorldMath.sunAltitude(lat: lat, lon: lon, at: date.addingTimeInterval(600)) > alt
        return daylightTone(alt: alt, rising: rising)
    }

    static func find(_ id: String?) -> Atmosphere? { id == daylightID ? daylight() : all.first { $0.id == id } }
    static var current: Atmosphere? { find(UserDefaults.standard.string(forKey: key)) }
}

/// Sayfa zemini: atmosfer seçiliyse renk geçişi, değilse temanın zemin rengi; üstünde %4 kâğıt dokusu (doğal tasarım
/// madde 8: kodla üretilen gri gren, tekrar eden 96 px döşeme)
struct AppBackdrop: View {
    @AppStorage(Atmosphere.key) private var atmo = ""
    @AppStorage("accent") private var accent = AccentPalette.defaultID
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        base.overlay {
            Image(uiImage: PaperGrain.image)
                .resizable(resizingMode: .tile)
                .opacity(scheme == .dark ? 0.05 : 0.04)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder private var base: some View {
        if atmo == Atmosphere.daylightID {
            // Gün ışığı tonu beş dakikada bir yenilenir; geçiş 1,2 sn (hareketi azalt açıksa anında)
            TimelineView(.periodic(from: .now, by: 300)) { ctx in
                let a = Atmosphere.daylight(at: ctx.date)
                LinearGradient(colors: [a.top, a.bottom], startPoint: .top, endPoint: .bottom)
                    .animation(.easeInOut(duration: 1.2), value: a.light.0)
            }
        } else if let a = Atmosphere.find(atmo) {
            LinearGradient(colors: [a.top, a.bottom], startPoint: .top, endPoint: .bottom)
        } else {
            Theme.bg
        }
    }
}

/// Kâğıt dokusu: sabit tohumlu gri gürültü (her açılışta aynı; Android'deki PaperGrain ile aynı boyut)
enum PaperGrain {
    static let image: UIImage = {
        let n = 96
        var seed: UInt32 = 7
        var bytes = [UInt8](repeating: 0, count: n * n)
        for i in 0..<bytes.count {
            seed = seed &* 1_664_525 &+ 1_013_904_223
            bytes[i] = UInt8(truncatingIfNeeded: seed >> 24)
        }
        let image: CGImage? = bytes.withUnsafeMutableBytes { buf in
            CGContext(data: buf.baseAddress, width: n, height: n, bitsPerComponent: 8, bytesPerRow: n,
                      space: CGColorSpaceCreateDeviceGray(), bitmapInfo: CGImageAlphaInfo.none.rawValue)?.makeImage()
        }
        return image.map { UIImage(cgImage: $0) } ?? UIImage()
    }()
}

/// Liste ve form sayfalarının zemini temanın zeminidir (atmosfer seçiliyse renk geçişi); tema değişince yeniden boyanır
struct ThemedBackground: ViewModifier {
    @AppStorage("accent") private var accent = AccentPalette.defaultID

    func body(content: Content) -> some View {
        content
            .scrollContentBackground(.hidden)
            .background(AppBackdrop().ignoresSafeArea())
    }
}

extension View {
    func themedBackground() -> some View { modifier(ThemedBackground()) }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255,
                  blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}
