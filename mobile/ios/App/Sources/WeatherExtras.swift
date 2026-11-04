import Charts
import SaatCore
import SwiftUI
import WeatherKit

/// Hava sekmesine ekler (2. analiz #107, #111, #113, #114; Android WeatherExtras.kt ile aynı kartlar):
/// saatlik eğri (sıcaklık, hissedilen, yağış, rüzgâr; dokununca saat ayrıntısı), gün ayrıntısı, dışarıdaki işlere
/// uygunluk (SaatCore Outdoor), 3 saatlik meteogram (WeatherKit verisi), model karşılaştırma ile hava kalitesi ve polen
/// (WeatherKit'te yok; Open-Meteo'dan, koordinat 2 ondalık).

// MARK: Ortak

private let rainBlue = Color(red: 0.12, green: 0.53, blue: 0.9)

private func celsius(_ t: Measurement<UnitTemperature>) -> Double { t.converted(to: .celsius).value }
private func kmh(_ s: Measurement<UnitSpeed>) -> Double { s.converted(to: .kilometersPerHour).value }

/// Pusula adı (8 yön)
private func compassName(_ deg: Double) -> String { L("weather.compass.\(WeatherWords.compass8(deg))") }

/// Saat için ortak çekirdeğin beklediği değerler
func outdoorHour(_ h: HourWeather) -> Outdoor.Hour {
    Outdoor.Hour(temp: celsius(h.temperature), feels: celsius(h.apparentTemperature),
                 rain: Int((h.precipitationChance * 100).rounded()), mm: h.precipitationAmount.converted(to: .millimeters).value,
                 wind: kmh(h.wind.speed), gust: h.wind.gust.map(kmh) ?? -1, code: WeatherSpeech.code(h.condition),
                 isDay: h.isDaylight, uv: Double(h.uvIndex.value))
}

private struct HourPoint: Identifiable {
    let date: Date
    let value: Double
    var id: Date { date }
}

// MARK: Saatlik eğri

struct HourlyChartCard: View {
    let weather: Weather
    let zone: TimeZone
    @State private var metric = 0
    @State private var selected: Date?

    private var hours: [HourWeather] {
        let now = Date().addingTimeInterval(-3600)
        return Array(weather.hourlyForecast.filter { $0.date > now }.prefix(24))
    }

    private func value(_ h: HourWeather) -> Double {
        switch metric {
        case 0: return Double(Units.tempValue(h.temperature))
        case 1: return Double(Units.tempValue(h.apparentTemperature))
        case 2: return (h.precipitationChance * 100).rounded()
        default: return Double(Units.windValue(h.wind.speed))
        }
    }

    private var color: Color { metric == 2 ? rainBlue : metric == 3 ? Theme.muted : Theme.accent }

    var body: some View {
        let list = hours
        let points = list.map { HourPoint(date: $0.date, value: value($0)) }
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("weather.hourlyHeading"))
            chips
            if points.count > 1 {
                chart(points)
                    .frame(height: 130)
                    .environment(\.layoutDirection, .leftToRight)
                if let h = nearest(list) { detail(h) }
            }
            Text(L("wx.tapHint")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }

