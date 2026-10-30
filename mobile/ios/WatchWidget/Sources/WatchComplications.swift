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
        WatchHijriWidget()
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
            let i = ProvinceIntent()
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
                    VStack(spacing: 0) {
                        Text(name).font(.system(size: 10, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
                        Text(time).font(.system(size: 15, weight: .bold, design: .rounded)).minimumScaleFactor(0.7)
                    }
                case .accessoryCorner:
                    Text(time).font(.system(size: 16, weight: .semibold, design: .rounded))
                        .widgetLabel { Text(name) }
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

    /// Ramazan'da akşam "İftar", imsak "Sahur"
    private func label(_ e: PrayerSchedule.Entry, zone: TimeZone) -> String {
        let ramadan = Hijri.date(e.at, timeZone: zone)?.month == 9
        if ramadan && e.prayer == .maghrib { return L("live.iftar") }
        if ramadan && e.prayer == .fajr { return L("live.sahur") }
        return e.prayer.localizedName
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
