import Foundation
import Observation
import CryptoKit

/// Hesapla eşitleme (sahibin kararı S1, 12 Kasım 2026: kayıtlar hem hesapta hem iCloud'da). Eşitlenen anahtarlar
/// CloudSync.keys'tir; değer JSON ilkel değeridir ("true", "5", "\"metin\"") ki Android ve web aynı kaydı okusun.
/// Anahtar başına son yazan kazanır: sunucu `t` (Unix ms) tutar, cihaz son eşitlenen değerin özetini ve zamanını tutar
/// (`account.sync`; iCloud'a ve hesaba gitmez). iCloud eşitlemesi ayrıca sürer: hesaptan gelen değer UserDefaults'a
/// yazılınca CloudSync onu öbür Apple cihazlarına da taşır.
/// Türler Android'deki AccountSync.TYPES ile aynıdır (check-swift.mjs denetler); iPhone'da `countdowns` ve
/// `timers.history` Data olarak, `clockModel` kimlikle saklanır (hesapta Android'deki gibi sıra numarası).
/// Rozetler (madde 13): bu cihazda kayıt olunan hesaba ilk girişte sormadan geçer; var olan hesaba girişte bir kez sorulur;
/// hesaptaki rozetler cihaza gelir.
@MainActor @Observable
final class AccountSync {
    static let shared = AccountSync()

    enum Kind { case s, i, b, f }

    nonisolated static let types: [String: Kind] = {
        let s = "metro classroom talking.every appearance accent units clockFont digital.style world.view qibla.style qibla.favs homeItems " +
            "features features.bar all.favs cal.mode cal.country cal.hidden cal.extra fsBg nightMode fs.text world places.saved countdowns " +
            "hiit.cfg hiit.routine hiit.log world.labels timers.visual routines timers.history journal meet.cities prayer.method " +
            "pomo.log pomo.todo study.log study.subjects wake.events pomo.task pomo.tag pomo.noise sleep.days sleep.noise quran.bookmark " +
            "zikir.kind track.prayer track.kaza track.fast zakat.values zakat.std track.when track.pauses track.pausedFrom track.kazaLog " +
            "track.life quran.hatim quran.read quran.theme dua.favs zikir.custom prayer.visual hijri.cal meet.h12 atmo night.color " +
            "dial.lume look.digitColor flip.card"
        let i = "clockModel fsSlideEvery pomo.goal pomo.vol pomo.work pomo.short pomo.long pomo.rounds sleep.wake sleep.dur sleep.remind " +
            "sleep.vol sleep.timer quran.size zikir.target zikir.total quran.goal hijri.offset fontScale night.from night.to"
        let b = "talking.chime h12 keepAwake secA secD cal.hijriNav tools.voice hiit.cue.next hiit.cue.half hiit.cue.ten timers.keypad " +
            "prayer.hanafi sleep.on quran.en zikir.tesbihat track.inline zikir.beads zikir.shake prayer.words meet.exchanges look.outline " +
            "flip.pair flip.smallSec flip.hinge"
        var out: [String: Kind] = [:]
        for k in s.split(separator: " ") { out[String(k)] = .s }
        for k in i.split(separator: " ") { out[String(k)] = .i }
        for k in b.split(separator: " ") { out[String(k)] = .b }
        out["fs.scale"] = .f
        return out
    }()
    /// iPhone'da JSON metni Data olarak saklanan anahtarlar
    nonisolated static let dataKeys: Set<String> = ["countdowns", "timers.history"]

    private(set) var busy = false
    /// Var olan hesaba girişte sorulacak rozet sayısı (0: soru yok)
    var askBadges = 0
    private(set) var accountBadges = 0
    private(set) var records = 0

    private let d = UserDefaults.standard
    private static let syncKey = "account.sync", syncUserKey = "account.syncUser"

    /// Cihazdaki değer JSON ilkel değeri olarak; yoksa nil
    func encode(_ key: String) -> String? {
        guard let kind = Self.types[key], let v = d.object(forKey: key) else { return nil }
        switch kind {
        case .s:
            let s: String? = (v as? String) ?? (v as? Data).map { String(decoding: $0, as: UTF8.self) }
            guard let s, let j = try? JSONSerialization.data(withJSONObject: s, options: .fragmentsAllowed) else { return nil }
            return String(decoding: j, as: UTF8.self)
        case .i:
            if key == "clockModel" {
                guard let id = v as? String, let n = ClockModel.allCases.firstIndex(where: { $0.rawValue == id }) else { return nil }
                return String(n)
            }
            return (v as? NSNumber).map { String($0.intValue) }
        case .b: return (v as? NSNumber).map { $0.boolValue ? "true" : "false" }
        case .f: return (v as? NSNumber).map { String($0.doubleValue) }
        }
    }

    /// Hesaptaki değeri cihaza yazar; tür tutmazsa (bozuk kayıt) yazmaz
    func apply(_ key: String, _ json: String?) {
        guard let kind = Self.types[key] else { return }
        guard let json else { d.removeObject(forKey: key); return }
        guard let v = try? JSONSerialization.jsonObject(with: Data(json.utf8), options: .fragmentsAllowed) else { return }
        let isBool = (v as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } ?? false
        switch kind {
        case .s:
            guard let s = v as? String else { return }
            if Self.dataKeys.contains(key) { d.set(Data(s.utf8), forKey: key) } else { d.set(s, forKey: key) }
        case .i:
            guard let n = v as? NSNumber, !isBool else { return }
            if key == "clockModel" {
                guard ClockModel.allCases.indices.contains(n.intValue) else { return }
                d.set(ClockModel.allCases[n.intValue].rawValue, forKey: key)
            } else { d.set(n.intValue, forKey: key) }
        case .b:
            guard let n = v as? NSNumber, isBool else { return }
            d.set(n.boolValue, forKey: key)
        case .f:
            guard let n = v as? NSNumber, !isBool else { return }
            d.set(n.doubleValue, forKey: key)
        }
    }

