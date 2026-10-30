import AppIntents
import SwiftUI
import WidgetKit

/* Saniyeli saat, dünya saatleri ve geri sayım widget'ları (30 Ekim 2026, rakip analizi #11).
   App Group olmadığı için uygulamanın listeleri okunamaz: şehirler ve geri sayım widget ayarından seçilir. */

/// 24 saat, her dilde Latin rakamı; verilen saat diliminde
private func hm(_ date: Date, _ tz: TimeZone) -> String {
    let f = DateFormatter()
    f.locale = Locale(identifier: "en_US_POSIX")
    f.timeZone = tz
    f.dateFormat = "HH:mm"
    return f.string(from: date)
}

/// Tarih her bölgede Gregoryen takvimle
private func longDate(_ date: Date) -> String {
    var s = Date.FormatStyle.dateTime.day().month(.wide).year().weekday(.wide)
    s.calendar = Calendar(identifier: .gregorian)
    return date.formatted(s)
}

// MARK: Saniyeli saat

/// Gece yarısından bu yana geçen süreyi sayan sayaç saati gösterir (widget'lar saniyede yenilenemediği için);
/// her gece yarısı yeni kayıt
struct SecondsProvider: TimelineProvider {
    func placeholder(in context: Context) -> ClockEntry { ClockEntry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (ClockEntry) -> Void) { completion(ClockEntry(date: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<ClockEntry>) -> Void) {
        let start = Calendar.current.startOfDay(for: Date())
        let days = (0..<3).compactMap { Calendar.current.date(byAdding: .day, value: $0, to: start) }
        completion(Timeline(entries: [ClockEntry(date: Date())] + days.dropFirst().map(ClockEntry.init), policy: .atEnd))
    }
}

struct SecondsClockWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "seconds-clock", provider: SecondsProvider()) { entry in
            let start = Calendar.current.startOfDay(for: entry.date)
            let end = Calendar.current.date(byAdding: .day, value: 1, to: start) ?? start.addingTimeInterval(86400)
            VStack(spacing: 6) {
                Text(timerInterval: start...end, countsDown: false)
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
                    .environment(\.layoutDirection, .leftToRight)
                Text(longDate(entry.date))
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(Theme.muted)
                    .lineLimit(2)
            }
            .foregroundStyle(Theme.text)
            .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.seconds.name"))
        .description(L("widget.seconds.desc"))
        .supportedFamilies([.systemSmall])
    }
}

// MARK: Dünya saatleri

/// cities.json'daki şehir (uygulamadaki dünya saatleriyle aynı liste)
struct CityEntity: AppEntity, Decodable {
    let key: String
    let tz: String
    let name: [String: String]

    var id: String { key }
    var displayName: String { name[Bundle.main.preferredLocalizations.first ?? "tr"] ?? name["en"] ?? key }
    var zone: TimeZone { TimeZone(identifier: tz) ?? .current }

    static var typeDisplayRepresentation: TypeDisplayRepresentation { TypeDisplayRepresentation(name: "widget.city") }
    static var defaultQuery = CityQuery()
    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(displayName)") }

    static let all: [CityEntity] = {
        guard let url = Bundle.main.url(forResource: "cities", withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([CityEntity].self, from: data)) ?? []
    }()

    /// Hiç şehir seçilmediyse
    static var defaults: [CityEntity] { ["london", "new-york", "tokyo", "dubai"].compactMap { k in all.first { $0.key == k } } }
}

struct CityQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [CityEntity] { CityEntity.all.filter { identifiers.contains($0.id) } }
    func entities(matching string: String) async throws -> [CityEntity] {
        let fold = { (s: String) in s.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil) }
        return CityEntity.all.filter { fold($0.displayName).contains(fold(string)) }
    }
    func suggestedEntities() async throws -> [CityEntity] { CityEntity.all }
}

struct WorldIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "world.h1"
    static var description = IntentDescription("widget.world.descIOS")

    @Parameter(title: "widget.city") var city1: CityEntity?
    @Parameter(title: "widget.city") var city2: CityEntity?
    @Parameter(title: "widget.city") var city3: CityEntity?
    @Parameter(title: "widget.city") var city4: CityEntity?

    var cities: [CityEntity] {
        let picked = [city1, city2, city3, city4].compactMap { $0 }
        return picked.isEmpty ? CityEntity.defaults : picked
    }
}

struct WorldEntry: TimelineEntry {
    let date: Date
    let cities: [CityEntity]
}

/// Her dakikaya bir kayıt (2 saat); bitince yeniden kurulur
struct WorldProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> WorldEntry { WorldEntry(date: Date(), cities: CityEntity.defaults) }
    func snapshot(for configuration: WorldIntent, in context: Context) async -> WorldEntry {
        WorldEntry(date: Date(), cities: configuration.cities)
    }
    func timeline(for configuration: WorldIntent, in context: Context) async -> Timeline<WorldEntry> {
        let now = Date()
        let start = Calendar.current.dateInterval(of: .minute, for: now)?.start ?? now
        let cities = configuration.cities
        return Timeline(entries: (0..<120).map { WorldEntry(date: start.addingTimeInterval(Double($0) * 60), cities: cities) }, policy: .atEnd)
    }
}

