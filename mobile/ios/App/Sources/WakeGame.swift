import SwiftUI

/*
 * Uyanma oyunu (3 Kasım 2026, 2. analiz #7; sahibin ekran görüntüleri): alarmı kapatınca civciv uyanır ve puan
 * kazanılır; puanla civciv büyür (yumurta → civciv → ibikli → atkılı → taçlı). Puan kayıttan hesaplanır: her uyanış 10,
 * ertelemeden kalkınca +10 (bir erteleme +3, iki ve üstü 0), kapatma görevi +5, üst üste ertelemesiz her gün +2 (en çok
 * +10). Kayıt `wake.events` (JSON `[{d, w, a, s, t}]`; gün başına bir kayıt, en çok 400). iPhone'da alarm Apple'ın
 * ekranında kapandığı için kalkış, günaydın kartı (MorningCheck) ya da kapatma görevi bitince yazılır; ertelemeler
 * erteleme düğmesinde sayılır (`wake.snoozes`, `wake.snoozeAt`). Android'deki WakeGame.kt ile aynı kurallar.
 */
enum WakeGame {
    static let key = "wake.events"
    static let max = 400
    private static let snoozesKey = "wake.snoozes"
    private static let snoozeAtKey = "wake.snoozeAt"

    struct Event: Codable, Equatable {
        var d: String
        var w: Int
        var a: Int
        var s: Int
        var t: Bool
    }

    struct Scored { let e: Event; let points: Int; let streak: Int }

    static func load() -> [Event] {
        guard let data = UserDefaults.standard.string(forKey: key)?.data(using: .utf8),
              let list = try? JSONDecoder().decode([Event].self, from: data) else { return [] }
        return list
    }

    static func save(_ l: [Event]) {
        if let data = try? JSONEncoder().encode(Array(l.suffix(max))) {
            UserDefaults.standard.set(String(decoding: data, as: UTF8.self), forKey: key)
        }
    }

    static func clear() { UserDefaults.standard.removeObject(forKey: key) }

    /// Aynı gün ikinci kez yazılırsa birleştirilir (erteleme en büyüğü, görev ya da ile)
    static func merge(_ log: [Event], _ e: Event) -> [Event] {
        guard let i = log.firstIndex(where: { $0.d == e.d }) else { return Array((log + [e]).sorted { $0.d < $1.d }.suffix(max)) }
        var l = log
        l[i].s = Swift.max(l[i].s, e.s)
        l[i].t = l[i].t || e.t
        if l[i].a < 0 { l[i].a = e.a }
        return l
    }

    /// Erteleme düğmesi (SnoozeAlarmIntent): son 3 saatteki ertelemeler sayılır
    static func noteSnooze(_ now: Date = Date()) {
        let d = UserDefaults.standard
        let fresh = now.timeIntervalSince1970 - d.double(forKey: snoozeAtKey) < 3 * 3600
        d.set((fresh ? d.integer(forKey: snoozesKey) : 0) + 1, forKey: snoozesKey)
        d.set(now.timeIntervalSince1970, forKey: snoozeAtKey)
    }

    /// Kalkış kaydı: `wake` kalkış anı, `alarm` alarmın çalma anı (bilinmiyorsa nil)
    static func record(wake: Date, alarm: Date?, task: Bool) {
        let d = UserDefaults.standard
        let snoozes = wake.timeIntervalSince1970 - d.double(forKey: snoozeAtKey) < 3 * 3600 ? d.integer(forKey: snoozesKey) : 0
        d.removeObject(forKey: snoozesKey)
        let cal = Calendar.current
        func minutes(_ t: Date) -> Int { cal.component(.hour, from: t) * 60 + cal.component(.minute, from: t) }
        save(merge(load(), Event(d: Study.dayString(wake), w: minutes(wake), a: alarm.map(minutes) ?? -1, s: snoozes, t: task)))
    }

