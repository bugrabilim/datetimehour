import SwiftUI
import UIKit
import CoreMotion
import AVFoundation
import SaatCore

/*
 * İbadet bölümü (30 Ekim 2026, sahibin isteği: rakip analizinde "bilerek yok" denenleri yap): Kur'an (Tanzil Arapça
 * metni + İngilizce Pickthall meali, Türkçe için Diyanet meal sitesine bağlantı), dualar, zikirmatik, namaz ve oruç
 * takibi, zekât hesaplama. Vakitler › İbadet. Veriler yalnız telefonda; rozet bilerek yok (inanç ele vermemek için).
 * Android'deki Religion.kt ile aynı anahtarlar ve biçimler (veri: mobile/shared/gen-religion.mjs).
 */

/// Türkçe ve Azericede Diyanet meal bağlantısı, okunuş satırı
private var turkic: Bool { AppLocale.code == "tr" || AppLocale.code == "az" }

private func resourceText(_ name: String, _ ext: String) -> String {
    Bundle.main.url(forResource: name, withExtension: ext).flatMap { try? String(contentsOf: $0, encoding: .utf8) } ?? ""
}

// MARK: - Kur'an

struct QuranSura: Identifiable, Hashable, Sendable {
    let no: Int, ar: String, tname: String, en: String, tr: String, mecca: Bool, ayas: Int, start: Int
    var id: Int { no }
    /// Arayüz dilinde ad
    var title: String {
        switch AppLocale.code {
        case "tr", "az": tr
        case "ar", "fa", "ur": ar
        default: tname
        }
    }
}

struct QuranData: Sendable {
    let suras: [QuranSura]
    /// Cüz başları (sure, ayet)
    let juz: [[Int]]
    let arabic: [String]
    let english: [String]
}

enum Quran {
    static let data: QuranData = {
        struct Meta: Decodable {
            let suras: [[Meta.Field]]
            let juz: [[Int]]
            enum Field: Decodable {
                case s(String), i(Int)
                init(from d: Decoder) throws {
                    let c = try d.singleValueContainer()
                    if let i = try? c.decode(Int.self) { self = .i(i) } else { self = .s(try c.decode(String.self)) }
                }
                var str: String {
                    switch self {
                    case .s(let v): return v
                    case .i: return ""
                    }
                }
                var int: Int {
                    switch self {
                    case .i(let v): return v
                    case .s: return 0
                    }
                }
            }
        }
        let meta = Bundle.main.url(forResource: "quran-meta", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? JSONDecoder().decode(Meta.self, from: $0) }
        let suras = (meta?.suras ?? []).enumerated().map { i, r in
            QuranSura(no: i + 1, ar: r[0].str, tname: r[1].str, en: r[2].str, tr: r[3].str, mecca: r[4].str == "M",
                      ayas: r[5].int, start: r[6].int)
        }
        func lines(_ name: String) -> [String] {
            Array(resourceText(name, "txt").split(separator: "\n", omittingEmptySubsequences: false).prefix(6236).map(String.init))
        }
        return QuranData(suras: suras, juz: meta?.juz ?? [], arabic: lines("quran-ar"), english: lines("quran-en"))
    }()

    static func ar(_ sura: Int, _ aya: Int) -> String { line(data.arabic, sura, aya) }
    static func en(_ sura: Int, _ aya: Int) -> String { line(data.english, sura, aya) }
    private static func line(_ l: [String], _ sura: Int, _ aya: Int) -> String {
        guard sura >= 1, sura <= data.suras.count else { return "" }
        let i = data.suras[sura - 1].start + aya - 1
        return l.indices.contains(i) ? l[i] : ""
    }

    /// Diyanet'in meal sitesinde ayet
    static func diyanetURL(_ sura: Int, _ aya: Int) -> URL {
        URL(string: "https://kuran.diyanet.gov.tr/kuran?sure=\(sura)&ayet=\(aya)&mode=kuran-meal")!
    }

    static let bookmarkKey = "quran.bookmark"
    /// Kaldığım yer "sure:ayet"
    static func parse(_ s: String) -> (Int, Int)? {
        let p = s.split(separator: ":").compactMap { Int($0) }
        return p.count == 2 ? (p[0], p[1]) : nil
    }
}

/// Arapça metin: sağa yaslı, sağdan sola
private struct ArabicText: View {
    let text: String
    let size: CGFloat
    var color: Color = Theme.ink
    var body: some View {
        Text(text)
            .font(.system(size: size))
            .lineSpacing(size * 0.5)
            .foregroundStyle(color)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, .rightToLeft)
    }
}

private struct QuranCredits: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(L("quran.credit")).font(.caption).foregroundStyle(Theme.muted)
            Link("tanzil.net", destination: URL(string: "https://tanzil.net")!).font(.caption)
            Link("kuran.diyanet.gov.tr", destination: URL(string: "https://kuran.diyanet.gov.tr")!).font(.caption)
        }
        .tint(Theme.accent)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 12)
    }
}

/// Kur'an: sure listesi, cüz, kaldığım yerden devam
struct QuranView: View {
    @AppStorage(Quran.bookmarkKey) private var bookmark = ""
    @State private var query = ""

