import SwiftUI
import WeatherKit
import SaatCore

/// Sitedeki saat sahnesinin 19 modeli (build.mjs STAGE_ORDER, aynı sıra ve kimlikler). Adlar sitedeki
/// `stage.models.<kimlik>` metinleri. Hava durumlu modeller (wx) seçili konumun havasını WeatherKit'ten alır.
enum ClockModel: String, CaseIterable, Identifiable {
    case wxDigitalCalWeek = "wx-digital-cal-week"
    case classic, analogDate = "analog-date", stack
    case wxAnalog = "wx-analog", wxStack = "wx-stack", wxAnalogDate = "wx-analog-date", wxFull = "wx-full"
    case hClassic = "h-classic", hAnalogDate = "h-analog-date", hStack = "h-stack"
    case hWxAnalog = "h-wx-analog", hWxStack = "h-wx-stack", hWxAnalogDate = "h-wx-analog-date", hWxFull = "h-wx-full"
    case digital, digitalDate = "digital-date", wxDigital = "wx-digital", wxDigitalCal = "wx-digital-cal"

    var id: String { rawValue }
    var name: String { L("stage.models.\(rawValue)") }
    var number: Int { (Self.allCases.firstIndex(of: self) ?? 0) + 1 }
}

/// Bir modelin çizimi; verilen alana sığacak şekilde ölçeklenir (sahne ve tam ekran)
struct ClockModelView: View {
    let model: ClockModel
    let now: Date
    let place: AppPlace

    var body: some View {
        GeometryReader { g in
            let w = g.size.width
            let h = g.size.height
            let s = min(w, h)
            Group {
                switch model {
                case .classic:
                    AnalogPart(now: now, size: s * 0.95)
                case .analogDate:
                    VStack(spacing: s * 0.04) {
                        AnalogPart(now: now, size: s * 0.78)
                        DatePart(now: now, size: s * 0.065)
                    }
                case .stack:
                    VStack(spacing: s * 0.03) {
                        AnalogPart(now: now, size: s * 0.66)
                        DigitalPart(now: now, size: s * 0.13)
                    }
                case .wxAnalog:
                    VStack(spacing: s * 0.04) {
                        AnalogPart(now: now, size: s * 0.66)
                        WeatherPart(kind: .line, place: place, size: s)
                    }
                case .wxStack:
                    VStack(spacing: s * 0.025) {
                        AnalogPart(now: now, size: s * 0.48)
                        DigitalPart(now: now, size: s * 0.1)
                        WeatherPart(kind: .card, place: place, size: s)
                    }
                case .wxAnalogDate:
                    VStack(spacing: s * 0.03) {
                        AnalogPart(now: now, size: s * 0.58)
                        DatePart(now: now, size: s * 0.06)
                        WeatherPart(kind: .line, place: place, size: s)
                    }
                case .wxFull:
                    VStack(spacing: s * 0.025) {
                        AnalogPart(now: now, size: s * 0.48)
                        DigitalPart(now: now, size: s * 0.1)
                        DatePart(now: now, size: s * 0.055)
                        WeatherPart(kind: .line, place: place, size: s)
                    }
                case .hClassic, .hAnalogDate, .hStack, .hWxAnalog, .hWxStack, .hWxAnalogDate, .hWxFull:
                    horizontal(w: w, h: h)
                case .digital:
                    DigitalPart(now: now, size: min(w * 0.2, h * 0.35))
                case .digitalDate:
                    VStack(spacing: s * 0.04) {
                        DigitalPart(now: now, size: min(w * 0.19, h * 0.3))
                        DatePart(now: now, size: s * 0.07)
                    }
                case .wxDigital:
                    VStack(spacing: s * 0.04) {
                        DigitalPart(now: now, size: min(w * 0.18, h * 0.26))
                        DatePart(now: now, size: s * 0.065)
                        WeatherPart(kind: .line, place: place, size: s)
                    }
                case .wxDigitalCal:
                    HStack(alignment: .center, spacing: w * 0.04) {
                        VStack(alignment: .leading, spacing: s * 0.03) {
                            DigitalPart(now: now, size: min(w * 0.085, h * 0.16))
                            DatePart(now: now, size: s * 0.05, alignment: .leading)
                            WeatherPart(kind: .line, place: place, size: s * 0.85, alignment: .leading)
                        }
                        .frame(width: w * 0.42)
                        MonthPart(now: now, width: w * 0.54)
                    }
                case .wxDigitalCalWeek:
                    VStack(spacing: s * 0.03) {
                        DigitalPart(now: now, size: min(w * 0.19, h * 0.2))
                        HStack(alignment: .top, spacing: w * 0.04) {
                            VStack(alignment: .leading, spacing: s * 0.025) {
                                DatePart(now: now, size: s * 0.048, alignment: .leading)
                                WeatherPart(kind: .week, place: place, size: s * 0.85, alignment: .leading)
                            }
                            .frame(width: w * 0.42)
                            MonthPart(now: now, width: w * 0.54)
                        }
                    }
                }
            }
            .frame(width: w, height: h)
        }
        .foregroundStyle(Theme.text)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(model.name)
    }

