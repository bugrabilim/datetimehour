import SwiftUI

/// "Tümü" sayfası (Seçenek B, 2. analiz #195–#197; Android AllScreen ile aynı kimlikler ve kayıtlar):
/// üstte genel arama (araç, ayar, içerik), 4 karelik sık kullanılanlar (uzun basınca menüden ekle/çıkar; boşsa son
/// kullanılanlar), sonra kategoriler (Süre ve alarm, Gün ve plan, Gök ve dünya, İbadet), sonda Ayarlar ve Rozetler
/// (Ayarlar ayrıca sağ üstteki dişliden; 4 Kasım 2026'dan beri saat ekranında dişli yok, Tümü her zaman çubukta). Yalnız açık özelliklerin girişleri görünür. Kayıt: `all.favs`, `all.recent` (virgülle kimlik).
enum AllCat: CaseIterable {
    case time, plan, sky, faith, more

    /// Kategori rengi (doğal tasarım madde 4): zaman kiremit, plan adaçayı, gök mavi, ibadet altın, diğer akşam moru
    var color: Color {
        switch self {
        case .time: Theme.terra
        case .plan: Theme.sage
        case .sky: Theme.sky
        case .faith: Theme.gold
        case .more: Theme.dusk
        }
    }

    var title: String {
        switch self {
        case .time: L("all.cat.time")
        case .plan: L("all.cat.plan")
        case .sky: L("all.cat.sky")
        case .faith: L("worship.title")
        case .more: L("tab.more")
        }
    }
}

/// Bir giriş: `tab` doluysa o özellik sekmesi açılır (çubukta değilse geri düğmeli sayfa), değilse Araçlar yolu `route`
struct AllEntry: Identifiable, Hashable {
    let id: String
    let titleKey: String
    let icon: String
    let cat: AllCat
    let feature: AppTab?
    var tab: AppTab?
    var route: ToolRoute?
    var words: [String] = []

    var title: String { L(titleKey) }
}

enum AllPrefs {
    static let favsKey = "all.favs"
    static let recentKey = "all.recent"
    static let max = 4

