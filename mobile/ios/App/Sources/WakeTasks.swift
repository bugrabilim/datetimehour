import SwiftUI
import CoreMotion
import UserNotifications
import VisionKit
import SaatCore

/* Alarm kapatma görevi ve uyanma denetimi (30 Ekim 2026, rakip analizi #13). AlarmKit'in alarm ekranındaki Durdur düğmesi
   her zaman alarmı susturur; görevli alarmda Durdur uygulamayı açar (AlarmStopIntent) ve görev istenir. Görev yapılmazsa
   alarm 1 dakika sonra yeniden çalar (en çok 10 kez). Uyanma denetimi: kapattıktan 5 dk sonra "Uyanık mısınız?" bildirimi;
   2 dk içinde uygulama açılmazsa (bildirime dokunulmazsa) yedek alarm çalar. Kayıtlar yalnız telefonda (wake.*). */

struct PendingWakeTask: Codable, Equatable, Identifiable {
    var id: String { "\(task)-\(rings)-\(repeatID ?? "")" }
    var task: String
    var level: Int
    var code: String
    var wakeCheck: Bool
    var title: String
    var rise: Bool
    var rings: Int
    var repeatID: String?
    /// Alarmın duvar kâğıdı (AlarmWalls; 2. analiz #2); eski kayıtlarda yok
    var wall: String?
    /// 2. analiz #34: asıl görevden sonra sırayla istenecek görevler ("random" görev ekranında seçilir); eski kayıtlarda yok
    var chain: [String]?

    /// Rastgele görevde seçilenler (kayıt ya da sensör gerektirmeyenler)
    static let randomTasks = ["math", "memory", "typing", "shake"]

    /// Sırayla istenecek görevler (kayıtsız kod/fotoğraf matematiğe düşer, rastgele olanlar burada seçilir)
    var steps: [String] {
        let all = [task] + (chain ?? [])
        let kinds = all.filter { $0 != "none" }.map { AlarmItem.effectiveTask($0, code: code) }
        let out = kinds.map { $0 == "random" ? (Self.randomTasks.randomElement() ?? "math") : $0 }
        return out.isEmpty ? ["math"] : out
    }
}

@MainActor
enum WakeTasks {
    static let pendingKey = "wake.pending", checkKey = "wake.check", checkNotification = "wake-check"

    private struct Check: Codable {
        var at: Double
        var backupID: String?
    }

    static var pending: PendingWakeTask? {
        get {
            UserDefaults.standard.data(forKey: pendingKey).flatMap { try? JSONDecoder().decode(PendingWakeTask.self, from: $0) }
        }
        set {
            if let newValue, let d = try? JSONEncoder().encode(newValue) {
                UserDefaults.standard.set(d, forKey: pendingKey)
            } else {
                UserDefaults.standard.removeObject(forKey: pendingKey)
            }
            // Görev bitene kadar uygulama silinemesin (2. analiz #46; Ekran Süresi onayı ve Apple yetkisi gerekir)
            FocusBlock.denyRemoval(newValue.map { $0.task != "none" } ?? false)
        }
    }

