import SwiftUI
import SaatCore
import UIKit

// MARK: Gökyüzü olayları (2. analiz #154)

struct SkyEvent: Decodable, Identifiable {
    let t: Int64
    let k: String
    let id_: String?
    let p: String?
    let a: String?
    let b: String?
    let zhr: Int?
    let illum: Int?
    let sep: Double?
    let deg: Double?
    let ev: Bool?

    enum CodingKeys: String, CodingKey { case t, k, id_ = "id", p, a, b, zhr, illum, sep, deg, ev }

    var id: String { "\(t)-\(k)" }
    var date: Date { Date(unixMilliseconds: t) }

    private func pn(_ x: String?) -> String { L("sky.planets.\(x ?? "")") }
    private func n1(_ v: Double?) -> String {
        let f = NumberFormatter()
        f.locale = AppLocale.numberLocale
        f.minimumFractionDigits = 1
        f.maximumFractionDigits = 1
        return f.string(from: NSNumber(value: v ?? 0)) ?? ""
    }

    var title: String {
        switch k {
        case "shower": return T("sky.evShower", L("sky.showers.\(id_ ?? "")"))
        case "opp": return T("sky.evOpp", pn(p))
        case "elong": return T((ev ?? false) ? "sky.evElongE" : "sky.evElongW", pn(p), n1(deg))
        case "conj": return T("sky.evConj", pn(a), pn(b), n1(sep))
        default: return T("sky.evMoon", pn(p), n1(sep))
        }
    }

    var detail: String? {
        guard k == "shower" else { return nil }
        let rate: String = zhr.map { T("sky.showerRate", $0) } ?? L("sky.showerVar")
        return rate + " · " + T("sky.moonLight", Fmt.percent(illum ?? 0))
    }

    static let all: [SkyEvent] = {
        guard let url = Bundle.main.url(forResource: "sky-events", withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([SkyEvent].self, from: data)) ?? []
    }()

    static func upcoming(_ now: Date = Date()) -> [SkyEvent] {
        let from = Int64(now.timeIntervalSince1970 * 1000) - 12 * 3_600_000
        return all.filter { $0.t > from }
    }
}

// MARK: Takımyıldızlar (d3-celestial, BSD-3)

struct Constellation {
    let id: String
    let rank: Int
    let lines: [[[Double]]]
    let at: [Double]?
    let names: [String: String]

    var name: String { names[AppLocale.code] ?? names["en"] ?? id }

    static let all: [Constellation] = {
        guard let url = Bundle.main.url(forResource: "constellations", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        var out: [Constellation] = []
        for (id, v) in obj {
            guard let d = v as? [String: Any] else { continue }
            let rank = d["rank"] as? Int ?? 3
            let lines = d["lines"] as? [[[Double]]] ?? []
            let at = d["at"] as? [Double]
            let n = d["n"] as? [String: String] ?? [:]
            out.append(Constellation(id: id, rank: rank, lines: lines, at: at, names: n))
        }
        return out
    }()
}

// MARK: Ortak gök hesapları

enum SkyCalc {
    static func ms(_ d: Date) -> Double { d.timeIntervalSince1970 * 1000 }
    static func sunAA(_ ms: Double, _ p: AppPlace) -> (alt: Double, az: Double) {
        let s = Sky.sun(ms: ms)
        return Sky.altAz(ms: ms, ra: s.ra, dec: s.dec, lat: p.lat, lon: p.lon)
    }
    static func moonAA(_ ms: Double, _ p: AppPlace) -> (alt: Double, az: Double) {
        let m = Nature.moonPosition(atMilliseconds: ms)
        let a = Sky.altAz(ms: ms, ra: m.rightAscension, dec: m.declination, lat: p.lat, lon: p.lon)
        return (a.alt - m.parallax * cos(a.alt * Double.pi / 180), a.az)
    }
    static func planetAA(_ name: String, _ ms: Double, _ p: AppPlace) -> (alt: Double, az: Double) {
        let x = Sky.planet(name, ms: ms)
        return Sky.altAz(ms: ms, ra: x.ra, dec: x.dec, lat: p.lat, lon: p.lon)
    }
    /// Güneş belirli yüksekliğin altındaki ilk aralık (10 dk adım)
    static func darkWindow(_ t0: Double, _ p: AppPlace, _ lim: Double) -> (Double, Double)? {
        let step = 600_000.0
        func below(_ t: Double) -> Bool { sunAA(t, p).alt < lim }
        var a: Double? = nil
        if below(t0) {
            for i in 1...144 { let t = t0 - Double(i) * step; if !below(t) { a = t + step; break } }
        } else {
            for i in 1...144 { let t = t0 + Double(i) * step; if below(t) { a = t; break } }
        }
        guard let start = a else { return nil }
        for i in 1...144 { let t = start + Double(i) * step; if !below(t) { return (start, t) } }
        return (start, start + 86_400_000)
    }
    struct Visibility { let first: Double?; let last: Double?; let bestT: Double; let bestAlt: Double; let firstAz: Double }
    static func visibility(_ name: String, _ night: (Double, Double)?, _ p: AppPlace) -> Visibility {
        var first: Double? = nil, last: Double? = nil, bt = 0.0, ba = -90.0, faz = 0.0
        if let n = night {
            var t = n.0
            while t <= n.1 {
                if sunAA(t, p).alt <= -6 {
                    let a = planetAA(name, t, p)
                    if a.alt >= 5 { if first == nil { first = t; faz = a.az }; last = t }
                    if a.alt > ba { ba = a.alt; bt = t }
                }
                t += 600_000
            }
        }
        return Visibility(first: first, last: last, bestT: bt, bestAlt: ba, firstAz: faz)
    }
    static func dirName(_ az: Double) -> String { L("weather.compass.\(Int((az / 45).rounded()) % 8)") }
    static let planetColors: [String: Color] = [
        "mercury": Color(red: 0.73, green: 0.65, blue: 0.55), "venus": Color(red: 0.96, green: 0.89, blue: 0.63),
        "mars": Color(red: 0.91, green: 0.44, blue: 0.30), "jupiter": Color(red: 0.91, green: 0.77, blue: 0.56),
        "saturn": Color(red: 0.85, green: 0.76, blue: 0.48),
    ]
}

// MARK: Gece gökyüzü (2. analiz #155–#157)

/// Gökyüzü çizimi (yıldızlar, takımyıldızlar, gezegenler, dokulu Ay), saat kaydırıcı ve oynatma, kırmızı ışık;
/// bu gece özeti; gezegen tablosu; gökyüzü olayları. Sitedeki /gece-gokyuzu/ sayfasının karşılığı.
struct SkyView: View {
    @State private var base: Double = (SkyCalc.ms(Date()) / 900_000).rounded(.down) * 900_000
    @State private var slot: Double = 0
    @State private var playing = false
    @State private var red = false
    @State private var showCon = true
    @State private var shown = 10
    private let timer = Timer.publish(every: 0.16, on: .main, in: .common).autoconnect()