    static let entries: [AllEntry] = [
        AllEntry(id: "alarm", titleKey: "tools.hub.cards.alarm.name", icon: "alarm", cat: .time, feature: .tools, route: .alarm),
        AllEntry(id: "timer", titleKey: "tools.timer", icon: "timer", cat: .time, feature: .tools, route: .timer,
                 words: ["countdowns.heading", "hiit.tab"]),
        AllEntry(id: "stopwatch", titleKey: "tools.hub.cards.stopwatch.name", icon: "stopwatch", cat: .time, feature: .tools, route: .stopwatch),
        AllEntry(id: "pomodoro", titleKey: "tools.hub.cards.pomodoro.name", icon: "leaf", cat: .time, feature: .tools, route: .pomodoro),
        AllEntry(id: "study", titleKey: "study.title", icon: "graduationcap", cat: .time, feature: .tools, route: .study),
        AllEntry(id: "sleep", titleKey: "sleep.title", icon: "bed.double", cat: .time, feature: .tools, route: .sleep),
        AllEntry(id: "wakeGame", titleKey: "wakegame.title", icon: "bird", cat: .time, feature: .tools, route: .wakeGame),
        AllEntry(id: "breath", titleKey: "breath.title", icon: "wind", cat: .time, feature: .tools, route: .breath),
        // Sitedeki araçlar (4 Kasım 2026; Android'deki kimliklerle)
        AllEntry(id: "chess", titleKey: "chess.h1", icon: "checkerboard.rectangle", cat: .time, feature: .tools, route: .chess),
        AllEntry(id: "metronome", titleKey: "metronome.h1", icon: "metronome", cat: .time, feature: .tools, route: .metronome, words: ["metronome.bpm"]),
        AllEntry(id: "talking", titleKey: "talking.h1", icon: "speaker.wave.2", cat: .time, feature: .tools, route: .talking),
        AllEntry(id: "exam", titleKey: "examTimer.h1", icon: "pencil.and.list.clipboard", cat: .time, feature: .tools, route: .exam),
        AllEntry(id: "classroom", titleKey: "classroom.h1", icon: "person.3", cat: .time, feature: .tools, route: .classroom,
                 words: ["classroom.tabPicker", "classroom.tabTeams", "classroom.tabCounter"]),
        AllEntry(id: "timecalc", titleKey: "timeCalc.h1", icon: "clock.badge.questionmark", cat: .time, feature: .tools, route: .timecalc),
        AllEntry(id: "calendar", titleKey: "tab.calendar", icon: "calendar", cat: .plan, feature: .calendar, tab: .calendar),
        AllEntry(id: "leaf", titleKey: "leaf.title", icon: "doc.text", cat: .plan, feature: .calendar, route: .leaf),
        AllEntry(id: "datecalc", titleKey: "tools.hub.cards.datecalc.name", icon: "calendar.badge.clock", cat: .plan, feature: .tools, route: .datecalc),
        AllEntry(id: "planner", titleKey: "planner.h1", icon: "person.2.wave.2", cat: .plan, feature: .tools, route: .planner),
        AllEntry(id: "journal", titleKey: "journal.title", icon: "sun.horizon", cat: .plan, feature: .tools, route: .journal),
        AllEntry(id: "bridge", titleKey: "bridge.h1", icon: "beach.umbrella", cat: .plan, feature: .tools, route: .bridge),
        AllEntry(id: "retire", titleKey: "retire.h1", icon: "calendar.badge.clock", cat: .plan, feature: .tools, route: .retire),
        AllEntry(id: "weather", titleKey: "tab.weather", icon: "cloud.sun", cat: .sky, feature: .weather, tab: .weather),
        AllEntry(id: "world", titleKey: "tab.world", icon: "globe.europe.africa", cat: .sky, feature: .world, tab: .world),
        AllEntry(id: "seasons", titleKey: "tab.seasons", icon: "leaf", cat: .sky, feature: .seasons, tab: .seasons),
        AllEntry(id: "moon", titleKey: "moon.calendarLink", icon: "moon", cat: .sky, feature: .seasons, route: .moon),
        AllEntry(id: "sky", titleKey: "sky.h1", icon: "sparkles", cat: .sky, feature: .seasons, route: .sky),
        AllEntry(id: "sunpath", titleKey: "sunpath.title", icon: "sun.horizon", cat: .sky, feature: .seasons, route: .sunpath),
        AllEntry(id: "zodiac", titleKey: "zodiac.h1", icon: "sparkles", cat: .sky, feature: .tools, route: .zodiac),
        AllEntry(id: "prayer", titleKey: "tab.prayer", icon: "moon.stars", cat: .faith, feature: .prayer, tab: .prayer),
        AllEntry(id: "qibla", titleKey: "qibla.title", icon: "location.north.line", cat: .faith, feature: .prayer, route: .qibla),
        AllEntry(id: "quran", titleKey: "quran.title", icon: "book", cat: .faith, feature: .prayer, route: .quran),
        AllEntry(id: "zikir", titleKey: "zikir.title", icon: "hand.tap", cat: .faith, feature: .prayer, route: .zikir),
        AllEntry(id: "track", titleKey: "track.title", icon: "checkmark.circle", cat: .faith, feature: .prayer, route: .track),
        AllEntry(id: "duas", titleKey: "dua.title", icon: "hands.sparkles", cat: .faith, feature: .prayer, route: .duas),
        AllEntry(id: "esma", titleKey: "esma.title", icon: "sparkles", cat: .faith, feature: .prayer, route: .esma),
        AllEntry(id: "hadith", titleKey: "hadith.title", icon: "quote.opening", cat: .faith, feature: .prayer, route: .hadith),
        AllEntry(id: "guide", titleKey: "guide.title", icon: "figure.mind.and.body", cat: .faith, feature: .prayer, route: .guide),
        AllEntry(id: "reldays", titleKey: "relday.title", icon: "calendar.badge.exclamationmark", cat: .faith, feature: .prayer, route: .reldays),
        AllEntry(id: "greet", titleKey: "greet.title", icon: "gift", cat: .faith, feature: .prayer, route: .greet),
        AllEntry(id: "zakat", titleKey: "zakat.title", icon: "percent", cat: .faith, feature: .prayer, route: .zakat),
        AllEntry(id: "settings", titleKey: "settings.title", icon: "gearshape", cat: .more, feature: nil, route: .settings,
                 words: ["settings.color", "lang.title", "settings.notifications"]),
        AllEntry(id: "badges", titleKey: "badges.h1", icon: "rosette", cat: .more, feature: nil, route: .badges),
    ]

