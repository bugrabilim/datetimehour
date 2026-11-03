import SwiftUI
import WeatherKit
import SaatCore

/// Sitedeki saat sahnesinin 20 modeli (build.mjs STAGE_ORDER, aynı sıra ve kimlikler). Adlar sitedeki
/// `stage.models.<kimlik>` metinleri. Hava durumlu modeller (wx) seçili konumun havasını WeatherKit'ten alır.
enum ClockModel: String, CaseIterable, Identifiable {
    case classic // 1. sırada: varsayılan saniyeli rakamlı analog (30 Ekim 2026, sahibin isteği)
    case wxDigitalCalWeek = "wx-digital-cal-week"
    case analogDate = "analog-date", stack
    case wxAnalog = "wx-analog", wxStack = "wx-stack", wxAnalogDate = "wx-analog-date", wxFull = "wx-full"
    case hClassic = "h-classic", hAnalogDate = "h-analog-date", hStack = "h-stack"
    case hWxAnalog = "h-wx-analog", hWxStack = "h-wx-stack", hWxAnalogDate = "h-wx-analog-date", hWxFull = "h-wx-full"
    case digital, digitalDate = "digital-date", wxDigital = "wx-digital", wxDigitalCal = "wx-digital-cal"
    case flip // 20: çevirmeli saat (30 Ekim 2026, rakip analizi #5)
    case dual // 21: ikili saat dilimi (2. analiz #12; yalnız uygulamalarda, sitede yok)

    var id: String { rawValue }
    var name: String { self == .dual ? L("dual.model") : L("stage.models.\(rawValue)") }
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
                case .dual:
                    DualClockPart(now: now, width: w, height: h)
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
                case .flip:
                    VStack(spacing: s * 0.05) {
                        FlipPart(now: now, width: w * 0.92, height: h * 0.55)
                        DatePart(now: now, size: s * 0.065)
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
    @AppStorage("secA") private var seconds = true // varsayılan: analogda saniye açık (30 Ekim 2026)
    @AppStorage(SecondsMotion.key) private var motionRaw = SecondsMotion.tick.rawValue

    var body: some View {
        let motion = SecondsMotion(rawValue: motionRaw) ?? .tick
        Group {
            // Mekanik ve kayan saniyede ibre saniyede birden sık çizilir (2. analiz #10)
            if seconds && motion != .tick {
                TimelineView(.animation(minimumInterval: motion.interval)) { ctx in
                    AnalogClock(date: ctx.date, showSeconds: true, motion: motion)
                }
            } else {
                AnalogClock(date: now, showSeconds: seconds)
            }
        }
            .frame(width: size, height: size)
            .contentShape(Circle())
            .onTapGesture {
                seconds.toggle()
                BadgeStore.shared.hit(.saniye)
            }
    }
}

/// Dijital saat; dokununca dijital saniye açılır/kapanır
private struct DigitalPart: View {
    let now: Date
    let size: CGFloat
    @AppStorage("secD") private var seconds = true
    @AppStorage("h12") private var h12 = false
    @AppStorage("clockFont") private var clockFont = ClockFont.rounded.rawValue

    /// Saniye küçük ve tabana hizalı (2. analiz #21)
    @AppStorage("secSmall") private var small = false
    /// Rakam rengi ve dış hatlı rakamlar (2. analiz #24)
    @AppStorage(ClockDigits.colorKey) private var digitColor = "theme"
    @AppStorage(ClockDigits.outlineKey) private var outline = false