    static func scored(_ log: [Event]) -> [Scored] {
        var streak = 0
        var prev: Date?
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return log.sorted { $0.d < $1.d }.map { e in
            let day = f.date(from: e.d)
            let consecutive = prev != nil && day != nil && Calendar.current.date(byAdding: .day, value: 1, to: prev!).map { Calendar.current.isDate($0, inSameDayAs: day!) } == true
            streak = e.s == 0 ? (consecutive ? streak + 1 : 1) : 0
            prev = day
            let base = 10 + (e.s == 0 ? 10 : e.s == 1 ? 3 : 0) + (e.t ? 5 : 0)
            return Scored(e: e, points: base + (e.s == 0 ? Swift.min(10, 2 * (streak - 1)) : 0), streak: streak)
        }
    }

    static func total(_ log: [Event]) -> Int { scored(log).reduce(0) { $0 + $1.points } }

    /// Seviye: 1 → 0, 2 → 100, 3 → 300, 4 → 600 … en çok 10
    static func threshold(_ level: Int) -> Int { 100 * level * (level - 1) / 2 }

    static func level(_ points: Int) -> Int {
        var l = 1
        while l < 10 && points >= threshold(l + 1) { l += 1 }
        return l
    }

    /// 0 yumurta, 1 civciv, 2 ibikli, 3 atkılı, 4 taçlı
    static func stage(_ level: Int) -> Int { level <= 1 ? 0 : level <= 3 ? 1 : level <= 5 ? 2 : level <= 7 ? 3 : 4 }

    static func awakeToday(_ log: [Event], today: Date = Date()) -> Bool { log.contains { $0.d == Study.dayString(today) } }

    static func bestStreak(_ log: [Event]) -> Int { scored(log).map(\.streak).max() ?? 0 }

    static func hm(_ m: Int) -> String { m < 0 ? "–" : String(format: "%02d:%02d", (m / 60) % 24, m % 60) }
}

private let petYellow = Color(hex: 0xFFD43B)
private let petYellowDark = Color(hex: 0xF2B705)
private let petOrange = Color(hex: 0xF08C00)
private let petRed = Color(hex: 0xE03131)
private let petInk = Color(hex: 0x2B2B2B)

/// Civciv çizimi: uyurken gözler kapalı ve "z", uyanıkken gözler açık; hâline göre ibik, atkı, taç
struct WakePet: View {
    let stage: Int
    let awake: Bool
    let size: CGFloat

    var body: some View {
        Canvas { ctx, sz in draw(&ctx, sz) }
            .frame(width: size, height: size)
            .accessibilityElement()
            .accessibilityLabel(L(awake ? "wakegame.awake" : "wakegame.sleeping"))
    }