    var body: some View {
        let data = Quran.data
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        let list = data.suras.filter {
            q.isEmpty || $0.tr.lowercased().contains(q) || $0.tname.lowercased().contains(q) || $0.ar.contains(q) || String($0.no) == q
        }
        ScrollView {
            LazyVStack(spacing: 6) {
                HatimCard().padding(.bottom, 4)
                if let mark = Quran.parse(bookmark), mark.0 >= 1, mark.0 <= data.suras.count {
                    NavigationLink {
                        SuraView(sura: mark.0, start: mark.1)
                    } label: {
                        Label(T("quran.continue", data.suras[mark.0 - 1].title, mark.1), systemImage: "bookmark.fill")
                            .foregroundStyle(Theme.text)
                            .card()
                    }
                    .padding(.bottom, 4)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(Array(data.juz.enumerated()), id: \.offset) { i, j in
                            NavigationLink {
                                SuraView(sura: j[0], start: j[1])
                            } label: {
                                Text(T("quran.juz", i + 1))
                                    .font(.subheadline)
                                    .foregroundStyle(Theme.text)
                                    .padding(.horizontal, 10).padding(.vertical, 6)
                                    .background(Theme.surface2, in: Capsule())
                            }
                        }
                    }
                }
                .padding(.bottom, 6)
                ForEach(list) { s in
                    NavigationLink {
                        SuraView(sura: s.no, start: 1)
                    } label: {
                        HStack(spacing: 10) {
                            Text("\(s.no)").monospacedDigit().fontWeight(.semibold).foregroundStyle(Theme.accent)
                                .frame(width: 34, alignment: .leading)
                                .environment(\.layoutDirection, .leftToRight)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(s.title).font(.body.weight(.semibold)).foregroundStyle(Theme.text)
                                Text(T("quran.suraInfo", s.ayas, L(s.mecca ? "quran.mecca" : "quran.medina")))
                                    .font(.caption).foregroundStyle(Theme.muted)
                            }
                            Spacer()
                            Text(s.ar).font(.title3).foregroundStyle(Theme.ink)
                        }
                        .padding(.horizontal, 12).padding(.vertical, 10)
                        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                QuranCredits()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .searchable(text: $query, prompt: L("quran.search"))
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("quran.title"))
        .navigationBarTitleDisplayMode(.large)
    }
}

/// Sure okuma: Arapça metin, isteğe bağlı İngilizce meal, Diyanet meal bağlantısı; ayete dokununca kaldığım yer
struct SuraView: View {
    let sura: Int
    let start: Int
    @AppStorage(Quran.bookmarkKey) private var bookmark = ""
    @AppStorage("quran.size") private var size = 24
    @AppStorage("quran.en") private var showEn = AppLocale.code == "en" || AppLocale.code == "de"
    @AppStorage(QuranPlan.themeKey) private var theme = "app"

    var body: some View {
        let s = Quran.data.suras[max(0, min(sura, Quran.data.suras.count) - 1)]
        let pal = QuranPlan.palette(theme)
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 10) {
                    QuranReadingOptions()
                    Toggle(L("quran.showEn"), isOn: $showEn).tint(Theme.accent).font(.subheadline).foregroundStyle(pal.ink)
                    if turkic {
                        Link(destination: Quran.diyanetURL(sura, 1)) {
                            Label(L("quran.diyanet"), systemImage: "arrow.up.right.square")
                        }
                        .tint(Theme.accent)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    ForEach(1...max(1, s.ayas), id: \.self) { a in
                        let marked = Quran.parse(bookmark).map { $0 == (sura, a) } ?? false
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 6) {
                                Text("\(sura):\(a)").font(.caption.weight(.semibold)).monospacedDigit().foregroundStyle(Theme.accent)
                                    .environment(\.layoutDirection, .leftToRight)
                                if marked {
                                    Image(systemName: "bookmark.fill").font(.caption2).foregroundStyle(Theme.accent)
                                        .accessibilityLabel(L("quran.bookmark"))
                                }
                                Spacer()
                                if turkic {
                                    Link(L("quran.meal"), destination: Quran.diyanetURL(sura, a)).font(.caption).tint(Theme.accent)
                                }
                            }
                            ArabicText(text: Quran.ar(sura, a), size: CGFloat(size), color: pal.ink)
                            if showEn {
                                Text(Quran.en(sura, a)).font(.subheadline).foregroundStyle(pal.muted)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .padding(12)
                        .background(marked ? pal.soft : pal.card, in: RoundedRectangle(cornerRadius: 12))
                        .contentShape(Rectangle())
                        .onTapGesture { QuranPlan.setBookmark(sura, a) }
                        .id(a)
                    }
                    QuranCredits()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .onAppear { if start > 1 { proxy.scrollTo(start, anchor: .top) } }
        }
        .background(pal.bg.ignoresSafeArea())
        .navigationTitle("\(s.no). \(s.title)")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button { size = max(16, size - 2) } label: { Image(systemName: "textformat.size.smaller") }
                    .accessibilityLabel(L("quran.smaller"))
                Button { size = min(40, size + 2) } label: { Image(systemName: "textformat.size.larger") }
                    .accessibilityLabel(L("quran.larger"))
            }
        }
    }
}

// MARK: - Dualar

struct Dua: Identifiable, Sendable {
    let id: String, title: String, when: String, ar: String, okunus: String, mean: String?, refs: [String]
    var cats: [String] = []
}

enum Duas {
    static let all: [Dua] = {
        struct Raw: Decodable {
            let id: String
            let title: [String: String]
            let when: [String: String]
            let ar: String
            let okunus: String?
            let mean: [String: String]
            let refs: [String]?
            let cat: [String]?
        }
        let raw = Bundle.main.url(forResource: "duas", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? JSONDecoder().decode([Raw].self, from: $0) } ?? []
        let lang = AppLocale.code
        return raw.map { r in
            let refs = r.refs ?? []
            // Anlam: arayüz dilinde; Kur'an'dan olanlarda İngilizce meal (tr/az'da Diyanet bağlantısı), Arapça yazılı dillerde (ar, fa, ur) yok
            let mean: String? = if lang == "ar" { nil }
                else if !refs.isEmpty { !["tr", "az", "ar", "fa", "ur"].contains(lang) ? r.mean["en"] : nil }
                else { r.mean[lang] ?? r.mean["en"] }
            return Dua(id: r.id, title: r.title[lang] ?? r.title["en"] ?? r.id, when: r.when[lang] ?? r.when["en"] ?? "",
                       ar: r.ar, okunus: r.okunus ?? "", mean: mean, refs: refs, cats: r.cat ?? [])
        }
    }()

