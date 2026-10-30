import AVFoundation
import SwiftUI
import UniformTypeIdentifiers
import SaatCore

/// CSV dosyası paylaşımı (rakip analizi #28: kronometre turları, pomodoro geçmişi). Excel Türkçe karakterleri doğru açsın
/// diye UTF-8 BOM ile; dosya paylaşılırken geçici klasörde üretilir.
struct CSVFile: Transferable {
    let name: String
    let rows: [[String]]

    var text: String {
        rows.map { $0.map(Self.escape).joined(separator: ",") }.joined(separator: "\r\n") + "\r\n"
    }

    static func escape(_ v: String) -> String {
        v.contains(where: { ",\"\n\r;".contains($0) }) ? "\"" + v.replacingOccurrences(of: "\"", with: "\"\"") + "\"" : v
    }

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(exportedContentType: .commaSeparatedText) { f in
            let url = FileManager.default.temporaryDirectory.appendingPathComponent(f.name)
            try Data(("\u{FEFF}" + f.text).utf8).write(to: url, options: .atomic)
            return SentTransferredFile(url)
        }
    }
}

/* Pomodoro istatistikleri ve arka plan sesi (30 Ekim 2026, rakip analizi #16; sitedeki pomodoroyla aynı tasarım) ve
   uyku sesleri (#17) için ortak ses çalar. Kayıt "pomo.log" (JSON, sitedeki sth-pomo-log biçimi), yalnız telefonda. */

enum PomoLog {
    static let key = "pomo.log"

    static func load() -> [FocusLog.Entry] {
        guard let s = UserDefaults.standard.string(forKey: key), let d = s.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([FocusLog.Entry].self, from: d)) ?? []
    }

    static func save(_ log: [FocusLog.Entry]) {
        if let d = try? JSONEncoder().encode(log) { UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: key) }
    }

    /// Tamamlanan çalışma turu (çalışmanın bittiği yerel güne yazılır); etiket "none" ise yazılmaz
    static func record(minutes: Int, task: String, tag: String = "none", at: Date) {
        save(FocusLog.add(load(), .init(d: BadgeStore.day(at), m: minutes, t: task, g: tag == "none" ? nil : tag)))
    }

    /// Etiketler (sitedeki pomodoroyla aynı)
    static let tags = ["none", "work", "study", "personal", "other"]

    static func csv(_ log: [FocusLog.Entry]) -> CSVFile {
        CSVFile(name: "pomodoro-\(BadgeStore.day(Date())).csv",
                rows: [["date", "minutes", "task", "tag"]] + log.map { [$0.d, String($0.m), $0.t, $0.g ?? ""] })
    }

    /// Son 7 günün anahtarları, eskiden yeniye
    static func lastDays(_ now: Date = Date()) -> [String] {
        (0..<7).reversed().map { BadgeStore.day(Calendar.current.date(byAdding: .day, value: -$0, to: now) ?? now) }
    }
}

/// Gürültü türünün adı
func noiseName(_ kind: String) -> String {
    switch kind {
    case "white": L("tools.pomodoro.noiseWhite")
    case "pink": L("tools.pomodoro.noisePink")
    case "brown": L("tools.pomodoro.noiseBrown")
    case "rain": L("tools.pomodoro.noiseRain")
    case "waves": L("noise.waves")
    default: L("tools.pomodoro.noiseOff")
    }
}

/// Telefonda üretilen gürültüyü döngüde çalar (dosya yok). Ekran kilitliyken de sürer (arka plan ses modu, Info.plist);
/// `until` gelince durur, uyku seslerinde son 30 sn'de kısılarak. Aynı anda tek kaynak: pomodoro ya da uyku.
@MainActor
final class NoisePlayer {
    static let shared = NoisePlayer()
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private var buffers: [String: AVAudioPCMBuffer] = [:]
    private var timer: Timer?
    private(set) var kind = ""
    private(set) var source = ""
    private(set) var until: Date?
    private var fade = false
    private var gain: Float = 0.4
    private var observers: [NSObjectProtocol] = []

    var playing: Bool { !kind.isEmpty }