    /// Alarm durduruldu (AlarmStopIntent): görev varsa bekleyen görev + 1 dk sonra yeniden çalma; yoksa uyanma denetimi
    static func stopped(_ p: PendingWakeTask) async {
        guard p.task != "none" else {
            if p.wakeCheck { await scheduleCheck(title: p.title) }
            return
        }
        var next = p
        next.repeatID = nil
        if p.rings < WakeTask.repeatMax {
            let id = UUID()
            #if canImport(AlarmKit)
            if #available(iOS 26.0, *) {
                var o = KitAlarms.Options(snooze: 0, remaining: -1, rise: p.rise)
                o.wake = p
                o.wake?.rings = p.rings + 1
                if await KitAlarms.schedule(id: id, at: Date().addingTimeInterval(Double(WakeTask.repeatMin * 60)), title: p.title, options: o) {
                    next.repeatID = id.uuidString
                }
            }
            #endif
        }
        pending = next
    }

    /// Görev yapıldı: yeniden çalma iptal, istenirse uyanma denetimi kurulur
    static func solved() async {
        guard let p = pending else { return }
        if let id = p.repeatID.flatMap(UUID.init(uuidString:)) { AlarmCenter.cancel(id: id) }
        pending = nil
        if p.wakeCheck { await scheduleCheck(title: p.title) }
    }

    /// Uyanma denetimi: 5 dk sonra bildirim, 7 dk sonra yedek alarm (uygulama açılınca iptal)
    static func scheduleCheck(title: String) async {
        let at = Date().addingTimeInterval(Double(WakeTask.checkAfterMin * 60))
        let content = UNMutableNotificationContent()
        content.title = L("wake.checkTitle")
        content.body = L("wake.checkBody")
        content.sound = .default
        content.interruptionLevel = .timeSensitive
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: at.timeIntervalSinceNow, repeats: false)
        try? await UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: checkNotification, content: content, trigger: trigger))
        let backup = UUID()
        await AlarmCenter.schedule(id: backup, at: at.addingTimeInterval(Double(WakeTask.checkGraceMin * 60)), title: L("wake.checkAlarm"))
        if let d = try? JSONEncoder().encode(Check(at: at.timeIntervalSince1970, backupID: backup.uuidString)) {
            UserDefaults.standard.set(d, forKey: checkKey)
        }
    }

    /// Uygulama öne geldi: bekleyen görev varsa true. Uyanma denetiminin vakti geldiyse ve yedek alarma süre varsa
    /// geri sayım halkası açılır (2. analiz #45; "Uyanığım" ile biter, `checkDeadline`); süre geçtiyse kayıt silinir.
    static func onActive(now: Date = Date()) -> Bool {
        if let d = UserDefaults.standard.data(forKey: checkKey), let c = try? JSONDecoder().decode(Check.self, from: d),
           now.timeIntervalSince1970 >= c.at + Double(WakeTask.checkGraceMin * 60) {
            UserDefaults.standard.removeObject(forKey: checkKey)
        }
        return pending != nil
    }

    /// Denetim vakti geldi, yedek alarm henüz çalmadı: halkanın bitiş anı
    static func checkDeadline(now: Date = Date()) -> Date? {
        guard let d = UserDefaults.standard.data(forKey: checkKey), let c = try? JSONDecoder().decode(Check.self, from: d),
              now.timeIntervalSince1970 >= c.at else { return nil }
        let end = Date(timeIntervalSince1970: c.at + Double(WakeTask.checkGraceMin * 60))
        return end > now ? end : nil
    }

    /// "Uyanığım": yedek alarm iptal, bildirim kalkar
    static func awake() {
        if let d = UserDefaults.standard.data(forKey: checkKey), let c = try? JSONDecoder().decode(Check.self, from: d),
           let id = c.backupID.flatMap(UUID.init(uuidString:)) {
            AlarmCenter.cancel(id: id)
        }
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [checkNotification])
        UserDefaults.standard.removeObject(forKey: checkKey)
    }
}