    var body: some View {
        let font = ClockFont(rawValue: clockFont) ?? .rounded
        let ink = ClockDigits.color(digitColor)
        HStack(alignment: .firstTextBaseline, spacing: size * 0.08) {
            if seconds && small {
                HStack(alignment: .firstTextBaseline, spacing: 0) {
                    ClockDigits.text(Fmt.time(now), font: font.font(size), color: ink, outline: outline, width: size * 0.012)
                    ClockDigits.text(String(format: ":%02d", Calendar.current.component(.second, from: now)), font: font.font(size * 0.45),
                                     color: ink, outline: outline, width: size * 0.008)
                }
                .monospacedDigit()
            } else {
                ClockDigits.text(Fmt.time(now, seconds: seconds), font: font.font(size), color: ink, outline: outline, width: size * 0.012)
                    .monospacedDigit()
            }
            if h12 {
                Text(Fmt.ampm(now)).font(.system(size: size * 0.3, weight: .semibold))
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.3)
        .environment(\.layoutDirection, .leftToRight)
        .contentShape(Rectangle())
        .onTapGesture {
            seconds.toggle()
            BadgeStore.shared.hit(.saniye)
        }
    }
}

/// Çevirmeli saat: her rakam bir kart, değişince kart yukarıdan aşağı döner (hareketi azalt tercihinde dönmez).
/// Dokununca dijital saniye açılır/kapanır (sitedeki gibi dijital sayılır).
/// Çevirmeli saatin görünümü (2. analiz #20): iki rakam tek kartta, köşede küçük saniye, kart çizgisi, kart rengi.
/// Android'deki FlipLook ile aynı anahtarlar ve renkler.
enum FlipLook {
    static let pairKey = "flip.pair"
    static let smallSecKey = "flip.smallSec"
    static let hingeKey = "flip.hinge"
    static let cardKey = "flip.card"
    static let cards: [(id: String, bg: UInt32, fg: UInt32)] = [
        ("theme", 0, 0), ("black", 0x1A1B1E, 0xF5F5F5), ("white", 0xF7F7F5, 0x1A1B1E),
        ("red", 0xA61E1E, 0xFFFFFF), ("blue", 0x1F4E9C, 0xFFFFFF), ("green", 0x1E6B34, 0xFFFFFF),
    ]
    static func colors(_ id: String) -> (bg: Color, fg: Color) {
        guard id != "theme", let c = cards.first(where: { $0.id == id }) else { return (Theme.surface2, Theme.ink) }
        return (Color(hex: c.bg), Color(hex: c.fg))
    }
}

/// Rakam rengi ve dış hatlı rakam (2. analiz #24; Android'deki ClockLook.digitColor/outline ile aynı)
enum ClockDigits {
    static let colorKey = "look.digitColor"
    static let outlineKey = "look.outline"
    /// Büyük rakam için iki temada da okunur orta tonlar
    static let colors: [(id: String, rgb: UInt32)] = [
        ("red", 0xE03131), ("orange", 0xF76707), ("amber", 0xF59F00), ("green", 0x2F9E44),
        ("aqua", 0x1098AD), ("blue", 0x1C7ED6), ("violet", 0x7048E8), ("pink", 0xD6336C),
    ]
    static func color(_ id: String) -> Color {
        colors.first { $0.id == id }.map { Color(hex: $0.rgb) } ?? Theme.ink
    }

    /// Dolu ya da dış hatlı metin: dış hat dört yöne kaydırılmış kopyalarla çizilir, iç zemin rengiyle oyulur
    @ViewBuilder
    static func text(_ s: String, font: Font, color: Color, outline: Bool, width: CGFloat) -> some View {
        if outline {
            let w = max(1, width)
            ZStack {
                ForEach(0..<8, id: \.self) { i in
                    let a = Double(i) * .pi / 4
                    Text(s).font(font).foregroundStyle(color).offset(x: cos(a) * w, y: sin(a) * w)
                }
                Text(s).font(font).foregroundStyle(Theme.bg).blendMode(.destinationOut)
            }
            .compositingGroup()
        } else {
            Text(s).font(font).foregroundStyle(color)
        }
    }
}

/// Çevirmeli saat: her rakam (ya da iki rakam) bir kart. Sahne enine göre uzunsa saat ve dakika kartları alt alta;
/// 12 saat biçiminde ÖÖ/ÖS saat kartının köşesinde, küçük saniye seçiliyse dakika kartının köşesinde yazar.
private struct FlipPart: View {
    let now: Date
    let width: CGFloat
    let height: CGFloat
    @AppStorage("secD") private var seconds = true
    @AppStorage("h12") private var h12 = false
    @AppStorage(FlipLook.pairKey) private var pair = false
    @AppStorage(FlipLook.smallSecKey) private var smallSec = false
    @AppStorage(FlipLook.hingeKey) private var hinge = true
    @AppStorage(FlipLook.cardKey) private var cardID = "theme"

