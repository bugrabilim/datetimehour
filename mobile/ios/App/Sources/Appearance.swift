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
    @State private var item: PhotosPickerItem?

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
                        if background == .photo { bgRaw = FullscreenBackground.plain.rawValue }
                    }
                }
            } header: {
                Text(L("look.photo"))
            } footer: {
                Text(L("look.photo.note") + " " + L("look.bg.note"))
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("look.title"))
        .navigationBarTitleDisplayMode(.inline)
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
