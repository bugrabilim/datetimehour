import SwiftUI
import SaatCore

/// Dünya saatleri (dönen dünya ile) ve Mevsimler ve Ay: Araçlar sekmesinden açılır

// MARK: Dünya saatleri

/// Dünya şehirleri (cities.json; sitedeki 31 şehir)
struct City: Decodable, Identifiable, Hashable {
    let key: String
    let tz: String
    let lat: Double
    let lon: Double
    let name: [String: String]
    let country: [String: String]

    var id: String { key }
    var displayName: String { name[AppLocale.code] ?? name["en"] ?? key }
    var displayCountry: String { country[AppLocale.code] ?? country["en"] ?? "" }
    var zone: TimeZone { TimeZone(identifier: tz) ?? .current }

    /// Sıradaki saat değişimi (`days` gün içindeyse): an ve fark (sn; ileri pozitif)
    func nextDST(after now: Date, days: Double = 7) -> (at: Date, delta: Int)? {
        guard let t = zone.nextDaylightSavingTimeTransition(after: now), t.timeIntervalSince(now) <= days * 86400 else { return nil }
        let delta = zone.secondsFromGMT(for: t.addingTimeInterval(3600)) - zone.secondsFromGMT(for: t.addingTimeInterval(-3600))
        return delta == 0 ? nil : (t, delta)
    }

    /// "2 Kasım: saatler 1 sa geri alınıyor" (şehrin yerel tarihiyle)
    func dstNote(now: Date, days: Double = 7) -> String? {
        guard let d = nextDST(after: now, days: days) else { return nil }
        return T(d.delta > 0 ? "world.dstForward" : "world.dstBack", Fmt.dayMonth(d.at, tz: zone), Fmt.hm(seconds: abs(d.delta)))
    }

    static let all: [City] = Bundle.main.url(forResource: "cities", withExtension: "json")
        .flatMap { try? Data(contentsOf: $0) }
        .flatMap { try? JSONDecoder().decode([City].self, from: $0) } ?? []

    /// Sitedeki varsayılan liste (Ankara ve İzmir İstanbul'la aynı dilimde olduğu için yok)
    static let defaultKeys = "istanbul,london,berlin,cairo,moscow,dubai,mumbai,beijing,tokyo,sydney,new-york,los-angeles"
}

struct WorldClockView: View {
    /// Gök simgesi + saat (+ ÖÖ/ÖS); tür denetimi için parçalı
    static func clockText(light: String, date: Date, zone: TimeZone) -> String {
        let ampm: String = Fmt.ampm(date, tz: zone)
        return Fmt.time(date, tz: zone) + (ampm.isEmpty ? "" : " " + ampm)
    }

    @AppStorage("world") private var keysRaw = City.defaultKeys
    @State private var adding = false
    @State private var shift = false
    @State private var when = Date()
    @State private var editMode = EditMode.inactive
    /// iPad ve geniş pencerede şehirler iki sütunda (2. analiz #145; düzenlerken tek sütun)
    @Environment(\.horizontalSizeClass) private var hsc
    /// Şehre dokununca ayrıntı (2. analiz #129); arama metni (#128)
    @State private var detail: WorldEntry?
    @State private var query = ""
    @AppStorage(WorldLabels.key) private var labelsRaw = ""
    /// Küre / harita / 24 saat kadranı (2. analiz #129, #131) ve şehir başına hava
    @AppStorage("world.view") private var view = "globe"
    @ObservedObject private var weather = WorldWeather.shared

