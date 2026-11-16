import SwiftUI
import CoreLocation
import WeatherKit
import SaatCore

/// Hava durumu: seçili konum için güncel, saatlik (24 saat) ve 7 günlük tahmin.
/// Sağlayıcı Apple WeatherKit (Apple Developer üyeliğinde ayda 500.000 çağrı dahil). Apple'ın şartı gereği
/// "Apple Hava Durumu" işareti ve yasal bilgiler bağlantısı ekranda gösterilir.
struct WeatherView: View {
    /// Birim değişince yeniden çizilir
    @AppStorage("units") private var units = Units.systemDefault
    @State private var data: Weather?
    @State private var failed = false
    /// Üst kartta hava canlandırması (2. analiz #193; Ayarlar › Saat görünümü)
    @AppStorage(WeatherScene.tabKey) private var animate = true
    /// Paylaşım kartı (2. analiz #105)
    @State private var sharing = false
    /// Gün ayrıntısı (2. analiz #107)
    @State private var openDay: Int?
    /// Kartların sırası (WeatherCards) ve sürüklerken üstünde durulan kart
    @AppStorage(WeatherCards.key) private var orderRaw = ""
    @State private var dropTarget: String?
    @State private var editingCards = false
    @Environment(\.colorScheme) private var colorScheme

    private var zone: TimeZone { PlaceModel.shared.place.zone }
    /// Sayfanın zeminindeki hava sahnesi (canlandırma açık ve veri geldiyse)
    private var pageScene: WeatherScene? { animate ? data.map { WeatherScene($0.currentWeather) } : nil }

    var body: some View {
        let place = PlaceModel.shared.place
        ScrollView {
            VStack(spacing: 18) {
                // Dokununca konum değişir (il ya da konumum; 18 Ekim 2026, sahibin isteği)
                PlacePicker(font: .headline)
                if let d = data {
                    current(d)
                    // İşaret verinin hemen altında, kaydırmadan görünür (App Review 5.2.5)
                    AppleWeatherMark()
                    // Kartlar kişinin sırasıyla (16 Kasım 2026, sahibin isteği): saatlik ve 10 günlük tahmin varsayılan olarak
                    // en üstte; bir kartı basılı tutup sürükleyerek ya da "Kartları sırala"dan yeri değişir
                    ForEach(WeatherCards.order(orderRaw), id: \.self) { id in
                        card(id, d, place)
                            .draggable(id) {
                                Text(WeatherCards.title(id)).font(.headline).padding(12)
                                    .background(Theme.surface, in: RoundedRectangle(cornerRadius: 14))
                            }
                            .dropDestination(for: String.self) { items, _ in
                                guard let from = items.first else { return false }
                                orderRaw = WeatherCards.move(orderRaw, from, before: id)
                                return true
                            } isTargeted: { on in dropTarget = on ? id : (dropTarget == id ? nil : dropTarget) }
                            .overlay {
                                if dropTarget == id {
                                    RoundedRectangle(cornerRadius: 22).stroke(Theme.accent, lineWidth: 2)
                                }
                            }
                    }
                } else if failed {
                    Text(L("weather.error")).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                    Button(L("weather.retry")) { Task { await load(place) } }.tint(Theme.accent)
                } else {
                    ProgressView(L("weather.loading")).padding(.top, 40)
                }
            }
            .padding(16)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        // Hava neyse ekran öyle (4 Kasım 2026, sahibin isteği): canlandırma açıkken yağmur, kar, bulut bütün sayfada;
        // okunurluk için sahnenin üstünde hafif karartma, yazılar koyu temanın açık renkleri
        .background {
            if let scene = pageScene {
                ZStack {
                    AnimatedBackground(kind: .weather, scene: scene, showMark: false)
                    Color.black.opacity(0.22).ignoresSafeArea()
                }
            } else {
                AppBackdrop().ignoresSafeArea()
            }
        }
        .environment(\.colorScheme, pageScene != nil ? .dark : colorScheme)
        .navigationTitle(L("weather.h1"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            if data != nil {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button { sharing = true } label: { Label(L("share.card"), systemImage: "square.and.arrow.up") }
                        Button { editingCards = true } label: { Label(L("weather.cards"), systemImage: "arrow.up.arrow.down") }
                    } label: { Image(systemName: "ellipsis") }
                    .accessibilityLabel(L("common.more"))
                }
            }
        }
        .sheet(isPresented: $editingCards) { WeatherCardsEditor(orderRaw: $orderRaw) }
        .task(id: place.key) { await load(place) }
        .sheet(isPresented: Binding(get: { openDay != nil }, set: { if !$0 { openDay = nil } })) {
            if let d = data, let i = openDay, i < d.dailyForecast.count {
                DayDetailSheet(weather: d, day: d.dailyForecast[i], zone: zone)
            }
        }
        .sheet(isPresented: $sharing) {
            if let d = data { ShareCardSheet(title: L("share.card")) { WeatherShareCard(place: place, weather: d) } }
        }
        .refreshable { await load(place) }
    }

