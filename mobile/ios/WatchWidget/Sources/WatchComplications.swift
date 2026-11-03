import AppIntents
import SaatCore
import SwiftUI
import WidgetKit

/// Apple Watch komplikasyonları (rakip analizi #6): sıradaki vakit (geri sayımlı) ve hicri tarih. Saat yüzü düzenlenirken
/// yer seçilemediği için her yer ayrı bir öneri olarak listelenir: "Otomatik (konumum)" ve 81 il (ProvinceIntent).
@main
struct SaatTarihWatchWidgets: WidgetBundle {
    var body: some Widget {
        WatchNextPrayerWidget()
        WatchTodayPrayersWidget()
        WatchHijriWidget()
        WatchWeatherWidget()
    }
}

// MARK: Sıradaki vakit

struct WatchPrayerEntry: TimelineEntry {
    let date: Date
    let name: String
    let schedule: PrayerSchedule
}

struct WatchPrayerProvider: AppIntentTimelineProvider {
    private static var sample: WatchPrayerEntry {
        WatchPrayerEntry(date: Date(), name: Province.find("istanbul").displayName, schedule: PrayerSchedule(province: Province.find("istanbul")))
    }

    func placeholder(in context: Context) -> WatchPrayerEntry { Self.sample }

    func snapshot(for configuration: ProvinceIntent, in context: Context) async -> WatchPrayerEntry {
        let place = await configuration.resolvePlace()
        return WatchPrayerEntry(date: Date(), name: place.name, schedule: place.schedule)
    }

    /// Şu an ve önümüzdeki 24 saatteki her vakit için bir kayıt (sıradaki vakit kendiliğinden değişir)
    func timeline(for configuration: ProvinceIntent, in context: Context) async -> Timeline<WatchPrayerEntry> {
        let place = await configuration.resolvePlace()
        let now = Date()
        let times = (0...1).flatMap { place.schedule.entries(on: now.addingTimeInterval(Double($0) * 86_400)) }
            .map(\.at).filter { $0 > now && $0 < now.addingTimeInterval(86_400) }
        let entries = ([now] + times.map { $0.addingTimeInterval(1) }).map { WatchPrayerEntry(date: $0, name: place.name, schedule: place.schedule) }
        return Timeline(entries: entries, policy: .after(entries.last!.date.addingTimeInterval(60)))
    }

    func recommendations() -> [AppIntentRecommendation<ProvinceIntent>] {
        ProvinceEntity.all.map { e in
            var i = ProvinceIntent()
            i.province = e
            return AppIntentRecommendation(intent: i, description: e.name)
        }
    }
}

struct WatchNextPrayerWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "watch-next-prayer", intent: ProvinceIntent.self, provider: WatchPrayerProvider()) { entry in
            WatchNextPrayerView(entry: entry)
        }
        .configurationDisplayName(L("widget.next.name"))
        .description(L("widget.next.desc"))
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

struct WatchNextPrayerView: View {
    @Environment(\.widgetFamily) private var family
    let entry: WatchPrayerEntry

    var body: some View {
        let s = entry.schedule
        Group {
            if let next = s.next(after: entry.date) {
                let name = label(next, zone: s.timeZone), time = s.clock(next.at)
                switch family {
                case .accessoryInline:
                    Text(verbatim: "\(name) \(time)")
                case .accessoryCircular:
                    // Sıradaki vakte kalan süre halkası (2. analiz #162): önceki vakitten kendiliğinden dolar
                    if let prev = previous(s), prev < next.at {
                        ProgressView(timerInterval: prev...next.at, countsDown: false) {
                            Text(name)
                        } currentValueLabel: {
                            Text(time).font(.system(size: 13, weight: .bold, design: .rounded)).minimumScaleFactor(0.6)
                        }
                        .progressViewStyle(.circular)
                        .widgetAccentable()
                    } else {
                        VStack(spacing: 0) {
                            Text(name).font(.system(size: 10, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
                            Text(time).font(.system(size: 15, weight: .bold, design: .rounded)).minimumScaleFactor(0.7)
                        }
                    }
                case .accessoryCorner:
                    // Köşede kenar boyunca ilerleme yayı ve kavisli yazı (2. analiz #162)
                    Text(time).font(.system(size: 16, weight: .semibold, design: .rounded))
                        .widgetLabel {
                            if let prev = previous(s), prev < next.at {
                                ProgressView(timerInterval: prev...next.at, countsDown: false) { Text(name) } currentValueLabel: { Text(name) }
                            } else {
                                Text(name)
                            }
                        }
                default:
                    VStack(alignment: .leading, spacing: 1) {
                        Text(entry.name).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                        Text(verbatim: "\(name) \(time)").font(.headline).lineLimit(1)
                        Text(next.at, style: .timer).font(.body.monospacedDigit())
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                Text(entry.name)
            }
        }
        .containerBackground(for: .widget) { Color.clear }
    }

    /// Önceki vakit (dün akşamdan bugüne)
    private func previous(_ s: PrayerSchedule) -> Date? {
        (s.entries(on: entry.date.addingTimeInterval(-86_400)) + s.entries(on: entry.date)).last { $0.at <= entry.date }?.at
    }

    /// Ramazan'da akşam "İftar", imsak "Sahur"
    private func label(_ e: PrayerSchedule.Entry, zone: TimeZone) -> String {
        let ramadan = Hijri.date(e.at, timeZone: zone)?.month == 9
        if ramadan && e.prayer == .maghrib { return L("live.iftar") }
        if ramadan && e.prayer == .fajr { return L("live.sahur") }
        return e.prayer.localizedName
    }
}

// MARK: Bugünün vakitleri (büyük dikdörtgen; 2. analiz #173)

struct WatchTodayPrayersWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "watch-today-prayers", intent: ProvinceIntent.self, provider: WatchPrayerProvider()) { entry in
            WatchTodayPrayersView(entry: entry)
        }
        .configurationDisplayName(L("prayer.today"))
        .description(L("widget.today.desc"))
        .supportedFamilies([.accessoryRectangular])
    }
}

/// Altı vakit üç satırda iki sütun; sıradaki vakit kalın
struct WatchTodayPrayersView: View {
    let entry: WatchPrayerEntry