    var body: some View {
        let place = PlaceModel.shared.place
        let night = SkyCalc.darkWindow(base, place, -0.833)
        let ms = base + slot * 900_000
        List {
            Section {
                SkyChart(ms: ms, place: place, showCon: showCon)
                    .aspectRatio(1, contentMode: .fit)
                    .colorMultiply(red ? Color(red: 0.72, green: 0.13, blue: 0.09) : .white)
                Text(timeLabel(ms, place)).font(.subheadline.weight(.semibold)).frame(maxWidth: .infinity)
                Slider(value: $slot, in: 0...96, step: 1)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Button(L(playing ? "sky.pause" : "sky.play")) { playing.toggle() }.buttonStyle(.bordered)
                        Button(L("sky.now")) {
                            playing = false
                            base = (SkyCalc.ms(Date()) / 900_000).rounded(.down) * 900_000
                            slot = 0
                        }.buttonStyle(.bordered)
                        Toggle(L("sky.constellations"), isOn: $showCon).toggleStyle(.button)
                        Toggle(L("sky.red"), isOn: $red).toggleStyle(.button)
                    }
                }
                if red { Text(L("sky.redHint")).font(.caption).foregroundStyle(.red) }
            } header: {
                PlacePicker()
            }
            TonightSection(place: place, night: night, base: base)
            Section(L("sky.tableHeading")) {
                ForEach(Sky.planets, id: \.self) { name in
                    PlanetRow(name: name, place: place, night: night)
                }
            }
            Section {
                Text(L("sky.eventsLead")).font(.footnote).foregroundStyle(Theme.muted)
                let evs = SkyEvent.upcoming()
                ForEach(Array(evs.prefix(shown))) { e in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(Fmt.shortDateTime(e.date, tz: place.zone)).font(.caption).foregroundStyle(Theme.muted)
                        Text(e.title).font(.subheadline.weight(.semibold))
                        if let d = e.detail { Text(d).font(.caption).foregroundStyle(Theme.muted) }
                    }
                }
                if evs.count > shown { Button(L("sky.evMore")) { shown += 10 } }
            } header: {
                Text(L("sky.eventsHeading"))
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("sky.eventsNote"))
                    Text(L("sky.note"))
                    Text(L("sky.conSource"))
                }
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("sky.h1"))
        .onReceive(timer) { _ in
            guard playing else { return }
            if slot >= 96 { playing = false } else { slot += 1 }
        }
        .onAppear {
            let now = SkyCalc.ms(Date())
            if SkyCalc.sunAA(now, place).alt > -6, let n = night {
                slot = max(0, min(96, ((n.0 + 3_600_000 - base) / 900_000).rounded()))
            }
        }
    }

    private func timeLabel(_ ms: Double, _ place: AppPlace) -> String {
        let d = Date(unixMilliseconds: Int64(ms))
        return place.hm(d) + " · " + Fmt.longDate(d, tz: place.zone)
    }
}

private struct PlanetRow: View {
    let name: String
    let place: AppPlace
    let night: (Double, Double)?

    var body: some View {
        let now = SkyCalc.ms(Date())
        let p = Sky.planet(name, ms: now)
        let v = SkyCalc.visibility(name, night, place)
        HStack(spacing: 10) {
            Circle().fill(SkyCalc.planetColors[name] ?? .gray).frame(width: 12, height: 12)
            VStack(alignment: .leading, spacing: 2) {
                Text(L("sky.planets.\(name)")).font(.subheadline.weight(.semibold))
                Text(whenText(p, v)).font(.caption).foregroundStyle(Theme.muted)
            }
            Spacer()
            Text(magText(p.mag)).font(.caption).monospacedDigit().foregroundStyle(Theme.muted)
        }
    }

