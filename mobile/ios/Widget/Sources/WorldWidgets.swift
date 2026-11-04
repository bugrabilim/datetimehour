import SwiftUI
import WidgetKit
import AppIntents

/*
 * Dünya ve araç widget'ları (2. analiz #130, #136; 4 Kasım 2026). App Group olmadığı için uygulamanın dünya
 * listesi okunamaz: şehirler widget ayarından seçilir (en çok 8; seçilmezse 8 varsayılan şehir).
 *  - 8 şehir: kısa kod, ad, gündüz/gece işareti, saat (dakikada bir kayıt)
 *  - Saat çakışması: şehirlerin 09–18 mesai saatleri 24 saatlik şeritlerde, ortak saatler çerçeveli
 *  - Dünya haritası: gece gölgesi, Güneş noktası ve şehirlerin saati (15 dakikada bir)
 *  - Hazır süreler: 5/10/15/30 dk zamanlayıcı ve 25 dk odak; dokununca uygulama açılır ve başlatır
 *    (iOS widget'ı uygulamanın süren zamanlayıcılarını okuyamaz; onlar kilit ekranında Live Activity olarak görünür)
 */

private func widgetFormat(_ key: String, _ arg: String) -> String { String(format: L(key), arg) }

private func hhmm(_ date: Date, _ tz: TimeZone) -> String {
    var cal = Calendar(identifier: .gregorian)
    cal.timeZone = tz
    let c = cal.dateComponents([.hour, .minute], from: date)
    return String(format: "%02d:%02d", c.hour ?? 0, c.minute ?? 0)
}

struct World8Intent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "widget.world8.name"
    static var description = IntentDescription("widget.world8.desc")

    @Parameter(title: "widget.city") var city1: CityEntity?
    @Parameter(title: "widget.city") var city2: CityEntity?
    @Parameter(title: "widget.city") var city3: CityEntity?
    @Parameter(title: "widget.city") var city4: CityEntity?
    @Parameter(title: "widget.city") var city5: CityEntity?
    @Parameter(title: "widget.city") var city6: CityEntity?
    @Parameter(title: "widget.city") var city7: CityEntity?
    @Parameter(title: "widget.city") var city8: CityEntity?

    static var defaults: [CityEntity] {
        ["istanbul", "london", "new-york", "tokyo", "dubai", "berlin", "los-angeles", "sydney"].compactMap { k in CityEntity.all.first { $0.key == k } }
    }

    var cities: [CityEntity] {
        let picked = [city1, city2, city3, city4, city5, city6, city7, city8].compactMap { $0 }
        return picked.isEmpty ? Self.defaults : picked
    }
}

struct World8Entry: TimelineEntry {
    let date: Date
    let cities: [CityEntity]
}

/// `step` saniyede bir kayıt (`count` kayıt); bitince yeniden kurulur
struct World8Provider: AppIntentTimelineProvider {
    var step: TimeInterval = 60
    var count = 120

    func placeholder(in context: Context) -> World8Entry { World8Entry(date: Date(), cities: World8Intent.defaults) }
    func snapshot(for configuration: World8Intent, in context: Context) async -> World8Entry {
        World8Entry(date: Date(), cities: configuration.cities)
    }
    func timeline(for configuration: World8Intent, in context: Context) async -> Timeline<World8Entry> {
        let now = Date()
        let start = Calendar.current.dateInterval(of: .minute, for: now)?.start ?? now
        let cities = configuration.cities
        let entries = (0..<count).map { World8Entry(date: start.addingTimeInterval(Double($0) * step), cities: cities) }
        return Timeline(entries: entries, policy: .atEnd)
    }
}

struct World8Widget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "world-8", intent: World8Intent.self, provider: World8Provider()) { entry in
            World8View(entry: entry).widgetURL(URL(string: "saattarih://world"))
        }
        .configurationDisplayName(L("widget.world8.name"))
        .description(L("widget.world8.desc"))
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

struct World8View: View {
    @Environment(\.widgetFamily) private var family
    let entry: World8Entry

    private func mark(_ c: CityEntity) -> String {
        switch WorldMath.light(lat: c.lat, lon: c.lon, zone: c.zone, at: entry.date) {
        case "day": return "☀"
        case "dusk": return "◐"
        default: return "☾"
        }
    }