struct WorldClockWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "world-clock", intent: WorldIntent.self, provider: WorldProvider()) { entry in
            WorldClockView(entry: entry)
        }
        .configurationDisplayName(L("world.h1"))
        .description(L("widget.world.descIOS"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct WorldClockView: View {
    @Environment(\.widgetFamily) private var family
    let entry: WorldEntry

    var body: some View {
        let cities = Array(entry.cities.prefix(family == .systemSmall ? 2 : 4))
        VStack(spacing: family == .systemSmall ? 10 : 6) {
            ForEach(cities, id: \.key) { c in
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(c.displayName).font(.subheadline.weight(.semibold)).lineLimit(1)
                        let diff = (c.zone.secondsFromGMT(for: entry.date) - TimeZone.current.secondsFromGMT(for: entry.date)) / 60
                        if diff != 0 {
                            Text((diff > 0 ? "+" : "−") + "\(abs(diff) / 60)" + (abs(diff) % 60 == 0 ? "" : ":\(String(format: "%02d", abs(diff) % 60))") + " " + L("client.hourShort"))
                                .font(.caption2)
                                .foregroundStyle(Theme.muted)
                                .environment(\.layoutDirection, .leftToRight)
                        }
                    }
                    Spacer(minLength: 4)
                    Text(hm(entry.date, c.zone))
                        .font(.system(size: family == .systemSmall ? 22 : 24, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .environment(\.layoutDirection, .leftToRight)
                }
            }
        }
        .foregroundStyle(Theme.text)
        .containerBackground(for: .widget) { Theme.bg }
    }
}

// MARK: Geri sayım

struct CountdownIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "tools.hub.cards.countdown.name"
    static var description = IntentDescription("widget.countdown.descIOS")

    @Parameter(title: "widget.countdown.titleParam") var title: String?
    @Parameter(title: "widget.countdown.dateParam") var date: Date?

    /// Seçilmediyse ya da geçtiyse sıradaki yılbaşı
    func target(now: Date) -> (title: String, date: Date) {
        if let date, date > now { return (title.flatMap { $0.isEmpty ? nil : $0 } ?? L("tools.hub.cards.countdown.name"), date) }
        let cal = Calendar(identifier: .gregorian)
        let next = cal.date(from: DateComponents(year: cal.component(.year, from: now) + 1, month: 1, day: 1)) ?? now
        return (L("calendar.events.newYear"), next)
    }
}

struct CountdownEntry: TimelineEntry {
    let date: Date
    let title: String
    let target: Date
}

/// Her gece yarısı ve hedef anında yeni kayıt (bir hafta)
struct CountdownProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> CountdownEntry {
        let t = CountdownIntent().target(now: Date())
        return CountdownEntry(date: Date(), title: t.title, target: t.date)
    }
    func snapshot(for configuration: CountdownIntent, in context: Context) async -> CountdownEntry {
        let t = configuration.target(now: Date())
        return CountdownEntry(date: Date(), title: t.title, target: t.date)
    }
    func timeline(for configuration: CountdownIntent, in context: Context) async -> Timeline<CountdownEntry> {
        let now = Date()
        let t = configuration.target(now: now)
        let start = Calendar.current.startOfDay(for: now)
        var dates = [now] + (1...7).compactMap { Calendar.current.date(byAdding: .day, value: $0, to: start) }
        dates = dates.filter { $0 < t.date } + [t.date.addingTimeInterval(1)]
        dates.append(t.date.addingTimeInterval(-86400))
        let entries = Set(dates.filter { $0 >= now }).sorted().map { CountdownEntry(date: $0, title: t.title, target: t.date) }
        return Timeline(entries: entries, policy: .atEnd)
    }
}

struct CountdownWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "countdown", intent: CountdownIntent.self, provider: CountdownProvider()) { entry in
            VStack(spacing: 6) {
                Text(entry.title).font(.subheadline.weight(.bold)).foregroundStyle(Theme.accent).lineLimit(1)
                if entry.target.timeIntervalSince(entry.date) < 86400 {
                    // Son 24 saat: saniyeli geri sayım
                    Text(entry.target, style: .timer)
                        .font(.system(size: 30, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .multilineTextAlignment(.center)
                        .environment(\.layoutDirection, .leftToRight)
                } else {
                    let days = Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: entry.date),
                                                               to: Calendar.current.startOfDay(for: entry.target)).day ?? 0
                    Text(String(format: L("client.daysLeft"), "\(days)"))
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.5)
                        .lineLimit(2)
                }
                Text(longDate(entry.target)).font(.caption2).foregroundStyle(Theme.muted).multilineTextAlignment(.center).lineLimit(2)
            }
            .foregroundStyle(Theme.text)
            .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("tools.hub.cards.countdown.name"))
        .description(L("widget.countdown.descIOS"))
        .supportedFamilies([.systemSmall])
    }
}
