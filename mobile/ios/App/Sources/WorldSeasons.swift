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

    static let all: [City] = Bundle.main.url(forResource: "cities", withExtension: "json")
        .flatMap { try? Data(contentsOf: $0) }
        .flatMap { try? JSONDecoder().decode([City].self, from: $0) } ?? []

    /// Sitedeki varsayılan liste (Ankara ve İzmir İstanbul'la aynı dilimde olduğu için yok)
    static let defaultKeys = "istanbul,london,berlin,cairo,moscow,dubai,mumbai,beijing,tokyo,sydney,new-york,los-angeles"
}

struct WorldClockView: View {
    @AppStorage("world") private var keysRaw = City.defaultKeys
    @State private var adding = false
    @State private var shift = false
    @State private var when = Date()

    private var cities: [City] {
        keysRaw.split(separator: ",").compactMap { k in City.all.first { $0.key == k } }
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let at = shift ? when : context.date
            List {
                Section {
                    GlobeView()
                        .frame(height: 340)
                        .listRowInsets(EdgeInsets())
                }
                Section {
                    LabeledContent(L("settings.location")) { PlacePicker(font: .body) }
                    Toggle(L("world.changeTime"), isOn: $shift)
                    if shift {
                        DatePicker(L("converter.whenLabel"), selection: $when)
                    }
                }
                Section {
                    ForEach(cities) { city in
                        row(city, at: at)
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
                } footer: {
                    Button(L("world.reset")) { keysRaw = City.defaultKeys }
                        .font(.footnote)
                }
            }
        }
        .navigationTitle(L("world.h1"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) { EditButton() }
            ToolbarItem(placement: .topBarTrailing) {
                Button { adding = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel(L("world.addCity"))
            }
        }
        .sheet(isPresented: $adding) {
            NavigationStack {
                List(City.all.filter { c in !cities.contains(c) }) { c in
                    Button {
                        keysRaw = (cities.map(\.key) + [c.key]).joined(separator: ",")
                        adding = false
                    } label: {
                        VStack(alignment: .leading) {
                            Text(c.displayName).foregroundStyle(Theme.text)
                            Text(c.displayCountry).font(.caption).foregroundStyle(Theme.muted)
                        }
                    }
                }
                .navigationTitle(L("world.addCity"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("world.done")) { adding = false } } }
            }
        }
    }

    private func row(_ city: City, at date: Date) -> some View {
        let diff = city.zone.secondsFromGMT(for: date) - TimeZone.current.secondsFromGMT(for: date)
        let cityDay = CalendarDay(date, timeZone: city.zone)
        let myDay = CalendarDay(date)
        // Şehirde gün farklıysa haftanın günü gösterilir
        let dayNote = cityDay == myDay ? "" : L("calendar.weekdaysShort.\(cityDay.weekday)")
        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(city.displayName).font(.headline)
                Text(diff == 0 ? L("client.diffSame") : T(diff > 0 ? "client.diffAhead" : "client.diffBehind", Fmt.hm(seconds: diff)))
                    .font(.caption)
                    .foregroundStyle(Theme.muted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(Fmt.time(date, tz: city.zone) + (Fmt.ampm(date, tz: city.zone).isEmpty ? "" : " " + Fmt.ampm(date, tz: city.zone)))
                    .font(.system(size: 28, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .environment(\.layoutDirection, .leftToRight)
                if !dayNote.isEmpty {
                    Text(dayNote).font(.caption2).foregroundStyle(Theme.accent)
                }
            }
        }
    }
}

// MARK: Mevsimler ve Ay

struct SeasonsView: View {

    private static let seasonKeys = ["spring", "summer", "autumn", "winter"]
    private static let phaseKeys = ["new", "waxingCrescent", "firstQuarter", "waxingGibbous", "full", "waningGibbous", "lastQuarter", "waningCrescent"]
    private static let phaseEmoji = ["🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"]

    var body: some View {
        let now = Date()
        let place = PlaceModel.shared.place
        let tr = place.zone
        let year = CalendarDay(now, timeZone: tr).year
        // Mevsim anları: bu yıl ve sonraki yılın ilkbaharı
        let marks = (Self.instants(year) + Self.instants(year + 1)).sorted { $0.1 < $1.1 }
        let current = marks.last { $0.1 <= now } ?? (3, Self.instants(year - 1)[3].1)
        let next = marks.first { $0.1 > now }
        let moon = Nature.moon(at: now)
        let sun = Self.sun(place: place, now: now)
        List {
            Section {
                Text(T("nature.current", L("nature.seasons.\(Self.seasonKeys[current.0])"))).font(.title3.weight(.semibold))
                if let next {
                    let days = DateCalc.days(from: CalendarDay(now, timeZone: tr), to: CalendarDay(next.1, timeZone: tr))
                    Text(T("nature.nextIn", L("nature.seasons.\(Self.seasonKeys[next.0])"), days))
                    Text(T("nature.startsOn", L("nature.seasons.\(Self.seasonKeys[next.0])"), Fmt.shortDateTime(next.1, tz: tr)))
                        .foregroundStyle(Theme.muted)
                }
            }
            Section(L("nature.sunHeading") + " · " + place.name) {
                if let sun {
                    LabeledContent(L("nature.sunrise"), value: place.hm(sun.rise))
                    LabeledContent(L("nature.sunset"), value: place.hm(sun.set))
                    LabeledContent(L("nature.solarNoon"), value: place.hm(sun.noon))
                    let len = Int(sun.set.timeIntervalSince(sun.rise)) / 60
                    LabeledContent(L("nature.dayLength"), value: T("seasons.dayLengthValue", len / 60, len % 60))
                }
            }
            Section(L("nature.moonHeading")) {
                HStack(spacing: 14) {
                    Text(Self.phaseEmoji[moon.phaseIndex]).font(.system(size: 48))
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L("nature.phases.\(Self.phaseKeys[moon.phaseIndex])")).font(.headline)
                        Text(T("nature.illumination", Int((moon.illumination * 100).rounded())))
                        Text(T("nature.age", String(format: "%.1f", moon.age)))
                            .foregroundStyle(Theme.muted)
                    }
                }
                LabeledContent(L("nature.nextNew"), value: Fmt.shortDateTime(Date(unixMilliseconds: moon.nextNewMoon), tz: tr))
                LabeledContent(L("nature.nextFull"), value: Fmt.shortDateTime(Date(unixMilliseconds: moon.nextFullMoon), tz: tr))
            }
            Section {
                ForEach(year...(year + 5), id: \.self) { y in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(y)).font(.headline)
                        let list = Self.instants(y)
                        ForEach(0..<4, id: \.self) { i in
                            LabeledContent(L("nature.seasons.\(Self.seasonKeys[i])"), value: Fmt.shortDateTime(list[i].1, tz: tr))
                                .font(.subheadline)
                        }
                    }
                }
            } header: {
                Text(L("nature.tableHeading"))
            } footer: {
                Text(L("nature.tableLead"))
            }
        }
        .navigationTitle(L("nature.h1"))
        .navigationBarTitleDisplayMode(.inline)
    }

    /// Yılın dört mevsim anı: (0 ilkbahar, 1 yaz, 2 sonbahar, 3 kış)
    private static func instants(_ year: Int) -> [(Int, Date)] {
        let s = Nature.seasons(year: year)
        return [s.march, s.june, s.september, s.december].enumerated().map { ($0.offset, Date(unixMilliseconds: $0.element)) }
    }

    /// Güneşin doğuşu, batışı ve tepe noktası (vakit düzeltmeleri olmadan, ufuk 0,833°)
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