    private var chips: some View {
        let labels = [L("wx.temp"), L("weather.feelsLike"), L("weather.precipitation"), L("weather.wind")]
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(0..<labels.count, id: \.self) { i in
                    Button { metric = i } label: {
                        Text(labels[i])
                            .font(.footnote.weight(i == metric ? .semibold : .regular))
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(Capsule().fill(i == metric ? Theme.accentSoft : Theme.surface2))
                            .foregroundStyle(i == metric ? Theme.accent : Theme.text)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(i == metric ? .isSelected : [])
                }
            }
        }
    }

    @ViewBuilder private func chart(_ points: [HourPoint]) -> some View {
        if metric == 2 {
            Chart(points) { p in
                BarMark(x: .value("t", p.date, unit: .hour), y: .value("v", p.value))
                    .foregroundStyle(rainBlue.opacity(0.6))
            }
            .chartYScale(domain: 0...100)
            .chartXSelection(value: $selected)
        } else {
            Chart(points) { p in
                AreaMark(x: .value("t", p.date), y: .value("v", p.value))
                    .foregroundStyle(color.opacity(0.12))
                LineMark(x: .value("t", p.date), y: .value("v", p.value))
                    .foregroundStyle(color)
                    .lineStyle(StrokeStyle(lineWidth: 3))
            }
            .chartYScale(domain: .automatic(includesZero: false))
            .chartXSelection(value: $selected)
        }
    }

    private func nearest(_ list: [HourWeather]) -> HourWeather? {
        guard let s = selected else { return list.first }
        return list.min { abs($0.date.timeIntervalSince(s)) < abs($1.date.timeIntervalSince(s)) }
    }

    private func detail(_ h: HourWeather) -> some View {
        let part = L(h.isDaylight ? "wx.day" : "wx.night")
        let rain = Fmt.percent(Int((h.precipitationChance * 100).rounded()))
        let mm = h.precipitationAmount.converted(to: .millimeters).value
        let mmText: String = mm > 0 ? " (" + Units.precip(mm) + ")" : ""
        let feels: String = L("weather.feelsLike") + " " + Units.temp(h.apparentTemperature)
        let line2: String = Units.temp(h.temperature) + " · " + feels + " · " + rain + mmText
        let dirText: String = T("weather.windFrom", compassName(h.wind.direction.converted(to: .degrees).value))
        let hum: String = L("weather.humidity") + " " + Fmt.percent(Int((h.humidity * 100).rounded()))
        let line3: String = L("weather.wind") + " " + Units.wind(h.wind.speed) + " · " + dirText + " · " + hum
        let head: String = Fmt.time(h.date, tz: zone) + " · " + h.condition.description + " · " + part
        return HStack(spacing: 10) {
            WeatherIcon3D(symbol: h.symbolName, size: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(head).font(.subheadline.weight(.semibold))
                Text(line2).font(.footnote).foregroundStyle(Theme.muted)
                Text(line3).font(.footnote).foregroundStyle(Theme.muted)
            }
            Spacer(minLength: 0)
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 12).fill(Theme.surface2.opacity(0.5)))
    }
}

// MARK: Gün ayrıntısı

struct DayDetailSheet: View {
    let weather: Weather
    let day: DayWeather
    let zone: TimeZone
    @Environment(\.dismiss) private var dismiss

    private var hours: [HourWeather] {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = zone
        return weather.hourlyForecast.filter { cal.isDate($0.date, inSameDayAs: day.date) }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Label { Text(day.condition.description) } icon: { WeatherIcon3D(symbol: day.symbolName, size: 26) }
                    Text("↑ \(Units.temp(day.highTemperature))   ↓ \(Units.temp(day.lowTemperature))")
                        .environment(\.layoutDirection, .leftToRight)
                }
                Section { dayParts }
                Section { values }
            }
            .themedBackground()
            .navigationTitle(Fmt.longDate(day.date, tz: zone))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
        .tint(Theme.accent)
    }

    @ViewBuilder private var dayParts: some View {
        let dayHours = hours.filter(\.isDaylight)
        let nightHours = hours.filter { !$0.isDaylight }
        if let top = dayHours.max(by: { celsius($0.temperature) < celsius($1.temperature) }) {
            Text(L("wx.day") + ": " + top.condition.description + " · " + Units.temp(top.temperature))
        }
        if let low = nightHours.min(by: { celsius($0.temperature) < celsius($1.temperature) }) {
            Text(L("wx.night") + ": " + low.condition.description + " · " + Units.temp(low.temperature))
        }
    }

    @ViewBuilder private var values: some View {
        let mm = day.precipitationAmount.converted(to: .millimeters).value
        let chance: String = Fmt.percent(Int((day.precipitationChance * 100).rounded()))
        let rainLine: String = L("weather.rainChanceShort") + ": " + chance + " · " + L("weather.precipitation") + ": " + Units.precip(mm)
        Text(rainLine)
        let hums: [Double] = hours.map(\.humidity)
        if let lo = hums.min(), let hi = hums.max() {
            let loS: String = Fmt.percent(Int((lo * 100).rounded()))
            let hiS: String = Fmt.percent(Int((hi * 100).rounded()))
            Text(L("weather.humidity") + ": " + loS + "–" + hiS)
        }
        if let strongest = hours.max(by: { kmh($0.wind.speed) < kmh($1.wind.speed) }) {
            let dir: String = compassName(strongest.wind.direction.converted(to: .degrees).value)
            let windLine: String = L("wx.windMax") + ": " + Units.wind(strongest.wind.speed) + " · " + T("weather.windFrom", dir)
            Text(windLine)
        }
        Text(L("weather.uv") + ": \(day.uvIndex.value)")
        let rise: String = day.sun.sunrise.map { Fmt.time($0, tz: zone) } ?? "–"
        let set: String = day.sun.sunset.map { Fmt.time($0, tz: zone) } ?? "–"
        let sunLine: String = L("weather.sunrise") + " " + rise + " · " + L("weather.sunset") + " " + set
        Text(sunLine)
    }
}

