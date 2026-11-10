import SwiftUI
import UIKit

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

/// Ayarlar › Hesap (üyelik aşama 1): girişsizken giriş / kayıt / şifremi unuttum; girişliyken görünen ad, dil ve tema
/// (uygulamanın ayarları), e-posta listesi, e-posta ve şifre değiştirme, verilerimi indir, çıkış, hesabı silme.
/// Android'deki AccountScreen ile aynı akış ve metinler (`account.*`).
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
                Link(L("account.noticeLink"), destination: AccountLinks.notice)
                Link(L("account.deletePage"), destination: AccountLinks.deletion)
            }
            .tint(Theme.accent)
        }
        .navigationTitle(L("account.title"))
        .themedBackground()
        .task { if Account.shared.signedIn { await Account.shared.refresh() } }
    }
}

/// Başarı ya da hata iletisi (metin anahtarıyla)
private struct AccountMessage: View {
    let key: String?
    var ok = false
    var body: some View {
        if let key {
            Label(L(key), systemImage: ok ? "checkmark.circle" : "exclamationmark.triangle")
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

private struct AccountSignedOut: View {
    let notice: String?
    @State private var mode = "in" // in, up, forgot
    @State private var email = ""
    @State private var password = ""
    @State private var name = ""
    @State private var busy = false
    @State private var err: String?
    @State private var info: String?
    @State private var canResend = false

    init(notice: String?) {
        self.notice = notice
        _info = State(initialValue: notice)
    }

    private func run(_ block: @escaping () async -> Account.Result, onOk: @escaping () -> Void = {}) {
        guard !busy else { return }
        busy = true; err = nil; info = nil
        Task {
            let r = await block()
            busy = false
            if r.ok { onOk() } else { err = r.err; canResend = r.err == "account.err.unverified" }
        }
    }

    var body: some View {
        Section {
            if mode != "forgot" {
                Picker("", selection: $mode) {
                    Text(L("account.signIn")).tag("in")
                    Text(L("account.signUp")).tag("up")
                }
                .pickerStyle(.segmented)
                .onChange(of: mode) { _, _ in err = nil }
            }
        } footer: {
            Text(L("account.intro"))
        }
        Section {
            switch mode {
            case "forgot":
                Text(L("account.forgotLead")).font(.subheadline)
                TextField(L("account.email"), text: $email).textContentType(.username).emailField()
                Button { run({ await Account.shared.forgot(email) }) { info = "account.forgotSent" } } label: {
                    BusyLabel(L("account.forgotSend"), busy: busy)
                }
                Button(L("account.backToSignIn")) { mode = "in"; err = nil; info = nil }
            case "up":
                TextField(L("account.nameOptional"), text: $name).textContentType(.name)
                    .onChange(of: name) { _, v in if v.count > 60 { name = String(v.prefix(60)) } }
                TextField(L("account.email"), text: $email).textContentType(.username).emailField()
                SecureField(L("account.password"), text: $password).textContentType(.newPassword)
                Button {
                    run({ await Account.shared.signUp(email: email, password: password, name: name) }) {
                        info = "account.signedUp"; canResend = true; password = ""; mode = "in"
                    }
                } label: { BusyLabel(L("account.signUpButton"), busy: busy) }
            default:
                TextField(L("account.email"), text: $email).textContentType(.username).emailField()
                SecureField(L("account.password"), text: $password).textContentType(.password)
                Button {
                    run({ await Account.shared.signIn(email: email, password: password) }) { password = "" }
                } label: { BusyLabel(L("account.signInButton"), busy: busy) }
                Button(L("account.forgot")) { mode = "forgot"; err = nil; info = nil }
            }
            AccountMessage(key: err)
            AccountMessage(key: info, ok: true)
            if canResend && mode == "in" {
                Button(L("account.resend")) { run({ await Account.shared.resendVerification(email) }) { info = "account.resent" } }
            }
        } header: {
            Text(L(mode == "forgot" ? "account.forgot" : mode == "up" ? "account.signUp" : "account.signIn"))
        } footer: {
            if mode == "up" {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("account.passwordHint"))
                    Link(L("account.noticeLead") + " " + L("account.noticeLink"), destination: AccountLinks.notice)
                }
            }
        }
        .tint(Theme.accent)
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

private struct AccountSignedIn: View {
    let onSignedOut: (String) -> Void
    @State private var name = Account.shared.user?.name ?? ""
    @State private var newEmail = ""
    @State private var current = ""
    @State private var next = ""
    @State private var deletePassword = ""
    @State private var confirmDelete = false
    @State private var busy = false
    /// Her bölümün kendi iletisi: (bölüm, metin anahtarı, başarılı mı)
    @State private var msg: (String, String, Bool)?
    @State private var export: URL?

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

    var body: some View {
        if let u = Account.shared.user {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text(u.name.isEmpty ? u.email : u.name).font(.headline)
                    if !u.name.isEmpty { Text(u.email).font(.subheadline).foregroundStyle(Theme.muted) }
                }
            } footer: { Text(L("account.signedInNote")) }

            Section(L("account.nameLabel")) {
                TextField(L("account.nameLabel"), text: $name).textContentType(.name)
                    .onChange(of: name) { _, v in if v.count > 60 { name = String(v.prefix(60)) } }
                Button(L("account.save")) { run("name", { await Account.shared.updateName(name) }, ok: "account.saved") }
                message("name")
            }

            Section {
                LanguageRow()
                NavigationLink { AccentPickerView() } label: { BoxLabel(L("settings.color"), duo: "palette", tint: Theme.dusk) }
                NavigationLink { AboutView() } label: { BoxLabel(L("list.heading"), duo: "mail", tint: Theme.sky) }
            } footer: { Text(L("account.prefs")) }

            Section(L("account.changeEmail")) {
                TextField(L("account.newEmail"), text: $newEmail).textContentType(.emailAddress).emailField()
                Button(L("account.changeEmail")) {
                    run("email", { await Account.shared.changeEmail(newEmail) }, ok: "account.changeEmailSent") { _ in newEmail = "" }
                }
                message("email")
            }

            Section {
                SecureField(L("account.passwordCurrent"), text: $current).textContentType(.password)
                SecureField(L("account.passwordNew"), text: $next).textContentType(.newPassword)
                Button(L("account.changePassword")) {
                    run("password", { await Account.shared.changePassword(current: current, new: next) }, ok: "account.passwordChanged") { _ in
                        current = ""; next = ""
                    }
                }
                message("password")
            } header: { Text(L("account.changePassword")) } footer: { Text(L("account.passwordHint")) }

            Section {
                // Döküm paylaşım penceresiyle kaydedilir (Dosyalar'a, e-postaya…)
                Button(L("account.download")) {
                    run("data", { await Account.shared.download() }, ok: nil) { r in
                        let url = FileManager.default.temporaryDirectory.appendingPathComponent("saat-tarih-hesap.json")
                        if (try? (r.data ?? Data("{}".utf8)).write(to: url, options: .atomic)) != nil { export = url }
                    }
                }
                message("data")
            } footer: { Text(L("account.downloadNote")) }

            Section {
                Button(L("account.signOut")) {
                    Task { await Account.shared.signOut(); onSignedOut("account.signedOut") }
                }
            }

            Section {
                SecureField(L("account.password"), text: $deletePassword).textContentType(.password)
                Button(L("account.delete"), role: .destructive) { if !deletePassword.isEmpty { confirmDelete = true } }
                message("delete")
            } header: { Text(L("account.delete")) } footer: { Text(L("account.deleteLead")) }
            .confirmationDialog(L("account.deleteConfirm"), isPresented: $confirmDelete, titleVisibility: .visible) {
                Button(L("account.delete"), role: .destructive) {
                    run("delete", { await Account.shared.delete(password: deletePassword) }, ok: nil) { _ in
                        deletePassword = ""; onSignedOut("account.deleted")
                    }
                }
                Button(L("common.cancel"), role: .cancel) {}
            }
            .sheet(item: Binding(get: { export.map(ExportFile.init) }, set: { if $0 == nil { export = nil } })) { f in
                AccountShareSheet(items: [f.url])
            }
        }
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
