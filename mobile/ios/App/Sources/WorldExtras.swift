import CoreLocation
import SwiftUI
import WeatherKit

/// Dünya saatleri ekleri (2. analiz #129, #131; 4 Kasım 2026): şehir başına hava (WeatherKit, her yer en çok saatte
/// bir; "worldWx" anahtarında cihazda saklanır; ekranda Apple Hava Durumu işareti), küre / harita / 24 saat kadranı
/// seçimi ve sürüklenerek döndürülen 24 saat kadranı (üst yarı gündüz, 12.00 tepede; her şehir bir ibre).
@MainActor
final class WorldWeather: ObservableObject {
    static let shared = WorldWeather()

    struct Entry: Codable {
        let at: Date
        let symbol: String
        let celsius: Double
    }

    @Published private(set) var version = 0
    private var cache: [String: Entry]
    private var loading = Set<String>()
    private static let key = "worldWx"
    private static let ttl: TimeInterval = 3600

    private init() {
        cache = UserDefaults.standard.data(forKey: Self.key)
            .flatMap { try? JSONDecoder().decode([String: Entry].self, from: $0) } ?? [:]
    }

    func cached(_ key: String) -> Entry? {
        guard let e = cache[key], Date().timeIntervalSince(e.at) < 6 * Self.ttl else { return nil }
        return e
    }

    var hasAny: Bool { cache.values.contains { Date().timeIntervalSince($0.at) < 6 * Self.ttl } }

    func refresh(_ list: [WorldEntry]) {
        let due = list.prefix(30).filter { e in
            e.lat != nil && e.lon != nil && !loading.contains(e.key) &&
                (cache[e.key].map { Date().timeIntervalSince($0.at) > Self.ttl } ?? true)
        }
        guard !due.isEmpty else { return }
        due.forEach { loading.insert($0.key) }
        Task {
            for e in due {
                defer { loading.remove(e.key) }
                guard let lat = e.lat, let lon = e.lon,
                      let w = try? await WeatherService.shared.weather(for: CLLocation(latitude: lat, longitude: lon), including: .current)
                else { continue }
                cache[e.key] = Entry(at: Date(), symbol: w.symbolName, celsius: w.temperature.converted(to: .celsius).value)
                if let data = try? JSONEncoder().encode(cache) { UserDefaults.standard.set(data, forKey: Self.key) }
                version += 1
            }
        }
    }
}

/// Şehir satırındaki küçük hava (simge + sıcaklık)
struct WorldWeatherBadge: View {
    let key: String
    let color: Color
    @ObservedObject private var store = WorldWeather.shared

    var body: some View {
        if let e = store.cached(key) {
            HStack(spacing: 3) {
                WeatherIcon3D(symbol: e.symbol, size: 16)
                Text(Units.temp(celsius: e.celsius))
            }
            .font(.caption2.weight(.semibold))
            .foregroundStyle(color)
            .environment(\.layoutDirection, .leftToRight)
        }
    }
}

/// Küre / harita / 24 saat kadranı seçimi
struct WorldViewPicker: View {
    @Binding var view: String

    var body: some View {
        Picker("", selection: $view) {
            Text(L("world.view.globe")).tag("globe")
            Text(L("world.view.map")).tag("map")
            Text(L("world.view.dial")).tag("dial")
        }
        .pickerStyle(.segmented)
        .labelsHidden()
    }
}

/// 24 saatlik çok ibreli kadran; parmakla döndürmek saati kaydırır (360° = 24 saat)
struct WorldDial: View {
    let entries: [WorldEntry]
    let date: Date
    let onShift: (Date) -> Void
    @State private var lastAngle: Double?

    private func hours(_ e: WorldEntry) -> Double {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = e.zone
        let c = cal.dateComponents([.hour, .minute], from: date)
        return Double(c.hour ?? 0) + Double(c.minute ?? 0) / 60
    }

    /// 0 saat altta, 12 üstte; saat yönünde artar
    private static func unit(_ h: Double) -> CGPoint {
        let a = (h / 24 * 360 + 90) * Double.pi / 180
        return CGPoint(x: cos(a), y: sin(a))
    }

