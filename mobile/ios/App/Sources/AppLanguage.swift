import SwiftUI

/*
 * Uygulama içinde dil seçimi (3 Kasım 2026, 2. analiz #5; sahibin ekran görüntüsü): bayrak ve dilin kendi adıyla 15 dil
 * ve "Sistem dili". iOS'ta uygulamanın dili `AppleLanguages` kaydıyla değişir (iPhone Ayarlar › Saat Tarih › Dil ile
 * aynı kayıt) ve uygulama yeniden açılınca geçerli olur; widget'lar telefonun dilinde kalır. Android'deki
 * AppLanguage.kt ile aynı sıra ve adlar.
 */
enum AppLanguage {
    static let key = "app.lang"

    struct Option: Identifiable { let code: String; let flag: String; let native: String; var id: String { code } }

    static let all: [Option] = [
        .init(code: "tr", flag: "🇹🇷", native: "Türkçe"), .init(code: "en", flag: "🇬🇧", native: "English"),
        .init(code: "de", flag: "🇩🇪", native: "Deutsch"), .init(code: "az", flag: "🇦🇿", native: "Azərbaycanca"),
        .init(code: "ar", flag: "🇸🇦", native: "العربية"), .init(code: "id", flag: "🇮🇩", native: "Bahasa Indonesia"),
        .init(code: "ur", flag: "🇵🇰", native: "اردو"), .init(code: "fa", flag: "🇮🇷", native: "فارسی"),
        .init(code: "fr", flag: "🇫🇷", native: "Français"), .init(code: "ru", flag: "🇷🇺", native: "Русский"),
        .init(code: "ja", flag: "🇯🇵", native: "日本語"), .init(code: "ko", flag: "🇰🇷", native: "한국어"),
        .init(code: "zh", flag: "🇨🇳", native: "简体中文"), .init(code: "fil", flag: "🇵🇭", native: "Filipino"),
        .init(code: "th", flag: "🇹🇭", native: "ไทย"),
        .init(code: "es", flag: "🇪🇸", native: "Español"), .init(code: "pt", flag: "🇧🇷", native: "Português"),
        .init(code: "hi", flag: "🇮🇳", native: "हिन्दी"),
    ]

    static func set(_ code: String) {
        let d = UserDefaults.standard
        if code.isEmpty {
            d.removeObject(forKey: "AppleLanguages")
            d.removeObject(forKey: key)
        } else {
            d.set([code == "zh" ? "zh-Hans" : code], forKey: "AppleLanguages")
            d.set(code, forKey: key)
        }
    }
}

/// Ayarlar › Dil
struct LanguageView: View {
    @AppStorage(AppLanguage.key) private var selected = ""
    @State private var changed = false

    var body: some View {
        List {
            if changed {
                Section {
                    Label(L("lang.restart"), systemImage: "arrow.clockwise.circle.fill").foregroundStyle(Theme.accent)
                }
            }
            Section {
                row("🌐", L("lang.system"), on: selected.isEmpty) { pick("") }
                ForEach(AppLanguage.all) { o in
                    row(o.flag, o.native, on: selected == o.code) { pick(o.code) }
                }
            } footer: {
                Text(L("lang.noteIOS"))
            }
        }
        .themedBackground()
        .navigationTitle(L("lang.title"))
        .navigationBarTitleDisplayMode(.large)
    }

    private func pick(_ code: String) {
        guard code != selected else { return }
        AppLanguage.set(code)
        changed = true
    }

    private func row(_ flag: String, _ name: String, on: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Text(flag).font(.title2)
                Text(name).foregroundStyle(on ? Theme.accent : Theme.text).fontWeight(on ? .semibold : .regular)
                Spacer()
                if on { Image(systemName: "checkmark").foregroundStyle(Theme.accent) }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

/// Ayarlardaki satır: seçili dil
struct LanguageRow: View {
    @AppStorage(AppLanguage.key) private var selected = ""

    var body: some View {
        NavigationLink { LanguageView() } label: {
            LabeledContent {
                Text(AppLanguage.all.first { $0.code == selected }.map { "\($0.flag)  \($0.native)" } ?? L("lang.system"))
            } label: {
                Label(L("lang.title"), systemImage: "globe")
            }
        }
    }
}
