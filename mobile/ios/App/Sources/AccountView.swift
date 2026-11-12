import SwiftUI
import UIKit
import PhotosUI

/// Ayarlar'ın en üstündeki satır: hesap simgesi, "Hesap", sağda e-posta ya da girişsiz notu
struct AccountRow: View {
    var body: some View {
        NavigationLink { AccountView() } label: {
            LabeledContent {
                Text(Account.shared.user?.email ?? L("account.rowSignedOut")).lineLimit(1).truncationMode(.middle)
            } label: {
                BoxLabel(L("account.title"), duo: "account", tint: Theme.sage)
            }
        }
    }
}

private func A(_ key: String) -> String { L("account.\(key)") }

/// Hata ya da başarı iletisinin metni; "account.err.field:alan" biçimi alan adını yazar
private func messageText(_ key: String) -> String {
    key.hasPrefix("account.err.field:") ? T("account.err.field", String(key.dropFirst("account.err.field:".count))) : L(key)
}

/// Profil seçenekleri (server/src/profile.mjs ile aynı)
private let socialPlatforms = ["instagram", "x", "facebook", "linkedin", "youtube", "tiktok", "threads", "bluesky", "github", "telegram", "web"]
private let visibleFields: [(String, String)] = [("ad", "firstName"), ("soyad", "lastName"), ("takmaAd", "nickname"), ("foto", "photo"),
    ("dogumTarihi", "birthDate"), ("cinsiyet", "gender"), ("telefon", "phone"), ("ulke", "country"), ("sehir", "city"),
    ("meslek", "job"), ("hakkimda", "about"), ("sosyal", "social")]

private func flag(_ cc: String) -> String {
    cc.count == 2 ? String(String.UnicodeScalarView(cc.uppercased().unicodeScalars.compactMap { UnicodeScalar(127397 + $0.value) })) : ""
}

/// Ülke kodları ve telefon kodları (server/data/countries.json'dan; gen-strings uygulamaya kopyalar)
private enum Countries {
    struct Item: Hashable { let code: String; let dial: String; let name: String }
    static let list: [Item] = {
        guard let url = Bundle.main.url(forResource: "countries", withExtension: "json"), let d = try? Data(contentsOf: url),
              let o = (try? JSONSerialization.jsonObject(with: d)) as? [String: Any], let u = o["ulkeler"] as? [String: String] else { return [] }
        return u.map { Item(code: $0.key, dial: $0.value, name: AppLocale.locale.localizedString(forRegionCode: $0.key) ?? $0.key) }
            .sorted { $0.name.compare($1.name, locale: AppLocale.locale) == .orderedAscending }
    }()
}

/// [metin](adres) bağlantılı metin (onay cümlesi, isteğe bağlı bilgiler notu)
private struct LinkedText: View {
    let raw: String
    var body: some View {
        Text((try? AttributedString(markdown: raw, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace))) ?? AttributedString(raw))
            .font(.footnote).foregroundStyle(Theme.muted).tint(Theme.accent)
    }
}

/// Şifre kuralı: alanın altında dört satır, sağlandıkça işaretlenir
private struct PasswordRules: View {
    let password: String
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(A("pwRuleTitle")).foregroundStyle(Theme.muted)
            ForEach(Account.passwordRules(password), id: \.0) { k, ok in
                Label(A(k), systemImage: ok ? "checkmark.circle.fill" : "circle").foregroundStyle(ok ? Theme.ok : Theme.muted)
            }
        }
        .font(.footnote)
    }
}

/// Başarı ya da hata iletisi (metin anahtarıyla)
private struct AccountMessage: View {
    let key: String?
    var ok = false
    var body: some View {
        if let key {
            Label(messageText(key), systemImage: ok ? "checkmark.circle" : "exclamationmark.triangle")
                .font(.subheadline)
                .foregroundStyle(ok ? Theme.ok : Theme.err)
        }
    }
}

private extension View {
    /// E-posta alanı: büyük harf ve düzeltme kapalı
    func emailField() -> some View {
        keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled()
    }
}

/// Düğme yazısı; istek sürerken yanında dönen gösterge
private struct BusyLabel: View {
    let title: String
    let busy: Bool
    init(_ title: String, busy: Bool) { self.title = title; self.busy = busy }
    var body: some View {
        HStack {
            Text(title).font(.headline)
            if busy { Spacer(); ProgressView() }
        }
    }
}

