import SwiftUI
import SaatCore

/*
 * Esmaü'l-Hüsna, günün hadisi, namaz rehberi ve takvim yaprağı (30 Ekim 2026, rakip analizi listesi #69–#72).
 * Veri mobile/shared/religion/ → gen-religion.mjs (esma.json, hadith.json, guide.json, sayings.json). Android'deki
 * Content.kt ile aynı içerik ve sıra. Kayıt tutulmaz.
 */

private func bundleJSON<T: Decodable>(_ name: String, _ type: T.Type) -> T? {
    Bundle.main.url(forResource: name, withExtension: "json").flatMap { try? Data(contentsOf: $0) }
        .flatMap { try? JSONDecoder().decode(T.self, from: $0) }
}

/// Arapça satır: sağdan sola
private struct RTLText: View {
    let text: String
    var size: CGFloat = 22
    var body: some View {
        Text(text).font(.system(size: size)).lineSpacing(size * 0.4).foregroundStyle(Theme.ink)
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, .rightToLeft)
    }
}

// MARK: - Esmaü'l-Hüsna

struct EsmaName: Identifiable {
    let id: Int, ar: String, okunus: String, meaning: String
}

enum Esma {
    static let all: [EsmaName] = {
        let rows = bundleJSON("esma", [[String]].self) ?? []
        let lang = AppLocale.code
        return rows.enumerated().map { i, r in
            // Sütunlar: [ar, Türkçe okunuş, Latin okunuş, tr, en, de, az, id, ur, fa, fr, ru]; Farsça ve Urducada okunuş yok
            let read = switch lang { case "tr", "az": r[1]; case "ur", "fa": ""; default: r[2] }
            let col = ["tr": 3, "en": 4, "de": 5, "az": 6, "id": 7, "ur": 8, "fa": 9, "fr": 10, "ru": 11][lang]
            let mean = col.map { $0 < r.count ? r[$0] : "" } ?? ""
            return EsmaName(id: i + 1, ar: r[0], okunus: read, meaning: mean)
        }
    }()
}

