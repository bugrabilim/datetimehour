import SwiftUI
import SaatCore

/// Hava durumu: seçili il için güncel, saatlik (24 saat) ve 7 günlük tahmin.
/// Sağlayıcı Open-Meteo (sitedeki gibi; anahtarsız). Koordinat 2 ondalığa yuvarlanır (yaklaşık 1 km).
/// Lisans notu: Open-Meteo ücretsiz katmanı ticari olmayan kullanım içindir; mağaza yayınından önce karar verilecek.
struct WeatherView: View {
    @AppStorage("province") private var provinceSlug = "istanbul"
    @State private var data: Forecast?
    @State private var failed = false

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
                Link(L("weather.attribution"), destination: URL(string: "https://open-meteo.com/")!)
                    .font(.caption)
                    .tint(Theme.muted)
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
        do { data = try await Forecast.fetch(lat: p.lat, lon: p.lon) } catch { failed = data == nil }
    }

    private func current(_ d: Forecast) -> some View {
        let c = d.current
        let info = Forecast.info(c.weather_code, day: c.is_day == 1)
        return VStack(spacing: 6) {
            Text(info.icon).font(.system(size: 64))
            Text(Forecast.temp(c.temperature_2m))
                .font(.system(size: 56, weight: .semibold, design: .rounded))
                .environment(\.layoutDirection, .leftToRight)
            Text(L("weather.codes.\(info.key)")).font(.title3)
            if let hi = d.daily.temperature_2m_max.first, let lo = d.daily.temperature_2m_min.first {
                Text("↑ \(Forecast.temp(hi))   ↓ \(Forecast.temp(lo))")
                    .foregroundStyle(Theme.muted)
                    .environment(\.layoutDirection, .leftToRight)
            }
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 8) {
                GridRow {
                    pill(L("weather.feelsLike"), Forecast.temp(c.apparent_temperature))
                    pill(L("weather.humidity"), "%\(Int(c.relative_humidity_2m.rounded()))")
                }
                GridRow {
                    pill(L("weather.wind"), T("weather.km", Int(c.wind_speed_10m.rounded())))
                    pill(L("weather.uv"), String(format: "%.0f", c.uv_index ?? 0))
                }
                GridRow {
                    pill(L("weather.precipitation"), String(format: "%.1f mm", c.precipitation))
                    pill(L("weather.pressure"), "\(Int(c.pressure_msl.rounded())) hPa")
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

    private func hourly(_ d: Forecast) -> some View {
        let now = Date()
        let items = d.hourlyItems.filter { $0.date > now.addingTimeInterval(-3600) }.prefix(24)
        return VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("weather.hourlyHeading"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(Array(items), id: \.date) { h in
                        VStack(spacing: 4) {
                            Text(Fmt.time(h.date, tz: d.zone)).font(.caption).foregroundStyle(Theme.muted)
                            Text(Forecast.info(h.code, day: h.isDay).icon).font(.title2)
                            Text(Forecast.temp(h.temp)).font(.subheadline.weight(.semibold))
                            if h.rain > 0 { Text("%\(h.rain)").font(.caption2).foregroundStyle(.blue) }
                        }
                        .environment(\.layoutDirection, .leftToRight)
                    }
                }
            }
        }
        .card()
    }

    private func daily(_ d: Forecast) -> some View {
        let lo = d.daily.temperature_2m_min.min() ?? 0
        let hi = d.daily.temperature_2m_max.max() ?? 1
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("weather.dailyHeading"))
            ForEach(0..<d.daily.time.count, id: \.self) { i in
                let day = CalendarDay(iso: d.daily.time[i])
                HStack(spacing: 10) {
                    Text(day.map { i == 0 ? L("client.today") : L("calendar.weekdaysShort.\($0.weekday)") } ?? "")
                        .frame(width: 52, alignment: .leading)
                    Text(Forecast.info(d.daily.weather_code[i], day: true).icon)
                    Text(Forecast.temp(d.daily.temperature_2m_min[i])).foregroundStyle(Theme.muted).frame(width: 40)
                    GeometryReader { g in
                        let w = g.size.width
                        let span = max(1, hi - lo)
                        let x0 = (d.daily.temperature_2m_min[i] - lo) / span * w
                        let x1 = (d.daily.temperature_2m_max[i] - lo) / span * w
                        ZStack(alignment: .leading) {
                            Capsule().fill(Theme.surface2).frame(height: 6)
                            Capsule().fill(LinearGradient(colors: [.blue, .orange], startPoint: .leading, endPoint: .trailing))
                                .frame(width: max(6, x1 - x0), height: 6)
                                .offset(x: x0)
                        }
                        .frame(maxHeight: .infinity)
                    }
                    .frame(height: 20)
                    Text(Forecast.temp(d.daily.temperature_2m_max[i])).frame(width: 40)
                }
                .environment(\.layoutDirection, .leftToRight)
            }
        }
        .card()
    }
}

