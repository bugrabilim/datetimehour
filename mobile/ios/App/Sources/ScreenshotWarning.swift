import SwiftUI
import UIKit

/// Ekran görüntüsü alınınca üstte kısa uyarı (2. rakip analizi #82).
/// Yalnız kişisel kayıt içeren ekranlarda kullanılır: namaz ve oruç takibi, zekât, sabah günlüğü.
/// iOS ekran görüntüsünü engellemez ve görüntüyü uygulamaya vermez; yalnız alındığını haber verir.
/// Hiçbir şey kaydedilmez ya da gönderilmez.
struct ScreenshotWarning: ViewModifier {
    @State private var shown = false
    @State private var hideTask: Task<Void, Never>?

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) {
                if shown {
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "eye").foregroundStyle(Theme.accent)
                            .accessibilityHidden(true)
                        Text(L("screenshot.warn"))
                            .font(.subheadline)
                            .foregroundStyle(Theme.text)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.accent.opacity(0.5)))
                    .shadow(color: .black.opacity(0.15), radius: 8, y: 2)
                    .padding(.horizontal, 12)
                    .padding(.top, 6)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .onTapGesture { hide() }
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.userDidTakeScreenshotNotification)) { _ in
                withAnimation { shown = true }
                UIAccessibility.post(notification: .announcement, argument: L("screenshot.warn"))
                hideTask?.cancel()
                hideTask = Task { @MainActor in
                    try? await Task.sleep(for: .seconds(5))
                    if !Task.isCancelled { hide() }
                }
            }
            .onDisappear { hideTask?.cancel() }
    }

    private func hide() {
        withAnimation { shown = false }
    }
}

extension View {
    /// Kişisel kayıt içeren ekranlarda ekran görüntüsü uyarısı.
    func screenshotWarning() -> some View { modifier(ScreenshotWarning()) }
}
