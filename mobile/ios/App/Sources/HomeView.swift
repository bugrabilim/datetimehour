import SwiftUI
import SaatCore

/// Saat sekmesi: sitedeki 20 saat modeli (kaydırarak ya da sayaçtan seçilir), altında kullanıcının seçtiği
/// alanlar (Ayarlar › Saat ekranı; dokununca ilgili sayfa açılır) ve tam ekran.
struct ClockTab: View {
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("homeItems") private var homeItems = HomeItem.defaultRaw
    @AppStorage(Features.key) private var features = ""
    @AppStorage("fs.text") private var personalText = ""
    @AppStorage(WeatherScene.clockKey) private var weatherBg = false
    @Environment(\.colorScheme) private var colorScheme
    @State private var fullscreen = false
    @State private var customizing = false
    /// Yan çevirince tam ekran açılır, dikeye dönünce kapanır (düğmeyle açılan tam ekran dikeyde de kalır)
    @Environment(\.verticalSizeClass) private var vsc
    @State private var autoFull = false

    /// Dönen dünyanın yüksekliği: varsayılan düzende sayfa ekrana tam otursun diye kalan boşluğu doldurur
    @State private var globeHeight: CGFloat = 260
    /// Alttaki düğme satırının yüksekliği (2. analiz #202)
    static let actionBar: CGFloat = 52

