import SwiftUI
import SaatCore

let moonPhaseKeys = ["new", "waxingCrescent", "firstQuarter", "waxingGibbous", "full", "waningGibbous", "lastQuarter", "waningCrescent"]
let moonPhaseEmoji = ["🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"]

extension AppPlace {
    /// Yerin o gününün gece yarısı
    func startOfDay(_ d: CalendarDay) -> Date {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = zone
        return cal.date(from: DateComponents(year: d.year, month: d.month, day: d.day)) ?? Date()
    }

    /// O gün içinde Ay'ın doğuşu ve batışı (gece yarısından ertesi gece yarısına)
    func moonRiseSet(_ d: CalendarDay) -> Nature.MoonRiseSet {
        let start = startOfDay(d), end = startOfDay(d.adding(days: 1))
        return Nature.moonRiseSet(startMilliseconds: Int64(start.timeIntervalSince1970 * 1000), latitude: lat, longitude: lon,
                                  spanMilliseconds: Int64(end.timeIntervalSince(start) * 1000))
    }

    /// Ay doğuş/batış saati (dakikaya yuvarlanır) ya da "bugün doğmuyor/batmıyor"
    func moonTime(_ ms: Int64?, none: String) -> String {
        guard let ms else { return L(none) }
        return hm(Date(unixMilliseconds: (ms + 30_000) / 60_000 * 60_000))
    }
}

/// NASA tablosundaki tutulma (src/data/eclipses.json; ut: en büyük tutulma anı, UTC ms)
struct Eclipse: Decodable, Identifiable {
    let kind: String
    let ut: Int64
    let type: String
    let regions: [String]
    let path: [String]

    var id: Int64 { ut }
    var date: Date { Date(unixMilliseconds: ut) }
    var title: String { L("nature.eclipseTypes.\(kind)\(type.prefix(1).uppercased())\(type.dropFirst())") }

    static let all: [Eclipse] = {
        guard let url = Bundle.main.url(forResource: "eclipses", withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([Eclipse].self, from: data)) ?? []
    }()

    /// Bölge ve ülke adları uygulamanın dilinde (UN M49 bölge kodları ve ISO ülke kodları)
    static func names(_ codes: [String]) -> String {
        codes.map { AppLocale.locale.localizedString(forRegionCode: $0) ?? $0 }.joined(separator: ", ")
    }
}

/// Ay takvimi (her gün öğle saatinde evre; yeni ay ve dolunay günleri adıyla), bugün Ay doğuşu/batışı ve yaklaşan tutulmalar
struct MoonView: View {
    @State private var month = CalendarDay(Date())
    /// Tarih kaydırıcı (2. analiz #146): bugünden −60…+60 gün; takvimde güne dokunmak da seçer
    @State private var offset = 0