    private var cities: [WorldEntry] { WorldEntry.list(keysRaw) }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let at = shift ? when : context.date
            List {
                Section {
                    WorldViewPicker(view: $view)
                    switch view {
                    case "map":
                        WorldMapCard(entries: cities, date: at)
                    case "dial":
                        WorldDial(entries: cities, date: at) { d in
                            shift = true
                            when = d
                        }
                        .frame(maxWidth: .infinity)
                    default:
                        GlobeView()
                            .frame(height: 340)
                            .listRowInsets(EdgeInsets())
                    }
                }
                Section {
                    LabeledContent(L("settings.location")) { PlacePicker(font: .body) }
                    Toggle(L("world.changeTime"), isOn: $shift)
                    if shift {
                        DatePicker(L("converter.whenLabel"), selection: $when)
                            .onChange(of: when) { BadgeStore.shared.hit(.cevirici) }
                    }
                }
                Section {
                    if hsc == .regular && !editMode.isEditing {
                        ForEach(Array(stride(from: 0, to: cities.count, by: 2)), id: \.self) { i in
                            HStack(alignment: .center, spacing: 16) {
                                Button { detail = cities[i] } label: { row(cities[i], at: at) }
                                    .buttonStyle(.plain)
                                    .frame(maxWidth: .infinity)
                                Divider()
                                if i + 1 < cities.count {
                                    Button { detail = cities[i + 1] } label: { row(cities[i + 1], at: at) }
                                        .buttonStyle(.plain)
                                        .frame(maxWidth: .infinity)
                                } else {
                                    Color.clear.frame(maxWidth: .infinity)
                                }
                            }
                        }
                    } else {
                    ForEach(cities) { city in
                        Button { detail = city } label: { row(city, at: at) }
                            .buttonStyle(.plain)
                    }
                    .onDelete { offsets in
                        var list = cities.map(\.key)
                        list.remove(atOffsets: offsets)
                        keysRaw = list.joined(separator: ",")
                    }
                    .onMove { from, to in
                        var list = cities.map(\.key)
                        list.move(fromOffsets: from, toOffset: to)
                        keysRaw = list.joined(separator: ",")
                    }
                    }
                } header: {
                    // Düzenle ve + şehir listesinin başında (18 Ekim 2026, sahibin isteği)
                    HStack(spacing: 18) {
                        Text(L("world.h1")).foregroundStyle(Theme.muted)
                        Spacer()
                        Button(editMode.isEditing ? L("world.done") : L("home.edit")) {
                            withAnimation { editMode = editMode.isEditing ? .inactive : .active }
                        }
                        Button { adding = true } label: { Image(systemName: "plus").font(.body.weight(.semibold)) }
                            .accessibilityLabel(L("world.addCity"))
                    }
                    .font(.subheadline.weight(.semibold))
                    .textCase(nil)
                    .foregroundStyle(Theme.accent)
                    .buttonStyle(.borderless)
                } footer: {
                    VStack(alignment: .leading, spacing: 8) {
                        if weather.hasAny {
                            Text(L("world.wxNote")).font(.footnote).foregroundStyle(Theme.muted)
                            // WeatherKit gösterimi: Apple Hava Durumu işareti ve yasal bağlantı (App Review 5.2.5)
                            AppleWeatherMark(compact: true)
                        }
                        Button(L("world.reset")) { keysRaw = City.defaultKeys }
                            .font(.footnote)
                    }
                }
            }
            .themedBackground()
            .environment(\.editMode, $editMode)
        }
        .navigationTitle(L("world.h1"))
        .navigationBarTitleDisplayMode(.large)
        .sheet(item: $detail) { WorldDetailView(entry: $0) }
        .task(id: keysRaw) { weather.refresh(cities) }
        .sheet(isPresented: $adding) {
            NavigationStack {
                // Şehir arama (2. analiz #128): 31 şehir, 1960 yer (places.json) ve kısaltmalar (EST, CET…)
                List(WorldEntry.search(query, exclude: Set(cities.map(\.key)))) { c in
                    Button {
                        keysRaw = (cities.map(\.key) + [c.key]).joined(separator: ",")
                        adding = false
                        query = ""
                        BadgeStore.shared.hit(.dunya)
                    } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text((c.flag.isEmpty ? "" : c.flag + " ") + c.name).foregroundStyle(Theme.text)
                                if !c.country.isEmpty { Text(c.country).font(.caption).foregroundStyle(Theme.muted) }
                            }
                            Spacer()
                            Text(Fmt.time(Date(), tz: c.zone)).font(.footnote).monospacedDigit().foregroundStyle(Theme.muted)
                        }
                    }
                }
                .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: L("world.searchHint"))
                .themedBackground()
                .navigationTitle(L("world.addCity"))
                .navigationBarTitleDisplayMode(.large)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("world.done")) { adding = false } } }
            }
        }
    }

    private func row(_ city: WorldEntry, at date: Date) -> some View {
        _ = labelsRaw
        let diff = city.zone.secondsFromGMT(for: date) - TimeZone.current.secondsFromGMT(for: date)
        let cityDay = CalendarDay(date, timeZone: city.zone)
        let myDay = CalendarDay(date)
        // Şehirde gün farklıysa ertesi/önceki gün işareti ve haftanın günü
        let dayNote = cityDay == myDay ? "" : (cityDay > myDay ? "+1 " : "−1 ") + L("calendar.weekdaysShort.\(cityDay.weekday)")
        let light = city.light(date)
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                if let label = city.label { Text(label).font(.caption.weight(.semibold)).foregroundStyle(Theme.accent).lineLimit(1) }
                Text((city.flag.isEmpty ? "" : city.flag + " ") + city.name).font(.headline).lineLimit(1)
                Text((diff == 0 ? L("client.diffSame") : T(diff > 0 ? "client.diffAhead" : "client.diffBehind", Fmt.hm(seconds: diff))) + " · " + city.abbr(date))
                    .font(.caption)
                    .foregroundStyle(Theme.muted)
                // Yaz saati uyarısı (rakip analizi #30): şehirde 7 gün içinde saat değişiyorsa
                if let note = city.dstNote(now: date) {
                    Label(note, systemImage: "clock.arrow.2.circlepath").font(.caption2).foregroundStyle(Theme.accent)
                }
            }
            Spacer()
            // Küçük gök penceresi + kutusuz saat (doğal tasarım 4 Kasım 2026; önceden göz alan açık mavi kutu, 2. analiz #129)
            SkyWindow(light: light)
            VStack(alignment: .trailing, spacing: 2) {
                Text(Self.clockText(light: light, date: date, zone: city.zone))
                    .font(.system(size: 24, weight: .light))
                    .monospacedDigit()
                    .foregroundStyle(Theme.text)
                    .environment(\.layoutDirection, .leftToRight)
                if !dayNote.isEmpty {
                    Text(dayNote).font(.caption2.weight(.medium)).foregroundStyle(Theme.accent)
                }
                WorldWeatherBadge(key: city.key, color: Theme.muted)
            }
        }
        .contentShape(Rectangle())
    }
}

