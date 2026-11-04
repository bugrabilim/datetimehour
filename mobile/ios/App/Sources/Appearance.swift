import PhotosUI
import SwiftUI
import UIKit

/// Saat yazı tipi (30 Ekim 2026, rakip analizi #26): dijital ve çevirmeli saat rakamları; seçim `clockFont`
enum ClockFont: String, CaseIterable, Identifiable {
    case rounded, standard, serif, mono, light

    var id: String { rawValue }
    var title: String { L("look.font.\(rawValue)") }
    static var current: ClockFont { ClockFont(rawValue: UserDefaults.standard.string(forKey: "clockFont") ?? "") ?? .rounded }

    func font(_ size: CGFloat, weight: Font.Weight = .bold) -> Font {
        switch self {
        case .rounded: .system(size: size, weight: weight, design: .rounded)
        case .standard: .system(size: size, weight: weight, design: .default)
        case .serif: .system(size: size, weight: weight, design: .serif)
        case .mono: .system(size: size, weight: weight, design: .monospaced)
        case .light: .system(size: size, weight: .thin, design: .default)
        }
    }
}

/// Tam ekran arka plan fotoğrafı: yalnız telefonda (Application Support), en uzun kenar 1600 px JPEG
enum FullscreenPhoto {
    static let versionKey = "fsPhoto"
    private static let name = "fs-photo.jpg"
    static var url: URL { PhotoFile.url(name) }
    static var image: UIImage? { PhotoFile.image(name, version: UserDefaults.standard.double(forKey: versionKey)) }

    static func save(_ data: Data) -> Bool {
        guard PhotoFile.save(data, name: name, maxSide: 1600) else { return false }
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: versionKey)
        return true
    }

    static func remove() {
        PhotoFile.remove(name)
        UserDefaults.standard.set(0.0, forKey: versionKey)
    }

    // MARK: Fotoğraf slaytı (rakip analizi #24): ana fotoğrafa ek en çok 10 fotoğraf, `fsSlideEvery` saniyede bir sırayla
    static let slidesKey = "fsSlides", slideVersionKey = "fsSlideV", slideEveryKey = "fsSlideEvery"
    static let maxSlides = 10
    static let intervals = [10, 30, 60, 300]
    private static func slideName(_ i: Int) -> String { "fs-slide-\(i).jpg" }
    static var slideCount: Int { UserDefaults.standard.integer(forKey: slidesKey) }
    static var hasAny: Bool { UserDefaults.standard.double(forKey: versionKey) > 0 || slideCount > 0 }

    /// Seçilen fotoğrafları slayda ekler; eklenen sayıyı döner
    static func addSlides(_ items: [Data]) -> Int {
        var n = slideCount, added = 0
        for d in items where n < maxSlides {
            if PhotoFile.save(d, name: slideName(n), maxSide: 1600) { n += 1; added += 1 }
        }
        UserDefaults.standard.set(n, forKey: slidesKey)
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: slideVersionKey)
        return added
    }

    static func clearSlides() {
        for i in 0..<maxSlides { PhotoFile.remove(slideName(i)) }
        UserDefaults.standard.set(0, forKey: slidesKey)
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: slideVersionKey)
    }

    /// O anda gösterilecek fotoğraf (ana fotoğraf + slaytlar sırayla)
    static func image(at date: Date) -> UIImage? {
        let d = UserDefaults.standard
        var list: [(String, Double)] = []
        let v = d.double(forKey: versionKey), sv = d.double(forKey: slideVersionKey)
        if v > 0 { list.append((name, v)) }
        for i in 0..<slideCount { list.append((slideName(i), sv)) }
        guard !list.isEmpty else { return nil }
        let every = max(5, d.integer(forKey: slideEveryKey) == 0 ? 30 : d.integer(forKey: slideEveryKey))
        let i = Int(date.timeIntervalSince1970 / Double(every)) % list.count
        return PhotoFile.image(list[i].0, version: list[i].1)
    }
}

/// Tam ekranda yön kilidi (rakip analizi #22): AppDelegate bu maskeyi döner
@MainActor
enum OrientationLock {
    static var mask: UIInterfaceOrientationMask = UIDevice.current.userInterfaceIdiom == .pad ? .all : .allButUpsideDown

    static func set(_ m: UIInterfaceOrientationMask) {
        mask = m
        guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first else { return }
        for w in scene.windows { w.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations() }
        scene.requestGeometryUpdate(.iOS(interfaceOrientations: m)) { _ in }
    }

