import AppKit
import ServiceManagement
import SwiftUI
import SaatCore

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
        timer = Timer.scheduledTimer(withTimeInterval: 15, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.refresh() }
        }
        Task { await locate() }
    }

    var selection: String { UserDefaults.standard.string(forKey: DeskPlace.key) ?? "istanbul" }
    var place: Place { DeskPlace.resolve(selection, auto: autoPlace) }

    func refresh() {
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