    private func load(_ p: AppPlace) async {
        failed = false
        do {
            data = try await WeatherCache.weather(lat: p.lat, lon: p.lon)
        } catch {
            failed = data == nil
        }
    }

    private func current(_ d: Weather) -> some View {
        let c = d.currentWeather
        return VStack(spacing: 6) {
            // Simge ve sıcaklık yan yana (16 Kasım 2026): tahminler ilk ekrana sığsın
            HStack(spacing: 14) {
                icon(c.symbolName, size: 52)
                Text(temp(c.temperature))
                    .font(AppFonts.numerals(56))
            }
            .environment(\.layoutDirection, .leftToRight)
            Text(c.condition.description).font(.title3)
            if let today = d.dailyForecast.first {
                Text("↑ \(temp(today.highTemperature))   ↓ \(temp(today.lowTemperature))")
                    .foregroundStyle(Theme.muted)
                    .environment(\.layoutDirection, .leftToRight)
            }
            // Sözlü günlük özet (2. analiz #108)
            let sum = WeatherSpeech.summary(d, zone: zone)
            if !sum.isEmpty { Text(sum).font(.subheadline).multilineTextAlignment(.center).padding(.top, 2) }
        }
        .frame(maxWidth: .infinity)
        .modifier(WeatherHero(scene: animate ? WeatherScene(c) : nil))
    }