    static let cats = ["prayer", "faith", "quran", "family", "forgiveness", "daily", "ramadan"]
    static let favsKey = "dua.favs"
}

/// Duaları sesle okuma (4 Kasım 2026, sahibin isteği): telefonun kendi konuşma sesiyle Arapçası ve anlamı (uygulamanın
/// dili). İnsan sesi kaydı değildir (kayıtların hakları okuyanlarda; 30 Ekim araştırması). Ses telefonda üretilir.
@MainActor
final class DuaSpeaker: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    static let shared = DuaSpeaker()
    /// Okunan parça: "<dua>-ar" ya da "<dua>-mean"
    @Published private(set) var playing: String?
    private let synth = AVSpeechSynthesizer()

    override init() {
        super.init()
        synth.delegate = self
    }

    static var arabicVoice: AVSpeechSynthesisVoice? {
        AVSpeechSynthesisVoice(language: "ar-SA") ?? AVSpeechSynthesisVoice.speechVoices().first { $0.language.hasPrefix("ar") }
    }

    func toggle(_ key: String, text: String, arabic: Bool) {
        let was = playing
        synth.stopSpeaking(at: .immediate)
        playing = nil
        if was == key { return }
        let u = AVSpeechUtterance(string: text)
        u.voice = arabic ? Self.arabicVoice : NotificationSounds.bestVoice
        u.rate = AVSpeechUtteranceDefaultSpeechRate * (arabic ? 0.8 : 0.95)
        // Sessiz düğmesi açıkken de duyulsun; çalan müzik kısılır, durmaz
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        playing = key
        synth.speak(u)
    }

    func stop() {
        synth.stopSpeaking(at: .immediate)
        playing = nil
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in self.playing = nil }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        Task { @MainActor in if !self.synth.isSpeaking { self.playing = nil } }
    }
}

struct DuasView: View {
    @AppStorage(Duas.favsKey) private var favsRaw = ""
    @State private var query = ""
    @State private var cat = "all"
    @ObservedObject private var speaker = DuaSpeaker.shared

    private var favs: Set<String> { Set(favsRaw.split(separator: ",").map(String.init)) }

    var body: some View {
        let today = CalendarDay(Date())
        // Ramazan'da günün duası: Kur'an'daki dualardan sırayla (2. analiz #69)
        let ramadanDay = CalendarData.shared.ramadans.first { $0.from <= today && today <= $0.to }.map { today.dayNumber - $0.from.dayNumber + 1 }
        let quranDuas = Duas.all.filter { !$0.refs.isEmpty }
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        let list = Duas.all.filter { d in
            (cat == "all" || (cat == "fav" && favs.contains(d.id)) || d.cats.contains(cat)) &&
                (q.isEmpty || [d.title, d.when, d.okunus, d.mean ?? "", d.ar].contains { $0.lowercased().contains(q) })
        }
        ScrollView {
            VStack(spacing: 12) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(["all", "fav"] + Duas.cats, id: \.self) { c in
                            Button { cat = c } label: {
                                Text(L("dua.cat.\(c)")).font(.subheadline)
                                    .foregroundStyle(c == cat ? Color.white : Theme.text)
                                    .padding(.horizontal, 12).padding(.vertical, 7)
                                    .background(c == cat ? Theme.accent : Theme.surface2, in: Capsule())
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(c == cat ? .isSelected : [])
                        }
                    }
                }
                if let n = ramadanDay, !quranDuas.isEmpty, q.isEmpty, cat == "all" {
                    let d = quranDuas[(n - 1) % quranDuas.count]
                    VStack(alignment: .leading, spacing: 8) {
                        Text(T("dua.ramadanDay", n)).font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
                        Text(d.title).font(.headline).foregroundStyle(Theme.text)
                        ArabicText(text: d.ar, size: 22)
                        Text(L("dua.ramadanNote")).font(.caption).foregroundStyle(Theme.muted)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 14))
                }
                if list.isEmpty {
                    Text(L("dua.empty")).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
                }
                ForEach(list) { d in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(d.title).font(.title3.weight(.semibold)).foregroundStyle(Theme.text)
                            Spacer()
                            let fav = favs.contains(d.id)
                            Button { toggleFav(d.id) } label: {
                                Image(systemName: fav ? "star.fill" : "star").foregroundStyle(Theme.accent)
                            }
                            .accessibilityLabel(L(fav ? "dua.unfav" : "dua.fav"))
                        }
                        Text(d.when).font(.caption).foregroundStyle(Theme.muted)
                        ArabicText(text: d.ar, size: 22)
                        if turkic, !d.okunus.isEmpty {
                            Text(d.okunus).font(.subheadline.weight(.medium)).foregroundStyle(Theme.text)
                        }
                        if let m = d.mean { Text(m).font(.subheadline).foregroundStyle(Theme.muted) }
                        // Sesle okuma: Arapçası ve anlamı
                        HStack(spacing: 18) {
                            speakButton("\(d.id)-ar", text: d.ar, arabic: true, title: L("dua.listenAr"))
                                .disabled(DuaSpeaker.arabicVoice == nil)
                            if let m = d.mean { speakButton("\(d.id)-mean", text: m, arabic: false, title: L("dua.listenMean")) }
                        }
                        .font(.subheadline.weight(.medium))
                        if turkic {
                            ForEach(d.refs, id: \.self) { r in
                                if let ref = Quran.parse(r) {
                                    Link(T("dua.diyanetRef", r), destination: Quran.diyanetURL(ref.0, ref.1)).font(.footnote).tint(Theme.accent)
                                }
                            }
                        }
                    }
                    .card()
                }
                Text(L("dua.note")).font(.caption).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(L("dua.voiceNote")).font(.caption).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
        .onDisappear { speaker.stop() }
        .searchable(text: $query, prompt: L("dua.search"))
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("dua.title"))
        .navigationBarTitleDisplayMode(.large)
    }

    private func toggleFav(_ id: String) {
        var f = favs
        if f.contains(id) { f.remove(id) } else { f.insert(id) }
        favsRaw = f.sorted().joined(separator: ",")
    }

    private func speakButton(_ key: String, text: String, arabic: Bool, title: String) -> some View {
        let on = speaker.playing == key
        return Button { speaker.toggle(key, text: text, arabic: arabic) } label: {
            Label(on ? L("dua.stop") : title, systemImage: on ? "stop.circle.fill" : "speaker.wave.2")
        }
        .buttonStyle(.borderless)
        .tint(Theme.accent)
    }
}

