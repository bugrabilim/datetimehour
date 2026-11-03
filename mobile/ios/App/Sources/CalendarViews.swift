import EventKit
import SaatCore
import SwiftUI

/// Takvim görünümleri ve ülke seçimi (2. analiz #116, #119, #122, #126, #127; Android CalendarViews.kt ile aynı):
/// ay / hafta / gün (zaman çizelgesi, bayram kartı) / ajanda (60 gün) / yıl (yoğunluk ısı haritası); hicri aylarla gezme;
/// ülke seçimi (30 ülkenin ulusal tatilleri, world-holidays.json) ve gösterilen gün türleri.
/// Kayıt: `cal.mode`, `cal.hijriNav`, `cal.country` (TR varsayılan), `cal.hidden` (virgülle tür).
enum CalPrefs {
    static let modeKey = "cal.mode"
    static let hijriNavKey = "cal.hijriNav"
    static let countryKey = "cal.country"
    static let hiddenKey = "cal.hidden"
    static let modes = ["month", "week", "day", "agenda", "year"]

    static var country: String { UserDefaults.standard.string(forKey: countryKey) ?? "TR" }
    static var hidden: Set<String> {
        Set((UserDefaults.standard.string(forKey: hiddenKey) ?? "").split(separator: ",").map(String.init))
    }
}

/// Dünya resmî tatilleri (Nager.Date, MIT; gen-strings ad tablosuyla)
enum WorldHol {
    private struct File: Decodable {
        let s: [String]
        let countries: [String: [String: [[Value]]]]
        let tx: [String: [String: String]]
        let native: [String: [String]]
    }

    enum Value: Decodable {
        case s(String), i(Int)
        init(from d: Decoder) throws {
            let c = try d.singleValueContainer()
            if let i = try? c.decode(Int.self) { self = .i(i) } else { self = .s(try c.decode(String.self)) }
        }
        var str: String { if case .s(let v) = self { return v } else { return "" } }
        var int: Int { if case .i(let v) = self { return v } else { return 0 } }
    }

    private static let file: File? = Bundle.main.url(forResource: "world-holidays", withExtension: "json")
        .flatMap { try? Data(contentsOf: $0) }
        .flatMap { try? JSONDecoder().decode(File.self, from: $0) }

    /// ülke → iso tarih → [(İngilizce ad sırası, yerel ad sırası)]
    private static let byDate: [String: [String: [(Int, Int)]]] = {
        guard let f = file else { return [:] }
        var out: [String: [String: [(Int, Int)]]] = [:]
        for (cc, years) in f.countries {
            var m: [String: [(Int, Int)]] = [:]
            for (_, rows) in years { for r in rows where r.count == 3 { m[r[0].str, default: []].append((r[1].int, r[2].int)) } }
            out[cc] = m
        }
        return out
    }()

    static var countries: [String] { Array((file?.countries ?? [:]).keys) }

    static func countryName(_ cc: String) -> String {
        Locale(identifier: AppLocale.code).localizedString(forRegionCode: cc) ?? cc
    }

    /// Sitedeki holName kuralı: İngilizcede İngilizce ad, ülkenin dilinde yerel ad, diğerlerinde çeviri (yoksa İngilizce)
    private static func name(_ cc: String, _ en: Int, _ local: Int) -> String {
        guard let f = file else { return "" }
        let lang = AppLocale.code
        if lang == "en" { return f.s[en] }
        if (f.native[lang] ?? []).contains(cc) { return f.s[local] }
        return f.tx[String(en)]?[lang] ?? f.s[en]
    }

    static func events(_ cc: String, on day: CalendarDay) -> [CalendarData.Event] {
        (byDate[cc]?[day.iso] ?? []).enumerated().map { i, p in
            CalendarData.Event(date: day.iso, key: "w:\(cc):\(p.0):\(i)", type: "holiday", label: name(cc, p.0, p.1))
        }
    }
}