    private func whenText(_ p: Sky.Planet, _ v: SkyCalc.Visibility) -> String {
        guard let first = v.first else { return L(p.elong < 15 ? "sky.tooClose" : "sky.below") }
        let last = max(v.last ?? first, first + 600_000)
        let w: String = T("sky.window", place.hm(Date(unixMilliseconds: Int64(first))), place.hm(Date(unixMilliseconds: Int64(last))))
        let hi: String = T("sky.altFmt", Int(v.bestAlt.rounded()), place.hm(Date(unixMilliseconds: Int64(v.bestT))))
        return w + " · " + SkyCalc.dirName(v.firstAz) + " · " + hi
    }

    private func magText(_ m: Double) -> String {
        let f = NumberFormatter()
        f.locale = AppLocale.numberLocale
        f.minimumFractionDigits = 1
        f.maximumFractionDigits = 1
        let s = f.string(from: NSNumber(value: abs(m))) ?? ""
        return (m < 0 ? "−" : "") + s
    }
}

/// Ortası başucu, kenar ufuk; kuzey yukarıda, doğu solda
struct SkyChart: View {
    let ms: Double
    let place: AppPlace
    let showCon: Bool

    var body: some View {
        let m = Nature.moon(atMilliseconds: Int64(ms))
        let moonImg = MoonTexture.image(frac: m.fraction, south: place.lat < 0, size: 48)
        Canvas { ctx, size in
            let R = min(size.width, size.height) / 2 * 0.9
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            func proj(_ alt: Double, _ az: Double) -> CGPoint {
                let r = CGFloat((90 - alt) / 90) * R
                let z = az * Double.pi / 180
                return CGPoint(x: c.x - r * CGFloat(sin(z)), y: c.y - r * CGFloat(cos(z)))
            }
            let sa = SkyCalc.sunAA(ms, place)
            let bg: Color = sa.alt > 0 ? Color(red: 0.50, green: 0.69, blue: 0.87)
                : sa.alt > -6 ? Color(red: 0.23, green: 0.31, blue: 0.52)
                : sa.alt > -12 ? Color(red: 0.11, green: 0.16, blue: 0.33) : Color(red: 0.05, green: 0.09, blue: 0.19)
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - R, y: c.y - R, width: 2 * R, height: 2 * R)), with: .color(bg))
            for alt in [30.0, 60.0] {
                let r = CGFloat((90 - alt) / 90) * R
                ctx.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)), with: .color(.white.opacity(0.18)),
                           style: StrokeStyle(lineWidth: 1, dash: [3, 4]))
            }
            for (i, az) in [0.0, 90.0, 180.0, 270.0].enumerated() {
                let pt = proj(-6, az)
                ctx.draw(Text(L("weather.compass.\(i * 2)")).font(.caption.weight(.bold)).foregroundColor(Theme.muted), at: pt)
            }
            if sa.alt < -3 && showCon {
                for k in Constellation.all {
                    for ln in k.lines {
                        var path = Path()
                        var pen = false
                        for pt in ln where pt.count == 2 {
                            let a = Sky.altAz(ms: ms, ra: pt[0], dec: pt[1], lat: place.lat, lon: place.lon)
                            if a.alt < -2 { pen = false; continue }
                            let o = proj(a.alt, a.az)
                            if pen { path.addLine(to: o) } else { path.move(to: o) }
                            pen = true
                        }
                        ctx.stroke(path, with: .color(Color(red: 0.63, green: 0.75, blue: 1).opacity(0.38)), lineWidth: 1)
                    }
                    if k.rank <= 2, let at = k.at, at.count == 2 {
                        let a = Sky.altAz(ms: ms, ra: at[0], dec: at[1], lat: place.lat, lon: place.lon)
                        if a.alt > 12 {
                            ctx.draw(Text(k.name).font(.system(size: 8)).foregroundColor(Color(red: 0.67, green: 0.78, blue: 1).opacity(0.7)), at: proj(a.alt, a.az))
                        }
                    }
                }
            }
            if sa.alt < 0 {
                for s in Sky.stars {
                    let a = Sky.altAz(ms: ms, ra: s.ra, dec: s.dec, lat: place.lat, lon: place.lon)
                    if a.alt <= 0 { continue }
                    let o = proj(a.alt, a.az)
                    let r = CGFloat(max(0.8, 3.2 - s.mag * 0.9))
                    ctx.fill(Path(ellipseIn: CGRect(x: o.x - r, y: o.y - r, width: 2 * r, height: 2 * r)), with: .color(.white.opacity(0.9)))
                    if s.mag < 0.9 {
                        ctx.draw(Text(s.name).font(.system(size: 9)).foregroundColor(.white.opacity(0.75)), at: CGPoint(x: o.x + 4, y: o.y + 3), anchor: .leading)
                    }
                }
            }
            let ma = SkyCalc.moonAA(ms, place)
            if ma.alt > -0.5 {
                let o = proj(ma.alt, ma.az)
                if let img = moonImg {
                    ctx.draw(Image(decorative: img, scale: 3), in: CGRect(x: o.x - 8, y: o.y - 8, width: 16, height: 16))
                } else {
                    ctx.fill(Path(ellipseIn: CGRect(x: o.x - 7, y: o.y - 7, width: 14, height: 14)), with: .color(Color(white: 0.92)))
                }
                ctx.draw(Text(L("sky.moon")).font(.caption2.weight(.semibold)).foregroundColor(.white), at: CGPoint(x: o.x + 12, y: o.y), anchor: .leading)
            }
            if sa.alt > -0.833 {
                let o = proj(sa.alt, sa.az)
                ctx.fill(Path(ellipseIn: CGRect(x: o.x - 8, y: o.y - 8, width: 16, height: 16)), with: .color(Color(red: 1, green: 0.83, blue: 0.3)))
            }
            for name in Sky.planets {
                let p = Sky.planet(name, ms: ms)
                let a = Sky.altAz(ms: ms, ra: p.ra, dec: p.dec, lat: place.lat, lon: place.lon)
                if a.alt <= 0 { continue }
                let o = proj(a.alt, a.az)
                let r = CGFloat(max(2.5, 5 - p.mag * 0.6))
                ctx.fill(Path(ellipseIn: CGRect(x: o.x - r, y: o.y - r, width: 2 * r, height: 2 * r)), with: .color(SkyCalc.planetColors[name] ?? .white))
                ctx.draw(Text(L("sky.planets.\(name)")).font(.caption2.weight(.semibold)).foregroundColor(.white), at: CGPoint(x: o.x + 7, y: o.y - 6), anchor: .leading)
            }
        }
    }
}