    /// 9–15: solda analog, sağda bilgi
    @ViewBuilder
    private func horizontal(w: CGFloat, h: CGFloat) -> some View {
        let a = min(w * 0.5, h * 0.9)
        let t = min(w, h)
        HStack(spacing: w * 0.05) {
            AnalogPart(now: now, size: a)
            VStack(alignment: .leading, spacing: t * 0.03) {
                switch model {
                case .hClassic:
                    Text(Self.zoneName(now))
                        .font(.system(size: t * 0.06, weight: .medium))
                        .foregroundStyle(Theme.muted)
                case .hAnalogDate:
                    DatePart(now: now, size: t * 0.065, alignment: .leading)
                case .hStack:
                    DigitalPart(now: now, size: t * 0.12)
                case .hWxAnalog:
                    WeatherPart(kind: .line, place: place, size: t, alignment: .leading)
                case .hWxStack:
                    DigitalPart(now: now, size: t * 0.11)
                    WeatherPart(kind: .line, place: place, size: t, alignment: .leading)
                case .hWxAnalogDate:
                    DatePart(now: now, size: t * 0.055, alignment: .leading)
                    WeatherPart(kind: .line, place: place, size: t, alignment: .leading)
                default:
                    DigitalPart(now: now, size: t * 0.1)
                    DatePart(now: now, size: t * 0.05, alignment: .leading)
                    WeatherPart(kind: .line, place: place, size: t, alignment: .leading)
                }
            }
            .frame(maxWidth: w - a - w * 0.05, alignment: .leading)
        }
    }

    static func zoneName(_ now: Date) -> String {
        TimeZone.current.localizedName(for: TimeZone.current.isDaylightSavingTime(for: now) ? .daylightSaving : .standard,
                                       locale: AppLocale.locale) ?? TimeZone.current.identifier
    }
}

// MARK: Parçalar

/// Analog saat; dokununca analog saniye açılır/kapanır (sitedeki gibi: dokunulan saatin türü)
private struct AnalogPart: View {
    let now: Date
    let size: CGFloat
    @AppStorage("secA") private var seconds = false

    var body: some View {
        AnalogClock(date: now, showSeconds: seconds)
            .frame(width: size, height: size)
            .contentShape(Circle())
            .onTapGesture { seconds.toggle() }
    }
}

