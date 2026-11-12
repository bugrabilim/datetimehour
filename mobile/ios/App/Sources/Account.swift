import Foundation
import Security
import Observation
import ImageIO
import UniformTypeIdentifiers

/// Uygulamalardaki hesap (Bumba Genel Standartlar 5.15, madde 7 ve 13; 12 Kasım 2026): e-posta + şifre, üyelik sunucusu
/// hesap.time.bumba.tr (server/, Better Auth). Hesap isteğe bağlıdır; hiçbir özellik hesap istemez. Kayıt ekranı, onay
/// cümlesi, şifre kuralı ve hesap sayfası (10 bölüm) web'dekiyle aynıdır; metinler server/i18n/account.json (`account.*`).
/// Oturum belirteci Anahtar Zinciri'nde "yalnız bu cihaz" erişimiyle durur (iCloud Anahtar Zinciri'ne ve başka cihaza
/// geçmez); CloudSync bu anahtarları eşitlemez. İstekler `X-Saat-Istemci: ios` taşır (sunucuda 90 günlük uygulama
/// oturumu). Android'deki Account.kt ile aynı uçlar ve hata eşlemesi.
@MainActor @Observable
final class Account {
    static let shared = Account()
    static let base = "https://hesap.time.bumba.tr"

    struct User: Codable, Equatable {
        var id: String
        var email: String
        var lang: String
    }

    struct Resp {
        let code: Int
        let body: Data
        let token: String?
    }

    /// Sonuç: `err` boşsa başarılı; doluysa `account.err.*` metninin anahtarı (ya da "account.bumba": Bumba üyesi)
    struct Result {
        var err: String?
        var data: Data?
        var ok: Bool { err == nil }
        static let success = Result()
        static func fail(_ key: String) -> Result { Result(err: key) }
    }

    private(set) var user: User?
    private var token: String?
    private static let userKey = "account.user"
    private static let tzKey = "account.tz"

    var signedIn: Bool { user != nil && token != nil }

    private init() {
        token = Keychain.read()
        user = token == nil ? nil : UserDefaults.standard.data(forKey: Self.userKey).flatMap { try? JSONDecoder().decode(User.self, from: $0) }
        // Belirteç yoksa (yeni telefon, yedekten geri yükleme) çıkış sayılır
        if token == nil { UserDefaults.standard.removeObject(forKey: Self.userKey) }
    }

    private func save(_ newToken: String?, _ u: User?) {
        token = newToken
        user = newToken == nil ? nil : u
        if let newToken, let u {
            Keychain.write(newToken)
            UserDefaults.standard.set(try? JSONEncoder().encode(u), forKey: Self.userKey)
        } else {
            Keychain.delete()
            UserDefaults.standard.removeObject(forKey: Self.userKey)
        }
    }

    /// Bu cihazda kayıt olunan adres: ilk girişte cihazdaki rozetler sormadan hesaba geçer (madde 13, "Üye")
    var createdHere: String? {
        get { UserDefaults.standard.string(forKey: "account.new") }
        set { UserDefaults.standard.set(newValue, forKey: "account.new") }
    }
    /// Var olan hesaba girişte rozet sorusu her hesap için bir kez
    func badgesAsked(_ id: String) -> Bool { UserDefaults.standard.bool(forKey: "account.asked.\(id)") }
    func setBadgesAsked(_ id: String) { UserDefaults.standard.set(true, forKey: "account.asked.\(id)") }

    nonisolated static func json(_ d: Data?) -> [String: Any]? {
        guard let d else { return nil }
        return (try? JSONSerialization.jsonObject(with: d)) as? [String: Any]
    }

    private static func parseUser(_ o: [String: Any]) -> User {
        User(id: o["id"] as? String ?? "", email: o["email"] as? String ?? "", lang: o["lang"] as? String ?? "en")
    }