    private func row(_ c: CityEntity) -> some View {
        HStack(spacing: 6) {
            Text(WorldMath.code(c.displayName)).font(.caption.weight(.bold)).foregroundStyle(Theme.accent).frame(width: 34, alignment: .leading)
            Text(c.displayName).font(.caption).lineLimit(1)
            Spacer(minLength: 2)
            Text(mark(c)).font(.caption2).foregroundStyle(Theme.muted)
            Text(hhmm(entry.date, c.zone)).font(.system(.subheadline, design: .rounded).weight(.bold)).monospacedDigit()
                .environment(\.layoutDirection, .leftToRight)
        }
    }

    var body: some View {
        let list = Array(entry.cities.prefix(8))
        Group {
            if family == .systemMedium {
                // Ortada iki sütun
                let half = (list.count + 1) / 2
                HStack(alignment: .top, spacing: 12) {
                    VStack(spacing: 4) { ForEach(Array(list.prefix(half)), id: \.key) { row($0) } }
                    VStack(spacing: 4) { ForEach(Array(list.dropFirst(half)), id: \.key) { row($0) } }
                }
            } else {
                VStack(spacing: 8) { ForEach(list, id: \.key) { row($0) } }
            }
        }
        .foregroundStyle(Theme.text)
        .containerBackground(for: .widget) { Theme.bg }
    }
}

struct OverlapWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "time-overlap", intent: World8Intent.self, provider: World8Provider(step: 900, count: 16)) { entry in
            OverlapGrid(pins: entry.cities.prefix(6).map { (name: $0.displayName, zone: $0.zone) }, date: entry.date)
                .foregroundStyle(Theme.text)
                .containerBackground(for: .widget) { Theme.bg }
                .widgetURL(URL(string: "saattarih://world"))
        }
        .configurationDisplayName(L("widget.overlap.name"))
        .description(L("widget.overlap.desc"))
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}

struct WorldMapWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "world-map", intent: World8Intent.self, provider: World8Provider(step: 900, count: 16)) { entry in
            let pins: [MapPin] = entry.cities.compactMap { c in
                guard let lat = c.lat, let lon = c.lon else { return nil }
                return MapPin(id: c.key, name: c.displayName, zone: c.zone, lat: lat, lon: lon)
            }
            WorldMapDrawing(pins: pins, date: entry.date, fontSize: 9)
                .containerBackground(for: .widget) { Color(red: 0.055, green: 0.1, blue: 0.18) }
                .widgetURL(URL(string: "saattarih://world"))
        }
        .configurationDisplayName(L("widget.worldMap.name"))
        .description(L("widget.worldMap.desc"))
        .supportedFamilies([.systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

struct PresetEntry: TimelineEntry { let date: Date }

struct PresetProvider: TimelineProvider {
    func placeholder(in context: Context) -> PresetEntry { PresetEntry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (PresetEntry) -> Void) { completion(PresetEntry(date: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<PresetEntry>) -> Void) {
        completion(Timeline(entries: [PresetEntry(date: Date())], policy: .never))
    }
}

struct TimerPresetsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "timer-presets", provider: PresetProvider()) { _ in
            TimerPresetsView()
        }
        .configurationDisplayName(L("widget.timers.name"))
        .description(L("widget.timers.desc"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct TimerPresetsView: View {
    @Environment(\.widgetFamily) private var family

    private func chip(_ text: String, _ url: String) -> some View {
        Link(destination: URL(string: url)!) {
            Text(text)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Theme.accent)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Theme.accent.opacity(0.14), in: RoundedRectangle(cornerRadius: 12))
        }
    }

    var body: some View {
        let presets = [5, 10, 15, 30]
        let cols = family == .systemSmall ? 2 : 4
        VStack(alignment: .leading, spacing: 8) {
            Label(L("widget.timers.name"), systemImage: "timer").font(.caption.weight(.semibold)).foregroundStyle(Theme.muted)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: cols), spacing: 6) {
                ForEach(presets, id: \.self) { m in
                    chip(widgetFormat("watch.minutes", "\(m)"), "saattarih://timer?min=\(m)").frame(height: family == .systemSmall ? 46 : 54)
                }
            }
            if family == .systemMedium {
                chip("🍅 " + L("widget.focus.start"), "saattarih://pomodoro?start=1").frame(height: 34)
            }
        }
        .containerBackground(for: .widget) { Theme.bg }
    }
}