    private func oval(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> Path { Path(ellipseIn: CGRect(x: x, y: y, width: w, height: h)) }
    private func circle(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat) -> Path { oval(cx - r, cy - r, 2 * r, 2 * r) }

    private func draw(_ ctx: inout GraphicsContext, _ sz: CGSize) {
        let w = sz.width, h = sz.height, cx = w / 2
        ctx.fill(oval(w * 0.2, h * 0.86, w * 0.6, h * 0.08), with: .color(.black.opacity(0.10)))
        if stage == 0 {
            let egg = oval(w * 0.27, h * 0.18, w * 0.46, h * 0.7)
            ctx.fill(egg, with: .color(Color(hex: 0xFFF4E0)))
            ctx.stroke(egg, with: .color(Color(hex: 0xE9D8BA)), lineWidth: w * 0.012)
            if awake {
                var crack = Path()
                let pts: [(CGFloat, CGFloat)] = [(0.29, 0.48), (0.36, 0.43), (0.42, 0.5), (0.5, 0.42), (0.57, 0.5), (0.64, 0.43), (0.71, 0.48)]
                crack.move(to: CGPoint(x: w * pts[0].0, y: h * pts[0].1))
                for p in pts.dropFirst() { crack.addLine(to: CGPoint(x: w * p.0, y: h * p.1)) }
                ctx.stroke(crack, with: .color(Color(hex: 0xB08D57)), lineWidth: w * 0.015)
                eyes(&ctx, cx, h * 0.58, w * 0.08, w, open: true)
            } else { zzz(&ctx, w, h) }
            return
        }
        let r = w * (0.24 + 0.02 * CGFloat(stage)), cy = h * 0.58
        ctx.fill(oval(cx - r * 1.15, cy - r * 0.1, r * 0.55, r * 0.75), with: .color(petYellowDark))
        ctx.fill(oval(cx + r * 0.6, cy - r * 0.1, r * 0.55, r * 0.75), with: .color(petYellowDark))
        ctx.fill(Path(roundedRect: CGRect(x: cx - r * 0.45, y: cy + r * 0.85, width: r * 0.25, height: r * 0.3), cornerRadius: r * 0.1), with: .color(petOrange))
        ctx.fill(Path(roundedRect: CGRect(x: cx + r * 0.2, y: cy + r * 0.85, width: r * 0.25, height: r * 0.3), cornerRadius: r * 0.1), with: .color(petOrange))
        ctx.fill(circle(cx, cy, r), with: .color(petYellow))
        if stage >= 2 {
            for dx in [-0.22, 0, 0.22] as [CGFloat] { ctx.fill(circle(cx + r * dx, cy - r * 1.02, r * 0.16), with: .color(petRed)) }
        }
        if stage >= 4 {
            let top = cy - r * 1.05
            var crown = Path()
            crown.move(to: CGPoint(x: cx - r * 0.45, y: top))
            let crownPts: [(CGFloat, CGFloat)] = [(-0.45, -0.38), (-0.22, -0.18), (0, -0.45), (0.22, -0.18), (0.45, -0.38), (0.45, 0)]
            for p in crownPts {
                crown.addLine(to: CGPoint(x: cx + r * p.0, y: top + r * p.1))
            }
            crown.closeSubpath()
            ctx.fill(crown, with: .color(Color(hex: 0xFAB005)))
            ctx.stroke(crown, with: .color(Color(hex: 0xE67700)), lineWidth: r * 0.04)
        }
        eyes(&ctx, cx, cy - r * 0.2, r * 0.32, w, open: awake)
        var beak = Path()
        beak.move(to: CGPoint(x: cx - r * 0.14, y: cy + r * 0.02))
        beak.addLine(to: CGPoint(x: cx + r * 0.14, y: cy + r * 0.02))
        beak.addLine(to: CGPoint(x: cx, y: cy + r * 0.22))
        beak.closeSubpath()
        ctx.fill(beak, with: .color(petOrange))
        ctx.fill(circle(cx - r * 0.5, cy + r * 0.05, r * 0.1), with: .color(Color(hex: 0xFF8787).opacity(0.5)))
        ctx.fill(circle(cx + r * 0.5, cy + r * 0.05, r * 0.1), with: .color(Color(hex: 0xFF8787).opacity(0.5)))
        if stage >= 3 {
            let blue = Color(hex: 0x1C7ED6)
            ctx.fill(Path(roundedRect: CGRect(x: cx - r * 0.75, y: cy + r * 0.38, width: r * 1.5, height: r * 0.2), cornerRadius: r * 0.1), with: .color(blue))
            ctx.fill(Path(roundedRect: CGRect(x: cx + r * 0.35, y: cy + r * 0.45, width: r * 0.2, height: r * 0.45), cornerRadius: r * 0.08), with: .color(blue))
        }
        if !awake { zzz(&ctx, w, h) }
    }

    private func eyes(_ ctx: inout GraphicsContext, _ cx: CGFloat, _ y: CGFloat, _ dx: CGFloat, _ w: CGFloat, open: Bool) {
        for x in [-dx, dx] {
            if open {
                ctx.fill(circle(cx + x, y, w * 0.03), with: .color(petInk))
                ctx.fill(circle(cx + x + w * 0.01, y - w * 0.01, w * 0.01), with: .color(.white))
            } else {
                var arc = Path()
                arc.addArc(center: CGPoint(x: cx + x, y: y - w * 0.005), radius: w * 0.035, startAngle: .degrees(20), endAngle: .degrees(160), clockwise: false)
                ctx.stroke(arc, with: .color(petInk), lineWidth: w * 0.012)
            }
        }
    }

    private func zzz(_ ctx: inout GraphicsContext, _ w: CGFloat, _ h: CGFloat) {
        let marks: [(CGFloat, CGFloat, CGFloat)] = [(0.74, 0.3, 0.07), (0.82, 0.2, 0.055), (0.88, 0.12, 0.04)]
        for (x, y, s) in marks {
            var p = Path()
            p.move(to: CGPoint(x: w * x, y: h * y)); p.addLine(to: CGPoint(x: w * (x + s), y: h * y))
            p.addLine(to: CGPoint(x: w * x, y: h * y + w * s)); p.addLine(to: CGPoint(x: w * (x + s), y: h * y + w * s))
            ctx.stroke(p, with: .color(Color(hex: 0x748FFC)), lineWidth: w * 0.012)
        }
    }
}

/// Uyanma oyunu: civciv (Beni uyandır), Puanlarım, Rapor
struct WakeGameView: View {
    var initialTab = "pet"
    @State private var log = WakeGame.load()
    @State private var tab = ""
    @State private var confirm = false

