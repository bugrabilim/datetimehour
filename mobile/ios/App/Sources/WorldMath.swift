import SwiftUI
import UIKit

/// Dünya haritası ve 24 saat kadranı için ortak hesap ve çizim (uygulama ve widget; 2. analiz #129–#131).
/// Güneş'in alt noktası GlobeSky.sun ile aynı formül (sitedeki globe.js); widget hedefinde GlobeView olmadığı için
/// burada ayrıca durur.
enum WorldMath {
    private static let rad = Double.pi / 180

    private static func wrap(_ x: Double) -> Double {
        var v = (x + 180).truncatingRemainder(dividingBy: 360)
        if v < 0 { v += 360 }
        return v - 180
    }

    /// Güneş'in tepe noktası (enlem, boylam)
    static func sun(_ date: Date) -> (lat: Double, lon: Double) {
        let d = date.timeIntervalSince1970 / 86400 + 2440587.5 - 2451545.0
        let g = (357.529 + 0.98560028 * d) * rad
        let q = 280.459 + 0.98564736 * d
        let l = (q + 1.915 * sin(g) + 0.02 * sin(2 * g)) * rad
        let e = (23.439 - 0.00000036 * d) * rad
        let ra = atan2(cos(e) * sin(l), cos(l)) / rad
        let dec = asin(sin(e) * sin(l)) / rad
        let eqt = wrap(q - ra)
        var ms = (date.timeIntervalSince1970 * 1000).truncatingRemainder(dividingBy: 86_400_000)
        if ms < 0 { ms += 86_400_000 }
        let utcH = ms / 3_600_000
        return (dec, wrap(-15 * (utcH - 12) - eqt))
    }

    /// Bir noktada Güneş'in yüksekliği (derece)
    static func sunAltitude(lat: Double, lon: Double, at date: Date) -> Double {
        let s = sun(date)
        let v = sin(lat * rad) * sin(s.lat * rad) + cos(lat * rad) * cos(s.lat * rad) * cos((lon - s.lon) * rad)
        return asin(max(-1, min(1, v))) / rad
    }

    /// Eşdikdörtgen haritada gece bölgesi (terminatör: tan φ = −cos(λ − λs) / tan δ)
    static func nightPath(_ date: Date, size: CGSize) -> Path {
        let s = sun(date)
        let dec = abs(s.lat) < 0.1 ? 0.1 : s.lat
        let w = size.width, h = size.height
        var p = Path()
        var lon = -180.0
        var first = true
        while lon <= 180 {
            let lat = atan(-cos((lon - s.lon) * rad) / tan(dec * rad)) / rad
            let pt = CGPoint(x: CGFloat((lon + 180) / 360) * w, y: CGFloat((90 - lat) / 180) * h)
            if first { p.move(to: pt); first = false } else { p.addLine(to: pt) }
            lon += 3
        }
        // Kuzey yazında gece güneyde (alt kenara kapat), kışın kuzeyde
        let closeY: CGFloat = dec > 0 ? h : 0
        p.addLine(to: CGPoint(x: w, y: closeY))
        p.addLine(to: CGPoint(x: 0, y: closeY))
        p.closeSubpath()
        return p
    }

    static func point(lat: Double, lon: Double, size: CGSize) -> CGPoint {
        CGPoint(x: CGFloat((lon + 180) / 360) * size.width, y: CGFloat((90 - lat) / 180) * size.height)
    }

    /// Küçültülmüş Natural Earth dokusu (earth.jpg, 720×360)
    static let earth: UIImage? = {
        guard let path = Bundle.main.path(forResource: "earth", ofType: "jpg") else { return nil }
        return UIImage(contentsOfFile: path)
    }()

    /// Bir şehrin satırı/işareti için kısa kod: Latin adlarda üç harf, diğerlerinde ilk iki karakter
    static func code(_ name: String) -> String {
        let latin = name.unicodeScalars.allSatisfy { $0.value < 0x0250 || !CharacterSet.letters.contains($0) }
        return latin ? String(name.filter(\.isLetter).prefix(3)).uppercased() : String(name.prefix(2))
    }

    /// Gündüz / alacakaranlık / gece
    static func light(lat: Double?, lon: Double?, zone: TimeZone, at date: Date) -> String {
        guard let lat, let lon else {
            var cal = Calendar(identifier: .gregorian)
            cal.timeZone = zone
            let h = cal.component(.hour, from: date)
            return (7...17).contains(h) ? "day" : ([6, 18, 19].contains(h) ? "dusk" : "night")
        }
        let alt = sunAltitude(lat: lat, lon: lon, at: date)
        return alt > 0 ? "day" : (alt > -6 ? "dusk" : "night")
    }

    /// İbre renkleri (Android'deki 24 saat kadranıyla aynı)
    static let handColors: [Color] = [0xE4572E, 0x2E86AB, 0x3BB273, 0xF4B400, 0x8E44AD, 0xE91E63, 0x00ACC1, 0x795548].map { v in
        Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
    }
}

/// Haritada gösterilecek yer (uygulamada WorldEntry, widget'ta CityEntity'den)
struct MapPin: Identifiable {
    let id: String
    let name: String
    let zone: TimeZone
    let lat: Double
    let lon: Double
}