struct FocusStartWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "focus-start", provider: PresetProvider()) { _ in
            VStack(spacing: 10) {
                Text("🍅").font(.system(size: 34))
                Text(L("widget.focus.name")).font(.caption.weight(.semibold)).foregroundStyle(Theme.muted)
                Text(L("widget.focus.start"))
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Theme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(Theme.accent.opacity(0.14), in: RoundedRectangle(cornerRadius: 12))
            }
            .containerBackground(for: .widget) { Theme.bg }
            .widgetURL(URL(string: "saattarih://pomodoro?start=1"))
        }
        .configurationDisplayName(L("widget.focus.name"))
        .description(L("widget.focus.desc"))
        .supportedFamilies([.systemSmall])
    }
}

// MARK: Saniyeli çevirmeli saat (2. analiz #18)

struct FlipSecondsEntry: TimelineEntry { let date: Date }

/// Dakika başlarında kayıt (iki saat); saniye kartı sistemin sayaç metniyle akar
struct FlipSecondsProvider: TimelineProvider {
    func placeholder(in context: Context) -> FlipSecondsEntry { FlipSecondsEntry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (FlipSecondsEntry) -> Void) { completion(FlipSecondsEntry(date: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<FlipSecondsEntry>) -> Void) {
        let now = Date()
        let start = Calendar.current.dateInterval(of: .minute, for: now)?.start ?? now
        let entries = (0..<120).map { FlipSecondsEntry(date: start.addingTimeInterval(Double($0) * 60)) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

/// Widget'lar saniyede bir yenilenemediği için saniye kartında dakikanın başından sayan sistem sayacı (0:00–0:59)
/// gösterilir; baştaki "0:" kartın dışında kalacak biçimde kırpılır. Saniyeli analog widget iOS'ta yapılamaz
/// (WidgetKit ibreyi saniyede döndürmez).
struct FlipSecondsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "flip-seconds", provider: FlipSecondsProvider()) { entry in
            FlipSecondsView(date: entry.date)
                .containerBackground(for: .widget) { Theme.bg }
                .widgetURL(URL(string: "saattarih://clock"))
        }
        .configurationDisplayName(L("widget.flipSeconds.name"))
        .description(L("widget.flipSeconds.desc"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct FlipSecondsView: View {
    @Environment(\.widgetFamily) private var family
    let date: Date

    private func card(_ content: some View, width: CGFloat, height: CGFloat) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: height * 0.12).fill(dc(0x1A1B1E))
            content
            Rectangle().fill(Color.black.opacity(0.55)).frame(height: 1.5)
        }
        .frame(width: width, height: height)
    }

    var body: some View {
        let small = family == .systemSmall
        let h: CGFloat = small ? 58 : 92
        let fs: CGFloat = h * 0.62
        let comps = Calendar.current.dateComponents([.hour, .minute], from: date)
        let hh = String(format: "%02d", comps.hour ?? 0), mm = String(format: "%02d", comps.minute ?? 0)
        let minuteStart = Calendar.current.dateInterval(of: .minute, for: date)?.start ?? date
        let digitW = fs * 0.62
        HStack(spacing: small ? 4 : 8) {
            card(Text(hh).font(.system(size: fs, weight: .bold, design: .rounded)).monospacedDigit().foregroundStyle(dc(0xF5F5F5)),
                 width: h * 1.05, height: h)
            card(Text(mm).font(.system(size: fs, weight: .bold, design: .rounded)).monospacedDigit().foregroundStyle(dc(0xF5F5F5)),
                 width: h * 1.05, height: h)
            if !small {
                card(Text(timerInterval: minuteStart...minuteStart.addingTimeInterval(60), countsDown: false)
                        .font(.system(size: fs, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(Theme.accent)
                        .multilineTextAlignment(.trailing)
                        .frame(width: digitW * 2.05, alignment: .trailing)
                        .clipped(),
                     width: h * 1.05, height: h)
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .bottom) {
            if small {
                Text(timerInterval: minuteStart...minuteStart.addingTimeInterval(60), countsDown: false)
                    .font(.system(size: 14, weight: .bold, design: .rounded)).monospacedDigit()
                    .foregroundStyle(Theme.accent)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 20, alignment: .trailing)
                    .clipped()
                    .environment(\.layoutDirection, .leftToRight)
            }
        }
    }
}
