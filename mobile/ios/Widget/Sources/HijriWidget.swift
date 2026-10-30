import SwiftUI
import WidgetKit

/// Hicri tarih widget'ı (rakip analizi #9): bugünün hicri tarihi (Diyanet takvimi, Hijri.swift) ve miladi tarih.
/// Ana ekranda küçük, kilit ekranında satır ve dikdörtgen. Konumsuz; her gece yarısı yeni kayıt.
struct HijriEntry: TimelineEntry {
    let date: Date
}

/// Şu an ve sonraki 7 gece yarısı
struct HijriProvider: TimelineProvider {
    func placeholder(in context: Context) -> HijriEntry { HijriEntry(date: Date()) }

    func getSnapshot(in context: Context, completion: @escaping (HijriEntry) -> Void) {
        completion(HijriEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HijriEntry>) -> Void) {
        let now = Date(), cal = Calendar.current
        let midnight = cal.startOfDay(for: now)
        let days = (1...7).compactMap { cal.date(byAdding: .day, value: $0, to: midnight) }
        completion(Timeline(entries: [HijriEntry(date: now)] + days.map(HijriEntry.init), policy: .atEnd))
    }
}

/// "30 Ekim 2026 Cuma" (her bölgede Gregoryen takvimle)
private func gregorianDate(_ d: Date, weekday: Bool) -> String {
    var style = Date.FormatStyle.dateTime.day().month(.wide).year()
    if weekday { style = style.weekday(.wide) }
    style.calendar = Calendar(identifier: .gregorian)
    return d.formatted(style)
}

struct HijriWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "hijri-date", provider: HijriProvider()) { entry in
            HijriWidgetView(entry: entry)
        }
        .configurationDisplayName(L("widget.hijri.name"))
        .description(L("widget.hijri.desc"))
        .supportedFamilies([.systemSmall, .accessoryInline, .accessoryRectangular])
    }
}

struct HijriWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: HijriEntry

    var body: some View {
        let h = Hijri.date(entry.date)
        Group {
            switch family {
            case .accessoryInline:
                Text(verbatim: "☾ " + (h?.text ?? gregorianDate(entry.date, weekday: false)))
            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 1) {
                    Text(L("hijri.title")).font(.caption2).foregroundStyle(.secondary)
                    Text(h?.text ?? "–").font(.headline).lineLimit(1).minimumScaleFactor(0.7)
                    Text(gregorianDate(entry.date, weekday: false)).font(.caption).lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            default:
                VStack(spacing: 2) {
                    Image(systemName: "moon.stars")
                        .font(.subheadline)
                        .foregroundStyle(Theme.accent)
                    if let h {
                        Text(verbatim: "\(h.day)")
                            .font(.system(size: 44, weight: .bold, design: .rounded))
                            .foregroundStyle(Theme.text)
                            .environment(\.layoutDirection, .leftToRight)
                        Text(h.monthName)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Theme.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Text(verbatim: "\(h.year)")
                            .font(.caption)
                            .foregroundStyle(Theme.muted)
                    }
                    Spacer(minLength: 0)
                    Text(gregorianDate(entry.date, weekday: true))
                        .font(.caption2)
                        .foregroundStyle(Theme.muted)
                        .lineLimit(2)
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.7)
                }
            }
        }
        .containerBackground(for: .widget) { Theme.bg }
    }
}