// MARK: Mevsimler ve Ay

/// Mevsimler: çocuklara yönelik, görselli anlatım (18 Ekim 2026, sahibin isteği: "şu anki yapı çok karmaşık").
/// Üstte şu anki mevsim ve ilerleyişi; sonra neden mevsim olur (eğik eksen ve yörünge çizimi), ışığın açısı,
/// "kış 1 Aralık'ta mı 21 Aralık'ta mı" (gökbilim ve meteoroloji şeridi), güney yarımküre, ilginç bilgiler;
/// en altta bugünün Güneşi, Ay ve mevsim başlangıçları.
struct SeasonsView: View {
    @AppStorage(LiveActivities.sunKey) private var liveSun = false

    static let seasonKeys = ["spring", "summer", "autumn", "winter"]
    static let seasonEmoji = ["🌸", "☀️", "🍂", "⛄"]
    static let seasonColor: [Color] = [Color(red: 0.30, green: 0.69, blue: 0.31), Color(red: 0.98, green: 0.62, blue: 0.11),
                                       Color(red: 0.80, green: 0.42, blue: 0.16), Color(red: 0.25, green: 0.53, blue: 0.85)]

    var body: some View {
        let now = Date()
        let place = PlaceModel.shared.place
        let tr = place.zone
        let year = CalendarDay(now, timeZone: tr).year
        // Mevsim anları: geçen yılın kışı, bu yıl ve sonraki yılın ilkbaharı
        let marks = (Self.instants(year - 1) + Self.instants(year) + Self.instants(year + 1)).sorted { $0.1 < $1.1 }
        let current = marks.last { $0.1 <= now } ?? (3, now)
        let next = marks.first { $0.1 > now } ?? (0, now)
        ScrollView {
            VStack(spacing: 16) {
                hero(current: current, next: next, now: now, tz: tr)
                card(L("kids.why.title"), emoji: "globe") {
                    OrbitDiagram().frame(height: 230)
                    Text(L("kids.why.text"))
                    Text(L("kids.why.myth"))
                        .font(.callout)
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 12))
                }
                card(L("kids.light.title"), emoji: "flashlight") {
                    LightAngleDiagram().frame(height: 170)
                    Text(L("kids.light.text"))
                }
                card(L("kids.dates.title"), emoji: "calendar") {
                    SeasonYearBars(year: year)
                    Text(L("kids.dates.text"))
                }
                card(L("kids.south.title"), emoji: "beach") {
                    HemispheresDiagram(north: current.0).frame(height: 150)
                    Text(L("kids.south.text"))
                }
                card(L("kids.facts.title"), emoji: "idea") {
                    // Madde imleri emoji yerine iki tonlu simge (8 Kasım 2026): en uzun gün, ekinoks, kutup, ekvator, yörünge
                    ForEach(1...5, id: \.self) { i in
                        HStack(alignment: .top, spacing: 10) {
                            DuoIcon(name: ["sun", "halfmoon", "snowman", "umbrella", "telescope"][i - 1], size: 20, color: Theme.sky)
                            Text(L("kids.fact\(i)")).fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
                sunAndMoon(place: place, now: now)
                // Kilit ekranında Güneş sayacı (2. analiz #150): sıradaki gün doğumu ya da batımına kalan süre
                Toggle(isOn: $liveSun) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("live.sun.toggle"))
                        Text(L("live.sun.note")).font(.caption).foregroundStyle(Theme.muted)
                    }
                }
                .tint(Theme.accent)
                .padding(14)
                .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
                .onChange(of: liveSun) { LiveActivities.sync() }
                lightHours(place: place, now: now)
                FolkCalendarCard(day: CalendarDay(now, timeZone: place.zone))
                startsTable(year: year, tz: tr)
            }
            .padding(16)
            .frame(maxWidth: 640)
            .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("nature.h1"))
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: Bölümler

    /// Şu anki mevsim: büyük simge, ad, ilerleme çubuğu ve sıradaki mevsime kalan gün
    private func hero(current: (Int, Date), next: (Int, Date), now: Date, tz: TimeZone) -> some View {
        let total = next.1.timeIntervalSince(current.1)
        let done = total > 0 ? min(1, max(0, now.timeIntervalSince(current.1) / total)) : 0
        let days = DateCalc.days(from: CalendarDay(now, timeZone: tz), to: CalendarDay(next.1, timeZone: tz))
        let color = Self.seasonColor[current.0]
        let nextName = L("nature.seasons.\(Self.seasonKeys[next.0])")
        return VStack(spacing: 10) {
            LineIcon(name: LineIcons.seasons[current.0], size: 76)
            Text(T("nature.current", L("nature.seasons.\(Self.seasonKeys[current.0])")))
                .font(.title.weight(.bold))
                .multilineTextAlignment(.center)
            VStack(alignment: .leading, spacing: 6) {
                Text(L("kids.progress")).font(.caption.weight(.semibold)).foregroundStyle(Theme.muted)
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Theme.surface2)
                        Capsule().fill(color).frame(width: max(14, g.size.width * done))
                        DuoIcon(name: LineIcons.seasons[next.0], size: 16, color: Self.seasonColor[next.0])
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .padding(.trailing, 4)
                    }
                }
                .frame(height: 22)
                HStack {
                    Text(Fmt.shortDateTime(current.1, tz: tz))
                    Spacer()
                    Text(Fmt.shortDateTime(next.1, tz: tz))
                }
                .font(.caption2)
                .foregroundStyle(Theme.muted)
            }
            Text(T("nature.nextIn", nextName, days))
                .font(.headline)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(18)
        .background(LinearGradient(colors: [color.opacity(0.28), color.opacity(0.08)], startPoint: .top, endPoint: .bottom),
                    in: RoundedRectangle(cornerRadius: 20))
    }

    private func card<Content: View>(_ title: String, emoji: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                // İki tonlu simge (8 Kasım 2026, DUOTONE.md): kart başlığı, gök mavi
                DuoChip(name: emoji, color: Theme.sky)
                Text(title).font(.title3.weight(.bold)).foregroundStyle(Theme.text)
            }
            content()
        }
        .foregroundStyle(Theme.text)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Theme.border.opacity(0.6)))
    }

    private func sunAndMoon(place: AppPlace, now: Date) -> some View {
        let moon = Nature.moon(at: now)
        return HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                // "🌅" emojisinin yerine iki tonlu simge (8 Kasım 2026)
                HStack(spacing: 6) {
                    DuoIcon(name: "sunrise", size: 18, color: Theme.sky)
                    Text(L("nature.sunHeading")).font(.headline)
                }
                if let sun = Self.sun(place: place, now: now) {
                    Text("↑ " + place.hm(sun.rise)).monospacedDigit()
                    Text("↓ " + place.hm(sun.set)).monospacedDigit()
                    let len = Int(sun.set.timeIntervalSince(sun.rise)) / 60
                    Text(T("seasons.dayLengthValue", len / 60, len % 60)).font(.caption).foregroundStyle(Theme.muted)
                }
                Text(place.name).font(.caption2).foregroundStyle(Theme.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            // Ay: evre, aydınlık, bugün doğuş/batış; dokununca Ay takvimi ve tutulmalar
            NavigationLink { MoonView() } label: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("nature.moonHeading")).font(.headline)
                    HStack(spacing: 8) {
                        Text(moonPhaseEmoji[moon.phaseIndex]).font(.system(size: 36))
                        Text(L("nature.phases.\(moonPhaseKeys[moon.phaseIndex])")).font(.subheadline)
                    }
                    Text(T("nature.illumination", Int((moon.illumination * 100).rounded())))
                        .font(.caption).foregroundStyle(Theme.muted)
                    let rs = place.moonRiseSet(CalendarDay(now, timeZone: place.zone))
                    Text("↑ " + place.moonTime(rs.rise, none: "nature.moonNoRise")).monospacedDigit()
                    Text("↓ " + place.moonTime(rs.set, none: "nature.moonNoSet")).monospacedDigit()
                    Text(L("moon.calendarLink") + " ›").font(.caption.weight(.semibold)).foregroundStyle(Theme.accent)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(14)
                .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .foregroundStyle(Theme.text)
        }
        .environment(\.layoutDirection, AppLocale.isRTL ? .rightToLeft : .leftToRight)
    }

    /// Bu yılın mevsim başlangıçları; sonraki yıllar açılır listede
    private func startsTable(year: Int, tz: TimeZone) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(L("nature.tableHeading")).font(.headline)
            yearRows(year, tz: tz)
            DisclosureGroup(L("kids.more")) {
                ForEach((year + 1)...(year + 5), id: \.self) { y in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(y)).font(.subheadline.weight(.bold)).padding(.top, 8)
                        yearRows(y, tz: tz)
                    }
                }
            }
            .tint(Theme.accent)
            Text(L("nature.tableLead")).font(.caption).foregroundStyle(Theme.muted)
        }
        .padding(16)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 18))
    }

    private func yearRows(_ y: Int, tz: TimeZone) -> some View {
        let list = Self.instants(y)
        return ForEach(0..<4, id: \.self) { i in
            HStack {
                DuoIcon(name: LineIcons.seasons[i], size: 18, color: Self.seasonColor[i])
                Text(L("nature.seasons.\(Self.seasonKeys[i])"))
                Spacer()
                Text(Fmt.shortDateTime(list[i].1, tz: tz)).monospacedDigit().foregroundStyle(Theme.muted)
            }
            .font(.subheadline)
        }
    }

    /// Yılın dört mevsim anı: (0 ilkbahar, 1 yaz, 2 sonbahar, 3 kış)
    static func instants(_ year: Int) -> [(Int, Date)] {
        let s = Nature.seasons(year: year)
        return [s.march, s.june, s.september, s.december].enumerated().map { ($0.offset, Date(unixMilliseconds: $0.element)) }
    }

    /// Güneşin doğuşu, batışı ve tepe noktası (vakit düzeltmeleri olmadan, ufuk 0,833°)
    /// Altın ve mavi saat (rakip analizi #15; sitedeki Mevsimler ile aynı: Güneş +6°/−4°/−6°)
    private func lightHours(place: AppPlace, now: Date) -> some View {
        let d = CalendarDay(now, timeZone: place.zone)
        let noon = d.noon(in: place.zone), midnight = noon.addingTimeInterval(-12 * 3600)
        let off = Double(place.zone.secondsFromGMT(for: noon)) / 3600
        let l = PrayerTimes.lightHours(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon, utcOffset: off)
        func span(_ r: (Double?, Double?)) -> String {
            guard let a = r.0, let b = r.1 else { return "–" }
            return place.hm(midnight.addingTimeInterval(a * 3600)) + "–" + place.hm(midnight.addingTimeInterval(b * 3600))
        }
        // Alacakaranlık (2. analiz #151): sivil −6°, denizcilik −12°, astronomik −18°; ilk ve son ışık
        func alt(_ a: Double) -> PrayerTimes.AltitudeTimes {
            PrayerTimes.sunAltitudeTimes(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon, utcOffset: off, altitude: a)
        }
        let s0 = alt(-0.833), c6 = alt(-6), c12 = alt(-12), c18 = alt(-18)
        func one(_ h: Double?) -> String { guard let h else { return "–" }; return place.hm(midnight.addingTimeInterval(h * 3600)) }
        func two(_ a: (Double?, Double?), _ b: (Double?, Double?)) -> String { span(a) + " · " + span(b) }
        // Emoji yerine iki tonlu simge ve gökyüzü rengi (8 Kasım 2026): mavi saat mavi, altın saat altın, ilk/son ışık ve
        // astronomik alacakaranlık mor, sivil kiremit, denizcilik mavi
        let rows: [(String, String, Color, String)] = [
            ("nature.blueAm", "dawn", Theme.sky, span(l.blueAm)), ("nature.goldenAm", "sunrise", Theme.gold, span(l.goldenAm)),
            ("nature.goldenPm", "sunset", Theme.gold, span(l.goldenPm)), ("nature.bluePm", "sunset", Theme.sky, span(l.bluePm)),
            ("nature.tw.firstLight", "night", Theme.dusk, one(c18.rise)), ("nature.tw.lastLight", "night", Theme.dusk, one(c18.set)),
            ("nature.tw.civil", "sunset", Theme.terra, two((c6.rise, s0.rise), (s0.set, c6.set))),
            ("nature.tw.nautical", "compass", Theme.sky, two((c12.rise, c6.rise), (c6.set, c12.set))),
            ("nature.tw.astro", "telescope", Theme.dusk, two((c18.rise, c12.rise), (c12.set, c18.set))),
        ]
        return VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    DuoIcon(name: row.1, size: 18, color: row.2)
                    Text(L(row.0))
                    Spacer()
                    Text(row.3).monospacedDigit().environment(\.layoutDirection, .leftToRight)
                }
            }
            Text(L("nature.lightNote")).font(.caption).foregroundStyle(Theme.muted)
            Text(L("nature.tw.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .foregroundStyle(Theme.text)
        .padding(14)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
    }

    static func sun(place: AppPlace, now: Date) -> (rise: Date, set: Date, noon: Date)? {
        var p = PrayerTimes.Parameters()
        p.adjustments.sunrise = 0
        p.adjustments.dhuhr = 0
        p.adjustments.maghrib = 0
        let d = CalendarDay(now, timeZone: place.zone)
        let t = PrayerTimes.times(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon,
                                  timeZone: place.zone, parameters: p)
        guard let r = t.sunrise, let s = t.maghrib, let n = t.dhuhr else { return nil }
        let midnight = d.noon(in: place.zone).addingTimeInterval(-12 * 3600)
        return (midnight.addingTimeInterval(r * 3600), midnight.addingTimeInterval(s * 3600), midnight.addingTimeInterval(n * 3600))
    }
}

