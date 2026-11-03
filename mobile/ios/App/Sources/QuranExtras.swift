import SwiftUI
import SaatCore

/*
 * Kur'an ekleri (3 Kasım 2026, 2. analiz #62, #71; Android'deki QuranExtras.kt ile aynı kayıt anahtarları): hatim takibi
 * (kaldığım yerden ilerleme, 30 cüz halkası, tahmini bitiş, tamamlanan hatim sayısı; `quran.hatim`), okuma zemini
 * (`quran.theme`: uygulama, kâğıt, sepya, yeşil, gül, gece mavisi, koyu) ve günlük ayet hedefi (`quran.goal`, okunan
 * ayetler `quran.read`: kaldığım yer işareti ileri alındıkça; tek seferde 50 ayetten uzun atlama okuma sayılmaz).
 */
enum QuranPlan {
    static let total = 6236
    private static var d: UserDefaults { .standard }

    static func index(_ s: Int, _ a: Int) -> Int {
        let suras = Quran.data.suras
        guard s >= 1, s <= suras.count else { return 0 }
        return suras[s - 1].start + a - 1
    }

    static func bookmarkIndex() -> Int? {
        Quran.parse(d.string(forKey: Quran.bookmarkKey) ?? "").map { index($0.0, $0.1) }
    }

    /// Ayetin cüzü (1–30)
    static func juzOf(_ i: Int) -> Int {
        (Quran.data.juz.lastIndex { $0.count == 2 && index($0[0], $0[1]) <= i } ?? 0) + 1
    }

    private static func hatim() -> [String: Any] {
        d.string(forKey: "quran.hatim").flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
    }

    private static func putHatim(_ o: [String: Any]) {
        if let data = try? JSONSerialization.data(withJSONObject: o, options: [.sortedKeys]) {
            d.set(String(decoding: data, as: UTF8.self), forKey: "quran.hatim")
        }
    }

    static var completed: Int { (hatim()["n"] as? NSNumber)?.intValue ?? 0 }

    /// Başlangıç günü ve ayeti (yoksa şimdiki yer bugünden başlamış sayılır)
    @discardableResult
    static func start() -> (CalendarDay, Int) {
        let h = hatim()
        if let s = h["s"] as? String, let day = CalendarDay(iso: s) { return (day, (h["i"] as? NSNumber)?.intValue ?? 0) }
        let now = (CalendarDay(Date()), bookmarkIndex() ?? 0)
        putHatim(["s": now.0.iso, "i": now.1, "n": 0])
        return now
    }

    /// Tahmini bitiş (en az bir gün ve bir ayet ilerleme gerekir)
    static func eta(_ today: CalendarDay = CalendarDay(Date())) -> CalendarDay? {
        guard let i = bookmarkIndex() else { return nil }
        let (s, i0) = start()
        let days = today.dayNumber - s.dayNumber
        let read = i - i0
        guard read > 0, days >= 1 else { return nil }
        let left = Double(total - 1 - i)
        return today.adding(days: Int((left / (Double(read) / Double(days))).rounded(.up)))
    }

    static func newHatim() {
        let done = (bookmarkIndex() ?? 0) >= total - 70
        putHatim(["s": CalendarDay(Date()).iso, "i": 0, "n": completed + (done ? 1 : 0)])
        d.set("1:1", forKey: Quran.bookmarkKey)
    }

    // Günlük hedef
    static let goalKey = "quran.goal"
    static let goals = [0, 5, 10, 20, 50, 100]

    static func readToday(_ today: CalendarDay = CalendarDay(Date())) -> Int {
        let o = d.string(forKey: "quran.read").flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
        return (o[today.iso] as? NSNumber)?.intValue ?? 0
    }

    /// Kaldığım yeri değiştir; ileri doğru ve 50 ayetten kısa adımlar bugünün okumasına eklenir
    static func setBookmark(_ s: Int, _ a: Int) {
        let old = bookmarkIndex()
        d.set("\(s):\(a)", forKey: Quran.bookmarkKey)
        let new = index(s, a)
        if let old, new > old, new - old <= 50 {
            var o = d.string(forKey: "quran.read").flatMap { $0.data(using: .utf8) }
                .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
            let today = CalendarDay(Date())
            o[today.iso] = ((o[today.iso] as? NSNumber)?.intValue ?? 0) + (new - old)
            let cut = today.adding(days: -60).iso
            for k in o.keys where k < cut { o.removeValue(forKey: k) }
            if let data = try? JSONSerialization.data(withJSONObject: o, options: [.sortedKeys]) {
                d.set(String(decoding: data, as: UTF8.self), forKey: "quran.read")
            }
        }
        start()
    }

    // Okuma zemini
    struct Palette { let bg: Color, card: Color, ink: Color, muted: Color, soft: Color }
    static let themeKey = "quran.theme"
    static let themes = ["app", "paper", "sepia", "green", "rose", "night", "dark"]

