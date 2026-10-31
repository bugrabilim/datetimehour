import SwiftUI
import AppIntents

// MARK: Ayarlar

struct SettingsView: View {
    @AppStorage("h12") private var h12 = false
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("appearance") private var appearance = "system"
    @AppStorage("units") private var units = Units.systemDefault
    /// Renk değişince bu sayfa da yeni renkle çizilir
    @AppStorage("accent") private var accent = AccentPalette.defaultID
    @AppStorage(Features.key) private var features = ""
    @State private var syncOn = CloudSync.enabled

    var body: some View {
        Form {
            Section {
                NavigationLink { FeaturesView() } label: { Label(L("features.title"), systemImage: "square.grid.2x2") }
            }
            Section {
                LabeledContent(L("settings.location")) { PlacePicker(font: .body) }
                NavigationLink { NotificationsView() } label: { Label(L("settings.notifications"), systemImage: "bell") }
            }
            Section {
                NavigationLink { AccentPickerView() } label: {
                    HStack {
                        Label(L("settings.color"), systemImage: "paintpalette")
                        Spacer()
                        Circle().fill(Theme.accent).frame(width: 18, height: 18)
                    }
                }
                NavigationLink { AppearanceView() } label: { Label(L("look.title"), systemImage: "textformat.size") }
                Picker(L("settings.units"), selection: $units) {
                    Text(L("settings.units.metric")).tag("metric")
                    Text(L("settings.units.imperial")).tag("imperial")
                }
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
            // Sunucusuz eşitleme (CloudSync.swift): aynı Apple hesabındaki cihazlar arasında, iCloud anahtar-değer deposu
            Section {
                Toggle(isOn: Binding(get: { syncOn }, set: { syncOn = $0; CloudSync.setEnabled($0) })) {
                    Label(L("sync.title"), systemImage: "icloud")
                }
            } footer: {
                Text(L("sync.note"))
            }
            // Siri ve Kısayollar (AppIntents.swift): Kısayollar uygulamasındaki eylemlere bağlantı ve şarj otomasyonu notu
            Section {
                ShortcutsLink()
                    .shortcutsLinkStyle(.automaticOutline)
            } header: {
                Text(L("siri.title"))
            } footer: {
                Text(L("siri.note"))
            }
            // Araçlar kapalıyken rozetler ve hakkında buradadır (başka yoldan açılmazlar)
            if !Features.parse(features).contains(.tools) {
                Section {
                    NavigationLink { BadgesView() } label: { Label(L("badges.h1"), systemImage: "rosette") }
                    NavigationLink { AboutView() } label: { Label(L("settings.about"), systemImage: "info.circle") }
                }
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("settings.title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// Uygulama rengi: dört grupta 24 renk (genel, çocuklar, zarif, iş); seçim `accent` anahtarında
struct AccentPickerView: View {
    @AppStorage("accent") private var accent = AccentPalette.defaultID

    var body: some View {
        List {
            ForEach(AccentPalette.Group.allCases, id: \.self) { group in
                Section(L("settings.color.\(group.rawValue)")) {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 6), spacing: 12) {
                        let colors = AccentPalette.all.filter { $0.group == group }
                        ForEach(Array(colors.enumerated()), id: \.element.id) { i, p in
                            swatch(p, label: L("settings.color.\(group.rawValue)") + " \(i + 1)")
                        }
                    }
                    .padding(.vertical, 6)
                }
            }
            Section {
                HStack(spacing: 12) {
                    AnalogClock(date: Date(), showSeconds: true).frame(width: 64, height: 64)
                    Text(L("app.displayName")).font(.headline)
                    Spacer()
                    Text(L("settings.color.sample")).font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(Theme.accent, in: Capsule())
                        .foregroundStyle(Theme.bg)
                }
                .foregroundStyle(Theme.accent)
            }
            Section {
                Button(L("world.reset")) { accent = AccentPalette.defaultID }
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("settings.color"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func swatch(_ p: AccentPalette, label: String) -> some View {
        let on = p.id == accent
        return Button {
            if accent != p.id { BadgeStore.shared.hit(.renk) }
            accent = p.id
        } label: {
            // Üç renk: ana renk, ikinci renk ve kadran/zemin tonu
            HStack(spacing: 0) {
                Rectangle().fill(p.color)
                VStack(spacing: 0) {
                    Rectangle().fill(p.secondColor)
                    Rectangle().fill(p.faceColor)
                }
            }
            .frame(width: 40, height: 40)
            .clipShape(Circle())
            .overlay {
                if on {
                    Image(systemName: "checkmark")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                        .shadow(radius: 2)
                }
            }
            .overlay(Circle().stroke(Theme.text.opacity(on ? 0.6 : 0), lineWidth: 2).padding(-4))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(on ? .isSelected : [])
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
        .themedBackground()
        .navigationTitle(L("settings.privacy"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Hakkında

/// Uygulama adı, sürüm, web sitesi ve iletişim; iletişimin altında sağda sitedeki Bumba Life rozeti (açık/koyu);
/// altında e-posta listesi formu ve gizlilik notu
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
            } footer: {
                // Sitedeki Bumba Life rozeti, iletişimin hemen altında sağda (18 Ekim 2026, sahibin isteği)
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
                .padding(.top, 12)
            }
            .tint(Theme.text)
            // E-posta listesi doğrudan burada (düğme değil, yazma alanı; 18 Ekim 2026, sahibin isteği)
            ListSignupSection()
            // Gizlilik açık hâlde Hakkında'nın altında
            Section(L("settings.privacy")) {
                Text(L("settings.privacyNote")).font(.subheadline)
                Link(L("privacy.full"), destination: SiteLinks.privacy)
                    .tint(Theme.accent)
            }
        }
        .themedBackground()
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
    var body: some View {
        Form { ListSignupSection() }
            .themedBackground()
            .navigationTitle(L("list.heading"))
            .navigationBarTitleDisplayMode(.inline)
    }
}

/// Liste formu (Hakkında sayfasında): adres alanı, onay anahtarı, aydınlatma bağlantısı ve gönder
struct ListSignupSection: View {
    @State private var email = ""
    @State private var consent = false
    @State private var sending = false
    @State private var result: Bool?

    var body: some View {
        Section {
            Text(L("list.lead")).font(.subheadline)
            TextField(L("list.emailLabel"), text: $email)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.send)
                .onSubmit { if canSend { Task { await send() } } }
            Toggle(isOn: $consent) {
                Text(L("list.consentText")).font(.subheadline)
            }
            .tint(Theme.accent)
            Link(L("list.readNote"), destination: ListService.noteURL)
                .font(.subheadline)
                .tint(Theme.accent)
            Button {
                Task { await send() }
            } label: {
                HStack {
                    Text(L("list.submit")).font(.headline)
                    if sending { Spacer(); ProgressView() }
                }
            }
            .disabled(!canSend)
            .tint(Theme.accent)
            if let result {
                Label(L(result ? "list.ok" : "list.err"), systemImage: result ? "checkmark.circle" : "exclamationmark.triangle")
                    .foregroundStyle(result ? Theme.accent : .red)
            }
        } header: {
            Text(L("list.heading"))
        } footer: {
            Text(L("list.popupText"))
        }
    }

    private var canSend: Bool { consent && email.contains("@") && !sending }

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
