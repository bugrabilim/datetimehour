import AppIntents
import SwiftUI
import WidgetKit

/// Saat widget'larının görünümü (2. analiz #17): hazır renk takımları (galeri), yazı tipi ve çerçeve. Widget'a
/// uzun basıp Düzenle'den seçilir. App Group olmadığı için uygulamanın teması widget'a gelmez; "Uygulama" sitedeki
/// varsayılan renklerdir (açık/koyu temaya uyar), diğerleri sabit renklidir.
enum WidgetStyle: String, AppEnum {
    case app, paper, night, mint, rose, ocean, mono, neon
    /// Şeffaf: zemin duvar kâğıdından kesilen parça (2. analiz #76; WallpaperWidget.swift)
    case wallpaper

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "widget.style"
    static let caseDisplayRepresentations: [WidgetStyle: DisplayRepresentation] = [
        .app: "widget.style.app", .paper: "widget.style.paper", .night: "widget.style.night", .mint: "widget.style.mint",
        .rose: "widget.style.rose", .ocean: "widget.style.ocean", .mono: "widget.style.mono", .neon: "widget.style.neon",
        .wallpaper: "widget.style.wallpaper",
    ]

    struct Palette {
        let bg, text, muted, face, border, accent, second: Color
    }

    var palette: Palette? {
        func c(_ v: UInt32) -> Color { Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255) }
        func p(_ bg: UInt32, _ text: UInt32, _ muted: UInt32, _ face: UInt32, _ border: UInt32, _ accent: UInt32, _ second: UInt32) -> Palette {
            Palette(bg: c(bg), text: c(text), muted: c(muted), face: c(face), border: c(border), accent: c(accent), second: c(second))
        }
        switch self {
        case .app, .wallpaper: return nil
        case .paper: return p(0xF6F0E4, 0x2B2520, 0x6A5F55, 0xEDE4D3, 0xE2D6C1, 0x9A3F00, 0x9A3F00)
        case .night: return p(0x000000, 0xEEF0F4, 0xA5ACB9, 0x15181E, 0x343B49, 0xF2A65A, 0xFF6B6B)
        case .mint: return p(0xE3F4EA, 0x12351F, 0x3F6B50, 0xCDEBD8, 0x9CCFB0, 0x1B7A43, 0xD84315)
        case .rose: return p(0xFBE9EF, 0x3A1424, 0x7A4A5C, 0xF6D3DF, 0xE6A9BF, 0xAD1457, 0x8D6E63)
        case .ocean: return p(0x0B2540, 0xE6F1FF, 0x9FB8D6, 0x12345A, 0x2B5580, 0x5AA9FF, 0x4DD0E1)
        case .mono: return p(0xFFFFFF, 0x000000, 0x555555, 0xF2F2F2, 0x000000, 0x000000, 0x000000)
        case .neon: return p(0x0A0A12, 0xF0F0FF, 0x9A9AC0, 0x15152A, 0x3A1F6E, 0x39FF14, 0xFF2BD6)
        }
    }

    var clockColors: ClockColors? {
        palette.map { ClockColors(face: $0.face, border: $0.border, tick: $0.text, minorTick: $0.muted, ink: $0.text, hand: $0.accent, second: $0.second) }
    }
}

/// Dijital rakamların yazı tipi (uygulamadaki saat yazı tipleriyle aynı adlar)
enum WidgetFont: String, AppEnum {
    case rounded, standard, serif, mono, light

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "look.font"
    static let caseDisplayRepresentations: [WidgetFont: DisplayRepresentation] = [
        .rounded: "look.font.rounded", .standard: "look.font.standard", .serif: "look.font.serif",
        .mono: "look.font.mono", .light: "look.font.light",
    ]

    func font(_ size: CGFloat) -> Font {
        switch self {
        case .rounded: .system(size: size, weight: .bold, design: .rounded)
        case .standard: .system(size: size, weight: .bold, design: .default)
        case .serif: .system(size: size, weight: .bold, design: .serif)
        case .mono: .system(size: size, weight: .bold, design: .monospaced)
        case .light: .system(size: size, weight: .thin, design: .default)
        }
    }
}

