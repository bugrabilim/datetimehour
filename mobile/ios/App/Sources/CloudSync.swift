import Foundation

/*
 * Sunucusuz eşitleme (30 Ekim 2026, sahibin kararı: hesap ve sunucu yok). Aynı Apple hesabındaki iPhone, iPad ve Mac
 * arasında ayarlar ve kişisel kayıtlar iCloud anahtar-değer deposuyla (NSUbiquitousKeyValueStore) eşitlenir; veri
 * kullanıcının iCloud hesabında durur, bize gelmez. Yalnız aşağıdaki anahtarlar: cihaza özgü olanlar (konum, alarmlar,
 * süren sayaçlar, bildirim izinleri, rozetler, fotoğraflar) eşitlenmez. Çakışmada son yazan kazanır. İlk açılışta
 * iCloud'daki değer yereldekinin önüne geçer (yeni cihaz eski cihazın ayarlarını alır); iOS ilk indirme bitmeden
 * yapılan yazmaları kendisi yok sayar. Ayarlar › "iCloud ile eşitle" (`sync.on`, varsayılan açık).
 * Yetki: App/SaatTarih.entitlements `com.apple.developer.ubiquity-kvstore-identifier`; App ID'de iCloud açık olmalı.
 */
@MainActor
enum CloudSync {
    nonisolated static let enabledKey = "sync.on"
    private static let initKey = "sync.inited"

    /// Eşitlenen anahtarlar (Android'deki yedekle aynı adlar; değer biçimleri iki platformda aynı)
    static let keys: [String] = [
        // Görünüm ve saat
        "appearance", "accent", "h12", "units", "keepAwake", "clockModel", "clockFont", "secA", "secD", "homeItems",
        Features.key, FullscreenBackground.key, NightMode.key, "fs.text", "fs.scale", FullscreenPhoto.slideEveryKey,
        // Kişisel listeler
        "world", SavedPlaces.key, "countdowns", "tools.voice", "hiit.cfg",
        // Pomodoro ve uyku
        PomoLog.key, PomoTodo.key, "pomo.task", "pomo.goal", "pomo.tag", "pomo.noise", "pomo.vol", "pomo.work", "pomo.short", "pomo.long", "pomo.rounds",
        SleepPlan.onKey, SleepPlan.wakeKey, SleepPlan.durationKey, SleepPlan.daysKey, SleepPlan.remindKey,
        SleepSounds.kindKey, SleepSounds.volumeKey, SleepSounds.timerKey,
        // İbadet
        Quran.bookmarkKey, "quran.size", "quran.en", "zikir.kind", "zikir.target", "zikir.tesbihat", "zikir.total",
        "track.prayer", "track.kaza", "track.fast", "zakat.values",
    ]

    private static let store = NSUbiquitousKeyValueStore.default
    private static var started = false
    private static var pushTask: Task<Void, Never>?
    /// iCloud'dan gelen değer yazılırken oluşan değişiklik bildirimi geri gönderilmesin
    private static var applying = false

    nonisolated static var enabled: Bool { UserDefaults.standard.object(forKey: enabledKey) as? Bool ?? true }

    /// Uygulama açılışında (SaatTarihApp.init) ve ayar açılınca
    static func start() {
        guard enabled else { return }
        if !started {
            started = true
            NotificationCenter.default.addObserver(forName: NSUbiquitousKeyValueStore.didChangeExternallyNotification,
                                                   object: store, queue: .main) { note in
                let changed = note.userInfo?[NSUbiquitousKeyValueStoreChangedKeysKey] as? [String]
                MainActor.assumeIsolated { pull(changed ?? keys) }
            }
            NotificationCenter.default.addObserver(forName: UserDefaults.didChangeNotification, object: nil, queue: .main) { _ in
                MainActor.assumeIsolated { schedulePush() }
            }
        }
        store.synchronize()
        if !UserDefaults.standard.bool(forKey: initKey) {
            // Bu cihazda ilk kez: iCloud'da olan değerler alınır, sonra yereldeki diğerleri gönderilir
            pull(keys)
            UserDefaults.standard.set(true, forKey: initKey)
        }
        push()
    }

    /// Ayar kapatılınca: bu cihaz artık göndermez ve almaz (iCloud'daki kopya durur); açılınca yeniden ilk eşitleme
    static func setEnabled(_ on: Bool) {
        UserDefaults.standard.set(on, forKey: enabledKey)
        if on {
            UserDefaults.standard.removeObject(forKey: initKey)
            start()
        } else {
            pushTask?.cancel()
        }
    }

    private static func same(_ a: Any?, _ b: Any?) -> Bool {
        switch (a, b) {
        case (nil, nil): true
        case let (x?, y?): (x as AnyObject).isEqual(y)
        default: false
        }
    }

    private static func pull(_ changed: [String]) {
        guard enabled else { return }
        applying = true
        defer { applying = false }
        let d = UserDefaults.standard
        for k in changed where keys.contains(k) {
            let cloud = store.object(forKey: k)
            if same(d.object(forKey: k), cloud) { continue }
            if let cloud { d.set(cloud, forKey: k) } else if d.bool(forKey: initKey) { d.removeObject(forKey: k) }
        }
    }

    private static func schedulePush() {
        guard enabled, !applying else { return }
        pushTask?.cancel()
        pushTask = Task {
            try? await Task.sleep(for: .seconds(3))
            guard !Task.isCancelled else { return }
            push()
        }
    }

    private static func push() {
        guard enabled else { return }
        let d = UserDefaults.standard
        var dirty = false
        for k in keys {
            let local = d.object(forKey: k), cloud = store.object(forKey: k)
            if same(local, cloud) { continue }
            if let local { store.set(local, forKey: k) } else { store.removeObject(forKey: k) }
            dirty = true
        }
        if dirty { store.synchronize() }
    }
}
