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

    private var zone: TimeZone { PlaceModel.shared.place.zone }

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
                    hourly(d)
                    daily(d)
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
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("weather.h1"))
        .navigationBarTitleDisplayMode(.inline)
        .task(id: place.key) { await load(place) }
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
        let rain = d.hourlyForecast.first(where: { $0.date >= Date().addingTimeInterval(-3600) })?.precipitationChance ?? 0
        return VStack(spacing: 6) {
            icon(c.symbolName, size: 64)
            Text(temp(c.temperature))
                .font(.system(size: 56, weight: .semibold, design: .rounded))
                .environment(\.layoutDirection, .leftToRight)
            Text(c.condition.description).font(.title3)
            if let today = d.dailyForecast.first {
                Text("↑ \(temp(today.highTemperature))   ↓ \(temp(today.lowTemperature))")
                    .foregroundStyle(Theme.muted)
                    .environment(\.layoutDirection, .leftToRight)
            }
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 8) {
                GridRow {
                    pill(L("weather.feelsLike"), temp(c.apparentTemperature))
                    pill(L("weather.humidity"), "%\(Int((c.humidity * 100).rounded()))")
                }
                GridRow {
                    pill(L("weather.wind"), Units.wind(c.wind.speed))
                    pill(L("weather.uv"), "\(c.uvIndex.value)")
                }
                GridRow {
                    pill(L("weather.rainChanceShort"), "%\(Int((rain * 100).rounded()))")
                    pill(L("weather.pressure"), Units.pressure(c.pressure))
                }
            }
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .card()
    }

    private func pill(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.caption).foregroundStyle(Theme.muted)
            Text(value).font(.headline).environment(\.layoutDirection, .leftToRight)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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
                                Text("%\(Int((h.precipitationChance * 100).rounded()))").font(.caption2).foregroundStyle(.blue)
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
        let days = Array(d.dailyForecast.prefix(7))
        let lo = days.map { $0.lowTemperature.converted(to: .celsius).value }.min() ?? 0
        let hi = days.map { $0.highTemperature.converted(to: .celsius).value }.max() ?? 1
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("weather.dailyHeading"))
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
            }
        }
        .card()
    }

    private func icon(_ name: String, size: CGFloat) -> some View {
        Image(systemName: name)
            .symbolRenderingMode(.multicolor)
            .font(.system(size: size))
            .frame(height: size * 1.1)
    }

    private func temp(_ t: Measurement<UnitTemperature>) -> String {
        Units.temp(t)
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
        let legal = Link(L("weather.legal"), destination: attribution?.legalPageURL ?? Self.fallbackLegalURL)
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
