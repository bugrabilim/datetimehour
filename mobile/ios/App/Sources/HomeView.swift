import SwiftUI
import SaatCore

/// Saat sekmesi: sitedeki 19 saat modeli (kaydırarak ya da sayaçtan seçilir), altında kullanıcının seçtiği
/// alanlar (Ayarlar › Saat ekranı; dokununca ilgili sayfa açılır) ve tam ekran.
struct ClockTab: View {
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw
    @State private var fullscreen = false
    @State private var customizing = false

    /// Dönen dünyanın yüksekliği: varsayılan düzende sayfa ekrana tam otursun diye kalan boşluğu doldurur
    @State private var globeHeight: CGFloat = 260

    var body: some View {
        GeometryReader { geo in
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = PlaceModel.shared.place
                let items = HomeLayout(raw: homeItems).visible
                ScrollView {
                    VStack(spacing: 16) {
                        ClockStage(now: now, place: place, height: Self.stageHeight(geo.size.height))
                        ForEach(items) { item in
                            HomeItemView(item: item, now: now, place: place, globeHeight: globeHeight)
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
                    .padding(.vertical, 10)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { h in
                        globeHeight = Self.fitGlobe(contentHeight: h, viewport: geo.size.height, globe: globeHeight,
                                                    hasGlobe: items.contains(.globe))
                    }
                    .frame(maxWidth: 640)
                    .frame(maxWidth: .infinity)
                }
                .background(Theme.bg.ignoresSafeArea())
            }
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

extension ClockTab {
    /// Saat sahnesinin yüksekliği ekrana göre (küçük telefonda da saat, alanlar ve dünya bir ekrana sığsın)
    static func stageHeight(_ viewport: CGFloat) -> CGFloat {
        min(380, max(280, viewport * 0.46))
    }

    /// Dünya, sayfayı ekranın altına kadar dolduracak yüksekliği alır (en az 170 pt; daha fazla alan
    /// eklenmişse sayfa kayar). İçerik yüksekliği ölçülür, dünya dışındaki kısım çıkarılır.
    static func fitGlobe(contentHeight: CGFloat, viewport: CGFloat, globe: CGFloat, hasGlobe: Bool) -> CGFloat {
        guard hasGlobe else { return globe }
        let target = max(170, (viewport - (contentHeight - globe)).rounded(.down))
        return abs(target - globe) > 1 ? target : globe
    }
}

/// Saat sahnesi: modeller yatay kaydırılır; altta (sitedeki gibi) ‹ 1/19 › — sayaca dokununca model listesi.
/// Yükseklik sabit: model değişince alttaki alanlar oynamaz. Saate dokununca saniye açılır/kapanır
/// (dijitalde varsayılan açık, analogda kapalı).
struct ClockStage: View {
    let now: Date
    let place: AppPlace
    var height: CGFloat = 380
    @AppStorage("clockModel") private var modelID = ClockModel.classic.rawValue

    private var model: ClockModel { ClockModel(rawValue: modelID) ?? .classic }

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
            .frame(height: height)
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
    @AppStorage("clockModel") private var modelID = ClockModel.classic.rawValue
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw
    @State private var globeHeight: CGFloat = 260

    var body: some View {
        GeometryReader { geo in
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = PlaceModel.shared.place
                let items = HomeLayout(raw: homeItems).visible
                ScrollView {
                    VStack(spacing: 16) {
                        TabView(selection: $modelID) {
                            ForEach(ClockModel.allCases) { m in
                                ClockModelView(model: m, now: now, place: place)
                                    .padding(.horizontal, 8)
                                    .tag(m.rawValue)
                            }
                        }
                        .tabViewStyle(.page(indexDisplayMode: .never))
                        .frame(height: items.isEmpty ? max(300, geo.size.height - 60) : ClockTab.stageHeight(geo.size.height * 1.08))
                        ForEach(items) { item in
                            HomeItemView(item: item, now: now, place: place, globeHeight: globeHeight) { dismiss() }
                        }
                        Button { dismiss() } label: {
                            Label(L("client.exitFullscreen"), systemImage: "arrow.down.right.and.arrow.up.left")
                                .font(.footnote)
                        }
                        .foregroundStyle(Theme.muted)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { h in
                        globeHeight = ClockTab.fitGlobe(contentHeight: h, viewport: geo.size.height, globe: globeHeight,
                                                        hasGlobe: items.contains(.globe))
                    }
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