/// "Bu gece" özeti: tam karanlık, Ay'ın karanlıktaki etkisi, görülebilen gezegenler, öneriler ve yükseklik grafiği
private struct TonightSection: View {
    let place: AppPlace
    let night: (Double, Double)?
    let base: Double

    var body: some View {
        let ref: Double = night.map { $0.0 + 3_600_000 } ?? base
        let dark = SkyCalc.darkWindow(ref, place, -18)
        let m = Nature.moon(atMilliseconds: Int64(dark?.0 ?? ref))
        let up: [Double] = moonUp(dark)
        let impact: Double = dark.map { d in Double(up.count) * 600_000 / max(1, d.1 - d.0) * m.illumination } ?? m.illumination
        let vis: [(String, SkyCalc.Visibility)] = Sky.planets.map { ($0, SkyCalc.visibility($0, night, place)) }.filter { $0.1.first != nil }
        let score: String = scoreKey(dark, impact)
        let il: String = Fmt.percent(Int((m.illumination * 100).rounded()))
        Section(L("sky.tonightHeading")) {
            Text(L("sky.score.\(score)")).font(.headline).foregroundStyle(score == "good" ? Theme.ok : score == "poor" ? Theme.err : Theme.ink)
            LabeledContent(L("sky.darkLabel"), value: darkText(dark))
            LabeledContent(L("sky.moon"), value: moonText(up, il))
            LabeledContent(L("sky.planetsLabel"), value: vis.isEmpty ? L("sky.planetsNone") : vis.map { L("sky.planets.\($0.0)") }.joined(separator: ", "))
            if m.illumination > 0.5 && !up.isEmpty {
                Text("• " + L("sky.tipMoon")).font(.footnote)
            } else if dark != nil && up.isEmpty {
                Text("• " + L("sky.tipDark")).font(.footnote)
            }
            let best: [(String, SkyCalc.Visibility)] = Array(vis.sorted { $0.1.bestAlt > $1.1.bestAlt }.prefix(3))
            ForEach(best.indices, id: \.self) { i in
                Text("• " + tipBest(best[i].0, best[i].1)).font(.footnote)
            }
            SkyGraph(place: place, night: night, dark: dark).frame(height: 160)
            Text(L("sky.graphCaption")).font(.caption).foregroundStyle(Theme.muted)
        }
    }

    private func moonUp(_ dark: (Double, Double)?) -> [Double] {
        guard let d = dark else { return [] }
        var out: [Double] = []
        var t = d.0
        while t <= d.1 { if SkyCalc.moonAA(t, place).alt > 0 { out.append(t) }; t += 600_000 }
        return out
    }
    private func scoreKey(_ dark: (Double, Double)?, _ impact: Double) -> String {
        guard let d = dark, impact <= 0.6 else { return "poor" }
        return impact < 0.25 && d.1 - d.0 >= 3 * 3_600_000 ? "good" : "fair"
    }
    private func darkText(_ dark: (Double, Double)?) -> String {
        guard let d = dark else { return L("sky.darkNone") }
        return T("sky.window", place.hm(Date(unixMilliseconds: Int64(d.0))), place.hm(Date(unixMilliseconds: Int64(d.1))))
    }
    private func moonText(_ up: [Double], _ il: String) -> String {
        guard let f = up.first, let l = up.last else { return T("sky.moonDownDark", il) }
        return T("sky.moonUpDark", il, place.hm(Date(unixMilliseconds: Int64(f))), place.hm(Date(unixMilliseconds: Int64(l))))
    }
    private func tipBest(_ name: String, _ v: SkyCalc.Visibility) -> String {
        T("sky.tipBest", L("sky.planets.\(name)"), place.hm(Date(unixMilliseconds: Int64(v.bestT))), Int(v.bestAlt.rounded()))
    }
}

private struct SkyGraph: View {
    let place: AppPlace
    let night: (Double, Double)?
    let dark: (Double, Double)?

