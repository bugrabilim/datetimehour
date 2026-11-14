import SwiftUI
import UIKit
import SaatCore

/*
 * Tebrik kartları (3 Kasım 2026, 2. analiz #74; 4 Kasım 2026'da sahibin isteğiyle genişledi; Android'deki Greetings.kt
 * ile aynı): Cuma, Ramazan, Ramazan ve Kurban bayramı, kandil ve hicri yılbaşı için telefonda çizilen kare görsel.
 * - Söz her açılışta kendiliğinden değişir: vesileye özgü dilek (6) + dua (10) + kapanış (8), vesile başına 480 bileşim,
 *   18 dilde (`greet.m.*`, `greet.bless.*`, `greet.close.*`); "Başka söz" yenisini seçer; kendi mesajını yazan da olur.
 * - Desen vesileye göre seçilir (Ramazan'da fenerler, hilal; bayramlarda lale, ışıklar; Cuma'da cami…), renk ayrıca.
 * - Paylaşımda görselin yanında uygulamanın bağlantısı da gider (App Store).
 * Dış görsel yok; görsel yalnız kullanıcı paylaşınca sistemin paylaşım penceresiyle gider.
 */
enum Greetings {
    static let religious = ["cuma", "ramadan", "rb", "kb", "kandil", "hijri"]
    /// Nötr vesileler (10 Kasım 2026, Müslüman olmayanlar araştırması): "Kutlama kartları" yalnız bunları gösterir
    static let neutral = ["birthday", "newyear", "anniversary", "graduation", "newjob"]
    static let occasions = religious + neutral
    /// Nötr kartlarda dua içeren kapanış (2: "dualarımla") kullanılmaz
    private static let neutralCloses = [1, 3, 4, 5, 6, 7, 8]
    static let styles = ["night", "emerald", "gold"]
    static let arabic = ["cuma": "جمعة مباركة", "ramadan": "رمضان كريم", "rb": "عيد مبارك", "kb": "عيد مبارك",
                         "kandil": "ليلة مباركة", "hijri": "عام هجري سعيد"]
    /// Vesileye göre desenler (ilk desen varsayılan)
    static let patterns: [String: [String]] = [
        "cuma": ["mosque", "geo", "arch"],
        "ramadan": ["lanterns", "crescent", "geo"],
        "rb": ["tulips", "lights", "geo"],
        "kb": ["kaaba", "tulips", "geo"],
        "kandil": ["lamp", "stars", "arch"],
        "hijri": ["crescent", "stars", "geo"],
        "birthday": ["lights", "stars", "tulips"],
        "newyear": ["stars", "lights", "geo"],
        "anniversary": ["tulips", "lights", "stars"],
        "graduation": ["stars", "geo", "lights"],
        "newjob": ["geo", "tulips", "stars"],
    ]

    /// Söz: vesileye özgü dilek + dua + kapanış; tohumdan seçilir (Android'deki Greetings.message ile aynı kural)
    static func message(_ occasion: String, seed: UInt64) -> String {
        if neutral.contains(occasion) {
            // Nötr: vesile sözü (3) + dilek (6, dua yok) + kapanış (7)
            let w = Int(seed % 3) + 1, b = Int((seed / 3) % 6) + 1, c = neutralCloses[Int((seed / 18) % UInt64(neutralCloses.count))]
            return [L("greet.m.\(occasion).\(w)"), L("greet.wish.\(b)"), L("greet.close.\(c)")].joined(separator: "\n")
        }
        let w = Int(seed % 6) + 1, b = Int((seed / 6) % 10) + 1, c = Int((seed / 60) % 8) + 1
        return [L("greet.m.\(occasion).\(w)"), L("greet.bless.\(b)"), L("greet.close.\(c)")].joined(separator: "\n")
    }

    /// Kutlama kartları açılışı: yılbaşına yakınsa (20 Aralık–7 Ocak) yeni yıl, değilse doğum günü
    static func suggestNeutral(_ today: CalendarDay) -> String {
        (today.month == 12 && today.day >= 20) || (today.month == 1 && today.day <= 7) ? "newyear" : "birthday"
    }

