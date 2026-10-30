import SwiftUI
import SaatCore
import WeatherKit

/// Saat ekranında saatin altında görünebilecek alanlar. Seçim ve sıra "homeItems" anahtarında:
/// bütün alanlar sırayla, kapalı olanın başında "-" ("date,week,next,-prayers,…").
enum HomeItem: String, CaseIterable, Identifiable {
    case date, week, next, prayers, upcoming, world, weather, sun, globe

    var id: String { rawValue }
    var title: String { L("home.item.\(rawValue)") }

    var icon: String {
        switch self {
        case .date: return "calendar"
        case .week: return "number"
        case .next: return "hourglass"
        case .prayers: return "moon.stars"
        case .upcoming: return "star"
        case .world: return "globe"
        case .weather: return "cloud.sun"
        case .sun: return "sunrise"
        case .globe: return "globe.europe.africa"
        }
    }

    /// Dönen dünya varsayılan olarak açık (18 Ekim 2026, sahibin isteği); "Düzenle" ile kapatılabilir
    static let defaultRaw = "-date,-week,-next,-prayers,-upcoming,-world,-weather,-sun,-globe" // varsayılan: yalnız saat (30 Ekim 2026)
}

struct HomeLayout {
    var order: [HomeItem]
    var enabled: Set<HomeItem>

    init(raw: String) {
        var order: [HomeItem] = []
        var enabled = Set<HomeItem>()
        for part in raw.split(separator: ",") {
            let off = part.hasPrefix("-")
            guard let item = HomeItem(rawValue: String(off ? part.dropFirst() : part)), !order.contains(item) else { continue }
            order.append(item)
            if !off { enabled.insert(item) }
        }
        // sonradan eklenen alanlar sona; varsayılanda açıksa açık gelir
        let added = HomeItem.allCases.filter { !order.contains($0) }
        if !added.isEmpty {
            let defaults = HomeLayout(raw: HomeItem.defaultRaw).enabled
            enabled.formUnion(added.filter(defaults.contains))
            order += added
        }
        self.order = order
        self.enabled = enabled
    }

    var raw: String { order.map { (enabled.contains($0) ? "" : "-") + $0.rawValue }.joined(separator: ",") }
    var visible: [HomeItem] { order.filter(enabled.contains) }
}

/// Ayarlar › Saat ekranı: alanları aç/kapat, sürükleyerek sırala
struct HomeItemsView: View {
    @AppStorage("homeItems") private var raw = HomeItem.defaultRaw

    var body: some View {
        let layout = HomeLayout(raw: raw)
        List {
            Section {
                ForEach(layout.order) { item in
                    Toggle(isOn: Binding(
                        get: { layout.enabled.contains(item) },
                        set: { on in
                            var l = HomeLayout(raw: raw)
                            if on { l.enabled.insert(item) } else { l.enabled.remove(item) }
                            raw = l.raw
                            BadgeStore.shared.hit(.duzenle)
                        }
                    )) {
                        Label(item.title, systemImage: item.icon)
                    }
                    .tint(Theme.accent)
                }
                .onMove { from, to in
                    var l = HomeLayout(raw: raw)
                    l.order.move(fromOffsets: from, toOffset: to)
                    raw = l.raw
                }
            } footer: {
                Text(L("home.customizeNote"))
            }
            Section {
                Button(L("world.reset")) { raw = HomeItem.defaultRaw }
                    .tint(Theme.accent)
            }
        }
        .themedBackground()
        .environment(\.editMode, .constant(.active))
        .navigationTitle(L("home.customize"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Saat ekranındaki bir alan; dokununca ilgili sayfa açılır
struct HomeItemView: View {
    let item: HomeItem
    let now: Date
    let place: AppPlace
    /// Dönen dünyanın yüksekliği (saat ekranı kalan boşluğa göre verir)
    var globeHeight: CGFloat = 300
    /// Sayfa açıldıktan sonra (tam ekran saat kendini kapatır)
    var opened: () -> Void = {}

    var body: some View {
        // Button değil: hava kartındaki Apple yasal bağlantısı kendi dokunuşunu alabilsin
        content
            .contentShape(Rectangle())
            .onTapGesture {
                Router.shared.open(item)
                opened()
            }
            .accessibilityAddTraits(.isButton)
    }

    @ViewBuilder
    private var content: some View {
        switch item {
        case .date:
            Text(Fmt.longDate(now))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
        case .week:
            let today = CalendarDay(now)
            Text(T("clock.weekDay", today.isoWeek.week, today.dayOfYear))
                .font(.subheadline)
                .foregroundStyle(Theme.muted)
        case .next:
            NextPrayerCard(now: now, place: place)
        case .prayers:
            TodayPrayersCard(now: now, place: place)
        case .upcoming:
            UpcomingDayCard(now: now)
        case .world:
            WorldMiniCard(now: now)
        case .weather:
            WeatherMiniCard(place: place)
        case .sun:
            SunCard(now: now, place: place)
        case .globe:
            GlobeView(interactive: false)
                .allowsHitTesting(false)
                .frame(height: globeHeight)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(alignment: .topLeading) {
                    AppleWeatherMark(compact: true, onDark: true).padding(10) // WeatherKit işareti, tıklanabilir
                }
        }
    }
}

/// Seçili ilin sıradaki vakti ve kalan süre
struct NextPrayerCard: View {
    let now: Date
    let place: AppPlace

    var body: some View {
        if let next = place.schedule.next(after: now) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L("home.item.next"))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.accent)
                    Text("\(next.prayer.localizedName) \(place.hm(next.at))")
                        .font(.headline)
                        .foregroundStyle(Theme.text)
                }
                Spacer()
                Text(Fmt.countdown(Int(next.at.timeIntervalSince(now).rounded(.up))))
                    .font(.title3.weight(.semibold).monospacedDigit())
                    .foregroundStyle(Theme.accent)
                    .environment(\.layoutDirection, .leftToRight)
            }
            .card()
        }
    }
}