    var body: some View {
        let t0: Double = (night?.0 ?? SkyCalc.ms(Date())) - 3_600_000
        let t1: Double = (night?.1 ?? (t0 + 12 * 3_600_000)) + 3_600_000
        Canvas { ctx, size in
            func x(_ t: Double) -> CGFloat { CGFloat((t - t0) / (t1 - t0)) * size.width }
            func y(_ a: Double) -> CGFloat { CGFloat(1 - max(0, a) / 90) * size.height }
            if let d = dark {
                let a = x(max(t0, d.0)), b = x(min(t1, d.1))
                ctx.fill(Path(CGRect(x: a, y: 0, width: max(0, b - a), height: size.height)), with: .color(Color(red: 0.13, green: 0.25, blue: 0.63).opacity(0.18)))
            }
            for a in [30.0, 60.0] {
                var p = Path(); p.move(to: CGPoint(x: 0, y: y(a))); p.addLine(to: CGPoint(x: size.width, y: y(a)))
                ctx.stroke(p, with: .color(Theme.muted.opacity(0.4)), style: StrokeStyle(lineWidth: 1, dash: [4, 5]))
            }
            var names = Sky.planets
            names.append("moon")
            for n in names {
                var path = Path()
                var pen = false
                var t = t0
                while t <= t1 {
                    let a: Double = n == "moon" ? SkyCalc.moonAA(t, place).alt : SkyCalc.planetAA(n, t, place).alt
                    if a <= 0 { pen = false } else {
                        let pt = CGPoint(x: x(t), y: y(a))
                        if pen { path.addLine(to: pt) } else { path.move(to: pt) }
                        pen = true
                    }
                    t += 900_000
                }
                let col: Color = SkyCalc.planetColors[n] ?? Color(white: 0.6)
                ctx.stroke(path, with: .color(col), lineWidth: 2)
            }
            var h = ((t0 / 3_600_000).rounded(.down) + 1) * 3_600_000
            while h < t1 {
                ctx.draw(Text(place.hm(Date(unixMilliseconds: Int64(h)))).font(.system(size: 9)).foregroundColor(Theme.muted), at: CGPoint(x: x(h), y: size.height - 6))
                h += 3 * 3_600_000
            }
        }
    }
}

// MARK: Güneş yolu (2. analiz #158)

struct SunPathView: View {
    @StateObject private var compass = Compass()

    var body: some View {
        let place = PlaceModel.shared.place
        let now = SkyCalc.ms(Date())
        let today = CalendarDay(Date(), timeZone: place.zone)
        let paths: [[(Double, Double, Double)]] = [dayPath(today, place), dayPath(CalendarDay(today.year, 6, 21), place), dayPath(CalendarDay(today.year, 12, 21), place)]
        let nowAA = SkyCalc.sunAA(now, place)
        let noon = paths[0].max { $0.1 < $1.1 }
        List {
            Section {
                SunPathDial(paths: paths, now: nowAA, heading: compass.heading ?? 0)
                    .aspectRatio(1, contentMode: .fit)
                Text(L(compass.heading != nil ? "sunpath.compass" : "qibla.noCompass")).font(.caption).foregroundStyle(Theme.muted)
            } header: {
                PlacePicker()
            }
            Section {
                Text(nowText(nowAA)).font(.subheadline.weight(.semibold))
                if let r = crossing(paths[0], rise: true) { LabeledContent(L("sunpath.rise"), value: SkyCalc.dirName(r) + " · \(Int(r.rounded()))°") }
                if let s = crossing(paths[0], rise: false) { LabeledContent(L("sunpath.set"), value: SkyCalc.dirName(s) + " · \(Int(s.rounded()))°") }
                if let n = noon {
                    LabeledContent(L("sunpath.noon"), value: "\(Int(n.1.rounded()))° · " + place.hm(Date(unixMilliseconds: Int64(n.0))))
                }
                if nowAA.alt > 0.5 {
                    Text(T("sunpath.shadow", shadowText(nowAA.alt)))
                }
            } footer: {
                Text(L("sunpath.legend"))
            }
        }
        .themedBackground()
        .navigationTitle(L("sunpath.title"))
        .onAppear { compass.start() }
        .onDisappear { compass.stop() }
    }

    /// Günün Güneş yolu: (an, yükseklik, azimut), 5 dk adım
    private func dayPath(_ d: CalendarDay, _ place: AppPlace) -> [(Double, Double, Double)] {
        let start = SkyCalc.ms(place.startOfDay(d))
        return (0...288).map { i in
            let t = start + Double(i) * 300_000
            let a = SkyCalc.sunAA(t, place)
            return (t, a.alt, a.az)
        }
    }
    private func crossing(_ list: [(Double, Double, Double)], rise: Bool) -> Double? {
        for i in 1..<list.count {
            let a = list[i - 1].1 + 0.833, b = list[i].1 + 0.833
            if rise && a < 0 && b >= 0 { return list[i].2 }
            if !rise && a >= 0 && b < 0 { return list[i - 1].2 }
        }
        return nil
    }
    private func nowText(_ a: (alt: Double, az: Double)) -> String {
        guard a.alt > 0 else { return L("sunpath.below") }
        return T("sunpath.now", Int(a.alt.rounded()), SkyCalc.dirName(a.az), Int(a.az.rounded()))
    }
    private func shadowText(_ alt: Double) -> String {
        let f = NumberFormatter()
        f.locale = AppLocale.numberLocale
        f.minimumFractionDigits = 1
        f.maximumFractionDigits = 1
        return f.string(from: NSNumber(value: min(99, 1 / tan(alt * Double.pi / 180)))) ?? ""
    }
}