// MARK: Mevsim çizimleri

/// Güneş ortada, Dünya yörüngede dört konumda. Eksen hep aynı yöne eğik (kuzey kutbu sola): Haziran'da kuzey
/// Güneş'e dönük (yaz), Aralık'ta uzağa dönük (kış). Güneş'e bakan yarı aydınlık.
private struct OrbitDiagram: View {
    var body: some View {
        Canvas { ctx, size in
            let c = CGPoint(x: size.width / 2, y: size.height / 2 - 6)
            let rx = size.width * 0.36, ry = size.height * 0.30
            // yörünge
            ctx.stroke(Path(ellipseIn: CGRect(x: c.x - rx, y: c.y - ry, width: 2 * rx, height: 2 * ry)),
                       with: .color(Theme.muted.opacity(0.5)), style: StrokeStyle(lineWidth: 1.2, dash: [4, 4]))
            // Güneş
            let sr: CGFloat = 26
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - sr * 1.8, y: c.y - sr * 1.8, width: sr * 3.6, height: sr * 3.6)),
                     with: .radialGradient(Gradient(colors: [.yellow.opacity(0.55), .orange.opacity(0)]),
                                           center: c, startRadius: sr * 0.6, endRadius: sr * 1.8))
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - sr, y: c.y - sr, width: 2 * sr, height: 2 * sr)),
                     with: .radialGradient(Gradient(colors: [Color(red: 1, green: 0.93, blue: 0.5), .orange]),
                                           center: c, startRadius: 2, endRadius: sr))
            // Dünyalar: sağ = Haziran (yaz), alt = Mart, sol = Aralık (kış), üst = Eylül
            let spots: [(CGPoint, Int, Int)] = [
                (CGPoint(x: c.x + rx, y: c.y), 5, 1), (CGPoint(x: c.x, y: c.y + ry), 2, 0),
                (CGPoint(x: c.x - rx, y: c.y), 11, 3), (CGPoint(x: c.x, y: c.y - ry), 8, 2),
            ]
            let er: CGFloat = 17
            let tilt = 23.4 * Double.pi / 180
            for (p, month, season) in spots {
                let earth = Path(ellipseIn: CGRect(x: p.x - er, y: p.y - er, width: 2 * er, height: 2 * er))
                ctx.fill(earth, with: .linearGradient(Gradient(colors: [Color(red: 0.2, green: 0.55, blue: 0.95),
                                                                         Color(red: 0.2, green: 0.7, blue: 0.4)]),
                                                       startPoint: CGPoint(x: p.x, y: p.y - er), endPoint: CGPoint(x: p.x, y: p.y + er)))
                // Güneş'ten uzak yarı karanlık
                let ang = atan2(c.y - p.y, c.x - p.x)
                var night = Path()
                night.addArc(center: p, radius: er, startAngle: .radians(ang + .pi / 2), endAngle: .radians(ang + 3 * .pi / 2), clockwise: false)
                night.closeSubpath()
                ctx.fill(night, with: .color(.black.opacity(0.45)))
                // eksen: kuzey ucu sola eğik
                let dx = CGFloat(sin(tilt)) * er * 1.45, dy = CGFloat(cos(tilt)) * er * 1.45
                var axis = Path()
                axis.move(to: CGPoint(x: p.x - dx, y: p.y - dy))
                axis.addLine(to: CGPoint(x: p.x + dx, y: p.y + dy))
                ctx.stroke(axis, with: .color(Theme.text), lineWidth: 1.5)
                ctx.fill(Path(ellipseIn: CGRect(x: p.x - dx - 2.5, y: p.y - dy - 2.5, width: 5, height: 5)), with: .color(.red))
                // ay adı ve mevsim (kuzey yarımküre)
                let label = Text(SeasonsView.seasonEmoji[season] + " " + L("calendar.months.\(month)"))
                    .font(.caption2.weight(.semibold))
                ctx.draw(label, at: CGPoint(x: p.x, y: p.y + er + 12))
            }
            ctx.draw(Text(L("globe.sun")).font(.caption2.weight(.bold)).foregroundStyle(.black.opacity(0.75)), at: c)
            ctx.draw(Text(L("kids.tilt")).font(.caption2).foregroundStyle(Theme.muted),
                     at: CGPoint(x: size.width / 2, y: size.height - 6))
        }
        .accessibilityElement()
        .accessibilityLabel(L("kids.why.title"))
    }
}