    /// Bugüne en uygun vesile: yakın bayram/kandil/hicri yılbaşı/Ramazan, yoksa Cuma
    static func suggest(_ today: CalendarDay) -> String {
        func near(_ keys: Set<String>, _ days: Int) -> Bool {
            RelDays.all.contains { keys.contains($0.key) && (-2...days).contains(RelDays.days(today, $0.day)) }
        }
        if near(["rb1", "rbEve"], 3) { return "rb" }
        if near(["kb1", "kbEve"], 3) { return "kb" }
        if near(["regaib", "mirac", "berat", "kadir", "mawlid"], 1) { return "kandil" }
        if near(["hijri"], 1) { return "hijri" }
        if CalendarData.shared.ramadans.contains(where: { $0.from <= today && today <= $0.to }) { return "ramadan" }
        return "cuma"
    }
}

private func rgb(_ hex: UInt32) -> Color {
    Color(red: Double(hex >> 16 & 0xFF) / 255, green: Double(hex >> 8 & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
}

/// Kart desenleri: üst şerit (0–0,27) ve alt şerit (0,74–0,93) yazı alanının dışında; geometrik desen bütün kartta soluk
enum GreetMotifs {
    static func draw(_ p: String, _ ctx: inout GraphicsContext, _ s: CGFloat, gold: Color, ink: Color) {
        switch p {
        case "arch": arch(&ctx, s, gold)
        case "stars": stars(&ctx, s, gold, crescent: false)
        case "crescent": stars(&ctx, s, gold, crescent: true)
        case "lanterns": lanterns(&ctx, s, gold)
        case "mosque": mosque(&ctx, s, gold)
        case "tulips": tulips(&ctx, s, gold)
        case "lights": lights(&ctx, s, gold, ink)
        case "kaaba": kaaba(&ctx, s, gold)
        case "lamp": lamp(&ctx, s, gold)
        default: geo(&ctx, s, gold)
        }
    }

    /// Sekiz köşeli yıldız deseni
    static func geo(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        let step = s / 6
        for i in 0...6 {
            for j in 0...6 {
                var p = Path()
                for k in 0..<16 {
                    let a = Double.pi / 8 * Double(k)
                    let r = k % 2 == 0 ? step * 0.34 : step * 0.21
                    let pt = CGPoint(x: CGFloat(i) * step + r * CGFloat(cos(a)), y: CGFloat(j) * step + r * CGFloat(sin(a)))
                    if k == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                }
                p.closeSubpath()
                ctx.stroke(p, with: .color(gold.opacity(0.28)), lineWidth: s / 360)
            }
        }
    }

    /// Kemer ve kandil noktası
    static func arch(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        var p = Path()
        p.move(to: CGPoint(x: s * 0.12, y: s * 0.92)); p.addLine(to: CGPoint(x: s * 0.12, y: s * 0.4))
        p.addQuadCurve(to: CGPoint(x: s * 0.5, y: s * 0.06), control: CGPoint(x: s * 0.12, y: s * 0.1))
        p.addQuadCurve(to: CGPoint(x: s * 0.88, y: s * 0.4), control: CGPoint(x: s * 0.88, y: s * 0.1))
        p.addLine(to: CGPoint(x: s * 0.88, y: s * 0.92))
        ctx.stroke(p, with: .color(gold), lineWidth: s / 120)
        ctx.fill(Path(ellipseIn: CGRect(x: s * 0.475, y: s * 0.145, width: s * 0.05, height: s * 0.05)), with: .color(gold))
    }

    /// Yıldızlar (sabit tohumlu; yazı alanına düşmez), istenirse hilal
    static func stars(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color, crescent: Bool) {
        var seed: UInt64 = 7
        func rnd() -> CGFloat { seed = seed &* 6364136223846793005 &+ 1442695040888963407; return CGFloat(seed >> 33) / CGFloat(1 << 31) }
        var placed = 0
        while placed < 60 {
            let x = rnd() * s, y = rnd() * s * 0.92, a = rnd(), r = 1 + rnd() * 2.2 * s / 360
            if y > s * 0.27 && y < s * 0.8 && x > s * 0.08 && x < s * 0.92 { continue }
            ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: 2 * r, height: 2 * r)), with: .color(gold.opacity(0.35 + 0.6 * a)))
            placed += 1
        }
        if crescent {
            let cx = s * 0.76, cy = s * 0.15, rr = s * 0.08
            var moon = Path(ellipseIn: CGRect(x: cx - rr, y: cy - rr, width: 2 * rr, height: 2 * rr))
            let cut = Path(ellipseIn: CGRect(x: cx + rr * 0.45 - rr * 0.88, y: cy - rr * 0.18 - rr * 0.88, width: rr * 1.76, height: rr * 1.76))
            moon = moon.subtracting(cut)
            ctx.fill(moon, with: .color(gold))
        }
    }

    /// Asılı fenerler (Ramazan)
    static func lanterns(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        let spots: [(CGFloat, CGFloat, CGFloat)] = [(0.1, 0.14, 1.0), (0.26, 0.07, 0.8), (0.74, 0.09, 0.85), (0.9, 0.15, 1.0)]
        for (x, len, k) in spots {
            let cx = s * x, top = s * len, w = s * 0.07 * k, h = s * 0.11 * k
            var cord = Path(); cord.move(to: CGPoint(x: cx, y: 0)); cord.addLine(to: CGPoint(x: cx, y: top))
            ctx.stroke(cord, with: .color(gold.opacity(0.7)), lineWidth: s / 400)
            let c = CGPoint(x: cx, y: top + h * 0.55)
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - w * 1.4, y: c.y - w * 1.4, width: w * 2.8, height: w * 2.8)),
                     with: .radialGradient(Gradient(colors: [gold.opacity(0.35), .clear]), center: c, startRadius: 0, endRadius: w * 1.4))
            var cap = Path()
            cap.move(to: CGPoint(x: cx, y: top)); cap.addLine(to: CGPoint(x: cx + w * 0.45, y: top + h * 0.18))
            cap.addLine(to: CGPoint(x: cx - w * 0.45, y: top + h * 0.18)); cap.closeSubpath()
            ctx.fill(cap, with: .color(gold))
            var body = Path()
            body.move(to: CGPoint(x: cx - w * 0.45, y: top + h * 0.18)); body.addLine(to: CGPoint(x: cx + w * 0.45, y: top + h * 0.18))
            body.addLine(to: CGPoint(x: cx + w * 0.5, y: top + h * 0.75)); body.addLine(to: CGPoint(x: cx, y: top + h))
            body.addLine(to: CGPoint(x: cx - w * 0.5, y: top + h * 0.75)); body.closeSubpath()
            ctx.fill(body, with: .color(gold.opacity(0.85)))
            for f in [-0.2, 0.0, 0.2] as [CGFloat] {
                var l = Path(); l.move(to: CGPoint(x: cx + w * f, y: top + h * 0.22)); l.addLine(to: CGPoint(x: cx + w * f, y: top + h * 0.72))
                ctx.stroke(l, with: .color(.black.opacity(0.25)), lineWidth: s / 500)
            }
        }
    }

    /// Cami silueti (Cuma)
    static func mosque(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        let base = s * 0.93
        var p = Path()
        p.addRect(CGRect(x: s * 0.3, y: s * 0.84, width: s * 0.4, height: base - s * 0.84))
        p.addEllipse(in: CGRect(x: s * 0.37, y: s * 0.75, width: s * 0.26, height: s * 0.19))
        p.addRect(CGRect(x: s * 0.497, y: s * 0.71, width: s * 0.006, height: s * 0.05))
        for x in [0.19, 0.81] as [CGFloat] {
            p.addRect(CGRect(x: s * x - s * 0.017, y: s * 0.72, width: s * 0.034, height: base - s * 0.72))
            var cone = Path()
            cone.move(to: CGPoint(x: s * x - s * 0.021, y: s * 0.72)); cone.addLine(to: CGPoint(x: s * x, y: s * 0.65))
            cone.addLine(to: CGPoint(x: s * x + s * 0.021, y: s * 0.72)); cone.closeSubpath()
            p.addPath(cone)
            p.addRect(CGRect(x: s * x - s * 0.025, y: s * 0.78, width: s * 0.05, height: s * 0.011))
        }
        for x in [0.31, 0.69] as [CGFloat] { p.addEllipse(in: CGRect(x: s * x - s * 0.05, y: s * 0.82, width: s * 0.1, height: s * 0.06)) }
        ctx.fill(p, with: .color(gold.opacity(0.3)))
    }

    /// Lale sırası (bayramlar)
    static func tulips(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        let xs: [CGFloat] = [0.08, 0.2, 0.32, 0.68, 0.8, 0.92]
        for (i, x) in xs.enumerated() {
            let cx = s * x, fy = s * (0.78 + CGFloat(i % 2) * 0.03), bottom = s * 0.93
            var stem = Path(); stem.move(to: CGPoint(x: cx, y: bottom))
            stem.addQuadCurve(to: CGPoint(x: cx, y: fy + s * 0.05), control: CGPoint(x: cx + s * 0.015, y: (bottom + fy) / 2))
            ctx.stroke(stem, with: .color(gold.opacity(0.6)), lineWidth: s / 220)
            var leaf = Path(); leaf.move(to: CGPoint(x: cx, y: bottom - s * 0.02))
            leaf.addQuadCurve(to: CGPoint(x: cx - s * 0.045, y: bottom - s * 0.09), control: CGPoint(x: cx - s * 0.05, y: bottom - s * 0.02))
            leaf.addQuadCurve(to: CGPoint(x: cx, y: bottom - s * 0.02), control: CGPoint(x: cx - s * 0.005, y: bottom - s * 0.08))
            ctx.fill(leaf, with: .color(gold.opacity(0.45)))
            let w = s * 0.035, h = s * 0.055
            var cup = Path()
            cup.move(to: CGPoint(x: cx - w, y: fy)); cup.addLine(to: CGPoint(x: cx - w * 0.5, y: fy + h * 0.35))
            cup.addLine(to: CGPoint(x: cx, y: fy)); cup.addLine(to: CGPoint(x: cx + w * 0.5, y: fy + h * 0.35))
            cup.addLine(to: CGPoint(x: cx + w, y: fy))
            cup.addQuadCurve(to: CGPoint(x: cx, y: fy + h), control: CGPoint(x: cx + w * 1.05, y: fy + h))
            cup.addQuadCurve(to: CGPoint(x: cx - w, y: fy), control: CGPoint(x: cx - w * 1.05, y: fy + h))
            cup.closeSubpath()
            ctx.fill(cup, with: .color(gold.opacity(0.85)))
        }
    }

    /// Sarkan ışık dizisi (bayramlar)
    static func lights(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color, _ ink: Color) {
        for row in 0..<2 {
            let y0 = s * (0.04 + CGFloat(row) * 0.07), dip = s * 0.06
            let a = CGPoint(x: -s * 0.02, y: y0), b = CGPoint(x: s * 1.02, y: y0), c = CGPoint(x: s * 0.5, y: y0 + 2 * dip)
            var wire = Path(); wire.move(to: a); wire.addQuadCurve(to: b, control: c)
            ctx.stroke(wire, with: .color(gold.opacity(0.5)), lineWidth: s / 500)
            for i in 0...11 {
                let t = CGFloat(i) / 11
                let x = (1 - t) * (1 - t) * a.x + 2 * (1 - t) * t * c.x + t * t * b.x
                let y = (1 - t) * (1 - t) * a.y + 2 * (1 - t) * t * c.y + t * t * b.y
                let r = s * 0.012, col = (i + row) % 3 == 0 ? ink : gold
                let g = CGPoint(x: x, y: y + r)
                ctx.fill(Path(ellipseIn: CGRect(x: g.x - r * 2.4, y: g.y - r * 2.4, width: r * 4.8, height: r * 4.8)),
                         with: .radialGradient(Gradient(colors: [col.opacity(0.35), .clear]), center: g, startRadius: 0, endRadius: r * 2.4))
                ctx.fill(Path(ellipseIn: CGRect(x: x - r * 0.7, y: y, width: r * 1.4, height: r * 1.9)), with: .color(col.opacity(0.9)))
            }
        }
    }

    /// Kâbe (Kurban Bayramı): siyah örtü, altın kuşak
    static func kaaba(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        let c = CGPoint(x: s * 0.5, y: s * 0.84)
        ctx.fill(Path(ellipseIn: CGRect(x: c.x - s * 0.3, y: c.y - s * 0.3, width: s * 0.6, height: s * 0.6)),
                 with: .radialGradient(Gradient(colors: [gold.opacity(0.25), .clear]), center: c, startRadius: 0, endRadius: s * 0.3))
        let w = s * 0.17, h = s * 0.14, x = s * 0.5 - w / 2 - s * 0.02, y = s * 0.92 - h, d = s * 0.04
        var side = Path()
        side.move(to: CGPoint(x: x + w, y: y)); side.addLine(to: CGPoint(x: x + w + d, y: y - d * 0.5))
        side.addLine(to: CGPoint(x: x + w + d, y: y + h - d * 0.5)); side.addLine(to: CGPoint(x: x + w, y: y + h)); side.closeSubpath()
        var top = Path()
        top.move(to: CGPoint(x: x, y: y)); top.addLine(to: CGPoint(x: x + d, y: y - d * 0.5))
        top.addLine(to: CGPoint(x: x + w + d, y: y - d * 0.5)); top.addLine(to: CGPoint(x: x + w, y: y)); top.closeSubpath()
        ctx.fill(side, with: .color(Color(white: 0.05)))
        ctx.fill(top, with: .color(Color(white: 0.17)))
        ctx.fill(Path(CGRect(x: x, y: y, width: w, height: h)), with: .color(Color(white: 0.1)))
        let by = y + h * 0.22, bh = h * 0.09
        ctx.fill(Path(CGRect(x: x, y: by, width: w, height: bh)), with: .color(gold))
        var sb = Path()
        sb.move(to: CGPoint(x: x + w, y: by)); sb.addLine(to: CGPoint(x: x + w + d, y: by - d * 0.5))
        sb.addLine(to: CGPoint(x: x + w + d, y: by - d * 0.5 + bh)); sb.addLine(to: CGPoint(x: x + w, y: by + bh)); sb.closeSubpath()
        ctx.fill(sb, with: .color(gold.opacity(0.8)))
        ctx.fill(Path(CGRect(x: x + w * 0.62, y: y + h * 0.45, width: w * 0.16, height: h * 0.4)), with: .color(gold.opacity(0.9)))
    }

    /// Kandil (yağ lambası) ve alevi
    static func lamp(_ ctx: inout GraphicsContext, _ s: CGFloat, _ gold: Color) {
        let cx = s * 0.5, by = s * 0.9
        let fc = CGPoint(x: cx + s * 0.075, y: by - s * 0.1)
        ctx.fill(Path(ellipseIn: CGRect(x: fc.x - s * 0.16, y: fc.y - s * 0.16, width: s * 0.32, height: s * 0.32)),
                 with: .radialGradient(Gradient(colors: [gold.opacity(0.45), .clear]), center: fc, startRadius: 0, endRadius: s * 0.16))
        var body = Path()
        body.move(to: CGPoint(x: cx - s * 0.1, y: by - s * 0.04))
        body.addQuadCurve(to: CGPoint(x: cx + s * 0.06, y: by - s * 0.045), control: CGPoint(x: cx - s * 0.02, y: by + s * 0.03))
        body.addLine(to: CGPoint(x: cx + s * 0.08, y: by - s * 0.068))
        body.addLine(to: CGPoint(x: cx + s * 0.05, y: by - s * 0.062))
        body.addQuadCurve(to: CGPoint(x: cx - s * 0.1, y: by - s * 0.04), control: CGPoint(x: cx - s * 0.03, y: by - s * 0.09))
        body.closeSubpath()
        ctx.fill(body, with: .color(gold))
        var handle = Path()
        handle.addArc(center: CGPoint(x: cx - s * 0.1, y: by - s * 0.05), radius: s * 0.025, startAngle: .degrees(90), endAngle: .degrees(270), clockwise: false)
        ctx.stroke(handle, with: .color(gold), lineWidth: s / 150)
        ctx.fill(Path(roundedRect: CGRect(x: cx - s * 0.05, y: by - s * 0.005, width: s * 0.08, height: s * 0.018), cornerRadius: s * 0.006),
                 with: .color(gold.opacity(0.85)))
        var flame = Path()
        flame.move(to: CGPoint(x: fc.x, y: fc.y - s * 0.035))
        flame.addQuadCurve(to: CGPoint(x: fc.x, y: fc.y + s * 0.03), control: CGPoint(x: fc.x + s * 0.03, y: fc.y + s * 0.01))
        flame.addQuadCurve(to: CGPoint(x: fc.x, y: fc.y - s * 0.035), control: CGPoint(x: fc.x - s * 0.03, y: fc.y + s * 0.01))
        ctx.fill(flame, with: .color(rgb(0xFFD173)))
        stars(&ctx, s, gold.opacity(0.7), crescent: false)
    }
}