/// Widget'a dokununca açılacak ekran (2. analiz #168); uygulama saattarih:// bağlantısını açar (Router.handle)
enum WidgetTap: String, AppEnum {
    case clock, calendar, prayer, world, alarm, stopwatch

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "widget.tap"
    static let caseDisplayRepresentations: [WidgetTap: DisplayRepresentation] = [
        .clock: "tab.clock", .calendar: "tab.calendar", .prayer: "tab.prayer", .world: "tab.world",
        .alarm: "tools.hub.cards.alarm.name", .stopwatch: "tools.hub.cards.stopwatch.name",
    ]

    var url: URL { URL(string: "saattarih://\(rawValue)")! }
}

struct ClockStyleIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "widget.style"
    static var description = IntentDescription("widget.styleNote")

    @Parameter(title: "widget.style", default: .app) var style: WidgetStyle
    @Parameter(title: "look.font", default: .rounded) var font: WidgetFont
    @Parameter(title: "widget.frame", default: false) var frame: Bool
    @Parameter(title: "widget.tap", default: .clock) var tap: WidgetTap
    /// Şeffaf görünümde widget'ın ana ekrandaki yeri (diğer görünümlerde kullanılmaz)
    @Parameter(title: "widget.spot", default: .topLeft) var spot: WidgetSpot
}

struct StyledEntry: TimelineEntry {
    let date: Date
    var style: WidgetStyle = .app
    var font: WidgetFont = .rounded
    var frame = false
    var city: CityEntity? = nil
    var tap: WidgetTap = .clock
    var spot: WidgetSpot = .topLeft

    /// Kadran renkleri: şeffaf görünümde duvar kâğıdına göre açık ya da koyu
    var clockColors: ClockColors? { style == .wallpaper ? WallpaperStore.clockColors(spot) : style.clockColors }
    var muted: Color { style == .wallpaper ? WallpaperStore.textColor(spot).opacity(0.75) : (style.palette?.muted ?? Theme.muted) }
}

/// Her dakikaya bir kayıt (2 saat); bitince yeniden kurulur
struct StyledClockProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> StyledEntry { StyledEntry(date: Date()) }
    func snapshot(for c: ClockStyleIntent, in context: Context) async -> StyledEntry {
        StyledEntry(date: Date(), style: c.style, font: c.font, frame: c.frame, tap: c.tap, spot: c.spot)
    }
    func timeline(for c: ClockStyleIntent, in context: Context) async -> Timeline<StyledEntry> {
        let now = Date()
        let start = Calendar.current.dateInterval(of: .minute, for: now)?.start ?? now
        return Timeline(entries: (0..<120).map {
            StyledEntry(date: start.addingTimeInterval(Double($0) * 60), style: c.style, font: c.font, frame: c.frame, tap: c.tap, spot: c.spot)
        }, policy: .atEnd)
    }
}

extension View {
    /// Seçilen görünümün yazı rengi, zemini ve (isteğe bağlı) çerçevesi
    func styledWidget(_ e: StyledEntry) -> some View { modifier(StyledWidget(entry: e)) }
}

private struct StyledWidget: ViewModifier {
    @Environment(\.widgetFamily) private var family
    let entry: StyledEntry

    func body(content: Content) -> some View {
        let e = entry
        let p = e.style.palette
        let wallpaper = e.style == .wallpaper ? WallpaperStore.image(family, e.spot) : nil
        let text = e.style == .wallpaper ? WallpaperStore.textColor(e.spot) : (p?.text ?? Theme.text)
        return content
            .foregroundStyle(text)
            .widgetURL(e.tap.url)
            .containerBackground(for: .widget) {
                ZStack {
                    if let wallpaper {
                        Image(uiImage: wallpaper).resizable().scaledToFill()
                    } else if e.style == .app, !e.frame, let a = Atmosphere.widgetDaylight(at: e.date) {
                        // Gün ışığı tonu (doğal tasarım madde 19): uygulamada "Gün ışığı" seçiliyken, varsayılan görünümde
                        LinearGradient(colors: [a.top, a.bottom], startPoint: .top, endPoint: .bottom)
                    } else {
                        p?.bg ?? Theme.bg
                    }
                    if e.frame {
                        ContainerRelativeShape().inset(by: 5).stroke(e.style == .wallpaper ? text : (p?.accent ?? Theme.accent), lineWidth: 3)
                    }
                }
            }
    }
}