/// Seçili ülkeye ve türlere göre bir günün özel günleri
func calEvents(_ day: CalendarDay) -> [CalendarData.Event] {
    let base = CalendarData.shared.events(on: day)
    let cc = CalPrefs.country
    let list = cc == "TR" ? base : base.filter { $0.type == "religious" } + WorldHol.events(cc, on: day)
    let hidden = CalPrefs.hidden
    return list.filter { !hidden.contains($0.type) }
}

/// Ay / hafta / gün / ajanda / yıl seçimi
struct CalModeBar: View {
    @AppStorage(CalPrefs.modeKey) private var mode = "month"

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(CalPrefs.modes, id: \.self) { m in chip(m) }
            }
        }
    }

    private func chip(_ m: String) -> some View {
        let on = m == mode
        return Button { mode = m } label: {
            Text(L("cal.mode.\(m)"))
                .font(.subheadline.weight(on ? .semibold : .regular))
                .padding(.horizontal, 14).padding(.vertical, 7)
                .background(Capsule().fill(on ? Theme.accentSoft : Theme.surface2))
                .foregroundStyle(on ? Theme.accent : Theme.text)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

private struct EventLine: View {
    let text: String
    let color: Color

    var body: some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 4, height: 18)
            Text(text).font(.subheadline).lineLimit(1)
        }
    }
}

/// Hafta: seçili günün haftası (pazartesi başlar)
struct WeekListView: View {
    let selected: CalendarDay
    let today: CalendarDay
    let onPick: (CalendarDay) -> Void

    var body: some View {
        let start = selected.adding(days: -selected.weekday)
        VStack(spacing: 0) {
            ForEach(0..<7, id: \.self) { i in
                row(start.adding(days: i), index: i)
                if i < 6 { Divider().padding(.leading, 66) }
            }
        }
        .background(RoundedRectangle(cornerRadius: 14).fill(Theme.surface))
    }

