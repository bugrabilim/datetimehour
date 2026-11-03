import SwiftUI
import WidgetKit
import AppIntents
import SaatCore

/* 2. analiz K-4 widget'ları (3 Kasım 2026): yıl ilerlemesi (#165), yazıyla tarih (#172), gün doğumu/batımı (#15) ve
   günün içeriği: ayet, Esma, hadis, söz (#16). Hepsi telefonda hesaplanır, internet gerekmez. */

/// Widget'ın dili (uygulamanın yerelleştirmesi; Basitleştirilmiş Çince "zh")
private let widgetLang: String = {
    let c = Bundle.main.preferredLocalizations.first ?? "tr"
    return c.hasPrefix("zh") ? "zh" : c
}()

private let widgetLocale = Locale(identifier: widgetLang == "zh" ? "zh-Hans" : widgetLang == "th" ? "th_TH@calendar=gregorian" : widgetLang)

private var gregorianCal: Calendar {
    var c = Calendar(identifier: .gregorian)
    c.locale = widgetLocale
    return c
}

/// Yüzde dilin yazımıyla (Türkçe "%84", Almanca/Fransızca/Rusça "84 %", diğerleri "84%"); Latin rakam
private func percentText(_ n: Int) -> String {
    switch widgetLang {
    case "tr": "%\(n)"
    case "de", "fr", "ru": "\(n) %"
    default: "\(n)%"
    }
}

/// Gece yarısına kadar tek kayıt
private struct DayProvider: TimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry { SimpleEntry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> Void) { completion(SimpleEntry(date: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<SimpleEntry>) -> Void) {
        let now = Date()
        let midnight = Calendar.current.startOfDay(for: now).addingTimeInterval(86_400 + 5)
        completion(Timeline(entries: [SimpleEntry(date: now)], policy: .after(midnight)))
    }
}

struct SimpleEntry: TimelineEntry {
    let date: Date
}

// MARK: Yıl ilerlemesi (#165)

struct YearProgressWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "year-progress", provider: DayProvider()) { entry in
            YearProgressView(date: entry.date)
        }
        .configurationDisplayName(L("widget.year.name"))
        .description(L("widget.year.desc"))
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular])
    }
}

struct YearProgressView: View {
    @Environment(\.widgetFamily) private var family
    let date: Date

    var body: some View {
        let cal = Calendar.current
        let year = cal.component(.year, from: date)
        let day = cal.ordinality(of: .day, in: .year, for: date) ?? 1
        let total = cal.range(of: .day, in: .year, for: date)?.count ?? 365
        let pct = Int((Double(day) / Double(total) * 100).rounded(.down))
        Group {
            switch family {
            case .accessoryCircular:
                Gauge(value: Double(day), in: 0...Double(total)) {
                    Text(verbatim: "\(year)")
                } currentValueLabel: {
                    Text(verbatim: percentText(pct)).font(.system(size: 13, weight: .semibold))
                }
                .gaugeStyle(.accessoryCircular)
                .widgetAccentable()
            case .accessoryRectangular:
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: "\(year) · \(day)/\(total)").font(.headline)
                    ProgressView(value: Double(day), total: Double(total)).widgetAccentable()
                    Text(String(format: L("client.daysLeft"), "\(total - day)")).font(.caption)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            default:
                HStack(spacing: 14) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(verbatim: "\(year)").font(.caption.weight(.semibold)).foregroundStyle(Theme.accent).widgetAccentable()
                        Text(verbatim: "\(day)/\(total)")
                            .font(.system(size: family == .systemSmall ? 26 : 34, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(Theme.text)
                            .minimumScaleFactor(0.6)
                            .lineLimit(1)
                        Text(verbatim: percentText(pct)).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accent)
                        if family != .systemSmall {
                            Text(String(format: L("client.daysLeft"), "\(total - day)")).font(.caption).foregroundStyle(Theme.muted)
                        }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                    if family != .systemSmall {
                        YearDots(day: day, total: total, cols: 30)
                    }
                }
                .overlay(alignment: .bottom) {
                    if family == .systemSmall { YearDots(day: day, total: total, cols: 25).frame(height: 62).offset(y: 6) }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
        }
        .containerBackground(for: .widget) { Theme.bg }
    }
}

/// 365 (artık yılda 366) nokta: geçen günler dolu
struct YearDots: View {
    let day: Int
    let total: Int
    let cols: Int