/// Bölüm başlığı: numara + ad (web'deki hesap sayfasıyla aynı sıra)
private struct Numbered: View {
    let n: Int
    let title: String
    var body: some View {
        HStack(spacing: 8) {
            Text("\(n)").font(.headline).foregroundStyle(Theme.accent)
            Text(title).font(.headline).foregroundStyle(Theme.text)
        }
        .textCase(nil)
    }
}

/// Ayarlar › Hesap (standart 5.15, madde 7): girişsizken giriş, kayıt (yalnız e-posta ve şifre, Bumba üyeliği sorusu),
/// şifremi unuttum; girişliyken web'deki hesap sayfasının 10 bölümü, aynı sırayla. Android'deki AccountScreen ile aynı
/// akış ve metinler (`account.*`).
struct AccountView: View {
    @State private var notice: String?

    var body: some View {
        Form {
            if Account.shared.signedIn {
                AccountSignedIn { notice = $0 }
            } else {
                AccountSignedOut(notice: notice)
            }
            Section {
                Link(A("notice"), destination: AccountLinks.notice)
                Link(A("terms"), destination: AccountLinks.terms)
                Link(A("deletionInfo"), destination: AccountLinks.deletion)
            }
            .tint(Theme.accent)
        }
        .navigationTitle(A("title"))
        .themedBackground()
        .task { if Account.shared.signedIn { await Account.shared.refresh() } }
    }
}

private struct AccountSignedOut: View {
    let notice: String?
    /// in, up, forgot, link (Bumba üyeliğiyle giriş), done (kayıt bitti)
    @State private var mode = "in"
    @State private var email = ""
    @State private var password = ""
    /// Kayıtta e-posta başka bir Bumba ürününde kayıtlı
    @State private var bumba = false
    @State private var busy = false
    @State private var err: String?
    @State private var info: String?
    @State private var canResend = false

    init(notice: String?) {
        self.notice = notice
        _info = State(initialValue: notice)
    }

    private func run(_ block: @escaping () async -> Account.Result, onOk: @escaping (Account.Result) -> Void = { _ in }) {
        guard !busy else { return }
        busy = true; err = nil; info = nil
        Task {
            let r = await block()
            busy = false
            if r.ok { onOk(r) } else if r.err == "account.bumba" { bumba = true } else { err = r.err; canResend = r.err == "account.err.unverified" }
        }
    }

    var body: some View {
        Section {
            if mode == "in" || mode == "up" {
                Picker("", selection: $mode) {
                    Text(A("signIn")).tag("in")
                    Text(A("signUp")).tag("up")
                }
                .pickerStyle(.segmented)
                .onChange(of: mode) { _, _ in err = nil; bumba = false }
            }
        } footer: {
            Text(A("intro"))
        }
        Section {
            switch mode {
            case "forgot":
                Text(A("forgotLead")).font(.subheadline)
                TextField(A("email"), text: $email).textContentType(.username).emailField()
                Button { run({ await Account.shared.forgot(email) }) { _ in info = "account.forgotSent" } } label: {
                    BusyLabel(A("forgotSend"), busy: busy)
                }
                Button(A("back")) { mode = "in"; err = nil; info = nil }
            case "up":
                if bumba {
                    Text(A("bumbaQ")).font(.headline)
                    Text(A("bumbaNote")).font(.subheadline).foregroundStyle(Theme.muted)
                    Button(A("yes")) { bumba = false; password = ""; mode = "link" }
                    Button(A("no")) { bumba = false; email = ""; info = nil; err = "account.bumbaNo" }
                } else {
                    TextField(A("email"), text: $email).textContentType(.username).emailField()
                    SecureField(A("password"), text: $password).textContentType(.newPassword)
                    PasswordRules(password: password)
                    Button {
                        run({
                            let q = await Account.shared.lookup(email)
                            if !q.ok { return q }
                            if q.data.map({ String(decoding: $0, as: UTF8.self) }) == "var" { return .fail("account.bumba") }
                            return await Account.shared.signUp(email: email, password: password)
                        }) { _ in password = ""; mode = "done"; canResend = true }
                    } label: { BusyLabel(A("signUp"), busy: busy) }
                    LinkedText(raw: A("consent"))
                }
            case "link":
                Text(A("bumbaNote")).font(.subheadline).foregroundStyle(Theme.muted)
                LabeledContent(A("email"), value: email)
                SecureField(A("password"), text: $password).textContentType(.password)
                Button { run({ await Account.shared.link(email: email, password: password) }) { _ in password = "" } } label: {
                    BusyLabel(A("signIn"), busy: busy)
                }
                Button(A("forgot")) { mode = "forgot"; err = nil }
            case "done":
                AccountMessage(key: "account.signedUp", ok: true)
                Button(A("signIn")) { mode = "in"; info = nil }
            default:
                TextField(A("email"), text: $email).textContentType(.username).emailField()
                SecureField(A("password"), text: $password).textContentType(.password)
                Button { run({ await Account.shared.signIn(email: email, password: password) }) { _ in password = "" } } label: {
                    BusyLabel(A("signIn"), busy: busy)
                }
                Button(A("forgot")) { mode = "forgot"; err = nil; info = nil }
            }
            AccountMessage(key: err)
            AccountMessage(key: info, ok: true)
            if canResend && (mode == "in" || mode == "done") {
                Button(A("resend")) { run({ await Account.shared.resendVerification(email) }) { _ in info = "account.resent" } }
            }
        } header: {
            if mode == "forgot" { Text(A("forgot")) } else if mode == "link" { Text(A("bumbaSignIn")) }
        }
        .tint(Theme.accent)
    }
}