// MARK: İki saat (2. analiz #12)

struct DualIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "widget.dual.name"
    static var description = IntentDescription("widget.dual.desc")

    @Parameter(title: "widget.city") var city: CityEntity?
    @Parameter(title: "widget.style", default: .app) var style: WidgetStyle
    @Parameter(title: "widget.frame", default: false) var frame: Bool
    @Parameter(title: "widget.spot", default: .topLeft) var spot: WidgetSpot

    var resolvedCity: CityEntity? {
        city ?? CityEntity.all.first { $0.key == (TimeZone.current.identifier == "Europe/London" ? "new-york" : "london") }
    }
}

struct DualProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> StyledEntry { StyledEntry(date: Date(), city: CityEntity.defaults.first) }
    func snapshot(for c: DualIntent, in context: Context) async -> StyledEntry {
        StyledEntry(date: Date(), style: c.style, frame: c.frame, city: c.resolvedCity, spot: c.spot)
    }
    func timeline(for c: DualIntent, in context: Context) async -> Timeline<StyledEntry> {
        let now = Date()
        let start = Calendar.current.dateInterval(of: .minute, for: now)?.start ?? now
        let city = c.resolvedCity
        return Timeline(entries: (0..<120).map {
            StyledEntry(date: start.addingTimeInterval(Double($0) * 60), style: c.style, frame: c.frame, city: city, spot: c.spot)
        }, policy: .atEnd)
    }
}

struct DualClockWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "dual-clock", intent: DualIntent.self, provider: DualProvider()) { entry in
            DualWidgetView(entry: entry)
        }
        .configurationDisplayName(L("widget.dual.name"))
        .description(L("widget.dual.desc"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct DualWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: StyledEntry

    var body: some View {
        let zone = entry.city?.zone ?? .current
        let small = family == .systemSmall
        HStack(spacing: small ? 6 : 16) {
            face(.current, caption: nil, title: L("dual.here"), small: small)
            face(zone, caption: entry.city.map { code($0.key) + " " + diff(zone) }, title: entry.city?.displayName ?? "", small: small)
        }
        .padding(small ? 2 : 6)
        .styledWidget(entry)
    }

    private func face(_ zone: TimeZone, caption: String?, title: String, small: Bool) -> some View {
        VStack(spacing: 3) {
            AnalogClock(date: entry.date, timeZone: zone, colors: entry.clockColors, caption: small ? nil : caption)
            Text(small ? (caption ?? title) : title).font(.system(size: small ? 11 : 13, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
                .environment(\.layoutDirection, .leftToRight)
        }
    }

    private func diff(_ zone: TimeZone) -> String {
        let d = zone.secondsFromGMT(for: entry.date) - TimeZone.current.secondsFromGMT(for: entry.date)
        if d == 0 { return "±0" }
        let h = abs(d) / 3600, m = (abs(d) % 3600) / 60
        return (d > 0 ? "+" : "−") + (m == 0 ? "\(h)" : String(format: "%d:%02d", h, m))
    }

    private func code(_ key: String) -> String {
        let known = ["new-york": "NYC", "los-angeles": "LAX", "hong-kong": "HKG", "mexico-city": "MEX", "sao-paulo": "SAO",
                     "buenos-aires": "BUE", "mumbai": "BOM", "beijing": "BJS", "tokyo": "TYO", "seoul": "SEL"]
        return known[key] ?? String(key.replacingOccurrences(of: "-", with: "").prefix(3)).uppercased()
    }
}