/// Bugünün altı vakti tek satırda; sıradaki vurgulu
private struct TodayPrayersCard: View {
    let now: Date
    let place: AppPlace

    var body: some View {
        let schedule = place.schedule
        let entries = schedule.entries(on: now)
        let next = schedule.next(after: now)
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 4) {
                ForEach(entries) { e in
                    let isNext = e.prayer == next?.prayer && Calendar.current.isDate(e.at, inSameDayAs: next?.at ?? .distantPast)
                    VStack(spacing: 2) {
                        Text(e.prayer.localizedName)
                            .font(.caption2)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Text(place.hm(e.at))
                            .font(.subheadline.weight(.semibold).monospacedDigit())
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    .foregroundStyle(isNext ? Theme.accent : Theme.text)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .card()
    }
}

/// Yaklaşan ilk özel gün (resmî tatil, dini gün…)
private struct UpcomingDayCard: View {
    let now: Date

    var body: some View {
        let today = CalendarDay(now, timeZone: PrayerSchedule.zone)
        if let e = CalendarData.shared.upcoming(from: today, limit: 1).first, let day = e.day {
            HStack(spacing: 10) {
                Circle().fill(e.color).frame(width: 10, height: 10)
                VStack(alignment: .leading, spacing: 2) {
                    Text(e.name).font(.headline).foregroundStyle(Theme.text)
                    Text(Fmt.longDate(day.noon(in: PrayerSchedule.zone), tz: PrayerSchedule.zone))
                        .font(.caption)
                        .foregroundStyle(Theme.muted)
                }
                Spacer()
                Text(relative(day, today))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.accent)
            }
            .card()
        }
    }
}

/// Dünya saatleri listesinin ilk dört şehri
private struct WorldMiniCard: View {
    let now: Date
    @AppStorage("world") private var keysRaw = City.defaultKeys

    var body: some View {
        let cities = keysRaw.split(separator: ",").compactMap { k in City.all.first { $0.key == k } }.prefix(4)
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], alignment: .leading, spacing: 10) {
            ForEach(Array(cities)) { c in
                VStack(alignment: .leading, spacing: 0) {
                    Text(c.displayName).font(.caption).foregroundStyle(Theme.muted).lineLimit(1)
                    Text(Fmt.time(now, tz: c.zone) + (Fmt.ampm(now, tz: c.zone).isEmpty ? "" : " " + Fmt.ampm(now, tz: c.zone)))
                        .font(.title3.weight(.semibold).monospacedDigit())
                        .foregroundStyle(Theme.text)
                        .environment(\.layoutDirection, .leftToRight)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .card()
    }
}

/// Seçili ilde şu anki hava (WeatherKit, 15 dk önbellek) ve Apple Hava Durumu işareti
private struct WeatherMiniCard: View {
    let place: AppPlace
    @State private var data: Weather?

    var body: some View {
        VStack(spacing: 8) {
            if let d = data {
                let c = d.currentWeather
                HStack(spacing: 12) {
                    Image(systemName: c.symbolName)
                        .symbolRenderingMode(.multicolor)
                        .font(.system(size: 32))
                    Text(c.condition.description).font(.subheadline).foregroundStyle(Theme.text)
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(Self.temp(c.temperature))
                            .font(.title.weight(.semibold))
                            .foregroundStyle(Theme.text)
                        if let today = d.dailyForecast.first {
                            Text("↑\(Self.temp(today.highTemperature)) ↓\(Self.temp(today.lowTemperature))")
                                .font(.caption)
                                .foregroundStyle(Theme.muted)
                        }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                }
                AppleWeatherMark(compact: true)
            } else {
                ProgressView().frame(maxWidth: .infinity)
            }
        }
        .card()
        .task(id: place.key) { data = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) }
    }

    private static func temp(_ t: Measurement<UnitTemperature>) -> String {
        Units.temp(t)
    }
}

/// Güneşin doğuşu, batışı ve gün uzunluğu
private struct SunCard: View {
    let now: Date
    let place: AppPlace

    var body: some View {
        if let sun = SeasonsView.sun(place: place, now: now) {
            let len = Int(sun.set.timeIntervalSince(sun.rise) / 60)
            HStack {
                column(L("nature.sunrise"), place.hm(sun.rise), "sunrise")
                column(L("nature.sunset"), place.hm(sun.set), "sunset")
                column(L("nature.dayLength"), T("seasons.dayLengthValue", len / 60, len % 60), "sun.max")
            }
            .card()
        }
    }

    private func column(_ title: String, _ value: String, _ icon: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon).foregroundStyle(Theme.accent)
            Text(value).font(.subheadline.weight(.semibold).monospacedDigit()).foregroundStyle(Theme.text)
            Text(title).font(.caption2).foregroundStyle(Theme.muted).lineLimit(1).minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity)
    }
}
