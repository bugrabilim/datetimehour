import SwiftUI

/// Alt bar yerine özellik aç/kapa (30 Ekim 2026, sahibin kararı): ilk açılışta yalnız Saat; Ayarlar › Özellikler'den
/// Hava, Takvim, Dünya, Mevsimler, Vakitler ve Araçlar tek tek açılır. 3 Kasım 2026'dan beri (Seçenek B) alt çubukta
/// Saat, kullanıcının seçtiği en çok 3 özellik ve "Tümü" durur; yalnız Saat açıksa çubuk yok (Ayarlar dişliden açılır).
/// Kayıt `features` (virgülle, Android ile aynı). Kayıt yoksa: önceki sürümü kullananlarda hepsi açık (göç), yeni kurulumda boş.
enum Features {
    static let key = "features"
    /// Alt çubuktaki özellikler (en çok 3, sırayla; Seçenek B, 2. analiz #194; Android ile aynı)
    static let barKey = "features.bar"
    static let barMax = 3
    static let optional: [AppTab] = [.weather, .calendar, .world, .seasons, .prayer, .tools]
    /// Alt çubuğa girebilenler (Araçlar her zaman "Tümü"nde)
    static let barOptions: [AppTab] = [.weather, .calendar, .world, .seasons, .prayer]
    /// Birden çok özellik aynı anda açılınca (ilk kurulum, göç) çubuğa giriş sırası
    static let barDefault: [AppTab] = [.prayer, .weather, .calendar, .world, .seasons]

    static func parse(_ raw: String) -> Set<AppTab> {
        Set(raw.split(separator: ",").compactMap { k in optional.first { $0.key == k } })
    }

    static func raw(_ tabs: Set<AppTab>) -> String {
        optional.filter(tabs.contains).map(\.key).joined(separator: ",")
    }

    static func parseBar(_ raw: String) -> [AppTab] {
        var out: [AppTab] = []
        for k in raw.split(separator: ",") {
            if let t = barOptions.first(where: { $0.key == k }), !out.contains(t) { out.append(t) }
        }
        return Array(out.prefix(barMax))
    }

    /// Alt çubuk (Seçenek B): Saat her zaman ilk, "Tümü" (.tools) her zaman son, aradakiler kullanıcının seçtiği en çok 3
    /// özellik. Hiçbir özellik açık değilse çubuk yok. Çubukta olmayan bir özellik bildirimden, widget'tan ya da Tümü'nden
    /// açılırsa çubuk değişmez; sayfa üstte geri düğmesiyle açılır (2. analiz #203)
    static func barTabs(raw: String, bar: String) -> [AppTab] {
        let on = parse(raw)
        if on.isEmpty { return [.clock] }
        return [.clock] + parseBar(bar).filter(on.contains) + [.tools]
    }

    static func isOn(_ tab: AppTab) -> Bool {
        tab == .clock || parse(UserDefaults.standard.string(forKey: key) ?? "").contains(tab)
    }

    /// Açık özellikleri yazar; yeni açılan özellik çubukta yer varsa kendiliğinden girer, kapanan çıkar
    static func save(_ set: Set<AppTab>) {
        let d = UserDefaults.standard
        let old = parse(d.string(forKey: key) ?? "")
        d.set(raw(set), forKey: key)
        var bar = parseBar(d.string(forKey: barKey) ?? "").filter(set.contains)
        for t in barDefault where set.contains(t) && !old.contains(t) && !bar.contains(t) && bar.count < barMax { bar.append(t) }
        d.set(bar.map(\.key).joined(separator: ","), forKey: barKey)
    }

    static func set(_ tab: AppTab, on: Bool) {
        var s = parse(UserDefaults.standard.string(forKey: key) ?? "")
        if on { s.insert(tab) } else { s.remove(tab) }
        save(s)
    }

    static func saveBar(_ list: [AppTab]) {
        UserDefaults.standard.set(list.filter(barOptions.contains).prefix(barMax).map(\.key).joined(separator: ","), forKey: barKey)
    }

    /// "Alt çubukta" işareti; çubuk doluysa false
    @discardableResult static func toggleBar(_ tab: AppTab) -> Bool {
        var bar = parseBar(UserDefaults.standard.string(forKey: barKey) ?? "")
        if let i = bar.firstIndex(of: tab) { bar.remove(at: i); saveBar(bar); return true }
        guard bar.count < barMax, barOptions.contains(tab), isOn(tab) else { return false }
        bar.append(tab)
        saveBar(bar)
        return true
    }