/// Profil (1. ve 2. bölüm): sunucudaki `profil` nesnesiyle aynı alanlar
private struct ProfileForm {
    var ad = "", soyad = "", takma = "", dogum = "", cinsiyet = "", ulke = "", kod = "", numara = "", sehir = "", meslek = "", hakkimda = ""
    var sosyal: [(platform: String, value: String)] = []
    var foto: String?
    var acik = false
    var alanlar: Set<String> = []

    init() {}
    init(_ p: [String: Any]) {
        func s(_ k: String) -> String { p[k] as? String ?? "" }
        ad = s("ad"); soyad = s("soyad"); takma = s("takmaAd"); dogum = s("dogumTarihi"); cinsiyet = s("cinsiyet"); ulke = s("ulke")
        sehir = s("sehir"); meslek = s("meslek"); hakkimda = s("hakkimda")
        if let t = p["telefon"] as? [String: Any] { kod = t["ulkeKodu"] as? String ?? ""; numara = t["numara"] as? String ?? "" }
        sosyal = (p["sosyal"] as? [[String: Any]] ?? []).map { ($0["platform"] as? String ?? "web", $0["deger"] as? String ?? "") }
        foto = (p["foto"] as? String).flatMap { $0.isEmpty ? nil : $0 }
        let g = p["gorunurluk"] as? [String: Any]
        acik = g?["acik"] as? Bool ?? false
        alanlar = Set(g?["alanlar"] as? [String] ?? [])
    }

    var json: [String: Any] {
        func v(_ s: String) -> Any { let t = s.trimmingCharacters(in: .whitespacesAndNewlines); return t.isEmpty ? NSNull() as Any : t as Any }
        let n = numara.filter(\.isNumber)
        return ["ad": v(ad), "soyad": v(soyad), "takmaAd": v(takma), "dogumTarihi": v(dogum), "cinsiyet": v(cinsiyet), "ulke": v(ulke),
                "sehir": v(sehir), "meslek": v(meslek), "hakkimda": v(hakkimda),
                "telefon": !kod.isEmpty && !n.isEmpty ? ["ulkeKodu": kod, "numara": n] as Any : NSNull() as Any,
                "sosyal": sosyal.filter { !$0.value.trimmingCharacters(in: .whitespaces).isEmpty }
                    .map { ["platform": $0.platform, "deger": $0.value.trimmingCharacters(in: .whitespaces)] },
                "gorunurluk": ["acik": acik, "alanlar": visibleFields.map(\.0).filter { alanlar.contains($0) }]]
    }
}

private struct AccountSignedIn: View {
    let onSignedOut: (String) -> Void
    @State private var data: [String: Any]?
    @State private var loadErr: String?
    @State private var p = ProfileForm()
    @State private var busy = false
    /// Her bölümün kendi iletisi: (bölüm, metin anahtarı, başarılı mı)
    @State private var msg: (String, String, Bool)?
    @State private var separate = false
    @State private var mergePassword = ""
    @State private var mergeErr: String?
    @State private var photoItem: PhotosPickerItem?
    @State private var newEmail = ""
    @State private var current = ""
    @State private var next = ""
    @State private var celebrate = true
    @State private var sessions: [[String: Any]] = []
    @State private var products: [[String: Any]]?
    @State private var askOpen: [String]?
    @State private var askClose: [String]?
    @State private var openPassword = ""
    @State private var scopeAll = false
    @State private var deletePassword = ""
    @State private var confirmDelete = false
    @State private var export: URL?
    private var sync: AccountSync { AccountSync.shared }

