import SwiftUI

/*
 * Ders çalışma sayacı (3 Kasım 2026, 2. analiz #84; sahibin ekran görüntüsü): ders seçimi ("Genel" + kullanıcının
 * dersleri), yedi bölütlü sarı LED rakamlarla süre (sönük bölütler görünür), çözülen soru sayacı, oturum notu ve
 * istatistik; "Pomodoro" sekmesi uygulamanın pomodorosunu dersin adıyla açar. Kayıtlar: `study.subjects` (JSON dizi),
 * `study.subject` (seçili; boş = Genel), süren oturum `study.start`/`study.acc` (Unix ms), `study.q`, `study.note`,
 * geçmiş `study.log` (JSON `[{d, s, m, q, n}]`, en çok 1000). Android'deki Study.kt ile aynı.
 */
enum Study {
    static let subjectsKey = "study.subjects"
    static let subjectKey = "study.subject"
    static let logKey = "study.log"
    static let maxLog = 1000

    struct Entry: Codable, Equatable {
        var d: String
        var s: String
        var m: Int
        var q: Int
        var n: String?
    }

    struct Total: Equatable { var s: String; var m: Int; var q: Int }

    static func subjects() -> [String] {
        guard let data = UserDefaults.standard.string(forKey: subjectsKey)?.data(using: .utf8),
              let list = try? JSONDecoder().decode([String].self, from: data) else { return [] }
        return list.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    static func saveSubjects(_ list: [String]) {
        var seen = Set<String>()
        let unique = list.filter { seen.insert($0).inserted }.prefix(30)
        if let data = try? JSONEncoder().encode(Array(unique)) {
            UserDefaults.standard.set(String(decoding: data, as: UTF8.self), forKey: subjectsKey)
        }
    }

    static func load() -> [Entry] {
        guard let data = UserDefaults.standard.string(forKey: logKey)?.data(using: .utf8),
              let list = try? JSONDecoder().decode([Entry].self, from: data) else { return [] }
        return list
    }

    static func save(_ log: [Entry]) {
        if let data = try? JSONEncoder().encode(Array(log.suffix(maxLog))) {
            UserDefaults.standard.set(String(decoding: data, as: UTF8.self), forKey: logKey)
        }
    }

    static func clear() { UserDefaults.standard.removeObject(forKey: logKey) }

    static func name(_ s: String) -> String { s.isEmpty ? L("study.general") : s }

    /// Süren oturumun geçen süresi (ms)
    static func elapsed(start: Double, acc: Double, now: Double) -> Double { acc + (start > 0 ? max(0, now - start) : 0) }

    /// Oturum kaydı: en az 1 dakika ya da en az 1 soru varsa yazılır
    static func record(_ log: [Entry], day: String, subject: String, ms: Double, q: Int, note: String) -> [Entry]? {
        let m = Int((ms + 30_000) / 60_000)
        guard m >= 1 || q >= 1 else { return nil }
        let n = String(note.trimmingCharacters(in: .whitespacesAndNewlines).prefix(200))
        return Array((log + [Entry(d: day, s: subject, m: m, q: q, n: n.isEmpty ? nil : n)]).suffix(maxLog))
    }

    static func dayString(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    private static func from(_ today: Date, days: Int) -> String {
        days > 0 ? dayString(Calendar.current.date(byAdding: .day, value: -(days - 1), to: today) ?? today) : ""
    }

    /// Bugün dahil son `days` gün (0 = bütün kayıtlar)
    static func total(_ log: [Entry], today: Date, days: Int) -> Total {
        let lo = from(today, days: days), hi = dayString(today)
        let sel = log.filter { $0.d >= lo && $0.d <= hi }
        return Total(s: "", m: sel.reduce(0) { $0 + $1.m }, q: sel.reduce(0) { $0 + $1.q })
    }

    /// Derslere göre son `days` gün, en çok çalışılandan
    static func bySubject(_ log: [Entry], today: Date, days: Int = 7) -> [Total] {
        let lo = from(today, days: days), hi = dayString(today)
        let groups = Dictionary(grouping: log.filter { $0.d >= lo && $0.d <= hi }, by: \.s)
        return groups.map { Total(s: $0.key, m: $0.value.reduce(0) { $0 + $1.m }, q: $0.value.reduce(0) { $0 + $1.q }) }
            .sorted { $0.m != $1.m ? $0.m > $1.m : $0.q > $1.q }
    }
}

private let ledOn = Color(hex: 0xFFC83D)
private let ledPanel = Color(hex: 0x14110A)

/// Yedi bölütlü rakam göstergesi: 0–9, ':' ve '-'; sönük bölütler de çizilir
struct SevenSegment: View {
    let text: String
    let height: CGFloat
    var color: Color = ledOn

    private static let digitW: CGFloat = 0.56, colonW: CGFloat = 0.26, gapW: CGFloat = 0.14
    private static let segments: [Character: String] = [
        "0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc",
        "5": "afgcd", "6": "afgedc", "7": "abc", "8": "abcdefg", "9": "abcdfg", "-": "g",
    ]

    private var width: CGFloat {
        let units = text.reduce(CGFloat(0)) { $0 + ($1 == ":" ? Self.colonW : Self.digitW) }
        return height * (units + Self.gapW * CGFloat(max(0, text.count - 1)))
    }

    var body: some View {
        Canvas { ctx, size in
            let h = size.height
            var x: CGFloat = 0
            for ch in text {
                if ch == ":" {
                    let r = h * 0.055
                    for y in [0.32, 0.68] {
                        ctx.fill(Path(ellipseIn: CGRect(x: x + h * Self.colonW / 2 - r, y: h * y - r, width: 2 * r, height: 2 * r)), with: .color(color))
                    }
                    x += h * Self.colonW
                } else {
                    drawDigit(&ctx, ch, x: x, w: h * Self.digitW, h: h)
                    x += h * Self.digitW
                }
                x += h * Self.gapW
            }
        }
        .frame(width: width, height: height)
        .accessibilityElement()
        .accessibilityLabel(text)
    }

    private func drawDigit(_ ctx: inout GraphicsContext, _ ch: Character, x: CGFloat, w: CGFloat, h: CGFloat) {
        let lit = Self.segments[ch] ?? ""
        let t = h * 0.11, g = t * 0.18, mid = h / 2
        func horiz(_ y: CGFloat) -> Path {
            let x0 = x + g, x1 = x + w - g
            var p = Path()
            p.move(to: CGPoint(x: x0, y: y)); p.addLine(to: CGPoint(x: x0 + t / 2, y: y - t / 2))
            p.addLine(to: CGPoint(x: x1 - t / 2, y: y - t / 2)); p.addLine(to: CGPoint(x: x1, y: y))
            p.addLine(to: CGPoint(x: x1 - t / 2, y: y + t / 2)); p.addLine(to: CGPoint(x: x0 + t / 2, y: y + t / 2))
            p.closeSubpath()
            return p
        }
        func vert(_ cx: CGFloat, _ y0: CGFloat, _ y1: CGFloat) -> Path {
            let a = y0 + g, b = y1 - g
            var p = Path()
            p.move(to: CGPoint(x: cx, y: a)); p.addLine(to: CGPoint(x: cx + t / 2, y: a + t / 2))
            p.addLine(to: CGPoint(x: cx + t / 2, y: b - t / 2)); p.addLine(to: CGPoint(x: cx, y: b))
            p.addLine(to: CGPoint(x: cx - t / 2, y: b - t / 2)); p.addLine(to: CGPoint(x: cx - t / 2, y: a + t / 2))
            p.closeSubpath()
            return p
        }
        let top = t / 2, bottom = h - t / 2, left = x + t / 2, right = x + w - t / 2
        let paths: [(Character, Path)] = [
            ("a", horiz(top)), ("g", horiz(mid)), ("d", horiz(bottom)),
            ("f", vert(left, top, mid)), ("b", vert(right, top, mid)),
            ("e", vert(left, mid, bottom)), ("c", vert(right, mid, bottom)),
        ]
        for (k, p) in paths { ctx.fill(p, with: .color(lit.contains(k) ? color : color.opacity(0.10))) }
    }
}

/// LED paneli: koyu zemin üstünde yedi bölütlü yazı ve küçük etiket
struct LedPanel: View {
    let text: String
    let label: String
    let height: CGFloat

    var body: some View {
        VStack(spacing: 8) {
            SevenSegment(text: text, height: height)
                .environment(\.layoutDirection, .leftToRight)
            Text(label).font(.caption.weight(.semibold)).foregroundStyle(ledOn.opacity(0.75)).lineLimit(1)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(ledPanel, in: RoundedRectangle(cornerRadius: 16))
    }
}

struct StudyView: View {
    @AppStorage("study.mode") private var mode = "timer"
    @AppStorage(Study.subjectKey) private var subject = ""
    @AppStorage("pomo.task") private var pomoTask = ""
    @State private var subjects = Study.subjects()
    @State private var adding = false
    @State private var newName = ""
    @State private var removing: String?

    var body: some View {
        VStack(spacing: 10) {
            VStack(spacing: 10) {
                Picker(L("study.title"), selection: $mode) {
                    Text(L("study.modeTimer")).tag("timer")
                    Text(L("study.modePomo")).tag("pomo")
                }
                .pickerStyle(.segmented)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach([""] + subjects, id: \.self) { s in chip(s) }
                        Button { newName = ""; adding = true } label: {
                            Image(systemName: "plus").font(.body.weight(.semibold)).foregroundStyle(Theme.accent)
                                .padding(.horizontal, 14).padding(.vertical, 9)
                                .background(Theme.surface2, in: Capsule())
                        }
                        .accessibilityLabel(L("study.addSubject"))
                    }
                }
                if mode == "pomo" {
                    Text(L("study.pomoNote")).font(.caption).foregroundStyle(Theme.muted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            if mode == "pomo" {
                PomodoroView().id(subject)
            } else {
                StudyTimerView(subject: subject)
            }
        }
        .themedBackground()
        .navigationTitle(L("study.title"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if mode == "pomo" { pomoTask = Study.name(subject) } }
        .onChange(of: mode) { _, m in if m == "pomo" { pomoTask = Study.name(subject) } }
        .alert(L("study.addSubject"), isPresented: $adding) {
            TextField(L("study.subjectName"), text: $newName)
            Button(L("alarm.cancel"), role: .cancel) {}
            Button(L("common.done")) {
                let n = String(newName.trimmingCharacters(in: .whitespaces).prefix(30))
                if !n.isEmpty, n != L("study.general") {
                    subjects.append(n)
                    Study.saveSubjects(subjects)
                    subjects = Study.subjects()
                    select(n)
                }
            }
        }
        .alert(T("study.removeSubject", removing ?? ""), isPresented: Binding(get: { removing != nil }, set: { if !$0 { removing = nil } })) {
            Button(L("alarm.cancel"), role: .cancel) {}
            Button(L("study.remove"), role: .destructive) {
                if let s = removing {
                    subjects.removeAll { $0 == s }
                    Study.saveSubjects(subjects)
                    if subject == s { select("") }
                }
            }
        } message: {
            Text(L("study.removeNote"))
        }
    }

    private func select(_ s: String) {
        subject = s
        if mode == "pomo" { pomoTask = Study.name(s) }
    }

    private func chip(_ s: String) -> some View {
        let on = s == subject
        return Button { select(s) } label: {
            Text(Study.name(s)).font(.subheadline.weight(.semibold))
                .foregroundStyle(on ? Theme.bg : Theme.text)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(on ? Theme.accent : Theme.surface2, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
        .contextMenu {
            if !s.isEmpty {
                Button(L("study.remove"), systemImage: "trash", role: .destructive) { removing = s }
            }
        }
    }
}

private struct StudyTimerView: View {
    let subject: String
    @AppStorage("study.start") private var start = 0.0
    @AppStorage("study.acc") private var acc = 0.0
    @AppStorage("study.q") private var q = 0
    @AppStorage("study.note") private var note = ""
    @State private var log = Study.load()
    @State private var confirmClear = false

    private static func nowMs() -> Double { Date().timeIntervalSince1970 * 1000 }

    private static func led(_ ms: Double) -> String {
        let s = Int(ms / 1000)
        return String(format: "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    let ms = Study.elapsed(start: start, acc: acc, now: context.date.timeIntervalSince1970 * 1000)
                    LedPanel(text: Self.led(ms), label: Study.name(subject), height: 56)
                }
                HStack(spacing: 12) {
                    Button { if q > 0 { q -= 1 } } label: {
                        Image(systemName: "minus").font(.title2.weight(.semibold)).frame(width: 56, height: 56)
                            .background(Theme.surface2, in: Circle()).foregroundStyle(Theme.text)
                    }
                    .accessibilityLabel(L("study.minus"))
                    LedPanel(text: String(format: "%03d", min(q, 999)), label: L("study.questions"), height: 44)
                    Button { q += 1 } label: {
                        Image(systemName: "plus").font(.largeTitle.weight(.semibold)).frame(width: 72, height: 72)
                            .background(Theme.accent, in: Circle()).foregroundStyle(Theme.bg)
                    }
                    .accessibilityLabel(L("study.plus"))
                    .sensoryFeedback(.increase, trigger: q)
                }
                .buttonStyle(.plain)
                HStack(spacing: 10) {
                    let running = start > 0
                    Button {
                        let t = Self.nowMs()
                        if running { acc += t - start; start = 0 } else { start = t }
                    } label: {
                        Text(L(running ? "tools.stopwatch.stop" : acc > 0 ? "tools.stopwatch.resume" : "tools.stopwatch.start"))
                            .font(.headline).frame(maxWidth: .infinity).padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    Button {
                        let ms = Study.elapsed(start: start, acc: acc, now: Self.nowMs())
                        if let l = Study.record(log, day: Study.dayString(Date()), subject: subject, ms: ms, q: q, note: note) {
                            log = l
                            Study.save(l)
                        }
                        start = 0; acc = 0; q = 0; note = ""
                    } label: {
                        Text(L("study.finish")).font(.headline).frame(maxWidth: .infinity).padding(.vertical, 14)
                    }
                    .buttonStyle(.bordered)
                }
                .tint(Theme.accent)
                TextField(L("study.notePlaceholder"), text: $note, axis: .vertical)
                    .lineLimit(2...4)
                    .padding(12)
                    .background(Theme.surface, in: RoundedRectangle(cornerRadius: 12))
                    .onChange(of: note) { _, v in if v.count > 200 { note = String(v.prefix(200)) } }
                stats
                Text(L("study.privacyIOS")).font(.footnote).foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
        }
        .confirmationDialog(L("study.clearConfirm"), isPresented: $confirmClear, titleVisibility: .visible) {
            Button(L("study.clear"), role: .destructive) { Study.clear(); log = [] }
        }
    }

    private var stats: some View {
        let today = Date()
        return VStack(alignment: .leading, spacing: 10) {
            Text(L("study.stats")).font(.headline)
            if log.isEmpty {
                Text(L("study.empty")).foregroundStyle(Theme.muted)
            } else {
                HStack(spacing: 8) {
                    ForEach([("study.today", 1), ("study.week", 7), ("study.total", 0)], id: \.0) { k, d in
                        let t = Study.total(log, today: today, days: d)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(L(k)).font(.caption).foregroundStyle(Theme.muted)
                            Text(T("study.min", t.m)).font(.title3.bold())
                            Text(T("study.qCount", t.q)).font(.caption).foregroundStyle(Theme.muted)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                let subjects = Study.bySubject(log, today: today)
                if !subjects.isEmpty {
                    Text(L("study.bySubject")).font(.subheadline).foregroundStyle(Theme.muted)
                    let top = max(1, subjects.map(\.m).max() ?? 1)
                    ForEach(subjects.prefix(8), id: \.s) { s in
                        VStack(alignment: .leading, spacing: 3) {
                            HStack {
                                Text(Study.name(s.s)).lineLimit(1)
                                Spacer()
                                Text("\(T("study.min", s.m)) · \(T("study.qCount", s.q))").font(.subheadline).foregroundStyle(Theme.muted)
                            }
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule().fill(Theme.surface2)
                                    Capsule().fill(ledOn).frame(width: geo.size.width * CGFloat(s.m) / CGFloat(top))
                                }
                            }
                            .frame(height: 8)
                        }
                    }
                }
                Text(L("study.recent")).font(.subheadline).foregroundStyle(Theme.muted)
                ForEach(Array(log.suffix(5).reversed().enumerated()), id: \.offset) { _, e in
                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text("\(Self.dayLabel(e.d)) · \(Study.name(e.s))").lineLimit(1)
                            Spacer()
                            Text("\(T("study.min", e.m)) · \(T("study.qCount", e.q))").font(.subheadline).foregroundStyle(Theme.muted)
                        }
                        if let n = e.n, !n.isEmpty { Text(n).font(.subheadline).foregroundStyle(Theme.muted).lineLimit(2) }
                    }
                }
                Button(L("study.clear"), role: .destructive) { confirmClear = true }
            }
        }
        .foregroundStyle(Theme.text)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
    }

    /// "2026-11-03" → "3 Kasım"
    private static func dayLabel(_ d: String) -> String {
        let p = d.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3, let date = Calendar.current.date(from: DateComponents(year: p[0], month: p[1], day: p[2])) else { return d }
        return Fmt.dayMonth(date)
    }
}