/// Uyanma denetimi ekranı (2. analiz #45): yedek alarma kalan süre halka olarak azalır; "Uyanığım" denetimi bitirir.
struct WakeCheckView: View {
    let deadline: Date
    let onAwake: () -> Void
    let onTimeout: () -> Void

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.25)) { ctx in
            let left = max(0, deadline.timeIntervalSince(ctx.date))
            let total = Double(WakeTask.checkGraceMin * 60)
            VStack(spacing: 24) {
                Spacer()
                Text(L("wake.checkTitle")).font(.largeTitle.bold()).foregroundStyle(Theme.text).multilineTextAlignment(.center)
                ZStack {
                    Circle().stroke(Theme.surface2, lineWidth: 16)
                    Circle().trim(from: 0, to: min(1, left / total))
                        .stroke(Theme.accent, style: StrokeStyle(lineWidth: 16, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                    Text(verbatim: String(format: "%d:%02d", Int(left) / 60, Int(left) % 60))
                        .font(.system(size: 44, weight: .bold, design: .rounded)).monospacedDigit()
                        .environment(\.layoutDirection, .leftToRight)
                        .foregroundStyle(Theme.ink)
                }
                .frame(width: 220, height: 220)
                .accessibilityElement(children: .combine)
                Text(L("wake.checkRing")).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                RoundButton(title: L("wake.awake"), prominent: true) { onAwake() }.frame(maxWidth: 360)
                Spacer()
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(AppBackdrop().ignoresSafeArea())
            .onChange(of: left == 0) { _, done in if done { onTimeout() } }
        }
    }
}

/// Görev ekranı: çözülmeden kapanmaz. Sensör ya da kamera yoksa "Başka görev" zor matematiğe geçer.
struct WakeTaskView: View {
    let task: PendingWakeTask
    /// Önizleme (2. analiz #35): uyanma kaydı, rozet ve el feneri yok
    var preview = false
    /// Verilirse bu görevler (ertele görevi gibi); yoksa alarmın zinciri
    var only: [String]? = nil
    let onDone: () -> Void
    @State private var steps: [String] = []
    @State private var step = 0
    @State private var kind = ""
    @State private var problems: [WakeTask.Problem] = []
    @State private var index = 0
    @State private var answer = ""
    @State private var wrong = false
    @State private var count = 0
    @State private var scanning = false
    @State private var codeWrong = false
    @State private var motion = MotionReader()
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: 22) {
            // El feneri (2. analiz #41)
            if !preview { TorchButton() }
            Spacer(minLength: 12)
            Image(systemName: "alarm.waves.left.and.right").font(.system(size: 52)).foregroundStyle(Theme.accent)
            Text(L("wake.title")).font(.title2.weight(.semibold)).foregroundStyle(Theme.text).multilineTextAlignment(.center)
            if steps.count > 1 { Text(T("wake.step", step + 1, steps.count)).font(.subheadline).foregroundStyle(Theme.muted) }
            Group {
                switch kind {
                case "math": math
                case "shake", "steps", "squat": moving
                case "object": ObjectTaskView(code: task.code) { finish() }
                case "speak": SpeakTaskView(onSolved: { finish() }, onUnavailable: { kind = "typing" })
                case "code": code
                case "memory": MemoryTaskView(level: task.level) { finish() }
                case "typing": TypingTaskView { finish() }
                case "photo": PhotoTaskView(code: task.code) { finish() }
                default: EmptyView()
                }
            }
            .id("\(step)-\(kind)")
            Spacer()
            if kind != "math" {
                Button(L("wake.other")) { useMath() }.font(.subheadline).tint(Theme.muted)
            }
        }
        .padding(24)
        .frame(maxWidth: 520)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // Duvar kâğıdı seçiliyse zemin o, yazılar koyu temanın açık renkleri (2. analiz #2)
        .background {
            if let w = task.wall, !w.isEmpty { AlarmWall(id: w).ignoresSafeArea() } else { AppBackdrop().ignoresSafeArea() }
        }
        .environment(\.colorScheme, (task.wall ?? "").isEmpty ? colorScheme : .dark)
        .interactiveDismissDisabled()
        .onAppear {
            steps = only ?? task.steps
            begin()
        }
        .onDisappear { motion.stop() }
        .sheet(isPresented: $scanning) {
            CodeScannerView { value in
                scanning = false
                if value == task.code { finish() } else { codeWrong = true }
            }
            .ignoresSafeArea()
        }
    }

    private var math: some View {
        VStack(spacing: 14) {
            Text(T("wake.math", index + 1, problems.count)).foregroundStyle(Theme.muted)
            if index < problems.count {
                Text(verbatim: problems[index].text + " = ?")
                    .font(.system(size: 40, weight: .bold, design: .rounded)).monospacedDigit()
                    .environment(\.layoutDirection, .leftToRight)
                    .foregroundStyle(Theme.text)
            }
            TextField(L("wake.answer"), text: $answer)
                .keyboardType(.numberPad)
                .font(.title2.monospacedDigit())
                .multilineTextAlignment(.center)
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 220)
            if wrong { Text(L("wake.wrong")).foregroundStyle(Theme.err).font(.subheadline) }
            RoundButton(title: L("wake.check"), prominent: true) { check() }
        }
    }

    private var moving: some View {
        let total = Self.total(kind)
        let key = kind == "shake" ? "wake.shake" : (kind == "squat" ? "wake.squat" : "wake.steps")
        return VStack(spacing: 14) {
            Text(T(key, total)).font(.headline).foregroundStyle(Theme.text).multilineTextAlignment(.center)
            Text(T("wake.progress", min(count, total), total))
                .font(.system(size: 48, weight: .bold, design: .rounded)).monospacedDigit()
                .environment(\.layoutDirection, .leftToRight)
                .foregroundStyle(Theme.accent)
            ProgressView(value: Double(min(count, total)), total: Double(total)).tint(Theme.accent).frame(maxWidth: 280)
            if !motion.available { Text(L("wake.noSensor")).font(.subheadline).foregroundStyle(Theme.muted).multilineTextAlignment(.center) }
        }
    }

    private var code: some View {
        VStack(spacing: 14) {
            Text(L("wake.code")).font(.headline).foregroundStyle(Theme.text)
            if CodeScannerView.available {
                RoundButton(title: L("wake.code.scan"), prominent: true) { codeWrong = false; scanning = true }
            } else {
                Text(L("wake.noCamera")).font(.subheadline).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
            }
            if codeWrong { Text(L("wake.code.wrong")).foregroundStyle(Theme.err).font(.subheadline) }
        }
    }

    private func begin() {
        kind = steps.indices.contains(step) ? steps[step] : "math"
        count = 0
        if kind == "math" { useMath(level: task.level) }
        startMotion()
    }

    private func useMath(level: Int = 3) {
        motion.stop()
        var rng = SystemRandomNumberGenerator()
        problems = (0..<WakeTask.mathCount).map { _ in WakeTask.problem(level: level, using: &rng) }
        index = 0
        answer = ""
        kind = "math"
    }

    private func check() {
        guard index < problems.count else { return }
        if Int(answer.trimmingCharacters(in: .whitespaces)) == problems[index].answer {
            wrong = false
            answer = ""
            index += 1
            if index >= problems.count { finish() }
        } else {
            wrong = true
            answer = ""
        }
    }

    private func startMotion() {
        guard kind == "shake" || kind == "steps" || kind == "squat" else { return }
        let total = Self.total(kind)
        let counter: MotionCounter = kind == "shake" ? .shake() : (kind == "squat" ? .squat() : .steps())
        motion.start(counter: counter) { n in
            count = n
            if n >= total { finish() }
        }
    }

    private static func total(_ kind: String) -> Int {
        switch kind {
        case "shake": return WakeTask.shakes
        case "squat": return WakeTask.squats
        default: return WakeTask.steps
        }
    }

    private func finish() {
        motion.stop()
        // Zincir (2. analiz #34): sıradaki görev
        if step + 1 < steps.count {
            step += 1
            begin()
            return
        }
        if preview { onDone(); return }
        Torch.set(false)
        SleepLogStore.recordWake()
        WakeGame.record(wake: Date(), alarm: nil, task: true) // uyanma oyunu (2. analiz #7)
        BadgeStore.shared.hit(.uyanik)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        onDone()
    }
}