    private func run(_ card: String, _ block: @escaping () async -> Account.Result, ok okKey: String?,
                     onOk: @escaping (Account.Result) -> Void = { _ in }) {
        guard !busy else { return }
        busy = true; msg = nil
        Task {
            let r = await block()
            busy = false
            if r.ok { onOk(r); if let okKey { msg = (card, okKey, true) } } else { msg = (card, r.err ?? "account.err.generic", false) }
        }
    }

    @ViewBuilder private func message(_ card: String) -> some View {
        if let m = msg, m.0 == card { AccountMessage(key: m.1, ok: m.2) }
    }

    private func load() async {
        let r = await Account.shared.load()
        if r.ok, let o = Account.json(r.data) {
            data = o
            p = ProfileForm(o["profil"] as? [String: Any] ?? [:])
            celebrate = o["kutlama"] as? Bool ?? true
        } else { loadErr = r.err }
    }

    private var memberNo: String { (data?["uyeNo"] as? NSNumber).map { String($0.int64Value) } ?? "" }

    private static let iso: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
    private static func date(_ s: Any?) -> Date? {
        guard let s = s as? String else { return nil }
        return iso.date(from: s) ?? ISO8601DateFormatter().date(from: s)
    }
    private static func dayString(_ d: Date) -> String {
        let c = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: d)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    var body: some View {
        if let u = Account.shared.user {
            if data == nil {
                Section { Text(loadErr.map(messageText) ?? "…").foregroundStyle(Theme.muted) }
                    .task(id: u.id) {
                        await load()
                        separate = await Account.shared.separateAccount()
                        sessions = await Account.shared.sessions()
                        products = await Account.shared.products()
                        await sync.sync()
                    }
            } else {
                header(u)
                badges
                profile
                visibility
                emailSection
                passwordSection
                prefs
                emails
                sessionsSection
                membership
                downloadSection
                deleteSection
            }
        }
    }

    // Üst: e-posta ve çıkış
    @ViewBuilder private func header(_ u: Account.User) -> some View {
        Section {
            Text(u.email).font(.headline)
            Button(A("signOut")) { Task { await Account.shared.signOut(); onSignedOut("account.signedOut") } }
        } footer: { Text(A("signedInNote")) }
    }

    // Rozetlerim ve kayıtlarım (sahibin kararı S3, 12 Kasım 2026: ayrı bölüm)
    @ViewBuilder private var badges: some View {
        Section {
            Text(T("account.badgesCount", max(sync.accountBadges, 0)))
            Text(sync.records > 0 ? T("account.recordsCount", sync.records) : A("recordsNone")).foregroundStyle(Theme.muted)
            if !BadgeStore.shared.saved.r.isEmpty {
                Button(A("badgesAdd")) {
                    run("badges", { await sync.addBadges() ? .success : .fail("account.err.generic") }, ok: "account.badgesAdded")
                }
            }
            Button(A("sync")) { run("badges", { await sync.sync() ? .success : .fail("account.err.network") }, ok: "account.saved") }
            message("badges")
        } header: { Text(A("badgesTitle")) } footer: { Text(A("syncNote") + "\n" + A("badgesNote")) }
        .alert(T("account.badgesAsk", sync.askBadges), isPresented: Binding(get: { sync.askBadges > 0 }, set: { if !$0 { sync.declineBadges() } })) {
            Button(A("yes")) { Task { await sync.addBadges() } }
            Button(A("no"), role: .cancel) { sync.declineBadges() }
        }
        .alert(A("ayriQ"), isPresented: $separate) {
            SecureField(A("password"), text: $mergePassword)
            Button(A("yes")) {
                Task {
                    let r = await Account.shared.merge(password: mergePassword)
                    mergePassword = ""
                    if r.ok { msg = ("badges", "account.ayriDone", true); await load() } else { msg = ("badges", r.err ?? "account.err.generic", false) }
                }
            }
            Button(A("no"), role: .cancel) { mergePassword = "" }
        } message: { Text(A("ayriNote")) }
    }