    var body: some View {
        Canvas { ctx, size in
            let rows = Int((Double(total) / Double(cols)).rounded(.up))
            let cell = min(size.width / CGFloat(cols), size.height / CGFloat(rows))
            let r = cell * 0.34
            for i in 0..<total {
                let x = (CGFloat(i % cols) + 0.5) * cell
                let y = (CGFloat(i / cols) + 0.5) * cell
                let dot = Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2))
                ctx.fill(dot, with: .color(i < day ? Theme.accent : Theme.accentSoft))
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .widgetAccentable()
        .accessibilityHidden(true)
    }
}

// MARK: Yazıyla tarih (#172)

struct WordsDateWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "words-date", provider: ClockProvider()) { entry in
            WordsDateView(date: entry.date)
        }
        .configurationDisplayName(L("widget.words.name"))
        .description(L("widget.words.desc"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct WordsDateView: View {
    @Environment(\.widgetFamily) private var family
    let date: Date

    private static let spell: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .spellOut
        f.locale = widgetLocale
        return f
    }()

    private func words(_ n: Int) -> String { Self.spell.string(from: NSNumber(value: n)) ?? "\(n)" }

    var body: some View {
        let c = Calendar.current.dateComponents([.day, .hour, .minute], from: date)
        let weekday = date.formatted(Date.FormatStyle(calendar: gregorianCal).weekday(.wide).locale(widgetLocale))
        let month = date.formatted(Date.FormatStyle(calendar: gregorianCal).month(.wide).locale(widgetLocale))
        let hour = c.hour ?? 0, minute = c.minute ?? 0
        let time = minute == 0 ? words(hour) : words(hour) + " " + words(minute)
        VStack(alignment: .leading, spacing: family == .systemSmall ? 2 : 4) {
            Text(weekday).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accent).widgetAccentable()
            Text(words(c.day ?? 1))
                .font(.system(size: family == .systemSmall ? 24 : 32, weight: .heavy, design: .serif))
                .foregroundStyle(Theme.text)
                .minimumScaleFactor(0.5)
                .lineLimit(2)
            Text(month.uppercased(with: widgetLocale))
                .font(.system(size: family == .systemSmall ? 15 : 18, weight: .semibold))
                .tracking(2)
                .foregroundStyle(Theme.text)
            Spacer(minLength: 0)
            Text(time).font(.caption).foregroundStyle(Theme.muted).lineLimit(2).minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .containerBackground(for: .widget) { Theme.bg }
    }
}

// MARK: Gün doğumu ve batımı (#15)

struct SunEntry: TimelineEntry {
    let date: Date
    let placeName: String
    let rise: Date?
    let set: Date?
    let zone: TimeZone
    var latitude: Double = 41
}

/// Sıradaki mevsim başlangıcı (ekinoks ya da gündönümü; 2. analiz #150): mevsim anahtarı ve kalan gün; güney yarımkürede
/// adlar yer değiştirir (Android'deki SeasonTurn ile aynı)
enum SeasonTurn {
    static func next(lat: Double, now: Date) -> (String, Int) {
        let ms = Int64(now.timeIntervalSince1970 * 1000)
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "UTC")!
        let y = cal.component(.year, from: now)
        let names = lat < 0 ? ["autumn", "winter", "spring", "summer"] : ["spring", "summer", "autumn", "winter"]
        for yy in y...(y + 1) {
            let s = Nature.seasons(year: yy)
            for (i, t) in [s.march, s.june, s.september, s.december].enumerated() where t > ms {
                return (names[i], Int((Double(t - ms) / 86_400_000).rounded(.up)))
            }
        }
        return (names[0], 0)
    }
}