/// Kartın kendisi (kare)
struct GreetingArt: View {
    let occasion: String
    let style: String
    let pattern: String
    let message: String
    let today: CalendarDay

    var body: some View {
        let (top, bottom, ink, gold): (Color, Color, Color, Color) = switch style {
        case "emerald": (rgb(0x0B4D3B), rgb(0x06281F), .white, rgb(0xE9C46A))
        case "gold": (rgb(0xFBF3E0), rgb(0xEAD9B0), rgb(0x3B2A12), rgb(0xA8761E))
        default: (rgb(0x101A3F), rgb(0x2A1D52), .white, rgb(0xFFD173))
        }
        GeometryReader { g in
            let s = g.size.width
            ZStack {
                LinearGradient(colors: [top, bottom], startPoint: .top, endPoint: .bottom)
                Canvas { ctx, size in GreetMotifs.draw(pattern, &ctx, size.width, gold: gold, ink: ink) }
                RadialGradient(colors: [gold.opacity(0.13), .clear], center: UnitPoint(x: 0.5, y: 0.5), startRadius: 0, endRadius: s * 0.45)
                VStack(spacing: s * 0.022) {
                    if AppLocale.code != "ar", let ar = Greetings.arabic[occasion] {
                        Text(ar).font(.system(size: s * 0.055)).foregroundStyle(gold)
                    }
                    Text(L("greet.\(occasion)")).font(.system(size: s * 0.066, weight: .bold)).foregroundStyle(ink)
                        .multilineTextAlignment(.center)
                    Text(message).font(.system(size: s * 0.036)).foregroundStyle(ink.opacity(0.9))
                        .multilineTextAlignment(.center).lineSpacing(s * 0.006).minimumScaleFactor(0.6)
                    // Dini kartta hicri tarih, nötr kartta miladi tarih
                    Text(Greetings.neutral.contains(occasion) ? Fmt.day(today) : (Hijri.date(today.noon(in: .current))?.text ?? Fmt.day(today)))
                        .font(.system(size: s * 0.028)).foregroundStyle(ink.opacity(0.7))
                }
                .padding(.horizontal, s * 0.14)
                .frame(maxHeight: s * 0.48)
                .offset(y: -s * 0.005)
                VStack {
                    Spacer()
                    Text(verbatim: "Saat Tarih · time.bumba.tr").font(.system(size: s * 0.027)).foregroundStyle(ink.opacity(0.65))
                        .padding(.bottom, s * 0.025)
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

/// Sistem paylaşım penceresi: görsel ve bağlantılı metin birlikte
private struct ActivitySheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController { UIActivityViewController(activityItems: items, applicationActivities: nil) }
    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}

struct GreetingView: View {
    /// Kutlama kartları (nötr) yalnız nötr vesileleri, İbadet'teki tebrik kartları önce dini sonra nötr vesileleri gösterir
    var neutral = false
    @State private var picked = ""
    /// Seçili vesile; seçilmediyse bugüne uygun olan
    private var occasion: String { picked.isEmpty ? (neutral ? Greetings.suggestNeutral(today) : Greetings.suggest(today)) : picked }
    @State private var style = "night"
    @State private var pattern = ""
    /// Her girişte yeni söz (4 Kasım 2026, sahibin isteği)
    @State private var seed = UInt64.random(in: 0..<UInt64(UInt32.max))
    @State private var own = ""
    @State private var image: UIImage?
    @State private var sharing = false
    private let today = CalendarDay(Date())

    private var currentPattern: String { pattern.isEmpty ? (Greetings.patterns[occasion]?.first ?? "geo") : pattern }
    private var message: String {
        let t = own.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? Greetings.message(occasion, seed: seed) : t
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                GreetingArt(occasion: occasion, style: style, pattern: currentPattern, message: message, today: today)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .accessibilityElement(children: .combine)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(neutral ? Greetings.neutral : Greetings.occasions, id: \.self) { o in
                            chip(L("greet.\(o)"), o == occasion) { picked = o; pattern = "" }
                        }
                    }
                }
                HStack {
                    Button { seed = UInt64.random(in: 0..<UInt64(UInt32.max)); own = "" } label: {
                        Label(L("greet.shuffle"), systemImage: "shuffle")
                    }
                    .buttonStyle(.bordered)
                    .tint(Theme.accent)
                    Spacer()
                }
                TextField(L("greet.own"), text: $own, axis: .vertical)
                    .lineLimit(1...4)
                    .textFieldStyle(.roundedBorder)
                Text(L("greet.style")).font(.footnote).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 8) {
                    ForEach(Greetings.patterns[occasion] ?? ["geo"], id: \.self) { p in
                        chip(L("greet.pat.\(p)"), p == currentPattern) { pattern = p }
                    }
                    Spacer()
                }
                Text(L("greet.palette")).font(.footnote).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 8) {
                    let names = ["night": L("quran.theme.night"), "emerald": L("quran.theme.green"), "gold": L("quran.theme.paper")]
                    ForEach(Greetings.styles, id: \.self) { st in chip(names[st] ?? st, st == style) { style = st } }
                    Spacer()
                }
                Button { sharing = image != nil } label: {
                    Label(L("share.card"), systemImage: "square.and.arrow.up").font(.headline)
                        .frame(maxWidth: .infinity).padding(.vertical, 12)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .disabled(image == nil)
                Text(L("greet.note")).font(.caption).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L(neutral ? "greet.titleN" : "greet.title"))
        .navigationBarTitleDisplayMode(.large)
        .task(id: "\(occasion)|\(style)|\(currentPattern)|\(message)") {
            let r = ImageRenderer(content: GreetingArt(occasion: occasion, style: style, pattern: currentPattern, message: message, today: today)
                .frame(width: 360, height: 360))
            r.scale = 3
            image = r.uiImage
        }
        .sheet(isPresented: $sharing) {
            if let image {
                ActivitySheet(items: [image, T("greet.shareText", L("greet.\(occasion)"), Support.appStore.absoluteString)])
                    .presentationDetents([.medium, .large])
            }
        }
    }

    private func chip(_ title: String, _ on: Bool, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(.subheadline)
                .foregroundStyle(on ? Color.white : Theme.text)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(on ? Theme.accent : Theme.surface2, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}