    // 1. Profil
    @ViewBuilder private var profile: some View {
        Section {
            HStack(spacing: 14) {
                if let img = p.foto.flatMap(Self.photoImage) {
                    Image(uiImage: img).resizable().scaledToFill().frame(width: 80, height: 80).clipShape(Circle())
                        .accessibilityLabel(A("photo"))
                }
                VStack(alignment: .leading, spacing: 8) {
                    PhotosPicker(selection: $photoItem, matching: .images) { Text(A("photoChoose")) }
                    if p.foto != nil {
                        Button(A("photoRemove")) { Task { if await Account.shared.savePhoto(nil).ok { p.foto = nil } } }
                    }
                }
                .buttonStyle(.borderless)
            }
            .onChange(of: photoItem) { _, item in
                guard let item else { return }
                Task {
                    photoItem = nil
                    guard let raw = try? await item.loadTransferable(type: Data.self),
                          let url = await Task.detached(operation: { Account.photoDataURL(raw) }).value else {
                        msg = ("profile", "account.err.generic", false); return
                    }
                    let r = await Account.shared.savePhoto(url)
                    if r.ok { p.foto = url } else { msg = ("profile", r.err ?? "account.err.generic", false) }
                }
            }
            Text(A("photoNote")).font(.footnote).foregroundStyle(Theme.muted)
            TextField(A("firstName"), text: $p.ad.limited(100)).textContentType(.givenName)
            TextField(A("lastName"), text: $p.soyad.limited(100)).textContentType(.familyName)
            TextField(A("nickname"), text: $p.takma.limited(60)).textContentType(.nickname)
            birthDate
            Picker(A("gender"), selection: $p.cinsiyet) {
                Text(A("none")).tag("")
                Text(A("genderF")).tag("kadin")
                Text(A("genderM")).tag("erkek")
                Text(A("genderN")).tag("belirtmek-istemiyorum")
            }
            Picker(A("country"), selection: $p.ulke) {
                Text(A("none")).tag("")
                ForEach(Countries.list, id: \.code) { c in Text("\(flag(c.code)) \(c.name)").tag(c.code) }
            }
            .onChange(of: p.ulke) { _, cc in
                if p.kod.isEmpty, let dial = Countries.list.first(where: { $0.code == cc })?.dial, !dial.isEmpty { p.kod = "+" + dial }
            }
            Picker(A("phoneCode"), selection: $p.kod) {
                Text(A("none")).tag("")
                ForEach(Self.dialCodes, id: \.0) { code, label in Text(label).tag(code) }
            }
            TextField(A("phoneNumber"), text: Binding(get: { p.numara }, set: { p.numara = String($0.filter { $0.isNumber || $0 == " " }.prefix(20)) }))
                .keyboardType(.phonePad).textContentType(.telephoneNumber)
            TextField(A("city"), text: $p.sehir.limited(100)).textContentType(.addressCity)
            TextField(A("job"), text: $p.meslek.limited(100)).textContentType(.jobTitle)
            VStack(alignment: .leading, spacing: 4) {
                TextField(A("about"), text: $p.hakkimda.limited(160), axis: .vertical).lineLimit(2...5)
                Text(A("aboutHint") + " · \(p.hakkimda.count)/160").font(.caption).foregroundStyle(Theme.muted)
            }
        } header: { Numbered(n: 1, title: A("profile")) }

        Section {
            ForEach(p.sosyal.indices, id: \.self) { i in
                Picker(A("social"), selection: Binding(get: { p.sosyal[i].platform }, set: { p.sosyal[i].platform = $0 })) {
                    ForEach(socialPlatforms, id: \.self) { s in Text(s == "web" ? A("socialWeb") : s.prefix(1).uppercased() + s.dropFirst()).tag(s) }
                }
                TextField(A("socialValue"), text: Binding(get: { p.sosyal[i].value }, set: { p.sosyal[i].value = String($0.prefix(300)) }))
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                Button(A("socialRemove"), role: .destructive) { p.sosyal.remove(at: i) }
            }
            if p.sosyal.count < 8 { Button(A("socialAdd")) { p.sosyal.append((socialPlatforms[0], "")) } }
            LinkedText(raw: A("optionalNote"))
            Button(A("save")) { run("profile", { await Account.shared.saveProfile(p.json) }, ok: "account.saved") }
            message("profile")
        } header: { Text(A("social")) }
    }