// MARK: Dışarıdaki işler

struct ActivitiesCard: View {
    let weather: Weather
    let zone: TimeZone

    private var colors: [Color] { [Theme.surface2, Color(red: 0.95, green: 0.70, blue: 0.24), Theme.ok] }

    var body: some View {
        let now = Date().addingTimeInterval(-3600)
        let items = Array(weather.hourlyForecast.filter { $0.date > now }.prefix(12))
        let hours = items.map(outdoorHour)
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("act.title"))
            ForEach(Outdoor.Activity.allCases, id: \.self) { a in
                row(a, items: items, hours: hours)
            }
            legend
            Text(L("act.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }

    private func row(_ a: Outdoor.Activity, items: [HourWeather], hours: [Outdoor.Hour]) -> some View {
        let best = Outdoor.best(a, hours)
        let bestText: String
        if let b = best {
            bestText = T("act.best", Fmt.time(items[b].date, tz: zone)) + " · " + L("act.lvl.\(Outdoor.score(a, hours[b]))")
        } else {
            bestText = L("act.none")
        }
        return VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text(L("act.\(a.rawValue)")).font(.subheadline.weight(.semibold)).frame(width: 92, alignment: .leading)
                HStack(spacing: 2) {
                    ForEach(0..<hours.count, id: \.self) { i in
                        RoundedRectangle(cornerRadius: 3).fill(colors[Outdoor.score(a, hours[i])]).frame(height: 16)
                    }
                }
                .environment(\.layoutDirection, .leftToRight)
            }
            Text(bestText).font(.caption).foregroundStyle(Theme.muted).padding(.leading, 100)
        }
    }

    private var legend: some View {
        HStack(spacing: 12) {
            ForEach([2, 1, 0], id: \.self) { lv in
                HStack(spacing: 4) {
                    Circle().fill(colors[lv]).frame(width: 10, height: 10)
                    Text(L("act.lvl.\(lv)")).font(.caption).foregroundStyle(Theme.muted)
                }
            }
        }
    }
}

// MARK: Meteogram

struct MeteogramCard: View {
    let weather: Weather
    let zone: TimeZone

    var body: some View {
        let now = Date().addingTimeInterval(-3600)
        let all = Array(weather.hourlyForecast.filter { $0.date > now }.prefix(48))
        let items = all.enumerated().filter { $0.offset % 3 == 0 }.map(\.element)
        VStack(alignment: .leading, spacing: 6) {
            SectionTitle(text: L("meteo.title"))
            header
            ForEach(items, id: \.date) { h in row(h) }
        }
        .environment(\.layoutDirection, .leftToRight)
        .card()
    }

