import ActivityKit
import AppIntents
import Foundation

/// Kilit ekranında ve Dynamic Island'da hava durumu (4 Kasım 2026, sahibin isteği: canlı saatin yerine; saat zaten
/// telefonun kendisinde var). Uygulama ve widget hedefinde ortak: açık/kapalı bilgisi ve son hava özeti App Group'ta
/// durur ki etkinlikteki kapat düğmesi ve Denetim Merkezi düğmesi (widget uzantısında çizilir) de okuyabilsin.
/// Veri uygulamada WeatherKit'ten alınır (LiveActivities.refreshWeather); etkinlik uygulama açıldıkça ve arka plan
/// yenilemesinde güncellenir. iOS etkinliği en çok 8 saat açık tutar, uygulama açılınca yeniden başlar.
enum LiveWeather {
    static let key = "live.weather"
    private static let snapKey = "live.weather.snap"
    private static var group: UserDefaults? { UserDefaults(suiteName: "group.tr.bumba.time") }

    /// Varsayılan kapalı
    static var isOn: Bool { group?.bool(forKey: key) ?? UserDefaults.standard.bool(forKey: key) }

    static func setOn(_ on: Bool) {
        group?.set(on, forKey: key)
        UserDefaults.standard.set(on, forKey: key)
    }

    /// Son hava özeti (yazılar uygulamanın dilinde hazırlanır)
    struct Snapshot: Codable, Equatable {
        var value: String
        var symbol: String
        var title: String
        var detail: String
        var at: Date
    }

    static func snapshot() -> Snapshot? {
        guard let data = group?.data(forKey: snapKey) else { return nil }
        return try? JSONDecoder().decode(Snapshot.self, from: data)
    }

    static func save(_ s: Snapshot) {
        if let data = try? JSONEncoder().encode(s) { group?.set(data, forKey: snapKey) }
    }

    static let attributes = SaatActivityAttributes(kind: "weather", id: "weather")

    static func state(_ s: Snapshot) -> SaatActivityAttributes.ContentState {
        .init(date: s.at, start: s.at, title: s.title, subtitle: s.detail, value: s.value, symbol: s.symbol)
    }

    /// İki saatten eski veri soluk gösterilir
    static func staleDate(_ s: Snapshot) -> Date { s.at.addingTimeInterval(2 * 3600) }

    /// Denetim Merkezi düğmesinin türü (uygulamada değişince düğme yenilenir)
    static let controlKind = "tr.bumba.time.control.liveWeather"

    /// Güncel havayı alıp etkinliği kuran işlev: uygulama verir (SaatTarihApp init; WeatherKit yalnız uygulamada).
    /// Denetim Merkezi düğmesi uygulamayı arka planda başlattığında da kurulur.
    static var refresher: (() async -> Void)?

    /// Aç/kapa: kapatınca etkinlik hemen kalkar; açınca güncel hava alınır, alınamazsa son özetle başlar
    static func apply(_ on: Bool) async {
        setOn(on)
        // Etkinliği yalnız uygulama başlatıp kapatabilir (düğmelerin niyetleri LiveActivityIntent olduğu için
        // uygulamanın sürecinde çalışır); widget uzantısında derlenen kopyada bu kısım yok
        #if !WIDGET_EXTENSION
        let current = Activity<SaatActivityAttributes>.activities.filter { $0.attributes.kind == "weather" }
        if on {
            if let refresher {
                await refresher()
                return
            }
            guard current.isEmpty, let s = snapshot(), ActivityAuthorizationInfo().areActivitiesEnabled else { return }
            _ = try? Activity.request(attributes: attributes,
                                      content: ActivityContent(state: state(s), staleDate: staleDate(s)), pushType: nil)
        } else {
            for a in current { await a.end(nil, dismissalPolicy: .immediate) }
        }
        #endif
    }
}

/// Etkinliğin üstündeki × düğmesi. LiveActivityIntent olduğu için iki hedefte de derlenir ve uygulamanın sürecinde
/// çalışır (etkinliği yalnız uygulama kapatabilir).
struct LiveWeatherOffIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "live.weather.hide"
    static let isDiscoverable = false

    init() {}

    func perform() async throws -> some IntentResult {
        await LiveWeather.apply(false)
        return .result()
    }
}

/// Denetim Merkezi'ndeki aç/kapa düğmesi (iOS 18)
@available(iOS 18.0, *)
struct LiveWeatherToggleIntent: SetValueIntent, LiveActivityIntent {
    static let title: LocalizedStringResource = "live.weather.control"
    static let isDiscoverable = false

    @Parameter(title: "live.weather.control")
    var value: Bool

    init() {}

    func perform() async throws -> some IntentResult {
        await LiveWeather.apply(value)
        return .result()
    }
}