private struct SunPathDial: View {
    let paths: [[(Double, Double, Double)]]
    let now: (alt: Double, az: Double)
    let heading: Double

    var body: some View {
        let cols: [Color] = [Color(red: 0.95, green: 0.72, blue: 0.02), Color(red: 0.91, green: 0.44, blue: 0.30), Color(red: 0.29, green: 0.48, blue: 0.82)]
        Canvas { ctx, size in
            let R = min(size.width, size.height) / 2 * 0.86
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            func proj(_ alt: Double, _ az: Double) -> CGPoint {
                let r = CGFloat((90 - alt) / 90) * R
                let z = (az - heading) * Double.pi / 180
                return CGPoint(x: c.x + r * CGFloat(sin(z)), y: c.y - r * CGFloat(cos(z)))
            }
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - R, y: c.y - R, width: 2 * R, height: 2 * R)), with: .color(Theme.muted.opacity(0.08)))
            ctx.stroke(Path(ellipseIn: CGRect(x: c.x - R, y: c.y - R, width: 2 * R, height: 2 * R)), with: .color(Theme.muted.opacity(0.5)), lineWidth: 2)
            for alt in [30.0, 60.0] {
                let r = CGFloat((90 - alt) / 90) * R
                ctx.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)), with: .color(Theme.muted.opacity(0.3)), style: StrokeStyle(lineWidth: 1, dash: [4, 5]))
            }
            for i in stride(from: paths.count - 1, through: 0, by: -1) {
                var path = Path()
                var pen = false
                for p in paths[i] {
                    if p.1 < 0 { pen = false; continue }
                    let o = proj(p.1, p.2)
                    if pen { path.addLine(to: o) } else { path.move(to: o) }
                    pen = true
                }
                ctx.stroke(path, with: .color(cols[i]), lineWidth: i == 0 ? 3.5 : 2)
            }
            for (i, az) in [0.0, 90.0, 180.0, 270.0].enumerated() {
                ctx.draw(Text(L("weather.compass.\(i * 2)")).font(.caption.weight(.bold)).foregroundColor(Theme.muted), at: proj(-8, az))
            }
            if now.alt > 0 {
                let o = proj(now.alt, now.az)
                ctx.fill(Path(ellipseIn: CGRect(x: o.x - 9, y: o.y - 9, width: 18, height: 18)), with: .color(Color(red: 1, green: 0.76, blue: 0.1)))
            }
        }
    }
}

// MARK: Tutulma ayrıntısı (2. analiz #159)

struct EclipseDetail: View {
    let e: Eclipse
    let place: AppPlace

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if e.kind == "lunar" {
                lunar
            } else {
                solar
            }
            Text(L("nature.eclApprox")).font(.caption2).foregroundStyle(Theme.muted)
        }
    }

    @ViewBuilder private var lunar: some View {
        let c = Sky.lunarContacts(ut: Double(e.ut))
        LunarDiagram(c: c).frame(height: 170)
        let rows: [(String, Int64?)] = [("p1", c.p1), ("u1", c.u1), ("u2", c.u2), ("max", c.max), ("u3", c.u3), ("u4", c.u4), ("p4", c.p4)]
        ForEach(rows.indices, id: \.self) { i in
            let row = rows[i]
            if let t = row.1 {
                let up = Nature.moonAltitude(Double(t), lat: place.lat, lon: place.lon) > 0
                HStack {
                    Text(L("nature.eclContacts.\(row.0)"))
                    Spacer()
                    Text(place.hm(Date(unixMilliseconds: (t + 30_000) / 60_000 * 60_000))).monospacedDigit().fontWeight(.semibold)
                }
                .font(.footnote)
                .opacity(up ? 1 : 0.45)
            }
        }
        if c.umag > 0 { Text(T("nature.eclMag", String(format: "%.2f", c.umag))).font(.caption).foregroundStyle(Theme.muted) }
    }

    @ViewBuilder private var solar: some View {
        let r = Sky.solarLocal(ut: Double(e.ut), lat: place.lat, lon: place.lon)
        let visible = r.map { $0.altMax >= 0 } ?? false
        Text(solarText(r)).font(.footnote.weight(.semibold)).foregroundStyle(visible ? Theme.accent : Theme.muted)
        if let r, visible {
            SolarDisk(d: Sky.solarDisks(ms: Double(r.max), lat: place.lat, lon: place.lon)).frame(width: 110, height: 110)
        }
        EclipseMapView(e: e, place: place)
    }

    private func hm(_ t: Int64?) -> String {
        guard let t else { return "–" }
        return place.hm(Date(unixMilliseconds: (t + 30_000) / 60_000 * 60_000))
    }
    private func solarText(_ r: Sky.SolarLocal?) -> String {
        guard let r else { return T("nature.eclLocalNone", place.name) }
        if r.altMax < 0 { return T("nature.eclLocalLow", place.name) }
        if r.central == "total" { return T("nature.eclLocalTotal", place.name, hm(r.c1), hm(r.c4), hm(r.max)) }
        if r.central == "annular" { return T("nature.eclLocalAnnular", place.name, hm(r.c1), hm(r.c4), hm(r.max)) }
        let ob: String = Fmt.percent(Int((r.obsc * 100).rounded()))
        return T("nature.eclLocal", place.name, ob, hm(r.c1), hm(r.c4), hm(r.max))
    }
}