    var body: some View {
        GeometryReader { geo in
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = PlaceModel.shared.place
                let items = HomeLayout(raw: homeItems).visible
                // Düğme satırı alt çubuğun hemen üstünde, başparmak bölgesinde (2. analiz #202)
                let viewport = geo.size.height - Self.actionBar
                // Saat ekranında yalnız saat varsa (30 Ekim kararı) saat ve denetim satırı dikeyde ortalanır, saat genişliğe
                // büyür (2. analiz #94)
                let onlyClock = items.isEmpty && personalText.isEmpty
                let stage = onlyClock ? max(280, min(viewport - 150, min(geo.size.width, 640) * 0.95)) : Self.stageHeight(viewport)
                VStack(spacing: 0) {
                ScrollView {
                    VStack(spacing: 16) {
                        ClockStage(now: now, place: place, height: stage)
                        if !personalText.isEmpty {
                            Text(personalText).font(.title3.weight(.semibold)).multilineTextAlignment(.center).foregroundStyle(Theme.text)
                        }
                        ForEach(items) { item in
                            HomeItemView(item: item, now: now, place: place, globeHeight: globeHeight)
                        }
                        // Yenilikler: yeni kadranlar bir kez tanıtılır (2. analiz #1)
                        DialNewsCard()
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
                    .frame(minHeight: onlyClock ? viewport - 20 : nil)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { h in
                        globeHeight = Self.fitGlobe(contentHeight: h, viewport: viewport, globe: globeHeight,
                                                    hasGlobe: items.contains(.globe))
                    }
                    .frame(maxWidth: 640)
                    .frame(maxWidth: .infinity)
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
                .frame(height: Self.actionBar)
                .frame(maxWidth: .infinity)
                }
                // Hava neyse ekran öyle (2. analiz #193): seçilirse zemin seçili yerin havası, yazılar koyu temanın renkleri
                .background {
                    if weatherBg { AnimatedBackground(kind: .weather) } else { AppBackdrop().ignoresSafeArea() }
                }
                .environment(\.colorScheme, weatherBg ? .dark : colorScheme)
                // Saniye tık sesi (2. analiz #10; Ayarlar › Saat görünümü)
                .onChange(of: Int(now.timeIntervalSince1970)) { _, s in TickSound.shared.tick(s) }
            }
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = keepAwake }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            TickSound.shared.stop()
        }
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
                    // Model adı ve sırası (2. analiz #202): "Klasik analog · 1/20"; dokununca liste
                    Text("\(model.name) · \(model.number)/\(ClockModel.allCases.count)")
                        .font(.subheadline.weight(.semibold).monospacedDigit())
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .frame(minWidth: 60, maxWidth: 260)
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
    /// Uyku zamanlayıcısı (rakip analizi #1): bitiş anı (1970'ten saniye, 0 = kapalı); tam ekrandan çıkınca silinir
    @AppStorage(SleepTimer.key) private var sleepUntil = 0.0
    @AppStorage(FullscreenPhoto.slidesKey) private var slideCount = 0
    /// Saatin altındaki kişisel metin, saat boyutu, karartma ve yön kilidi (rakip analizi #25, #22)
    @AppStorage("fs.text") private var personalText = ""
    @AppStorage("fs.scale") private var scale = 1.0
    @AppStorage("fs.dim") private var dim = 0.0
    @AppStorage("fs.lock") private var locked = false
    @AppStorage(FsDuo.key) private var duo = "off"
    @State private var dimStart: Double?
    @State private var scaleStart: Double?
    @State private var globeHeight: CGFloat = 260
    /// Metni tabela gibi büyük gösterme (2. analiz #22)
    @AppStorage("fs.banner") private var banner = false
    /// Gösterilen sayfa: saat modeli ya da iki yandaki araç sayfası (2. analiz #26: ilk saatten geriye pomodoro,
    /// son saatten sonra kronometre). Saat modeline geçilince seçim kaydedilir.
    @State private var page = UserDefaults.standard.string(forKey: "clockModel") ?? ClockModel.classic.rawValue
    /// Düğmeler dokununca belirir, 4 sn sonra kaybolur (sitedeki tam ekran gibi; 2. analiz #204)
    @State private var controlsShown = true
    @State private var hideTask: Task<Void, Never>?

    private func pokeControls() {
        controlsShown = true
        hideTask?.cancel()
        hideTask = Task { @MainActor in
            try? await Task.sleep(for: .seconds(4))
            guard !Task.isCancelled else { return }
            withAnimation(.easeOut(duration: 0.4)) { controlsShown = false }
        }
    }

    var body: some View {
        // Arka plan: fotoğraf (koyulaştırılmış; slayt varsa sırayla) ya da hareketli; ikisinde de yazılar açık renk
        let bg = FullscreenBackground(rawValue: bgRaw) ?? (photoVersion > 0 ? .photo : .plain)
        let hasPhoto = bg == .photo && (photoVersion > 0 || slideCount > 0)
        let darkText = hasPhoto || bg.animated
        GeometryReader { geo in
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let now = context.date
                let place = PlaceModel.shared.place
                let items = HomeLayout(raw: homeItems).visible
                // Gece modunda siyah zemin, kırmızı ve kısık yazı, düşük parlaklık; arka plan fotoğrafı ve hareket yok
                // Gün doğumu ışığı (rakip analizi #17): alarmdan önce zemin siyahtan sıcak beyaza döner, parlaklık artar
                let sunrise = AlarmItem.nextSunrise(now: now).map { SunriseLight.level(now: now, alarmAt: $0.at, window: $0.window) } ?? 0
                let night = sunrise == 0 && NightMode.active(nightRaw, place: place, at: now)
                // Uyku zamanlayıcısı doldu: ekran kararır, telefon kendini kilitleyebilir, uyku sesleri durur
                let asleep = sleepUntil > 0 && now.timeIntervalSince1970 >= sleepUntil
                let showBanner = banner && !personalText.isEmpty
                let stage = Self.stageHeight(geo.size, hasItems: !items.isEmpty)
                ScrollView {
                    VStack(spacing: 16) {
                        // Tabela: metin ekranın üst yarısını doldurur, saat altında küçülür
                        if showBanner {
                            Text(bannerLines(personalText, aspect: Double((geo.size.width - 32) / (geo.size.height * 0.5))))
                                .font(.system(size: 400, weight: .heavy, design: .rounded))
                                .minimumScaleFactor(0.02)
                                .lineLimit(3)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                                .frame(height: geo.size.height * 0.5)
                        }
                        // Yatayda saatin yanında ikinci alan (2. analiz #169, `fs.duo`): takvim, vakitler ya da hava
                        let duoOn = duo != "off" && geo.size.width > geo.size.height && !showBanner
                        HStack(spacing: 16) {
                            TabView(selection: $page) {
                                FullscreenToolPage(tool: .pomodoro).tag(FullscreenTool.pomodoro.rawValue)
                                ForEach(ClockModel.allCases) { m in
                                    ClockModelView(model: m, now: now, place: place)
                                        .scaleEffect(scale)
                                        .padding(.horizontal, 8)
                                        .tag(m.rawValue)
                                }
                                FullscreenToolPage(tool: .stopwatch).tag(FullscreenTool.stopwatch.rawValue)
                            }
                            .tabViewStyle(.page(indexDisplayMode: .never))
                            .frame(maxWidth: duoOn ? geo.size.width * 0.56 : .infinity)
                            if duoOn {
                                FsDuoPanel(kind: duo, now: now, place: place, height: stage) { dismiss() }
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        .frame(height: showBanner ? max(180, geo.size.height * 0.4) : stage)
                        if !personalText.isEmpty && !showBanner {
                            Text(personalText).font(.title2.weight(.semibold)).multilineTextAlignment(.center)
                        }
                        ForEach(items) { item in
                            HomeItemView(item: item, now: now, place: place, globeHeight: globeHeight) { dismiss() }
                        }
                        HStack(spacing: 20) {
                            // Gece modu: açıkken kapatır, kapalıyken açar (otomatik seçim Ayarlar › Saat görünümü'nde)
                            Button { nightRaw = NightMode.toggled(night) } label: {
                                Label(L("night.title"), systemImage: night ? "moon.fill" : "moon")
                                    .font(.footnote)
                            }
                            Menu {
                                ForEach(SleepTimer.choices, id: \.self) { m in
                                    Button(m == 0 ? L("alarm.snoozeOff") : T("alarm.snoozeMin", m)) {
                                        sleepUntil = m > 0 ? Date().timeIntervalSince1970 + Double(m * 60) : 0
                                    }
                                }
                            } label: {
                                let left = Int((sleepUntil - now.timeIntervalSince1970).rounded(.up))
                                Label(sleepUntil > 0 && left > 0 ? clock(left) : L("night.sleepTimer"), systemImage: "moon.zzz")
                                    .font(.footnote.monospacedDigit())
                                    .foregroundStyle(sleepUntil > 0 ? Theme.accent : Theme.muted)
                            }
                            // Yön kilidi: şu anki yöne kilitler (tam ekrandan çıkınca bırakılır)
                            Button {
                                locked.toggle()
                                if locked { OrientationLock.lockCurrent() } else { OrientationLock.unlock() }
                            } label: {
                                Image(systemName: locked ? "lock.rotation" : "rotate.right").font(.footnote)
                            }
                            .accessibilityLabel(L(locked ? "fs.unlock" : "fs.lock"))
                            Button { dismiss() } label: {
                                Label(L("client.exitFullscreen"), systemImage: "arrow.down.right.and.arrow.up.left")
                                    .font(.footnote)
                            }
                        }
                        .foregroundStyle(Theme.muted)
                        .opacity(controlsShown ? 1 : 0)
                        .allowsHitTesting(controlsShown)
                        .accessibilityHidden(!controlsShown)
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
                    } else if hasPhoto, let photo = FullscreenPhoto.image(at: now) {
                        Image(uiImage: photo).resizable().scaledToFill()
                            .overlay(Color.black.opacity(0.45))
                            .ignoresSafeArea()
                            .id(ObjectIdentifier(photo))
                            .transition(.opacity)
                    } else if bg.animated {
                        AnimatedBackground(kind: bg)
                    } else {
                        AppBackdrop().ignoresSafeArea()
                    }
                }
                // Kırmızıya çevirme: siyah zemin üstünde çarpım (beyaz → kısık kırmızı, siyah siyah kalır)
                .overlay {
                    if night {
                        NightMode.tint.blendMode(.multiply).ignoresSafeArea().allowsHitTesting(false)
                    }
                }
                // Karartma: ekranın kenarında yukarı-aşağı kaydırma; iki parmakla saat boyutu
                .overlay {
                    Color.black.opacity(dim * 0.85).ignoresSafeArea().allowsHitTesting(false)
                }
                .overlay(alignment: .leading) {
                    Color.clear.frame(width: 44).contentShape(Rectangle())
                        .gesture(DragGesture(minimumDistance: 8).onChanged { v in
                            let start = dimStart ?? dim
                            dimStart = start
                            dim = min(1, max(0, start + Double(v.translation.height) / 400))
                        }.onEnded { _ in dimStart = nil })
                        .accessibilityHidden(true)
                }
                .overlay {
                    if dimStart != nil {
                        Text(T("fs.dim", Fmt.percent(Int((dim * 100).rounded())))).font(.headline.monospacedDigit())
                            .padding(10).background(.regularMaterial, in: Capsule()).allowsHitTesting(false)
                    }
                }
                .simultaneousGesture(MagnifyGesture().onChanged { v in
                    let start = scaleStart ?? scale
                    scaleStart = start
                    scale = min(1.3, max(0.5, start * Double(v.magnification)))
                }.onEnded { _ in scaleStart = nil })
                .overlay {
                    // Dokununca saat geri gelir
                    if asleep {
                        Color.black.ignoresSafeArea().onTapGesture { sleepUntil = 0 }
                    }
                }
                .onChange(of: Int(now.timeIntervalSince1970)) { _, s in
                    if !asleep { TickSound.shared.tick(s) }
                }
                .onChange(of: asleep) { _, a in
                    UIApplication.shared.isIdleTimerDisabled = !a
                    if a { NoisePlayer.shared.stop(source: "sleep") }
                }
                .environment(\.colorScheme, sunrise > 0.6 ? .light : (night || darkText || sunrise > 0 ? .dark : colorScheme))
                .modifier(NightBrightness(target: asleep ? CGFloat(0.01) : (sunrise > 0 ? CGFloat(SunriseLight.brightness(sunrise)) : (night ? CGFloat(0.05) : nil))))
            }
        }
        .onTapGesture(count: 2) { dismiss() }
        .simultaneousGesture(TapGesture().onEnded { pokeControls() })
        .simultaneousGesture(DragGesture(minimumDistance: 12).onEnded { _ in pokeControls() })
        .onAppear { pokeControls() }
        .onDisappear { hideTask?.cancel() }
        .onChange(of: vsc) { _, v in
            if v == .regular, closeOnPortrait { dismiss() }
        }
        .overlay(alignment: .top) { BadgeToast() }
        .onChange(of: page) { _, new in
            guard ClockModel(rawValue: new) != nil, new != modelID else { return }
            modelID = new
        }
        .onChange(of: modelID) { _, new in
            if let i = ClockModel.allCases.firstIndex(where: { $0.rawValue == new }) {
                BadgeStore.shared.sawModel(i, of: ClockModel.allCases.count)
            }
            BadgeStore.shared.hit(.ilkModel)
        }
        .statusBarHidden()
        .persistentSystemOverlays(.hidden)
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
            if locked { OrientationLock.lockCurrent() }
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = UserDefaults.standard.object(forKey: "keepAwake") as? Bool ?? true
            sleepUntil = 0
            if locked { OrientationLock.unlock() }
        }
    }

    /// Yatayda saat ekranı doldurur (alanlar kaydırınca görünür); dikeyde alan yoksa saat ekranı doldurur
    static func stageHeight(_ size: CGSize, hasItems: Bool) -> CGFloat {
        if size.width > size.height { return max(220, size.height - 44) }
        return hasItems ? ClockTab.stageHeight(size.height * 1.08) : max(300, size.height - 60)
    }
}

/// Yatay tam ekranda saatin yanındaki alan (2. analiz #169): kayıt `fs.duo` (off, calendar, prayer, weather);
/// Android'deki FsDuo ile aynı
enum FsDuo {
    static let key = "fs.duo"
    static let all = ["off", "calendar", "prayer", "weather"]
}

struct FsDuoPanel: View {
    let kind: String
    let now: Date
    let place: AppPlace
    let height: CGFloat
    let onOpen: () -> Void

    var body: some View {
        switch kind {
        case "calendar":
            MonthPart(now: now, width: min(height * 0.92, 420))
        case "prayer":
            VStack(spacing: 12) {
                HomeItemView(item: .next, now: now, place: place, globeHeight: 0) { onOpen() }
                HomeItemView(item: .prayers, now: now, place: place, globeHeight: 0) { onOpen() }
            }
            .frame(maxWidth: 420)
        case "weather":
            HomeItemView(item: .weather, now: now, place: place, globeHeight: 0) { onOpen() }
                .frame(maxWidth: 420)
        default:
            EmptyView()
        }
    }
}

/// Saat görünümü sayfasındaki seçim
struct FsDuoPicker: View {
    @AppStorage(FsDuo.key) private var duo = "off"

    var body: some View {
        Picker(L("fs.duo"), selection: $duo) {
            ForEach(FsDuo.all, id: \.self) { Text(L("fs.duo.\($0)")).tag($0) }
        }
        Text(L("fs.duoNote")).font(.footnote).foregroundStyle(Theme.muted)
    }
}