struct SunProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> SunEntry { entry(Date(), Place(province: Province.find("istanbul"))) }
    func snapshot(for c: ProvinceIntent, in context: Context) async -> SunEntry { entry(Date(), await c.resolvePlace()) }
    func timeline(for c: ProvinceIntent, in context: Context) async -> Timeline<SunEntry> {
        let place = await c.resolvePlace()
        let now = Date()
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.schedule.timeZone
        let next = cal.startOfDay(for: now).addingTimeInterval(86_400 + 5)
        return Timeline(entries: [entry(now, place)], policy: .after(next))
    }

    private func entry(_ date: Date, _ place: Place) -> SunEntry {
        let s = place.schedule
        var p = PrayerTimes.Parameters()
        p.adjustments.sunrise = 0
        p.adjustments.dhuhr = 0
        p.adjustments.maghrib = 0
        let d = CalendarDay(date, timeZone: s.timeZone)
        let t = PrayerTimes.times(year: d.year, month: d.month, day: d.day, latitude: s.latitude, longitude: s.longitude,
                                  timeZone: s.timeZone, parameters: p)
        let midnight = d.noon(in: s.timeZone).addingTimeInterval(-12 * 3600)
        return SunEntry(date: date, placeName: place.name,
                        rise: t.sunrise.flatMap { $0.isFinite ? midnight.addingTimeInterval($0 * 3600) : nil },
                        set: t.maghrib.flatMap { $0.isFinite ? midnight.addingTimeInterval($0 * 3600) : nil },
                        zone: s.timeZone, latitude: s.latitude)
    }
}

struct SunWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "sun", intent: ProvinceIntent.self, provider: SunProvider()) { entry in
            SunWidgetView(entry: entry)
        }
        .configurationDisplayName(L("widget.sun.name"))
        .description(L("widget.sun.desc"))
        .supportedFamilies([.systemSmall, .accessoryRectangular])
    }
}

struct SunWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SunEntry

    private func hm(_ d: Date?) -> String {
        guard let d else { return "—" }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        f.timeZone = entry.zone
        return f.string(from: d)
    }

    var body: some View {
        let len: String = {
            guard let r = entry.rise, let s = entry.set, s > r else { return "—" }
            let m = Int(s.timeIntervalSince(r) / 60)
            return String(format: L("seasons.dayLengthValue"), "\(m / 60)", "\(m % 60)")
        }()
        Group {
            if family == .accessoryRectangular {
                VStack(alignment: .leading, spacing: 1) {
                    Text(entry.placeName).font(.caption2).foregroundStyle(.secondary)
                    Text(verbatim: "↑ \(hm(entry.rise))   ↓ \(hm(entry.set))").font(.headline).monospacedDigit()
                    Text(len).font(.caption)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    Label(entry.placeName, systemImage: "mappin.and.ellipse")
                        .font(.caption.weight(.semibold)).foregroundStyle(Theme.accent).lineLimit(1).widgetAccentable()
                    Spacer(minLength: 0)
                    sunRow("sunrise", L("nature.sunrise"), hm(entry.rise))
                    sunRow("sunset", L("nature.sunset"), hm(entry.set))
                    Text(L("nature.dayLength") + ": " + len).font(.caption2).foregroundStyle(Theme.muted).lineLimit(1).minimumScaleFactor(0.7)
                    let turn = SeasonTurn.next(lat: entry.latitude, now: entry.date)
                    Text(String(format: L("widget.sun.turnIn"), L("nature.seasons.\(turn.0)"), "\(turn.1)"))
                        .font(.caption2.weight(.semibold)).foregroundStyle(Theme.accent).lineLimit(1).minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .containerBackground(for: .widget) { Theme.bg }
    }

    private func sunRow(_ icon: String, _ title: String, _ value: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon).foregroundStyle(Theme.accent).widgetAccentable()
            VStack(alignment: .leading, spacing: 0) {
                Text(title).font(.caption2).foregroundStyle(Theme.muted).lineLimit(1)
                Text(verbatim: value).font(.title3.weight(.bold).monospacedDigit()).foregroundStyle(Theme.text)
                    .environment(\.layoutDirection, .leftToRight)
            }
        }
    }
}

