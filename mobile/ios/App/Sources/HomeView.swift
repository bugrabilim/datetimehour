import SwiftUI
import SaatCore

/// Saat sekmesi: sitedeki 19 saat modeli (kaydırarak ya da sayaçtan seçilir), altında kullanıcının seçtiği
/// alanlar (Ayarlar › Saat ekranı; dokununca ilgili sayfa açılır) ve tam ekran.
struct ClockTab: View {
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw
    @State private var fullscreen = false
    @State private var customizing = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            let place = PlaceModel.shared.place
            ScrollView {
                VStack(spacing: 22) {
                    ClockStage(now: now, place: place)
                    ForEach(HomeLayout(raw: homeItems).visible) { item in
                        HomeItemView(item: item, now: now, place: place)
                    }
                    HStack(spacing: 28) {
                        Button {
                            fullscreen = true
                        } label: {
                            Label(L("clock.fullscreen"), systemImage: "arrow.up.left.and.arrow.down.right")
                        }
                        Button {
                            customizing = true
                        } label: {
                            Label(L("home.edit"), systemImage: "slider.horizontal.3")
                        }
                    }
                    .font(.headline)
                    .foregroundStyle(Theme.accent)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
            .background(Theme.bg.ignoresSafeArea())
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = keepAwake }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .onChange(of: keepAwake) { UIApplication.shared.isIdleTimerDisabled = keepAwake }
        .fullScreenCover(isPresented: $fullscreen) { FullscreenClock() }
        .sheet(isPresented: $customizing) {
            NavigationStack {
                HomeItemsView()
                    .toolbar { Button(L("common.done")) { customizing = false } }
            }
        }
        .task {
            // CI: bildirim seslerinin örneği üretilir, ekran görüntüleriyle birlikte dışarı alınır
            guard ProcessInfo.processInfo.arguments.contains("-makeSoundSample") else { return }
            _ = await NotificationSounds.make(name: NotificationSounds.fileName(.dhuhr, lead: 0),
                                              text: NotificationSounds.spokenText(.dhuhr, lead: 0))
            _ = await NotificationSounds.make(name: NotificationSounds.fileName(.asr, lead: 10),
                                              text: NotificationSounds.spokenText(.asr, lead: 10))
        }
    }
}

/// Saat sahnesi: modeller yatay kaydırılır; altta (sitedeki gibi) ‹ 1/19 › — sayaca dokununca model listesi.
/// Yükseklik sabit: model değişince alttaki alanlar oynamaz. Saate dokununca saniye açılır/kapanır
/// (dijitalde varsayılan açık, analogda kapalı).
struct ClockStage: View {
    let now: Date
    let place: AppPlace
    @AppStorage("clockModel") private var modelID = ClockModel.wxDigitalCalWeek.rawValue

    private var model: ClockModel { ClockModel(rawValue: modelID) ?? .wxDigitalCalWeek }

    var body: some View {
        VStack(spacing: 8) {
            TabView(selection: $modelID) {
                ForEach(ClockModel.allCases) { m in
                    ClockModelView(model: m, now: now, place: place)
                        .padding(.horizontal, 2)
                        .tag(m.rawValue)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: 380)
            HStack(spacing: 22) {
                Button { step(-1) } label: { Image(systemName: "chevron.backward") }
                    .accessibilityLabel(L("stage.prev"))
                Menu {
                    Picker(L("stage.dots"), selection: $modelID) {
                        ForEach(ClockModel.allCases) { m in
                            Text("\(m.number). \(m.name)").tag(m.rawValue)
                        }
                    }
                } label: {
                    Text("\(model.number)/\(ClockModel.allCases.count)")
                        .font(.headline.monospacedDigit())
                        .frame(minWidth: 60)
                }
                .accessibilityLabel(model.name)
                Button { step(1) } label: { Image(systemName: "chevron.forward") }
                    .accessibilityLabel(L("stage.next"))
            }
            .font(.headline)
            .foregroundStyle(Theme.accent)
            .environment(\.layoutDirection, .leftToRight)
        }
    }

    private func step(_ d: Int) {
        let all = ClockModel.allCases
        let i = ((all.firstIndex(of: model) ?? 0) + d + all.count) % all.count
        withAnimation { modelID = all[i].rawValue }
    }
}

/// Tam ekran saat: 19 model yatay kaydırılır, altında saat ekranındaki alanlar (Düzenle ile seçilenler);
/// ekran kapanmaz, çift dokununca ya da düğmeyle çıkılır. Bir alana dokununca tam ekran kapanır, sayfası açılır.
struct FullscreenClock: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage("clockModel") private var modelID = ClockModel.wxDigitalCalWeek.rawValue
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw

    var body: some View {
        GeometryReader { geo in
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = PlaceModel.shared.place
                let items = HomeLayout(raw: homeItems).visible
                ScrollView {
                    VStack(spacing: 22) {
                        TabView(selection: $modelID) {
                            ForEach(ClockModel.allCases) { m in
                                ClockModelView(model: m, now: now, place: place)
                                    .padding(.horizontal, 8)
                                    .tag(m.rawValue)
                            }
                        }
                        .tabViewStyle(.page(indexDisplayMode: .never))
                        .frame(height: items.isEmpty ? max(380, geo.size.height - 70) : max(380, geo.size.height * 0.6))
                        ForEach(items) { item in
                            HomeItemView(item: item, now: now, place: place) { dismiss() }
                        }
                        Button { dismiss() } label: {
                            Label(L("client.exitFullscreen"), systemImage: "arrow.down.right.and.arrow.up.left")
                                .font(.footnote)
                        }
                        .foregroundStyle(Theme.muted)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .frame(maxWidth: 720)
                    .frame(maxWidth: .infinity)
                }
                .scrollIndicators(.hidden)
                .foregroundStyle(Theme.text)
                .background(Theme.bg.ignoresSafeArea())
            }
        }
        .onTapGesture(count: 2) { dismiss() }
        .statusBarHidden()
        .persistentSystemOverlays(.hidden)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = UserDefaults.standard.object(forKey: "keepAwake") as? Bool ?? true
        }
    }
}
