import SwiftUI
import WidgetKit
import SaatCore

struct PrayerEntry: TimelineEntry {
    let date: Date
    let placeName: String
    let schedule: PrayerSchedule
    let today: [PrayerSchedule.Entry]
    let next: PrayerSchedule.Entry?
    /// Önceki vakit (sıradaki vakte kalan süre halkası için; 2. analiz #162)
    var prev: Date? = nil

    /// Önceki vakitten sıradakine geçen sürenin oranı (0–1)
    var progress: Double {
        guard let next, let prev, next.at > prev else { return 0 }
        return min(1, max(0, date.timeIntervalSince(prev) / next.at.timeIntervalSince(prev)))
    }
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
        // Kilit ekranındaki dairesel halka ilerlesin diye önümüzdeki 4 saatte 10 dakikada bir kayıt (2. analiz #162)
        moments += (1...24).map { now.addingTimeInterval(Double($0) * 600) }
        let entries = Set(moments).sorted().prefix(40).map { entry(at: $0, place: place) }
        return Timeline(entries: entries, policy: .atEnd)
    }

    private func entry(at date: Date, place: Place) -> PrayerEntry {
        let yesterday = date.addingTimeInterval(-86_400)
        let prev = (place.schedule.entries(on: yesterday) + place.schedule.entries(on: date)).last { $0.at <= date }?.at
        return PrayerEntry(date: date, placeName: place.name, schedule: place.schedule,
                           today: place.schedule.entries(on: date), next: place.schedule.next(after: date), prev: prev)
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
        .supportedFamilies([.systemSmall, .accessoryRectangular, .accessoryInline, .accessoryCircular])
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
                    Text(verbatim: "\(next.prayer.ramadanName(at: next.at, zone: entry.schedule.timeZone)) \(entry.schedule.clock(next.at))")
                case .accessoryCircular:
                    // Kilit ekranı dairesel (2. analiz #14, #162): önceki vakitten sıradakine dolan halka, ortada saat
                    Gauge(value: entry.progress) {
                        Text(next.prayer.ramadanName(at: next.at, zone: entry.schedule.timeZone))
                    } currentValueLabel: {
                        Text(entry.schedule.clock(next.at))
                            .font(.system(size: 12, weight: .semibold).monospacedDigit())
                            .minimumScaleFactor(0.6)
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    .gaugeStyle(.accessoryCircular)
                    .widgetAccentable()
                case .accessoryRectangular:
                    VStack(alignment: .leading, spacing: 1) {
                        Text(entry.placeName).font(.caption2).foregroundStyle(.secondary)
                        Text(verbatim: "\(next.prayer.ramadanName(at: next.at, zone: entry.schedule.timeZone)) \(entry.schedule.clock(next.at))").font(.headline)
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
            Text(next.prayer.ramadanName(at: next.at, zone: entry.schedule.timeZone))
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
                .widgetAccentable()
            // Sıradaki vakte kalan süre çubuğu: kendiliğinden ilerler (2. analiz #162)
            if let prev = entry.prev, prev < next.at {
                ProgressView(timerInterval: prev...next.at, countsDown: false, label: { EmptyView() }, currentValueLabel: { EmptyView() })
                    .progressViewStyle(.linear)
                    .tint(Theme.accent)
                    .environment(\.layoutDirection, .leftToRight)
            }
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
            DayProgressBar(entry: entry)
        }
        .containerBackground(for: .widget) { Theme.bg }
    }
}

/// Günün ilerlemesi (2. analiz #163): vakitler arası beş parça; geçenler dolu, içinde bulunulan parça kendiliğinden dolar
struct DayProgressBar: View {
    let entry: PrayerEntry

    var body: some View {
        let t = entry.today
        HStack(spacing: 4) {
            ForEach(0..<max(0, t.count - 1), id: \.self) { i in
                let start = t[i].at, end = t[i + 1].at
                if entry.date >= end {
                    Capsule().fill(Theme.accent).frame(height: 4)
                } else if entry.date < start || start >= end {
                    Capsule().fill(Theme.accentSoft).frame(height: 4)
                } else {
                    ProgressView(timerInterval: start...end, countsDown: false, label: { EmptyView() }, currentValueLabel: { EmptyView() })
                        .progressViewStyle(.linear)
                        .tint(Theme.accent)
                }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .widgetAccentable()
        .accessibilityHidden(true)
    }
}