// MARK: Günün içeriği: ayet, Esma, hadis, söz (#16)

enum DailyKind: String, AppEnum {
    case saying, verse, esma, hadith

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "widget.daily.kind"
    static let caseDisplayRepresentations: [DailyKind: DisplayRepresentation] = [
        .saying: "widget.daily.saying", .verse: "widget.daily.verse", .esma: "widget.daily.esma", .hadith: "hadith.today",
    ]
}

struct DailyIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "widget.daily.name"
    static var description = IntentDescription("widget.daily.desc")

    @Parameter(title: "widget.daily.kind", default: .saying) var kind: DailyKind
}

struct DailyEntry: TimelineEntry {
    let date: Date
    let kind: DailyKind
    let title: String
    let arabic: String
    let text: String
    let source: String
}

/// Günün içeriği: yılın gününe göre sırayla (uygulamadaki günün hadisi ve sözüyle aynı sıra)
enum DailyContent {
    private static func json<T: Decodable>(_ name: String, _ type: T.Type) -> T? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json"), let d = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(type, from: d)
    }

    private static func pick<T>(_ list: [T], _ day: CalendarDay, step: Int = 1) -> T? {
        list.isEmpty ? nil : list[((day.dayNumber * step) % list.count + list.count) % list.count]
    }

    static func entry(_ kind: DailyKind, date: Date) -> DailyEntry {
        let day = CalendarDay(date)
        switch kind {
        case .saying:
            let all = json("sayings", [String: [String]].self) ?? [:]
            let s = pick(all[widgetLang] ?? all["en"] ?? [], day) ?? ""
            return DailyEntry(date: date, kind: kind, title: L("widget.daily.saying"), arabic: "", text: s, source: "")
        case .esma:
            let rows = json("esma", [[String]].self) ?? []
            guard let r = pick(rows, day) else { break }
            let read = ["tr", "az"].contains(widgetLang) ? r[1] : (["ur", "fa"].contains(widgetLang) ? "" : r[2])
            let col = ["tr": 3, "en": 4, "de": 5, "az": 6, "id": 7, "ur": 8, "fa": 9, "fr": 10, "ru": 11, "ja": 12, "ko": 13, "zh": 14, "fil": 15, "th": 16][widgetLang]
            let mean = col.map { $0 < r.count ? r[$0] : "" } ?? ""
            return DailyEntry(date: date, kind: kind, title: L("widget.daily.esma"), arabic: r[0], text: [read, mean].filter { !$0.isEmpty }.joined(separator: " — "), source: "")
        case .hadith:
            struct Raw: Decodable { let n: Int; let ar: String; let r: [String: String]; let s: [String: String]; let t: [String: String] }
            guard let h = pick(json("hadith", [Raw].self) ?? [], day) else { break }
            let text = widgetLang == "ar" ? "" : (h.t[widgetLang] ?? h.t["en"] ?? "")
            let src = String(format: L("hadith.source"), h.r[widgetLang] ?? h.r["en"] ?? "", h.s[widgetLang] ?? h.s["en"] ?? "", "\(h.n)")
            return DailyEntry(date: date, kind: kind, title: L("hadith.today"), arabic: h.ar, text: text, source: src)
        case .verse:
            return verse(date: date, day: day)
        }
        return DailyEntry(date: date, kind: kind, title: "", arabic: "", text: "", source: "")
    }

    /// Günün ayeti: kısa ayetlerden (en çok 120 harf) yılın gününe göre biri; Arapça (Tanzil) + İngilizce ve Almanca
    /// arayüzde Pickthall meali (uygulamadaki gibi; Türkçe meal yok)
    private static func verse(date: Date, day: CalendarDay) -> DailyEntry {
        func lines(_ name: String) -> [Substring] {
            guard let url = Bundle.main.url(forResource: name, withExtension: "txt"), let s = try? String(contentsOf: url, encoding: .utf8) else { return [] }
            return s.split(separator: "\n", omittingEmptySubsequences: false)
        }
        struct Meta: Decodable { let suras: [[MetaValue]] }
        enum MetaValue: Decodable {
            case s(String), i(Int)
            init(from d: Decoder) throws {
                let c = try d.singleValueContainer()
                if let v = try? c.decode(Int.self) { self = .i(v) } else { self = .s(try c.decode(String.self)) }
            }
            var str: String { if case let .s(v) = self { return v }; return "" }
            var int: Int { if case let .i(v) = self { return v }; return 0 }
        }
        let ar = lines("quran-ar")
        let suras = json("quran-meta", Meta.self)?.suras ?? []
        let count = 6236
        guard ar.count >= count, !suras.isEmpty else {
            return DailyEntry(date: date, kind: .verse, title: L("widget.daily.verse"), arabic: "", text: "", source: "")
        }
        var i = ((day.dayNumber * 211) % count + count) % count
        for _ in 0..<count where ar[i].count > 120 { i = (i + 1) % count }
        // Sure ve ayet numarası: başlangıç sırasına göre
        var sura = 0
        for (k, row) in suras.enumerated() where row.count > 6 && row[6].int <= i { sura = k }
        let ayah = i - (suras[sura].count > 6 ? suras[sura][6].int : 0) + 1
        let row = suras[sura]
        let name = ["tr", "az"].contains(widgetLang) ? row[3].str : (["ar", "ur", "fa"].contains(widgetLang) ? row[0].str : row[1].str)
        let meal = ["en", "de"].contains(widgetLang) ? lines("quran-en").dropFirst(i).first.map(String.init) ?? "" : ""
        return DailyEntry(date: date, kind: .verse, title: L("widget.daily.verse"), arabic: String(ar[i]), text: meal,
                          source: "\(name) \(sura + 1):\(ayah)")
    }
}