    /// Aramada ayar sayfaları da çıkar
    static let settings: [AllEntry] = [
        AllEntry(id: "s:settings", titleKey: "settings.title", icon: "gearshape", cat: .more, feature: nil, route: .settings),
        AllEntry(id: "s:appearance", titleKey: "look.title", icon: "clock", cat: .more, feature: nil, route: .appearance),
        AllEntry(id: "s:color", titleKey: "settings.color", icon: "paintpalette", cat: .more, feature: nil, route: .color),
        AllEntry(id: "s:dials", titleKey: "dial.title", icon: "circle.dashed", cat: .more, feature: nil, route: .dials),
        AllEntry(id: "s:widgets", titleKey: "widgets.gallery", icon: "square.grid.2x2", cat: .more, feature: nil, route: .widgets),
        AllEntry(id: "s:homeItems", titleKey: "home.customize", icon: "list.bullet", cat: .more, feature: nil, route: .homeItems),
        AllEntry(id: "s:features", titleKey: "features.title", icon: "square.grid.3x3", cat: .more, feature: nil, route: .features,
                 words: ["features.barTitle"]),
        AllEntry(id: "s:notifications", titleKey: "settings.notifications", icon: "bell", cat: .more, feature: nil, route: .notifications),
        AllEntry(id: "s:language", titleKey: "lang.title", icon: "globe", cat: .more, feature: nil, route: .language),
        AllEntry(id: "s:about", titleKey: "settings.about", icon: "info.circle", cat: .more, feature: nil, route: .about),
        AllEntry(id: "s:shareQr", titleKey: "support.qr", icon: "qrcode", cat: .more, feature: nil, route: .shareQr),
    ]

    /// Emeklilik hesabı yalnız Türkçe arayüzde (Türkiye mevzuatı, metni Türkçe)
    static func visible(_ e: AllEntry) -> Bool { (e.feature.map(Features.isOn) ?? true) && (e.id != "retire" || AppLocale.code == "tr") }

    static func byID(_ id: String) -> AllEntry? { (entries + settings).first { $0.id == id } }

    static func list(_ raw: String) -> [String] { raw.split(separator: ",").map(String.init) }

    /// Ekler ya da çıkarır; 4 doluysa nil
    static func toggleFav(_ id: String) -> Bool? {
        var v = list(UserDefaults.standard.string(forKey: favsKey) ?? "")
        if let i = v.firstIndex(of: id) { v.remove(at: i) } else if v.count >= max { return nil } else { v.append(id) }
        UserDefaults.standard.set(v.joined(separator: ","), forKey: favsKey)
        return v.contains(id)
    }

    static func used(_ id: String) {
        let old = list(UserDefaults.standard.string(forKey: recentKey) ?? "").filter { $0 != id }
        UserDefaults.standard.set(([id] + old).prefix(max).joined(separator: ","), forKey: recentKey)
    }

    /// Arama: ad, kategori ve ek sözcükler; aksan ve büyük harf duyarsız (District.fold)
    static func search(_ q: String) -> [(AllEntry, String)] {
        let words = District.fold(q.trimmingCharacters(in: .whitespaces)).split(separator: " ").map(String.init)
        guard !words.isEmpty else { return [] }
        func hit(_ text: String) -> Bool { let t = District.fold(text); return words.allSatisfy { t.contains($0) } }
        var out: [(AllEntry, String)] = []
        for e in entries.filter(visible) + settings {
            let hay = ([e.title, e.cat.title] + e.words.map { L($0) }).joined(separator: " ")
            if hit(hay) { out.append((e, e.cat.title)) }
        }
        // İçerik: sure adları (Kur'an o sureyle açılır)
        if Features.isOn(.prayer) {
            for s in Quran.data.suras where hit("\(s.no). \(s.title)") {
                out.append((AllEntry(id: "quran", titleKey: "quran.title", icon: "book", cat: .faith, feature: .prayer,
                                     route: .sura(s.no)), "\(s.no). \(s.title)"))
            }
        }
        return out
    }
}