    var body: some View {
        let place = PlaceModel.shared.place
        let now = Date()
        let today = CalendarDay(now, timeZone: place.zone)
        let rs = place.moonRiseSet(today)
        List {
            Section { MoonScrubber(place: place, today: today, offset: $offset) }
            Section {
                HStack {
                    Button { month = month.adding(months: -1) } label: { Image(systemName: "chevron.left") }
                        .accessibilityLabel(L("prayer.prevMonth"))
                    Spacer()
                    Text("\(L("calendar.months.\(month.month - 1)")) \(String(month.year))").font(.headline)
                    Spacer()
                    Button { month = month.adding(months: 1) } label: { Image(systemName: "chevron.right") }
                        .accessibilityLabel(L("prayer.nextMonth"))
                }
                .buttonStyle(.borderless)
                .environment(\.layoutDirection, .leftToRight)
                MoonMonthGrid(year: month.year, month: month.month, place: place, today: today) { d in
                    offset = max(-60, min(60, d.dayNumber - today.dayNumber))
                }
            }
            MoonDataSection(place: place, now: now)
            Section {
                LabeledContent(L("nature.moonrise"), value: place.moonTime(rs.rise, none: "nature.moonNoRise"))
                LabeledContent(L("nature.moonset"), value: place.moonTime(rs.set, none: "nature.moonNoSet"))
            } footer: {
                Text(place.name)
            }
            CrescentSection(place: place, now: now)
            YearMoonsSection(place: place, now: now)
            Section {
                Text(T("nature.eclipseLead", place.name)).font(.footnote).foregroundStyle(Theme.muted)
                ForEach(Eclipse.all.filter { $0.date > now.addingTimeInterval(-6 * 3600) }.prefix(6)) { e in
                    VStack(alignment: .leading, spacing: 4) {
                        Text((e.kind == "solar" ? "☀️ " : "🌕 ") + e.title).font(.headline)
                        Text("\(Fmt.longDate(e.date, tz: place.zone)) · \(place.hm(Date(unixMilliseconds: (e.ut + 30_000) / 60_000 * 60_000)))")
                            .font(.subheadline.weight(.semibold))
                        Text(T("nature.eclipseRegions", Eclipse.names(e.regions))).font(.caption).foregroundStyle(Theme.muted)
                        if !e.path.isEmpty {
                            Text(T("nature.eclipsePath", Eclipse.names(e.path))).font(.caption).foregroundStyle(Theme.muted)
                        }
                        if e.kind == "lunar" {
                            // Ay o anda yerin ufkunun üstündeyse tutulma buradan izlenebilir
                            let up = Nature.moonAltitude(Double(e.ut), lat: place.lat, lon: place.lon) > 0
                            Text(T(up ? "nature.eclipseHere" : "nature.eclipseNotHere", place.name))
                                .font(.caption.weight(up ? .semibold : .regular))
                                .foregroundStyle(up ? Theme.accent : Theme.muted)
                        }
                    }
                    .padding(.vertical, 4)
                }
            } header: {
                Text(L("nature.eclipseHeading"))
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("nature.eclipseSafety")).fontWeight(.semibold)
                    Text(L("nature.eclipseSource"))
                }
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("moon.calendarLink"))
        .navigationBarTitleDisplayMode(.large)
    }
}

/// Ayın günleri: evre emojisi, gün, yeni ay ve dolunay adı (hesaplanan anın yerin saatiyle düştüğü gün)
private struct MoonMonthGrid: View {
    let year: Int
    let month: Int
    let place: AppPlace
    let today: CalendarDay
    var onPick: (CalendarDay) -> Void = { _ in }