struct DailyProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> DailyEntry { DailyContent.entry(.saying, date: Date()) }
    func snapshot(for c: DailyIntent, in context: Context) async -> DailyEntry { DailyContent.entry(c.kind, date: Date()) }
    func timeline(for c: DailyIntent, in context: Context) async -> Timeline<DailyEntry> {
        let now = Date()
        let midnight = Calendar.current.startOfDay(for: now).addingTimeInterval(86_400 + 5)
        return Timeline(entries: [DailyContent.entry(c.kind, date: now)], policy: .after(midnight))
    }
}

struct DailyContentWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "daily-content", intent: DailyIntent.self, provider: DailyProvider()) { entry in
            DailyContentView(entry: entry)
        }
        .configurationDisplayName(L("widget.daily.name"))
        .description(L("widget.daily.desc"))
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

struct DailyContentView: View {
    @Environment(\.widgetFamily) private var family
    let entry: DailyEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(entry.title).font(.caption.weight(.semibold)).foregroundStyle(Theme.accent).widgetAccentable()
            if !entry.arabic.isEmpty {
                Text(entry.arabic)
                    .font(.system(size: family == .systemSmall ? 18 : 22))
                    .foregroundStyle(Theme.text)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .environment(\.layoutDirection, .rightToLeft)
                    .minimumScaleFactor(0.5)
                    .lineLimit(family == .systemLarge ? 8 : 4)
            }
            if !entry.text.isEmpty {
                Text(entry.text)
                    .font(entry.arabic.isEmpty ? .system(size: family == .systemSmall ? 15 : 17, weight: .medium, design: .serif) : .footnote)
                    .foregroundStyle(Theme.text)
                    .minimumScaleFactor(0.6)
                    .lineLimit(family == .systemLarge ? 12 : (family == .systemSmall ? 5 : 4))
            }
            Spacer(minLength: 0)
            if !entry.source.isEmpty {
                Text(entry.source).font(.caption2).foregroundStyle(Theme.muted).lineLimit(1).minimumScaleFactor(0.7)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .containerBackground(for: .widget) { Theme.bg }
    }
}