// MARK: - Zikirmatik

enum Zikir {
    struct Kind: Identifiable { let id: String; let ar: String }
    static let kinds = [
        Kind(id: "subhanallah", ar: "سُبْحَانَ اللَّهِ"), Kind(id: "elhamdulillah", ar: "الْحَمْدُ لِلَّهِ"),
        Kind(id: "allahuekber", ar: "اللَّهُ أَكْبَرُ"), Kind(id: "tevhid", ar: "لَا إِلَهَ إِلَّا اللَّهُ"),
        Kind(id: "istigfar", ar: "أَسْتَغْفِرُ اللَّهَ"), Kind(id: "salavat", ar: "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"),
        Kind(id: "free", ar: ""),
    ]
    static let targets = [33, 99, 100, 500, 1000, 0]
    /// Tesbihat sırası: her biri 33
    static let tesbihat = ["subhanallah", "elhamdulillah", "allahuekber"]

    static func name(_ id: String) -> String { L("zikir.kind.\(id)") }

    /// Tesbihatta sayaca göre o anki zikir ve bitip bitmediği (99'da biter)
    static func tesbihatStep(_ count: Int) -> (kind: String, done: Bool) {
        (tesbihat[min(count / 33, 2)], count >= 99)
    }
}

/// Kendi zikirleri (`zikir.custom`: [{id, n, a}], en çok 20; 2. analiz #70)
enum ZikirCustom {
    struct Item: Codable, Identifiable, Hashable { let id: String; let n: String; let a: String }
    static let key = "zikir.custom"
    static func load(_ raw: String) -> [Item] { raw.data(using: .utf8).flatMap { try? JSONDecoder().decode([Item].self, from: $0) } ?? [] }
    static func encode(_ l: [Item]) -> String {
        (try? JSONEncoder().encode(Array(l.suffix(20)))).map { String(decoding: $0, as: UTF8.self) } ?? "[]"
    }
}

/// Sallayınca bir geri al (ivmeölçer 2,3 g üstü, 1 sn bekleme)
@MainActor
final class ShakeWatcher: ObservableObject {
    private let motion = CMMotionManager()
    private var last = Date.distantPast
    private var onShake: (() -> Void)?
    func start(_ onShake: @escaping () -> Void) {
        self.onShake = onShake
        guard motion.isAccelerometerAvailable, !motion.isAccelerometerActive else { return }
        motion.accelerometerUpdateInterval = 0.05
        motion.startAccelerometerUpdates(to: .main) { [weak self] data, _ in
            guard let self, let a = data?.acceleration else { return }
            let g = (a.x * a.x + a.y * a.y + a.z * a.z).squareRoot()
            MainActor.assumeIsolated { self.hit(g) }
        }
    }
    private func hit(_ g: Double) {
        if g > 2.3, Date().timeIntervalSince(last) > 1 { last = Date(); onShake?() }
    }
    func stop() { motion.stopAccelerometerUpdates() }
}

struct ZikirView: View {
    @AppStorage("zikir.kind") private var kind = "subhanallah"
    @AppStorage("zikir.target") private var target = 33
    @AppStorage("zikir.tesbihat") private var tesbihat = false
    @AppStorage("zikir.count") private var count = 0
    @AppStorage("zikir.total") private var total = 0
    @AppStorage("zikir.beads") private var beads = false
    @AppStorage("zikir.shake") private var shakeUndo = false
    @AppStorage(ZikirCustom.key) private var customRaw = "[]"
    /// "yyyy-MM-dd|sayı"
    @AppStorage("zikir.today") private var todayRaw = ""
    @State private var confirm = false
    @State private var adding = false
    @State private var newName = ""
    @State private var newAr = ""
    @State private var deleting: ZikirCustom.Item?
    @StateObject private var shake = ShakeWatcher()

    private var todayKey: String { CalendarDay(Date()).iso }
    private var todayCount: Int {
        let p = todayRaw.split(separator: "|")
        return p.count == 2 && p[0] == todayKey ? Int(p[1]) ?? 0 : 0
    }
    private var custom: [ZikirCustom.Item] { ZikirCustom.load(customRaw) }
    private var kinds: [Zikir.Kind] { Zikir.kinds + custom.map { Zikir.Kind(id: $0.id, ar: $0.a) } }
    private func nameOf(_ id: String) -> String { custom.first { $0.id == id }?.n ?? Zikir.name(id) }

    private var step: (kind: String, done: Bool) {
        tesbihat ? Zikir.tesbihatStep(count) : (kind: kind, done: target > 0 && count >= target)
    }

