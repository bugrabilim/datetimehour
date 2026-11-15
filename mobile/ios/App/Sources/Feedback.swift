import SwiftUI
import UIKit

/*
 * Hata bildir (sahibin isteği, 15 Kasım 2026): Ayarlar › Destek. Hesap gerekmez; bildirim hesap sunucusuna
 * (`POST /v1/geri-bildirim`) gider, oradan yalnız e-postayla bilgi@bumbagroup.com'a iletilir. Gönderilen: metin,
 * isteğe bağlı e-posta, uygulama sürümü, cihaz türü (iPhone/iPad, iOS sürümü) ve dil. Android'de FeedbackScreen aynı.
 */
@MainActor
enum Feedback {
    static let min = 10
    static let max = 4000
    static var privacyURL: URL {
        URL(string: "https://time.bumba.tr" + (AppLocale.code == "tr" ? "/gizlilik/#hata-bildirimi" : "/\(AppLocale.code)/privacy/#report-problem"))!
    }

    /// nil: gönderildi; değilse gösterilecek metnin anahtarı
    static func send(message: String, email: String) async -> String? {
        let msg = message.trimmingCharacters(in: .whitespacesAndNewlines)
        let mail = email.trimmingCharacters(in: .whitespaces).lowercased()
        if msg.count < min { return "account.fbShort" }
        if !mail.isEmpty && !Account.validEmail(mail) { return "account.err.email" }
        let device = "\(UIDevice.current.model) · iOS \(UIDevice.current.systemVersion)"
        let r = await Account.shared.call("POST", "/v1/geri-bildirim", [
            "mesaj": String(msg.prefix(max)), "eposta": mail, "kaynak": "ios",
            "surum": Support.version, "cihaz": device, "dil": AppLocale.code,
        ])
        if r.code == 200 { return nil }
        if r.code == 0 { return "account.err.network" }
        if r.code == 429 { return "account.err.tooMany" }
        let body = String(data: r.body, encoding: .utf8) ?? ""
        if body.contains("\"kisa\"") { return "account.fbShort" }
        if body.contains("\"eposta\"") { return "account.err.email" }
        return "account.err.generic"
    }
}

struct FeedbackView: View {
    @State private var message = ""
    @State private var email = ""
    @State private var busy = false
    @State private var result: String? // "ok" ya da hata anahtarı

    var body: some View {
        Form {
            Section {
                Text(L("account.fbIntro")).foregroundStyle(Theme.text)
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("account.fbMessage")).font(.subheadline).foregroundStyle(Theme.muted)
                    TextEditor(text: $message)
                        .frame(minHeight: 150)
                        .onChange(of: message) { _, v in if v.count > Feedback.max { message = String(v.prefix(Feedback.max)) } }
                }
                TextField(L("account.fbEmail"), text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if let result {
                    Text(L(result == "ok" ? "account.fbSent" : result))
                        .foregroundStyle(result == "ok" ? Theme.ok : Theme.err)
                }
                RoundButton(title: L("account.fbSend"), prominent: true) {
                    guard !busy else { return }
                    busy = true
                    result = nil
                    Task {
                        let err = await Feedback.send(message: message, email: email)
                        busy = false
                        if err == nil { message = ""; result = "ok" } else { result = err }
                    }
                }
                .disabled(busy)
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("account.fbPrivacy"))
                    Link(L("account.fbPrivacyLink"), destination: Feedback.privacyURL)
                }
            }
        }
        .navigationTitle(L("account.fbTitle"))
        .themedBackground()
    }
}
