import SwiftUI
import CoreLocation
import WeatherKit
import SaatCore

/// Hava durumu: seçili il için güncel, saatlik (24 saat) ve 7 günlük tahmin.
/// Sağlayıcı Apple WeatherKit (Apple Developer üyeliğinde ayda 500.000 çağrı dahil). Apple'ın şartı gereği
/// "Apple Hava Durumu" işareti ve yasal bilgiler bağlantısı ekranda gösterilir.
struct WeatherView: View {
    @AppStorage("province") private var provinceSlug = "istanbul"
    @Environment(\.colorScheme) private var scheme
    @State private var data: Weather?
    @State private var attribution: WeatherAttribution?
    @State private var failed = false

    private let zone = PrayerSchedule.zone

    var body: some View {
        let province = Province.find(provinceSlug)
        ScrollView {
            VStack(spacing: 18) {
                Label(province.displayName, systemImage: "mappin.and.ellipse")
                    .font(.headline)
                    .foregroundStyle(Theme.accent)
                if let d = data {
                    current(d)
                    hourly(d)
                    daily(d)
                } else if failed {
                    Text(L("weather.error")).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                    Button(L("weather.retry")) { Task { await load(province) } }.tint(Theme.accent)
                } else {
                    ProgressView(L("weather.loading")).padding(.top, 40)
                }
                attributionView
            }
            .padding(16)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("weather.h1"))
        .navigationBarTitleDisplayMode(.inline)
        .task(id: provinceSlug) { await load(province) }
        .refreshable { await load(province) }
    }

    private func load(_ p: Province) async {
        failed = false
        do {
            data = try await WeatherCache.weather(lat: p.lat, lon: p.lon)
        } catch {
            failed = data == nil
        }
        if attribution == nil { attribution = try? await WeatherService.shared.attribution }
    }

    /// Apple Hava Durumu işareti ve yasal bilgiler (WeatherKit şartı)
    @ViewBuilder
    private var attributionView: some View {
        if let a = attribution {
            VStack(spacing: 6) {
                AsyncImage(url: scheme == .dark ? a.combinedMarkDarkURL : a.combinedMarkLightURL) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    Text(a.serviceName).font(.caption)
                }
                .frame(height: 14)
                Link(L("weather.legal"), destination: a.legalPageURL)
                    .font(.caption)
                    .tint(Theme.muted)
            }
            .padding(.top, 4)
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
                    pill(L("weather.wind"), T("weather.km", Int(c.wind.speed.converted(to: .kilometersPerHour).value.rounded())))
                    pill(L("weather.uv"), "\(c.uvIndex.value)")
                }
                GridRow {
                    pill(L("weather.rainChanceShort"), "%\(Int((rain * 100).rounded()))")
                    pill(L("weather.pressure"), "\(Int(c.pressure.converted(to: .hectopascals).value.rounded())) hPa")
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
                HStack(spacing: 14) {
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
        "\(Int(t.converted(to: .celsius).value.rounded()))°"
    }
}

/// Aynı il için 15 dakika içinde yeniden sorulmaz (aylık çağrı hakkı idareli kullanılır)
@MainActor
enum WeatherCache {
    private static var cache: (key: String, at: Date, value: Weather)?

    static func weather(lat: Double, lon: Double) async throws -> Weather {
        let key = String(format: "%.2f,%.2f", lat, lon)
        if let c = cache, c.key == key, Date().timeIntervalSince(c.at) < 900 { return c.value }
        let value = try await WeatherService.shared.weather(for: CLLocation(latitude: lat, longitude: lon))
        cache = (key, Date(), value)
        return value
    }
}