    /// Şu anki ölçümler (hissedilen, nem, rüzgâr, UV, yağış ihtimali, basınç); 16 Kasım 2026'ya kadar üst kartın içindeydi
    private func nowCard(_ d: Weather) -> some View {
        let c = d.currentWeather
        let rain = d.hourlyForecast.first(where: { $0.date >= Date().addingTimeInterval(-3600) })?.precipitationChance ?? 0
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("weather.now"))
            let tc = c.temperature.converted(to: .celsius).value, fc = c.apparentTemperature.converted(to: .celsius).value
            let wk = c.wind.speed.converted(to: .kilometersPerHour).value
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 8) {
                GridRow {
                    pill(L("weather.feelsLike"), temp(c.apparentTemperature), WeatherWords.feels(tc, fc).map { L($0) })
                    pill(L("weather.humidity"), Fmt.percent(Int((c.humidity * 100).rounded())), L(WeatherWords.humidity(c.humidity * 100)))
                }
                GridRow {
                    pill(L("weather.wind"), Units.wind(c.wind.speed), L(WeatherWords.wind(wk)))
                    pill(L("weather.uv"), "\(c.uvIndex.value)", L(WeatherWords.uv(Double(c.uvIndex.value))))
                }
                GridRow {
                    pill(L("weather.rainChanceShort"), Fmt.percent(Int((rain * 100).rounded())))
                    pill(L("weather.pressure"), Units.pressure(c.pressure))
                }
            }
        }
        .card()
    }

    @ViewBuilder
    private func card(_ id: String, _ d: Weather, _ place: AppPlace) -> some View {
        switch id {
        case "hourly": hourly(d)
        case "daily": daily(d)
        case "now": nowCard(d)
        // Resmî uyarılar (2. analiz #112; WeatherKit, kaynak varsa)
        case "alerts": WeatherAlertsCard(alerts: d.weatherAlerts ?? [])
        // Saatlik eğri (2. analiz #107)
        case "chart": HourlyChartCard(weather: d, zone: zone)
        // Dışarıdaki işlere uygunluk (2. analiz #113)
        case "activities": ActivitiesCard(weather: d, zone: zone)
        case "details": details(d)
        // Hava kalitesi ve polen (2. analiz #111; Open-Meteo)
        case "air": AirCard(place: place)
        // Güneş yayı ve Ay (2. analiz #109)
        case "sun": SunMoonArc(now: Date(), place: place)
        case "moon": moonCard(place)
        // Meteogram (WeatherKit) ve model karşılaştırma (Open-Meteo; 2. analiz #114)
        case "meteogram": MeteogramCard(weather: d, zone: zone)
        case "models": ModelsCard(place: place)
        case "report": WeatherReportCard(lat: place.lat, lon: place.lon)
        // Kilit ekranında ve Dynamic Island'da hava (4 Kasım 2026, sahibin isteği; kolay aç/kapa)
        case "live": LiveWeatherToggle().frame(maxWidth: .infinity, alignment: .leading).card()
        default: EmptyView()
        }
    }

    private func pill(_ title: String, _ value: String, _ note: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.caption).foregroundStyle(Theme.muted)
            Text(value).font(.headline).environment(\.layoutDirection, .leftToRight)
            if let note { Text(note).font(.caption).foregroundStyle(Theme.muted).lineLimit(2) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Ek ölçümler (2. analiz #106): rüzgâr yönü ve hamle, görüş, çiy noktası, bugünkü yağış, gün doğumu/batımı
    private func details(_ d: Weather) -> some View {
        let c = d.currentWeather
        let today = d.dailyForecast.first
        let dir = c.wind.direction.converted(to: .degrees).value
        let rain = d.hourlyForecast.first(where: { $0.date >= Date().addingTimeInterval(-3600) })?.precipitationChance ?? 0
        let compass = (0..<8).map { L("weather.compass.\($0)") }
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("weather.details"))
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 10) {
                GridRow {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("weather.wind")).font(.caption).foregroundStyle(Theme.muted)
                        HStack(spacing: 6) {
                            // Ok rüzgârın gittiği yönü gösterir (gelen yön + 180°)
                            Image(systemName: "location.north.fill").rotationEffect(.degrees(dir + 180)).foregroundStyle(Theme.accent)
                            Text(Units.wind(c.wind.speed)).font(.headline).environment(\.layoutDirection, .leftToRight)
                        }
                        Text(T("weather.windFrom", compass[WeatherWords.compass8(dir)])).font(.caption).foregroundStyle(Theme.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if let g = c.wind.gust { pill(L("weather.gusts"), Units.wind(g)) } else { Color.clear }
                }
                GridRow {
                    pill(L("weather.visibility"), Units.distance(c.visibility), L(WeatherWords.visibility(c.visibility.converted(to: .meters).value)))
                    pill(L("weather.dewPoint"), temp(c.dewPoint), L(WeatherWords.dew(c.dewPoint.converted(to: .celsius).value)))
                }
                GridRow {
                    pill(L("weather.precipitation"), today.map { Units.precip($0.precipitationAmount) } ?? "–")
                    pill(L("weather.rainChanceShort"), Fmt.percent(Int((rain * 100).rounded())))
                }
                GridRow {
                    pill(L("weather.sunrise"), today?.sun.sunrise.map { Fmt.time($0, tz: zone) } ?? "–")
                    pill(L("weather.sunset"), today?.sun.sunset.map { Fmt.time($0, tz: zone) } ?? "–")
                }
            }
        }
        .card()
    }

    /// Ay kartı (2. analiz #109): evre, aydınlık yüzde, yeni aya ve dolunaya kalan gün, doğuş/batış; dokununca Ay takvimi
    private func moonCard(_ place: AppPlace) -> some View {
        let now = Date()
        let moon = Nature.moon(at: now)
        let rs = place.moonRiseSet(CalendarDay(now, timeZone: place.zone))
        func days(_ ms: Int64) -> Int { max(0, Int((Double(ms) / 1000 - now.timeIntervalSince1970) / 86_400 + 0.5)) }
        return NavigationLink { MoonView() } label: {
            VStack(alignment: .leading, spacing: 8) {
                SectionTitle(text: L("nature.moonHeading"))
                HStack(spacing: 12) {
                    Text(moonPhaseEmoji[moon.phaseIndex]).font(.system(size: 40))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("nature.phases.\(moonPhaseKeys[moon.phaseIndex])")).font(.headline)
                        Text(T("nature.illumination", Int((moon.illumination * 100).rounded()))).font(.caption).foregroundStyle(Theme.muted)
                        Text("↑ " + place.moonTime(rs.rise, none: "nature.moonNoRise") + "   ↓ " + place.moonTime(rs.set, none: "nature.moonNoSet"))
                            .font(.caption).monospacedDigit()
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("🌑 " + T("weather.moonDays", days(moon.nextNewMoon))).font(.caption).foregroundStyle(Theme.muted)
                        Text("🌕 " + T("weather.moonDays", days(moon.nextFullMoon))).font(.caption).foregroundStyle(Theme.muted)
                    }
                }
            }
            .foregroundStyle(Theme.ink)
        }
        .buttonStyle(.plain)
        .card()
    }

    private func hourly(_ d: Weather) -> some View {
        let now = Date()
        let items = Array(d.hourlyForecast.filter { $0.date > now.addingTimeInterval(-3600) }.prefix(24))
        return VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("weather.hourlyHeading"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 14) {
                    ForEach(items, id: \.date) { h in
                        VStack(spacing: 4) {
                            Text(Fmt.time(h.date, tz: zone)).font(.caption).foregroundStyle(Theme.muted)
                            icon(h.symbolName, size: 22)
                            Text(temp(h.temperature)).font(.subheadline.weight(.semibold))
                            if h.precipitationChance >= 0.1 {
                                Text(Fmt.percent(Int((h.precipitationChance * 100).rounded()))).font(.caption2).foregroundStyle(.blue)
                            }
                        }
                        .environment(\.layoutDirection, .leftToRight)
                    }
                }
            }
        }
        .card()
    }

    private func daily(_ d: Weather) -> some View {
        let days = Array(d.dailyForecast.prefix(10))
        let lo = days.map { $0.lowTemperature.converted(to: .celsius).value }.min() ?? 0
        let hi = days.map { $0.highTemperature.converted(to: .celsius).value }.max() ?? 1
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("weather.dailyHeading10"))
            ForEach(Array(days.enumerated()), id: \.offset) { i, day in
                let low = day.lowTemperature.converted(to: .celsius).value
                let high = day.highTemperature.converted(to: .celsius).value
                HStack(spacing: 10) {
                    Text(i == 0 ? L("client.today") : L("calendar.weekdaysShort.\(CalendarDay(day.date, timeZone: zone).weekday)"))
                        .frame(width: 52, alignment: .leading)
                    icon(day.symbolName, size: 20).frame(width: 28)
                    Text(temp(day.lowTemperature)).foregroundStyle(Theme.muted).frame(width: 40)
                    GeometryReader { g in
                        let w = g.size.width
                        let span = max(1, hi - lo)
                        let x0 = (low - lo) / span * w
                        let x1 = (high - lo) / span * w
                        ZStack(alignment: .leading) {
                            Capsule().fill(Theme.surface2).frame(height: 6)
                            Capsule().fill(LinearGradient(colors: [.blue, .orange], startPoint: .leading, endPoint: .trailing))
                                .frame(width: max(6, x1 - x0), height: 6)
                                .offset(x: x0)
                        }
                        .frame(maxHeight: .infinity)
                    }
                    .frame(height: 20)
                    Text(temp(day.highTemperature)).frame(width: 40)
                }
                .environment(\.layoutDirection, .leftToRight)
                .contentShape(Rectangle())
                .onTapGesture { openDay = i }
            }
        }
        .card()
    }

    /// Gerçekçi 3D hava simgesi (4 Kasım 2026, sahibin seçimi)
    private func icon(_ name: String, size: CGFloat) -> some View {
        WeatherIcon3D(symbol: name, size: size * 1.25)
            .frame(height: size * 1.25)
    }

    private func temp(_ t: Measurement<UnitTemperature>) -> String {
        Units.temp(t)
    }
}