    private func row(_ d: CalendarDay, index i: Int) -> some View {
        let ev = calEvents(d)
        let mine = DeviceCalendar.shared.events(on: d)
        return Button { onPick(d) } label: {
            HStack(alignment: .top, spacing: 12) {
                VStack(spacing: 2) {
                    Text(L("calendar.weekdaysShort.\(i)")).font(.caption).foregroundStyle(i >= 5 ? Theme.accent : Theme.muted)
                    Text("\(d.day)").font(.headline)
                        .foregroundStyle(d == today ? Theme.surface : Theme.text)
                        .frame(width: 32, height: 32)
                        .background(Circle().fill(d == today ? Theme.accent : d == selected ? Theme.accentSoft : .clear))
                }
                .frame(width: 54)
                VStack(alignment: .leading, spacing: 3) {
                    ForEach(ev, id: \.self) { e in EventLine(text: e.name, color: e.color) }
                    ForEach(mine, id: \.eventIdentifier) { e in
                        EventLine(text: (e.isAllDay ? "" : Fmt.time(e.startDate) + " ") + (e.title ?? ""), color: Color(cgColor: e.calendar.cgColor))
                    }
                    if ev.isEmpty && mine.isEmpty { Text("–").foregroundStyle(Theme.border) }
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12).padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Gün: tüm gün olanlar üstte (bayramda çizimli kart), altında 24 saatlik zaman çizelgesi (etkinlikler ve namaz vakitleri)
struct DayTimelineView: View {
    let day: CalendarDay

    var body: some View {
        let ev = calEvents(day)
        let mine = DeviceCalendar.shared.events(on: day)
        VStack(spacing: 10) {
            if let b = ev.first(where: { ($0.key.hasPrefix("rb") || $0.key.hasPrefix("kb")) && !$0.key.hasSuffix("Eve") }) {
                BayramCard(name: b.name)
            }
            allDay(ev, mine.filter(\.isAllDay))
            hours(mine.filter { !$0.isAllDay })
        }
    }

    @ViewBuilder private func allDay(_ ev: [CalendarData.Event], _ mine: [EKEvent]) -> some View {
        if !ev.isEmpty || !mine.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text(L("calendar.allDay")).font(.caption).foregroundStyle(Theme.muted)
                ForEach(ev, id: \.self) { e in EventLine(text: e.name, color: e.color) }
                ForEach(mine, id: \.eventIdentifier) { e in EventLine(text: e.title ?? "", color: Color(cgColor: e.calendar.cgColor)) }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .card()
        }
    }

    private func hours(_ timed: [EKEvent]) -> some View {
        let prayers: [PrayerSchedule.Entry] = Features.isOn(.prayer)
            ? AppPlace.stored().schedule.entries(on: day.noon(in: .current)) : []
        let cal = Calendar.current
        return VStack(spacing: 0) {
            ForEach(0..<24, id: \.self) { h in
                HStack(alignment: .top, spacing: 8) {
                    Text(String(format: "%02d:00", h)).font(.caption.monospacedDigit()).foregroundStyle(Theme.muted).frame(width: 48, alignment: .leading)
                    VStack(alignment: .leading, spacing: 3) {
                        ForEach(prayers.filter { cal.component(.hour, from: $0.at) == h }, id: \.id) { p in
                            Text("☾ " + p.prayer.localizedName + " · " + Fmt.time(p.at)).font(.footnote).foregroundStyle(Theme.accent)
                        }
                        ForEach(timed.filter { cal.component(.hour, from: $0.startDate) == h }, id: \.eventIdentifier) { e in
                            Text(Fmt.time(e.startDate) + "–" + Fmt.time(e.endDate) + "  " + (e.title ?? ""))
                                .font(.footnote)
                                .padding(6)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(RoundedRectangle(cornerRadius: 6).fill(Color(cgColor: e.calendar.cgColor).opacity(0.18)))
                        }
                    }
                    Spacer(minLength: 0)
                }
                .frame(minHeight: 34, alignment: .top)
                .padding(.horizontal, 10).padding(.vertical, 4)
                if h < 23 { Divider() }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .background(RoundedRectangle(cornerRadius: 14).fill(Theme.surface))
    }
}

/// Bayram kartı: telefonda çizilen hilal, yıldızlar ve fener; altında tebrik
struct BayramCard: View {
    let name: String

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(colors: [Color(red: 0.08, green: 0.2, blue: 0.31), Color(red: 0.18, green: 0.36, blue: 0.48), Color(red: 0.72, green: 0.53, blue: 0.04)],
                           startPoint: .topLeading, endPoint: .bottomTrailing)
            Canvas { ctx, size in
                let w = size.width, h = size.height
                let c = CGPoint(x: w * 0.82, y: h * 0.32)
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - h * 0.2, y: c.y - h * 0.2, width: h * 0.4, height: h * 0.4)), with: .color(Color(red: 1, green: 0.91, blue: 0.66)))
                ctx.fill(Path(ellipseIn: CGRect(x: c.x + h * 0.08 - h * 0.17, y: c.y - h * 0.04 - h * 0.17, width: h * 0.34, height: h * 0.34)),
                         with: .color(Color(red: 0.13, green: 0.27, blue: 0.4)))
                for (x, y) in [(0.12, 0.2), (0.3, 0.12), (0.5, 0.25), (0.65, 0.1), (0.2, 0.45)] {
                    ctx.fill(Path(ellipseIn: CGRect(x: w * x - 2, y: h * y - 2, width: 4, height: 4)), with: .color(.white.opacity(0.8)))
                }
                let fx = w * 0.12
                var line = Path()
                line.move(to: CGPoint(x: fx, y: 0))
                line.addLine(to: CGPoint(x: fx, y: h * 0.35))
                ctx.stroke(line, with: .color(Color(red: 1, green: 0.91, blue: 0.66)), lineWidth: 2)
                var lantern = Path()
                lantern.move(to: CGPoint(x: fx - 10, y: h * 0.38))
                lantern.addLine(to: CGPoint(x: fx + 10, y: h * 0.38))
                lantern.addLine(to: CGPoint(x: fx + 14, y: h * 0.62))
                lantern.addLine(to: CGPoint(x: fx, y: h * 0.72))
                lantern.addLine(to: CGPoint(x: fx - 14, y: h * 0.62))
                lantern.closeSubpath()
                ctx.fill(lantern, with: .color(Color(red: 1, green: 0.78, blue: 0.34)))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(name).font(.title3.weight(.bold)).foregroundStyle(.white)
                Text(L("cal.bayram")).font(.subheadline).foregroundStyle(Color(red: 1, green: 0.91, blue: 0.66))
            }
            .padding(16)
        }
        .frame(height: 150)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

/// Ajanda: seçili günden başlayarak 60 gün, özel gün ya da etkinlik olan günler
struct AgendaListView: View {
    let from: CalendarDay
    let onPick: (CalendarDay) -> Void

    var body: some View {
        let days = (0..<60).map { from.adding(days: $0) }
            .filter { !calEvents($0).isEmpty || !DeviceCalendar.shared.events(on: $0).isEmpty }
        if days.isEmpty {
            EmptyState(icon: "calendar", title: L("cal.agendaEmpty"), text: nil)
        } else {
            VStack(spacing: 8) {
                ForEach(days, id: \.self) { d in dayCard(d) }
            }
        }
    }

    private func dayCard(_ d: CalendarDay) -> some View {
        Button { onPick(d) } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text(Fmt.day(d)).font(.subheadline.weight(.semibold))
                ForEach(calEvents(d), id: \.self) { e in EventLine(text: e.name, color: e.color) }
                ForEach(DeviceCalendar.shared.events(on: d), id: \.eventIdentifier) { e in
                    EventLine(text: (e.isAllDay ? "" : Fmt.time(e.startDate) + " ") + (e.title ?? ""), color: Color(cgColor: e.calendar.cgColor))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .card()
        }
        .buttonStyle(.plain)
    }
}

/// Yıl: 12 küçük ay, her gün özel gün ve etkinlik sayısına göre koyulaşır (yoğunluk ısı haritası)
struct YearHeatmapView: View {
    let year: Int
    let today: CalendarDay
    /// Telefon takvimindeki etkinlik sayısı (gün numarası → sayı; ay ay sorulur)
    let deviceCounts: [Int: Int]
    let onMonth: (CalendarDay) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
                ForEach(1...12, id: \.self) { m in month(m) }
            }
            Text(L("cal.density")).font(.caption).foregroundStyle(Theme.muted)
        }
    }

    private func month(_ m: Int) -> some View {
        let first = CalendarDay(year, m, 1)
        let len = CalendarDay.daysIn(year, m)
        let cells = Array(repeating: 0, count: first.weekday) + Array(1...len)
        return Button { onMonth(first) } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text(L("calendar.months.\(m - 1)")).font(.caption.weight(.semibold)).foregroundStyle(Theme.accent).lineLimit(1)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 1), count: 7), spacing: 1) {
                    ForEach(Array(cells.enumerated()), id: \.offset) { _, n in cell(m, n) }
                }
            }
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder private func cell(_ m: Int, _ n: Int) -> some View {
        if n == 0 {
            Color.clear.frame(height: 12)
        } else {
            let d = CalendarDay(year, m, n)
            let k = calEvents(d).count + (deviceCounts[d.dayNumber] ?? 0)
            Text("\(n)")
                .font(.system(size: 7, weight: d == today ? .bold : .regular))
                .foregroundStyle(d == today ? Theme.accent : k >= 3 ? Theme.surface : Theme.text)
                .frame(maxWidth: .infinity, minHeight: 12)
                .background(RoundedRectangle(cornerRadius: 2).fill(k == 0 ? Color.clear : Theme.accent.opacity(min(0.95, 0.18 + 0.22 * Double(min(k, 4))))))
        }
    }
}