struct AllView: View {
    @State private var router = Router.shared
    @AppStorage(AllPrefs.favsKey) private var favsRaw = ""
    @AppStorage(AllPrefs.recentKey) private var recentRaw = ""
    @AppStorage(Features.key) private var features = ""
    @Environment(\.horizontalSizeClass) private var hsc
    @Environment(\.colorScheme) private var scheme
    @State private var query = ""
    @State private var toast: String?

    private var columns: [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: 8), count: hsc == .regular ? 6 : 4)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                if query.trimmingCharacters(in: .whitespaces).isEmpty {
                    home
                } else {
                    results
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: L("all.search"))
        .themedBackground()
        .navigationTitle(L("tab.all"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { router.toolsPath.append(.settings) } label: { Image(systemName: "gearshape") }
                    .accessibilityLabel(L("settings.title"))
            }
        }
        .overlay(alignment: .bottom) {
            if let t = toast {
                Text(t)
                    .font(.subheadline)
                    .foregroundStyle(Theme.bg)
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(Capsule().fill(Theme.text.opacity(0.9)))
                    .padding(16)
                    .transition(.opacity)
            }
        }
        .task(id: toast) {
            guard toast != nil else { return }
            try? await Task.sleep(for: .seconds(2.2))
            toast = nil
        }
    }

    @ViewBuilder private var home: some View {
        let favs = AllPrefs.list(favsRaw).compactMap(AllPrefs.byID).filter(AllPrefs.visible)
        let recent = AllPrefs.list(recentRaw).compactMap(AllPrefs.byID).filter(AllPrefs.visible)
        let top = favs.isEmpty ? recent : favs
        sectionTitle(favs.isEmpty && !recent.isEmpty ? L("all.recent") : L("all.favs"))
        if top.isEmpty {
            Text(L("all.favHint")).font(.footnote).foregroundStyle(Theme.muted)
        } else {
            grid(top, fav: !favs.isEmpty)
            if favs.isEmpty { Text(L("all.favHint")).font(.caption).foregroundStyle(Theme.muted) }
        }
        ForEach(AllCat.allCases, id: \.self) { cat in
            let list = AllPrefs.entries.filter { $0.cat == cat && AllPrefs.visible($0) }
            if !list.isEmpty {
                sectionTitle(cat.title).padding(.top, 8)
                grid(list, fav: false)
            }
        }
        Button { router.toolsPath.append(.features) } label: {
            Text(L("all.editFeatures") + " ›").font(.subheadline.weight(.semibold))
        }
        .padding(.top, 14)
    }

    @ViewBuilder private var results: some View {
        let found = AllPrefs.search(query)
        if found.isEmpty {
            EmptyState(icon: "magnifyingglass", title: L("settings.noResult"), text: nil)
        } else {
            VStack(spacing: 0) {
                ForEach(Array(found.prefix(40).enumerated()), id: \.offset) { i, pair in
                    resultRow(pair.0, sub: pair.1)
                    if i < min(found.count, 40) - 1 { Divider().padding(.leading, 60) }
                }
            }
            .background(RoundedRectangle(cornerRadius: 14).fill(Theme.surface))
        }
    }

    private func resultRow(_ e: AllEntry, sub: String) -> some View {
        let isSura: Bool = { if case .sura = e.route { return true } else { return false } }()
        return Button { open(e) } label: {
            HStack(spacing: 12) {
                // İki tonlu simge (8 Kasım 2026, DUOTONE.md): satır 32/19, kategori renginde
                IconBox(icon: e.icon, tint: e.cat.color, line: LineIconMap.entry(e.id))
                VStack(alignment: .leading, spacing: 2) {
                    Text(isSura ? sub : e.title).foregroundStyle(Theme.text)
                    Text(isSura ? e.title : sub).font(.caption).foregroundStyle(Theme.muted)
                }
                Spacer()
                Image(systemName: "chevron.forward").font(.caption).foregroundStyle(Theme.muted)
            }
            .padding(.horizontal, 12).padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text).font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted).padding(.top, 8)
    }

    private func grid(_ list: [AllEntry], fav: Bool) -> some View {
        LazyVGrid(columns: columns, spacing: 8) {
            ForEach(list) { e in tile(e, fav: fav) }
        }
    }

    private func tile(_ e: AllEntry, fav: Bool) -> some View {
        let isFav = AllPrefs.list(favsRaw).contains(e.id)
        return Button { open(e) } label: {
            VStack(spacing: 6) {
                // İki tonlu simge (8 Kasım 2026, DUOTONE.md): kare 42/25, kategori renginde
                IconBox(icon: e.icon, tint: e.cat.color, line: LineIconMap.entry(e.id), box: 42, glyph: 25)
                    .overlay(alignment: .topTrailing) {
                        if fav { Image(systemName: "star.fill").font(.system(size: 9)).foregroundStyle(Theme.accent).offset(x: 4, y: -4) }
                    }
                Text(e.title)
                    .font(.caption)
                    .foregroundStyle(Theme.text)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
            }
            .frame(maxWidth: .infinity, minHeight: 88)
            .padding(.vertical, 6)
            .background { PaperShape(radius: 16, dark: scheme == .dark) }
            .contentShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(PressScaleStyle())
        .contextMenu {
            Button { toggle(e) } label: {
                Label(isFav ? L("all.favRemove") : L("all.favAdd"), systemImage: isFav ? "star.slash" : "star")
            }
        }
    }

    private func toggle(_ e: AllEntry) {
        switch AllPrefs.toggleFav(e.id) {
        case .some(true): toast = L("all.favAdded")
        case .some(false): toast = L("all.favRemoved")
        case .none: toast = L("all.favFull")
        }
    }

    private func open(_ e: AllEntry) {
        if !e.id.hasPrefix("s:") { AllPrefs.used(e.id) }
        if let t = e.tab {
            router.tab = t
        } else if let r = e.route {
            router.toolsPath.append(r)
        }
    }
}