/// Aynı ışık demeti: yazın dik gelir, dar alanı ısıtır; kışın eğik gelir, geniş alana yayılır
private struct LightAngleDiagram: View {
    var body: some View {
        HStack(spacing: 12) {
            panel(steep: true)
            panel(steep: false)
        }
        .accessibilityElement(children: .combine)
    }

    private func panel(steep: Bool) -> some View {
        VStack(spacing: 6) {
            Canvas { ctx, size in
                let ground = size.height - 14
                let sun = steep ? CGPoint(x: size.width / 2, y: 18) : CGPoint(x: 18, y: ground - 46)
                ctx.fill(Path(ellipseIn: CGRect(x: sun.x - 12, y: sun.y - 12, width: 24, height: 24)), with: .color(.orange))
                // demet: aynı genişlikte üç ışın
                let target = CGPoint(x: size.width / 2 + (steep ? 0 : 18), y: ground)
                let dir = CGVector(dx: target.x - sun.x, dy: target.y - sun.y)
                let len = sqrt(dir.dx * dir.dx + dir.dy * dir.dy)
                let n = CGVector(dx: -dir.dy / len, dy: dir.dx / len)
                let half: CGFloat = 14
                var hits: [CGFloat] = []
                for k in [-1.0, 0.0, 1.0] as [CGFloat] {
                    let start = CGPoint(x: sun.x + n.dx * half * k, y: sun.y + n.dy * half * k)
                    let t = (ground - start.y) / dir.dy
                    let end = CGPoint(x: start.x + dir.dx * t, y: ground)
                    hits.append(end.x)
                    var ray = Path()
                    ray.move(to: start)
                    ray.addLine(to: end)
                    ctx.stroke(ray, with: .color(.yellow.opacity(0.9)), style: StrokeStyle(lineWidth: 2, dash: k == 0 ? [] : [3, 3]))
                }
                // yer ve ısınan alan
                var g = Path()
                g.move(to: CGPoint(x: 0, y: ground))
                g.addLine(to: CGPoint(x: size.width, y: ground))
                ctx.stroke(g, with: .color(Color(red: 0.45, green: 0.32, blue: 0.2)), lineWidth: 3)
                let lo = hits.min() ?? 0, hi = hits.max() ?? 0
                ctx.fill(Path(roundedRect: CGRect(x: lo, y: ground - 4, width: hi - lo, height: 8), cornerRadius: 4),
                         with: .color(steep ? .red.opacity(0.85) : .orange.opacity(0.55)))
            }
            Text(L(steep ? "kids.light.summer" : "kids.light.winter"))
                .font(.caption.weight(.semibold))
        }
        .padding(8)
        .background((steep ? Color.orange : Color.blue).opacity(0.10), in: RoundedRectangle(cornerRadius: 12))
    }
}

