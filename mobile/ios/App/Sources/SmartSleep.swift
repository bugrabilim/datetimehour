import SwiftUI
import UIKit
import CoreMotion
import SaatCore

/// Akıllı alarm (31 Ekim 2026, sahibin kararı: uyku takibinden yalnız akıllı alarm; Android'deki SmartSleep ile aynı kural).
/// Uyku ekranında "Akıllı alarmla uyu" ile akıllı alarm penceresi seçili (alarm başına `smart` dk) sıradaki alarm için
/// oturum açılır: karartılmış tam ekran saat açık kalır (ekran kilitlenmez), pencere başlayınca ivmeölçer saniyede 10 kez
/// okunur (SaatCore SmartWake) ve hafif uyku belirtisi (iki dakika hareket) görülünce alarm hemen çalar (AlarmKit; asıl
/// çalma atlanır). iOS uygulamayı arka planda askıya aldığı için oturum uygulama açıkken ya da uyku sesi çalarken sürer;
/// aksi hâlde alarm her zamanki gibi saatinde çalar. Mikrofon ve kayıt yok; hareket verisi yalnız bellekte, son 10 dakika.
@MainActor
final class SmartSleep: ObservableObject {
    static let shared = SmartSleep()

    struct Session: Equatable {
        let id: UUID
        let at: Date
        let window: Int
        let title: String
        var windowStart: Date { at.addingTimeInterval(-Double(window * 60)) }
    }

    @Published private(set) var session: Session?
    @Published private(set) var watching = false
    @Published private(set) var movingMinutes = 0
    private let motion = CMMotionManager()
    private var detector = SmartWake()
    private var timer: Timer?

    /// Akıllı alarm penceresi seçili sıradaki alarm ve çalma anı
    static func next(now: Date = Date()) -> (item: AlarmItem, at: Date)? {
        AlarmItem.load().filter { $0.enabled && $0.smart > 0 }
            .compactMap { i in i.next(after: now).map { (i, $0) } }
            .min { $0.1 < $1.1 }
    }

    /// Oturumu başlatır; akıllı alarm yoksa false
    @discardableResult
    func start() -> Bool {
        guard let n = Self.next() else { return false }
        session = Session(id: n.item.id, at: n.at, window: n.item.smart, title: n.item.title)
        detector = SmartWake()
        movingMinutes = 0
        watching = false
        UIApplication.shared.isIdleTimerDisabled = true
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in Task { @MainActor in SmartSleep.shared.tick() } }
        tick()
        return true
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        if motion.isAccelerometerActive { motion.stopAccelerometerUpdates() }
        watching = false
        session = nil
        UIApplication.shared.isIdleTimerDisabled = UserDefaults.standard.bool(forKey: "keepAwake")
    }

    private func tick() {
        guard let s = session else { return }
        let now = Date()
        // Alarm saatinde çaldı: oturum kapanır
        if now >= s.at.addingTimeInterval(Double(SmartWake.afterMs) / 1000) { stop(); return }
        if now >= s.windowStart, !motion.isAccelerometerActive, motion.isAccelerometerAvailable {
            motion.accelerometerUpdateInterval = 0.1
            motion.startAccelerometerUpdates(to: .main) { data, _ in
                guard let a = data?.acceleration else { return }
                let x = a.x, y = a.y, z = a.z
                MainActor.assumeIsolated { SmartSleep.shared.feed(x, y, z) }
            }
            watching = true
        }
    }

    private func feed(_ x: Double, _ y: Double, _ z: Double) {
        guard let s = session else { return }
        let ms = Int64(Date().timeIntervalSince1970 * 1000)
        detector.add(tMs: ms, x: x, y: y, z: z)
        if detector.movingMinutes != movingMinutes { movingMinutes = detector.movingMinutes }
        if detector.shouldWake(nowMs: ms, alarmMs: Int64(s.at.timeIntervalSince1970 * 1000), windowMin: s.window) {
            stop()
            Task { await Self.fireEarly(s) }
        }
    }

    /// Erken çalma: asıl çalma atlanır (bir kezlik alarm kapanır), alarm şimdi çalar
    static func fireEarly(_ s: Session) async {
        var items = AlarmItem.load()
        guard let i = items.firstIndex(where: { $0.id == s.id }) else { return }
        let item = items[i]
        if items[i].days.isEmpty { items[i].enabled = false } else { items[i].skipUntil = s.at.addingTimeInterval(60) }
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
        let at = Date().addingTimeInterval(2)
        #if canImport(AlarmKit)
        if #available(iOS 26.0, *) {
            var options = KitAlarms.Options(item)
            if let say = await AlarmSpeech.sound(for: item) { options.soundName = say }
            if await KitAlarms.schedule(id: UUID(), at: at, title: item.title, options: options) { return }
        }
        #endif
        await AlarmCenter.schedule(id: UUID(), at: at, title: item.title)
    }
}

/// Akıllı alarm oturumu ekranı: siyah zemin, kısık kırmızı saat, pencere ve hareket bilgisi, durdurma
struct SmartSleepView: View {
    @ObservedObject var smart = SmartSleep.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            VStack(spacing: 18) {
                Spacer()
                Text(ctx.date, format: .dateTime.hour().minute())
                    .font(.system(size: 84, weight: .light, design: .rounded)).monospacedDigit()
                    .environment(\.layoutDirection, .leftToRight)
                if let s = smart.session {
                    Text(T("smart.running", Fmt.time(s.windowStart), Fmt.time(s.at)))
                        .font(.subheadline).multilineTextAlignment(.center)
                    Text(smart.watching ? T("smart.moving", smart.movingMinutes) : L("smart.waiting"))
                        .font(.footnote)
                }
                Spacer()
                Button(L("smart.stop")) { smart.stop(); dismiss() }
                    .font(.headline).padding(.horizontal, 28).padding(.vertical, 12)
                    .overlay(Capsule().stroke(NightMode.tint.opacity(0.7)))
                Text(L("smart.howIOS")).font(.caption2).multilineTextAlignment(.center).padding(.horizontal, 24)
            }
            .padding(.bottom, 24)
            .offset(NightMode.shift(ctx.date))
        }
        .foregroundStyle(NightMode.tint)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.ignoresSafeArea())
        .modifier(NightBrightness(target: 0.02))
        .persistentSystemOverlays(.hidden)
        .statusBarHidden()
        .onChange(of: smart.session) { _, s in if s == nil { dismiss() } }
    }
}

/// Uyku ekranındaki akıllı alarm bölümü
struct SmartSleepSection: View {
    @ObservedObject var smart = SmartSleep.shared
    @State private var showing = false

    var body: some View {
        Section {
            TimelineView(.periodic(from: .now, by: 30)) { _ in
                if let s = smart.session {
                    Text(T("smart.running", Fmt.time(s.windowStart), Fmt.time(s.at))).font(.subheadline.weight(.semibold))
                    Button(L("smart.stop"), role: .destructive) { smart.stop() }
                } else if let n = SmartSleep.next() {
                    Text(T("smart.next", Fmt.time(n.at.addingTimeInterval(-Double(n.item.smart * 60))), Fmt.time(n.at))).font(.subheadline)
                    Button {
                        if smart.start() { showing = true }
                    } label: {
                        Label(L("smart.start"), systemImage: "bed.double.fill")
                    }
                } else {
                    Text(L("smart.none")).font(.subheadline)
                }
            }
        } header: {
            Text(L("alarm.smart"))
        } footer: {
            Text(L("smart.how") + " " + L("smart.howIOS"))
        }
        .fullScreenCover(isPresented: $showing) { SmartSleepView() }
    }
}
