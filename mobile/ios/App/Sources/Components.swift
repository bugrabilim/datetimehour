import SwiftUI

/*
 * Ortak görünüm parçaları (2. analiz #96, #99; 3 Kasım 2026). Android'deki Components.kt ile aynı ölçüler.
 */

/// Boş durum (#96): 64 pt çizgi simge (ana renk %60), 17 pt başlık, soluk açıklama ve hap biçimli eylem düğmesi.
/// Başlık ya da eylem verilmezse o satır çizilmez.
struct EmptyState: View {
    let icon: String
    var title: String?
    var text: String?
    var action: String?
    var onAction: (() -> Void)?

    var body: some View {
        VStack(spacing: 8) {
            // Küçük resim (doğal tasarım madde 18): çakıl taşı zemin, çizgi simge, köşede küçük güneş
            ZStack {
                PebbleShape().fill(Theme.accentSoft).frame(width: 88, height: 88)
                Circle().fill(Theme.gold.opacity(0.55)).frame(width: 18, height: 18).offset(x: 40, y: -40)
                if let line = LineIconMap.of(icon) {
                    LineIcon(name: line, size: 48, c1: Theme.accent, c2: Theme.sage)
                } else {
                    Image(systemName: icon).font(.system(size: 40, weight: .light)).foregroundStyle(Theme.accent)
                }
            }
            .frame(width: 104, height: 104)
            .accessibilityHidden(true)
            if let title, !title.isEmpty {
                Text(title).font(AppFonts.cardTitle).foregroundStyle(Theme.text)
            }
            if let text, !text.isEmpty {
                Text(text).font(.subheadline).foregroundStyle(Theme.muted)
            }
            if let action, let onAction {
                Button(action: onAction) {
                    Text(action)
                        .font(.subheadline.weight(.medium))
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Theme.accent))
                        .foregroundStyle(Theme.onAccent)
                }
                .buttonStyle(.plain)
                .padding(.top, 4)
            }
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 24)
        .padding(.vertical, 28)
    }
}

/// Liste satırlarındaki renkli simge kutusu (#99; doğal tasarım madde 4, 4 Kasım 2026): "çakıl taşı" biçimi (köşeler
/// hafif farklı yuvarlak), zemin simge renginin açık tonu, içinde 3D simge (4 Kasım 2026; karşılığı yoksa SF Symbol).
/// `line` verilirse o simge çizilir.
struct IconBox: View {
    let icon: String
    var tint: Color = Theme.accent
    var line: String?
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        Group {
            if let name = line ?? LineIconMap.of(icon) {
                LineIcon(name: name, size: 25, c1: tint, c2: tint.opacity(0.72))
            } else {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(tint)
            }
        }
        .frame(width: 36, height: 36)
        .background(PebbleShape().fill(tint.opacity(scheme == .dark ? 0.20 : 0.13)))
        .accessibilityHidden(true)
    }
}

/// Çakıl taşı biçimi (doğal tasarım bölüm 3.3)
struct PebbleShape: Shape {
    func path(in r: CGRect) -> Path {
        let w = r.width
        return UnevenRoundedRectangle(topLeadingRadius: w * 0.44, bottomLeadingRadius: w * 0.48,
                                      bottomTrailingRadius: w * 0.46, topTrailingRadius: w * 0.5, style: .continuous).path(in: r)
    }
}

/// Simge kutulu satır etiketi: `Label` yerine (Form/List satırlarında aynı hizada)
struct BoxLabel: View {
    let title: String
    let icon: String
    var tint: Color = Theme.accent

    init(_ title: String, icon: String, tint: Color = Theme.accent) {
        self.title = title
        self.icon = icon
        self.tint = tint
    }

    var body: some View {
        Label { Text(title).foregroundStyle(Theme.text) } icon: { IconBox(icon: icon, tint: tint) }
    }
}

/// Araç ekranı kalıbı (2. analiz #200): altta başparmak bölgesinde tam genişlikte tek ana düğme (isteğe bağlı solda
/// ikincil, soluk düğme); ikincil işler sağ üstte "…" menüsünde (MoreMenu)
struct ToolPrimaryBar: View {
    let primary: String
    var secondary: String?
    var onSecondary: () -> Void = {}
    let onPrimary: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            if let secondary {
                RoundButton(title: secondary, action: onSecondary)
            }
            RoundButton(title: primary, prominent: true, action: onPrimary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(.bar)
    }
}

/// "…" menüsündeki bir iş
struct MoreItem: Identifiable {
    let id = UUID()
    let title: String
    let icon: String
    var destructive = false
    let action: () -> Void
}

/// "…" menüsü: paylaş, dışa aktar, sıfırla, ayarlar gibi ikincil işler
struct MoreMenu: View {
    let items: [MoreItem]

    var body: some View {
        Menu {
            ForEach(items) { item in
                Button(role: item.destructive ? .destructive : nil, action: item.action) {
                    Label(item.title, systemImage: item.icon)
                }
            }
        } label: {
            Image(systemName: "ellipsis.circle")
        }
        .accessibilityLabel(L("timers.more"))
    }
}

/// Hareket belirteçleri (doğal tasarım madde 12, 4 Kasım 2026): kısa 0,15, orta 0,25, uzun 0,4, sahne 1,2 sn; yumuşak eğri
/// (0.2, 0.8, 0.2, 1). Hareketi azalt açıksa yalnız soluklaşma kullanılır.
enum Motion {
    static let short = Animation.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.15)
    static let medium = Animation.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.25)
    static let long = Animation.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.4)
}

/// Basınca hafif küçülme (0,97): kartlar ve kareler için
struct PressScaleStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.97 : 1)
            .opacity(configuration.isPressed && reduceMotion ? 0.8 : 1)
            .animation(Motion.short, value: configuration.isPressed)
    }
}

/// "Nasıl çalışır?" (doğal tasarım madde 13 ve ilke 4, 4 Kasım 2026): ekranda en çok iki satırlık not kalır; daha uzun
/// açıklamalar bu bağlantının açtığı alt sayfadadır. Bölüm başlığı (isteğe bağlı) ve metin. Android'deki HowItWorks ile aynı.
struct HowItWorks: View {
    let sections: [(String?, String)]
    @State private var open = false

    var body: some View {
        Button { open = true } label: {
            HStack(spacing: 6) {
                LineIcon(name: "info", size: 18, c1: Theme.accent, c2: Theme.accent)
                Text(L("common.how")).font(.subheadline.weight(.medium)).foregroundStyle(Theme.accent)
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L("common.how"))
        .sheet(isPresented: $open) {
            HowItWorksSheet(sections: sections)
                .presentationDetents([.medium, .large])
        }
    }
}

/// "Nasıl çalışır?" alt sayfası: sola hizalı, okunaklı satır aralığı
struct HowItWorksSheet: View {
    let sections: [(String?, String)]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(Array(sections.enumerated()), id: \.offset) { _, section in
                        if let title = section.0 {
                            Text(title).font(.headline).foregroundStyle(Theme.text).padding(.top, 8)
                        }
                        Text(section.1)
                            .font(.body)
                            .foregroundStyle(Theme.text)
                            .lineSpacing(4)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
            .background(Theme.surface)
            .navigationTitle(L("common.how"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } }
            }
        }
    }
}
