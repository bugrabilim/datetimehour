import PhotosUI
import SwiftUI

/// Şeffaf widget ayarı (2. analiz #76): ana ekranın boş sayfasının ekran görüntüsü seçilir, uygulama her widget yeri için
/// duvar kâğıdından parçayı keser (WallpaperStore). Widget'ta görünüm "Şeffaf" ve "Ekrandaki yeri" seçilir.
/// Yalnız iPhone (iPad'in ana ekran düzeni farklı). Görüntü yalnız telefonda, App Group klasöründe kalır.
struct WallpaperView: View {
    @State private var item: PhotosPickerItem?
    @State private var meta = WallpaperStore.meta()
    @State private var message: String?
    @State private var busy = false
    @State private var version = 0

    private var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }
    /// Dikey ekranın piksel boyutu ve nokta başına piksel (ince ayar noktayla gösterilir, pikselle saklanır)
    private var screen: CGSize { UIScreen.main.nativeBounds.size }
    private var scale: Double { Double(UIScreen.main.nativeScale) }

    var body: some View {
        List {
            Section {
                Text(L("wallpaper.intro")).font(.subheadline).foregroundStyle(Theme.text)
                Text(L("wallpaper.steps")).font(.footnote).foregroundStyle(Theme.muted)
            }
            if isPad {
                Section { Text(L("wallpaper.unavailable")).foregroundStyle(Theme.err) }
            } else {
                Section {
                    PhotosPicker(selection: $item, matching: .screenshots) {
                        Label(L("wallpaper.pick"), systemImage: "photo.on.rectangle")
                    }
                    .disabled(busy)
                    if busy { ProgressView() }
                    if let message { Text(message).font(.footnote).foregroundStyle(Theme.err) }
                }
                if let m = meta {
                    Section {
                        Label(L("wallpaper.ready"), systemImage: "checkmark.circle.fill").foregroundStyle(Theme.ok)
                        if m.estimated { Text(L("wallpaper.estimated")).font(.footnote).foregroundStyle(Theme.muted) }
                        preview
                        Picker(L("wallpaper.text"), selection: Binding(get: { m.text }, set: { apply(text: $0, dx: m.dx, dy: m.dy) })) {
                            Text(L("wallpaper.text.auto")).tag("auto")
                            Text(L("wallpaper.text.light")).tag("light")
                            Text(L("wallpaper.text.dark")).tag("dark")
                        }
                    }
                    Section {
                        Stepper(value: Binding(get: { Double(m.dx) / scale }, set: { apply(text: m.text, dx: Int(($0 * scale).rounded()), dy: m.dy) }),
                                in: -40...40, step: 1) {
                            Text("\(L("wallpaper.adjustX")): \(Int((Double(m.dx) / scale).rounded()))")
                        }
                        Stepper(value: Binding(get: { Double(m.dy) / scale }, set: { apply(text: m.text, dx: m.dx, dy: Int(($0 * scale).rounded())) }),
                                in: -40...40, step: 1) {
                            Text("\(L("wallpaper.adjustY")): \(Int((Double(m.dy) / scale).rounded()))")
                        }
                    } header: {
                        Text(L("wallpaper.adjust"))
                    }
                    Section {
                        Button(role: .destructive) {
                            WallpaperStore.remove()
                            meta = nil
                        } label: { Text(L("wallpaper.remove")) }
                    }
                }
            }
            Section { Text(L("wallpaper.privacy")).font(.footnote).foregroundStyle(Theme.muted) }
        }
        .themedBackground()
        .navigationTitle(L("wallpaper.title"))
        .onChange(of: item) { _, new in
            guard let new else { return }
            busy = true
            message = nil
            Task {
                defer { busy = false; item = nil }
                guard let data = try? await new.loadTransferable(type: Data.self), let img = UIImage(data: data) else {
                    message = L("wallpaper.wrongSize")
                    return
                }
                let old = meta
                handle(WallpaperStore.build(from: img, screen: screen, text: old?.text ?? "auto", dx: old?.dx ?? 0, dy: old?.dy ?? 0))
            }
        }
    }

    /// Altı kare yerin önizlemesi (ana ekrandaki dizilişle)
    private var preview: some View {
        let cols = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
        return LazyVGrid(columns: cols, spacing: 12) {
            ForEach(WidgetSpot.allCases, id: \.self) { spot in
                if let img = WallpaperStore.image(.systemSmall, spot) {
                    Image(uiImage: img)
                        .resizable()
                        .scaledToFit()
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(Text("12:45").font(.system(size: 22, weight: .bold, design: .rounded))
                            .foregroundStyle(WallpaperStore.textColor(spot)))
                        .accessibilityLabel(L("widget.spot.\(spot.rawValue)"))
                }
            }
        }
        .id(version)
        .padding(.vertical, 4)
    }

    private func apply(text: String, dx: Int, dy: Int) {
        handle(WallpaperStore.rebuild(text: text, dx: dx, dy: dy))
    }

    private func handle(_ r: WallpaperStore.BuildResult) {
        switch r {
        case .ok:
            meta = WallpaperStore.meta()
            message = nil
            version += 1
        case .wrongSize: message = L("wallpaper.wrongSize")
        case .unavailable: message = L("wallpaper.unavailable")
        }
    }
}
