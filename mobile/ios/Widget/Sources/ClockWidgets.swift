import SwiftUI
import WidgetKit

/// Saat widget'ları konumsuz çalışır. Saniye yok (widget'lar saniyede yenilenemez).
struct ClockEntry: TimelineEntry {
    let date: Date
}

/// Her dakikaya bir kayıt (2 saat); bitince yeniden kurulur.
struct ClockProvider: TimelineProvider {
    func placeholder(in context: Context) -> ClockEntry { ClockEntry(date: Date()) }

    func getSnapshot(in context: Context, completion: @escaping (ClockEntry) -> Void) {
        completion(ClockEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ClockEntry>) -> Void) {
        let now = Date()
        let start = Calendar.current.dateInterval(of: .minute, for: now)?.start ?? now
        let entries = (0..<120).map { ClockEntry(date: start.addingTimeInterval(Double($0) * 60)) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

/// 24 saat, her dilde Latin rakamı (uygulama ve sitedeki gibi)
private let hourMinute: DateFormatter = {
    let f = DateFormatter()
    f.locale = Locale(identifier: "en_US_POSIX")
    f.dateFormat = "HH:mm"
    return f
}()

struct AnalogClockWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "analog-clock", provider: ClockProvider()) { entry in
            AnalogClock(date: entry.date)
                .padding(4)
                .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.analog.name"))
        .description(L("widget.analog.desc"))
        .supportedFamilies([.systemSmall, .systemLarge])
    }
}

struct DigitalClockWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "digital-clock", provider: ClockProvider()) { entry in
            Text(hourMinute.string(from: entry.date))
                .font(.system(size: 44, weight: .bold, design: .rounded))
                .monospacedDigit()
                .minimumScaleFactor(0.5)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .foregroundStyle(Theme.text)
                .environment(\.layoutDirection, .leftToRight)
                .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.digital.name"))
        .description(L("widget.digital.desc"))
        .supportedFamilies([.systemSmall])
    }
}

struct ClockDateWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "clock-date", provider: ClockProvider()) { entry in
            VStack(spacing: 6) {
                Text(hourMinute.string(from: entry.date))
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
                    .multilineTextAlignment(.center)
                    .environment(\.layoutDirection, .leftToRight)
                Text(entry.date.formatted(.dateTime.day().month(.wide)))
                    .font(.headline)
                Text(entry.date.formatted(.dateTime.weekday(.wide).year()))
                    .font(.subheadline)
                    .foregroundStyle(Theme.muted)
            }
            .foregroundStyle(Theme.text)
            .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.clockDate.name"))
        .description(L("widget.clockDate.desc"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
