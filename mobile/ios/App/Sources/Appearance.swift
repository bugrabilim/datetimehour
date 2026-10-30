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
    static var url: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("fs-photo.jpg")
    }
    static var image: UIImage? { UIImage(contentsOfFile: url.path) }

    static func save(_ data: Data) -> Bool {
        guard let img = UIImage(data: data) else { return false }
        let scale = min(1, 1600 / max(img.size.width, img.size.height))
        let size = CGSize(width: img.size.width * scale, height: img.size.height * scale)
        let out = UIGraphicsImageRenderer(size: size).image { _ in img.draw(in: CGRect(origin: .zero, size: size)) }
        guard let jpg = out.jpegData(compressionQuality: 0.8) else { return false }
        try? jpg.write(to: url, options: .atomic)
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: versionKey)
        return true
    }

    static func remove() {
        try? FileManager.default.removeItem(at: url)
        UserDefaults.standard.set(0.0, forKey: versionKey)
    }
}

/// Ayarlar › Saat görünümü: yazı tipi (canlı örnek) ve tam ekran fotoğrafı
struct AppearanceView: View {
    @AppStorage("clockFont") private var clockFont = ClockFont.rounded.rawValue
    @AppStorage(FullscreenPhoto.versionKey) private var photoVersion = 0.0
    @State private var item: PhotosPickerItem?

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
                if photoVersion > 0, let img = FullscreenPhoto.image {
                    Image(uiImage: img).resizable().scaledToFill()
                        .frame(height: 160).frame(maxWidth: .infinity).clipped()
                        .overlay(Color.black.opacity(0.45))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .id(photoVersion)
                }
                PhotosPicker(selection: $item, matching: .images) { Label(L("look.photo.pick"), systemImage: "photo") }
                if photoVersion > 0 {
                    Button(L("look.photo.remove"), role: .destructive) {
                        FullscreenPhoto.remove()
                        photoVersion = 0
                    }
                }
            } header: {
                Text(L("look.photo"))
            } footer: {
                Text(L("look.photo.note"))
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
                }
                item = nil
            }
        }
    }
}