    var body: some View {
        let total = WakeGame.total(log)
        let level = WakeGame.level(total)
        let awake = WakeGame.awakeToday(log)
        let scored = WakeGame.scored(log)
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Picker(L("wakegame.title"), selection: $tab) {
                    ForEach(["pet", "points", "report"], id: \.self) { Text(L("wakegame.tab.\($0)")).tag($0) }
                }
                .pickerStyle(.segmented)
                switch tab {
                case "points":
                    card {
                        Text(L("wakegame.total")).font(.footnote).foregroundStyle(Theme.muted)
                        Text("\(total)").font(.system(size: 44, weight: .bold, design: .rounded)).foregroundStyle(Theme.accent)
                        levelBar(total, level)
                    }
                    card {
                        Text(L("wakegame.how")).font(.headline)
                        Text(L("wakegame.howText")).font(.subheadline).foregroundStyle(Theme.muted)
                    }
                    if !scored.isEmpty {
                        card {
                            ForEach(Array(scored.suffix(10).reversed().enumerated()), id: \.offset) { _, s in
                                HStack {
                                    Text(Self.dayLabel(s.e.d))
                                    Spacer()
                                    Text(T("wakegame.earned", s.points)).fontWeight(.semibold).foregroundStyle(Theme.accent)
                                }
                            }
                        }
                    }
                case "report":
                    report(scored)
                default:
                    card {
                        VStack(spacing: 8) {
                            WakePet(stage: WakeGame.stage(level), awake: awake, size: 200)
                            Text(L(awake ? "wakegame.awakeTitle" : "wakegame.wakeMe")).font(.title2.bold())
                            Text(L(awake ? "wakegame.awakeText" : "wakegame.sleepText")).foregroundStyle(Theme.muted)
                                .multilineTextAlignment(.center)
                            levelBar(total, level)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                if log.isEmpty {
                    Text(L("wakegame.empty")).font(.subheadline).foregroundStyle(Theme.muted)
                } else {
                    Button(L("wakegame.reset"), role: .destructive) { confirm = true }
                }
                Text(L("wakegame.privacyIOS")).font(.footnote).foregroundStyle(Theme.muted)
            }
            .foregroundStyle(Theme.text)
            .padding(16)
        }
        .themedBackground()
        .navigationTitle(L("wakegame.title"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if tab.isEmpty { tab = initialTab }; log = WakeGame.load() }
        .confirmationDialog(L("wakegame.resetConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("wakegame.reset"), role: .destructive) { WakeGame.clear(); log = [] }
        }
    }

    private func card<C: View>(@ViewBuilder _ content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 8) { content() }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
    }

    private func levelBar(_ total: Int, _ level: Int) -> some View {
        let lo = WakeGame.threshold(level), hi = WakeGame.threshold(level + 1)
        let frac = level >= 10 ? 1 : Double(total - lo) / Double(hi - lo)
        return VStack(spacing: 4) {
            HStack {
                Text(T("wakegame.level", level)).font(.subheadline.weight(.semibold))
                Spacer()
                if level < 10 { Text(T("wakegame.next", hi - total)).font(.footnote).foregroundStyle(Theme.muted) }
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Theme.surface2)
                    Capsule().fill(petYellowDark).frame(width: geo.size.width * min(1, max(0, frac)))
                }
            }
            .frame(height: 10)
        }
    }