    private init() {
        engine.attach(player)
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { note in
            let ended = (note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt) == AVAudioSession.InterruptionType.ended.rawValue
            if ended { MainActor.assumeIsolated { NoisePlayer.shared.restart() } }
        })
        observers.append(center.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: .main) { _ in
            MainActor.assumeIsolated { NoisePlayer.shared.restart() }
        })
    }

    /// Çalar ya da çalanı günceller (aynı tür yeniden başlamaz)
    func play(kind: String, volume: Int, until: Date?, source: String, fadeOut: Bool = false) {
        guard NoiseGen.kinds.contains(kind) else { stop(); return }
        if let until, until <= Date() { stop(source: source); return }
        self.until = until
        self.source = source
        self.fade = fadeOut
        gain = NoiseGen.gain(volume)
        if self.kind == kind, engine.isRunning {
            applyVolume()
            return
        }
        self.kind = kind
        startEngine()
        if timer == nil {
            let t = Timer(timeInterval: 0.5, repeats: true) { _ in MainActor.assumeIsolated { NoisePlayer.shared.tick() } }
            RunLoop.main.add(t, forMode: .common)
            timer = t
        }
    }

    func setVolume(_ volume: Int) {
        gain = NoiseGen.gain(volume)
        applyVolume()
    }

    /// Durdurur; kaynak verilirse yalnız o kaynak çalıyorsa
    func stop(source: String? = nil) {
        if let source, source != self.source { return }
        timer?.invalidate()
        timer = nil
        kind = ""
        self.source = ""
        until = nil
        player.stop()
        engine.stop()
        // Aralık antrenmanının duyuruları sürüyorsa ses oturumu açık kalır
        let hiit = UserDefaults.standard.double(forKey: "hiit.start") > 0 && UserDefaults.standard.double(forKey: "hiit.paused") == 0
        if !hiit { try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation) }
    }

    private func tick() {
        guard let until else { return }
        if Date() >= until { stop(); return }
        applyVolume()
    }

    private func applyVolume() {
        var v = gain
        if fade, let until {
            let left = until.timeIntervalSinceNow
            if left < 30 { v *= Float(max(0, left) / 30) }
        }
        player.volume = v
    }

    private func buffer(_ kind: String) -> AVAudioPCMBuffer? {
        if let b = buffers[kind] { return b }
        let samples = NoiseGen.samples(kind)
        guard let format = AVAudioFormat(standardFormatWithSampleRate: Double(NoiseGen.rate), channels: 1),
              let b = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)),
              let data = b.floatChannelData else { return nil }
        b.frameLength = b.frameCapacity
        samples.withUnsafeBufferPointer { data[0].update(from: $0.baseAddress!, count: samples.count) }
        buffers = [kind: b] // bellekte tek arabellek
        return b
    }

    private func startEngine() {
        guard let b = buffer(kind) else { return }
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
        player.stop()
        engine.stop()
        engine.connect(player, to: engine.mainMixerNode, format: b.format)
        player.scheduleBuffer(b, at: nil, options: .loops)
        applyVolume()
        engine.prepare()
        do {
            try engine.start()
            player.play()
        } catch {
            kind = ""
        }
    }

    private func restart() {
        guard playing else { return }
        startEngine()
    }
}

/// Pomodoro gürültüsü: çalışma süresi işlerken ve tür seçiliyse çalar, süre bitince durur
enum PomoNoise {
    @MainActor
    static func sync() {
        let d = UserDefaults.standard
        let end = d.double(forKey: "pomo.end")
        let kind = d.string(forKey: "pomo.noise") ?? "off"
        let phase = d.string(forKey: "pomo.phase") ?? "work"
        let endDate = Date(timeIntervalSinceReferenceDate: end)
        if end > 0, endDate > Date(), phase == "work", kind != "off" {
            let vol = d.object(forKey: "pomo.vol") == nil ? 40 : d.integer(forKey: "pomo.vol")
            NoisePlayer.shared.play(kind: kind, volume: vol, until: endDate, source: "pomo")
        } else {
            NoisePlayer.shared.stop(source: "pomo")
        }
    }
}

/// Gürültü türü seçici ve ses düzeyi (pomodoro ve uyku sesleri)
struct NoiseControls: View {
    @Binding var kind: String
    @Binding var volume: Int
    var allowOff = true

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach((allowOff ? ["off"] : []) + NoiseGen.kinds, id: \.self) { k in
                        Button { kind = k } label: {
                            Text(noiseName(k))
                                .font(.subheadline.weight(kind == k ? .semibold : .regular))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .background(kind == k ? Theme.accent.opacity(0.22) : Theme.surface2, in: Capsule())
                                .foregroundStyle(kind == k ? Theme.accent : Theme.text)
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(kind == k ? .isSelected : [])
                    }
                }
            }
            HStack {
                Image(systemName: "speaker.wave.1").foregroundStyle(Theme.muted)
                Slider(value: Binding(get: { Double(volume) }, set: { volume = Int($0.rounded()) }), in: 0...100)
                    .tint(Theme.accent)
                    .accessibilityLabel(L("tools.pomodoro.volume"))
                Image(systemName: "speaker.wave.3").foregroundStyle(Theme.muted)
            }
        }
    }
}

