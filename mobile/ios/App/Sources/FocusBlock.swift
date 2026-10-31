import FamilyControls
import ManagedSettings
import SwiftUI

/// Odakta uygulama ve site engelleme (rakip analizi listesi #98): pomodoro çalışma süresi işlerken seçilen uygulamalar,
/// uygulama kategorileri ve siteler Ekran Süresi kalkanıyla kapatılır (FamilyControls + ManagedSettings; seçim kişinin
/// kendi cihazı için, `.individual`). Seçim Apple'ın şifreli belirteçleridir: uygulama hangi uygulamaların seçildiğini
/// göremez, hiçbir yere gönderilmez.
///
/// **Apple onayı:** FamilyControls yetkisi (com.apple.developer.family-controls) dağıtım için Apple'ın onayını ister.
/// Onay gelene kadar yetki SaatTarih.entitlements'ta yoktur ve bölüm gizlidir (Info.plist `SaatFamilyControls` = false).
/// Onaydan sonra: yetkiyi ekle, bayrağı true yap.
///
/// Kalkan, süre bittiğinde uygulama açıksa hemen, kapalıysa uygulama bir sonraki açılışta kaldırılır (pomodoro bitiş
/// bildirimi uygulamayı açar); kalkan ekranında da "Saat Tarih'i açın" yazar.
@MainActor
enum FocusBlock {
    static let onKey = "focus.block"
    static let selectionKey = "focus.selection"
    private static let store = ManagedSettingsStore(named: ManagedSettingsStore.Name("focus"))

    /// Bu yapıda özellik açık mı (Apple onayı gelince Info.plist'te true)
    static var available: Bool { Bundle.main.object(forInfoDictionaryKey: "SaatFamilyControls") as? Bool == true }

    static var selection: FamilyActivitySelection {
        get {
            guard let data = UserDefaults.standard.data(forKey: selectionKey),
                  let s = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) else { return FamilyActivitySelection() }
            return s
        }
        set { UserDefaults.standard.set(try? JSONEncoder().encode(newValue), forKey: selectionKey) }
    }

    static var isEmpty: Bool {
        let s = selection
        return s.applicationTokens.isEmpty && s.categoryTokens.isEmpty && s.webDomainTokens.isEmpty
    }

    /// Pomodoro çalışma süresi işliyor mu (pomo.end: Date referans saniyesi)
    static var working: Bool {
        let d = UserDefaults.standard
        let end = d.double(forKey: "pomo.end")
        return end > 0 && Date(timeIntervalSinceReferenceDate: end) > Date() && (d.string(forKey: "pomo.phase") ?? "work") == "work"
    }

    /// Kalkanı pomodoro durumuna göre kurar ya da kaldırır (uygulama açılışında ve pomodoro değişince)
    static func sync() {
        guard available else { return }
        let d = UserDefaults.standard
        guard d.bool(forKey: onKey), working, AuthorizationCenter.shared.authorizationStatus == .approved, !isEmpty else {
            store.clearAllSettings()
            return
        }
        let s = selection
        store.shield.applications = s.applicationTokens.isEmpty ? nil : s.applicationTokens
        store.shield.applicationCategories = s.categoryTokens.isEmpty ? nil : .specific(s.categoryTokens)
        store.shield.webDomains = s.webDomainTokens.isEmpty ? nil : s.webDomainTokens
        store.shield.webDomainCategories = s.categoryTokens.isEmpty ? nil : .specific(s.categoryTokens)
    }

    static func requestAuthorization() async -> Bool {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
        } catch {
            return false
        }
        return AuthorizationCenter.shared.authorizationStatus == .approved
    }
}

/// Pomodoro sayfasındaki kart: aç/kapat, uygulama/site seçimi (sistemin seçicisi), not
struct FocusBlockCard: View {
    @AppStorage(FocusBlock.onKey) private var on = false
    @State private var selection = FocusBlock.selection
    @State private var picking = false
    @State private var denied = false

    var body: some View {
        if FocusBlock.available {
            VStack(alignment: .leading, spacing: 8) {
                SectionTitle(text: L("focus.title"))
                Toggle(L("focus.toggle"), isOn: Binding(get: { on }, set: { v in
                    if v {
                        Task {
                            let ok = await FocusBlock.requestAuthorization()
                            denied = !ok
                            on = ok
                            if ok && FocusBlock.isEmpty { picking = true }
                            FocusBlock.sync()
                        }
                    } else {
                        on = false
                        FocusBlock.sync()
                    }
                }))
                .tint(Theme.accent)
                if on {
                    Button(T("focus.chosenIOS", selection.applicationTokens.count, selection.categoryTokens.count, selection.webDomainTokens.count)) {
                        picking = true
                    }
                }
                if denied { Text(L("focus.denied")).font(.caption).foregroundStyle(.red) }
                Text(L("focus.noteIOS")).font(.caption).foregroundStyle(Theme.muted)
            }
            .card()
            .familyActivityPicker(isPresented: $picking, selection: $selection)
            // Seçici kapanınca seçim kaydedilir
            .onChange(of: picking) {
                guard !picking else { return }
                FocusBlock.selection = selection
                FocusBlock.sync()
            }
        }
    }
}