    private var header: some View {
        HStack {
            Text("").frame(maxWidth: .infinity)
            Text("").frame(width: 30)
            cell(L("wx.temp"))
            cell("mm")
            cell(L("weather.wind") + "\n" + Units.windUnit)
            cell(L("weather.gusts") + "\n" + Units.windUnit)
            cell(L("meteo.dir"))
        }
        .font(.caption2)
        .foregroundStyle(Theme.muted)
    }

    private func cell(_ text: String) -> some View {
        Text(text).multilineTextAlignment(.center).lineLimit(2).minimumScaleFactor(0.7).frame(maxWidth: .infinity)
    }

    private func row(_ h: HourWeather) -> some View {
        let mm = h.precipitationAmount.converted(to: .millimeters).value
        let dir = h.wind.direction.converted(to: .degrees).value
        return HStack {
            Text(Fmt.shortDateTime(h.date, tz: zone)).font(.caption).frame(maxWidth: .infinity, alignment: .leading)
            WeatherIcon3D(symbol: h.symbolName, size: 24).frame(width: 30)
            Text(Units.temp(h.temperature)).font(.footnote.weight(.semibold)).frame(maxWidth: .infinity)
            Text(mm > 0 ? String(format: "%.1f", mm) : "–").font(.footnote).foregroundStyle(mm > 0 ? rainBlue : Theme.muted)
                .frame(maxWidth: .infinity)
            Text("\(Units.windValue(h.wind.speed))").font(.footnote).frame(maxWidth: .infinity)
            Text(h.wind.gust.map { "\(Units.windValue($0))" } ?? "–").font(.footnote).foregroundStyle(Theme.muted).frame(maxWidth: .infinity)
            Image(systemName: "arrow.up").rotationEffect(.degrees(dir + 180)).foregroundStyle(Theme.accent).frame(maxWidth: .infinity)
        }
    }
}

// MARK: Open-Meteo: hava kalitesi, polen ve model karşılaştırma

struct AirData: Sendable {
    let eaqi: Double
    let pm25: Double?
    let pm10: Double?
    let ozone: Double?
    let no2: Double?
    /// alder, birch, grass, mugwort, olive, ragweed → tane/m³
    let pollen: [(String, Double)]
}

struct ModelDay: Sendable {
    let date: String
    let max: Double
    let min: Double
    let mm: Double
}

@MainActor
enum OpenMeteoExtras {
    static let pollenKinds = ["alder", "birch", "grass", "mugwort", "olive", "ragweed"]
    static let models = [("ecmwf_ifs025", "ECMWF"), ("gfs_seamless", "GFS"), ("icon_seamless", "ICON")]
    private static var airCache: (String, Date, AirData)?
    private static var modelCache: (String, Date, [(String, [ModelDay])])?

    private static func key(_ lat: Double, _ lon: Double) -> String { String(format: "%.2f,%.2f", lat, lon) }