/// Hava ekranındaki kartların sırası (16 Kasım 2026, sahibin isteği; Android'de aynı kimlikler ve anahtar `weather.order`).
/// Kayıtta olmayan yeni kart sona eklenir.
enum WeatherCards {
    static let key = "weather.order"
    static let all = ["hourly", "daily", "now", "alerts", "chart", "activities", "details", "air", "sun", "moon",
                      "meteogram", "models", "report", "live"]

    static func order(_ raw: String) -> [String] {
        var out: [String] = []
        for id in raw.split(separator: ",").map(String.init) where all.contains(id) && !out.contains(id) { out.append(id) }
        for id in all where !out.contains(id) { out.append(id) }
        return out
    }

    /// `from` kartını `target` kartının yerine taşır (aşağı taşınınca hedefin altına, yukarı taşınınca üstüne)
    static func move(_ raw: String, _ from: String, before target: String) -> String {
        var o = order(raw)
        guard from != target, let i = o.firstIndex(of: from), let j = o.firstIndex(of: target) else { return raw }
        o.remove(at: i)
        o.insert(from, at: j)
        return o.joined(separator: ",")
    }

    static func title(_ id: String) -> String {
        switch id {
        case "hourly": return L("weather.hourlyHeading")
        case "daily": return L("weather.dailyHeading10")
        case "now": return L("weather.now")
        case "alerts": return L("wx.alerts")
        case "chart": return L("weather.card.chart")
        case "activities": return L("act.title")
        case "details": return L("weather.details")
        case "air": return L("aqi.title")
        case "sun": return L("weather.card.sun")
        case "moon": return L("nature.moonHeading")
        case "meteogram": return L("meteo.title")
        case "models": return L("models.title")
        case "report": return L("wxr.title")
        default: return L("live.weather.control")
        }
    }
}