    /// Uygulama açılırken ilk iş (başka bir şey yazılmadan): uygulamanın kendi kayıtlarında anahtar varsa önceki
    /// sürüm kullanılmıştır. Sistemin uygulamaya özgü yazdığı anahtarlar (dil seçimi vb.) sayılmaz.
    static func migrate() {
        let defaults = UserDefaults.standard
        if defaults.object(forKey: key) == nil {
            let own = defaults.persistentDomain(forName: Bundle.main.bundleIdentifier ?? "") ?? [:]
            let system = ["Apple", "NS", "com.apple", "UI", "WebKit", "AK", "PK"]
            let existing = own.keys.contains { k in !system.contains { k.hasPrefix($0) } }
            defaults.set(existing ? raw(Set(optional)) : "", forKey: key)
            // Yeni kurulum: ilk açılışta kurulum sihirbazı (Onboarding; 2. analiz #4)
            if !existing { defaults.set(true, forKey: Onboarding.key) }
        }
        // Seçenek B göçü: çubuk kaydı yoksa açık özelliklerden varsayılan sırayla en çok 3
        if defaults.object(forKey: barKey) == nil {
            let on = parse(defaults.string(forKey: key) ?? "")
            defaults.set(barDefault.filter(on.contains).prefix(barMax).map(\.key).joined(separator: ","), forKey: barKey)
        }
    }
}

/// Ayarlar › Özellikler: Saat her zaman açık, diğerleri anahtarla; açık özelliklerde "Alt çubukta" işareti ve sıra (#194)
struct FeaturesView: View {
    @AppStorage(Features.key) private var raw = ""
    @AppStorage(Features.barKey) private var bar = ""
    @State private var full = false

    var body: some View {
        Form {
            Section {
                BarPreview()
                let list = Features.parseBar(bar)
                ForEach(list, id: \.self) { tab in
                    barRow(tab, list)
                }
            } header: {
                Text(L("features.barTitle"))
            } footer: {
                Text(L("features.barNote"))
            }
            Section {
                row(.clock, isOn: .constant(true))
                    .disabled(true)
                ForEach(Features.optional, id: \.self) { tab in
                    VStack(alignment: .leading, spacing: 6) {
                        row(tab, isOn: Binding(
                            get: { Features.parse(raw).contains(tab) },
                            set: { on in Features.set(tab, on: on) }
                        ))
                        if Features.barOptions.contains(tab) && Features.parse(raw).contains(tab) {
                            let inBar = Features.parseBar(bar).contains(tab)
                            Button {
                                if !Features.toggleBar(tab) { full = true }
                            } label: {
                                Label(L("features.bar"), systemImage: inBar ? "checkmark.circle.fill" : "circle")
                                    .font(.footnote)
                            }
                            .buttonStyle(.bordered)
                            .tint(inBar ? Theme.accent : Theme.muted)
                            .padding(.leading, 40)
                        }
                    }
                }
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    if full { Text(L("features.barFull")).foregroundStyle(Theme.err) }
                    Text(L("features.note"))
                }
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("features.title"))
        .navigationBarTitleDisplayMode(.large)
        .task(id: full) {
            guard full else { return }
            try? await Task.sleep(for: .seconds(3))
            full = false
        }
    }

    /// Çubuktaki sıra: yukarı/aşağı düğmeleri (Android ile aynı)
    private func barRow(_ tab: AppTab, _ list: [AppTab]) -> some View {
        let i = list.firstIndex(of: tab) ?? 0
        return HStack {
            Label(tab.title, systemImage: tab.icon)
            Spacer()
            Button { move(list, i, -1) } label: { Image(systemName: "chevron.up") }
                .disabled(i == 0)
                .accessibilityLabel(L("features.moveUp"))
            Button { move(list, i, 1) } label: { Image(systemName: "chevron.down") }
                .disabled(i == list.count - 1)
                .accessibilityLabel(L("features.moveDown"))
        }
        .buttonStyle(.borderless)
    }

    private func move(_ list: [AppTab], _ i: Int, _ d: Int) {
        let j = i + d
        guard list.indices.contains(j) else { return }
        var m = list
        m.swapAt(i, j)
        Features.saveBar(m)
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

/// Canlı alt çubuk önizlemesi (Özellikler ve ilk kurulum)
struct BarPreview: View {
    @AppStorage(Features.key) private var raw = ""
    @AppStorage(Features.barKey) private var bar = ""

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Features.barTabs(raw: raw, bar: bar), id: \.self) { tab in
                VStack(spacing: 3) {
                    Image(systemName: tab.icon).font(.system(size: 18))
                    Text(tab.barTitle).font(.system(size: 11)).lineLimit(1).minimumScaleFactor(0.7)
                }
                .foregroundStyle(tab == .clock ? Theme.accent : Theme.muted)
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
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