/// Hafıza (#75): 3×3 ızgarada kareler sırayla yanar, aynı sırayla dokunulur; yanlışta yeni dizi
private struct MemoryTaskView: View {
    let level: Int
    let onSolved: () -> Void
    @State private var round = 0
    @State private var seq: [Int] = []
    @State private var lit = -1
    @State private var showing = true
    @State private var pos = 0
    @State private var wrong = false

    var body: some View {
        VStack(spacing: 14) {
            Text(L(showing ? "wake.memory.watch" : "wake.memory.repeat")).font(.headline).foregroundStyle(Theme.text)
            Grid(horizontalSpacing: 8, verticalSpacing: 8) {
                ForEach(0..<3, id: \.self) { r in
                    GridRow {
                        ForEach(0..<3, id: \.self) { c in
                            let i = r * 3 + c
                            RoundedRectangle(cornerRadius: 14).fill(lit == i ? Theme.accent : Theme.surface2)
                                .frame(width: 72, height: 72)
                                .onTapGesture { tap(i) }
                        }
                    }
                }
            }
            .environment(\.layoutDirection, .leftToRight)
            Text(T("wake.progress", pos, seq.count)).font(.subheadline).foregroundStyle(Theme.muted)
            if wrong { Text(L("wake.memory.wrong")).font(.subheadline).foregroundStyle(Theme.err) }
        }
        .task(id: round) {
            var rng = SystemRandomNumberGenerator()
            seq = WakeTask.memorySequence(level: level, using: &rng)
            pos = 0; showing = true
            try? await Task.sleep(for: .milliseconds(600))
            for c in seq {
                lit = c
                try? await Task.sleep(for: .milliseconds(650))
                lit = -1
                try? await Task.sleep(for: .milliseconds(250))
            }
            showing = false
        }
    }