    var body: some View {
        let first = CalendarDay(year, month, 1)
        let count = CalendarDay.daysIn(year, month)
        let marks = Self.marks(year: year, month: month, place: place)
        let cells: [CalendarDay?] = Array(repeating: nil, count: first.weekday) + (1...count).map { CalendarDay(year, month, $0) }
        let columns = Array(repeating: GridItem(.flexible(), spacing: 2), count: 7)
        LazyVGrid(columns: columns, spacing: 4) {
            ForEach(0..<7, id: \.self) { i in
                Text(L("calendar.weekdaysShort.\(i)")).font(.caption2.weight(.semibold)).foregroundStyle(Theme.muted)
            }
            ForEach(Array(cells.enumerated()), id: \.offset) { _, d in
                if let d {
                    let noon = place.startOfDay(d).addingTimeInterval(12 * 3600)
                    let m = Nature.moon(at: noon)
                    VStack(spacing: 1) {
                        Text(moonPhaseEmoji[m.phaseIndex]).font(.system(size: 20))
                        Text("\(d.day)").font(.caption.weight(.semibold)).monospacedDigit()
                        Text(marks[d].map { L("nature.phases.\($0)") } ?? " ")
                            .font(.system(size: 8))
                            .foregroundStyle(Theme.accent)
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                            .minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity, minHeight: 58, alignment: .top)
                    .padding(.vertical, 3)
                    .background(d == today ? Theme.accentSoft : .clear, in: RoundedRectangle(cornerRadius: 8))
                    .contentShape(Rectangle())
                    .onTapGesture { onPick(d) }
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isButton)
                    .accessibilityLabel("\(d.day) · \(L("nature.phases.\(moonPhaseKeys[m.phaseIndex])"))")
                } else {
                    Color.clear.frame(height: 1)
                }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    static func marks(year: Int, month: Int, place: AppPlace) -> [CalendarDay: String] {
        let start = place.startOfDay(CalendarDay(year, month, 1)).timeIntervalSince1970 * 1000
        let k0 = Int(floor((start / 86_400_000 + 2440587.5 - 2451550.09766) / 29.530588861)) - 1
        var out: [CalendarDay: String] = [:]
        for k in k0...(k0 + 3) {
            for (ms, key) in [(Nature.phaseMilliseconds(k: k, full: false), "new"), (Nature.phaseMilliseconds(k: k, full: true), "full")] {
                let d = CalendarDay(Date(unixMilliseconds: ms), timeZone: place.zone)
                if d.year == year && d.month == month { out[d] = key }
            }
        }
        return out
    }
}

/// Yılın dolunayları (2. analiz #148, #161): sayı, Mavi Ay (aynı takvim ayında ikinci dolunay), Kuzey Amerika adları;
/// Hasat Ayı sonbahar ekinoksuna en yakın dolunay (sitedeki Mevsimler ile aynı kural).
enum YearMoons {
    struct Item: Identifiable { let id: Int64; let date: Date; let name: String; let blue: Bool }

    static func list(year: Int, zone: TimeZone) -> [Item] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = zone
        guard let start = cal.date(from: DateComponents(year: year, month: 1, day: 1)) else { return [] }
        let jan1 = start.timeIntervalSince1970 * 1000
        let k0 = Int((jan1 / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861) - 2
        var times: [Int64] = []
        var months: [Int] = []
        for k in k0...(k0 + 16) {
            let ms = Nature.phaseMilliseconds(k: k, full: true)
            let c = cal.dateComponents([.year, .month], from: Date(timeIntervalSince1970: Double(ms) / 1000))
            if c.year == year { times.append(ms); months.append(c.month ?? 1) }
        }
        if times.isEmpty { return [] }
        let eq = Nature.seasons(year: year).september
        var hv = 0
        for i in times.indices where abs(times[i] - eq) < abs(times[hv] - eq) { hv = i }
        var names = months.map { L("nature.ym.names.\($0 - 1)") }
        var blue = Array(repeating: false, count: times.count)
        for i in times.indices where i > 0 && months[i - 1] == months[i] {
            blue[i] = true
            names[i] = L("nature.ym.blueName")
        }
        names[hv] = L("nature.ym.harvest")
        if months[hv] == 10 && hv + 1 < times.count { names[hv + 1] = L("nature.ym.names.9") }
        return times.indices.map { Item(id: times[$0], date: Date(timeIntervalSince1970: Double(times[$0]) / 1000), name: names[$0], blue: blue[$0]) }
    }
}

private struct YearMoonsSection: View {
    let place: AppPlace
    let now: Date

    private var year: Int { CalendarDay(now, timeZone: place.zone).year }
    private var items: [YearMoons.Item] { YearMoons.list(year: year, zone: place.zone) }
    private var summary: String {
        let list = items
        let tail: String
        if let b = list.first(where: { $0.blue }) { tail = T("nature.ym.blue", Fmt.dayMonth(b.date, tz: place.zone)) } else { tail = L("nature.ym.noBlue") }
        return T("nature.ym.count", String(year), String(list.count)) + " " + tail
    }

    var body: some View {
        Section {
            Text(summary).font(.footnote).foregroundStyle(Theme.text)
            ForEach(items) { m in
                HStack {
                    Text((m.blue ? "🔵 " : "🌕 ") + m.name)
                        .foregroundStyle(m.date < now ? Theme.muted : Theme.text)
                    Spacer()
                    Text(Fmt.shortDateTime(m.date, tz: place.zone)).monospacedDigit().foregroundStyle(Theme.muted)
                }
            }
        } header: {
            Text(T("nature.ym.heading", String(year)))
        } footer: {
            Text(L("nature.ym.namesNote"))
        }
    }
}