    /// Şu anki yöne kilitle
    static func lockCurrent() {
        let o = UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.interfaceOrientation }.first ?? .portrait
        switch o {
        case .landscapeLeft: set(.landscapeLeft)
        case .landscapeRight: set(.landscapeRight)
        case .portraitUpsideDown: set(.portraitUpsideDown)
        default: set(.portrait)
        }
    }

    static func unlock() { set(UIDevice.current.userInterfaceIdiom == .pad ? .all : .allButUpsideDown) }
}

/// Telefonda saklanan küçültülmüş fotoğraflar (tam ekran saat, geri sayımlar): Application Support altında JPEG.
/// Okunan görüntü sürüm numarasıyla önbellekte tutulur (saniyede yenilenen ekranlar dosyayı tekrar okumasın).
enum PhotoFile {
    private static let cache = NSCache<NSString, UIImage>()

    static func url(_ name: String) -> URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent(name)
    }

    static func image(_ name: String, version: Double) -> UIImage? {
        guard version > 0 else { return nil }
        let key = "\(name)#\(version)" as NSString
        if let img = cache.object(forKey: key) { return img }
        guard let img = UIImage(contentsOfFile: url(name).path) else { return nil }
        cache.setObject(img, forKey: key)
        return img
    }

    /// En uzun kenarı `maxSide` piksele küçültüp JPEG olarak yazar
    static func save(_ data: Data, name: String, maxSide: CGFloat) -> Bool {
        guard let img = UIImage(data: data) else { return false }
        let scale = min(1, maxSide / max(img.size.width, img.size.height))
        let size = CGSize(width: img.size.width * scale, height: img.size.height * scale)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let out = UIGraphicsImageRenderer(size: size, format: format).image { _ in img.draw(in: CGRect(origin: .zero, size: size)) }
        guard let jpg = out.jpegData(compressionQuality: 0.8) else { return false }
        return (try? jpg.write(to: url(name), options: .atomic)) != nil
    }

    static func remove(_ name: String) {
        try? FileManager.default.removeItem(at: url(name))
    }
}

/// Ayarlar › Saat görünümü: yazı tipi (canlı örnek) ve tam ekran fotoğrafı
struct AppearanceView: View {
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("clockFont") private var clockFont = ClockFont.rounded.rawValue
    @AppStorage(FullscreenPhoto.versionKey) private var photoVersion = 0.0
    @AppStorage(FullscreenBackground.key) private var bgRaw = ""
    @AppStorage(NightMode.key) private var nightRaw = "off"
    @AppStorage(ClockDigits.colorKey) private var digitColor = "theme"
    @AppStorage(ClockDigits.outlineKey) private var digitOutline = false
    @AppStorage(FlipLook.pairKey) private var flipPair = false
    @AppStorage(FlipLook.smallSecKey) private var flipSmallSec = false
    @AppStorage(FlipLook.hingeKey) private var flipHinge = true
    @AppStorage(FlipLook.cardKey) private var flipCard = "theme"

    private func digitSwatch(_ id: String) -> some View {
        let color: Color = ClockDigits.color(id)
        return swatch(color, L("color." + id), digitColor == id) { digitColor = id }
    }

    private func cardSwatch(_ id: String) -> some View {
        let color: Color = FlipLook.colors(id).bg
        let label: String = id == "theme" ? L("wall.none") : L("color." + id)
        return swatch(color, label, flipCard == id) { flipCard = id }
    }