private struct LunarDiagram: View {
    let c: Sky.Lunar

    var body: some View {
        Canvas { ctx, size in
            let p1: Double? = c.p1.map { Double($0) }
            let p4: Double? = c.p4.map { Double($0) }
            let path = c.path.filter { pt in (p1 == nil || pt.t >= p1! - 600_000) && (p4 == nil || pt.t <= p4! + 600_000) }
            var ext = c.rP
            for pt in path { ext = max(ext, max(abs(pt.x), abs(pt.y)) + c.rM) }
            let sc = CGFloat(Double(min(size.width, size.height)) / 2 * 0.95 / ext)
            let ctr = CGPoint(x: size.width / 2, y: size.height / 2)
            func circle(_ o: CGPoint, _ r: Double) -> Path { let rr = CGFloat(r) * sc; return Path(ellipseIn: CGRect(x: o.x - rr, y: o.y - rr, width: 2 * rr, height: 2 * rr)) }
            ctx.fill(circle(ctr, c.rP), with: .color(Color.gray.opacity(0.18)))
            ctx.fill(circle(ctr, c.rU), with: .color(Color(red: 0.59, green: 0.16, blue: 0.12).opacity(0.35)))
            var track = Path()
            for (i, pt) in path.enumerated() {
                let o = CGPoint(x: ctr.x - CGFloat(pt.x) * sc, y: ctr.y - CGFloat(pt.y) * sc)
                if i == 0 { track.move(to: o) } else { track.addLine(to: o) }
            }
            ctx.stroke(track, with: .color(.gray), style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
            let marks: [(String, Int64?)] = [("p1", c.p1), ("u1", c.u1), ("max", c.max), ("u4", c.u4), ("p4", c.p4)]
            for m in marks {
                guard let t = m.1 else { continue }
                guard let q = path.min(by: { abs($0.t - Double(t)) < abs($1.t - Double(t)) }) else { continue }
                let o = CGPoint(x: ctr.x - CGFloat(q.x) * sc, y: ctr.y - CGFloat(q.y) * sc)
                let col: Color = m.0 == "max" ? Color(red: 0.75, green: 0.27, blue: 0.16).opacity(0.75) : Color(white: 0.92).opacity(0.55)
                ctx.fill(circle(o, c.rM), with: .color(col))
                ctx.stroke(circle(o, c.rM), with: .color(.gray), lineWidth: 1)
            }
        }
    }
}

private struct SolarDisk: View {
    let d: Sky.Disks

    var body: some View {
        Canvas { ctx, size in
            let sc = CGFloat(Double(min(size.width, size.height)) / 2 / (d.rs * 1.6))
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            let rs = CGFloat(d.rs) * sc, rm = CGFloat(d.rm) * sc, off = CGFloat(d.d) * sc * 0.7
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - rs, y: c.y - rs, width: 2 * rs, height: 2 * rs)), with: .color(Color(red: 1, green: 0.76, blue: 0.1)))
            ctx.fill(Path(ellipseIn: CGRect(x: c.x + off - rm, y: c.y - off - rm, width: 2 * rm, height: 2 * rm)), with: .color(Color(red: 0.13, green: 0.15, blue: 0.19)))
        }
    }
}

/// Tutulma haritası (sitedeki eclipse-map.js): 3° ızgarada yerel en büyük örtülme; kara sınırları yerine enlem/boylam ızgarası
private struct EclipseMapView: View {
    let e: Eclipse
    let place: AppPlace
    @State private var cells: [(Double, Double, Double)] = []

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(L("nature.eclMapHeading")).font(.subheadline.weight(.semibold))
            Canvas { ctx, size in
                ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(Color(red: 0.85, green: 0.90, blue: 0.95)))
                if let img = Self.earth { ctx.draw(Image(uiImage: img), in: CGRect(origin: .zero, size: size)) }
                let cw = size.width / 120, ch = size.height / 60
                for q in cells {
                    let m = q.2
                    let col: Color = m > 0.75 ? Color(red: 0.76, green: 0.25, blue: 0.05) : m > 0.5 ? Color(red: 0.92, green: 0.48, blue: 0.14)
                        : m > 0.25 ? Color(red: 0.96, green: 0.65, blue: 0.35) : Color(red: 0.98, green: 0.81, blue: 0.61)
                    let x = CGFloat((q.1 + 180 - 1.5) / 360) * size.width
                    let y = CGFloat((90 - q.0 - 1.5) / 180) * size.height
                    ctx.fill(Path(CGRect(x: x, y: y, width: cw + 1, height: ch + 1)), with: .color(col.opacity(0.55)))
                }
                let px = CGFloat((place.lon + 180) / 360) * size.width, py = CGFloat((90 - place.lat) / 180) * size.height
                ctx.fill(Path(ellipseIn: CGRect(x: px - 4, y: py - 4, width: 8, height: 8)), with: .color(.black))
            }
            .aspectRatio(2, contentMode: .fit)
            Text(L("nature.eclMapSolar")).font(.caption2).foregroundStyle(Theme.muted)
        }
        .task(id: e.ut) {
            let ut = Double(e.ut)
            cells = await Task.detached(priority: .utility) { Self.compute(ut) }.value
        }
    }

    static let earth: UIImage? = {
        guard let url = Bundle.main.url(forResource: "earth", withExtension: "jpg"), let d = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: d)
    }()

    nonisolated static func compute(_ ut: Double) -> [(Double, Double, Double)] {
        var out: [(Double, Double, Double)] = []
        let steps: [Double] = (0...84).map { ut - 210 * 60_000 + Double($0) * 300_000 }
        var lat = -88.5
        while lat < 90 {
            var lon = -178.5
            while lon < 180 {
                var best = 0.0
                for t in steps {
                    let x = Sky.solarDisks(ms: t, lat: lat, lon: lon)
                    if x.alt > 0 && x.d < x.rs + x.rm { best = max(best, (x.rs + x.rm - x.d) / (2 * x.rs)) }
                }
                if best > 0 { out.append((lat, lon, best)) }
                lon += 3
            }
            lat += 3
        }
        return out
    }
}