    private static func json(_ url: String) async -> [String: Any]? {
        guard let u = URL(string: url) else { return nil }
        var req = URLRequest(url: u, timeoutInterval: 15)
        req.setValue("SaatTarih/1.0", forHTTPHeaderField: "User-Agent")
        guard let (data, resp) = try? await URLSession.shared.data(for: req),
              (resp as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }

    static func air(lat: Double, lon: Double) async -> AirData? {
        let k = key(lat, lon)
        if let c = airCache, c.0 == k, Date().timeIntervalSince(c.1) < 1800 { return c.2 }
        let parts = k.split(separator: ",")
        let fields = "european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide," + pollenKinds.map { "\($0)_pollen" }.joined(separator: ",")
        let url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=\(parts[0])&longitude=\(parts[1])&timezone=auto&current=\(fields)"
        guard let o = await json(url), let c = o["current"] as? [String: Any], let eaqi = c["european_aqi"] as? Double else {
            return airCache?.0 == k ? airCache?.2 : nil
        }
        let pollen = pollenKinds.compactMap { p in (c["\(p)_pollen"] as? Double).map { (p, $0) } }
        let v = AirData(eaqi: eaqi, pm25: c["pm2_5"] as? Double, pm10: c["pm10"] as? Double, ozone: c["ozone"] as? Double,
                        no2: c["nitrogen_dioxide"] as? Double, pollen: pollen)
        airCache = (k, Date(), v)
        return v
    }

    static func modelDays(lat: Double, lon: Double) async -> [(String, [ModelDay])]? {
        let k = key(lat, lon)
        if let c = modelCache, c.0 == k, Date().timeIntervalSince(c.1) < 3600 { return c.2 }
        let parts = k.split(separator: ",")
        let url = "https://api.open-meteo.com/v1/forecast?latitude=\(parts[0])&longitude=\(parts[1])&timezone=auto&forecast_days=7"
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&models=" + models.map(\.0).joined(separator: ",")
        guard let o = await json(url), let d = o["daily"] as? [String: Any], let t = d["time"] as? [String] else { return nil }
        var out: [(String, [ModelDay])] = []
        for (id, name) in models {
            let mx = d["temperature_2m_max_\(id)"] as? [Any] ?? []
            let mn = d["temperature_2m_min_\(id)"] as? [Any] ?? []
            let pr = d["precipitation_sum_\(id)"] as? [Any] ?? []
            var days: [ModelDay] = []
            for (i, date) in t.enumerated() where i < mx.count && i < mn.count {
                guard let a = mx[i] as? Double, let b = mn[i] as? Double else { continue }
                days.append(ModelDay(date: date, max: a, min: b, mm: (i < pr.count ? pr[i] as? Double : nil) ?? 0))
            }
            if !days.isEmpty { out.append((name, days)) }
        }
        modelCache = (k, Date(), out)
        return out
    }
}

/// Avrupa hava kalitesi endeksi renkleri (EEA): iyi … son derece kötü
let aqiColors: [Color] = [
    Color(red: 0.31, green: 0.94, blue: 0.90), Color(red: 0.31, green: 0.80, blue: 0.67), Color(red: 0.94, green: 0.90, blue: 0.25),
    Color(red: 1.0, green: 0.31, blue: 0.31), Color(red: 0.59, green: 0.0, blue: 0.20), Color(red: 0.49, green: 0.13, blue: 0.51),
]

struct AirCard: View {
    let place: AppPlace
    @State private var data: AirData?

    var body: some View {
        Group {
            if let a = data { content(a) }
        }
        .task(id: place.key) { data = await OpenMeteoExtras.air(lat: place.lat, lon: place.lon) }
    }

    private func content(_ a: AirData) -> some View {
        let lv = Outdoor.aqiLevel(a.eaqi)
        let pol = a.pollen.filter { Outdoor.pollenLevel($0.1) > 0 }
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("aqi.title"))
            HStack(spacing: 12) {
                Text("\(Int(a.eaqi.rounded()))")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(lv >= 4 ? Color.white : Color(white: 0.1))
                    .frame(width: 52, height: 52)
                    .background(Circle().fill(aqiColors[lv]))
                VStack(alignment: .leading, spacing: 2) {
                    Text(L("aqi.lvl.\(lv)")).font(.headline)
                    Text(L("aqi.index")).font(.caption).foregroundStyle(Theme.muted)
                }
            }
            HStack {
                pollutant("PM2.5", a.pm25)
                pollutant("PM10", a.pm10)
                pollutant("O₃", a.ozone)
                pollutant("NO₂", a.no2)
            }
            .environment(\.layoutDirection, .leftToRight)
            Text(L("aqi.pollen")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
            if pol.isEmpty {
                Text(L("pollen.none")).font(.caption).foregroundStyle(Theme.muted)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(pol.indices, id: \.self) { i in pollenChip(pol[i].0, pol[i].1) }
                    }
                }
            }
            Text(L("aqi.source")).font(.caption2).foregroundStyle(Theme.muted)
        }
        .card()
    }

    private func pollutant(_ name: String, _ v: Double?) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(name).font(.caption).foregroundStyle(Theme.muted)
            Text(v.map { "\(Int($0.rounded()))" } ?? "–").font(.subheadline.weight(.semibold))
            Text("µg/m³").font(.caption2).foregroundStyle(Theme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func pollenChip(_ kind: String, _ grains: Double) -> some View {
        let pl = Outdoor.pollenLevel(grains)
        let tint = [Theme.surface2, aqiColors[1], aqiColors[2], aqiColors[3]][pl]
        return Text(L("pollen.\(kind)") + " · " + L("pollen.lvl.\(pl)"))
            .font(.footnote)
            .padding(.horizontal, 12).padding(.vertical, 6)
            .background(Capsule().fill(tint.opacity(0.35)))
    }
}