/// İki şerit, 12 ay: üstte gökbilim (gündönümü ve ekinoks günlerinde başlar), altta meteoroloji (ayın 1'inde)
private struct SeasonYearBars: View {
    let year: Int

    var body: some View {
        let marks = SeasonsView.instants(year).map { CalendarDay($0.1, timeZone: PrayerSchedule.zone).dayOfYear }
        let total = CalendarDay(year, 12, 31).dayOfYear
        // Meteoroloji: Mart, Haziran, Eylül, Aralık'ın ilk günü
        let meteo = [3, 6, 9, 12].map { CalendarDay(year, $0, 1).dayOfYear }
        VStack(alignment: .leading, spacing: 6) {
            Text(L("kids.dates.astro")).font(.caption.weight(.semibold))
            bar(starts: marks, total: total, showDates: true)
            Text(L("kids.dates.meteo")).font(.caption.weight(.semibold)).padding(.top, 4)
            bar(starts: meteo, total: total, showDates: false)
            HStack(spacing: 0) {
                ForEach(0..<12, id: \.self) { m in
                    Text(String(L("calendar.months.\(m)").prefix(AppLocale.isRTL ? 4 : 3)))
                        .font(.system(size: 9))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .frame(maxWidth: .infinity)
                }
            }
            .foregroundStyle(Theme.muted)
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    /// starts: ilkbahar, yaz, sonbahar, kışın yılın kaçıncı gününde başladığı
    private func bar(starts: [Int], total: Int, showDates: Bool) -> some View {
        GeometryReader { g in
            let w = g.size.width
            let x = { (d: Int) in CGFloat(d - 1) / CGFloat(total) * w }
            // Ocak başı önceki yılın kışı
            let segments: [(Int, CGFloat, CGFloat)] = [
                (3, 0, x(starts[0])), (0, x(starts[0]), x(starts[1])), (1, x(starts[1]), x(starts[2])),
                (2, x(starts[2]), x(starts[3])), (3, x(starts[3]), w),
            ]
            ZStack(alignment: .topLeading) {
                ForEach(0..<segments.count, id: \.self) { i in
                    let s = segments[i]
                    Rectangle()
                        .fill(SeasonsView.seasonColor[s.0].opacity(0.85))
                        .frame(width: max(0, s.2 - s.1), height: 22)
                        .overlay {
                            if s.2 - s.1 > 30 { Text(SeasonsView.seasonEmoji[s.0]).font(.system(size: 12)) }
                        }
                        .offset(x: s.1)
                }
                if showDates {
                    ForEach(0..<4, id: \.self) { i in
                        let d = CalendarDay(year, 1, 1).adding(days: starts[i] - 1)
                        Text("\(d.day)")
                            .font(.system(size: 9, weight: .bold))
                            .padding(.horizontal, 3)
                            .background(Theme.surface, in: Capsule())
                            .position(x: x(starts[i]), y: 30)
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 6))
        }
        .frame(height: showDates ? 38 : 22)
    }
}

/// Dünya ikiye bölünmüş: kuzeydeki mevsimin tersi güneyde (kış ↔ yaz, ilkbahar ↔ sonbahar)
private struct HemispheresDiagram: View {
    let north: Int
    private var south: Int { (north + 2) % 4 }

    var body: some View {
        HStack(spacing: 18) {
            ZStack {
                Circle().fill(LinearGradient(colors: [Color(red: 0.2, green: 0.55, blue: 0.95), Color(red: 0.2, green: 0.7, blue: 0.4)],
                                             startPoint: .top, endPoint: .bottom))
                Rectangle().fill(.white.opacity(0.8)).frame(height: 2)
                VStack(spacing: 0) {
                    Text(SeasonsView.seasonEmoji[north]).font(.system(size: 34)).frame(maxHeight: .infinity)
                    Text(south == 1 ? "🏖️" : SeasonsView.seasonEmoji[south]).font(.system(size: 34)).frame(maxHeight: .infinity)
                }
                .padding(.vertical, 8)
            }
            .frame(width: 130, height: 130)
            .clipShape(Circle())
            VStack(alignment: .leading, spacing: 30) {
                Label(L("kids.south.north") + ": " + L("nature.seasons.\(SeasonsView.seasonKeys[north])"), systemImage: "arrow.up")
                Label(L("kids.south.southLabel") + ": " + L("nature.seasons.\(SeasonsView.seasonKeys[south])"), systemImage: "arrow.down")
            }
            .font(.subheadline.weight(.semibold))
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}

/// Halk takvimi (2. analiz #60): src/data/folk-calendar.json ile aynı geleneksel tarihler (ay, gün)
struct FolkCalendarCard: View {
    let day: CalendarDay
    struct Item: Identifiable { let id: String; let fm: Int, fd: Int, tm: Int, td: Int }
    static let items: [Item] = [
        Item(id: "erbain", fm: 12, fd: 22, tm: 1, td: 30), Item(id: "hamsin", fm: 1, fd: 31, tm: 3, td: 21),
        Item(id: "cemre1", fm: 2, fd: 19, tm: 2, td: 20), Item(id: "cemre2", fm: 2, fd: 26, tm: 2, td: 27),
        Item(id: "cemre3", fm: 3, fd: 5, tm: 3, td: 6), Item(id: "berdulacuz", fm: 3, fd: 11, tm: 3, td: 17),
        Item(id: "eyyamibahur", fm: 7, fd: 31, tm: 8, td: 7),
    ]

    var body: some View {
        let md = day.month * 100 + day.day
        VStack(alignment: .leading, spacing: 8) {
            Text(L("nature.folkHeading")).font(.headline)
            ForEach(Self.items) { it in
                let a = it.fm * 100 + it.fd, b = it.tm * 100 + it.td
                let now = a <= b ? (a...b).contains(md) : (md >= a || md <= b)
                HStack(alignment: .firstTextBaseline) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("nature.folkNames.\(it.id)")).fontWeight(now ? .semibold : .regular)
                            .foregroundStyle(now ? Theme.accent : Theme.text)
                        if now { Text(L("nature.folkNow")).font(.caption.weight(.bold)).foregroundStyle(Theme.accent) }
                    }
                    Spacer()
                    Text("\(it.fd) \(L("calendar.months.\(it.fm - 1)")) – \(it.td) \(L("calendar.months.\(it.tm - 1)"))")
                        .font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.trailing)
                }
            }
            Text(L("nature.folkNote")).font(.caption).foregroundStyle(Theme.muted)
        }
        .foregroundStyle(Theme.text)
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
    }
}

/// Küçük gök penceresi (doğal tasarım 4 Kasım 2026): gündüz güneşli gök, alacakaranlıkta ufukta güneş, gecede hilal.
/// Android'deki SkyWindow ile aynı renkler.
struct SkyWindow: View {
    let light: String