    var body: some View {
        let shownAr = kinds.first { $0.id == step.kind }?.ar ?? ""
        let done = step.done
        ScrollView {
            VStack(spacing: 14) {
                Toggle(L("zikir.tesbihat"), isOn: $tesbihat).tint(Theme.accent)
                    .onChange(of: tesbihat) { count = 0 }
                if !tesbihat {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(kinds) { k in
                                Button { kind = k.id; count = 0 } label: {
                                    Text(nameOf(k.id))
                                        .font(.subheadline)
                                        .foregroundStyle(k.id == kind ? Theme.accent : Theme.text)
                                        .padding(.horizontal, 12).padding(.vertical, 7)
                                        .background(k.id == kind ? Theme.accentSoft : Theme.surface2, in: Capsule())
                                }
                                .contextMenu {
                                    if let item = custom.first(where: { $0.id == k.id }) {
                                        Button(L("zikir.delete"), role: .destructive) { deleting = item }
                                    }
                                }
                            }
                            Button { newName = ""; newAr = ""; adding = true } label: {
                                Text("+ " + L("zikir.add")).font(.subheadline).foregroundStyle(Theme.accent)
                                    .padding(.horizontal, 12).padding(.vertical, 7)
                                    .background(Theme.surface2, in: Capsule())
                            }
                        }
                    }
                    Picker(L("zikir.target"), selection: $target) {
                        ForEach(Zikir.targets, id: \.self) { t in
                            Text(t == 0 ? L("zikir.noTarget") : "\(t)").tag(t)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(Theme.accent)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                if !shownAr.isEmpty {
                    Text(shownAr).font(.system(size: 30)).foregroundStyle(Theme.ink).multilineTextAlignment(.center)
                }
                Text(nameOf(step.kind)).foregroundStyle(Theme.muted)
                // Büyük dokunma alanı: dokun = say, uzun bas = geri al, sağa/sola kaydır = zikir değiştir
                ZStack {
                    if beads {
                        Canvas { ctx, size in
                            let n = 33, r = min(size.width, size.height) / 2 * 0.86
                            let c = CGPoint(x: size.width / 2, y: size.height / 2)
                            let k = count % n
                            for i in 0..<n {
                                let a = (-90 + Double(i) * 360 / Double(n)) * .pi / 180
                                let p = CGPoint(x: c.x + r * CGFloat(cos(a)), y: c.y + r * CGFloat(sin(a)))
                                let br: CGFloat = i == k ? 13 : 9
                                let on = i < k || (done && k == 0 && count > 0)
                                ctx.fill(Path(ellipseIn: CGRect(x: p.x - br, y: p.y - br, width: 2 * br, height: 2 * br)),
                                         with: .color(on ? Theme.accent : Theme.surface2))
                            }
                            ctx.fill(Path(ellipseIn: CGRect(x: c.x - 7, y: c.y - r - 23, width: 14, height: 14)), with: .color(Theme.accent.opacity(0.8)))
                        }
                    } else {
                        Circle().fill(done ? Theme.accent : Theme.accentSoft)
                    }
                    VStack(spacing: 4) {
                        Text("\(count)").font(.system(size: 64, weight: .light)).monospacedDigit()
                            .foregroundStyle(done && !beads ? Color.white : Theme.ink)
                        let goal = tesbihat ? 99 : target
                        if goal > 0 {
                            Text("/ \(goal)").monospacedDigit().foregroundStyle(done && !beads ? Color.white : Theme.muted)
                        }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                }
                .frame(width: 260, height: 260)
                .contentShape(Circle())
                .onTapGesture(perform: tap)
                .onLongPressGesture(perform: undo)
                .gesture(DragGesture(minimumDistance: 30).onEnded { v in
                    if abs(v.translation.width) > 80 { switchKind(v.translation.width < 0 ? 1 : -1) }
                })
                .accessibilityElement(children: .ignore)
                .accessibilityAddTraits(.isButton)
                .accessibilityLabel(L("zikir.tap"))
                .accessibilityValue("\(count)")
                .accessibilityAction { tap() }
                .accessibilityAction(named: L("zikir.undo")) { undo() }
                if done { Text(L("zikir.done")).fontWeight(.semibold).foregroundStyle(Theme.accent) }
                HStack(spacing: 12) {
                    Button(L("zikir.undo"), action: undo).buttonStyle(.bordered)
                    Button(L("zikir.reset")) { confirm = true }.buttonStyle(.bordered)
                }
                .tint(Theme.accent)
                Text(L("zikir.undoHint")).font(.caption).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                Toggle(L("zikir.beads"), isOn: $beads).tint(Theme.accent)
                Toggle(L("zikir.shakeUndo"), isOn: $shakeUndo).tint(Theme.accent)
                VStack(spacing: 8) {
                    row(L("zikir.today"), todayCount)
                    row(L("zikir.total"), total)
                }
                .card()
                Text(L("zikir.note")).font(.caption).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("zikir.title"))
        .navigationBarTitleDisplayMode(.large)
        .onAppear { if shakeUndo { shake.start(undo) } }
        .onDisappear { shake.stop() }
        .onChange(of: shakeUndo) { if shakeUndo { shake.start(undo) } else { shake.stop() } }
        .confirmationDialog(L("zikir.resetConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("zikir.reset"), role: .destructive) { count = 0 }
        }
        .confirmationDialog(deleting?.n ?? "", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible) {
            Button(L("zikir.delete"), role: .destructive) {
                if let d = deleting {
                    customRaw = ZikirCustom.encode(custom.filter { $0.id != d.id })
                    if kind == d.id { kind = "subhanallah"; count = 0 }
                }
                deleting = nil
            }
        }
        .alert(L("zikir.add"), isPresented: $adding) {
            TextField(L("zikir.name"), text: $newName)
            TextField(L("zikir.arabic"), text: $newAr)
            Button(L("zikir.add")) {
                let name = newName.trimmingCharacters(in: .whitespaces)
                guard !name.isEmpty else { return }
                let item = ZikirCustom.Item(id: "c\(Int(Date().timeIntervalSince1970 * 1000))", n: String(name.prefix(60)),
                                            a: String(newAr.trimmingCharacters(in: .whitespaces).prefix(120)))
                customRaw = ZikirCustom.encode(custom + [item])
                kind = item.id; count = 0
            }
            Button(L("badges.deleteNo"), role: .cancel) {}
        }
    }

    private func row(_ title: String, _ n: Int) -> some View {
        HStack {
            Text(title).foregroundStyle(Theme.text)
            Spacer()
            Text(num(n)).monospacedDigit().foregroundStyle(Theme.text)
        }
    }

    private func setToday(_ n: Int) { todayRaw = "\(todayKey)|\(n)" }

    private func switchKind(_ dir: Int) {
        guard !tesbihat else { return }
        let ids = kinds.map(\.id)
        let i = ids.firstIndex(of: kind) ?? 0
        kind = ids[(i + dir + ids.count) % ids.count]; count = 0
        UISelectionFeedbackGenerator().selectionChanged()
    }

    private func undo() {
        guard count > 0 else { return }
        count -= 1; total = max(0, total - 1); setToday(max(0, todayCount - 1))
        UIImpactFeedbackGenerator(style: .rigid).impactOccurred()
    }

    private func tap() {
        count += 1; total += 1; setToday(todayCount + 1)
        let finished = step.done
        let stepEnd = tesbihat && count % 33 == 0
        if finished || stepEnd {
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        } else {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        }
    }
}

// MARK: - Namaz ve oruç takibi

enum Tracker {
    static let prayers = ["fajr", "dhuhr", "asr", "maghrib", "isha"]
    static let kaza = prayers + ["witr"]

    private static func obj(_ key: String) -> [String: Any] {
        UserDefaults.standard.string(forKey: key)
            .flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
    }

    private static func put(_ key: String, _ o: [String: Any]) {
        if let d = try? JSONSerialization.data(withJSONObject: o, options: [.sortedKeys]) {
            UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: key)
        }
    }

    static func dayMask(_ d: CalendarDay) -> Int { (obj("track.prayer")[d.iso] as? NSNumber)?.intValue ?? 0 }
    static func setDayMask(_ d: CalendarDay, _ mask: Int) {
        var o = obj("track.prayer")
        if mask == 0 { o.removeValue(forKey: d.iso) } else { o[d.iso] = mask }
        put("track.prayer", o)
    }

    static func kaza(_ id: String) -> Int { (obj("track.kaza")[id] as? NSNumber)?.intValue ?? 0 }
    static func setKaza(_ id: String, _ n: Int) {
        var o = obj("track.kaza")
        o[id] = max(0, n)
        put("track.kaza", o)
    }

    static func fasted(_ d: CalendarDay) -> Bool { (obj("track.fast")[d.iso] as? NSNumber)?.boolValue ?? false }
    static func setFasted(_ d: CalendarDay, _ on: Bool) {
        var o = obj("track.fast")
        if on { o[d.iso] = true } else { o.removeValue(forKey: d.iso) }
        put("track.fast", o)
    }

    /// Son 7 günde kılınan vakit sayısı
    static func weekCount(_ today: CalendarDay) -> Int {
        (0...6).reduce(0) { $0 + dayMask(today.adding(days: -$1)).nonzeroBitCount }
    }

    static func clear() {
        for k in ["track.prayer", "track.kaza", "track.fast"] { UserDefaults.standard.removeObject(forKey: k) }
        TrackX.clearExtras()
    }
}

struct TrackerView: View {
    @AppStorage(TrackX.inlineKey) private var inline = true
    @State private var day = CalendarDay(Date())
    @State private var version = 0
    @State private var confirm = false
    private let today = CalendarDay(Date())

    var body: some View { bodyContent.screenshotWarning() }

    @ViewBuilder private var bodyContent: some View {
        let ramadans = CalendarData.shared.ramadans
        let ramadan = ramadans.first { $0.from <= today && today <= $0.to }
            ?? ramadans.last { $0.to < today } ?? ramadans.first
        ScrollView {
            VStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Button { day = day.adding(days: -1) } label: { Image(systemName: "chevron.backward") }
                            .accessibilityLabel(L("stage.prev"))
                        Text(Fmt.day(day)).fontWeight(.semibold).foregroundStyle(Theme.text)
                            .frame(maxWidth: .infinity)
                        Button { if day < today { day = day.adding(days: 1) } } label: { Image(systemName: "chevron.forward") }
                            .disabled(day >= today)
                            .accessibilityLabel(L("stage.next"))
                    }
                    .tint(Theme.accent)
                    .padding(.bottom, 6)
                    let mask = Tracker.dayMask(day)
                    ForEach(Array(Tracker.prayers.enumerated()), id: \.offset) { i, p in
                        let on = mask & (1 << i) != 0
                        Button {
                            Tracker.setDayMask(day, mask ^ (1 << i)); version += 1
                        } label: {
                            HStack(spacing: 10) {
                                Image(systemName: on ? "checkmark.square.fill" : "square")
                                    .font(.title3).foregroundStyle(on ? Theme.accent : Theme.muted)
                                Text(L("track.prayer.\(p)")).foregroundStyle(Theme.text)
                                Spacer()
                            }
                            .padding(.vertical, 5)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(on ? .isSelected : [])
                    }
                    Text(T("track.week", Tracker.weekCount(today))).font(.footnote).foregroundStyle(Theme.muted)
                    PauseRow(version: $version).padding(.top, 6)
                    Toggle(L("track.inline"), isOn: $inline).tint(Theme.accent)
                    Text(L("track.inlineNote")).font(.caption).foregroundStyle(Theme.muted)
                }
                .card()
                TrackerStatsCard(today: today, version: version)
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("track.kaza")).font(.headline).foregroundStyle(Theme.text)
                    ForEach(Tracker.kaza, id: \.self) { k in
                        counter(L("track.prayer.\(k)"), id: k)
                    }
                    KazaPace()
                    Text(L("track.kazaNote")).font(.caption).foregroundStyle(Theme.muted)
                }
                .card()
                LifetimeKazaCard { version += 1 }
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("track.fast")).font(.headline).foregroundStyle(Theme.text)
                    if let ramadan {
                        let days = ramadan.days.filter { $0 <= today }
                        if days.isEmpty {
                            Text(T("track.fastUpcoming", Fmt.day(ramadan.from))).font(.subheadline).foregroundStyle(Theme.muted)
                        } else {
                            Text(T("track.fastCount", days.filter(Tracker.fasted).count, days.count)).foregroundStyle(Theme.text)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 4) {
                                    ForEach(Array(days.enumerated()), id: \.offset) { i, d in
                                        let on = Tracker.fasted(d)
                                        Button { Tracker.setFasted(d, !on); version += 1 } label: {
                                            Text("\(i + 1)").font(.caption).monospacedDigit()
                                                .foregroundStyle(on ? Color.white : Theme.text)
                                                .frame(width: 34, height: 34)
                                                .background(on ? Theme.accent : Theme.surface2, in: Circle())
                                        }
                                        .buttonStyle(.plain)
                                        .accessibilityLabel(Fmt.day(d))
                                        .accessibilityAddTraits(on ? .isSelected : [])
                                    }
                                }
                            }
                            .environment(\.layoutDirection, .leftToRight)
                        }
                    }
                    FastKazaRows(version: $version)
                }
                .card()
                Text(L("track.privacy")).font(.caption).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button(L("track.clear"), role: .destructive) { confirm = true }
            }
            .padding(16)
            .id(version)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .onAppear { TrackX.migrate(); version += 1 }
        .navigationTitle(L("track.title"))
        .navigationBarTitleDisplayMode(.large)
        .confirmationDialog(L("track.clearConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("track.clear"), role: .destructive) { Tracker.clear(); version += 1 }
        }
    }

    private func counter(_ title: String, id: String) -> some View {
        HStack {
            Text(title).foregroundStyle(Theme.text)
            Spacer()
            Button {
                if Tracker.kaza(id) > 0 { Tracker.setKaza(id, Tracker.kaza(id) - 1); TrackX.logKazaPrayed() }
                version += 1
            } label: { Image(systemName: "minus.circle") }
                .accessibilityLabel(L("track.kazaPrayed"))
            Text("\(Tracker.kaza(id))").font(.body.weight(.semibold)).monospacedDigit().frame(minWidth: 44)
                .foregroundStyle(Theme.text)
                .environment(\.layoutDirection, .leftToRight)
            Button { Tracker.setKaza(id, Tracker.kaza(id) + 1); version += 1 } label: { Image(systemName: "plus.circle") }
                .accessibilityLabel(L("track.kazaAdd"))
        }
        .font(.title3)
        .tint(Theme.accent)
        .buttonStyle(.borderless)
    }
}

