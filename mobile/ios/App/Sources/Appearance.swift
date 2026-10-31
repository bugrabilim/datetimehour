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
    @AppStorage("fs.text") private var personalText = ""
    @AppStorage("fs.scale") private var scale = 1.0
    @AppStorage(FullscreenPhoto.slidesKey) private var slideCount = 0
    @AppStorage(FullscreenPhoto.slideEveryKey) private var slideEvery = 30
    @State private var item: PhotosPickerItem?
    @State private var slideItems: [PhotosPickerItem] = []

    private var background: FullscreenBackground { FullscreenBackground(rawValue: bgRaw) ?? (photoVersion > 0 ? .photo : .plain) }

    var body: some View {
        Form {
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
            }
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
            } header: {
                Text(L("look.text"))
            } footer: {
                Text(L("look.textNote"))
            }
            Section {
                Slider(value: $scale, in: 0.5...1.0, step: 0.05) { Text(L("look.size")) }
                Text("\(Int((scale * 100).rounded()))%").monospacedDigit().foregroundStyle(Theme.muted)
            } header: {
                Text(L("look.size"))
            } footer: {
                Text(L("look.fsNote"))
            }
            // Gece / yatak başı modu (tam ekran saat)
            Section {
                Picker(L("night.title"), selection: $nightRaw) {
                    ForEach(NightMode.choices, id: \.self) { Text(L("night.\($0)")).tag($0) }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            } header: {
                Text(L("night.title"))
            } footer: {
                Text(L("night.note"))
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("look.title"))
        .navigationBarTitleDisplayMode(.inline)
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
