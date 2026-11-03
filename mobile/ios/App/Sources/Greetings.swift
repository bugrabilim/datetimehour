import SwiftUI
import UIKit
import SaatCore

/*
 * Tebrik kartları (3 Kasım 2026, 2. analiz #74; Android'deki Greetings.kt ile aynı): Cuma, Ramazan, Ramazan ve Kurban
 * bayramı, kandil ve hicri yılbaşı için telefonda çizilen kare görsel; üç desen (gece, zümrüt, altın). Dış görsel yok,
 * görsel yalnız kullanıcı paylaşınca sistemin paylaşım penceresiyle gider. iMessage çıkartma paketi ayrı bir uzantı ve
 * paket kimliği istediği için bu turda yok.
 */
enum Greetings {
    static let occasions = ["cuma", "ramadan", "rb", "kb", "kandil", "hijri"]
    static let styles = ["night", "emerald", "gold"]
    static let arabic = ["cuma": "جمعة مباركة", "ramadan": "رمضان كريم", "rb": "عيد مبارك", "kb": "عيد مبارك",
                         "kandil": "ليلة مباركة", "hijri": "عام هجري سعيد"]

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

/// Kartın kendisi (kare)
struct GreetingArt: View {
    let occasion: String
    let style: String
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
                Canvas { ctx, size in
                    let s = size.width
                    switch style {
                    case "night":
                        // Yıldızlar (sabit tohumlu; yazı alanına düşmez) ve hilal
                        var seed: UInt64 = 7
                        func rnd() -> CGFloat { seed = seed &* 6364136223846793005 &+ 1442695040888963407; return CGFloat(seed >> 33) / CGFloat(1 << 31) }
                        var placed = 0
                        while placed < 60 {
                            let x = rnd() * s, y = rnd() * s * 0.9, a = rnd(), r = 1 + rnd() * 2.2
                            if y > s * 0.31 && y < s * 0.8 && x > s * 0.1 && x < s * 0.9 { continue }
                            ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: 2 * r, height: 2 * r)), with: .color(gold.opacity(0.35 + 0.6 * a)))
                            placed += 1
                        }
                        let cx = s * 0.74, cy = s * 0.2, rr = s * 0.08
                        var moon = Path(ellipseIn: CGRect(x: cx - rr, y: cy - rr, width: 2 * rr, height: 2 * rr))
                        let cut = Path(ellipseIn: CGRect(x: cx + rr * 0.45 - rr * 0.88, y: cy - rr * 0.18 - rr * 0.88, width: rr * 1.76, height: rr * 1.76))
                        moon = moon.subtracting(cut)
                        ctx.fill(moon, with: .color(gold))
                    case "emerald":
                        // Sekiz köşeli yıldız deseni
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
                    default:
                        // Kemer ve fener
                        var p = Path()
                        p.move(to: CGPoint(x: s * 0.18, y: s * 0.92)); p.addLine(to: CGPoint(x: s * 0.18, y: s * 0.42))
                        p.addQuadCurve(to: CGPoint(x: s * 0.5, y: s * 0.08), control: CGPoint(x: s * 0.18, y: s * 0.12))
                        p.addQuadCurve(to: CGPoint(x: s * 0.82, y: s * 0.42), control: CGPoint(x: s * 0.82, y: s * 0.12))
                        p.addLine(to: CGPoint(x: s * 0.82, y: s * 0.92))
                        ctx.stroke(p, with: .color(gold), lineWidth: s / 120)
                        ctx.fill(Path(ellipseIn: CGRect(x: s * 0.475, y: s * 0.165, width: s * 0.05, height: s * 0.05)), with: .color(gold))
                    }
                }
                RadialGradient(colors: [gold.opacity(0.13), .clear], center: UnitPoint(x: 0.5, y: 0.52), startRadius: 0, endRadius: s * 0.45)
                VStack(spacing: s * 0.03) {
                    if AppLocale.code != "ar", let ar = Greetings.arabic[occasion] {
                        Text(ar).font(.system(size: s * 0.06)).foregroundStyle(gold)
                    }
                    Text(L("greet.\(occasion)")).font(.system(size: s * 0.075, weight: .bold)).foregroundStyle(ink)
                        .multilineTextAlignment(.center)
                    Text(Hijri.date(today.noon(in: .current))?.text ?? Fmt.day(today))
                        .font(.system(size: s * 0.032)).foregroundStyle(ink.opacity(0.75))
                }
                .padding(.horizontal, s * 0.15)
                .offset(y: s * 0.04)
                VStack {
                    Spacer()
                    Text(verbatim: "Saat Tarih · time.bumba.tr").font(.system(size: s * 0.028)).foregroundStyle(ink.opacity(0.6))
                        .padding(.bottom, s * 0.035)
                }
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

struct GreetingView: View {
    @State private var occasion = Greetings.suggest(CalendarDay(Date()))
    @State private var style = "night"
    @State private var image: UIImage?
    @Environment(\.displayScale) private var displayScale
    private let today = CalendarDay(Date())

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                GreetingArt(occasion: occasion, style: style, today: today)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .accessibilityLabel(L("greet.\(occasion)"))
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Greetings.occasions, id: \.self) { o in chip(L("greet.\(o)"), o == occasion) { occasion = o } }
                    }
                }
                Text(L("greet.style")).font(.footnote).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 8) {
                    let names = ["night": L("quran.theme.night"), "emerald": L("quran.theme.green"), "gold": L("quran.theme.paper")]
                    ForEach(Greetings.styles, id: \.self) { st in chip(names[st] ?? st, st == style) { style = st } }
                    Spacer()
                }
                if let image {
                    ShareLink(item: Image(uiImage: image), preview: SharePreview(L("greet.\(occasion)"), image: Image(uiImage: image))) {
                        Label(L("share.card"), systemImage: "square.and.arrow.up").font(.headline)
                            .frame(maxWidth: .infinity).padding(.vertical, 12)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                }
                Text(L("greet.note")).font(.caption).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("greet.title"))
        .navigationBarTitleDisplayMode(.large)
        .task(id: occasion + style) {
            let r = ImageRenderer(content: GreetingArt(occasion: occasion, style: style, today: today).frame(width: 360, height: 360))
            r.scale = 3
            image = r.uiImage
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