    var body: some View {
        let s = entry.schedule
        let today = s.entries(on: entry.date)
        let next = s.next(after: entry.date)?.at
        Grid(alignment: .leading, horizontalSpacing: 8, verticalSpacing: 0) {
            ForEach(0..<3, id: \.self) { row in
                GridRow {
                    ForEach(0..<2, id: \.self) { col in
                        let i = row * 2 + col
                        if i < today.count {
                            let e = today[i]
                            HStack(spacing: 3) {
                                Text(e.prayer.localizedName).lineLimit(1).minimumScaleFactor(0.6)
                                Spacer(minLength: 2)
                                Text(s.clock(e.at)).monospacedDigit()
                            }
                            .font(.system(size: 13, weight: e.at == next ? .bold : .regular))
                            .widgetAccentable(e.at == next)
                        }
                    }
                }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .containerBackground(for: .widget) { Color.clear }
    }
}

// MARK: Hicri tarih

struct WatchDayEntry: TimelineEntry {
    let date: Date
}

/// Şu an ve sonraki 7 gece yarısı
struct WatchDayProvider: TimelineProvider {
    func placeholder(in context: Context) -> WatchDayEntry { WatchDayEntry(date: Date()) }

    func getSnapshot(in context: Context, completion: @escaping (WatchDayEntry) -> Void) {
        completion(WatchDayEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WatchDayEntry>) -> Void) {
        let cal = Calendar.current, midnight = cal.startOfDay(for: Date())
        let days = (1...7).compactMap { cal.date(byAdding: .day, value: $0, to: midnight) }
        completion(Timeline(entries: [WatchDayEntry(date: Date())] + days.map(WatchDayEntry.init), policy: .atEnd))
    }
}

struct WatchHijriWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "watch-hijri", provider: WatchDayProvider()) { entry in
            WatchHijriView(entry: entry)
        }
        .configurationDisplayName(L("widget.hijri.name"))
        .description(L("widget.hijri.desc"))
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

struct WatchHijriView: View {
    @Environment(\.widgetFamily) private var family
    let entry: WatchDayEntry

