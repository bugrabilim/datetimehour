import SwiftUI

// MARK: Ayarlar

struct SettingsView: View {
    @AppStorage("h12") private var h12 = false
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("appearance") private var appearance = "system"

    var body: some View {
        Form {
            Section {
                LabeledContent(L("settings.location")) { PlacePicker(font: .body) }
            }
            Section {
                Picker(L("client.formatLabel"), selection: $h12) {
                    Text(L("client.format24")).tag(false)
                    Text(L("client.format12")).tag(true)
                }
                Toggle(L("settings.keepAwake"), isOn: $keepAwake)
                Picker(L("settings.appearance"), selection: $appearance) {
                    Text(L("settings.system")).tag("system")
                    Text(L("settings.light")).tag("light")
                    Text(L("settings.dark")).tag("dark")
                }
                NavigationLink { HomeItemsView() } label: { Label(L("home.customize"), systemImage: "slider.horizontal.3") }
            }
            Section {
                NavigationLink { ListSignupView() } label: { Label(L("list.heading"), systemImage: "envelope") }
            }
        }
        .tint(Theme.accent)
        .navigationTitle(L("settings.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Gizlilik

struct PrivacyView: View {
    var body: some View {
        Form {
            Section {
                Text(L("settings.privacyNote"))
            }
            Section {
                Link(L("privacy.full"), destination: SiteLinks.privacy)
                    .tint(Theme.accent)
            }
        }
        .navigationTitle(L("settings.privacy"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Hakkında

/// Uygulama adı, sürüm, web sitesi ve iletişim; sağ altta sitedeki Bumba Life rozeti (açık/koyu)
struct AboutView: View {
    var body: some View {
        Form {
            Section {
                VStack(spacing: 8) {
                    Image(systemName: "clock")
                        .font(.system(size: 44, weight: .light))
                        .foregroundStyle(Theme.accent)
                    Text(L("app.displayName")).font(.title2.weight(.bold))
                    Text(Self.version).font(.footnote).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }
            Section {
                Link(destination: URL(string: "https://time.bumba.tr")!) {
                    LabeledContent(L("settings.website"), value: "time.bumba.tr")
                }
                Link(destination: URL(string: "mailto:bilgi@bumbagroup.com")!) {
                    LabeledContent(L("about.contact"), value: "bilgi@bumbagroup.com")
                }
            }
            .tint(Theme.text)
        }
        .safeAreaInset(edge: .bottom) {
            HStack {
                Spacer()
                Link(destination: URL(string: "https://bumbagroup.com/life")!) {
                    Image("BumbaLife")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 164, height: 28)
                }
                .accessibilityLabel(L("brand.product"))
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
        }
        .navigationTitle(L("settings.about"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private static var version: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? ""
        return "\(v) (\(b))"
    }
}

/// Sitedeki sayfalar, uygulamanın dilinde
enum SiteLinks {
    static var privacy: URL {
        let lang = AppLocale.code
        let path = lang == "tr" ? "/gizlilik/" : "/\(lang)/privacy/"
        return URL(string: "https://time.bumba.tr" + path)!
    }
}

// MARK: E-posta listesi

/// Ortak Bumba listesine katılım (sitedeki formla aynı alanlar). Onay kutusu işaretli gelmez; ortak servis
/// onay e-postası gönderir. Adres yalnız bu gönderimde kullanılır, uygulamada saklanmaz.
struct ListSignupView: View {
    @State private var email = ""
    @State private var consent = false
    @State private var sending = false
    @State private var result: Bool?

    var body: some View {
        Form {
            Section {
                Text(L("list.lead")).font(.subheadline)
            }
            Section {
                TextField(L("list.emailLabel"), text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Toggle(isOn: $consent) {
                    Text(L("list.consentText")).font(.subheadline)
                }
                .tint(Theme.accent)
                Link(L("list.readNote"), destination: ListService.noteURL)
                    .font(.subheadline)
                    .tint(Theme.accent)
            } footer: {
                Text(L("list.popupText"))
            }
            Section {
                Button {
                    Task { await send() }
                } label: {
                    HStack {
                        Text(L("list.submit")).font(.headline)
                        if sending { Spacer(); ProgressView() }
                    }
                }
                .disabled(!consent || !email.contains("@") || sending)
                .tint(Theme.accent)
            }
            if let result {
                Section {
                    Label(L(result ? "list.ok" : "list.err"), systemImage: result ? "checkmark.circle" : "exclamationmark.triangle")
                        .foregroundStyle(result ? Theme.accent : .red)
                }
            }
        }
        .navigationTitle(L("list.heading"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func send() async {
        sending = true
        result = await ListService.join(email: email.trimmingCharacters(in: .whitespaces))
        sending = false
        if result == true { email = ""; consent = false }
    }
}

enum ListService {
    /// Ortak servisin bildiği diller (sitedeki LIST_LANGS); diğerlerinde İngilizce
    private static let langs = ["tr", "en"]
    private static var lang: String { langs.contains(AppLocale.code) ? AppLocale.code : "en" }

    static var noteURL: URL {
        URL(string: "https://bumbagroup.com/liste/aydinlatma" + (lang == "tr" ? "" : "?dil=en"))!
    }

    /// Servis sonuçta dönüş adresine yönlendirir: "#liste-tamam" ya da "#liste-hata"
    static func join(email: String) async -> Bool {
        var req = URLRequest(url: URL(string: "https://bumbagroup.com/api/liste/katil")!)
        req.httpMethod = "POST"
        req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let source = "https://time.bumba.tr/ios"
        let fields = ["site": "saat-tarih", "eposta": email, "dil": lang, "riza": "on",
                      "kaynak": source, "donus": source, "web_sitesi": ""]
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._*")
        req.httpBody = fields.map { k, v in
            "\(k)=\(v.addingPercentEncoding(withAllowedCharacters: allowed) ?? "")"
        }.joined(separator: "&").data(using: .utf8)
        let session = URLSession(configuration: .ephemeral, delegate: NoRedirect(), delegateQueue: nil)
        defer { session.finishTasksAndInvalidate() }
        guard let (_, response) = try? await session.data(for: req),
              let http = response as? HTTPURLResponse else { return false }
        let location = http.value(forHTTPHeaderField: "Location") ?? ""
        return location.hasSuffix("#liste-tamam")
    }

    private final class NoRedirect: NSObject, URLSessionTaskDelegate {
        func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                        newRequest request: URLRequest) async -> URLRequest? { nil }
    }
}
