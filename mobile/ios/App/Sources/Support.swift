import SwiftUI
import UIKit
import StoreKit
import CoreLocation
import CoreImage.CIFilterBuiltins

/*
 * Destek ve paylaşım, yazı boyutu ve izin öncesi açıklama (3 Kasım 2026, 2. analiz #6, #77, #80, #81; sahibin ekran
 * görüntüleri). Android'deki Support.kt ile aynı kayıt anahtarları (`fontScale`).
 */

// MARK: Destek ve paylaşım (#6, #81)

enum Support {
    static let site = URL(string: "https://time.bumba.tr")!
    static let bumba = URL(string: "https://bumbagroup.com")!

    static var version: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? ""
        return "\(v) (\(b))"
    }

    static var shareText: String { T("support.shareText", L("app.displayName")) }

    /// Geri bildirim iletisi: konu ve yalnız uygulama/sistem sürümü (kullanıcı göndermeden önce görür)
    static var feedbackURL: URL {
        var c = URLComponents()
        c.scheme = "mailto"
        c.path = "bilgi@bumbagroup.com"
        c.queryItems = [
            URLQueryItem(name: "subject", value: T("support.feedbackSubject", L("app.displayName"))),
            URLQueryItem(name: "body", value: "\n\n—\n\(L("app.displayName")) \(version) · iOS \(UIDevice.current.systemVersion)"),
        ]
        return c.url ?? URL(string: "mailto:bilgi@bumbagroup.com")!
    }
}

/// Ayarlar'daki bölüm: puanla, paylaş, QR kod, geri bildirim, Bumba'nın diğer ürünleri, sürüm
struct SupportSection: View {
    @Environment(\.requestReview) private var requestReview

    var body: some View {
        Section {
            Button { requestReview() } label: { Label(L("support.rate"), systemImage: "star") }
            ShareLink(item: Support.site, subject: Text(L("app.displayName")), message: Text(Support.shareText)) {
                Label(L("support.share"), systemImage: "square.and.arrow.up")
            }
            NavigationLink { ShareQRView() } label: { Label(L("support.qr"), systemImage: "qrcode") }
            Link(destination: Support.feedbackURL) { Label(L("support.feedback"), systemImage: "envelope") }
            Link(destination: Support.bumba) { Label(L("support.moreApps"), systemImage: "square.grid.2x2") }
            LabeledContent(L("support.version"), value: Support.version)
        } header: {
            Text(L("support.title"))
        } footer: {
            Text(L("support.feedbackNote"))
        }
    }
}

/// QR kodla paylaşma: kamerayla okutulunca site açılır; altında paylaş ve puanla
struct ShareQRView: View {
    @Environment(\.requestReview) private var requestReview

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                Text(L("app.displayName")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                if let img = QRCode.image(Support.site.absoluteString) {
                    Image(uiImage: img)
                        .interpolation(.none)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 240, height: 240)
                        .padding(18)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 22))
                        .accessibilityLabel(L("support.qr"))
                }
                Text(verbatim: "time.bumba.tr").font(.headline.monospaced()).foregroundStyle(Theme.text)
                Text(L("support.qrNote")).font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                HStack(spacing: 12) {
                    ShareLink(item: Support.site, subject: Text(L("app.displayName")), message: Text(Support.shareText)) {
                        Label(L("support.share"), systemImage: "square.and.arrow.up")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity).padding(.vertical, 10)
                    }
                    .buttonStyle(.borderedProminent)
                    Button { requestReview() } label: {
                        Label(L("support.rate"), systemImage: "star")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity).padding(.vertical, 10)
                    }
                    .buttonStyle(.bordered)
                }
                .tint(Theme.accent)
                Link(destination: Support.bumba) { Label(L("support.moreApps"), systemImage: "square.grid.2x2") }
                    .font(.subheadline)
                    .tint(Theme.accent)
            }
            .padding(24)
            .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("support.qr"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

enum QRCode {
    /// Telefonda üretilir (CoreImage); siyah kareler beyaz zemin üstünde
    static func image(_ text: String) -> UIImage? {
        let f = CIFilter.qrCodeGenerator()
        f.message = Data(text.utf8)
        f.correctionLevel = "M"
        guard let out = f.outputImage?.transformed(by: CGAffineTransform(scaleX: 12, y: 12)),
              let cg = CIContext().createCGImage(out, from: out.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}

// MARK: Yazı boyutu (#77)

/// Uygulamadaki yazılar %85–%145 (5'er adım); %100 telefonun ayarıdır. iOS'ta yazı boyutu basamaklı olduğu için yüzde,
/// telefonun seçili boyutundan birkaç basamak yukarı ya da aşağı kaydırma olarak uygulanır. Sabit punto verilen yazılar
/// (saat rakamları gibi) değişmez.
enum FontScale {
    static let key = "fontScale"
    static let range = 85...145

    static func offset(_ p: Int) -> Int {
        switch p {
        case ..<88: -3
        case ..<93: -2
        case ..<98: -1
        case ..<103: 0
        case ..<113: 1
        case ..<123: 2
        case ..<133: 3
        case ..<143: 4
        default: 5
        }
    }

    static func apply(_ base: DynamicTypeSize, _ p: Int) -> DynamicTypeSize {
        let all = DynamicTypeSize.allCases
        guard let i = all.firstIndex(of: base) else { return base }
        return all[max(0, min(all.count - 1, i + offset(p)))]
    }
}

struct FontScaleModifier: ViewModifier {
    @AppStorage(FontScale.key) private var scale = 100
    @Environment(\.dynamicTypeSize) private var system

    func body(content: Content) -> some View {
        content.dynamicTypeSize(FontScale.apply(system, scale))
    }
}

extension View {
    func appFontScale() -> some View { modifier(FontScaleModifier()) }
}

/// Ayarlar'daki satır: kaydırıcı, yüzde ve canlı örnek metin
struct FontScaleRow: View {
    @AppStorage(FontScale.key) private var scale = 100

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(L("fontScale.title"))
                Spacer()
                Text(Fmt.percent(scale)).monospacedDigit().foregroundStyle(Theme.muted)
            }
            HStack(spacing: 10) {
                Image(systemName: "textformat.size.smaller").foregroundStyle(Theme.muted)
                Slider(value: Binding(get: { Double(scale) }, set: { scale = Int($0.rounded()) }),
                       in: Double(FontScale.range.lowerBound)...Double(FontScale.range.upperBound), step: 5)
                    .accessibilityLabel(L("fontScale.title"))
                    .accessibilityValue(Fmt.percent(scale))
                Image(systemName: "textformat.size.larger").foregroundStyle(Theme.muted)
            }
            Text(L("fontScale.sample"))
                .font(.body)
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 10))
            Text(L("fontScale.note")).font(.caption).foregroundStyle(Theme.muted)
        }
        .padding(.vertical, 4)
    }
}