/// Dijital saat; dokununca dijital saniye açılır/kapanır
private struct DigitalPart: View {
    let now: Date
    let size: CGFloat
    @AppStorage("secD") private var seconds = true
    @AppStorage("h12") private var h12 = false

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: size * 0.08) {
            Text(Fmt.time(now, seconds: seconds))
                .font(.system(size: size, weight: .bold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(Theme.ink)
            if h12 {
                Text(Fmt.ampm(now)).font(.system(size: size * 0.3, weight: .semibold))
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.3)
        .environment(\.layoutDirection, .leftToRight)
        .contentShape(Rectangle())
        .onTapGesture { seconds.toggle() }
    }
}

private struct DatePart: View {
    let now: Date
    let size: CGFloat
    var alignment: TextAlignment = .center

    var body: some View {
        Text(Fmt.longDate(now))
            .font(.system(size: size, weight: .semibold))
            .multilineTextAlignment(alignment)
            .lineLimit(2)
            .minimumScaleFactor(0.6)
    }
}

/// Hava bloğu: line (simge, sıcaklık, durum · yer), card (+ hissedilen, en yüksek/düşük, nem, rüzgâr),
/// week (+ önümüzdeki 6 gün). Apple Hava Durumu işareti altta (WeatherKit şartı).
private struct WeatherPart: View {
    enum Kind { case line, card, week }
    let kind: Kind
    let place: AppPlace
    let size: CGFloat
    var alignment: HorizontalAlignment = .center
    @State private var data: Weather?

    var body: some View {
        let f = max(11, size * 0.042)
        VStack(alignment: alignment, spacing: f * 0.3) {
            if let d = data {
                let c = d.currentWeather
                HStack(spacing: f * 0.5) {
                    Image(systemName: c.symbolName).symbolRenderingMode(.multicolor)
                    Text(temp(c.temperature)).fontWeight(.semibold)
                }
                .font(.system(size: f * 1.7))
                Text(c.condition.description + " · " + place.name)
                    .font(.system(size: f))
                    .foregroundStyle(Theme.muted)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                if kind == .card {
                    let today = d.dailyForecast.first
                    Text(L("weather.feelsLike") + " " + temp(c.apparentTemperature)
                         + (today.map { " · " + temp($0.highTemperature) + " / " + temp($0.lowTemperature) } ?? ""))
                        .font(.system(size: f * 0.9))
                        .foregroundStyle(Theme.muted)
                    Text(L("weather.humidity") + " %\(Int((c.humidity * 100).rounded())) · " + L("weather.wind") + " "
                         + Units.wind(c.wind.speed))
                        .font(.system(size: f * 0.9))
                        .foregroundStyle(Theme.muted)
                }
                if kind == .week {
                    VStack(alignment: .leading, spacing: f * 0.25) {
                        ForEach(Array(d.dailyForecast.dropFirst().prefix(6).enumerated()), id: \.offset) { _, day in
                            HStack(spacing: f * 0.4) {
                                Text(L("calendar.weekdaysShort.\(CalendarDay(day.date, timeZone: place.zone).weekday)"))
                                    .frame(width: f * 2.6, alignment: .leading)
                                Image(systemName: day.symbolName).symbolRenderingMode(.multicolor)
                                    .frame(width: f * 1.6)
                                Text(temp(day.highTemperature)).fontWeight(.semibold)
                                Text(temp(day.lowTemperature)).foregroundStyle(Theme.muted)
                            }
                            .font(.system(size: f))
                        }
                    }
                }
                AppleWeatherMark(compact: true)
            } else {
                ProgressView()
            }
        }
        .environment(\.layoutDirection, AppLocale.isRTL ? .rightToLeft : .leftToRight)
        .task(id: place.key) { data = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) }
    }

    private func temp(_ t: Measurement<UnitTemperature>) -> String {
        Units.temp(t)
    }
}

/// Aylık takvim (pazartesi başlar; bugün vurgulu, tatiller renkli). Güne dokununca Takvim sekmesinde açılır.
private struct MonthPart: View {
    let now: Date
    let width: CGFloat

    var body: some View {
        let today = CalendarDay(now)
        let first = today.firstOfMonth
        let count = CalendarDay.daysIn(today.year, today.month)
        let cells: [CalendarDay?] = Array(repeating: nil, count: first.weekday) + (0..<count).map { first.adding(days: $0) }
        let cell = width / 7
        let f = cell * 0.42
        VStack(spacing: cell * 0.12) {
            Text(Fmt.monthTitle(today.year, today.month))
                .font(.system(size: f * 1.15, weight: .semibold))
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(cell), spacing: 0), count: 7), spacing: cell * 0.08) {
                ForEach(0..<7, id: \.self) { i in
                    Text(L("calendar.weekdaysShort.\(i)"))
                        .font(.system(size: f * 0.8, weight: .semibold))
                        .foregroundStyle(i >= 5 ? Theme.accent : Theme.muted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                }
                ForEach(Array(cells.enumerated()), id: \.offset) { _, d in
                    if let d {
                        let holiday = CalendarData.shared.events(on: d).contains { $0.type == "holiday" }
                        Text("\(d.day)")
                            .font(.system(size: f, weight: d == today ? .bold : .regular).monospacedDigit())
                            .foregroundStyle(d == today ? Theme.surface : holiday ? CalendarData.color("holiday") : Theme.text)
                            .frame(width: cell * 0.86, height: cell * 0.86)
                            .background(d == today ? Theme.accent : .clear, in: Circle())
                            .contentShape(Rectangle())
                            .onTapGesture {
                                Router.shared.calendarDay = d
                                Router.shared.tab = .calendar
                            }
                    } else {
                        Color.clear.frame(width: cell * 0.86, height: cell * 0.86)
                    }
                }
            }
        }
        .frame(width: width)
        .environment(\.layoutDirection, AppLocale.isRTL ? .rightToLeft : .leftToRight)
    }
}
