import SwiftUI
import UIKit
import SaatCore

/// Apple TV uygulaması (rakip analizi listesi #95): televizyonda tam ekran saat, tarih (miladi + hicri), sıradaki vakit ve
/// kalan süre, bugünün vakitleri. Ekran koruyucuya geçmez (açıkken uyku kapalı). Yer 81 il ya da Apple TV'nin konumu.
@main
struct SaatTarihTVApp: App {
    var body: some Scene {
        WindowGroup { TVHome() }
    }
}

struct TVHome: View {
    @AppStorage(DeskPlace.key) private var sel = "istanbul"
    @State private var autoPlace: Place?
    @State private var picking = false

    var body: some View {
        let place = DeskPlace.resolve(sel, auto: autoPlace)
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 40) {
                DeskClock(place: place, scale: 2.1)
                    .foregroundStyle(.white)
                Button(L("wear.place") + ": " + place.name) { picking = true }
            }
            .padding(60)
        }
        .preferredColorScheme(.dark)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .sheet(isPresented: $picking) {
            NavigationStack { DeskPlaceList(sel: $sel) { picking = false } }
        }
        .task(id: sel) {
            guard sel == "auto" else { return }
            if let loc = await OneShotLocation().fetch(requestPermission: true) { autoPlace = Place(location: loc) }
        }
    }
}
