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
            Image(systemName: icon)
                .font(.system(size: 56, weight: .light))
                .frame(width: 64, height: 64)
                .foregroundStyle(Theme.accent.opacity(0.6))
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

/// Liste satırlarındaki renkli simge kutusu (#99): 36 pt, köşe 10 pt, zemin ana rengin açık tonu
struct IconBox: View {
    let icon: String
    var tint: Color = Theme.accent

    var body: some View {
        Image(systemName: icon)
            .font(.system(size: 17, weight: .medium))
            .foregroundStyle(tint)
            .frame(width: 36, height: 36)
            .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(tint.opacity(0.15)))
            .accessibilityHidden(true)
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
