import AVFoundation
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
            // Sütunlar: [ar, Türkçe okunuş, Latin okunuş, tr, en, de, az, id, ur, fa, fr, ru, ja, ko, zh, fil, th]; Farsça ve Urducada okunuş yok
            let read = switch lang { case "tr", "az": r[1]; case "ur", "fa": ""; default: r[2] }
            let col = ["tr": 3, "en": 4, "de": 5, "az": 6, "id": 7, "ur": 8, "fa": 9, "fr": 10, "ru": 11, "ja": 12, "ko": 13, "zh": 14, "fil": 15, "th": 16][lang]
            let mean = col.map { $0 < r.count ? r[$0] : "" } ?? ""
            return EsmaName(id: i + 1, ar: r[0], okunus: read, meaning: mean)
        }
    }()
}

struct EsmaView: View {
    @State private var query = ""
    /// Dokununca hat kartı ve sesli okuma (2. analiz #85)
    @State private var detail: EsmaName?
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
                    .contentShape(Rectangle())
                    .onTapGesture { detail = n }
                }
            } footer: { Text(L("esma.note")) }
        }
        .sheet(item: $detail) { EsmaCard(name: $0) }
        .searchable(text: $query, prompt: L("esma.search"))
        .themedBackground()
        .navigationTitle(L("esma.title"))
        .navigationBarTitleDisplayMode(.large)
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
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("hadith.title"))
        .navigationBarTitleDisplayMode(.large)
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
    private func stepTexts(_ id: String) -> [String] {
        (data?.sections.first { $0.id == id }?.steps ?? []).map { tx($0) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                GuidePictureCard(howto: stepTexts("howto"), wudu: stepTexts("wudu"))
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
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("guide.title"))
        .navigationBarTitleDisplayMode(.large)
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
                    Text(Rumi.text(day)).font(.footnote).foregroundStyle(.white.opacity(0.85))
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
                if Features.isOn(.prayer), let h = Hadiths.today(day) {
                    Text(L("hadith.today")).font(.headline).foregroundStyle(Theme.text).frame(maxWidth: .infinity, alignment: .leading)
                    HadithCard(h: h)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("calendar.monthOrigin.heading")).font(.headline).foregroundStyle(Theme.text)
                    Text(L("calendar.monthOrigin.items.\(day.month - 1)")).foregroundStyle(Theme.text)
                    Text(L("calendar.monthOrigin.source")).font(.caption2).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading).card()
                let otd = OnThisDay.items(day)
                if !otd.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(L("otd.h1")).font(.headline).foregroundStyle(Theme.text)
                        ForEach(otd, id: \.self) { it in
                            VStack(alignment: .leading, spacing: 1) {
                                Text(it.icon + " " + String(it.year) + " · " + it.title).foregroundStyle(Theme.text)
                                if !it.desc.isEmpty { Text(it.desc).font(.caption).foregroundStyle(Theme.muted) }
                            }
                        }
                        Text("Wikidata (CC0)").font(.caption2).foregroundStyle(Theme.muted)
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
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("leaf.title"))
        .navigationBarTitleDisplayMode(.large)
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

/// Rumi tarih (2. analiz #120): Jülyen takvimi, yılbaşı 1 Mart, yıl = Jülyen yıl − 584 (Mart–Aralık) ya da − 585;
/// Türkiye Takvimi geleneği (sitedeki hicri çeviriciyle aynı hesap)
enum Rumi {
    static let months = ["Kânûn-ı sânî", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül",
                         "Teşrîn-i evvel", "Teşrîn-i sânî", "Kânûn-ı evvel"]

    static func parts(_ d: CalendarDay) -> (day: Int, month: Int, year: Int) {
        // Miladi → Jülyen gün numarası → Jülyen takvimi
        let a = (14 - d.month) / 12, y = d.year + 4800 - a, m = d.month + 12 * a - 3
        let jdn = d.day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        let c = jdn + 32082, dd = (4 * c + 3) / 1461, e = c - 1461 * dd / 4, mm = (5 * e + 2) / 153
        let day = e - (153 * mm + 2) / 5 + 1, month = mm + 3 - 12 * (mm / 10), year = dd - 4800 + mm / 10
        return (day, month, year - (month >= 3 ? 584 : 585))
    }