    private static let dialCodes: [(String, String)] = {
        var seen = Set<String>()
        return Countries.list.compactMap { c in
            guard !c.dial.isEmpty, seen.insert("+" + c.dial).inserted else { return nil }
            return ("+" + c.dial, "\(flag(c.code)) +\(c.dial)")
        }
    }()

    private static func photoImage(_ dataUrl: String) -> UIImage? {
        guard let comma = dataUrl.firstIndex(of: ","), let d = Data(base64Encoded: String(dataUrl[dataUrl.index(after: comma)...])) else { return nil }
        return UIImage(data: d)
    }

    // Doğum tarihi: yalnız 16 ile 90 yaş arası seçilebilir (sunucu da denetler)
    @ViewBuilder private var birthDate: some View {
        let cal = Calendar(identifier: .gregorian)
        let latest = cal.date(byAdding: .year, value: -16, to: Date()) ?? Date()
        let earliest = cal.date(byAdding: .day, value: 1, to: cal.date(byAdding: .year, value: -90, to: Date()) ?? Date()) ?? Date()
        if p.dogum.isEmpty {
            Button(A("birthDate")) { p.dogum = Self.dayString(latest) }
        } else {
            DatePicker(A("birthDate"), selection: Binding(
                get: { Self.parseDay(p.dogum) ?? latest },
                set: { p.dogum = Self.dayString($0) }), in: earliest...latest, displayedComponents: .date)
            .environment(\.locale, AppLocale.locale)
            .environment(\.calendar, AppLocale.gregorian)
            Button(A("none")) { p.dogum = "" }
        }
    }

