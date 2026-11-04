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

struct MoonWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "moon-phase", provider: MoonProvider()) { entry in
            MoonWidgetView(entry: entry)
                .foregroundStyle(Theme.text)
                .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.moon.name"))
        .description(L("widget.moon.desc"))
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

/// Küçük: bugünkü evre; orta: + sıradaki dört ana evre; büyük: + bu ayın evre takvimi (2. analiz #149). Dokulu Ay (NASA, #103).
private struct MoonWidgetView: View {
    let entry: MoonEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let m = Nature.moon(at: entry.date)
        switch family {
        case .systemSmall: today(m, size: 56)
        case .systemMedium:
            HStack(spacing: 12) {
                today(m, size: 60).frame(maxWidth: .infinity)
                nextPhases.frame(maxWidth: .infinity)
            }
        default:
            VStack(spacing: 10) {
                HStack(spacing: 12) {
                    today(m, size: 60).frame(maxWidth: .infinity)
                    nextPhases.frame(maxWidth: .infinity)
                }
                monthGrid
            }
        }
    }

    private func today(_ m: Nature.Moon, size: CGFloat) -> some View {
        let full = m.nextFullMoon < m.nextNewMoon
        let next = Date(unixMilliseconds: full ? m.nextFullMoon : m.nextNewMoon)
        return VStack(spacing: 3) {
            MoonImage(fraction: m.fraction, size: size)
            Text(L("nature.phases.\(phaseKeys[m.phaseIndex])")).font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
            Text(String(format: L("nature.illumination"), "\(Int((m.illumination * 100).rounded()))")).font(.caption).foregroundStyle(Theme.muted)
            Text((full ? "🌕 " : "🌑 ") + dayMonth(next)).font(.caption.weight(.semibold)).foregroundStyle(Theme.accent)
        }
    }

    private var nextPhases: some View {
        let list = MoonPhases.next(from: entry.date)
        return VStack(alignment: .leading, spacing: 4) {
            ForEach(list.indices, id: \.self) { i in
                HStack(spacing: 6) {
                    MoonImage(fraction: Self.fraction(list[i].key), size: 18)
                    Text(L("nature.phases.\(list[i].key)")).font(.caption2).lineLimit(1).minimumScaleFactor(0.7)
                    Spacer(minLength: 2)
                    Text(dayMonth(list[i].date)).font(.caption2.weight(.semibold)).monospacedDigit()
                }
            }
        }
    }

    private static func fraction(_ key: String) -> Double {
        switch key {
        case "firstQuarter": return 0.25
        case "full": return 0.5
        case "lastQuarter": return 0.75
        default: return 0
        }
    }

    private var monthGrid: some View {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = .current
        let comps = cal.dateComponents([.year, .month], from: entry.date)
        let first = cal.date(from: comps) ?? entry.date
        let count = cal.range(of: .day, in: .month, for: first)?.count ?? 30
        let lead = (cal.component(.weekday, from: first) + 5) % 7
        let today = cal.component(.day, from: entry.date)
        let cols = Array(repeating: GridItem(.flexible(), spacing: 2), count: 7)
        return LazyVGrid(columns: cols, spacing: 3) {
            ForEach(0..<(lead + count), id: \.self) { i in
                if i < lead {
                    Color.clear.frame(height: 24)
                } else {
                    let d = i - lead + 1
                    let noon = first.addingTimeInterval(Double(d - 1) * 86_400 + 12 * 3600)
                    VStack(spacing: 0) {
                        MoonImage(fraction: Nature.moon(at: noon).fraction, size: 16)
                        Text("\(d)").font(.system(size: 8, weight: d == today ? .bold : .regular)).foregroundStyle(d == today ? Theme.accent : Theme.muted)
                    }
                }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }
}