/// Open-Meteo yanıtı (sitedeki weather-core.js ile aynı alanlar)
struct Forecast: Decodable {
    struct Current: Decodable {
        let temperature_2m: Double
        let apparent_temperature: Double
        let relative_humidity_2m: Double
        let precipitation: Double
        let weather_code: Int
        let wind_speed_10m: Double
        let wind_direction_10m: Double
        let pressure_msl: Double
        let is_day: Int
        let uv_index: Double?
    }
    struct Hourly: Decodable {
        let time: [String]
        let temperature_2m: [Double]
        let precipitation_probability: [Int?]
        let weather_code: [Int]
        let is_day: [Int]
    }
    struct Daily: Decodable {
        let time: [String]
        let weather_code: [Int]
        let temperature_2m_max: [Double]
        let temperature_2m_min: [Double]
    }
    let timezone: String
    let current: Current
    let hourly: Hourly
    let daily: Daily

    var zone: TimeZone { TimeZone(identifier: timezone) ?? .current }

    struct Hour { let date: Date; let temp: Double; let code: Int; let isDay: Bool; let rain: Int }
    var hourlyItems: [Hour] {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd'T'HH:mm"
        f.timeZone = zone
        return hourly.time.indices.compactMap { i in
            f.date(from: hourly.time[i]).map {
                Hour(date: $0, temp: hourly.temperature_2m[i], code: hourly.weather_code[i], isDay: hourly.is_day[i] == 1,
                     rain: hourly.precipitation_probability[i] ?? 0)
            }
        }
    }

    private static var cache: (key: String, at: Date, value: Forecast)?

    static func fetch(lat: Double, lon: Double) async throws -> Forecast {
        let la = String(format: "%.2f", lat), lo = String(format: "%.2f", lon), key = "\(la),\(lo)"
        if let c = cache, c.key == key, Date().timeIntervalSince(c.at) < 600 { return c.value }
        let url = URL(string: "https://api.open-meteo.com/v1/forecast?latitude=\(la)&longitude=\(lo)"
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,is_day,uv_index"
            + "&hourly=temperature_2m,precipitation_probability,weather_code,is_day"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
            + "&timezone=auto&forecast_days=7")!
        let (data, _) = try await URLSession.shared.data(from: url)
        let value = try JSONDecoder().decode(Forecast.self, from: data)
        cache = (key, Date(), value)
        return value
    }

    static func temp(_ t: Double) -> String { "\(Int(t.rounded()))°" }

    /// WMO hava kodu → metin anahtarı ve simge (sitedeki eşleme)
    static func info(_ c: Int, day: Bool) -> (key: String, icon: String) {
        let key: String
        switch c {
        case 0: key = "clear"
        case 1: key = "mostlyClear"
        case 2: key = "partly"
        case 3: key = "overcast"
        case 45, 48: key = "fog"
        case 51...55: key = "drizzle"
        case 56, 57: key = "freezingDrizzle"
        case 61...65: key = "rain"
        case 66, 67: key = "freezingRain"
        case 71...75: key = "snow"
        case 77: key = "snowGrains"
        case 80...82: key = "showers"
        case 85, 86: key = "snowShowers"
        case 95: key = "thunder"
        default: key = c >= 96 ? "thunderHail" : "overcast"
        }
        let icons: [String: (String, String)] = [
            "clear": ("☀️", "🌙"), "mostlyClear": ("🌤️", "🌙"), "partly": ("⛅", "☁️"), "overcast": ("☁️", "☁️"), "fog": ("🌫️", "🌫️"),
            "drizzle": ("🌦️", "🌧️"), "freezingDrizzle": ("🌧️", "🌧️"), "rain": ("🌧️", "🌧️"), "freezingRain": ("🌧️", "🌧️"),
            "snow": ("🌨️", "🌨️"), "snowGrains": ("❄️", "❄️"), "showers": ("🌦️", "🌧️"), "snowShowers": ("🌨️", "🌨️"),
            "thunder": ("⛈️", "⛈️"), "thunderHail": ("⛈️", "⛈️"),
        ]
        let pair = icons[key] ?? ("☁️", "☁️")
        return (key, day ? pair.0 : pair.1)
    }
}