struct EsmaView: View {
    @State private var query = ""
    private var list: [EsmaName] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return Esma.all }
        return Esma.all.filter { $0.okunus.lowercased().contains(q) || $0.meaning.lowercased().contains(q) || $0.ar.contains(q) }
    }

    var body: some View {
        List {
            Section {
                ForEach(list) { n in
                    HStack(alignment: .top, spacing: 12) {
                        Text("\(n.id)").font(.caption.monospacedDigit()).foregroundStyle(Theme.muted).frame(width: 26, alignment: .trailing)
                        VStack(alignment: .leading, spacing: 3) {
                            if AppLocale.code != "ar" {
                                if !n.okunus.isEmpty { Text(n.okunus).font(.headline).foregroundStyle(Theme.text) }
                                Text(n.meaning).font(n.okunus.isEmpty ? .body : .subheadline).foregroundStyle(n.okunus.isEmpty ? Theme.text : Theme.muted)
                            }
                        }
                        Spacer(minLength: 8)
                        Text(n.ar).font(.system(size: 24)).foregroundStyle(Theme.ink)
                    }
                    .accessibilityElement(children: .combine)
                }
            } footer: { Text(L("esma.note")) }
        }
        .searchable(text: $query, prompt: L("esma.search"))
        .themedBackground()
        .navigationTitle(L("esma.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Hadis

struct Hadith: Identifiable {
    let id: Int, ar: String, narrator: String, book: String, text: String
    var source: String { T("hadith.source", narrator, book, id) }
}

enum Hadiths {
    static let all: [Hadith] = {
        struct Raw: Decodable { let n: Int; let ar: String; let r: [String: String]; let s: [String: String]; let t: [String: String] }
        let lang = AppLocale.code
        return (bundleJSON("hadith", [Raw].self) ?? []).map { h in
            Hadith(id: h.n, ar: h.ar, narrator: h.r[lang] ?? h.r["en"] ?? "", book: h.s[lang] ?? h.s["en"] ?? "",
                   text: lang == "ar" ? "" : (h.t[lang] ?? h.t["en"] ?? ""))
        }
    }()

    /// Günün hadisi: yılın gününe göre sırayla
    static func today(_ day: CalendarDay = CalendarDay(Date())) -> Hadith? {
        all.isEmpty ? nil : all[(day.dayNumber % all.count + all.count) % all.count]
    }
}

private struct HadithCard: View {
    let h: Hadith
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if !h.text.isEmpty { Text(h.text).font(.body).foregroundStyle(Theme.text) }
            RTLText(text: h.ar, size: 18)
            Text(h.source).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }
}

struct HadithView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if let h = Hadiths.today() {
                    Text(L("hadith.today")).font(.headline).foregroundStyle(Theme.text)
                    HadithCard(h: h)
                }
                Text(L("hadith.title")).font(.headline).foregroundStyle(Theme.text).padding(.top, 8)
                ForEach(Hadiths.all) { HadithCard(h: $0) }
                Text(L("hadith.note")).font(.caption).foregroundStyle(Theme.muted)
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("hadith.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Namaz rehberi

struct PrayerGuideView: View {
    private struct Raw: Decodable {
        struct Section: Decodable {
            let id: String
            let title: [String: String]
            let intro: [String: String]
            let steps: [[String: String]]?
            let rows: [[Row]]?
            let duas: [String]?
        }
        enum Row: Decodable {
            case s(String), m([String: String])
            init(from d: Decoder) throws {
                let c = try d.singleValueContainer()
                if let s = try? c.decode(String.self) { self = .s(s) } else { self = .m(try c.decode([String: String].self)) }
            }
        }
        let sections: [Section]
        let note: [String: String]
    }
    private let data = bundleJSON("guide", Raw.self)
    private let lang = AppLocale.code
    private func tx(_ m: [String: String]) -> String { m[lang] ?? m["en"] ?? "" }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                ForEach(data?.sections ?? [], id: \.id) { s in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(tx(s.title)).font(.title3.weight(.semibold)).foregroundStyle(Theme.text)
                        Text(tx(s.intro)).font(.subheadline).foregroundStyle(Theme.muted)
                        ForEach(Array((s.steps ?? []).enumerated()), id: \.offset) { i, st in
                            HStack(alignment: .top, spacing: 10) {
                                Text("\(i + 1)").font(.subheadline.weight(.bold).monospacedDigit()).foregroundStyle(Theme.accent).frame(width: 22)
                                Text(tx(st)).font(.subheadline).foregroundStyle(Theme.text)
                            }
                        }
                        ForEach(Array((s.rows ?? []).enumerated()), id: \.offset) { _, r in
                            if r.count == 2, case let .s(key) = r[0], case let .m(m) = r[1] {
                                HStack(alignment: .top) {
                                    Text(L("prayer.\(key)")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text).frame(width: 90, alignment: .leading)
                                    Text(tx(m)).font(.subheadline).foregroundStyle(Theme.text)
                                }
                            }
                        }
                        if s.duas?.isEmpty == false {
                            NavigationLink { DuasView() } label: {
                                Label(L("guide.duas"), systemImage: "hands.sparkles").font(.subheadline.weight(.semibold))
                            }
                            .tint(Theme.accent)
                        }
                    }
                    .card()
                }
                if let n = data?.note { Text(tx(n)).font(.caption).foregroundStyle(Theme.muted) }
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("guide.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Takvim yaprağı ve günün sözü

enum Sayings {
    static let all: [String: [String]] = bundleJSON("sayings", [String: [String]].self) ?? [:]
    static func today(_ day: CalendarDay = CalendarDay(Date())) -> String? {
        let list = all[AppLocale.code] ?? all["en"] ?? []
        return list.isEmpty ? nil : list[(day.dayNumber % list.count + list.count) % list.count]
    }

    /// Rastgele bir söz (alarm yazma görevi)
    static func random() -> String? { (all[AppLocale.code] ?? all["en"])?.randomElement() }

    /// Hızır (6 Mayıs) ve Kasım (8 Kasım) günleri: geleneksel takvim yapraklarındaki gün sayısı
    static func hizirKasim(_ d: CalendarDay) -> (kasim: Bool, n: Int) {
        let hizir = CalendarDay(d.year, 5, 6), kasim = CalendarDay(d.year, 11, 8)
        if d >= hizir && d < kasim { return (false, d.dayNumber - hizir.dayNumber + 1) }
        let start = d < hizir ? CalendarDay(d.year - 1, 11, 8) : kasim
        return (true, d.dayNumber - start.dayNumber + 1)
    }
}

struct CalendarLeafView: View {
    @State private var day = CalendarDay(Date())
    private let place = PlaceModel.shared.place

    var body: some View {
        let date = day.noon(in: place.zone)
        let entries = place.schedule.entries(on: date)
        let events = CalendarData.shared.events(on: day)
        ScrollView {
            VStack(spacing: 14) {
                VStack(spacing: 4) {
                    Text(monthYear(date)).font(.headline).foregroundStyle(.white.opacity(0.9))
                    Text("\(day.day)").font(.system(size: 110, weight: .bold, design: .rounded)).foregroundStyle(.white).minimumScaleFactor(0.5)
                    Text(weekday(date)).font(.title3.weight(.semibold)).foregroundStyle(.white)
                    if let h = Hijri.date(year: day.year, month: day.month, day: day.day) {
                        Text("☾ " + h.text).font(.subheadline).foregroundStyle(.white.opacity(0.9))
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 22)
                .background(RoundedRectangle(cornerRadius: 18).fill(Theme.accent))
                .accessibilityElement(children: .combine)

                Text(T("leaf.dayOfYear", day.dayOfYear, day.isoWeek.week)).font(.subheadline).foregroundStyle(Theme.muted)
                if ["tr", "az"].contains(AppLocale.code) {
                    let hk = Sayings.hizirKasim(day)
                    Text(T(hk.kasim ? "leaf.kasim" : "leaf.hizir", hk.n)).font(.footnote).foregroundStyle(Theme.muted)
                }
                if !events.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(L("leaf.special")).font(.headline).foregroundStyle(Theme.text)
                        ForEach(events, id: \.self) { e in
                            Label { Text(e.name).foregroundStyle(Theme.text) } icon: { Circle().fill(e.color).frame(width: 10, height: 10) }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading).card()
                }
                if let rise = entries.first(where: { $0.prayer == .sunrise }), let set = entries.first(where: { $0.prayer == .maghrib }) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(L("leaf.sun") + " · " + place.name).font(.headline).foregroundStyle(Theme.text)
                        Text(T("leaf.sunTimes", place.hm(rise.at), place.hm(set.at))).foregroundStyle(Theme.text)
                        if Features.isOn(.prayer) {
                            Text(entries.map { $0.prayer.localizedName + " " + place.hm($0.at) }.joined(separator: " · "))
                                .font(.footnote).foregroundStyle(Theme.muted)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading).card()
                }
                if let s = Sayings.today(day) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(L("leaf.saying")).font(.headline).foregroundStyle(Theme.text)
                        Text("“" + s + "”").font(.title3).italic().foregroundStyle(Theme.text)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading).card()
                }
                HStack {
                    Button { day = day.adding(days: -1) } label: { Label(L("leaf.prev"), systemImage: "chevron.backward") }
                    Spacer()
                    Button { day = day.adding(days: 1) } label: { Label(L("leaf.next"), systemImage: "chevron.forward").labelStyle(.titleAndIcon) }
                }
                .tint(Theme.accent)
            }
            .padding(16)
        }
        .gesture(DragGesture(minimumDistance: 30).onEnded { v in
            if abs(v.translation.width) > abs(v.translation.height) { day = day.adding(days: v.translation.width < 0 ? 1 : -1) }
        })
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("leaf.title"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func monthYear(_ d: Date) -> String {
        var s = Date.FormatStyle.dateTime.month(.wide).year().locale(AppLocale.locale)
        s.timeZone = place.zone
        s.calendar = AppLocale.gregorian
        return d.formatted(s)
    }
    private func weekday(_ d: Date) -> String {
        var s = Date.FormatStyle.dateTime.weekday(.wide).locale(AppLocale.locale)
        s.timeZone = place.zone
        s.calendar = AppLocale.gregorian
        return d.formatted(s)
    }
}
