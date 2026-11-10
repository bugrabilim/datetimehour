import Foundation
import Security
import Observation

/// Uygulamalardaki hesap (üyelik aşama 1, 10 Kasım 2026; docs/uyelik-plani-2026-11.md): e-posta + şifre, hesap sunucusu
/// hesap.time.bumba.tr (server/, Better Auth). Hesap isteğe bağlıdır; hiçbir özellik hesap istemez (grup özellikleri
/// gelene kadar). Oturum belirteci Anahtar Zinciri'nde "yalnız bu cihaz" erişimiyle durur (iCloud Anahtar Zinciri'ne ve
/// başka cihaza geçmez); CloudSync bu anahtarları eşitlemez. Android'deki Account.kt ile aynı uçlar ve hata eşlemesi.
@MainActor @Observable
final class Account {
    static let shared = Account()
    static let base = "https://hesap.time.bumba.tr"

    struct User: Codable, Equatable {
        var id: String
        var email: String
        var name: String
        var lang: String
    }

    struct Resp {
        let code: Int
        let body: Data
        let token: String?
    }

    /// Sonuç: `err` boşsa başarılı; doluysa `account.err.*` metninin anahtarı
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

    nonisolated private static func json(_ d: Data) -> [String: Any]? { (try? JSONSerialization.jsonObject(with: d)) as? [String: Any] }

    private static func parseUser(_ o: [String: Any]) -> User {
        User(id: o["id"] as? String ?? "", email: o["email"] as? String ?? "", name: o["name"] as? String ?? "",
             lang: o["lang"] as? String ?? "en")
    }

    /// Better Auth hata kodlarını ve durumlarını uygulamanın metinlerine çevirir
    nonisolated static func errorOf(_ r: Resp, wrongPassword: Bool = false) -> String {
        let code = json(r.body)?["code"] as? String ?? ""
        if r.code == 0 { return "account.err.network" }
        if r.code == 429 { return "account.err.tooMany" }
        if code == "EMAIL_NOT_VERIFIED" { return "account.err.unverified" }
        if code == "INVALID_EMAIL_OR_PASSWORD" || (r.code == 401 && !wrongPassword) { return "account.err.credentials" }
        if code == "INVALID_PASSWORD" || (wrongPassword && (r.code == 400 || r.code == 401)) { return "account.err.wrongPassword" }
        if code == "INVALID_EMAIL" { return "account.err.email" }
        if code.hasPrefix("PASSWORD_TOO") { return "account.err.password" }
        return "account.err.generic"
    }

    nonisolated static func validEmail(_ e: String) -> Bool {
        e.trimmingCharacters(in: .whitespaces).range(of: #"^[^@\s]+@[^@\s]+\.[^@\s]+$"#, options: .regularExpression) != nil
    }
    nonisolated static func validPassword(_ p: String) -> Bool { (8...128).contains(p.count) }

    private func call(_ method: String, _ path: String, _ body: [String: Any]? = nil, auth: Bool = false) async -> Resp {
        var rq = URLRequest(url: URL(string: Self.base + path)!, timeoutInterval: 20)
        rq.httpMethod = method
        rq.setValue("application/json", forHTTPHeaderField: "Accept")
        rq.setValue("SaatTarih-iOS", forHTTPHeaderField: "User-Agent")
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

    func signUp(email: String, password: String, name: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        guard Self.validPassword(password) else { return .fail("account.err.password") }
        let r = await call("POST", "/api/auth/sign-up/email", ["email": email.trimmingCharacters(in: .whitespaces), "password": password,
                                                               "name": name.trimmingCharacters(in: .whitespaces), "lang": Self.lang])
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    func signIn(email: String, password: String) async -> Result {
        guard Self.validEmail(email) else { return .fail("account.err.email") }
        let r = await call("POST", "/api/auth/sign-in/email", ["email": email.trimmingCharacters(in: .whitespaces), "password": password])
        guard r.code == 200 else { return .fail(Self.errorOf(r)) }
        let o = Self.json(r.body)
        guard let t = r.token ?? (o?["token"] as? String).flatMap({ $0.isEmpty ? nil : $0 }),
              let u = o?["user"] as? [String: Any] else { return .fail("account.err.generic") }
        save(t, Self.parseUser(u))
        await syncLang()
        return .success
    }

    /// Sunucudaki güncel bilgiler; oturum geçersizse (silinmiş, şifre başka yerde değişmiş) çıkış
    @discardableResult
    func refresh() async -> Result {
        guard token != nil else { return .success }
        let r = await call("GET", "/api/auth/get-session", auth: true)
        if r.code == 0 { return .fail("account.err.network") }
        let o = Self.json(r.body)
        if r.code == 401 || (r.code == 200 && o?["user"] == nil) { save(nil, nil); return .success }
        guard r.code == 200, let u = o?["user"] as? [String: Any] else { return .fail(Self.errorOf(r)) }
        save(token, Self.parseUser(u))
        await syncLang()
        return .success
    }

    /// Hesaba gelen e-postalar uygulamanın dilinde gitsin: dil değişince sunucuya yazılır
    func syncLang() async {
        guard var u = user, u.lang != Self.lang else { return }
        let r = await call("POST", "/api/auth/update-user", ["lang": Self.lang], auth: true)
        if r.code == 200 { u.lang = Self.lang; save(token, u) }
    }

    func updateName(_ name: String) async -> Result {
        guard var u = user else { return .fail("account.err.generic") }
        let n = name.trimmingCharacters(in: .whitespaces)
        let r = await call("POST", "/api/auth/update-user", ["name": n], auth: true)
        guard r.code == 200 else { return .fail(Self.errorOf(r)) }
        u.name = n
        save(token, u)
        return .success
    }

    func changeEmail(_ newEmail: String) async -> Result {
        guard Self.validEmail(newEmail) else { return .fail("account.err.email") }
        let r = await call("POST", "/api/auth/change-email", ["newEmail": newEmail.trimmingCharacters(in: .whitespaces)], auth: true)
        return r.code == 200 ? .success : .fail(Self.errorOf(r))
    }

    /// Diğer oturumlar kapanır; sunucu bu cihaz için yeni belirteç verir
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

    /// Verilerimi indir (KVKK md. 11): sunucunun JSON dökümü
    func download() async -> Result {
        let r = await call("GET", "/v1/verilerim", auth: true)
        return r.code == 200 ? Result(data: r.body) : .fail(Self.errorOf(r))
    }

    func signOut() async {
        if token != nil { _ = await call("POST", "/api/auth/sign-out", [:], auth: true) }
        save(nil, nil)
    }

    func delete(password: String) async -> Result {
        let r = await call("POST", "/api/auth/delete-user", ["password": password], auth: true)
        guard r.code == 200 else { return .fail(Self.errorOf(r, wrongPassword: true)) }
        save(nil, nil)
        return .success
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

/// Sitedeki aydınlatma metni (gizlilik › hesap bölümü) ve hesap silme sayfası, uygulamanın dilinde
enum AccountLinks {
    static var notice: URL {
        URL(string: "https://time.bumba.tr" + (AppLocale.code == "tr" ? "/gizlilik/#hesap" : "/\(AppLocale.code)/privacy/#account"))!
    }
    static var deletion: URL {
        URL(string: "https://time.bumba.tr" + (AppLocale.code == "tr" ? "/hesap-silme/" : "/\(AppLocale.code)/account-deletion/"))!
    }
}