/// "Kartları sırala": sürükleyerek sıralama listesi ve varsayılana dönüş
struct WeatherCardsEditor: View {
    @Binding var orderRaw: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(WeatherCards.order(orderRaw), id: \.self) { id in
                        Text(WeatherCards.title(id))
                    }
                    .onMove { from, to in
                        var o = WeatherCards.order(orderRaw)
                        o.move(fromOffsets: from, toOffset: to)
                        orderRaw = o.joined(separator: ",")
                    }
                } footer: {
                    Text(L("weather.cardsHint"))
                }
                Section {
                    Button(L("world.reset")) { orderRaw = "" }.tint(Theme.accent)
                }
            }
            .environment(\.editMode, .constant(.active))
            .navigationTitle(L("weather.cards"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } } }
        }
    }
}

/// Aynı yer için 15 dakika içinde yeniden sorulmaz; aynı anda gelen istekler tek çağrıda birleşir
/// (saat modelleri ve ana sayfa kartı birlikte sorabilir; aylık çağrı hakkı idareli kullanılır)
@MainActor
enum WeatherCache {
    private static var cache: (key: String, at: Date, value: Weather)?
    private static var inflight: (key: String, task: Task<Weather, Error>)?

    static func weather(lat: Double, lon: Double) async throws -> Weather {
        let key = String(format: "%.2f,%.2f", lat, lon)
        if let c = cache, c.key == key, Date().timeIntervalSince(c.at) < 900 { return c.value }
        if let f = inflight, f.key == key { return try await f.task.value }
        let task = Task { try await WeatherService.shared.weather(for: CLLocation(latitude: lat, longitude: lon)) }
        inflight = (key, task)
        defer { if inflight?.key == key { inflight = nil } }
        let value = try await task.value
        cache = (key, Date(), value)
        return value
    }
}

/// Apple Hava Durumu işareti ve yasal bilgiler bağlantısı. WeatherKit verisi gösterilen her yerde şart
/// (App Review 5.2.5, 28 Ekim 2026). İşaret görseli WeatherService'ten gelir; gelmezse " Weather" yazısı ve Apple'ın
/// sabit yasal sayfası gösterilir, böylece işaret hiçbir durumda kaybolmaz. `onDark`: uydu görüntüsü üstünde (küre).
struct AppleWeatherMark: View {
    var compact = false
    var onDark = false
    @Environment(\.colorScheme) private var scheme
    @Environment(\.openURL) private var openURL
    @State private var attribution: WeatherAttribution?

    static let fallbackLegalURL = URL(string: "https://weatherkit.apple.com/legal-attribution.html")!