// MARK: - Zekât

/// Nisab 80,18 g altın (Diyanet), oran 1/40; altın fiyatını kullanıcı girer (dış servis yok)
enum Zakat {
    static let nisabGrams = 80.18
    static let fields = ["cash", "goldGrams", "goldPrice", "silverValue", "foreign", "trade", "receivable", "debt"]

    struct Result { let net: Double; let nisab: Double?; let due: Bool; let zakat: Double }

    static func calc(_ v: [String: Double]) -> Result {
        func g(_ k: String) -> Double { v[k] ?? 0 }
        let goldPrice = g("goldPrice")
        let assets = g("cash") + g("goldGrams") * goldPrice + g("silverValue") + g("foreign") + g("trade") + g("receivable")
        let net = max(0, assets - g("debt"))
        let nisab: Double? = goldPrice > 0 ? nisabGrams * goldPrice : nil
        let due = nisab.map { net >= $0 } ?? false
        return Result(net: net, nisab: nisab, due: due, zakat: due ? net / 40 : 0)
    }

    static func load() -> [String: Double] {
        let o = UserDefaults.standard.string(forKey: "zakat.values")
            .flatMap { $0.data(using: .utf8) }
            .flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
        return o.compactMapValues { ($0 as? NSNumber)?.doubleValue }
    }