/// Hicri ay ızgarası: hicri günler, altında küçük miladi gün
struct HijriMonthGridView: View {
    let year: Int
    let month: Int
    let today: CalendarDay
    let selected: CalendarDay
    let onPick: (CalendarDay) -> Void

    var body: some View {
        if let m = Hijri.month(year: year, month: month) {
            let first = CalendarDay(m.first.0, m.first.1, m.first.2)
            let cells = Array(repeating: 0, count: first.weekday) + Array(1...m.length)
            VStack(spacing: 4) {
                HStack(spacing: 0) {
                    ForEach(0..<7, id: \.self) { i in
                        Text(L("calendar.weekdaysShort.\(i)")).font(.caption).frame(maxWidth: .infinity)
                            .foregroundStyle(i >= 5 ? Theme.accent : Theme.muted)
                    }
                }
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 2), count: 7), spacing: 2) {
                    ForEach(Array(cells.enumerated()), id: \.offset) { _, n in cell(first, n) }
                }
            }
        }
    }

    @ViewBuilder private func cell(_ first: CalendarDay, _ n: Int) -> some View {
        if n == 0 {
            Color.clear.frame(height: 44)
        } else {
            let d = first.adding(days: n - 1)
            let ev = calEvents(d)
            Button { onPick(d) } label: {
                VStack(spacing: 1) {
                    Text("\(n)").font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(d == today ? Theme.surface : ev.contains { $0.type == "holiday" } ? CalendarData.color("holiday") : Theme.text)
                    Text("\(d.day)").font(.system(size: 9)).foregroundStyle(d == today ? Theme.surface : Theme.muted)
                    Circle().fill(ev.first?.color ?? .clear).frame(width: 5, height: 5)
                }
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(RoundedRectangle(cornerRadius: 10).fill(d == today ? Theme.accent : d == selected ? Theme.accentSoft : .clear))
            }
            .buttonStyle(.plain)
        }
    }
}