    var body: some View {
        let colors: [Color] = light == "day"
            ? [Color(red: 0.663, green: 0.788, blue: 0.875), Color(red: 0.949, green: 0.902, blue: 0.8)]
            : light == "dusk"
            ? [Color(red: 0.275, green: 0.188, blue: 0.353), Color(red: 0.91, green: 0.635, blue: 0.486)]
            : [Color(red: 0.122, green: 0.133, blue: 0.251), Color(red: 0.231, green: 0.216, blue: 0.384)]
        let sun = Color(red: 0.965, green: 0.765, blue: 0.357)
        let cream = Color(red: 1, green: 0.973, blue: 0.925)
        ZStack {
            LinearGradient(colors: colors, startPoint: .top, endPoint: .bottom)
            GeometryReader { g in
                let w = g.size.width
                if light == "day" {
                    Circle().fill(sun).frame(width: w * 0.4, height: w * 0.4).position(x: w * 0.62, y: w * 0.38)
                } else if light == "dusk" {
                    Circle().fill(sun).frame(width: w * 0.48, height: w * 0.48).position(x: w * 0.5, y: w * 0.94)
                } else {
                    Circle().fill(cream).frame(width: w * 0.38, height: w * 0.38).position(x: w * 0.56, y: w * 0.42)
                    Circle().fill(colors[0].opacity(0.92)).frame(width: w * 0.34, height: w * 0.34).position(x: w * 0.65, y: w * 0.35)
                    Circle().fill(cream.opacity(0.7)).frame(width: w * 0.06, height: w * 0.06).position(x: w * 0.22, y: w * 0.25)
                    Circle().fill(cream.opacity(0.5)).frame(width: w * 0.05, height: w * 0.05).position(x: w * 0.3, y: w * 0.7)
                }
            }
        }
        .frame(width: 30, height: 30)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .accessibilityHidden(true)
    }
}