    static func text(_ d: CalendarDay) -> String {
        let r = parts(d)
        return T("hijriConv.rumi", "\(r.day) \(months[r.month - 1]) \(r.year)")
    }
}

/// Tarihte bugün (2. analiz #124): Wikidata (CC0) kayıtları, dile göre otd-<dil>.json (gen-strings.mjs, sitedeki seçim kuralı)
enum OnThisDay {
    struct Item: Hashable {
        let year: Int
        let kind: String
        let name: String
        let desc: String
        var icon: String { ["e": "📜", "f": "🏛️", "b": "🎂", "d": "🕯️"][kind] ?? "📜" }
        var title: String { kind == "f" ? T("otd.foundedFmt", name) : name }
    }

    @MainActor private static var cache: (lang: String, days: [String: [Item]])?

    @MainActor static func items(_ d: CalendarDay) -> [Item] {
        let lang = AppLocale.code
        if cache?.lang != lang { cache = (lang, load(lang)) }
        return cache?.days[String(format: "%02d-%02d", d.month, d.day)] ?? []
    }

    private static func load(_ lang: String) -> [String: [Item]] {
        guard let url = Bundle.main.url(forResource: "otd-\(lang)", withExtension: "json") ?? Bundle.main.url(forResource: "otd-en", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: [[Any]]] else { return [:] }
        var out: [String: [Item]] = [:]
        for (md, rows) in obj {
            out[md] = rows.compactMap { r in
                guard r.count >= 4, let y = r[0] as? Int, let k = r[1] as? String, let n = r[2] as? String else { return nil }
                return Item(year: y, kind: k, name: n, desc: r[3] as? String ?? "")
            }
        }
        return out
    }
}

/// Esma kartı (2. analiz #85): ismin Arapçası büyük, sekiz köşeli yıldız çerçevede (telefonda çizilir; lisanslı hat
/// görseli kullanılmaz), okunuşu ve anlamı; "Dinle" telefonun Arapça konuşma sesiyle (AVSpeechSynthesizer) okur.
struct EsmaCard: View {
    let name: EsmaName
    @State private var synth = AVSpeechSynthesizer()

    private var voice: AVSpeechSynthesisVoice? { AVSpeechSynthesisVoice(language: "ar-SA") ?? AVSpeechSynthesisVoice.speechVoices().first { $0.language.hasPrefix("ar") } }

    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                Canvas { ctx, size in
                    let c = CGPoint(x: size.width / 2, y: size.height / 2)
                    let r = min(size.width, size.height) / 2 - 4
                    for k in 0..<2 {
                        var p = Path()
                        for i in 0..<16 {
                            let a = Double.pi / 8 * Double(i) - Double.pi / 2
                            let base = r - CGFloat(k) * 10
                            let rr = i % 2 == 0 ? base : base * 0.82
                            let pt = CGPoint(x: c.x + rr * CGFloat(cos(a)), y: c.y + rr * CGFloat(sin(a)))
                            if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                        }
                        p.closeSubpath()
                        ctx.stroke(p, with: .color(k == 0 ? Theme.accent : Theme.accent2), lineWidth: k == 0 ? 3 : 1.5)
                    }
                    let inner = r * 0.62
                    ctx.fill(Path(ellipseIn: CGRect(x: c.x - inner, y: c.y - inner, width: 2 * inner, height: 2 * inner)), with: .color(Theme.accent.opacity(0.08)))
                }
                .frame(height: 200)
                Text(name.ar).font(.system(size: 44, weight: .bold, design: .serif)).foregroundStyle(Theme.ink)
            }
            if AppLocale.code != "ar" {
                if !name.okunus.isEmpty { Text(name.okunus).font(.title3.weight(.semibold)) }
                Text(name.meaning).font(.subheadline).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
            }
            Button {
                let u = AVSpeechUtterance(string: name.ar)
                u.voice = voice
                u.rate = AVSpeechUtteranceDefaultSpeechRate * 0.8
                synth.speak(u)
            } label: {
                Label(L("esma.listen"), systemImage: "speaker.wave.2.fill").font(.headline)
            }
            .buttonStyle(.borderedProminent)
            .disabled(voice == nil)
            Text(L(voice == nil ? "esma.noVoice" : "esma.voiceNote")).font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
        }
        .padding(24)
        .presentationDetents([.medium, .large])
    }
}