    private static func hash(_ v: String?) -> String {
        guard let v else { return "-" }
        return SHA256.hash(data: Data(v.utf8)).prefix(12).map { String(format: "%02x", $0) }.joined()
    }

    private func synced() -> [String: [String: Any]] {
        (d.data(forKey: Self.syncKey).flatMap { try? JSONSerialization.jsonObject(with: $0) } as? [String: [String: Any]]) ?? [:]
    }

    /// Çıkışta ve silmede eşitleme durumu unutulur (cihazdaki kayıtlar kalır)
    func forget() {
        d.removeObject(forKey: Self.syncKey)
        d.removeObject(forKey: Self.syncUserKey)
        accountBadges = 0; records = 0; askBadges = 0
    }

    /// Kayıtlar ve rozetler. `first`: girişten hemen sonra; o hesapla ilk eşitlemede hesaptaki değer kazanır, hesapta
    /// olmayan cihaz değeri gönderilir. Sonraki eşitlemelerde değişen taraf kazanır, ikisi de değiştiyse cihaz (son yazan).
    @discardableResult
    func sync(first: Bool = false, now: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) async -> Bool {
        let account = Account.shared
        guard account.signedIn, !busy, let uid = account.user?.id else { return false }
        busy = true
        defer { busy = false }
        let fresh = first || d.string(forKey: Self.syncUserKey) != uid
        var map = fresh ? [:] : synced()
        let r = await account.call("GET", "/v1/kayitlar", auth: true)
        guard r.code == 200 else { return false }
        let server = Account.json(r.body)?["kayitlar"] as? [String: [String: Any]] ?? [:]
        var push: [String: Any] = [:]
        for key in Self.types.keys.sorted() {
            let local = encode(key)
            let srv = server[key]
            let srvV = srv?["v"] as? String
            let srvT = (srv?["t"] as? NSNumber)?.int64Value ?? 0
            if fresh {
                if srv != nil {
                    if srvV != local { apply(key, srvV) }
                    map[key] = ["h": Self.hash(srvV), "t": srvT]
                } else if let local {
                    push[key] = ["v": local, "t": now] as [String: Any]
                    map[key] = ["h": Self.hash(local), "t": now]
                }
                continue
            }
            let last = map[key]
            let localChanged = Self.hash(local) != (last?["h"] as? String ?? "-")
            let serverChanged = srv != nil && srvT > ((last?["t"] as? NSNumber)?.int64Value ?? 0)
            if localChanged {
                push[key] = ["v": local.map { $0 as Any } ?? NSNull(), "t": now] as [String: Any]
                map[key] = ["h": Self.hash(local), "t": now]
            } else if serverChanged {
                apply(key, srvV)
                map[key] = ["h": Self.hash(srvV), "t": srvT]
            }
        }
        if !push.isEmpty {
            let p = await account.call("POST", "/v1/kayitlar", ["degisiklik": push], auth: true)
            guard p.code == 200 else { return false }
        }
        d.set(try? JSONSerialization.data(withJSONObject: map), forKey: Self.syncKey)
        d.set(uid, forKey: Self.syncUserKey)
        records = server.count + push.keys.filter { server[$0] == nil }.count
        await syncBadges(first: first)
        return true
    }

    private var localBadges: [String: String] { BadgeStore.shared.saved.r }

    private func syncBadges(first: Bool) async {
        let account = Account.shared
        let r = await account.call("GET", "/v1/rozetler", auth: true)
        guard r.code == 200, let uid = account.user?.id else { return }
        let list = Account.json(r.body)?["rozetler"] as? [[String: Any]] ?? []
        var server: [String: String] = [:]
        for o in list { if let id = o["id"] as? String, let g = o["gun"] as? String { server[id] = g } }
        let missing = localBadges.filter { id, day in server[id].map { day < $0 } ?? true }
        if !missing.isEmpty, first {
            if let email = account.user?.email, email == account.createdHere {
                await addBadges(missing)
                account.createdHere = nil
            } else if !account.badgesAsked(uid) {
                askBadges = missing.count
            }
        }
        BadgeStore.shared.mergeFromAccount(server)
        accountBadges = max(server.count, accountBadges)
    }

    /// "Bu cihazdaki rozetleri hesabıma ekle" ve sorudaki "Evet"
    @discardableResult
    func addBadges(_ only: [String: String]? = nil) async -> Bool {
        let list = only ?? localBadges
        if list.isEmpty { return true }
        let body: [String: Any] = ["rozetler": list.map { ["id": $0.key, "gun": $0.value] }]
        let r = await Account.shared.call("POST", "/v1/rozetler", body, auth: true)
        guard r.code == 200 else { return false }
        accountBadges = (Account.json(r.body)?["rozetler"] as? [Any])?.count ?? accountBadges
        if let id = Account.shared.user?.id { Account.shared.setBadgesAsked(id) }
        askBadges = 0
        return true
    }

    func declineBadges() {
        if let id = Account.shared.user?.id { Account.shared.setBadgesAsked(id) }
        askBadges = 0
    }
}