// MARK: Çocuklar için Ay (2. analiz #160)

struct KidsMoonSection: View {
    @State private var day: Double = 7.4
    @State private var q = 0
    @State private var score = 0
    @State private var picked = -1
    private let answers = [1, 1, 1, 0, 0]

    var body: some View {
        let frac = day / 29.53
        let idx = Int(frac * 8 + 0.5) % 8
        Section(L("kidsmoon.title")) {
            Text(L("kidsmoon.text")).font(.footnote)
            HStack {
                VStack(spacing: 4) {
                    Text(L("kidsmoon.space")).font(.caption).foregroundStyle(Theme.muted)
                    OrbitDiagram(frac: frac).frame(width: 140, height: 140)
                }
                Spacer()
                VStack(spacing: 4) {
                    Text(L("kidsmoon.earth")).font(.caption).foregroundStyle(Theme.muted)
                    MoonImage(fraction: frac, size: 100)
                }
            }
            Slider(value: $day, in: 0...29.5)
            Text(T("kidsmoon.day", Int(day.rounded())) + " · " + L("nature.phases.\(moonPhaseKeys[idx])")).font(.subheadline.weight(.semibold))
        }
        Section(L("kidsmoon.quiz")) {
            if q >= answers.count {
                Text(T("kidsmoon.score", score, answers.count)).font(.headline).foregroundStyle(Theme.accent)
                Button(L("kidsmoon.again")) { q = 0; score = 0; picked = -1 }
            } else {
                Text("\(q + 1)/\(answers.count)  " + L("kidsmoon.q\(q + 1)")).font(.subheadline)
                let opts = L("kidsmoon.q\(q + 1)a").components(separatedBy: "|")
                ForEach(Array(opts.enumerated()), id: \.offset) { i, opt in
                    Button {
                        if picked != answers[q] {
                            if picked < 0 && i == answers[q] { score += 1 }
                            picked = i
                        }
                    } label: {
                        HStack {
                            Text(opt).foregroundStyle(Theme.ink)
                            Spacer()
                            if picked == i { Image(systemName: i == answers[q] ? "checkmark.circle.fill" : "xmark.circle").foregroundStyle(i == answers[q] ? Theme.ok : Theme.err) }
                        }
                    }
                }
                if picked >= 0 {
                    Text(L(picked == answers[q] ? "kidsmoon.correct" : "kidsmoon.wrong")).foregroundStyle(picked == answers[q] ? Theme.ok : Theme.err)
                    if picked == answers[q] { Button("→") { q += 1; picked = -1 } }
                }
            }
        }
    }
}

private struct OrbitDiagram: View {
    let frac: Double

    var body: some View {
        Canvas { ctx, size in
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            let R = min(size.width, size.height) / 2 * 0.72
            for i in 0..<4 {
                let y = size.height * (0.2 + CGFloat(i) * 0.2)
                var p = Path(); p.move(to: CGPoint(x: 0, y: y)); p.addLine(to: CGPoint(x: size.width * 0.12, y: y))
                ctx.stroke(p, with: .color(Color(red: 1, green: 0.76, blue: 0.1)), lineWidth: 2)
            }
            ctx.stroke(Path(ellipseIn: CGRect(x: c.x - R, y: c.y - R, width: 2 * R, height: 2 * R)), with: .color(.gray.opacity(0.4)), style: StrokeStyle(lineWidth: 1, dash: [4, 5]))
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - 9, y: c.y - 9, width: 18, height: 18)), with: .color(Color(red: 0.25, green: 0.5, blue: 0.82)))
            let ang = Double.pi + 2 * Double.pi * frac
            let mo = CGPoint(x: c.x + R * CGFloat(cos(ang)), y: c.y - R * CGFloat(sin(ang)))
            ctx.fill(Path(ellipseIn: CGRect(x: mo.x - 7, y: mo.y - 7, width: 14, height: 14)), with: .color(Color(white: 0.92)))
            var dark = Path()
            dark.addArc(center: mo, radius: 7, startAngle: .degrees(-90), endAngle: .degrees(90), clockwise: false)
            ctx.fill(dark, with: .color(Color(white: 0.2)))
        }
    }
}