    var body: some View {
        Group {
            if let h = Hijri.date(entry.date) {
                switch family {
                case .accessoryInline:
                    Text(verbatim: "☾ " + h.text)
                case .accessoryCircular:
                    VStack(spacing: 0) {
                        Text(verbatim: "\(h.day)").font(.system(size: 18, weight: .bold, design: .rounded))
                        Text(h.monthName).font(.system(size: 9, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.5)
                    }
                case .accessoryCorner:
                    Text(verbatim: "\(h.day)").font(.system(size: 18, weight: .bold, design: .rounded))
                        .widgetLabel { Text(h.monthName) }
                default:
                    VStack(alignment: .leading, spacing: 1) {
                        Text(L("hijri.title")).font(.caption2).foregroundStyle(.secondary)
                        Text(h.text).font(.headline).lineLimit(1).minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                Text(verbatim: "☾")
            }
        }
        .containerBackground(for: .widget) { Color.clear }
    }
}


// MARK: Hava (2. analiz #115)

/// Seçili yerin anlık sıcaklığı ve durumu: Open-Meteo (anahtarsız; koordinat 2 ondalık). Saat uzantısı App Group
/// olmadan iPhone'daki WeatherKit verisini okuyamadığı için kendisi sorar; 30 dk'da bir yenilenir. Atıf saat uygulamasında.
struct WatchWeatherEntry: TimelineEntry {
    let date: Date
    let name: String
    let temp: Int?
    let code: Int
}

enum WatchWeather {
    static func fetch(lat: Double, lon: Double) async -> (Int, Int)? {
        let url = String(format: "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code&timezone=auto", lat, lon)
        guard let u = URL(string: url), let (data, _) = try? await URLSession.shared.data(from: u),
              let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let c = o["current"] as? [String: Any], let t = c["temperature_2m"] as? Double else { return nil }
        return (Int(t.rounded()), (c["weather_code"] as? Int) ?? 2)
    }

    /// WMO kodu → SF Symbol
    static func symbol(_ c: Int) -> String {
        switch c {
        case 0: return "sun.max.fill"
        case 1, 2: return "cloud.sun.fill"
        case 3: return "cloud.fill"
        case 45, 48: return "cloud.fog.fill"
        case 51...67, 80...82: return "cloud.rain.fill"
        case 71...77, 85, 86: return "cloud.snow.fill"
        case 95...99: return "cloud.bolt.rain.fill"
        default: return "cloud.sun.fill"
        }
    }

    static func key(_ c: Int) -> String {
        switch c {
        case 0: return "clear"
        case 1: return "mostlyClear"
        case 2: return "partly"
        case 3: return "overcast"
        case 45, 48: return "fog"
        case 51...55: return "drizzle"
        case 56, 57: return "freezingDrizzle"
        case 61...65: return "rain"
        case 66, 67: return "freezingRain"
        case 71...75: return "snow"
        case 77: return "snowGrains"
        case 80...82: return "showers"
        case 85, 86: return "snowShowers"
        case 95: return "thunder"
        case 96, 99: return "thunderHail"
        default: return "partly"
        }
    }
}

struct WatchWeatherProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> WatchWeatherEntry {
        WatchWeatherEntry(date: Date(), name: Province.find("istanbul").displayName, temp: 18, code: 2)
    }

    func snapshot(for configuration: ProvinceIntent, in context: Context) async -> WatchWeatherEntry {
        await entry(configuration)
    }

    func timeline(for configuration: ProvinceIntent, in context: Context) async -> Timeline<WatchWeatherEntry> {
        let e = await entry(configuration)
        return Timeline(entries: [e], policy: .after(Date().addingTimeInterval(1800)))
    }

    private func entry(_ configuration: ProvinceIntent) async -> WatchWeatherEntry {
        let place = await configuration.resolvePlace()
        let w = await WatchWeather.fetch(lat: place.schedule.latitude, lon: place.schedule.longitude)
        return WatchWeatherEntry(date: Date(), name: place.name, temp: w?.0, code: w?.1 ?? 2)
    }

    func recommendations() -> [AppIntentRecommendation<ProvinceIntent>] {
        ProvinceEntity.all.map { e in
            var i = ProvinceIntent()
            i.province = e
            return AppIntentRecommendation(intent: i, description: e.name)
        }
    }
}

struct WatchWeatherWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "watch-weather", intent: ProvinceIntent.self, provider: WatchWeatherProvider()) { entry in
            WatchWeatherView(entry: entry)
        }
        .configurationDisplayName(L("widget.weather.name"))
        .description(L("widget.weather.desc"))
        .supportedFamilies([.accessoryCircular, .accessoryInline, .accessoryCorner, .accessoryRectangular])
    }
}

struct WatchWeatherView: View {
    @Environment(\.widgetFamily) private var family
    let entry: WatchWeatherEntry

    var body: some View {
        let temp = entry.temp.map { "\($0)°" } ?? "–"
        let label = L("weather.codes.\(WatchWeather.key(entry.code))")
        Group {
            switch family {
            case .accessoryInline:
                Label(temp + " " + label, systemImage: WatchWeather.symbol(entry.code))
            case .accessoryCircular:
                VStack(spacing: 0) {
                    Image(systemName: WatchWeather.symbol(entry.code)).font(.system(size: 14))
                    Text(temp).font(.system(size: 16, weight: .bold, design: .rounded))
                }
                .widgetAccentable()
            case .accessoryCorner:
                Image(systemName: WatchWeather.symbol(entry.code)).font(.system(size: 18))
                    .widgetLabel { Text(temp) }
            default:
                HStack(spacing: 6) {
                    Image(systemName: WatchWeather.symbol(entry.code)).font(.title3)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(entry.name).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                        Text(temp).font(.headline)
                        Text(label).font(.caption2).lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .containerBackground(for: .widget) { Color.clear }
    }
}