    var body: some View {
        let c = Calendar.current.dateComponents([.hour, .minute, .second], from: now)
        let hour = h12 ? ((c.hour ?? 0) % 12 == 0 ? 12 : (c.hour ?? 0) % 12) : (c.hour ?? 0)
        let small = seconds && smallSec
        let groups = [hour, c.minute ?? 0] + (seconds && !small ? [c.second ?? 0] : [])
        let vertical = height > width * 1.1
        let perGroup: CGFloat = pair ? 1.7 : 2.08
        let n = CGFloat(groups.count)
        let cardW = vertical ? min(width / (perGroup + 0.2), height / (n * 1.4 + (n - 1) * 0.25))
            : min(width / (n * perGroup + (n - 1) * 0.35), height / 1.4)
        let colors = FlipLook.colors(cardID)
        let ampm = h12 ? Fmt.ampm(now) : ""
        let layout = vertical ? AnyLayout(VStackLayout(spacing: cardW * 0.25)) : AnyLayout(HStackLayout(spacing: cardW * 0.35))
        layout {
            ForEach(Array(groups.enumerated()), id: \.offset) { i, v in
                let corner = i == 0 ? ampm : (i == 1 && small ? String(format: "%02d", c.second ?? 0) : "")
                if pair {
                    FlipCard(text: String(format: "%02d", v), width: cardW * 1.7, digit: cardW, colors: colors, hinge: hinge,
                             corner: corner, cornerLeading: i == 0)
                } else {
                    HStack(spacing: cardW * 0.08) {
                        FlipCard(text: "\(v / 10)", width: cardW, digit: cardW, colors: colors, hinge: hinge,
                                 corner: i == 0 ? corner : "", cornerLeading: true)
                        FlipCard(text: "\(v % 10)", width: cardW, digit: cardW, colors: colors, hinge: hinge,
                                 corner: i != 0 ? corner : "", cornerLeading: false)
                    }
                }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .contentShape(Rectangle())
        .onTapGesture {
            seconds.toggle()
            BadgeStore.shared.hit(.saniye)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Fmt.time(now, seconds: seconds))
    }
}

private struct FlipCard: View {
    let text: String
    let width: CGFloat
    let digit: CGFloat
    let colors: (bg: Color, fg: Color)
    let hinge: Bool
    let corner: String
    let cornerLeading: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var angle: Double = 0

    var body: some View {
        let h = digit * 1.4
        ZStack {
            RoundedRectangle(cornerRadius: digit * 0.12)
                .fill(colors.bg)
                .overlay(RoundedRectangle(cornerRadius: digit * 0.12).stroke(Theme.border.opacity(0.6), lineWidth: 1))
                .shadow(color: .black.opacity(0.12), radius: digit * 0.06, y: digit * 0.03)
            Text(text)
                .font(ClockFont.current.font(digit * 1.0))
                .monospacedDigit()
                .lineLimit(1)
                .foregroundStyle(colors.fg)
                .rotation3DEffect(.degrees(angle), axis: (x: 1, y: 0, z: 0), perspective: 0.5)
                .opacity(angle == 0 ? 1 : 0.4)
            // Ortadaki ayrım çizgisi
            if hinge { Rectangle().fill(Theme.bg).frame(height: max(1, digit * 0.03)) }
            if !corner.isEmpty {
                Text(corner)
                    .font(.system(size: digit * 0.2, weight: .bold).monospacedDigit())
                    .foregroundStyle(colors.fg.opacity(0.75))
                    .padding(digit * 0.07)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: cornerLeading ? .topLeading : .bottomTrailing)
            }
        }
        .frame(width: width, height: h)
        .onChange(of: text) {
            guard !reduceMotion else { return }
            angle = -80
            withAnimation(.easeOut(duration: 0.3)) { angle = 0 }
        }
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
