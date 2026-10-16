import SwiftUI
import WidgetKit
import SaatCore

struct PrayerEntry: TimelineEntry {
    let date: Date
    let placeName: String
    let schedule: PrayerSchedule
    let today: [PrayerSchedule.Entry]
    let next: PrayerSchedule.Entry?
}

/// Zaman çizelgesi: şimdi, bugünkü ve yarınki her vakit anı ve gece yarısı; sonra yeniden hesaplanır.
struct PrayerProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry {
        entry(at: Date(), place: Place(province: Province.find("istanbul")))
    }

    func snapshot(for configuration: ProvinceIntent, in context: Context) async -> PrayerEntry {
        entry(at: Date(), place: await configuration.resolvePlace())
    }

    func timeline(for configuration: ProvinceIntent, in context: Context) async -> Timeline<PrayerEntry> {
        let place = await configuration.resolvePlace()
        let now = Date()
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.schedule.timeZone
        let tomorrow = cal.date(byAdding: .day, value: 1, to: now) ?? now
        var moments = [now]
        moments += (place.schedule.entries(on: now) + place.schedule.entries(on: tomorrow)).map(\.at).filter { $0 > now }
        moments.append(cal.startOfDay(for: tomorrow))
        let entries = Set(moments).sorted().prefix(14).map { entry(at: $0, place: place) }
        return Timeline(entries: entries, policy: .atEnd)
    }

    private func entry(at date: Date, place: Place) -> PrayerEntry {
        PrayerEntry(date: date, placeName: place.name, schedule: place.schedule,
                    today: place.schedule.entries(on: date), next: place.schedule.next(after: date))
    }
}

// MARK: Sıradaki vakit (küçük + kilit ekranı)

struct NextPrayerWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "next-prayer", intent: ProvinceIntent.self, provider: PrayerProvider()) { entry in
            NextPrayerView(entry: entry)
        }
        .configurationDisplayName(L("widget.next.name"))
        .description(L("widget.next.desc"))
        .supportedFamilies([.systemSmall, .accessoryRectangular, .accessoryInline])
    }
}

struct NextPrayerView: View {
    @Environment(\.widgetFamily) private var family
    let entry: PrayerEntry

    var body: some View {
        Group {
            if let next = entry.next {
                switch family {
                case .accessoryInline:
                    Text(verbatim: "\(next.prayer.localizedName) \(entry.schedule.clock(next.at))")
                case .accessoryRectangular:
                    VStack(alignment: .leading, spacing: 1) {
                        Text(entry.placeName).font(.caption2).foregroundStyle(.secondary)
                        Text(verbatim: "\(next.prayer.localizedName) \(entry.schedule.clock(next.at))").font(.headline)
                        Text(next.at, style: .timer).font(.body.monospacedDigit())
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                default:
                    small(next)
                }
            } else {
                Text(entry.placeName)
            }
        }
        .containerBackground(for: .widget) { Theme.bg }
    }

    private func small(_ next: PrayerSchedule.Entry) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Label(entry.placeName, systemImage: "mappin.and.ellipse")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.accent)
                .lineLimit(1)
            Spacer(minLength: 0)
            Text(next.prayer.localizedName)
                .font(.headline)
                .foregroundStyle(Theme.text)
            Text(entry.schedule.clock(next.at))
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.text)
                .environment(\.layoutDirection, .leftToRight)
            Text(next.at, style: .timer)
                .font(.subheadline.weight(.semibold).monospacedDigit())
                .foregroundStyle(Theme.accent)
                .environment(\.layoutDirection, .leftToRight)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: Bugünün vakitleri (geniş)

struct TodayPrayersWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "today-prayers", intent: ProvinceIntent.self, provider: PrayerProvider()) { entry in
            TodayPrayersView(entry: entry)
        }
        .configurationDisplayName(L("prayer.today"))
        .description(L("widget.today.desc"))
        .supportedFamilies([.systemMedium])
    }
}

struct TodayPrayersView: View {
    let entry: PrayerEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Label(entry.placeName, systemImage: "mappin.and.ellipse")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.accent)
                    .lineLimit(1)
                Spacer()
                if let next = entry.next {
                    Text(next.at, style: .timer)
                        .font(.caption.weight(.semibold).monospacedDigit())
                        .foregroundStyle(Theme.accent)
                        .multilineTextAlignment(.trailing)
                        .environment(\.layoutDirection, .leftToRight)
                }
            }
            HStack(spacing: 4) {
                ForEach(entry.today) { e in
                    let isNext = e.at == entry.next?.at
                    VStack(spacing: 4) {
                        Text(e.prayer.localizedName)
                            .font(.caption2.weight(isNext ? .bold : .regular))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Text(entry.schedule.clock(e.at))
                            .font(.callout.weight(isNext ? .bold : .medium).monospacedDigit())
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    .foregroundStyle(Theme.text)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(isNext ? Theme.accentSoft : .clear, in: RoundedRectangle(cornerRadius: 8))
                }
            }
        }
        .containerBackground(for: .widget) { Theme.bg }
    }
}
