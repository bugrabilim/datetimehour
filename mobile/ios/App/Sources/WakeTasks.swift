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

    /// Uygulama öne geldi: denetim vakti geldiyse uyanık sayılır (yedek alarm iptal). Bekleyen görev varsa true.
    static func onActive(now: Date = Date()) -> Bool {
        if let d = UserDefaults.standard.data(forKey: checkKey), let c = try? JSONDecoder().decode(Check.self, from: d),
           now.timeIntervalSince1970 >= c.at {
            if let id = c.backupID.flatMap(UUID.init(uuidString:)) { AlarmCenter.cancel(id: id) }
            UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [checkNotification])
            UserDefaults.standard.removeObject(forKey: checkKey)
        }
        return pending != nil
    }
}

/// Görev ekranı: çözülmeden kapanmaz. Sensör ya da kamera yoksa "Başka görev" zor matematiğe geçer.
struct WakeTaskView: View {
    let task: PendingWakeTask
    let onDone: () -> Void
    @State private var kind = ""
    @State private var problems: [WakeTask.Problem] = []
    @State private var index = 0
    @State private var answer = ""
    @State private var wrong = false
    @State private var count = 0
    @State private var scanning = false
    @State private var codeWrong = false
    @State private var motion = MotionReader()

    var body: some View {
        VStack(spacing: 22) {
            Spacer(minLength: 12)
            Image(systemName: "alarm.waves.left.and.right").font(.system(size: 52)).foregroundStyle(Theme.accent)
            Text(L("wake.title")).font(.title2.weight(.semibold)).foregroundStyle(Theme.text).multilineTextAlignment(.center)
            switch kind {
            case "math": math
            case "shake", "steps": moving
            case "code": code
            default: EmptyView()
            }
            Spacer()
            if kind != "math" {
                Button(L("wake.other")) { useMath() }.font(.subheadline).tint(Theme.muted)
            }
        }
        .padding(24)
        .frame(maxWidth: 520)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Theme.bg.ignoresSafeArea())
        .interactiveDismissDisabled()
        .onAppear {
            kind = task.task == "code" && task.code.isEmpty ? "math" : task.task
            if kind == "math" { useMath(level: task.level) }
            startMotion()
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
            if wrong { Text(L("wake.wrong")).foregroundStyle(.red).font(.subheadline) }
            RoundButton(title: L("wake.check"), prominent: true) { check() }
        }
    }

    private var moving: some View {
        let total = kind == "shake" ? WakeTask.shakes : WakeTask.steps
        return VStack(spacing: 14) {
            Text(T(kind == "shake" ? "wake.shake" : "wake.steps", total)).font(.headline).foregroundStyle(Theme.text)
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
            if codeWrong { Text(L("wake.code.wrong")).foregroundStyle(.red).font(.subheadline) }
        }
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
        guard kind == "shake" || kind == "steps" else { return }
        let total = kind == "shake" ? WakeTask.shakes : WakeTask.steps
        motion.start(counter: kind == "shake" ? .shake() : .steps()) { n in
            count = n
            if n >= total { finish() }
        }
    }

    private func finish() {
        motion.stop()
        BadgeStore.shared.hit(.uyanik)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        onDone()
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