// MARK: İzin öncesi açıklama (#80)

/// Sistemin izin penceresinden önce kısa bir açıklama: "Şimdi değil" ya da "İzin ver". "Şimdi değil"de sistem penceresi
/// açılmaz (izin daha sonra ilgili ayardan istenebilir). Açıklama, o an en üstteki sayfanın üstüne açılır (ayarlar bir
/// sayfa içindeyken de görünür); aşağı kaydırarak kapatılamaz, iki düğmeden biri seçilir.
@MainActor
enum PermissionPrimer {
    enum Kind: String { case notify, alarm, location }

    private static var showing = false

    static func ask(_ kind: Kind) async -> Bool {
        guard !showing, let top = UIApplication.shared.topViewController else { return true }
        showing = true
        defer { showing = false }
        return await withCheckedContinuation { c in
            var host: UIViewController?
            let view = PermissionPrimerView(kind: kind) { ok in
                host?.dismiss(animated: true) { c.resume(returning: ok) }
                host = nil
            }
            let h = UIHostingController(rootView: view)
            h.modalPresentationStyle = .pageSheet
            h.isModalInPresentation = true
            if let s = h.sheetPresentationController {
                s.detents = [.medium(), .large()]
                s.prefersGrabberVisible = false
            }
            host = h
            top.present(h, animated: true)
        }
    }

    /// Konum izni henüz sorulmadıysa önce açıklama; izin zaten verildiyse ya da reddedildiyse doğrudan devam
    static func location() async -> Bool {
        guard CLLocationManager().authorizationStatus == .notDetermined else { return true }
        return await ask(.location)
    }
}

private struct PermissionPrimerView: View {
    let kind: PermissionPrimer.Kind
    let answer: (Bool) -> Void
    @AppStorage("appearance") private var appearance = "system"

    private var icon: String {
        switch kind {
        case .notify: "bell.badge"
        case .alarm: "alarm"
        case .location: "location"
        }
    }

    var body: some View {
        VStack(spacing: 16) {
            Spacer(minLength: 8)
            Image(systemName: icon)
                .font(.system(size: 40, weight: .regular))
                .foregroundStyle(Theme.accent)
                .frame(width: 84, height: 84)
                .background(Theme.accentSoft, in: Circle())
                .accessibilityHidden(true)
            Text(L("perm.\(kind.rawValue).title"))
                .font(.title3.weight(.bold))
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
            Text(L("perm.\(kind.rawValue).text"))
                .font(.subheadline)
                .foregroundStyle(Theme.muted)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 8)
            Button { answer(true) } label: {
                Text(L("perm.allow")).font(.headline).frame(maxWidth: .infinity).padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            Button { answer(false) } label: {
                Text(L("perm.notNow")).font(.subheadline.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 8)
            }
            .foregroundStyle(Theme.muted)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppBackdrop().ignoresSafeArea())
        .appFontScale()
        .preferredColorScheme(appearance == "light" ? .light : appearance == "dark" ? .dark : nil)
    }
}

extension UIApplication {
    /// Etkin penceredeki en üstteki sayfa (sunulmuş sayfaların en sonuncusu)
    var topViewController: UIViewController? {
        let scenes = connectedScenes.compactMap { $0 as? UIWindowScene }
        let scene = scenes.first { $0.activationState == .foregroundActive } ?? scenes.first
        let window = scene?.windows.first { $0.isKeyWindow } ?? scene?.windows.first
        var vc = window?.rootViewController
        while let next = vc?.presentedViewController, !next.isBeingDismissed { vc = next }
        return vc
    }
}
