import SwiftUI
import AppIntents

// MARK: Ayarlar

struct SettingsView: View {
    @AppStorage("h12") private var h12 = false
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("appearance") private var appearance = "system"
    @AppStorage("units") private var units = Units.systemDefault
    @AppStorage("units.pressure") private var pressureUnit = "auto"
    /// Renk değişince bu sayfa da yeni renkle çizilir
    @AppStorage("accent") private var accent = AccentPalette.defaultID
    @AppStorage(Features.key) private var features = ""
    @State private var syncOn = CloudSync.enabled

    /// Ayarlarda arama (2. analiz #198): satır başlıklarında geçen sözcükler
    @State private var query = ""

    private func shows(_ keys: String...) -> Bool {
        let q = query.trimmingCharacters(in: .whitespaces)
        return q.isEmpty || keys.contains { L($0).localizedStandardContains(q) }
    }

    var body: some View {
        // Beş başlık (2. analiz #198): Genel, Renk ve görünüm, Saat, Yedek ve kısayollar, Destek; satırda şimdiki değer,
        // uzun açıklamalar alt sayfada, en üstte arama
        let general = ["features.title", "settings.location", "settings.notifications", "lang.title", "settings.units",
                       "settings.pressureUnit", "client.formatLabel", "settings.keepAwake"]
        let look = ["settings.color", "settings.appearance", "fontScale.title"]
        let clock = ["look.title", "home.customize", "dial.title", "night.title"]
        let backup = ["sync.title", "siri.title"]
        let support = ["support.title", "support.rate", "support.share", "support.qr", "support.feedback", "support.moreApps",
                       "support.version", "badges.h1", "settings.about"]
        Form {
            if general.contains(where: { shows($0) }) {
                Section(L("settings.color.general")) {
                    if shows("features.title") {
                        NavigationLink { FeaturesView() } label: { BoxLabel(L("features.title"), icon: "square.grid.2x2") }
                    }
                    if shows("settings.location") {
                        LabeledContent { PlacePicker(font: .body) } label: { BoxLabel(L("settings.location"), icon: "location") }
                    }
                    if shows("settings.notifications") {
                        NavigationLink { NotificationsView() } label: { BoxLabel(L("settings.notifications"), icon: "bell") }
                    }
                    if shows("lang.title") { LanguageRow() }
                    if shows("settings.units") {
                        Picker(selection: $units) {
                            Text(L("settings.units.metric")).tag("metric")
                            Text(L("settings.units.imperial")).tag("imperial")
                        } label: { BoxLabel(L("settings.units"), icon: "ruler") }
                    }
                    // Basınç birimi (2. analiz #106)
                    if shows("settings.pressureUnit") {
                        Picker(selection: $pressureUnit) {
                            Text(L("countdowns.unit.auto")).tag("auto")
                            ForEach(["hPa", "mmHg", "inHg"], id: \.self) { Text($0).tag($0) }
                        } label: { BoxLabel(L("settings.pressureUnit"), icon: "gauge") }
                    }
                    if shows("client.formatLabel") {
                        Picker(selection: $h12) {
                            Text(L("client.format24")).tag(false)
                            Text(L("client.format12")).tag(true)
                        } label: { BoxLabel(L("client.formatLabel"), icon: "clock") }
                    }
                    if shows("settings.keepAwake") {
                        Toggle(isOn: $keepAwake) { BoxLabel(L("settings.keepAwake"), icon: "sun.max") }
                    }
                }
            }
            if look.contains(where: { shows($0) }) {
                Section(L("settings.group.look")) {
                    if shows("settings.color") {
                        NavigationLink { AccentPickerView() } label: {
                            HStack {
                                BoxLabel(L("settings.color"), icon: "paintpalette")
                                Spacer()
                                Circle().fill(Theme.accent).frame(width: 18, height: 18)
                            }
                        }
                    }
                    if shows("settings.appearance") {
                        Picker(selection: $appearance) {
                            Text(L("settings.system")).tag("system")
                            Text(L("settings.light")).tag("light")
                            Text(L("settings.dark")).tag("dark")
                        } label: { BoxLabel(L("settings.appearance"), icon: "circle.lefthalf.filled") }
                    }
                    // Yazı boyutu (2. analiz #77): uygulamanın bütün yazıları, %100 telefonun ayarı
                    if shows("fontScale.title") { FontScaleRow() }
                }
            }
            if clock.contains(where: { shows($0) }) {
                Section(L("tab.clock")) {
                    if shows("look.title", "dial.title", "night.title") {
                        NavigationLink { AppearanceView() } label: { BoxLabel(L("look.title"), icon: "textformat.size") }
                    }
                    if shows("home.customize") {
                        NavigationLink { HomeItemsView() } label: { BoxLabel(L("home.customize"), icon: "slider.horizontal.3") }
                    }
                }
            }
            if backup.contains(where: { shows($0) }) {
                Section(L("settings.group.backup")) {
                    // Sunucusuz eşitleme (CloudSync.swift): aynı Apple hesabındaki cihazlar arasında, iCloud anahtar-değer deposu
                    if shows("sync.title") {
                        Toggle(isOn: Binding(get: { syncOn }, set: { syncOn = $0; CloudSync.setEnabled($0) })) {
                            BoxLabel(L("sync.title"), icon: "icloud")
                        }
                        NavigationLink { SettingsNoteView(title: L("sync.title"), text: L("sync.note")) } label: {
                            Text(L("settings.details")).foregroundStyle(Theme.muted)
                        }
                    }
                    // Siri ve Kısayollar (AppIntents.swift): Kısayollar uygulamasındaki eylemlere bağlantı ve şarj otomasyonu notu
                    if shows("siri.title") {
                        NavigationLink {
                            SettingsNoteView(title: L("siri.title"), text: L("siri.note")) {
                                ShortcutsLink().shortcutsLinkStyle(.automaticOutline)
                            }
                        } label: { BoxLabel(L("siri.title"), icon: "wand.and.stars") }
                    }
                }
            }
            // Puanla, paylaş, QR kod, geri bildirim, Bumba'nın diğer ürünleri, sürüm (2. analiz #6, #81)
            if support.contains(where: { shows($0) }) {
                SupportSection()
                // Hakkında Ayarlar'dadır (Seçenek B); rozetler Tümü'nde, Tümü yokken (hiçbir özellik açık değil) burada
                Section {
                    if Features.parse(features).isEmpty {
                        NavigationLink { BadgesView() } label: { BoxLabel(L("badges.h1"), icon: "rosette") }
                    }
                    NavigationLink { AboutView() } label: { BoxLabel(L("settings.about"), icon: "info.circle") }
                }
            }
            if ![general, look, clock, backup, support].joined().contains(where: { shows($0) }) {
                ContentUnavailableView.search(text: query)
            }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: L("settings.search"))
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("settings.title"))
        .navigationBarTitleDisplayMode(.large)
    }
}