    var body: some View {
        VStack(spacing: 8) {
            GeometryReader { geo in
                let side = min(geo.size.width, geo.size.height)
                let r = side / 2 - 4
                let c = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
                ZStack {
                    dialFace(c: c, r: r)
                    hands(c: c, r: r)
                    Circle().fill(Theme.accent).frame(width: 12, height: 12).position(c)
                }
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 2)
                        .onChanged { v in
                            let a = atan2(Double(v.location.y - c.y), Double(v.location.x - c.x))
                            if let last = lastAngle {
                                var da = a - last
                                if da > .pi { da -= 2 * .pi }
                                if da < -.pi { da += 2 * .pi }
                                onShift(date.addingTimeInterval(da / (2 * .pi) * 86400))
                            }
                            lastAngle = a
                        }
                        .onEnded { _ in lastAngle = nil }
                )
            }
            .aspectRatio(1, contentMode: .fit)
            .frame(maxWidth: 360)
            Text(L("world.dialHint")).font(.caption).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
            legend
        }
    }

    private func dialFace(c: CGPoint, r: CGFloat) -> some View {
        let dayColor = Color(red: 0.835, green: 0.91, blue: 1)
        let nightColor = Color(red: 0.106, green: 0.141, blue: 0.251)
        return ZStack {
            Path { p in p.addArc(center: c, radius: r, startAngle: .degrees(180), endAngle: .degrees(360), clockwise: false) }.fill(dayColor)
            Path { p in p.addArc(center: c, radius: r, startAngle: .degrees(0), endAngle: .degrees(180), clockwise: false) }.fill(nightColor)
            Circle().stroke(Theme.muted.opacity(0.4), lineWidth: 1.5).frame(width: 2 * r, height: 2 * r).position(c)
            ForEach(0..<24, id: \.self) { h in
                let u = Self.unit(Double(h))
                let major = h % 3 == 0
                let ink: Color = (6...18).contains(h) ? Color(red: 0.08, green: 0.14, blue: 0.23) : Color(red: 0.91, green: 0.93, blue: 0.97)
                Path { p in
                    p.move(to: CGPoint(x: c.x + u.x * r * 0.9, y: c.y + u.y * r * 0.9))
                    p.addLine(to: CGPoint(x: c.x + u.x * r * (major ? 0.8 : 0.86), y: c.y + u.y * r * (major ? 0.8 : 0.86)))
                }
                .stroke(ink.opacity(0.7), lineWidth: major ? 2 : 1)
                if major {
                    let dy: CGFloat = (h == 6 || h == 18) ? -9 : 0
                    Text("\(h)").font(.system(size: 11, weight: .medium)).foregroundStyle(ink)
                        .position(x: c.x + u.x * r * 0.7, y: c.y + u.y * r * 0.7 + dy)
                }
            }
        }
    }

    private func hands(c: CGPoint, r: CGFloat) -> some View {
        let list = Array(entries.prefix(8))
        return ZStack {
            ForEach(Array(list.enumerated()), id: \.offset) { i, e in
                let u = Self.unit(hours(e))
                let len = r * (0.62 - CGFloat(i % 4) * 0.08)
                let tip = CGPoint(x: c.x + u.x * len, y: c.y + u.y * len)
                let color = WorldMath.handColors[i % WorldMath.handColors.count]
                Path { p in p.move(to: c); p.addLine(to: tip) }
                    .stroke(color, style: StrokeStyle(lineWidth: 3.5, lineCap: .round))
                Circle().fill(color).frame(width: 9, height: 9).position(tip)
                Text(String(e.displayName.prefix(10)))
                    .font(.system(size: 10.5, weight: .bold))
                    .foregroundStyle(.white)
                    .shadow(color: .black, radius: 2)
                    .fixedSize()
                    .position(x: c.x + u.x * (len + 16), y: c.y + u.y * (len + 16))
            }
        }
    }

    private var legend: some View {
        let list = Array(entries.prefix(8))
        return FlowLegend(items: list.enumerated().map { i, e in
            (WorldMath.handColors[i % WorldMath.handColors.count], e.displayName + " " + Fmt.time(date, tz: e.zone))
        })
    }
}

/// Renkli nokta + metin satırları (kadran açıklaması)
private struct FlowLegend: View {
    let items: [(Color, String)]

    var body: some View {
        let columns = [GridItem(.adaptive(minimum: 130), alignment: .leading)]
        LazyVGrid(columns: columns, alignment: .leading, spacing: 4) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, it in
                HStack(spacing: 5) {
                    Circle().fill(it.0).frame(width: 9, height: 9)
                    Text(it.1).font(.caption.weight(.semibold)).foregroundStyle(Theme.text).lineLimit(1)
                }
            }
        }
    }
}

/// Uygulamadaki harita kartı (dünya saatleri listesi)
struct WorldMapCard: View {
    let entries: [WorldEntry]
    let date: Date

    var body: some View {
        let pins: [MapPin] = entries.compactMap { e in
            guard let lat = e.lat, let lon = e.lon else { return nil }
            return MapPin(id: e.key, name: e.displayName, zone: e.zone, lat: lat, lon: lon)
        }
        WorldMapDrawing(pins: pins, date: date)
    }
}

/// Widget'lardan hızlı başlatma (2. analiz #136): zamanlayıcı (Siri'deki StartTimerIntent ile aynı kayıt) ve pomodoro
@MainActor
enum QuickStart {
    static func timer(minutes: Int) {
        let m = max(1, min(1440, minutes))
        let ms = Int64(m) * 60_000
        var t = TimerItem(name: T("watch.minutes", m), total: ms)
        t.end = TimerItem.nowMs + ms
        var items = TimerItem.load()
        items.append(t)
        TimerItem.save(items)
        if let id = UUID(uuidString: t.id) {
            let at = Date(timeIntervalSince1970: Double(t.end) / 1000)
            let title = t.doneTitle
            Task { await AlarmCenter.schedule(id: id, at: at, title: title) }
        }
        BadgeStore.shared.hit(.zamanlayici)
        LiveActivities.sync()
    }

    static func focus() {
        UserDefaults.standard.set("work", forKey: "pomo.phase")
        PomoEngine.start(seconds: PomoEngine.phaseSeconds)
    }
}
