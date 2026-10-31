import SwiftUI
import SaatCore

/// Vision Pro uygulaması (rakip analizi listesi #97): odaya yerleştirilen pencerede saat, tarih (miladi + hicri), sıradaki
/// vakit ve kalan süre, bugünün vakitleri. Yer 81 il ya da cihazın konumu; alttaki çubuktan değişir.
@main
struct SaatTarihVisionApp: App {
    var body: some Scene {
        WindowGroup { VisionHome() }
            .defaultSize(width: 760, height: 520)
    }
}

struct VisionHome: View {
    @AppStorage(DeskPlace.key) private var sel = "istanbul"
    @State private var autoPlace: Place?
    @State private var picking = false

    var body: some View {
        let place = DeskPlace.resolve(sel, auto: autoPlace)
        NavigationStack {
            DeskClock(place: place, scale: 0.9)
                .padding(40)
                .toolbar {
                    ToolbarItem(placement: .bottomOrnament) {
                        Button(L("wear.place") + ": " + place.name) { picking = true }
                    }
                }
        }
        .sheet(isPresented: $picking) {
            NavigationStack { DeskPlaceList(sel: $sel) { picking = false } }
        }
        .task(id: sel) {
            guard sel == "auto" else { return }
            if let loc = await OneShotLocation().fetch(requestPermission: true) { autoPlace = Place(location: loc) }
        }
    }
}