    private static func c(_ hex: UInt32) -> Color {
        Color(red: Double(hex >> 16 & 0xFF) / 255, green: Double(hex >> 8 & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }

    static func palette(_ id: String) -> Palette {
        switch id {
        case "paper": Palette(bg: c(0xFBF8F1), card: c(0xFFFFFF), ink: c(0x1B1B1B), muted: c(0x6B6B6B), soft: c(0xF1E6C9))
        case "sepia": Palette(bg: c(0xF3E7CF), card: c(0xF8EEDB), ink: c(0x3B2A12), muted: c(0x7A6346), soft: c(0xE8D3A6))
        case "green": Palette(bg: c(0xE6F2EA), card: c(0xF2F9F4), ink: c(0x10301F), muted: c(0x4F6F5C), soft: c(0xC9E4D2))
        case "rose": Palette(bg: c(0xF8E9EC), card: c(0xFDF3F5), ink: c(0x3A1A22), muted: c(0x7A5560), soft: c(0xF0D0D8))
        case "night": Palette(bg: c(0x0F1A2E), card: c(0x17243D), ink: c(0xE8EEF8), muted: c(0x9AA8C0), soft: c(0x263A5E))
        case "dark": Palette(bg: c(0x000000), card: c(0x121212), ink: c(0xEDEDED), muted: c(0x9E9E9E), soft: c(0x2A2A2A))
        default: Palette(bg: Theme.bg, card: Theme.surface, ink: Theme.ink, muted: Theme.muted, soft: Theme.accentSoft)
        }
    }
}

/// Hatim kartı: 30 cüz halkası, ilerleme, tahmini bitiş, yeni hatim
struct HatimCard: View {
    @AppStorage(Quran.bookmarkKey) private var bookmark = ""
    @AppStorage(QuranPlan.goalKey) private var goal = 0
    @State private var confirm = false
    @State private var version = 0

    var body: some View {
        let _ = (bookmark, version)
        let i = QuranPlan.bookmarkIndex() ?? 0
        let p = Double(i + 1) / Double(QuranPlan.total)
        let juz = QuranPlan.juzOf(i)
        let progress = T("quran.hatimProgress", Fmt.percent(Int((p * 100).rounded())), juz)
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 14) {
                ZStack {
                    ForEach(0..<30, id: \.self) { k in
                        let filled = k < juz - 1 || (k == juz - 1 && p >= 0.999)
                        Circle()
                            .trim(from: (CGFloat(k) * 12 + 1.2) / 360, to: (CGFloat(k) * 12 + 10.8) / 360)
                            .stroke(filled ? Theme.accent : k == juz - 1 ? Theme.accent.opacity(0.45) : Theme.surface2, lineWidth: 5)
                            .rotationEffect(.degrees(-90))
                    }
                    Text("\(juz)").font(.title3.weight(.bold)).monospacedDigit().foregroundStyle(Theme.text)
                }
                .frame(width: 70, height: 70)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(progress)
                VStack(alignment: .leading, spacing: 3) {
                    Text(L("quran.hatim")).font(.headline).foregroundStyle(Theme.text)
                    Text(progress).font(.footnote).foregroundStyle(Theme.muted)
                    if let eta = QuranPlan.eta() {
                        Text(T("quran.hatimEta", Fmt.day(eta))).font(.footnote).foregroundStyle(Theme.muted)
                    }
                    if QuranPlan.completed > 0 {
                        Text(T("quran.hatimCount", QuranPlan.completed)).font(.footnote).foregroundStyle(Theme.muted)
                    }
                }
            }
            if goal > 0 {
                let n = QuranPlan.readToday()
                Text(n >= goal ? L("quran.goalDone") : T("quran.todayRead", n, goal))
                    .font(.subheadline).foregroundStyle(n >= goal ? Theme.accent : Theme.text)
            }
            Button(L("quran.hatimNew")) { confirm = true }.font(.subheadline.weight(.semibold)).tint(Theme.accent)
        }
        .card()
        .confirmationDialog(L("quran.hatimConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("quran.hatimNew")) { QuranPlan.newHatim(); version += 1 }
        }
    }
}

/// Sure ekranının üstünde zemin ve günlük hedef seçimi
struct QuranReadingOptions: View {
    @AppStorage(QuranPlan.themeKey) private var theme = "app"
    @AppStorage(QuranPlan.goalKey) private var goal = 0
    @AppStorage(Quran.bookmarkKey) private var bookmark = ""

    var body: some View {
        let pal = QuranPlan.palette(theme)
        VStack(alignment: .leading, spacing: 8) {
            Text(L("quran.theme")).font(.footnote).foregroundStyle(pal.muted)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(QuranPlan.themes, id: \.self) { t in
                        let p = QuranPlan.palette(t)
                        Button { theme = t } label: {
                            HStack(spacing: 5) {
                                Circle().fill(t == theme ? Theme.accent : p.bg).frame(width: 12, height: 12)
                                Text(L("quran.theme.\(t)")).font(.footnote.weight(t == theme ? .bold : .regular)).foregroundStyle(p.ink)
                            }
                            .padding(.horizontal, 12).padding(.vertical, 7)
                            .background(p.card, in: Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(t == theme ? .isSelected : [])
                    }
                }
            }
            Picker(L("quran.goal"), selection: $goal) {
                ForEach(QuranPlan.goals, id: \.self) { g in
                    Text(g == 0 ? L("quran.goalOff") : T("quran.goalAyas", g)).tag(g)
                }
            }
            .pickerStyle(.menu)
            .tint(Theme.accent)
            if goal > 0 {
                let _ = bookmark
                let n = QuranPlan.readToday()
                Text(n >= goal ? L("quran.goalDone") : T("quran.todayRead", n, goal))
                    .font(.subheadline).foregroundStyle(n >= goal ? Theme.accent : pal.ink)
                Text(L("quran.goalNote")).font(.caption).foregroundStyle(pal.muted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