    private var dark: Bool { onDark || scheme == .dark }

    var body: some View {
        let height: CGFloat = compact ? 12 : 15
        let mark = Group {
            if let a = attribution {
                AsyncImage(url: dark ? a.combinedMarkDarkURL : a.combinedMarkLightURL) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    fallbackMark(height)
                }
            } else {
                fallbackMark(height)
            }
        }
        .frame(height: height)
        .accessibilityLabel("Apple Weather")
        // Link değil düğme (borderless): List satırında Link satırın her yerine dokununca açılıyordu (16 Kasım 2026,
        // küredeki büyütme düğmesi siteye gidiyordu)
        let legal = Button(L("weather.legal")) { openURL(attribution?.legalPageURL ?? Self.fallbackLegalURL) }
            .buttonStyle(.borderless)
            .font(compact ? .caption2 : .caption)
            .tint(onDark ? .white : Theme.muted)
        Group {
            if onDark {
                HStack(spacing: 8) { mark; legal }
                    .padding(.horizontal, 10).padding(.vertical, 6)
                    .background(.black.opacity(0.55), in: Capsule())
            } else if compact {
                HStack(spacing: 8) { mark.frame(width: 70, alignment: .leading); legal; Spacer(minLength: 0) }
            } else {
                VStack(spacing: 6) { mark; legal }.padding(.top, 4)
            }
        }
        .task { if attribution == nil { attribution = try? await WeatherService.shared.attribution } }
    }

    /// Görsel yokken Apple'ın işaret biçimi: Apple simgesi + "Weather"
    private func fallbackMark(_ height: CGFloat) -> some View {
        Text("\u{F8FF} Weather")
            .font(.system(size: height, weight: .semibold))
            .foregroundStyle(dark ? Color.white : Color.black)
            .lineLimit(1)
            .fixedSize()
    }
}

/// Hava sekmesinin üst kartı: canlandırma açıksa kart yok, hava sahnesi bütün sayfanın zemininde (4 Kasım 2026;
/// önceden yalnız bu kartın içinde yağıyordu); yazılara hafif gölge
private struct WeatherHero: ViewModifier {
    let scene: WeatherScene?

    @ViewBuilder
    func body(content: Content) -> some View {
        if scene != nil {
            content
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .shadow(color: .black.opacity(0.35), radius: 3, y: 1)
        } else {
            content.card()
        }
    }
}

/// Hava durumunun sözlü özeti (2. analiz #108; kural SaatCore WeatherWords, Android ve sitedeki ile aynı)
enum WeatherSpeech {
    /// WeatherKit durumu → özet kuralının beklediği WMO kodu (fırtına 95, kar 73, yağmur 61, diğer 1)
    static func code(_ c: WeatherCondition) -> Int {
        switch SkyWeather(c) {
        case .storm: return 95
        case .snow: return 73
        case .rain: return 61
        default: return 1
        }
    }

    static func summary(_ d: Weather, zone: TimeZone, now: Date = Date()) -> String {
        guard let today = d.dailyForecast.first else { return "" }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = zone
        let next = d.hourlyForecast.filter { $0.date > now.addingTimeInterval(-3600) }.prefix(12).map {
            WeatherWords.Hour(hour: cal.component(.hour, from: $0.date), code: code($0.condition),
                              rain: Int(($0.precipitationChance * 100).rounded()), wind: $0.wind.speed.converted(to: .kilometersPerHour).value)
        }
        let tomorrow = d.dailyForecast.count > 1 ? d.dailyForecast[1].highTemperature.converted(to: .celsius).value : nil
        let s = WeatherWords.summary(Array(next), todayMax: today.highTemperature.converted(to: .celsius).value, tomorrowMax: tomorrow)
        switch s.key {
        case "weather.sum.default":
            return T(s.key, d.currentWeather.condition.description, Units.temp(today.highTemperature), Units.temp(today.lowTemperature))
        case "weather.sum.colder", "weather.sum.warmer":
            return T(s.key, Units.delta(s.n))
        default:
            let at = cal.date(bySettingHour: s.hour, minute: 0, second: 0, of: now) ?? now
            return T(s.key, Fmt.time(at, tz: zone))
        }
    }
}
