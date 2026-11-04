import SwiftUI
import WeatherKit

/// Tam ekran saatin arka planı (30 Ekim 2026, rakip analizi #26): düz, fotoğraf ya da hareketli (gökyüzü, renk akışı,
/// yıldızlar). Seçim `fsBg`; kayıt yoksa fotoğraf varsa "photo", yoksa "plain". Hareketli olanlar Canvas ile çizilir;
/// "hareketi azalt" açıksa durağan tek kare. Gökyüzü, seçili yerde Güneş'in yüksekliğine göre (küredeki Güneş formülü)
/// gündüz, alacakaranlık ya da gece olur. Android'deki AnimatedBackground ile aynı renkler ve hareket.
enum FullscreenBackground: String, CaseIterable, Identifiable {
    case plain, photo, sky, flow, stars, weather
    /// Ahşap doku (2. analiz #24): durağan, telefonda çizilir
    case wood
    /// Üretken zeminler (2. analiz #23): eş yükselti, noktalı dünya, grenli geçiş, sıvı metal
    case topo, dots, grain, liquid

    static let key = "fsBg"
    var id: String { rawValue }
    var title: String { L("look.bg.\(rawValue)") }
    var animated: Bool { self != .plain && self != .photo }

    static var current: FullscreenBackground {
        if let raw = UserDefaults.standard.string(forKey: key), let v = FullscreenBackground(rawValue: raw) { return v }
        return UserDefaults.standard.double(forKey: FullscreenPhoto.versionKey) > 0 ? .photo : .plain
    }
}

/// Havaya göre arka planın türü (rakip analizi listesi #79): gökyüzünün üstüne çizilen hava; rüzgârlı 2. analiz #193
enum SkyWeather: String {
    case clear, cloudy, rain, snow, fog, storm, windy

    /// WeatherKit durumu → çizim türü
    init(_ c: WeatherCondition) {
        switch c {
        case .clear, .mostlyClear, .hot, .frigid: self = .clear
        case .partlyCloudy, .mostlyCloudy, .cloudy, .smoky: self = .cloudy
        case .breezy, .windy, .blowingDust: self = .windy
        case .foggy, .haze: self = .fog
        case .snow, .heavySnow, .flurries, .blowingSnow, .blizzard, .sleet, .wintryMix, .freezingRain, .hail, .sunFlurries: self = .snow
        case .thunderstorms, .isolatedThunderstorms, .scatteredThunderstorms, .strongStorms, .tropicalStorm, .hurricane: self = .storm
        case .rain, .heavyRain, .drizzle, .freezingDrizzle, .sunShowers: self = .rain
        @unknown default: self = .cloudy
        }
    }
}

/*
 * Ekrandaki hava (3 Kasım 2026, 2. analiz #193; sahibin isteği "hava durumu neyse ekran da öyle"): tür + yoğunluk.
 * Yağış (mm/sa) damla ve kar tanesi sayısını, rüzgâr (km/sa) bulut hızını, yağmurun eğimini ve kar savrulmasını,
 * bulut oranı bulut sayısını belirler; 30 km/sa ve üstü rüzgârda açık ya da bulutlu hava "rüzgârlı" olur (yapraklar
 * savrulur). Açık gündüz Güneş parlar, gece yıldızlar. Android'deki WeatherScene ve sitedeki weather-fx.js ile aynı
 * kurallar.
 */
struct WeatherScene: Equatable {
    var kind: SkyWeather
    var precip: Double = 0
    var wind: Double = 0
    /// 0–1; bilinmiyorsa −1
    var cloud: Double = -1

    static let windyKmh = 30.0
    /// Saat ekranının zemini hava (varsayılan kapalı) ve hava sekmesinin üst kartı (varsayılan açık)
    static let clockKey = "clock.weatherBg"
    static let tabKey = "weather.anim"

    init(kind: SkyWeather, precip: Double = 0, wind: Double = 0, cloud: Double = -1) {
        var k = kind
        if (k == .clear || k == .cloudy) && wind >= Self.windyKmh { k = .windy }
        self.kind = k
        self.precip = max(0, precip)
        self.wind = max(0, wind)
        self.cloud = cloud
    }