extension ToolRoute {
    /// Araçlar (Tümü) yolundaki sayfa
    @MainActor @ViewBuilder var view: some View {
        switch self {
        case .alarm: AlarmsView()
        case .timer: TimerView()
        case .stopwatch: StopwatchView()
        case .pomodoro: PomodoroView()
        case .study: StudyView()
        case .wakeGame: WakeGameView()
        case .sleep: SleepView()
        case .journal: JournalView()
        case .breath: BreathView()
        case .planner: MeetingPlannerView()
        case .datecalc: DateCalcView()
        case .settings: SettingsView()
        case .about: AboutView()
        case .badges: BadgesView()
        case .qibla: QiblaView()
        case .quran: QuranView()
        case .sura(let n): SuraView(sura: n, start: 1)
        case .duas: DuasView()
        case .zikir: ZikirView()
        case .track: TrackerView()
        case .zakat: ZakatView()
        case .reldays: ReligiousDaysView()
        case .greet: GreetingView()
        case .esma: EsmaView()
        case .hadith: HadithView()
        case .guide: PrayerGuideView()
        case .leaf: CalendarLeafView()
        case .moon: MoonView()
        case .sky: SkyView()
        case .sunpath: SunPathView()
        case .chess: ChessClockView()
        case .metronome: MetronomeView()
        case .talking: TalkingClockView()
        case .zodiac: ZodiacView()
        case .timecalc: TimeCalcView()
        case .bridge: BridgeView()
        case .retire: RetireView()
        case .exam: ExamTimerView()
        case .classroom: ClassroomView()
        case .appearance: AppearanceView()
        case .color: AccentPickerView()
        case .dials: DialGalleryView()
        case .widgets: WidgetGalleryView()
        case .homeItems: HomeItemsView()
        case .features: FeaturesView()
        case .notifications: NotificationsView()
        case .language: LanguageView()
        case .shareQr: ShareQRView()
        }
    }
}
