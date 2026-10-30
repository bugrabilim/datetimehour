import SwiftUI
import SaatCore

/// Saat sekmesi: sitedeki 20 saat modeli (kaydırarak ya da sayaçtan seçilir), altında kullanıcının seçtiği
/// alanlar (Ayarlar › Saat ekranı; dokununca ilgili sayfa açılır) ve tam ekran.
struct ClockTab: View {
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw
    @AppStorage(Features.key) private var features = ""
    @State private var fullscreen = false
    @State private var customizing = false
    /// Yan çevirince tam ekran açılır, dikeye dönünce kapanır (düğmeyle açılan tam ekran dikeyde de kalır)
    @Environment(\.verticalSizeClass) private var vsc
    @State private var autoFull = false

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
                                BadgeStore.shared.hit(.tamEkran)
                            } label: {
                                Label(L("clock.fullscreen"), systemImage: "arrow.up.left.and.arrow.down.right")
                            }
                            Button {
                                customizing = true
                            } label: {
                                Label(L("home.edit"), systemImage: "slider.horizontal.3")
                            }
                            // Ayarlar her zaman buradan da açılır (Araçlar kapalıysa tek yol budur)
                            Button {
                                Router.shared.sheet = .settings
                            } label: {
                                Image(systemName: "gearshape")
                            }
                            .accessibilityLabel(L("settings.title"))
                        }
                        .font(.headline)
                        .foregroundStyle(Theme.accent)
                        // Hiçbir özellik açık değilse (ilk açılış) özellikleri açmaya çağıran tek satır
                        if Features.parse(features).isEmpty {
                            Button {
                                Router.shared.sheet = .features
                            } label: {
                                Label(L("features.hint"), systemImage: "square.grid.2x2")
                                    .font(.subheadline)
                                    .multilineTextAlignment(.center)
                            }
                            .foregroundStyle(Theme.muted)
                        }
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
        .onChange(of: vsc, initial: true) { _, v in
            if v == .compact, !fullscreen {
                autoFull = true
                fullscreen = true
            }
        }
        .onChange(of: Router.shared.wantsFullscreen, initial: true) { _, want in
            guard want else { return }
            Router.shared.wantsFullscreen = false
            fullscreen = true
        }
        .fullScreenCover(isPresented: $fullscreen, onDismiss: { autoFull = false }) {
            FullscreenClock(closeOnPortrait: autoFull)
        }
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
        // iPad'de (yükseklik 1000 pt'den fazla) saat daha büyük
        min(viewport > 1000 ? 560 : 380, max(280, viewport * 0.46))
    }

    /// Dünya, sayfayı ekranın altına kadar dolduracak yüksekliği alır (en az 170 pt; daha fazla alan
    /// eklenmişse sayfa kayar). İçerik yüksekliği ölçülür, dünya dışındaki kısım çıkarılır.
    static func fitGlobe(contentHeight: CGFloat, viewport: CGFloat, globe: CGFloat, hasGlobe: Bool) -> CGFloat {
        guard hasGlobe else { return globe }
        let target = max(170, (viewport - (contentHeight - globe)).rounded(.down))
        return abs(target - globe) > 1 ? target : globe
    }
}

/// Saat sahnesi: modeller yatay kaydırılır; altta (sitedeki gibi) ‹ 1/20 › — sayaca dokununca model listesi.
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
        .onChange(of: modelID, initial: true) { old, new in
            // Rozetler: görülen modeller (açılıştaki dahil) ve ilk model değişimi
            if let i = ClockModel.allCases.firstIndex(where: { $0.rawValue == new }) {
                BadgeStore.shared.sawModel(i, of: ClockModel.allCases.count)
            }
            if old != new { BadgeStore.shared.hit(.ilkModel) }
        }
    }

    private func step(_ d: Int) {
        let all = ClockModel.allCases
        let i = ((all.firstIndex(of: model) ?? 0) + d + all.count) % all.count
        withAnimation { modelID = all[i].rawValue }
    }
}

/// Tam ekran saat: 20 model yatay kaydırılır, altında saat ekranındaki alanlar (Düzenle ile seçilenler);
/// ekran kapanmaz, çift dokununca ya da düğmeyle çıkılır. Bir alana dokununca tam ekran kapanır, sayfası açılır.
struct FullscreenClock: View {
    /// Yan çevirince açıldıysa telefon dik tutulunca kapanır
    var closeOnPortrait = false
    @Environment(\.dismiss) private var dismiss
    @Environment(\.verticalSizeClass) private var vsc
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("clockModel") private var modelID = ClockModel.classic.rawValue
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw
    @AppStorage(FullscreenPhoto.versionKey) private var photoVersion = 0.0
    @AppStorage(FullscreenBackground.key) private var bgRaw = ""
    @AppStorage(NightMode.key) private var nightRaw = "off"
    @State private var globeHeight: CGFloat = 260