    /// WeatherKit güncel hava → sahne
    init(_ c: CurrentWeather) {
        self.init(kind: SkyWeather(c.condition),
                  precip: c.precipitationIntensity.converted(to: .metersPerSecond).value * 3_600_000,
                  wind: c.wind.speed.converted(to: .kilometersPerHour).value,
                  cloud: c.cloudCover)
    }

    var clouds: Int {
        switch kind {
        case .clear: return cloud < 0 ? 0 : min(2, Int((cloud * 3).rounded()))
        case .fog: return 3
        default: return cloud < 0 ? 4 : max(2, min(6, Int((2 + cloud * 4).rounded())))
        }
    }
    var cloudSpeed: Double { (kind == .fog ? 0.5 : 1) * (1 + wind / 12) }
    var drops: Int {
        switch kind {
        case .rain: return precip <= 0 ? 90 : max(40, min(240, Int(40 + precip * 30)))
        case .storm: return max(140, min(300, Int(140 + precip * 20)))
        default: return 0
        }
    }
    var slant: Double { min(0.9, 0.08 + wind / 80) }
    var flakes: Int { kind == .snow ? (precip <= 0 ? 110 : max(60, min(240, Int(60 + precip * 40)))) : 0 }
    var leaves: Int {
        switch kind {
        case .windy: return max(8, min(30, 8 + Int((wind - Self.windyKmh) / 3)))
        case .storm, .rain: return wind >= Self.windyKmh ? 10 : 0
        default: return 0
        }
    }
}

/// Hareketli arka plan; yazılar üstünde koyu temanın açık renkleriyle okunur
struct AnimatedBackground: View {
    let kind: FullscreenBackground
    var place: AppPlace = PlaceModel.shared.place
    /// Verilirse bu hava çizilir (hava sekmesi); yoksa seçili yerin havası alınır
    var scene: WeatherScene? = nil
    /// Apple Hava Durumu işareti burada gösterilsin mi (hava sekmesinde işaret zaten verinin altında)
    var showMark = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// Hava durumu arka planı için seçili yerin güncel havası (WeatherKit, 15 dk önbellek)
    @State private var weather: WeatherScene?