struct ModelsCard: View {
    let place: AppPlace
    @State private var data: [(String, [ModelDay])]?

    var body: some View {
        Group {
            if let m = data, let first = m.first { content(m, dates: Array(first.1.prefix(5).map(\.date))) }
        }
        .task(id: place.key) { data = await OpenMeteoExtras.modelDays(lat: place.lat, lon: place.lon) }
    }

    private func content(_ m: [(String, [ModelDay])], dates: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("models.title"))
            HStack {
                Text("").frame(maxWidth: .infinity)
                ForEach(m.indices, id: \.self) { i in
                    Text(m[i].0).font(.caption.weight(.semibold)).foregroundStyle(Theme.muted).frame(maxWidth: .infinity)
                }
            }
            ForEach(dates, id: \.self) { date in
                HStack {
                    Text(weekday(date)).font(.footnote).frame(maxWidth: .infinity, alignment: .leading)
                    ForEach(m.indices, id: \.self) { i in cell(m[i].1.first { $0.date == date }) }
                }
            }
            Text(L("models.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .environment(\.layoutDirection, .leftToRight)
        .card()
    }

    private func weekday(_ iso: String) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone(identifier: "UTC")
        guard let d = f.date(from: iso) else { return iso }
        return L("calendar.weekdaysShort.\(CalendarDay(d, timeZone: TimeZone(identifier: "UTC")!).weekday)")
    }

    private func cell(_ x: ModelDay?) -> some View {
        VStack(spacing: 1) {
            Text(x.map { Units.temp(celsius: $0.max) + " / " + Units.temp(celsius: $0.min) } ?? "–").font(.footnote)
            if let x, x.mm >= 0.1 { Text(Units.precip(x.mm)).font(.caption2).foregroundStyle(rainBlue) }
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: Resmî uyarılar (2. analiz #112)

/// WeatherKit'in resmî hava uyarıları (Apple'ın kaynak sağladığı ülkelerde; yoksa kart görünmez). Her uyarıda kaynak kurum
/// ve ayrıntı bağlantısı gösterilir (WeatherKit koşulu).
struct WeatherAlertsCard: View {
    let alerts: [WeatherAlert]

    var body: some View {
        if !alerts.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                SectionTitle(text: L("wx.alerts"))
                ForEach(alerts.indices, id: \.self) { i in row(alerts[i]) }
            }
            .card()
        }
    }

    private func color(_ s: WeatherSeverity) -> Color {
        switch s {
        case .extreme: return Color(red: 0.6, green: 0.0, blue: 0.2)
        case .severe: return Theme.err
        case .moderate: return Color(red: 0.95, green: 0.6, blue: 0.1)
        default: return Color(red: 0.9, green: 0.8, blue: 0.2)
        }
    }

    private func row(_ a: WeatherAlert) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(color(a.severity))
            VStack(alignment: .leading, spacing: 3) {
                Text(a.summary).font(.subheadline.weight(.semibold))
                if let r = a.region { Text(r).font(.caption).foregroundStyle(Theme.muted) }
                Link(a.source, destination: a.detailsURL).font(.caption)
            }
        }
    }
}