    static func save(_ v: [String: Double]) {
        if let d = try? JSONSerialization.data(withJSONObject: v, options: [.sortedKeys]) {
            UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: "zakat.values")
        }
    }

    /// Sayı, uygulama dilinde (sağdan sola dillerde Latin rakamı)
    static func format(_ x: Double, fraction: Int = 2) -> String {
        let f = NumberFormatter()
        f.locale = AppLocale.numberLocale
        f.numberStyle = .decimal
        f.minimumFractionDigits = fraction
        f.maximumFractionDigits = fraction
        return f.string(from: NSNumber(value: x)) ?? String(x)
    }

    /// Kullanıcının yazdığı sayı ("1.234,5" ya da "1234.5")
    static func parse(_ s: String) -> Double? {
        let t = s.trimmingCharacters(in: .whitespaces)
        guard !t.isEmpty else { return nil }
        return Double(t.replacingOccurrences(of: ",", with: "."))
    }
}

/// Fitre tutarı (gen-strings, src/data/fitre.json): en son açıklanan yıl
enum Fitre {
    static let info: (year: Int, amount: Int, date: CalendarDay)? = {
        guard let url = Bundle.main.url(forResource: "fitre", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let o = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let y = o["year"] as? Int, let a = o["amount"] as? Int, let d = o["date"] as? String else { return nil }
        let p = d.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3 else { return nil }
        return (y, a, CalendarDay(p[0], p[1], p[2]))
    }()
}

struct ZakatView: View {
    @State private var fitrePeople = 1
    @State private var texts: [String: String] = Zakat.load().mapValues { v in
        v == v.rounded() ? String(Int64(v)) : String(v)
    }

