import SwiftUI

/// Alt bar yerine özellik aç/kapa (30 Ekim 2026, sahibin kararı): ilk açılışta yalnız Saat; Ayarlar › Özellikler'den
/// Hava, Takvim, Dünya, Mevsimler, Vakitler ve Araçlar tek tek açılır. Alt bar yalnız açık özellikleri gösterir,
/// yalnız Saat açıksa hiç görünmez (Ayarlar saat ekranındaki dişliden açılır). Kapalı bir özellik saat ekranındaki
/// bir alandan ya da rozet bildiriminden açılırsa o an geçici olarak barda görünür.
/// Kayıt `features` (virgülle, Android ile aynı). Kayıt yoksa: önceki sürümü kullananlarda hepsi açık (göç), yeni kurulumda boş.
enum Features {
    static let key = "features"
    static let optional: [AppTab] = [.weather, .calendar, .world, .seasons, .prayer, .tools]

    static func parse(_ raw: String) -> Set<AppTab> {
        Set(raw.split(separator: ",").compactMap { k in optional.first { $0.key == k } })
    }

    static func raw(_ tabs: Set<AppTab>) -> String {
        optional.filter(tabs.contains).map(\.key).joined(separator: ",")
    }

    /// Alt bardaki sekmeler: Saat, açık özellikler ve kapalı olsa da şu an açık olan sekme (tek sekme kalırsa bar yok)
    static func barTabs(raw: String, current: AppTab) -> [AppTab] {
        let on = parse(raw)
        return AppTab.allCases.filter { $0 == .clock || on.contains($0) || $0 == current }
    }

    static func isOn(_ tab: AppTab) -> Bool {
        tab == .clock || parse(UserDefaults.standard.string(forKey: key) ?? "").contains(tab)
    }

    /// Uygulama açılırken ilk iş (başka bir şey yazılmadan): uygulamanın kendi kayıtlarında anahtar varsa önceki
    /// sürüm kullanılmıştır. Sistemin uygulamaya özgü yazdığı anahtarlar (dil seçimi vb.) sayılmaz.
    static func migrate() {
        let defaults = UserDefaults.standard
        guard defaults.object(forKey: key) == nil else { return }
        let own = defaults.persistentDomain(forName: Bundle.main.bundleIdentifier ?? "") ?? [:]
        let system = ["Apple", "NS", "com.apple", "UI", "WebKit", "AK", "PK"]
        let existing = own.keys.contains { k in !system.contains { k.hasPrefix($0) } }
        defaults.set(existing ? raw(Set(optional)) : "", forKey: key)
    }
}

/// Ayarlar › Özellikler: Saat her zaman açık, diğerleri anahtarla
struct FeaturesView: View {
    @AppStorage(Features.key) private var raw = ""

    var body: some View {
        Form {
            Section {
                row(.clock, isOn: .constant(true))
                    .disabled(true)
                ForEach(Features.optional, id: \.self) { tab in
                    row(tab, isOn: Binding(
                        get: { Features.parse(raw).contains(tab) },
                        set: { on in
                            var set = Features.parse(raw)
                            if on { set.insert(tab) } else { set.remove(tab) }
                            raw = Features.raw(set)
                        }
                    ))
                }
            } footer: {
                Text(L("features.note"))
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("features.title"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func row(_ tab: AppTab, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            Label {
                VStack(alignment: .leading, spacing: 2) {
                    Text(tab.title)
                    Text(L("features.\(tab.key).note")).font(.caption).foregroundStyle(Theme.muted)
                }
            } icon: {
                Image(systemName: tab.icon).foregroundStyle(Theme.accent)
            }
        }
    }
}

/// Saat ekranındaki dişliden açılan sayfa (alt bar ve Araçlar'dan bağımsız)
enum SettingsSheet: String, Identifiable {
    case settings, features
    var id: String { rawValue }

    @MainActor @ViewBuilder var view: some View {
        switch self {
        case .settings: SettingsView()
        case .features: FeaturesView()
        }
    }
}