    /// Gece yazı rengi: siyah zemin üstünde kısık renk
    private func nightSwatch(_ id: String) -> some View {
        let selected: Bool = id == nightColor
        return Button { nightColor = id } label: {
            ZStack {
                Circle().fill(Color.black)
                Circle().fill(NightMode.color(id)).padding(6)
                Circle().stroke(Theme.text, lineWidth: selected ? 3 : 0)
            }
            .frame(width: 30, height: 30)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L("color." + id))
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    /// Renk seçimi dairesi (seçiliyse kalın kenar)
    private func swatch(_ color: Color, _ label: String, _ selected: Bool, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Circle().fill(color).frame(width: 28, height: 28)
                .overlay(Circle().stroke(selected ? Theme.text : Theme.border, lineWidth: selected ? 3 : 1).padding(-4))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
    @AppStorage(NightMode.colorKey) private var nightColor = "red"
    @AppStorage(NightMode.fromKey) private var nightFrom = 22 * 60
    @AppStorage(NightMode.toKey) private var nightTo = 7 * 60

    /// Gece yarısından dakika → "22:00" (telefonun 12/24 ayarıyla)
    static func hm(_ m: Int) -> String {
        Fmt.time(Calendar.current.date(bySettingHour: m / 60, minute: m % 60, second: 0, of: Date()) ?? Date())
    }
    @AppStorage("fs.text") private var personalText = ""
    @AppStorage("fs.scale") private var scale = 1.0
    @AppStorage(FullscreenPhoto.slidesKey) private var slideCount = 0
    @AppStorage(FullscreenPhoto.slideEveryKey) private var slideEvery = 30
    @State private var item: PhotosPickerItem?
    @State private var slideItems: [PhotosPickerItem] = []
    // Saniye hareketi, küçük saniye, tık sesi, tabela, ikinci saat dilimi (2. analiz #10, #21, #22, #12)
    @AppStorage(SecondsMotion.key) private var motionRaw = SecondsMotion.tick.rawValue
    @AppStorage("secSmall") private var secSmall = false
    @AppStorage(TickSound.key) private var tickSound = false
    @AppStorage("fs.banner") private var banner = false
    @AppStorage(DualZone.key) private var dualCity = ""
    @AppStorage(WeatherScene.clockKey) private var weatherClock = false
    @AppStorage(WeatherScene.tabKey) private var weatherTab = true
    @AppStorage(LiveActivities.clockKey) private var liveClock = false

    private var background: FullscreenBackground { FullscreenBackground(rawValue: bgRaw) ?? (photoVersion > 0 ? .photo : .plain) }

    var body: some View {
        Form {
            // Kadran koleksiyonu (2. analiz #1)
            Section {
                NavigationLink { DialGalleryView() } label: {
                    LabeledContent {
                        Text(DialFace.current.title)
                    } label: {
                        Label(L("dial.title"), systemImage: "clock")
                    }
                }
                // Widget galerisi (2. analiz #166)
                NavigationLink { WidgetGalleryView() } label: {
                    Label(L("widgets.gallery"), systemImage: "square.grid.2x2")
                }
            }
            // Dynamic Island'da canlı saat (2. analiz #19)
            Section {
                Toggle(L("live.clock"), isOn: $liveClock)
                    .tint(Theme.accent)
                    .onChange(of: liveClock) { _, _ in LiveActivities.sync() }
            } footer: {
                Text(L("live.clockNote"))
            }
            Section(L("look.font")) {
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    Text(Fmt.time(context.date, seconds: true))
                        .font((ClockFont(rawValue: clockFont) ?? .rounded).font(52))
                        .monospacedDigit()
                        .foregroundStyle(Theme.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .frame(maxWidth: .infinity)
                        .environment(\.layoutDirection, .leftToRight)
                }
                Picker(L("look.font"), selection: $clockFont) {
                    ForEach(ClockFont.allCases) { f in Text(f.title).tag(f.rawValue) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                // Rakam rengi ve dış hatlı rakamlar (2. analiz #24): dijital saatlerde
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("look.digitColor")).font(.subheadline).foregroundStyle(Theme.muted)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            swatch(Theme.ink, L("wall.none"), digitColor == "theme") { digitColor = "theme" }
                            ForEach(ClockDigits.colors.map(\.id), id: \.self) { id in digitSwatch(id) }
                        }
                        .padding(4)
                    }
                }
                Toggle(L("look.outline"), isOn: $digitOutline)
            }
            // Dijital rakam görünümü (2. analiz #9)
            Section(L("digital.style")) {
                DigitalStylePicker()
            }
            // Çevirmeli saat (2. analiz #20): iki rakam tek kart, köşede küçük saniye, kart çizgisi, kart rengi
            Section(L("stage.models.flip")) {
                Toggle(L("flip.pair"), isOn: $flipPair)
                Toggle(L("flip.smallSec"), isOn: $flipSmallSec)
                Toggle(L("flip.hinge"), isOn: $flipHinge)
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("flip.card")).font(.subheadline).foregroundStyle(Theme.muted)
                    HStack(spacing: 10) {
                        ForEach(FlipLook.cards.map(\.id), id: \.self) { id in cardSwatch(id) }
                    }
                    .padding(4)
                }
            }
            Section {
                TimelineView(.animation(minimumInterval: (SecondsMotion(rawValue: motionRaw) ?? .tick).interval)) { context in
                    AnalogClock(date: context.date, showSeconds: true, motion: SecondsMotion(rawValue: motionRaw) ?? .tick)
                        .frame(height: 120)
                        .frame(maxWidth: .infinity)
                }
                Picker(L("look.motion"), selection: $motionRaw) {
                    ForEach(SecondsMotion.allCases) { m in Text(L("look.motion.\(m.rawValue)")).tag(m.rawValue) }
                }
                Toggle(L("look.secSmall"), isOn: $secSmall)
                Toggle(L("look.tickSound"), isOn: $tickSound)
            } header: {
                Text(L("look.seconds"))
            } footer: {
                Text(L("look.secondsNote"))
            }
            Section {
                Picker(L("dual.city"), selection: Binding(get: { DualZone.city(dualCity)?.key ?? "" }, set: { dualCity = $0 })) {
                    ForEach(City.all) { c in Text(c.displayName).tag(c.key) }
                }
            } header: {
                Text(L("dual.model"))
            } footer: {
                Text(L("dual.note"))
            }
            AppIconSection()
            Section {
                // Canlı örnek: seçili arka plan ve saat
                preview
                Picker(L("look.photo"), selection: Binding(get: { background.rawValue }, set: { bgRaw = $0 })) {
                    ForEach(FullscreenBackground.allCases) { b in
                        Text(b.title).tag(b.rawValue)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                PhotosPicker(selection: $item, matching: .images) { Label(L("look.photo.pick"), systemImage: "photo") }
                if photoVersion > 0 {
                    Button(L("look.photo.remove"), role: .destructive) {
                        FullscreenPhoto.remove()
                        photoVersion = 0
                        if background == .photo && slideCount == 0 { bgRaw = FullscreenBackground.plain.rawValue }
                    }
                }
            } header: {
                Text(L("look.photo"))
            } footer: {
                Text(L("look.photo.note") + " " + L("look.bg.note") + " " + L("look.bg.weatherNote"))
            }
            FloatingClockSection()
            // Fotoğraf slaytı: ana fotoğrafa ek en çok 10 fotoğraf sırayla
            Section {
                if FullscreenPhoto.maxSlides - slideCount > 0 {
                    PhotosPicker(selection: $slideItems, maxSelectionCount: FullscreenPhoto.maxSlides - slideCount, matching: .images) {
                        Label(L("look.slideAdd"), systemImage: "photo.stack")
                    }
                }
                if slideCount > 0 {
                    Text(T("look.slideCount", slideCount)).foregroundStyle(Theme.muted)
                    Picker(L("look.slideEvery"), selection: $slideEvery) {
                        ForEach(FullscreenPhoto.intervals, id: \.self) { s in
                            Text(s < 60 ? "\(s) " + L("common.secondsShort") : "\(s / 60) " + L("client.minuteShort")).tag(s)
                        }
                    }
                    Button(L("look.slideClear"), role: .destructive) {
                        FullscreenPhoto.clearSlides()
                        slideCount = 0
                        if background == .photo && photoVersion == 0 { bgRaw = FullscreenBackground.plain.rawValue }
                    }
                }
            } header: {
                Text(L("look.slideshow"))
            } footer: {
                Text(L("look.slideNote"))
            }
            // Saatin altındaki kişisel metin ve tam ekranda saat boyutu (rakip analizi #25, #22)
            Section {
                TextField(L("look.textPlaceholder"), text: Binding(get: { personalText }, set: { personalText = String($0.prefix(60)) }))
                Toggle(L("look.banner"), isOn: $banner)
            } header: {
                Text(L("look.text"))
            } footer: {
                Text(L("look.textNote") + " " + L("look.bannerNote"))
            }
            Section {
                Slider(value: $scale, in: 0.5...1.3, step: 0.05) { Text(L("look.size")) }
                Text(Fmt.percent(Int((scale * 100).rounded()))).monospacedDigit().foregroundStyle(Theme.muted)
            } header: {
                Text(L("look.size"))
            } footer: {
                Text(L("look.fsNote") + " " + L("fs.toolsNote"))
            }
            // Hava neyse ekran öyle (2. analiz #193): saat ekranının zemini ve hava sekmesinin üst kartı
            Section {
                Toggle(L("look.weatherClock"), isOn: $weatherClock)
                Toggle(L("look.weatherTab"), isOn: $weatherTab)
            } header: {
                Text(L("look.weatherAnim"))
            } footer: {
                Text(L("look.weatherAnimNote"))
            }
            // Gece / yatak başı modu (tam ekran saat)
            Section {
                Picker(L("night.title"), selection: $nightRaw) {
                    ForEach(NightMode.choices, id: \.self) { Text(L("night.\($0)")).tag($0) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                // Belirli saatler (2. analiz #11): başlangıç ve bitiş, yarım saatlik adımlarla
                if nightRaw == "schedule" {
                    Picker(L("night.from"), selection: $nightFrom) {
                        ForEach(0..<48, id: \.self) { i in Text(Self.hm(i * 30)).tag(i * 30) }
                    }
                    Picker(L("night.to"), selection: $nightTo) {
                        ForEach(0..<48, id: \.self) { i in Text(Self.hm(i * 30)).tag(i * 30) }
                    }
                }
                // Gece yazı rengi (2. analiz #189): 7 renk, siyah zemin üstünde kısık
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("night.color")).font(.subheadline).foregroundStyle(Theme.muted)
                    HStack(spacing: 10) {
                        ForEach(NightMode.colors.map(\.id), id: \.self) { id in nightSwatch(id) }
                    }
                }
            } header: {
                Text(L("night.title"))
            } footer: {
                Text(L("night.note"))
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("look.title"))
        .navigationBarTitleDisplayMode(.large)
        .onChange(of: slideItems) { _, new in
            guard !new.isEmpty else { return }
            Task {
                var datas: [Data] = []
                for it in new { if let d = try? await it.loadTransferable(type: Data.self) { datas.append(d) } }
                if FullscreenPhoto.addSlides(datas) > 0 {
                    slideCount = FullscreenPhoto.slideCount
                    bgRaw = FullscreenBackground.photo.rawValue
                }
                slideItems = []
            }
        }
        .onChange(of: item) { _, new in
            guard let new else { return }
            Task {
                if let data = try? await new.loadTransferable(type: Data.self), FullscreenPhoto.save(data) {
                    photoVersion = UserDefaults.standard.double(forKey: FullscreenPhoto.versionKey)
                    bgRaw = FullscreenBackground.photo.rawValue
                }
                item = nil
            }
        }
    }

    /// Seçili arka planın küçük örneği (fotoğraf yoksa "Fotoğraf"ta düz zemin)
    @ViewBuilder private var preview: some View {
        let bg = background
        ZStack {
            if bg == .photo, photoVersion > 0, let img = FullscreenPhoto.image {
                Image(uiImage: img).resizable().scaledToFill().overlay(Color.black.opacity(0.45)).id(photoVersion)
            } else if bg.animated {
                AnimatedBackground(kind: bg)
            } else {
                Theme.bg
            }
            TimelineView(.periodic(from: .now, by: 1)) { context in
                Text(Fmt.time(context.date))
                    .font((ClockFont(rawValue: clockFont) ?? .rounded).font(44))
                    .monospacedDigit()
                    .foregroundStyle(Theme.ink)
                    .environment(\.layoutDirection, .leftToRight)
            }
        }
        .frame(height: 160)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .environment(\.colorScheme, bg == .plain || (bg == .photo && photoVersion == 0) ? colorScheme : .dark)
    }
}

/// Alternatif uygulama simgeleri (2. analiz #13): ana ekrandaki simge seçilir (iOS onay mesajı gösterir).
/// Simgeler `Assets.xcassets/AppIcon-<ad>` (üretici `mobile/ios/scripts/gen-alt-icons.py`).
struct AppIconSection: View {
    static let names = ["night", "paper", "flip", "mint"]
    @State private var current = UIApplication.shared.alternateIconName

    var body: some View {
        if UIApplication.shared.supportsAlternateIcons {
            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 14) {
                        option(nil, title: L("icon.default"), preview: "AppIconPreview")
                        ForEach(Self.names, id: \.self) { n in
                            option("AppIcon-\(n)", title: L("icon.\(n)"), preview: "AppIconPreview-\(n)")
                        }
                    }
                    .padding(.vertical, 6)
                }
            } header: {
                Text(L("icon.title"))
            } footer: {
                Text(L("icon.note"))
            }
        }
    }

    private func option(_ name: String?, title: String, preview: String) -> some View {
        Button {
            UIApplication.shared.setAlternateIconName(name) { error in
                if error == nil { current = name }
            }
        } label: {
            VStack(spacing: 6) {
                Image(preview).resizable().frame(width: 60, height: 60)
                    .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .stroke(current == name ? Theme.accent : Theme.border, lineWidth: current == name ? 3 : 1))
                Text(title).font(.caption).foregroundStyle(current == name ? Theme.accent : Theme.text)
            }
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(current == name ? .isSelected : [])
    }
}