    var body: some View {
        ZStack(alignment: .topLeading) {
            TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { context in
                let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
                Canvas { ctx, size in
                    switch kind {
                    case .sky: Self.sky(&ctx, size, t: t, alt: Self.sunAltitude(place: place, at: context.date))
                    case .weather: Self.weather(&ctx, size, t: t, alt: Self.sunAltitude(place: place, at: context.date),
                                                scene: scene ?? weather ?? WeatherScene(kind: .clear))
                    case .flow: Self.flow(&ctx, size, t: t)
                    case .wood: Self.wood(&ctx, size)
                    case .topo: GenArt.topo(&ctx, size, t: t)
                    case .dots: GenArt.dots(&ctx, size, date: context.date)
                    case .grain: GenArt.grain(&ctx, size)
                    case .liquid: GenArt.liquid(&ctx, size, t: t)
                    default: Self.stars(&ctx, size, t: t, base: [Color(hex: 0x05070F), Color(hex: 0x111A3A)], alpha: 1)
                    }
                }
            }
            .ignoresSafeArea()
            .accessibilityHidden(true)
            // WeatherKit verisi gösterildiği için Apple Hava Durumu işareti ve yasal bağlantı (App Review 5.2.5)
            if kind == .weather, showMark, scene != nil || weather != nil {
                AppleWeatherMark(compact: true, onDark: true)
                    .padding(12)
            }
        }
        .task(id: kind == .weather && scene == nil ? place.key : "") {
            guard kind == .weather, scene == nil else { return }
            while !Task.isCancelled {
                if let w = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) { weather = WeatherScene(w.currentWeather) }
                try? await Task.sleep(for: .seconds(900))
            }
        }
    }

    /// Gökyüzü (Güneş'in yüksekliğine göre) + hava: bulut, yağmur, kar, sis, şimşek, rüzgâr; yoğunluk sahneden
    static func weather(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double, alt: Double, scene: WeatherScene) {
        let w = scene.kind
        sky(&ctx, size, t: t, alt: alt, clouds: false)
        let full = Path(CGRect(origin: .zero, size: size))
        func rng(_ s0: UInt64) -> () -> Double {
            var seed = s0
            return { seed = seed &* 6364136223846793005 &+ 1442695040888963407; return Double(seed >> 11) / Double(1 << 53) }
        }
        switch w {
        case .clear, .windy:
            // Açık gündüz: Güneş parlaması (hafif nabız)
            if alt >= 6 {
                let c = CGPoint(x: size.width * 0.82, y: size.height * 0.12), r = size.width * 0.5
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)), with: .radialGradient(
                    Gradient(colors: [Color(hex: 0xFFF3C4).opacity(0.45 + 0.08 * sin(t * 0.5)), .clear]),
                    center: c, startRadius: 0, endRadius: r))
            }
        case .cloudy:
            ctx.fill(full, with: .color(Color(white: 0.45).opacity(0.15 + 0.15 * max(0, scene.cloud))))
        case .fog:
            ctx.fill(full, with: .linearGradient(Gradient(colors: [.white.opacity(0.15), .white.opacity(0.45)]),
                                                 startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
        case .rain, .storm:
            ctx.fill(full, with: .color(Color(white: 0.2).opacity(0.35)))
        case .snow:
            ctx.fill(full, with: .color(Color(white: 0.6).opacity(0.2)))
        }
        clouds(&ctx, size, t: t, count: scene.clouds, speed: scene.cloudSpeed, dark: w == .rain || w == .storm)
        if w == .storm {
            // Arada bir kısa şimşek (12 sn'de bir, 0,15 sn)
            let p = t.truncatingRemainder(dividingBy: 12)
            if p < 0.15 { ctx.fill(full, with: .color(.white.opacity(0.35 * (1 - p / 0.15)))) }
        }
        if scene.drops > 0 {
            let rnd = rng(0xA1A1), k = scene.slant
            var drops = Path()
            for _ in 0..<scene.drops {
                let x0 = rnd() * size.width, speed = 380 + rnd() * 220, len = 10 + rnd() * 14
                let y = (rnd() * size.height + t * speed).truncatingRemainder(dividingBy: size.height + len) - len
                var x = (x0 - (y + len) * k).truncatingRemainder(dividingBy: size.width)
                if x < 0 { x += size.width }
                drops.move(to: CGPoint(x: x, y: y))
                drops.addLine(to: CGPoint(x: x - len * k, y: y + len))
            }
            ctx.stroke(drops, with: .color(.white.opacity(0.45)), lineWidth: 1.2)
        }
        if scene.flakes > 0 {
            let rnd = rng(0x5E0), drift = scene.wind * 1.2
            for _ in 0..<scene.flakes {
                let x0 = rnd() * size.width, speed = 25 + rnd() * 45, r = 1.2 + rnd() * 2.6, ph = rnd() * 6.28
                let y = (rnd() * size.height + t * speed).truncatingRemainder(dividingBy: size.height + 10) - 5
                var x = (x0 + t * drift + 12 * sin(t * 0.8 + ph)).truncatingRemainder(dividingBy: size.width)
                if x < 0 { x += size.width }
                ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: 2 * r, height: 2 * r)), with: .color(.white.opacity(0.8)))
            }
        }
        if scene.leaves > 0 {
            // Savrulan yapraklar: hız rüzgârla artar, kendi çevresinde döner
            let rnd = rng(0x1EAF)
            let colors = [Color(hex: 0xE8590C), Color(hex: 0x82C91E), Color(hex: 0xB07D2B), Color(hex: 0xF59F00)]
            for i in 0..<scene.leaves {
                let speed = (90 + scene.wind * 3) * (0.7 + rnd() * 0.6), y0 = rnd() * size.height * 0.9, ph = rnd() * 6.28
                let x = (rnd() * (size.width + 40) + t * speed).truncatingRemainder(dividingBy: size.width + 40) - 20
                let y = y0 + 30 * sin(t * 1.5 + ph)
                var leaf = ctx
                leaf.translateBy(x: x, y: y)
                leaf.rotate(by: .radians(t * 3 + ph))
                leaf.fill(Path(ellipseIn: CGRect(x: -6, y: -3, width: 12, height: 6)), with: .color(colors[i % colors.count].opacity(0.85)))
            }
        }
    }

    /// Seçili yerde Güneş'in yüksekliği (derece): alt güneş noktasından küresel üçgen
    static func sunAltitude(place: AppPlace, at date: Date) -> Double {
        let s = GlobeSky.sun(date)
        let r = Double.pi / 180
        let v = sin(place.lat * r) * sin(s.lat * r) + cos(place.lat * r) * cos(s.lat * r) * cos((place.lon - s.lon) * r)
        return asin(max(-1, min(1, v))) / r
    }

    // MARK: Çizimler

    private static func gradient(_ ctx: inout GraphicsContext, _ size: CGSize, _ colors: [Color]) {
        ctx.fill(Path(CGRect(origin: .zero, size: size)),
                 with: .linearGradient(Gradient(colors: colors), startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
    }

    /// Gündüz mavi + süzülen bulutlar, alacakaranlıkta turuncu-mor, gece koyu lacivert + yıldızlar (geçişler yumuşak)
    static func sky(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double, alt: Double, clouds drawClouds: Bool = true) {
        let night = [Color(hex: 0x0B1026), Color(hex: 0x1B2350)]
        let dusk = [Color(hex: 0x2B3A67), Color(hex: 0xC8607A), Color(hex: 0xF6A75F)]
        let day = [Color(hex: 0x2F6FCB), Color(hex: 0x8EC9F2)]
        if alt <= -6 {
            stars(&ctx, size, t: t, base: night, alpha: 1)
        } else if alt < 6 {
            gradient(&ctx, size, dusk)
            // Alacakaranlığın karanlık ucunda yıldızlar belirir
            if alt < 0 { stars(&ctx, size, t: t, base: nil, alpha: -alt / 6) }
        } else {
            gradient(&ctx, size, day)
            if drawClouds { clouds(&ctx, size, t: t) }
        }
        // Yazılar okunsun diye hafif koyulaştırma
        ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black.opacity(alt >= 6 ? 0.2 : 0.1)))
    }

    /// Süzülen bulutlar: sayı bulut oranından, hız rüzgârdan; yağmurda koyu
    private static func clouds(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double, count: Int = 4, speed k: Double = 1, dark: Bool = false) {
        for i in 0..<count {
            let w = size.width * (0.45 + 0.1 * Double(i % 2)), h = w * 0.28
            let speed = (6.0 + Double(i % 4) * 3) * k // pt/sn
            let x = (Double(i) * size.width * 0.37 + t * speed).truncatingRemainder(dividingBy: size.width + w) - w
            let y = size.height * (0.12 + 0.17 * Double(i % 4) + 0.08 * Double(i / 4))
            let rect = CGRect(x: x, y: y, width: w, height: h)
            let c = dark ? Color(white: 0.75) : .white
            ctx.fill(Path(ellipseIn: rect), with: .radialGradient(
                Gradient(colors: [c.opacity(0.55), c.opacity(0)]),
                center: CGPoint(x: rect.midX, y: rect.midY), startRadius: 0, endRadius: w / 2))
        }
    }

    /// Koyu zemin üstünde dönerek kayan üç renk lekesi (uygulama renginden)
    /// Ahşap doku (2. analiz #24): ceviz tonlarında geçiş ve sabit tohumla dalgalı damarlar (Android'deki wood() ile aynı renkler)
    static func wood(_ ctx: inout GraphicsContext, _ size: CGSize) {
        let rect = CGRect(origin: .zero, size: size)
        ctx.fill(Path(rect), with: .linearGradient(
            Gradient(colors: [Color(hex: 0x4A2C17), Color(hex: 0x6B4226), Color(hex: 0x55331C), Color(hex: 0x6E4527)]),
            startPoint: .zero, endPoint: CGPoint(x: size.width, y: 0)))
        var seed: UInt64 = 0x5EED_1234
        func rnd() -> CGFloat {
            seed = seed &* 6364136223846793005 &+ 1442695040888963407
            return CGFloat(Double(seed >> 11) / Double(UInt64(1) << 53))
        }
        let lines = 70
        for i in 0..<lines {
            let x0 = size.width * (CGFloat(i) + rnd() * 0.8) / CGFloat(lines)
            let amp = size.width * (0.004 + rnd() * 0.012)
            let freq = 2 + rnd() * 4
            let phase = rnd() * 6.28
            var p = Path()
            p.move(to: CGPoint(x: x0, y: 0))
            var y: CGFloat = 0
            while y <= size.height {
                p.addLine(to: CGPoint(x: x0 + amp * sin(y / size.height * freq * 6.28 + phase), y: y))
                y += 12
            }
            let dark = rnd() < 0.5
            ctx.stroke(p, with: .color((dark ? Color(hex: 0x2E1A0C) : Color(hex: 0x8A5A36)).opacity(0.10 + rnd() * 0.18)),
                       lineWidth: 1 + rnd() * 3)
        }
        let cx = size.width * 0.68, cy = size.height * 0.36
        for k in 0..<4 {
            let w = size.width * (0.08 + CGFloat(k) * 0.06), h = size.width * (0.04 + CGFloat(k) * 0.03)
            ctx.stroke(Path(ellipseIn: CGRect(x: cx - w / 2, y: cy - h / 2, width: w, height: h)),
                       with: .color(Color(hex: 0x2E1A0C).opacity(0.18 - Double(k) * 0.03)), lineWidth: 2)
        }
    }

    static func flow(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double) {
        gradient(&ctx, size, [Color(hex: 0x0B1020), Color(hex: 0x141A33)])
        let colors = [Theme.accent, Theme.accent2, Color(hex: 0x6C4AB6)]
        let r = max(size.width, size.height) * 0.6
        for (k, c) in colors.enumerated() {
            let k = Double(k)
            let p = CGPoint(x: size.width * (0.5 + 0.35 * sin(t * 0.07 + k * 2.1)),
                            y: size.height * (0.5 + 0.35 * cos(t * 0.05 + k * 1.7)))
            ctx.fill(Path(ellipseIn: CGRect(x: p.x - r, y: p.y - r, width: 2 * r, height: 2 * r)), with: .radialGradient(
                Gradient(colors: [c.opacity(0.55), c.opacity(0)]), center: p, startRadius: 0, endRadius: r))
        }
    }

    /// Sabit tohumlu yıldızlar; her biri kendi hızıyla göz kırpar, gökyüzü çok yavaş kayar
    static func stars(_ ctx: inout GraphicsContext, _ size: CGSize, t: Double, base: [Color]?, alpha: Double) {
        if let base { gradient(&ctx, size, base) }
        var seed: UInt64 = 0x5EED
        func rnd() -> Double {
            seed = seed &* 6364136223846793005 &+ 1442695040888963407
            return Double(seed >> 11) / Double(1 << 53)
        }
        let drift = t * 2 // pt/sn
        for _ in 0..<140 {
            let x = (rnd() * size.width + drift).truncatingRemainder(dividingBy: size.width)
            let y = rnd() * size.height
            let s = 0.6 + rnd() * 1.6
            let phase = rnd() * 6.28, speed = 0.5 + rnd() * 1.5
            let a = (0.35 + 0.65 * (0.5 + 0.5 * sin(t * speed + phase))) * alpha
            ctx.fill(Path(ellipseIn: CGRect(x: x - s / 2, y: y - s / 2, width: s, height: s)), with: .color(.white.opacity(a)))
        }
    }
}

extension Color {
    /// 0xRRGGBB
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }
}