    var body: some View {
        // Arka plan: fotoğraf (koyulaştırılmış) ya da hareketli; ikisinde de yazılar koyu temanın açık renklerine geçer
        let bg = FullscreenBackground(rawValue: bgRaw) ?? (photoVersion > 0 ? .photo : .plain)
        let photo = bg == .photo && photoVersion > 0 ? FullscreenPhoto.image : nil
        let darkText = photo != nil || bg.animated
        GeometryReader { geo in
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = PlaceModel.shared.place
                let items = HomeLayout(raw: homeItems).visible
                // Gece modunda siyah zemin, kırmızı ve kısık yazı, düşük parlaklık; arka plan fotoğrafı ve hareket yok
                // Gün doğumu ışığı (rakip analizi #17): alarmdan önce zemin siyahtan sıcak beyaza döner, parlaklık artar
                let sunrise = AlarmItem.nextSunrise(now: now).map { SunriseLight.level(now: now, alarmAt: $0.at, window: $0.window) } ?? 0
                let night = sunrise == 0 && NightMode.active(nightRaw, place: place, at: now)
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
                        .frame(height: Self.stageHeight(geo.size, hasItems: !items.isEmpty))
                        ForEach(items) { item in
                            HomeItemView(item: item, now: now, place: place, globeHeight: globeHeight) { dismiss() }
                        }
                        HStack(spacing: 20) {
                            // Gece modu: açıkken kapatır, kapalıyken açar (otomatik seçim Ayarlar › Saat görünümü'nde)
                            Button { nightRaw = NightMode.toggled(night) } label: {
                                Label(L("night.title"), systemImage: night ? "moon.fill" : "moon")
                                    .font(.footnote)
                            }
                            Button { dismiss() } label: {
                                Label(L("client.exitFullscreen"), systemImage: "arrow.down.right.and.arrow.up.left")
                                    .font(.footnote)
                            }
                        }
                        .foregroundStyle(Theme.muted)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    // Ekran yanmasın diye dakikada bir birkaç nokta kayar
                    .offset(night ? NightMode.shift(now) : .zero)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { h in
                        globeHeight = ClockTab.fitGlobe(contentHeight: h, viewport: geo.size.height, globe: globeHeight,
                                                        hasGlobe: items.contains(.globe))
                    }
                    .frame(maxWidth: 720)
                    .frame(maxWidth: .infinity)
                }
                .scrollIndicators(.hidden)
                .foregroundStyle(Theme.text)
                .background {
                    if sunrise > 0 {
                        let c = SunriseLight.rgb(sunrise)
                        Color(red: Double(c.r) / 255, green: Double(c.g) / 255, blue: Double(c.b) / 255).ignoresSafeArea()
                    } else if night {
                        Color.black.ignoresSafeArea()
                    } else if let photo {
                        Image(uiImage: photo).resizable().scaledToFill()
                            .overlay(Color.black.opacity(0.45))
                            .ignoresSafeArea()
                    } else if bg.animated {
                        AnimatedBackground(kind: bg)
                    } else {
                        Theme.bg.ignoresSafeArea()
                    }
                }
                // Kırmızıya çevirme: siyah zemin üstünde çarpım (beyaz → kısık kırmızı, siyah siyah kalır)
                .overlay {
                    if night {
                        NightMode.tint.blendMode(.multiply).ignoresSafeArea().allowsHitTesting(false)
                    }
                }
                .environment(\.colorScheme, sunrise > 0.6 ? .light : (night || darkText || sunrise > 0 ? .dark : colorScheme))
                .modifier(NightBrightness(target: sunrise > 0 ? SunriseLight.brightness(sunrise) : (night ? 0.05 : nil)))
            }
        }
        .onTapGesture(count: 2) { dismiss() }
        .onChange(of: vsc) { _, v in
            if v == .regular, closeOnPortrait { dismiss() }
        }
        .overlay(alignment: .top) { BadgeToast() }
        .onChange(of: modelID) { _, new in
            if let i = ClockModel.allCases.firstIndex(where: { $0.rawValue == new }) {
                BadgeStore.shared.sawModel(i, of: ClockModel.allCases.count)
            }
            BadgeStore.shared.hit(.ilkModel)
        }
        .statusBarHidden()
        .persistentSystemOverlays(.hidden)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = UserDefaults.standard.object(forKey: "keepAwake") as? Bool ?? true
        }
    }

    /// Yatayda saat ekranı doldurur (alanlar kaydırınca görünür); dikeyde alan yoksa saat ekranı doldurur
    static func stageHeight(_ size: CGSize, hasItems: Bool) -> CGFloat {
        if size.width > size.height { return max(220, size.height - 44) }
        return hasItems ? ClockTab.stageHeight(size.height * 1.08) : max(300, size.height - 60)
    }
}
