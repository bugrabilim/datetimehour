import SwiftUI

/// Resimli namaz ve abdest rehberi (2. analiz #68; 4 Kasım 2026): adım adım (n/9) çizimler, erkek/kadın seçimi ve
/// Hanefî farkları. Duruşlar 100×100 kutuda eklem çizgileriyle tanımlı (Android'deki GuideFigures ile aynı sayılar);
/// yüz sağa (kıbleye) dönük. Abdest adımları simgeyle. Kayıt yok.
enum GuideFigures {
    static let prayer = ["stand", "takbir", "fold", "ruku", "stand", "sajda", "fold", "sit", "salam"]
    static let wudu = ["🤲", "✋", "👄", "👃", "🙂", "💪", "💆", "👂", "🦶"]
    static let diff: Set<Int> = [1, 2, 3, 5, 7]

    struct Pose {
        let head: CGPoint
        let lines: [[CGFloat]]
    }

    private static let body: [[CGFloat]] = [[50, 22, 50, 55], [50, 55, 51, 75], [51, 75, 50, 95]]

    static func pose(_ id: String, female: Bool) -> Pose {
        switch id {
        case "takbir":
            return Pose(head: CGPoint(x: 50, y: 14), lines: body + [[50, 26, 57, 26], [57, 26, female ? 58 : 56, female ? 24 : 14]])
        case "fold":
            let ey: CGFloat = female ? 38 : 44
            let hy: CGFloat = female ? 33 : 48
            return Pose(head: CGPoint(x: 50, y: 14), lines: body + [[50, 27, 56, ey], [56, ey, 60, hy]])
        case "ruku":
            if female {
                return Pose(head: CGPoint(x: 70, y: 38), lines: [[40, 56, 64, 44], [40, 56, 42, 75], [42, 75, 40, 95], [60, 46, 48, 62], [48, 62, 43, 70]])
            }
            return Pose(head: CGPoint(x: 78, y: 53), lines: [[40, 56, 70, 55], [40, 56, 40, 75], [40, 75, 40, 95], [66, 56, 54, 66], [54, 66, 42, 73]])
        case "sajda":
            let ex: CGFloat = female ? 64 : 70
            let ey: CGFloat = female ? 90 : 86
            return Pose(head: CGPoint(x: 74, y: 86), lines: [[38, 74, 66, 82], [38, 74, 46, 93], [46, 93, 22, 94], [62, 81, ex, ey], [ex, ey, 78, 94]])
        case "sit", "salam":
            if female {
                return Pose(head: CGPoint(x: 46, y: 42), lines: [[44, 84, 45, 50], [44, 84, 66, 88], [66, 88, 76, 94], [45, 54, 56, 68], [56, 68, 62, 80]])
            }
            return Pose(head: CGPoint(x: 46, y: 42), lines: [[42, 82, 45, 50], [42, 82, 64, 88], [64, 88, 36, 93], [36, 93, 30, 82], [45, 54, 56, 68], [56, 68, 62, 80]])
        default:
            return Pose(head: CGPoint(x: 50, y: 14), lines: body + [[50, 27, 51, 40], [51, 40, 52, 52]])
        }
    }
}

struct GuideFigure: View {
    let id: String
    let female: Bool

    var body: some View {
        Canvas { ctx, size in
            let s = min(size.width, size.height) / 100
            let ox = (size.width - 100 * s) / 2
            func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: y * s) }
            func line(_ a: CGPoint, _ b: CGPoint, _ color: Color, _ w: CGFloat) {
                var path = Path()
                path.move(to: a)
                path.addLine(to: b)
                ctx.stroke(path, with: .color(color), style: StrokeStyle(lineWidth: w, lineCap: .round))
            }
            line(p(12, 96.5), p(92, 96.5), Theme.accent.opacity(0.35), 5 * s)
            let pose = GuideFigures.pose(id, female: female)
            for l in pose.lines { line(p(l[0], l[1]), p(l[2], l[3]), Theme.ink, 4.2 * s) }
            let h = pose.head
            if female {
                let r = 9.5 * s
                let c = p(h.x - 1, h.y)
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)), with: .color(Theme.accent.opacity(0.55)))
            }
            let r = 6.5 * s
            let c = p(h.x, h.y)
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)), with: .color(Theme.ink))
            if id == "salam" {
                line(p(h.x - 10, h.y - 14), p(h.x + 10, h.y - 14), Theme.accent, 2.5 * s)
                line(p(h.x + 10, h.y - 14), p(h.x + 6, h.y - 17), Theme.accent, 2.5 * s)
                line(p(h.x - 10, h.y - 14), p(h.x - 6, h.y - 11), Theme.accent, 2.5 * s)
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityHidden(true)
    }
}

/// Rehberin başındaki resimli adım kartı; [howto] ve [wudu] adım metinleri guide.json'dan
struct GuidePictureCard: View {
    let howto: [String]
    let wudu: [String]
    @State private var mode = "prayer"
    @State private var female = false
    @State private var step = 0

    var body: some View {
        let isPrayer = mode == "prayer"
        let n = isPrayer ? GuideFigures.prayer.count : GuideFigures.wudu.count
        let i = min(max(0, step), n - 1)
        let texts = isPrayer ? howto : wudu
        VStack(alignment: .leading, spacing: 10) {
            Text(L("guide.pic.title")).font(.headline).foregroundStyle(Theme.text)
            Picker("", selection: $mode) {
                Text(L("guide.pic.prayer")).tag("prayer")
                Text(L("guide.pic.wudu")).tag("wudu")
            }
            .pickerStyle(.segmented)
            .onChange(of: mode) { step = 0 }
            if isPrayer {
                Picker("", selection: $female) {
                    Text(L("guide.pic.male")).tag(false)
                    Text(L("guide.pic.female")).tag(true)
                }
                .pickerStyle(.segmented)
            }
            ZStack {
                if isPrayer {
                    GuideFigure(id: GuideFigures.prayer[i], female: female).frame(width: 200, height: 200)
                } else {
                    Text(GuideFigures.wudu[i]).font(.system(size: 96))
                }
            }
            .frame(maxWidth: .infinity, minHeight: 200)
            Text("\(i + 1)/\(n)").font(.headline.monospacedDigit()).foregroundStyle(Theme.accent).frame(maxWidth: .infinity)
            if i < texts.count { Text(texts[i]).foregroundStyle(Theme.text) }
            if isPrayer && GuideFigures.diff.contains(i) {
                Text(L((female ? "guide.pic.f." : "guide.pic.m.") + "\(i)")).font(.subheadline).foregroundStyle(Theme.accent)
            }
            HStack {
                Button("‹ " + L("hiit.prev")) { step = max(0, i - 1) }.disabled(i == 0)
                Spacer()
                Button(L("onboard.next") + " ›") { step = min(n - 1, i + 1) }.disabled(i >= n - 1)
            }
            .tint(Theme.accent)
            if isPrayer { Text(L("guide.pic.note")).font(.caption).foregroundStyle(Theme.muted) }
        }
        .card()
    }
}