    private func tap(_ i: Int) {
        guard !showing, pos < seq.count else { return }
        if seq[pos] == i {
            lit = i; pos += 1
            if pos >= seq.count { onSolved() }
        } else {
            wrong = true; round += 1
        }
    }
}

/// Yazma (#75): günün sözlerinden rastgele biri aynen yazılır
private struct TypingTaskView: View {
    let onSolved: () -> Void
    @State private var phrase = Sayings.random() ?? "Saat Tarih"
    @State private var input = ""
    var body: some View {
        VStack(spacing: 14) {
            Text(L("wake.typing")).font(.headline).foregroundStyle(Theme.text)
            Text(phrase).font(.title3.weight(.bold)).foregroundStyle(Theme.ink).multilineTextAlignment(.center)
            TextField(L("wake.typing.field"), text: $input, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .autocorrectionDisabled()
                .onChange(of: input) { if WakeTask.typed(phrase, input) { onSolved() } }
        }
    }
}

/// Fotoğraf (#75): kayıtlı yerin fotoğrafı çekilir, 9×8 gri özet (dHash) kayıtlıyla karşılaştırılır. Fotoğraf saklanmaz.
enum PhotoHash {
    static let prefix = "ph:"
    static func of(_ image: UIImage) -> UInt64? {
        guard let cg = image.cgImage else { return nil }
        var px = [UInt8](repeating: 0, count: 72)
        guard let ctx = CGContext(data: &px, width: 9, height: 8, bitsPerComponent: 8, bytesPerRow: 9,
                                  space: CGColorSpaceCreateDeviceGray(), bitmapInfo: CGImageAlphaInfo.none.rawValue) else { return nil }
        ctx.interpolationQuality = .medium
        ctx.draw(cg, in: CGRect(x: 0, y: 0, width: 9, height: 8))
        return WakeTask.dHash(px.map(Int.init))
    }
    static func encode(_ h: UInt64) -> String { prefix + String(h, radix: 16) }
    static func decode(_ code: String) -> UInt64? { code.hasPrefix(prefix) ? UInt64(code.dropFirst(prefix.count), radix: 16) : nil }
}

/// Kamera ile tek fotoğraf (kamera izni NSCameraUsageDescription; fotoğraf yalnız özet için kullanılır)
struct CameraShot: UIViewControllerRepresentable {
    let onImage: (UIImage?) -> Void
    static var available: Bool { UIImagePickerController.isSourceTypeAvailable(.camera) }
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let p = UIImagePickerController()
        p.sourceType = .camera
        p.delegate = context.coordinator
        return p
    }
    func updateUIViewController(_ vc: UIImagePickerController, context: Context) {}
    func makeCoordinator() -> Coordinator { Coordinator(onImage: onImage) }
    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onImage: (UIImage?) -> Void
        init(onImage: @escaping (UIImage?) -> Void) { self.onImage = onImage }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            onImage(info[.originalImage] as? UIImage)
        }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) { onImage(nil) }
    }
}

