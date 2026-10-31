import SwiftUI
import WidgetKit
import SaatCore

/// Ay evresi widget'ı: bugünkü evre, aydınlık yüzdesi, sıradaki dolunay ya da yeni ay. Hesap telefonda (SaatCore), konumsuz.
struct MoonEntry: TimelineEntry {
    let date: Date
}

/// Altı saatte bir kayıt (iki gün); evre yavaş değişir
struct MoonProvider: TimelineProvider {
    func placeholder(in context: Context) -> MoonEntry { MoonEntry(date: Date()) }

    func getSnapshot(in context: Context, completion: @escaping (MoonEntry) -> Void) {
        completion(MoonEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<MoonEntry>) -> Void) {
        let now = Date()
        completion(Timeline(entries: (0..<8).map { MoonEntry(date: now.addingTimeInterval(Double($0) * 6 * 3600)) }, policy: .atEnd))
    }
}

/// "9 Kas" (her bölgede Gregoryen takvimle)
private func dayMonth(_ d: Date) -> String {
    var style = Date.FormatStyle.dateTime.day().month(.abbreviated)
    style.calendar = Calendar(identifier: .gregorian)
    return d.formatted(style)
}

private let phaseKeys = ["new", "waxingCrescent", "firstQuarter", "waxingGibbous", "full", "waningGibbous", "lastQuarter", "waningCrescent"]
private let phaseEmoji = ["🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"]

struct MoonWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "moon-phase", provider: MoonProvider()) { entry in
            let m = Nature.moon(at: entry.date)
            let full = m.nextFullMoon < m.nextNewMoon
            let next = Date(unixMilliseconds: full ? m.nextFullMoon : m.nextNewMoon)
            VStack(spacing: 4) {
                Text(phaseEmoji[m.phaseIndex]).font(.system(size: 44))
                Text(L("nature.phases.\(phaseKeys[m.phaseIndex])"))
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(String(format: L("nature.illumination"), "\(Int((m.illumination * 100).rounded()))"))
                    .font(.caption)
                    .foregroundStyle(Theme.muted)
                Text((full ? "🌕 " : "🌑 ") + dayMonth(next))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.accent)
            }
            .foregroundStyle(Theme.text)
            .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.moon.name"))
        .description(L("widget.moon.desc"))
        .supportedFamilies([.systemSmall])
    }
}