    private func report(_ scored: [WakeGame.Scored]) -> some View {
        let lo = Study.dayString(Calendar.current.date(byAdding: .day, value: -6, to: Date()) ?? Date())
        let week = log.filter { $0.d >= lo }
        let avg = week.isEmpty ? -1 : week.reduce(0) { $0 + $1.w } / week.count
        let onTime = week.isEmpty ? 0 : week.filter { $0.s == 0 }.count * 100 / week.count
        return VStack(spacing: 14) {
            card {
                Text(L("wakegame.week")).font(.headline)
                HStack(spacing: 8) {
                    stat(L("wakegame.avgWake"), WakeGame.hm(avg))
                    stat(L("wakegame.onTime"), Fmt.percent(onTime))
                }
                HStack(spacing: 8) {
                    stat(L("wakegame.snoozes"), "\(week.reduce(0) { $0 + $1.s })")
                    stat(L("wakegame.bestStreak"), T("wakegame.days", WakeGame.bestStreak(log)))
                }
            }
            if !scored.isEmpty {
                card {
                    ForEach(Array(scored.suffix(14).reversed().enumerated()), id: \.offset) { _, s in
                        VStack(alignment: .leading, spacing: 2) {
                            HStack {
                                Text(Self.dayLabel(s.e.d)).fontWeight(.semibold)
                                Spacer()
                                Text(WakeGame.hm(s.e.w)).monospacedDigit()
                            }
                            Text(parts(s).joined(separator: " · ")).font(.subheadline).foregroundStyle(Theme.muted)
                        }
                    }
                }
            }
        }
    }

    private func parts(_ s: WakeGame.Scored) -> [String] {
        var p: [String] = []
        if s.e.a >= 0 { p.append(T("wakegame.alarmAt", WakeGame.hm(s.e.a))) }
        p.append(s.e.s == 0 ? L("wakegame.noSnooze") : T("wakegame.snoozeN", s.e.s))
        if s.e.t { p.append(L("wakegame.taskDone")) }
        p.append(T("wakegame.earned", s.points))
        return p
    }

    private func stat(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption).foregroundStyle(Theme.muted).lineLimit(1)
            Text(value).font(.title3.bold())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 12))
    }

    static func dayLabel(_ d: String) -> String {
        let p = d.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3, let date = Calendar.current.date(from: DateComponents(year: p[0], month: p[1], day: p[2])) else { return d }
        return Fmt.dayMonth(date)
    }
}

/// Günaydın kartında: civciv uyandı ve kazanılan puan
struct WakeGameMorningSection: View {
    var body: some View {
        let log = WakeGame.load()
        if let today = WakeGame.scored(log).last(where: { $0.e.d == Study.dayString(Date()) }) {
            let level = WakeGame.level(WakeGame.total(log))
            Section {
                HStack(spacing: 12) {
                    WakePet(stage: WakeGame.stage(level), awake: true, size: 72)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("wakegame.awakeTitle")).font(.headline)
                        Text("\(T("wakegame.earned", today.points)) · \(T("wakegame.level", level))").foregroundStyle(Theme.accent)
                    }
                }
            }
        }
    }
}
