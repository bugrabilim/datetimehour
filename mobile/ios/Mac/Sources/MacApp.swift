import AppKit
import ServiceManagement
import SwiftUI
import SaatCore
import UserNotifications

/// Mac menü çubuğu uygulaması (rakip analizi listesi #96): menü çubuğunda sıradaki vakit ve kalan süre ("Öğle 12:58 · 25 dk"),
/// tıklayınca saat, tarih (miladi + hicri) ve bugünün vakitleri. Dock'ta simgesi yoktur (LSUIElement). Vakitte bildirim
/// için iPhone uygulaması kullanılır; bu uygulama yalnız gösterir. İnternet gerekmez.
@main
struct SaatTarihMacApp: App {
    @StateObject private var model = MacModel()

    var body: some Scene {
        MenuBarExtra {
            MacPanel(model: model)
        } label: {
            Text(model.title)
        }
        .menuBarExtraStyle(.window)
    }
}

@MainActor
final class MacModel: ObservableObject {
    @Published var title = "Saat Tarih"
    @Published var autoPlace: Place?
    private var timer: Timer?

    init() {
        refresh()
        // Pomodoro sürerken menü çubuğundaki süre saniyede bir yenilenir
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.tick() }
        }
        Task { await locate() }
    }

    var selection: String { UserDefaults.standard.string(forKey: DeskPlace.key) ?? "istanbul" }
    var place: Place { DeskPlace.resolve(selection, auto: autoPlace) }

    // MARK: Pomodoro (2. analiz #143): 25 dk çalışma / 5 dk mola; menü çubuğunda "🍅 24:59"; bitince bildirim
    @Published var pomoEnd: Double = UserDefaults.standard.double(forKey: "mac.pomo.end")
    @Published var pomoPhase: String = UserDefaults.standard.string(forKey: "mac.pomo.phase") ?? "work"
    @Published var pomoDone: Int = UserDefaults.standard.integer(forKey: "mac.pomo.done")
    private var lastPrayerRefresh = Date.distantPast

    var pomoMinutes: Int { pomoPhase == "work" ? 25 : 5 }
    var pomoRunning: Bool { pomoEnd > Date().timeIntervalSince1970 }

    private func savePomo() {
        let d = UserDefaults.standard
        d.set(pomoEnd, forKey: "mac.pomo.end")
        d.set(pomoPhase, forKey: "mac.pomo.phase")
        d.set(pomoDone, forKey: "mac.pomo.done")
    }

    func pomoStart() {
        if pomoEnd > 0 && !pomoRunning {
            if pomoPhase == "work" { pomoDone += 1 }
            pomoPhase = pomoPhase == "work" ? "break" : "work"
        }
        pomoEnd = Date().timeIntervalSince1970 + Double(pomoMinutes * 60)
        savePomo()
        let content = UNMutableNotificationContent()
        content.title = pomoPhase == "work" ? L("desk.pomoDone") : L("desk.breakDone")
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: Double(pomoMinutes * 60), repeats: false)
        let c = UNUserNotificationCenter.current()
        c.requestAuthorization(options: [.alert, .sound]) { _, _ in }
        c.add(UNNotificationRequest(identifier: "mac-pomo", content: content, trigger: trigger))
        refresh()
    }

    func pomoReset() {
        pomoEnd = 0
        pomoPhase = "work"
        savePomo()
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["mac-pomo"])
        refresh()
    }

    func pomoText(_ now: Date = Date()) -> String {
        let s = max(0, Int(pomoEnd - now.timeIntervalSince1970))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }

    func tick() {
        if pomoRunning || Date().timeIntervalSince(lastPrayerRefresh) >= 15 { refresh() }
    }

    func refresh() {
        lastPrayerRefresh = Date()
        if pomoRunning { title = "🍅 " + pomoText(); return }
        let now = Date()
        let p = place
        guard let next = p.schedule.next(after: now) else { title = "Saat Tarih"; return }
        title = "\(DeskPlace.label(next, zone: p.schedule.timeZone)) \(p.schedule.clock(next.at)) · \(DeskPlace.remaining(next.at.timeIntervalSince(now)))"
    }

    func locate() async {
        guard selection == "auto" else { return }
        if let loc = await OneShotLocation().fetch(requestPermission: true) { autoPlace = Place(location: loc) }
        refresh()
    }
}

struct MacPanel: View {
    @ObservedObject var model: MacModel
    @AppStorage(DeskPlace.key) private var sel = "istanbul"
    @AppStorage("h12") private var h12 = false
    @State private var atLogin = SMAppService.mainApp.status == .enabled

    var body: some View {
        VStack(spacing: 14) {
            DeskClock(place: model.place, scale: 0.42)
            Divider()
            // Pomodoro (2. analiz #143)
            HStack(spacing: 10) {
                Text("🍅").font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text(model.pomoPhase == "work" ? L("watch.work") : L("watch.break")).font(.headline)
                    Text(model.pomoRunning ? model.pomoText() : String(format: L("watch.minutes"), "\(model.pomoMinutes)"))
                        .font(.system(.title3, design: .rounded).weight(.semibold)).monospacedDigit()
                }
                Spacer()
                Text("× \(model.pomoDone)").foregroundStyle(.secondary)
                if model.pomoRunning || model.pomoEnd > 0 {
                    Button(L("watch.reset")) { model.pomoReset() }
                }
                if !model.pomoRunning {
                    Button(L("watch.start")) { model.pomoStart() }.keyboardShortcut(.defaultAction)
                }
            }
            Divider()
            Picker(L("wear.place"), selection: $sel) {
                Text(L("widget.province.auto")).tag("auto")
                ForEach(Province.all) { p in Text(p.displayName).tag(p.slug) }
            }
            Toggle(L("desk.h12"), isOn: $h12)
            Toggle(L("desk.launchAtLogin"), isOn: Binding(get: { atLogin }, set: { on in
                do {
                    if on { try SMAppService.mainApp.register() } else { try SMAppService.mainApp.unregister() }
                } catch {}
                atLogin = SMAppService.mainApp.status == .enabled
            }))
            HStack {
                Text(L("desk.macNote")).font(.caption).foregroundStyle(.secondary)
                Spacer()
                Button(L("desk.quit")) { NSApplication.shared.terminate(nil) }
            }
        }
        .padding(16)
        .frame(width: 380)
        .onChange(of: sel) {
            model.refresh()
            Task { await model.locate() }
        }
        .onChange(of: h12) { model.refresh() }
    }
}