/// Ülke ve gösterilen türler
struct CalCountrySheet: View {
    @AppStorage(CalPrefs.countryKey) private var country = "TR"
    @AppStorage(CalPrefs.hiddenKey) private var hiddenRaw = ""
    @Environment(\.dismiss) private var dismiss

    private var types: [String] { country == "TR" ? ["holiday", "half", "religious", "special", "school"] : ["holiday", "religious"] }

    var body: some View {
        NavigationStack {
            List {
                Section(L("cal.shownTypes")) {
                    ForEach(types, id: \.self) { t in typeRow(t) }
                }
                Section {
                    ForEach(["TR"] + WorldHol.countries.sorted { WorldHol.countryName($0) < WorldHol.countryName($1) }, id: \.self) { cc in
                        Button { country = cc } label: {
                            HStack {
                                Text(WorldHol.countryName(cc)).foregroundStyle(Theme.text)
                                Spacer()
                                if cc == country { Image(systemName: "checkmark").foregroundStyle(Theme.accent) }
                            }
                        }
                    }
                } header: {
                    Text(L("cal.country"))
                } footer: {
                    Text(L("cal.countryNote"))
                }
            }
            .themedBackground()
            .navigationTitle(L("cal.country"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } } }
        }
        .tint(Theme.accent)
    }

    private func typeRow(_ t: String) -> some View {
        let hidden = Set(hiddenRaw.split(separator: ",").map(String.init))
        return Toggle(isOn: Binding(get: { !hidden.contains(t) }, set: { on in
            var h = hidden
            if on { h.remove(t) } else { h.insert(t) }
            hiddenRaw = h.sorted().joined(separator: ",")
        })) {
            HStack(spacing: 8) {
                Circle().fill(CalendarData.color(t)).frame(width: 8, height: 8)
                Text(calendarTypeLabel(t))
            }
        }
    }
}