/// Gece gölgeli eşdikdörtgen dünya haritası, Güneş noktası ve şehirlerin saati
struct WorldMapDrawing: View {
    let pins: [MapPin]
    let date: Date
    var labels = true
    var fontSize: CGFloat = 11

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            ZStack(alignment: .topLeading) {
                if let img = WorldMath.earth {
                    Image(uiImage: img).resizable().frame(width: size.width, height: size.height)
                } else {
                    Color(red: 0.055, green: 0.1, blue: 0.18)
                }
                WorldMath.nightPath(date, size: size).fill(Color(red: 0.02, green: 0.04, blue: 0.1).opacity(0.8))
                let s = WorldMath.sun(date)
                let sp = WorldMath.point(lat: s.lat, lon: s.lon, size: size)
                Circle().fill(Color(red: 1, green: 0.79, blue: 0.3)).frame(width: 10, height: 10).position(sp)
                ForEach(pins) { p in
                    let pt = WorldMath.point(lat: p.lat, lon: p.lon, size: size)
                    let day = WorldMath.light(lat: p.lat, lon: p.lon, zone: p.zone, at: date) == "day"
                    Circle()
                        .fill(day ? Color(red: 1, green: 0.88, blue: 0.54) : Color(red: 0.62, green: 0.72, blue: 1))
                        .frame(width: 7, height: 7)
                        .position(pt)
                    if labels {
                        let leftSide = pt.x > size.width * 0.7
                        Text(String(p.name.prefix(12)) + " " + hmText(date, p.zone))
                            .font(.system(size: fontSize, weight: .bold))
                            .foregroundStyle(.white)
                            .shadow(color: .black, radius: 2)
                            .fixedSize()
                            .position(x: pt.x + (leftSide ? -42 : 42), y: pt.y)
                    }
                }
            }
        }
        .aspectRatio(2, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func hmText(_ d: Date, _ z: TimeZone) -> String {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = z
        let c = cal.dateComponents([.hour, .minute], from: d)
        return String(format: "%02d:%02d", c.hour ?? 0, c.minute ?? 0)
    }
}

/// Saat çakışması: satır başına şehir, sütun başına buradaki saat (bugün 0–23); 09–18 mesai yeşil,
/// sabah/akşam turuncu, gece gri; bütün şehirlerin mesaide olduğu saatler çerçeveli
struct OverlapGrid: View {
    let pins: [(name: String, zone: TimeZone)]
    let date: Date

    private func kind(_ hour: Int) -> Int { (9...17).contains(hour) ? 0 : ([7, 8, 18, 19, 20, 21].contains(hour) ? 1 : 2) }

    var body: some View {
        let start = Calendar.current.startOfDay(for: date)
        let rows: [[Int]] = pins.map { p in
            var cal = Calendar(identifier: .gregorian)
            cal.timeZone = p.zone
            return (0..<24).map { h in kind(cal.component(.hour, from: start.addingTimeInterval(Double(h) * 3600 + 1800))) }
        }
        let common: [Bool] = (0..<24).map { h in rows.allSatisfy { $0[h] == 0 } }
        let nowH = date.timeIntervalSince(start) / 3600
        GeometryReader { geo in
            let labelW: CGFloat = min(84, geo.size.width * 0.24)
            let cell = (geo.size.width - labelW) / 24
            let rowH = min(26, (geo.size.height - 14) / CGFloat(max(1, rows.count)))
            ZStack(alignment: .topLeading) {
                ForEach(Array(stride(from: 0, to: 24, by: 3)), id: \.self) { h in
                    Text("\(h)").font(.system(size: 9)).foregroundStyle(Theme.muted)
                        .position(x: labelW + CGFloat(h) * cell + 5, y: 6)
                }
                ForEach(Array(rows.enumerated()), id: \.offset) { r, row in
                    let y = 14 + CGFloat(r) * rowH
                    Text(pins[r].name).font(.system(size: 11)).lineLimit(1).frame(width: labelW - 4, alignment: .leading)
                        .position(x: (labelW - 4) / 2, y: y + rowH / 2)
                    ForEach(0..<24, id: \.self) { h in
                        let k = row[h]
                        let fill: Color = k == 0 ? Color(red: 0.18, green: 0.55, blue: 0.34) : (k == 1 ? Color(red: 0.79, green: 0.54, blue: 0.12) : Color.gray.opacity(0.25))
                        RoundedRectangle(cornerRadius: 2).fill(fill)
                            .frame(width: max(1, cell - 2), height: max(1, rowH - 6))
                            .position(x: labelW + CGFloat(h) * cell + cell / 2, y: y + rowH / 2)
                    }
                }
                let spans: [ClosedRange<Int>] = Self.runs(common).map { $0.0...$0.1 }
                ForEach(spans, id: \.lowerBound) { run in
                    let a = CGFloat(run.lowerBound), b = CGFloat(run.upperBound)
                    RoundedRectangle(cornerRadius: 4).stroke(Theme.text, lineWidth: 1.5)
                        .frame(width: (b - a) * cell, height: rowH * CGFloat(rows.count))
                        .position(x: labelW + (a + b) / 2 * cell, y: 14 + rowH * CGFloat(rows.count) / 2)
                }
                Rectangle().fill(Color(red: 0.89, green: 0.34, blue: 0.18))
                    .frame(width: 2, height: rowH * CGFloat(rows.count) + 4)
                    .position(x: labelW + CGFloat(nowH) * cell, y: 14 + rowH * CGFloat(rows.count) / 2)
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    static func runs(_ a: [Bool]) -> [(Int, Int)] {
        var out: [(Int, Int)] = []
        var i = 0
        while i < a.count {
            if !a[i] { i += 1; continue }
            let s = i
            while i < a.count && a[i] { i += 1 }
            out.append((s, i))
        }
        return out
    }
}