    private static func parseDay(_ s: String) -> Date? {
        let parts = s.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return Calendar(identifier: .gregorian).date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2], hour: 12))
    }

    // 2. Görünürlük
    @ViewBuilder private var visibility: some View {
        Section {
            Toggle(A("showProfile"), isOn: $p.acik).tint(Theme.accent)
            ForEach(visibleFields, id: \.0) { field, label in
                Toggle(A(label), isOn: Binding(get: { p.alanlar.contains(field) },
                                               set: { if $0 { p.alanlar.insert(field) } else { p.alanlar.remove(field) } }))
                    .tint(Theme.accent)
            }
            Text(T("account.displayName", shownName)).font(.footnote).foregroundStyle(Theme.muted)
            Button(A("save")) { run("visibility", { await Account.shared.saveProfile(p.json) }, ok: "account.saved") }
            message("visibility")
        } header: { Numbered(n: 2, title: A("visibility")) } footer: { Text(A("visibleFields") + "\n" + A("emailNever")) }
    }

    private var shownName: String {
        let takma = p.takma.trimmingCharacters(in: .whitespaces), ad = p.ad.trimmingCharacters(in: .whitespaces)
        let soyad = p.soyad.trimmingCharacters(in: .whitespaces)
        if p.acik && p.alanlar.contains("takmaAd") && !takma.isEmpty { return takma }
        if p.acik && p.alanlar.contains("ad") && !ad.isEmpty { return p.alanlar.contains("soyad") && !soyad.isEmpty ? "\(ad) \(soyad)" : ad }
        return T("account.member", memberNo)
    }

    // 3. E-posta
    @ViewBuilder private var emailSection: some View {
        Section {
            Text(data?["eposta"] as? String ?? "").font(.headline)
            TextField(A("newEmail"), text: $newEmail).textContentType(.emailAddress).emailField()
            Button(A("changeEmail")) {
                run("email", { await Account.shared.changeEmail(newEmail) }, ok: "account.changeEmailSent") { _ in newEmail = "" }
            }
            message("email")
        } header: { Numbered(n: 3, title: A("emailSection")) }
    }

    // 4. Şifre
    @ViewBuilder private var passwordSection: some View {
        Section {
            SecureField(A("passwordCurrent"), text: $current).textContentType(.password)
            SecureField(A("newPassword"), text: $next).textContentType(.newPassword)
            PasswordRules(password: next)
            Button(A("changePassword")) {
                run("password", { await Account.shared.changePassword(current: current, new: next) }, ok: "account.passwordChanged") { _ in
                    current = ""; next = ""
                }
            }
            message("password")
        } header: { Numbered(n: 4, title: A("passwordSection")) }
    }

    // 5. Dil, tema ve saat dilimi (uygulamada dil ve tema uygulamanın ayarlarıdır; saat dilimi telefonunki)
    @ViewBuilder private var prefs: some View {
        Section {
            LanguageRow()
            NavigationLink { AccentPickerView() } label: { BoxLabel(A("theme"), duo: "palette", tint: Theme.dusk) }
            LabeledContent(A("timeZone"), value: TimeZone.current.identifier.replacingOccurrences(of: "_", with: " "))
        } header: { Numbered(n: 5, title: A("prefs")) } footer: { Text(A("prefsApp")) }
    }

    // 6. E-postalar
    @ViewBuilder private var emails: some View {
        Section {
            Toggle(A("celebrate"), isOn: Binding(get: { celebrate }, set: { on in
                celebrate = on
                Task { if !(await Account.shared.setCelebrate(on)).ok { celebrate = !on } }
            })).tint(Theme.accent)
            Text(A("celebrateNote")).font(.footnote).foregroundStyle(Theme.muted)
            VStack(alignment: .leading, spacing: 4) {
                Text(A("list")).font(.subheadline.weight(.medium))
                Text(A("listNote")).font(.footnote).foregroundStyle(Theme.muted)
            }
            NavigationLink(A("listJoin")) { AboutView() }
        } header: { Numbered(n: 6, title: A("emails")) }
    }

    // 7. Açık oturumlar
    @ViewBuilder private var sessionsSection: some View {
        Section {
            ForEach(sessions.indices, id: \.self) { i in
                let s = sessions[i]
                let tok = s["token"] as? String ?? ""
                let isMe = Account.shared.isCurrent(tok)
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(Self.device(s["client"] as? String) + (isMe ? " · " + A("thisSession") : "")).font(.subheadline.weight(.medium))
                        Text(T("account.opened", Self.fmt(s["createdAt"])) + " · " + T("account.lastUsed", Self.fmt(s["updatedAt"])))
                            .font(.caption).foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    if !isMe {
                        Button(A("close")) { Task { await Account.shared.closeSession(tok); sessions = await Account.shared.sessions() } }
                            .buttonStyle(.borderless)
                    }
                }
            }
            Button(A("closeOthers")) { Task { await Account.shared.closeOtherSessions(); sessions = await Account.shared.sessions() } }
        } header: { Numbered(n: 7, title: A("sessions")) }
    }

    private static func device(_ client: String?) -> String {
        switch client ?? "" {
        case "ios": A("deviceIos")
        case "android": A("deviceAndroid")
        case "": A("deviceWeb")
        default: A("deviceApp")
        }
    }

    private static func longDate(_ v: Any?) -> String {
        guard let d = date(v) else { return "" }
        let f = DateFormatter()
        f.locale = AppLocale.locale
        f.calendar = AppLocale.gregorian
        f.dateStyle = .long
        return f.string(from: d)
    }

    private static func fmt(_ v: Any?) -> String {
        guard let d = date(v) else { return v as? String ?? "" }
        let f = DateFormatter()
        f.locale = AppLocale.locale
        f.calendar = AppLocale.gregorian
        f.dateStyle = .medium
        f.timeStyle = .short
        return f.string(from: d)
    }

    // 8. Üyelik ve öbür Bumba ürünleri
    @ViewBuilder private var membership: some View {
        let others = (products ?? []).filter { !Self.isSelf($0) }
        Section {
            LabeledContent(A("memberNo"), value: memberNo.isEmpty ? "—" : memberNo)
            LabeledContent(A("memberSince"), value: Self.longDate(data?["uyelikTarihi"]))
            VStack(alignment: .leading, spacing: 4) {
                Text(A("products")).font(.subheadline.weight(.medium))
                Text(A("productsNote")).font(.footnote).foregroundStyle(Theme.muted)
                if products == nil { Text(A("productsUnavailable")).font(.footnote).foregroundStyle(Theme.muted) }
            }
            ForEach((products ?? []).indices, id: \.self) { i in
                let pr = products![i]
                let site = pr["site"] as? String ?? "", name = pr["ad"] as? String ?? site
                if Self.isSelf(pr) {
                    LabeledContent(name + " · " + A("productThis"), value: A("productOn"))
                } else {
                    Toggle(name, isOn: Binding(get: { pr["acik"] as? Bool ?? false }, set: { want in
                        if want { askOpen = [site] } else { askClose = [site] }
                    })).tint(Theme.accent)
                }
            }
            if !others.isEmpty {
                Button(A("openAll")) {
                    let s = others.filter { !($0["acik"] as? Bool ?? false) }.compactMap { $0["site"] as? String }
                    if !s.isEmpty { askOpen = s }
                }
                Button(A("closeAll")) {
                    let s = others.filter { $0["acik"] as? Bool ?? false }.compactMap { $0["site"] as? String }
                    if !s.isEmpty { askClose = s }
                }
            }
            message("products")
        } header: { Numbered(n: 8, title: A("membership")) }
        .alert(A("confirmOpen"), isPresented: Binding(get: { askOpen != nil }, set: { if !$0 { askOpen = nil } })) {
            SecureField(A("password"), text: $openPassword)
            Button(A("open")) {
                let sites = askOpen ?? [], pw = openPassword
                openPassword = ""
                run("products", { await Account.shared.openProducts(sites, password: pw) }, ok: "account.saved") { _ in
                    Task { products = await Account.shared.products() }
                }
            }
            Button(A("cancel"), role: .cancel) { openPassword = "" }
        }
        .alert(T("account.confirmClose", (askClose ?? []).map { s in (products ?? []).first { $0["site"] as? String == s }?["ad"] as? String ?? s }
            .joined(separator: ", ")), isPresented: Binding(get: { askClose != nil }, set: { if !$0 { askClose = nil } })) {
            Button(A("close"), role: .destructive) {
                let sites = askClose ?? []
                run("products", { await Account.shared.closeProducts(sites) }, ok: "account.saved") { _ in
                    Task { products = await Account.shared.products() }
                }
            }
            Button(A("cancel"), role: .cancel) {}
        }
    }

    private static func isSelf(_ pr: [String: Any]) -> Bool { pr["site"] as? String == "saat-tarih" || pr["kendisi"] as? Bool == true }

    // 9. Verilerimi indir
    @ViewBuilder private var downloadSection: some View {
        Section {
            // Döküm paylaşım penceresiyle kaydedilir (Dosyalar'a, e-postaya…)
            Button(A("download")) {
                run("data", { await Account.shared.download() }, ok: nil) { r in
                    let url = FileManager.default.temporaryDirectory.appendingPathComponent("saat-tarih-hesap.json")
                    if (try? (r.data ?? Data("{}".utf8)).write(to: url, options: .atomic)) != nil { export = url }
                }
            }
            message("data")
        } header: { Numbered(n: 9, title: A("download")) } footer: { Text(A("downloadNote")) }
        .sheet(item: Binding(get: { export.map(ExportFile.init) }, set: { if $0 == nil { export = nil } })) { f in
            AccountShareSheet(items: [f.url])
        }
    }

    // 10. Hesabı sil
    @ViewBuilder private var deleteSection: some View {
        Section {
            Text(A("deleteLead")).font(.subheadline)
            Picker("", selection: $scopeAll) {
                Text(A("deleteScopeProduct")).tag(false)
                Text(A("deleteScopeAll")).tag(true)
            }
            .pickerStyle(.inline)
            .labelsHidden()
            SecureField(A("deletePassword"), text: $deletePassword).textContentType(.password)
            Button(A("delete"), role: .destructive) { confirmDelete = true }
            message("delete")
        } header: { Numbered(n: 10, title: A("delete")) } footer: { Text(A("deviceKeeps")) }
        .confirmationDialog(A("deleteConfirm"), isPresented: $confirmDelete, titleVisibility: .visible) {
            Button(A("delete"), role: .destructive) {
                let scope = scopeAll ? "hepsi" : "urun", pw = deletePassword
                run("delete", { await Account.shared.delete(scope: scope, password: pw) }, ok: nil) { _ in
                    deletePassword = ""; onSignedOut("account.deleted")
                }
            }
            Button(A("cancel"), role: .cancel) {}
        }
    }
}

private extension Binding where Value == String {
    /// Alan uzunluğu sınırı (sunucudaki sınırlarla aynı)
    func limited(_ n: Int) -> Binding<String> {
        Binding(get: { wrappedValue }, set: { wrappedValue = String($0.prefix(n)) })
    }
}

private struct ExportFile: Identifiable {
    let url: URL
    var id: String { url.path }
}

/// Sistem paylaşım penceresi (dökümü Dosyalar'a kaydetmek ya da göndermek için)
private struct AccountShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController { UIActivityViewController(activityItems: items, applicationActivities: nil) }
    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}