    /// Better Auth ve sunucunun hata kodlarını uygulamanın metinlerine çevirir (Android'deki errorOf ile aynı sıra)
    nonisolated static func errorOf(_ r: Resp, wrongPassword: Bool = false) -> String {
        let o = json(r.body)
        let code = (o?["code"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? (o?["hata"] as? String ?? "")
        if r.code == 0 { return "account.err.network" }
        if r.code == 429 { return "account.err.tooMany" }
        if code == "EMAIL_NOT_VERIFIED" { return "account.err.unverified" }
        if code == "BANNED_USER" { return "account.err.suspended" }
        if code == "BUMBA_MEMBER" { return "account.bumba" }
        if code == "EMAIL_TAKEN" || code == "USER_ALREADY_EXISTS" { return "account.err.emailTaken" }
        if code == "WEAK_PASSWORD" || code.hasPrefix("PASSWORD_TOO") { return "account.err.password" }
        if code == "INVALID_PASSWORD" || code == "sifre" || (wrongPassword && (r.code == 400 || r.code == 401)) { return "account.err.wrongPassword" }
        if code == "INVALID_EMAIL_OR_PASSWORD" || code == "bilgiler" || r.code == 401 { return "account.err.credentials" }
        if code == "INVALID_EMAIL" || code == "eposta" { return "account.err.email" }
        if code.hasPrefix("gecersiz:") { return "account.err.field:" + code.dropFirst("gecersiz:".count) }
        return "account.err.generic"
    }

    nonisolated static func validEmail(_ e: String) -> Bool {
        e.trimmingCharacters(in: .whitespaces).range(of: #"^[^@\s]+@[^@\s]+\.[^@\s]+$"#, options: .regularExpression) != nil
    }

    /// Şifre kuralı (standart madde 7; sunucudaki passwordChecks ile aynı): ekranda dört satır
    nonisolated static func passwordRules(_ p: String) -> [(String, Bool)] {
        [("pwLen", (8...128).contains(p.count) && !p.contains { $0.isWhitespace }),
         ("pwDigit", p.contains { $0.isNumber }),
         ("pwCase", p.contains { $0.isUppercase } && p.contains { $0.isLowercase }),
         ("pwSymbol", p.contains { !$0.isLetter && !$0.isNumber && !$0.isWhitespace })]
    }
    nonisolated static func validPassword(_ p: String) -> Bool { passwordRules(p).allSatisfy { $0.1 } }

    func call(_ method: String, _ path: String, _ body: [String: Any]? = nil, auth: Bool = false) async -> Resp {
        var rq = URLRequest(url: URL(string: Self.base + path)!, timeoutInterval: 20)
        rq.httpMethod = method
        rq.setValue("application/json", forHTTPHeaderField: "Accept")
        rq.setValue("SaatTarih-iOS", forHTTPHeaderField: "User-Agent")
        rq.setValue("ios", forHTTPHeaderField: "X-Saat-Istemci")
        if auth, let token { rq.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if let body {
            rq.setValue("application/json", forHTTPHeaderField: "Content-Type")
            rq.httpBody = try? JSONSerialization.data(withJSONObject: body)
        }
        do {
            let (data, res) = try await URLSession.shared.data(for: rq)
            let h = res as? HTTPURLResponse
            return Resp(code: h?.statusCode ?? 0, body: data, token: h?.value(forHTTPHeaderField: "set-auth-token"))
        } catch {
            return Resp(code: 0, body: Data(), token: nil)
        }
    }

    private static var lang: String { AppLocale.code }
    private static func clean(_ e: String) -> String { e.trimmingCharacters(in: .whitespaces).lowercased() }

    /// Kayıt ekranı: yazılan e-posta başka bir Bumba ürününde kayıtlı mı ("var" ise soru gösterilir)
    func lookup(_ email: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        let r = await call("POST", "/v1/uyelik/sorgula", ["eposta": Self.clean(email)])
        guard r.code == 200 else { return .fail(Self.errorOf(r)) }
        return Result(data: Data(((Self.json(r.body)?["durum"] as? String) ?? "yok").utf8))
    }

    /// Kayıt: yalnız e-posta ve şifre (onay kaydını sunucu tutar); dil ve saat dilimi e-postalar için
    func signUp(email: String, password: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        guard Self.validPassword(password) else { return .fail("account.err.password") }
        let r = await call("POST", "/api/auth/sign-up/email", ["email": Self.clean(email), "password": password, "name": "",
                                                               "lang": Self.lang, "timeZone": TimeZone.current.identifier])
        guard r.code == 200 else { return .fail(Self.errorOf(r)) }
        createdHere = Self.clean(email)
        return .success
    }

    private func keep(_ r: Resp) -> Result {
        let o = Self.json(r.body)
        guard let t = r.token ?? (o?["token"] as? String).flatMap({ $0.isEmpty ? nil : $0 }),
              let u = o?["user"] as? [String: Any] else { return .fail("account.err.generic") }
        save(t, Self.parseUser(u))
        return .success
    }

    func signIn(email: String, password: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        let r = await call("POST", "/api/auth/sign-in/email", ["email": email.trimmingCharacters(in: .whitespaces), "password": password])
        guard r.code == 200 else { return .fail(Self.errorOf(r)) }
        let k = keep(r)
        if k.ok { await afterSignIn() }
        return k
    }

    /// "Evet": başka bir Bumba ürünündeki üyelikle giriş; hesap burada açılır, numara aynı kalır
    func link(email: String, password: String) async -> Result {
        let r = await call("POST", "/v1/uyelik/bagla", ["eposta": Self.clean(email), "sifre": password, "dil": Self.lang])
        guard r.code == 200 else { return .fail(Self.errorOf(r)) }
        let k = keep(r)
        if k.ok { await afterSignIn() }
        return k
    }

    private func afterSignIn() async {
        await syncPrefs()
        await AccountSync.shared.sync(first: true)
    }

    /// Girişten sonra: aynı e-postayla başka bir Bumba ürününde ayrı hesap var mı
    func separateAccount() async -> Bool {
        let r = await call("GET", "/v1/uyelik/durum", auth: true)
        return r.code == 200 && (Self.json(r.body)?["ayriHesap"] as? Bool ?? false)
    }

    func merge(password: String) async -> Result {
        let r = await call("POST", "/v1/uyelik/birlestir", ["sifre": password], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r, wrongPassword: true))
    }

    /// Sunucudaki güncel bilgiler; oturum geçersizse (silinmiş, şifre başka yerde değişmiş) çıkış
    @discardableResult
    func refresh() async -> Result {
        guard token != nil else { return .success }
        let r = await call("GET", "/api/auth/get-session", auth: true)
        if r.code == 0 { return .fail("account.err.network") }
        let o = Self.json(r.body)
        if r.code == 401 || (r.code == 200 && o?["user"] == nil) { save(nil, nil); AccountSync.shared.forget(); return .success }
        guard r.code == 200, let u = o?["user"] as? [String: Any] else { return .fail(Self.errorOf(r)) }
        save(token, Self.parseUser(u))
        await syncPrefs()
        return .success
    }

    /// Hesaba gelen e-postalar uygulamanın dilinde gitsin; saat dilimi telefonunki (5. bölüm)
    func syncPrefs() async {
        guard var u = user else { return }
        let tz = TimeZone.current.identifier
        if u.lang == Self.lang && UserDefaults.standard.string(forKey: Self.tzKey) == tz { return }
        let r = await call("POST", "/api/auth/update-user", ["lang": Self.lang, "timeZone": tz], auth: true)
        if r.code == 200 { u.lang = Self.lang; save(token, u); UserDefaults.standard.set(tz, forKey: Self.tzKey) }
    }

    /// Hesap sayfasının verisi: e-posta, numara, üyelik tarihi, kutlama, profil (JSON)
    func load() async -> Result {
        let r = await call("GET", "/v1/hesap", auth: true)
        if r.code == 401 { save(nil, nil); AccountSync.shared.forget(); return .fail("account.err.generic") }
        return r.code == 200 ? Result(data: r.body) : .fail(Self.errorOf(r))
    }

    func saveProfile(_ profile: [String: Any]) async -> Result {
        let r = await call("POST", "/v1/profil", ["profil": profile], auth: true)
        return r.code == 200 ? Result(data: r.body) : .fail(Self.errorOf(r))
    }

    /// Fotoğraf: `data:image/webp;base64,…` (uygulamada 512 piksele küçültülür) ya da nil (kaldır)
    func savePhoto(_ dataUrl: String?) async -> Result {
        let r = await call("POST", "/v1/profil/foto", ["foto": dataUrl.map { $0 as Any } ?? NSNull()], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    func setCelebrate(_ on: Bool) async -> Result {
        let r = await call("POST", "/v1/kutlama", ["acik": on], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    func changeEmail(_ newEmail: String) async -> Result {
        guard Self.validEmail(newEmail) else { return .fail("account.err.email") }
        let r = await call("POST", "/api/auth/change-email", ["newEmail": Self.clean(newEmail)], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    /// Öbür oturumlar kapanır; sunucu bu cihaz için yeni belirteç verir
    func changePassword(current: String, new: String) async -> Result {
        guard Self.validPassword(new) else { return .fail("account.err.password") }
        let r = await call("POST", "/api/auth/change-password", ["currentPassword": current, "newPassword": new, "revokeOtherSessions": true], auth: true)
        guard r.code == 200 else { return .fail(Self.errorOf(r, wrongPassword: true)) }
        if let t = r.token ?? (Self.json(r.body)?["token"] as? String).flatMap({ $0.isEmpty ? nil : $0 }) { save(t, user) }
        return .success
    }

    /// Yanıt hesabın varlığını ele vermez (sunucu her durumda aynı döner)
    func forgot(_ email: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        let r = await call("POST", "/api/auth/request-password-reset", ["email": email.trimmingCharacters(in: .whitespaces)])
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    func resendVerification(_ email: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        let r = await call("POST", "/api/auth/send-verification-email", ["email": email.trimmingCharacters(in: .whitespaces)])
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    /// Açık oturumlar (7. bölüm): JSON dizi; bu oturumun belirteci `isCurrent` ile karşılaştırılır
    func sessions() async -> [[String: Any]] {
        let r = await call("GET", "/api/auth/list-sessions", auth: true)
        guard r.code == 200 else { return [] }
        return (try? JSONSerialization.jsonObject(with: r.body)) as? [[String: Any]] ?? []
    }
    func isCurrent(_ sessionToken: String) -> Bool {
        guard let token else { return false }
        return token == sessionToken || token.split(separator: ".").first.map(String.init) == sessionToken
    }
    func closeSession(_ sessionToken: String) async {
        _ = await call("POST", "/api/auth/revoke-session", ["token": sessionToken], auth: true)
    }
    func closeOtherSessions() async {
        _ = await call("POST", "/api/auth/revoke-other-sessions", [:], auth: true)
    }

    /// Öbür Bumba ürünlerinde üyeliğim (8. bölüm): [{site, ad, adres, acik, kendisi}]; nil: servise ulaşılamadı
    func products() async -> [[String: Any]]? {
        let r = await call("GET", "/v1/uyelik/urunler", auth: true)
        guard r.code == 200 else { return nil }
        return (Self.json(r.body)?["urunler"] as? [[String: Any]] ?? []).filter { !($0["site"] as? String ?? "").isEmpty }
    }
    func openProducts(_ sites: [String], password: String) async -> Result {
        let r = await call("POST", "/v1/uyelik/ac", ["hedefler": sites, "sifre": password], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r, wrongPassword: true))
    }
    func closeProducts(_ sites: [String]) async -> Result {
        let r = await call("POST", "/v1/uyelik/kapat", ["hedefler": sites], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    /// Verilerimi indir (KVKK md. 11): sunucunun JSON dökümü
    func download() async -> Result {
        let r = await call("GET", "/v1/verilerim", auth: true)
        return r.code == 200 ? Result(data: r.body) : .fail(Self.errorOf(r))
    }

    func signOut() async {
        if token != nil { _ = await call("POST", "/api/auth/sign-out", [:], auth: true) }
        save(nil, nil)
        AccountSync.shared.forget()
    }

    /// Hesabı sil: "urun" yalnız Saat Tarih'ten, "hepsi" bütün Bumba ürünlerinden. Şifre son 24 saatte girişte gerekmez.
    func delete(scope: String, password: String) async -> Result {
        var body: [String: Any] = ["kapsam": scope]
        if !password.isEmpty { body["sifre"] = password }
        let r = await call("POST", "/v1/hesap/sil", body, auth: true)
        guard r.code == 200 else { return .fail(Self.errorOf(r, wrongPassword: true)) }
        save(nil, nil)
        AccountSync.shared.forget()
        return .success
    }

    /// Fotoğraf: kare kırpılıp en çok 512 piksele küçültülür, WebP (≤ ~190 KB); yeniden kodlandığı için EXIF ve konum
    /// düşer. iOS'un görüntü kitaplığı WebP yazamıyorsa nil (ekranda hata).
    nonisolated static func photoDataURL(_ data: Data) -> String? {
        guard let src = CGImageSourceCreateWithData(data as CFData, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(src, 0, nil) as? [CFString: Any] else { return nil }
        let w = props[kCGImagePropertyPixelWidth] as? Int ?? 0, h = props[kCGImagePropertyPixelHeight] as? Int ?? 0
        guard w > 0, h > 0 else { return nil }
        // Kısa kenar en az 512 kalacak biçimde küçültülmüş, yönü düzeltilmiş görüntü
        let maxSide = max(512, Int(Double(max(w, h)) * 512 / Double(min(w, h))))
        let opts: [CFString: Any] = [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceCreateThumbnailWithTransform: true,
                                     kCGImageSourceThumbnailMaxPixelSize: maxSide]
        guard let img = CGImageSourceCreateThumbnailAtIndex(src, 0, opts as CFDictionary) else { return nil }
        let side = min(img.width, img.height)
        guard let square = img.cropping(to: CGRect(x: (img.width - side) / 2, y: (img.height - side) / 2, width: side, height: side)) else { return nil }
        let size = min(512, side)
        guard let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
                                  space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return nil }
        ctx.interpolationQuality = .high
        ctx.draw(square, in: CGRect(x: 0, y: 0, width: size, height: size))
        guard let out = ctx.makeImage() else { return nil }
        var q = 0.85
        while q >= 0.35 {
            let buf = NSMutableData()
            guard let dest = CGImageDestinationCreateWithData(buf as CFMutableData, UTType.webP.identifier as CFString, 1, nil) else { return nil }
            CGImageDestinationAddImage(dest, out, [kCGImageDestinationLossyCompressionQuality: q] as CFDictionary)
            guard CGImageDestinationFinalize(dest) else { return nil }
            if buf.length <= 190 * 1024 { return "data:image/webp;base64," + (buf as Data).base64EncodedString() }
            q -= 0.1
        }
        return nil
    }

    /// Anahtar Zinciri: yalnız bu cihaz, ilk kilit açılışından sonra (arka plandaki işler de okuyabilsin);
    /// eşitlenmez, yedekten başka cihaza geri yüklenmez
    private enum Keychain {
        static let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword,
                                           kSecAttrService as String: "tr.bumba.time.account",
                                           kSecAttrAccount as String: "token"]

        static func read() -> String? {
            var q = query
            q[kSecReturnData as String] = true
            q[kSecMatchLimit as String] = kSecMatchLimitOne
            var out: AnyObject?
            guard SecItemCopyMatching(q as CFDictionary, &out) == errSecSuccess, let d = out as? Data else { return nil }
            return String(data: d, encoding: .utf8)
        }

        static func write(_ token: String) {
            delete()
            var q = query
            q[kSecValueData as String] = Data(token.utf8)
            q[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(q as CFDictionary, nil)
        }

        static func delete() { SecItemDelete(query as CFDictionary) }
    }
}

/// Ortak hukuk metinleri (standart madde 7 "Hukuk"; sahibin kararı S2, 12 Kasım 2026): Türkçe arayüzden Türkçe metne,
/// öbür dillerden ?dil=en metnine. Hesap silme bilgi sayfası sitededir (herkese açık, mağaza kuralı).
enum AccountLinks {
    static var suffix: String { AppLocale.code == "tr" ? "" : "?dil=en" }
    static var notice: URL { URL(string: "https://bumbagroup.com/uyelik-aydinlatma" + suffix)! }
    static var terms: URL { URL(string: "https://bumbagroup.com/uyelik-sozlesmesi" + suffix)! }
    static var deletion: URL {
        URL(string: "https://time.bumba.tr" + (AppLocale.code == "tr" ? "/hesap-silme/" : "/\(AppLocale.code)/account-deletion/"))!
    }
}