/// Pomodoro istatistikleri: bugün, son 7 gün, toplam; 7 günlük çubuk grafik; en çok çalışılan 5 görev; geçmişi sil
struct FocusStatsCard: View {
    let log: [FocusLog.Entry]
    let onClear: () -> Void
    @State private var confirm = false

    var body: some View {
        let days = PomoLog.lastDays()
        let s = FocusLog.stats(log, days: days, noTask: L("tools.pomodoro.noTask"))
        let streak = FocusLog.streak(log, today: CalendarDay(Date()))
        let tags = FocusLog.tags(log, since: days[0])
        VStack(alignment: .leading, spacing: 12) {
            SectionTitle(text: L("tools.pomodoro.statsHeading"))
            // Odak serisi (rakip analizi #27): bugün ya da dün biten üst üste gün
            if streak >= 2 {
                Label(T("tools.pomodoro.streak", streak), systemImage: "flame").font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accent)
            }
            if log.isEmpty {
                Text(L("tools.pomodoro.empty")).font(.subheadline).foregroundStyle(Theme.muted)
            } else {
                stat(L("tools.pomodoro.today"), s.today)
                stat(L("tools.pomodoro.week"), s.week)
                stat(L("tools.pomodoro.total"), s.total)
                Text(L("tools.pomodoro.chartCaption")).font(.caption).foregroundStyle(Theme.muted)
                bars(days, s.perDay)
                if !s.top.isEmpty {
                    Text(L("tools.pomodoro.topTasks")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
                    ForEach(Array(s.top.enumerated()), id: \.offset) { _, t in
                        HStack {
                            Text(t.name).lineLimit(1)
                            Spacer()
                            Text(verbatim: "\(t.minutes) \(L("tools.pomodoro.minUnit"))").monospacedDigit().foregroundStyle(Theme.muted)
                        }
                        .font(.subheadline)
                    }
                }
                // Etiketlere göre son 7 gün (yalnız etiket kullanıldıysa)
                if tags.count > 1 || tags.first?.tag != "none", !tags.isEmpty {
                    Text(L("tools.pomodoro.byTag")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
                    ForEach(Array(tags.enumerated()), id: \.offset) { _, t in
                        HStack {
                            Text(L("tools.pomodoro.tags.\(t.tag)"))
                            Spacer()
                            Text(verbatim: "\(t.minutes) \(L("tools.pomodoro.minUnit"))").monospacedDigit().foregroundStyle(Theme.muted)
                        }
                        .font(.subheadline)
                    }
                }
                HStack {
                    ShareLink(item: PomoLog.csv(log), preview: SharePreview("pomodoro.csv")) {
                        Label(L("csv.pomo"), systemImage: "square.and.arrow.up")
                    }
                    Spacer()
                    Button(L("tools.pomodoro.clear"), role: .destructive) { confirm = true }
                }
                .font(.subheadline)
            }
        }
        .card()
        .confirmationDialog(L("tools.pomodoro.clearConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("tools.pomodoro.clear"), role: .destructive, action: onClear)
        }
    }

    private func stat(_ title: String, _ sum: FocusLog.Sum) -> some View {
        HStack {
            Text(title).foregroundStyle(Theme.muted)
            Spacer()
            Text(T("tools.pomodoro.statLine", sum.n, sum.m)).monospacedDigit().foregroundStyle(Theme.text)
        }
        .font(.subheadline)
    }

    private func bars(_ days: [String], _ minutes: [Int]) -> some View {
        let top = max(1, minutes.max() ?? 1)
        return HStack(alignment: .bottom, spacing: 8) {
            ForEach(0..<7, id: \.self) { i in
                VStack(spacing: 4) {
                    Spacer(minLength: 0)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(i == 6 ? Theme.accent : Theme.accent.opacity(0.55))
                        .frame(height: max(3, 90 * CGFloat(minutes[i]) / CGFloat(top)))
                    Text(weekday(days[i])).font(.caption2).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(weekday(days[i])): \(minutes[i]) \(L("tools.pomodoro.minUnit"))")
            }
        }
        .frame(height: 112)
    }

    /// "2026-10-30" → "Cum"
    private func weekday(_ key: String) -> String {
        let p = key.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3, let d = Calendar(identifier: .gregorian).date(from: DateComponents(year: p[0], month: p[1], day: p[2])) else { return key }
        let w = Calendar(identifier: .gregorian).component(.weekday, from: d) // 1 = pazar
        return L("calendar.weekdaysShort.\((w + 5) % 7)")
    }
}

/// Pomodoro yapılacaklar listesi (rakip analizi #16'nın kalanı): görev ekle, şu anki görev yap (dokununca), bitti
/// işaretle, sil; her tamamlanan çalışma turu şu anki görevin sayacını artırır. Kayıt "pomo.todo" (JSON [{id, t, d, n}],
/// en çok 50), yalnız telefonda; Android'deki PomoTodo ile aynı biçim.
struct PomoTodoItem: Codable, Identifiable, Equatable {
    var id = UUID().uuidString
    var t: String
    var d = false
    var n = 0
}

enum PomoTodo {
    static let key = "pomo.todo", max = 50

    static func load() -> [PomoTodoItem] {
        guard let s = UserDefaults.standard.string(forKey: key), let d = s.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([PomoTodoItem].self, from: d)) ?? []
    }

    static func save(_ items: [PomoTodoItem]) {
        if let d = try? JSONEncoder().encode(items) { UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: key) }
    }

    static func add(_ items: [PomoTodoItem], _ title: String) -> [PomoTodoItem] {
        let t = String(title.trimmingCharacters(in: .whitespacesAndNewlines).prefix(60))
        guard !t.isEmpty, !items.contains(where: { $0.t == t && !$0.d }) else { return items }
        return Array((items + [PomoTodoItem(t: t)]).suffix(max))
    }

    /// Tamamlanan çalışma turu: adı şu anki görevle aynı olan (bitmemiş) görevin sayacı artar
    static func credit(_ items: [PomoTodoItem], task: String) -> [PomoTodoItem] {
        var items = items
        if let i = items.firstIndex(where: { $0.t == task.trimmingCharacters(in: .whitespaces) && !$0.d }) { items[i].n += 1 }
        return items
    }
}

/// Yapılacaklar kartı: satıra dokununca şu anki görev olur
struct PomoTodoCard: View {
    @Binding var current: String
    @State private var items = PomoTodo.load()
    @State private var input = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(text: L("pomo.todo"))
            HStack {
                TextField(L("pomo.todoPlaceholder"), text: $input)
                    .textFieldStyle(.roundedBorder)
                    .submitLabel(.done)
                    .onSubmit(add)
                Button(L("pomo.todoAdd"), action: add).disabled(input.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            if items.isEmpty {
                Text(L("pomo.todoEmpty")).font(.footnote).foregroundStyle(Theme.muted)
            }
            ForEach(items) { it in
                let selected = it.t == current.trimmingCharacters(in: .whitespaces) && !it.d
                HStack(spacing: 10) {
                    Button { update(items.map { $0.id == it.id ? PomoTodoItem(id: $0.id, t: $0.t, d: !$0.d, n: $0.n) : $0 }) } label: {
                        Image(systemName: it.d ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(it.d ? Theme.accent : Theme.muted)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(it.t)
                    .accessibilityAddTraits(it.d ? .isSelected : [])
                    Text(it.t)
                        .strikethrough(it.d)
                        .fontWeight(selected ? .semibold : .regular)
                        .foregroundStyle(it.d ? Theme.muted : Theme.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(Rectangle())
                        .onTapGesture { if !it.d { current = it.t } }
                    if it.n > 0 {
                        Text(T("pomo.todoPomos", it.n)).font(.caption).foregroundStyle(Theme.muted)
                    }
                    Button { update(items.filter { $0.id != it.id }) } label: {
                        Image(systemName: "xmark").foregroundStyle(Theme.muted)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(L("tools.alarm.delete"))
                }
                .padding(.vertical, 6)
                .padding(.horizontal, 8)
                .background(selected ? Theme.accent.opacity(0.12) : Color.clear, in: RoundedRectangle(cornerRadius: 10))
            }
            if items.contains(where: \.d) {
                Button(L("pomo.todoClear")) { update(items.filter { !$0.d }) }.font(.subheadline)
            }
        }
        .card()
        .onAppear { items = PomoTodo.load() }
    }

    private func add() {
        update(PomoTodo.add(items, input))
        input = ""
    }

    private func update(_ l: [PomoTodoItem]) {
        items = l
        PomoTodo.save(l)
    }
}