    var body: some View { bodyContent.screenshotWarning() }

    @ViewBuilder private var bodyContent: some View {
        let values = texts.compactMapValues(Zakat.parse)
        let r = Zakat.calc(values)
        Form {
            Section {
                ForEach(Zakat.fields, id: \.self) { f in
                    LabeledContent(L("zakat.f.\(f)")) {
                        TextField("0", text: binding(f))
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .monospacedDigit()
                    }
                }
            } footer: {
                Text(L("zakat.intro"))
            }
            Section {
                LabeledContent(L("zakat.net")) { Text(Zakat.format(r.net)).monospacedDigit().fontWeight(.semibold) }
                LabeledContent(T("zakat.nisab", Zakat.format(Zakat.nisabGrams))) {
                    Text(r.nisab.map { Zakat.format($0) } ?? "–").monospacedDigit()
                }
                Text(r.nisab == nil ? L("zakat.needGold") : r.due ? L("zakat.due") : L("zakat.notDue"))
                    .font(.subheadline)
                    .foregroundStyle(r.due ? Theme.accent : Theme.muted)
                if r.due {
                    LabeledContent(L("zakat.amount")) {
                        Text(Zakat.format(r.zakat)).monospacedDigit().fontWeight(.bold).foregroundStyle(Theme.accent)
                    }
                }
            } footer: {
                Text(L("zakat.note"))
            }
            // Fitre (rakip analizi #14): Diyanet'in açıkladığı kişi başı tutar (fitre.json, sitedeki src/data/fitre.json)
            if let f = Fitre.info {
                Section {
                    Text(T("zakat.fitreText", f.year, f.amount, Fmt.day(f.date))).font(.subheadline)
                    Stepper("\(L("zakat.fitrePeople")): \(fitrePeople)", value: $fitrePeople, in: 1...99)
                    LabeledContent(L("zakat.fitreTotal")) {
                        Text(Zakat.format(Double(f.amount * fitrePeople)) + " TL").monospacedDigit().fontWeight(.bold).foregroundStyle(Theme.accent)
                    }
                } header: {
                    Text(L("zakat.fitreHeading"))
                } footer: {
                    Text(L("zakat.fitreNote"))
                }
            }
        }
        .themedBackground()
        .navigationTitle(L("zakat.title"))
        .navigationBarTitleDisplayMode(.large)
    }

    private func binding(_ f: String) -> Binding<String> {
        Binding(
            get: { texts[f] ?? "" },
            set: { s in
                texts[f] = String(s.filter { $0.isNumber || $0 == "," || $0 == "." }.prefix(14))
                Zakat.save(texts.compactMapValues(Zakat.parse))
            }
        )
    }
}

/// Vakitler sekmesindeki "İbadet" bölümü
/// Vakitler'deki ibadet satırı (2. analiz #199): dört kare (Kıble, Kur'an, Zikirmatik, Takip) ve "Tümü"
struct WorshipRow: View {
    let showAll: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(L("worship.title")).font(.headline).foregroundStyle(Theme.text)
                Spacer()
                Button(L("countdowns.all") + " ›", action: showAll).foregroundStyle(Theme.accent)
            }
            HStack(spacing: 10) {
                tile(L("qibla.title"), "location.north.circle") { QiblaView() }
                tile(L("quran.title"), "book") { QuranView() }
                tile(L("zikir.title"), "hand.tap") { ZikirView() }
                tile(L("track.title"), "checklist") { TrackerView() }
            }
        }
    }

    private func tile<D: View>(_ title: String, _ icon: String, @ViewBuilder _ dest: @escaping () -> D) -> some View {
        NavigationLink { dest() } label: {
            VStack(spacing: 6) {
                IconBox(icon: icon)
                Text(title).font(.caption).foregroundStyle(Theme.text).lineLimit(2).multilineTextAlignment(.center)
                    .minimumScaleFactor(0.8)
            }
            .padding(6)
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .background(RoundedRectangle(cornerRadius: 16).fill(Theme.surface))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.border.opacity(0.5)))
        }
        .buttonStyle(.plain)
    }
}

/// İbadet sayfası: bütün ibadet bölümleri (Vakitler › İbadet › Tümü)
struct WorshipLinks: View {
    var body: some View {
        ScrollView {
        VStack(alignment: .leading, spacing: 10) {
            link(L("qibla.link"), "location.north.circle") { QiblaView() }
            link(L("quran.title"), "book") { QuranView() }
            link(L("dua.title"), "hands.sparkles") { DuasView() }
            link(L("esma.title"), "sparkles") { EsmaView() }
            link(L("hadith.title"), "text.quote") { HadithView() }
            link(L("guide.title"), "figure.mind.and.body") { PrayerGuideView() }
            link(L("zikir.title"), "hand.tap") { ZikirView() }
            link(L("track.title"), "checklist") { TrackerView() }
            link(L("relday.title"), "calendar.badge.clock") { ReligiousDaysView() }
            link(L("greet.title"), "gift") { GreetingView() }
            link(L("zakat.title"), "percent") { ZakatView() }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 20)
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
    }

    private func link<D: View>(_ title: String, _ icon: String, @ViewBuilder _ dest: @escaping () -> D) -> some View {
        NavigationLink {
            dest()
        } label: {
            HStack {
                BoxLabel(title, icon: icon)
                Spacer()
                Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
            }
            .card()
        }
    }
}