private struct PhotoTaskView: View {
    let code: String
    let onSolved: () -> Void
    @State private var shooting = false
    @State private var wrong = false
    var body: some View {
        VStack(spacing: 14) {
            Text(L("wake.photo")).font(.headline).foregroundStyle(Theme.text).multilineTextAlignment(.center)
            if CameraShot.available {
                RoundButton(title: L("wake.photo.take"), prominent: true) { wrong = false; shooting = true }
            } else {
                Text(L("wake.noCamera")).font(.subheadline).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
            }
            if wrong { Text(L("wake.photo.wrong")).font(.subheadline).foregroundStyle(Theme.err).multilineTextAlignment(.center) }
        }
        .fullScreenCover(isPresented: $shooting) {
            CameraShot { img in
                shooting = false
                guard let img, let h = PhotoHash.of(img), let target = PhotoHash.decode(code) else { return }
                if WakeTask.samePhoto(h, target) { onSolved() } else { wrong = true }
            }
            .ignoresSafeArea()
        }
    }
}

/// İvmeölçer (izin istemez): 50 Hz, sayaç SaatCore.MotionCounter
@MainActor
final class MotionReader {
    private let manager = CMMotionManager()
    private var counter = MotionCounter.steps()
    private var onCount: ((Int) -> Void)?
    var available: Bool { manager.isAccelerometerAvailable }

    func start(counter: MotionCounter, onCount: @escaping (Int) -> Void) {
        guard manager.isAccelerometerAvailable else { return }
        self.counter = counter
        self.onCount = onCount
        manager.accelerometerUpdateInterval = 1.0 / 50
        manager.startAccelerometerUpdates(to: .main) { [weak self] data, _ in
            guard let a = data?.acceleration, let t = data?.timestamp else { return }
            MainActor.assumeIsolated { self?.feed(t, a.x, a.y, a.z) }
        }
    }

    private func feed(_ t: TimeInterval, _ x: Double, _ y: Double, _ z: Double) {
        if counter.feed(t: t, x: x, y: y, z: z) { onCount?(counter.count) }
    }

    func stop() {
        manager.stopAccelerometerUpdates()
        onCount = nil
    }
}

/// QR kod ve barkod okuyucu (VisionKit; kamera izni ilk kullanımda sorulur, görüntü telefonda işlenir)
struct CodeScannerView: UIViewControllerRepresentable {
    let onCode: (String) -> Void

    @MainActor static var available: Bool { DataScannerViewController.isSupported && DataScannerViewController.isAvailable }

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let vc = DataScannerViewController(recognizedDataTypes: [.barcode()], qualityLevel: .balanced,
                                           isHighFrameRateTrackingEnabled: false, isHighlightingEnabled: true)
        vc.delegate = context.coordinator
        try? vc.startScanning()
        return vc
    }

    func updateUIViewController(_ vc: DataScannerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onCode: onCode) }

    @MainActor final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        let onCode: (String) -> Void
        private var done = false
        init(onCode: @escaping (String) -> Void) { self.onCode = onCode }

        func dataScanner(_ scanner: DataScannerViewController, didAdd items: [RecognizedItem], allItems: [RecognizedItem]) {
            guard !done else { return }
            for item in items {
                if case let .barcode(b) = item, let value = b.payloadStringValue, !value.isEmpty {
                    done = true
                    scanner.stopScanning()
                    onCode(value)
                    return
                }
            }
        }
    }
}