/// Uygulama rengi: dört grupta 24 renk (genel, çocuklar, zarif, iş); seçim `accent` anahtarında
/// Ayarlardaki uzun açıklamalar için alt sayfa (2. analiz #198)
struct SettingsNoteView<Extra: View>: View {
    let title: String
    let text: String
    @ViewBuilder var extra: () -> Extra

    var body: some View {
        Form {
            Section { extra() } footer: { Text(text) }
        }
        .themedBackground()
        .navigationTitle(title)
    }
}

extension SettingsNoteView where Extra == EmptyView {
    init(title: String, text: String) {
        self.init(title: title, text: text) { EmptyView() }
    }
}

struct AccentPickerView: View {
    @AppStorage("accent") private var accent = AccentPalette.defaultID
    @AppStorage(Atmosphere.key) private var atmo = ""

    var body: some View {
        List {
            // Adlı atmosferler (2. analiz #78): yana kaydırmalı büyük kartlar; seçili kart ışır, köşesinde onay
            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        atmoCard(nil)
                        ForEach(Atmosphere.all) { atmoCard($0) }
                    }
                    .padding(.vertical, 10)
                    .padding(.horizontal, 4)
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 12, bottom: 0, trailing: 12))
            } header: {
                Text(L("atmo.title"))
            } footer: {
                Text(L("atmo.note"))
            }
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
        .navigationBarTitleDisplayMode(.large)
    }

    private func atmoCard(_ a: Atmosphere?) -> some View {
        let on = (a?.id ?? "") == atmo
        let name = a.map { L("atmo.\($0.id)") } ?? L("atmo.none")
        return Button { atmo = a?.id ?? "" } label: {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(a.map { LinearGradient(colors: [$0.top, $0.bottom], startPoint: .top, endPoint: .bottom) }
                          ?? LinearGradient(colors: [Theme.bg, Theme.bg], startPoint: .top, endPoint: .bottom))
                // Örnek kart ve vurgu rengi: zeminin üstünde nasıl görüneceği
                VStack(alignment: .leading, spacing: 6) {
                    RoundedRectangle(cornerRadius: 6).fill(Theme.surface).frame(width: 70, height: 26)
                    Capsule().fill(Theme.accent).frame(width: 40, height: 8)
                    Spacer()
                    Text(name).font(.footnote.weight(on ? .bold : .semibold)).foregroundStyle(Theme.text).lineLimit(2)
                }
                .padding(10)
                if on {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.white, Theme.accent)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                        .padding(8)
                }
            }
            .frame(width: 112, height: 150)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(on ? Theme.accent : Theme.border, lineWidth: on ? 2 : 1))
            .shadow(color: on ? Theme.accent.opacity(0.45) : .clear, radius: 10)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(name)
        .accessibilityAddTraits(on ? .isSelected : [])
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
        .navigationBarTitleDisplayMode(.large)
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
        .navigationBarTitleDisplayMode(.large)
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
            .navigationBarTitleDisplayMode(.large)
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
                    .foregroundStyle(result ? Theme.ok : Theme.err)
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
