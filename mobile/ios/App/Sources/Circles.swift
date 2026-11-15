import SwiftUI

/*
 * Halkalar (üyelik aşama 2b, 15 Kasım 2026; sahibin onayı "tüm önerilerin uygun"): ortak zikir ve hatim halkaları,
 * üyelik sunucusundaki `/v1/halka…` uçlarıyla (server/src/circles.mjs). Giriş ister; ilk kurma ya da katılmada ayrı açık
 * rıza (`circle.consent`). Halka adı ve görünen ad dışında kullanıcı içeriği yok; Bildir, Engelle, kurucu Çıkar.
 * Sunucuda kapalıyken uçlar 404 verir: ekran "yakında" der. Android'deki Circles.kt ile aynı.
 */
enum Circles {
    static let link = "https://time.bumba.tr/davet/#"
    /// Davet bağlantısıyla gelen kod (aşama 2c): Halkalar › Kodla katıl bununla açılır
    static var pendingCode: String?

    struct Member: Identifiable {
        let id: Int64
        let name: String?
        let blocked, you, owner: Bool
        let count: Int64
    }

    struct Juz: Identifiable {
        let n: Int
        let state: String
        let member: Int64?
        let you: Bool
        var id: Int { n }
    }

    struct Circle: Identifiable {
        let id: String
        let kind: String
        let name: String?
        let owner: Bool
        let members: Int
        var zikir: String?
        var target: Int64 = 0
        var total: Int64 = 0
        var round = 1
        var khatms = 0
        var doneJuz = 0
        var code: String?
        var memberList: [Member] = []
        var juz: [Juz] = []
    }

    /// Sonuç: `err` doluysa metin anahtarı; "closed" sunucuda kapalı, "riza" onay gerekli
    struct Res<T> {
        var value: T?
        var err: String?
    }

    static func errOf(_ r: Account.Resp) -> String {
        let o = Account.json(r.body)
        if r.code == 404, (o?["hata"] as? String) == "yok" { return "closed" }
        switch o?["hata"] as? String ?? "" {
        case "kod": return "circle.err.code"
        case "ad", "ad-icerik": return "circle.err.name"
        case "halka-sinir": return "circle.err.limit"
        case "alinmis", "senin-degil": return "circle.err.taken"
        case "bitti": return "circle.err.ended"
        case "dolu": return "circle.full"
        case "riza": return "riza"
        default: return Account.errorOf(r)
        }
    }

    private static func str(_ v: Any?) -> String? { (v as? String).flatMap { $0.isEmpty ? nil : $0 } }
    private static func int(_ v: Any?) -> Int64 { (v as? NSNumber)?.int64Value ?? 0 }

    static func parse(_ o: [String: Any]) -> Circle {
        let h = o["hatim"] as? [String: Any]
        let ms = o["uyeler"] as? [[String: Any]] ?? []
        let js = o["cuzler"] as? [[String: Any]] ?? []
        var c = Circle(id: o["id"] as? String ?? "", kind: o["tur"] as? String ?? "zikir", name: str(o["ad"]),
                       owner: o["sahip"] as? Bool ?? false, members: (o["uyeSayisi"] as? Int) ?? ms.count)
        c.zikir = str(o["zikir"])
        c.target = int(o["hedef"])
        c.total = int(o["toplam"])
        c.round = (h?["tur"] as? Int) ?? 1
        c.khatms = (h?["biten"] as? Int) ?? 0
        c.code = str(o["kod"])
        c.memberList = ms.map {
            Member(id: int($0["id"]), name: str($0["ad"]), blocked: $0["engelli"] as? Bool ?? false, you: $0["sen"] as? Bool ?? false,
                   owner: $0["sahip"] as? Bool ?? false, count: int($0["sayi"]))
        }
        c.juz = js.map {
            Juz(n: Int(int($0["cuz"])), state: $0["durum"] as? String ?? "bos", member: $0["uye"] is NSNull ? nil : ($0["uye"] as? NSNumber)?.int64Value,
                you: $0["sen"] as? Bool ?? false)
        }
        c.doneJuz = (h?["bitenCuz"] as? Int) ?? c.juz.filter { $0.state == "bitti" }.count
        return c
    }

    static func list() async -> Res<([Circle], Bool)> {
        let r = await Account.shared.call("GET", "/v1/halkalar", auth: true)
        guard r.code == 200, let o = Account.json(r.body) else { return Res(err: errOf(r)) }
        return Res(value: ((o["halkalar"] as? [[String: Any]] ?? []).map(parse), o["riza"] as? Bool ?? false))
    }

    private static func detailCall(_ method: String, _ path: String, _ body: [String: Any]? = nil) async -> Res<Circle> {
        let r = await Account.shared.call(method, path, body, auth: true)
        guard r.code == 200, let o = Account.json(r.body) else { return Res(err: errOf(r)) }
        return Res(value: parse(o))
    }

    static func detail(_ id: String) async -> Res<Circle> { await detailCall("GET", "/v1/halka/\(id)") }
    static func create(kind: String, name: String, zikir: String, target: Int64, consent: Bool) async -> Res<Circle> {
        await detailCall("POST", "/v1/halka", ["tur": kind, "ad": name, "zikir": zikir, "hedef": target, "riza": consent])
    }
    static func join(_ code: String, consent: Bool) async -> Res<Circle> { await detailCall("POST", "/v1/halka/katil", ["kod": code, "riza": consent]) }
    static func juz(_ id: String, _ n: Int, _ op: String) async -> Res<Circle> { await detailCall("POST", "/v1/halka/\(id)/cuz", ["cuz": n, "islem": op]) }
    static func kick(_ id: String, _ member: Int64) async -> Res<Circle> { await detailCall("POST", "/v1/halka/\(id)/cikar", ["uye": member]) }
    static func block(_ id: String, _ member: Int64, _ on: Bool) async -> Res<Circle> {
        await detailCall("POST", "/v1/halka/\(id)/engelle", ["uye": member, "kaldir": !on])
    }
    static func edit(_ id: String, _ body: [String: Any]) async -> Res<Circle> { await detailCall("POST", "/v1/halka/\(id)/duzenle", body) }

    /// Kodla önizleme: tür, ad, üye sayısı, dolu mu
    static func preview(_ code: String) async -> Res<[String: Any]> {
        let r = await Account.shared.call("POST", "/v1/halka/onizle", ["kod": code], auth: true)
        guard r.code == 200, let o = Account.json(r.body) else { return Res(err: errOf(r)) }
        return Res(value: o)
    }

    static func addZikir(_ id: String, _ n: Int) async -> String? { await simple("/v1/halka/\(id)/zikir", ["ekle": n]) }

    static func simple(_ path: String, _ body: [String: Any] = [:]) async -> String? {
        let r = await Account.shared.call("POST", path, body, auth: true)
        return r.code == 200 ? nil : errOf(r)
    }

    static func report(_ id: String, _ member: Int64?) async -> String? {
        await simple("/v1/halka/\(id)/bildir", member.map { ["uye": $0] } ?? [:])
    }

    static func num(_ n: Int64) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.locale = AppLocale.numberLocale
        return f.string(from: NSNumber(value: n)) ?? "\(n)"
    }

    static func kindTitle(_ c: Circle) -> String { L(c.kind == "hatim" ? "circle.kindHatim" : "circle.kindZikir") }
    static func title(_ c: Circle) -> String { c.name ?? L("circle.nameHidden") }
    static func progress(_ c: Circle) -> String {
        c.kind == "hatim" ? T("circle.khatm", c.round) + " · " + T("circle.juzDone", c.doneJuz) : num(c.total) + " / " + num(c.target)
    }
}

/// Halkalar: liste, kur, kodla katıl; seçilince ayrıntı
struct CirclesView: View {
    @State private var circles: [Circles.Circle]?
    @State private var consent = false
    @State private var err: String?
    @State private var signedIn = Account.shared.signedIn
    @State private var askWithdraw = false
    @State private var opened: String?
    @State private var joining = Circles.pendingCode != nil

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(L("circle.intro")).font(.subheadline).foregroundStyle(Theme.muted)
                if !signedIn {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(L("circle.signIn")).foregroundStyle(Theme.text)
                        NavigationLink { AccountView() } label: {
                            Text(L("circle.signInButton")).font(.body.weight(.medium)).frame(maxWidth: .infinity, minHeight: 50)
                                .background(Capsule().fill(Theme.accent)).foregroundStyle(Theme.onAccent)
                        }
                    }
                    .card()
                } else if err == "closed" {
                    EmptyState(icon: "person.3", title: L("circle.closed"))
                } else if let err {
                    Text(L(err)).foregroundStyle(Theme.err)
                } else if let circles {
                    if circles.isEmpty { EmptyState(icon: "person.3", title: L("circle.empty")) }
                    ForEach(circles) { c in
                        NavigationLink { CircleDetailView(id: c.id) } label: { CircleCard(c: c) }.buttonStyle(.plain)
                    }
                    HStack(spacing: 10) {
                        NavigationLink { CircleJoinView(hasConsent: consent) } label: { pill(L("circle.join"), false) }
                        NavigationLink { CircleCreateView(hasConsent: consent) } label: { pill(L("circle.create"), true) }
                    }
                    Text(L("circle.privacyNote")).font(.footnote).foregroundStyle(Theme.muted)
                    if consent {
                        Button(L("circle.withdraw")) { askWithdraw = true }.foregroundStyle(Theme.err)
                    }
                } else {
                    ProgressView().frame(maxWidth: .infinity)
                }
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 20)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("circle.title"))
        .navigationDestination(isPresented: $joining) { CircleJoinView(hasConsent: consent) }
        .task(id: signedIn) { await reload() }
        .onAppear {
            signedIn = Account.shared.signedIn
            Task { await reload() }
        }
        .confirmationDialog(L("circle.withdrawConfirm"), isPresented: $askWithdraw, titleVisibility: .visible) {
            Button(L("circle.withdraw"), role: .destructive) {
                Task { _ = await Circles.simple("/v1/halka/riza/geri-al"); await reload() }
            }
        }
    }

    private func pill(_ title: String, _ prominent: Bool) -> some View {
        Text(title).font(.body.weight(.medium)).frame(maxWidth: .infinity, minHeight: 50)
            .background(Capsule().fill(prominent ? Theme.accent : Theme.surface2))
            .foregroundStyle(prominent ? Theme.onAccent : Theme.text)
    }

    private func reload() async {
        guard Account.shared.signedIn else { return }
        let r = await Circles.list()
        err = r.err
        if let v = r.value { circles = v.0; consent = v.1 }
    }
}

private struct CircleCard: View {
    let c: Circles.Circle
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                DuoChip(name: c.kind == "hatim" ? "book" : "beads", color: Theme.gold)
                VStack(alignment: .leading, spacing: 2) {
                    Text(Circles.title(c)).font(.headline).foregroundStyle(c.name == nil ? Theme.muted : Theme.text)
                    Text(Circles.kindTitle(c) + " · " + T("circle.members", c.members)).font(.footnote).foregroundStyle(Theme.muted)
                }
                Spacer()
                Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
            }
            Text(Circles.progress(c)).monospacedDigit().foregroundStyle(Theme.text)
            if c.kind == "zikir", c.target > 0 {
                ProgressView(value: min(1, Double(c.total) / Double(c.target))).tint(Theme.accent)
            }
        }
        .card()
    }
}

/// Açık rıza: metin + onay kutusu (sunucu ilk kurma/katılmada ister)
private struct ConsentBlock: View {
    @Binding var agree: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(L("circle.consent")).font(.subheadline).foregroundStyle(Theme.text)
            Toggle(L("circle.consentCheck"), isOn: $agree).tint(Theme.accent).font(.body.weight(.semibold))
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Theme.surface2))
    }
}

struct CircleCreateView: View {
    let hasConsent: Bool
    @State private var kind = "zikir"
    @State private var name = ""
    @State private var zikir = "subhanallah"
    @State private var target = "1000"
    @State private var agree = false
    @State private var err: String?
    @State private var busy = false
    @State private var created: String?
    private let kinds = ["subhanallah", "elhamdulillah", "allahuekber", "tevhid", "istigfar", "salavat"]

    var body: some View {
        Form {
            Section {
                Picker("", selection: $kind) {
                    Text(L("circle.kindZikir")).tag("zikir")
                    Text(L("circle.kindHatim")).tag("hatim")
                }
                .pickerStyle(.segmented)
                TextField(L("circle.name"), text: $name).onChange(of: name) { name = String(name.prefix(40)) }
                if kind == "zikir" {
                    Picker(L("zikir.title"), selection: $zikir) {
                        ForEach(kinds, id: \.self) { Text(L("zikir.kind.\($0)")).tag($0) }
                    }
                    LabeledContent(L("circle.target")) {
                        TextField("", text: $target).keyboardType(.numberPad).multilineTextAlignment(.trailing)
                            .onChange(of: target) { target = String(target.filter(\.isNumber).prefix(8)) }
                    }
                }
            }
            if !hasConsent { Section { ConsentBlock(agree: $agree) }.listRowBackground(Color.clear).listRowInsets(EdgeInsets()) }
            if let err { Text(L(err)).foregroundStyle(Theme.err) }
            Section {
                RoundButton(title: L("circle.create"), prominent: true) {
                    guard !busy, !name.trimmingCharacters(in: .whitespaces).isEmpty, hasConsent || agree else { return }
                    busy = true
                    Task {
                        let r = await Circles.create(kind: kind, name: name.trimmingCharacters(in: .whitespaces), zikir: zikir,
                                                     target: Int64(target) ?? 0, consent: hasConsent || agree)
                        busy = false
                        err = r.err == "riza" ? nil : r.err
                        created = r.value?.id
                    }
                }
            }
            .listRowBackground(Color.clear).listRowInsets(EdgeInsets())
        }
        .navigationTitle(L("circle.create"))
        .themedBackground()
        .navigationDestination(item: $created) { CircleDetailView(id: $0) }
    }
}

struct CircleJoinView: View {
    let hasConsent: Bool
    @State private var code = Circles.pendingCode ?? ""
    @State private var preview: [String: Any]?
    @State private var agree = false
    @State private var err: String?
    @State private var joined: String?

    private var full: Bool { preview?["dolu"] as? Bool ?? false }
    private var ready: Bool { preview != nil && !full && (hasConsent || agree) }

    var body: some View {
        Form {
            Section {
                TextField(L("circle.codeField"), text: $code)
                    .textInputAutocapitalization(.characters).autocorrectionDisabled().monospacedDigit()
                    .onChange(of: code) {
                        let c = String(code.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(8))
                        if c != code { code = c }
                    }
            }
            if let p = preview {
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text((p["ad"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? L("circle.nameHidden")).font(.headline)
                        Text(L((p["tur"] as? String) == "hatim" ? "circle.kindHatim" : "circle.kindZikir") + " · "
                             + T("circle.members", (p["uyeSayisi"] as? Int) ?? 0)).font(.subheadline).foregroundStyle(Theme.muted)
                        if full { Text(L("circle.full")).foregroundStyle(Theme.err) }
                    }
                }
                if !hasConsent { Section { ConsentBlock(agree: $agree) }.listRowBackground(Color.clear).listRowInsets(EdgeInsets()) }
            }
            if let err { Text(L(err)).foregroundStyle(Theme.err) }
            Section {
                RoundButton(title: L("circle.joinButton"), prominent: ready) {
                    guard ready else { return }
                    Task {
                        let r = await Circles.join(code, consent: hasConsent || agree)
                        err = r.err == "riza" ? nil : r.err
                        joined = r.value?.id
                    }
                }
            }
            .listRowBackground(Color.clear).listRowInsets(EdgeInsets())
        }
        .navigationTitle(L("circle.join"))
        .themedBackground()
        .navigationDestination(item: $joined) { CircleDetailView(id: $0) }
        .task(id: code) {
            Circles.pendingCode = nil
            preview = nil
            guard code.count == 8 else { err = nil; return }
            let r = await Circles.preview(code)
            preview = r.value
            err = r.err
        }
    }
}

/// Halka ayrıntısı: zikir sayacı ya da 30 cüz, üyeler, davet ve kurucu işleri
struct CircleDetailView: View {
    let id: String
    @Environment(\.dismiss) private var dismiss
    @State private var c: Circles.Circle?
    @State private var err: String?
    @State private var note: String?
    @State private var pending = 0
    @State private var ask: String? // "leave" | "delete"
    @State private var renaming = false
    @State private var newName = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if let err { Text(L(err)).foregroundStyle(Theme.err) }
                if let note { Text(L(note)).foregroundStyle(Theme.ok) }
                if let c { content(c) }
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 20)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(c.map(Circles.title) ?? L("circle.title"))
        .toolbar {
            if let c {
                ToolbarItem(placement: .topBarTrailing) { MoreMenu(items: menu(c)) }
            }
        }
        .task { apply(await Circles.detail(id)) }
        .confirmationDialog(L(ask == "leave" ? "circle.leaveConfirm" : "circle.deleteConfirm"),
                            isPresented: Binding(get: { ask != nil }, set: { if !$0 { ask = nil } }), titleVisibility: .visible) {
            let what = ask
            Button(L(what == "leave" ? "circle.leave" : "circle.delete"), role: .destructive) {
                Task {
                    if let e = await Circles.simple("/v1/halka/\(id)/" + (what == "leave" ? "ayril" : "sil")) { err = e } else { dismiss() }
                }
            }
        }
        .alert(L("circle.rename"), isPresented: $renaming) {
            TextField(L("circle.name"), text: $newName)
            Button(L("common.save")) { Task { apply(await Circles.edit(id, ["ad": newName.trimmingCharacters(in: .whitespaces)])) } }
            Button(L("common.cancel"), role: .cancel) {}
        }
    }

    private func apply(_ r: Circles.Res<Circles.Circle>) {
        if let v = r.value { c = v }
        err = r.err
    }

    private func menu(_ c: Circles.Circle) -> [MoreItem] {
        var items = [MoreItem(title: L("circle.reportName"), icon: "flag") {
            Task { note = await Circles.report(id, nil) ?? "circle.reported" }
        }]
        if c.owner {
            items.append(MoreItem(title: L("circle.rename"), icon: "pencil") { newName = c.name ?? ""; renaming = true })
            items.append(MoreItem(title: L("circle.renewCode"), icon: "arrow.triangle.2.circlepath") {
                Task { apply(await Circles.edit(id, ["kodYenile": true])) }
            })
            items.append(MoreItem(title: L("circle.delete"), icon: "trash", destructive: true) { ask = "delete" })
        }
        items.append(MoreItem(title: L("circle.leave"), icon: "rectangle.portrait.and.arrow.right", destructive: true) { ask = "leave" })
        return items
    }

    @ViewBuilder private func content(_ c: Circles.Circle) -> some View {
        Text(Circles.kindTitle(c) + " · " + T("circle.members", c.memberList.count)).font(.subheadline).foregroundStyle(Theme.muted)
        if c.kind == "zikir" {
            VStack(alignment: .leading, spacing: 8) {
                Text(L("zikir.kind.\(c.zikir ?? "subhanallah")")).font(.subheadline).foregroundStyle(Theme.muted)
                Text(Circles.progress(c)).font(.system(size: 30, weight: .light)).monospacedDigit().foregroundStyle(Theme.text)
                if c.target > 0 { ProgressView(value: min(1, Double(c.total) / Double(c.target))).tint(Theme.accent) }
                Text(L("circle.yourShare") + ": " + Circles.num(c.memberList.first { $0.you }?.count ?? 0))
                    .font(.subheadline).foregroundStyle(Theme.muted)
            }
            .card()
            // Dokunarak sayma: bir dokunuş bir; "Gönder" birikeni ekler
            Button { pending += 1 } label: {
                VStack(spacing: 4) {
                    Text(Circles.num(Int64(pending))).font(.system(size: 44, weight: .light)).monospacedDigit().foregroundStyle(Theme.text)
                    Text(L("zikir.tap")).font(.subheadline).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity, minHeight: 150)
                .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(Theme.accentSoft))
            }
            .buttonStyle(.plain)
            .sensoryFeedback(.impact(weight: .light), trigger: pending)
            HStack(spacing: 8) {
                RoundButton(title: "+33") { pending += 33 }
                RoundButton(title: "+100") { pending += 100 }
                RoundButton(title: T("circle.send", pending), prominent: pending > 0) {
                    let n = min(pending, 10_000)
                    guard n > 0 else { return }
                    Task {
                        if let e = await Circles.addZikir(id, n) { err = e } else { pending -= n; apply(await Circles.detail(id)) }
                    }
                }
                .layoutPriority(1)
            }
        } else {
            VStack(alignment: .leading, spacing: 12) {
                Text(T("circle.khatm", c.round) + " · " + T("circle.juzDone", c.juz.filter { $0.state == "bitti" }.count))
                    .font(.headline).foregroundStyle(Theme.text)
                JuzGrid(c: c) { n, op in Task { apply(await Circles.juz(id, n, op)) } }
            }
            .card()
        }
        if let code = c.code {
            VStack(alignment: .leading, spacing: 8) {
                Text(L("circle.code")).font(.footnote).foregroundStyle(Theme.muted)
                Text(code).font(.system(size: 24, weight: .semibold)).monospacedDigit().tracking(3).foregroundStyle(Theme.text)
                    .textSelection(.enabled)
                ShareLink(item: T("circle.shareText", Circles.title(c), code, Circles.link + code)) {
                    Text(L("circle.share")).font(.body.weight(.medium)).frame(maxWidth: .infinity, minHeight: 50)
                        .background(Capsule().fill(Theme.surface2)).foregroundStyle(Theme.text)
                }
            }
            .card()
        }
        SectionTitle(text: L("circle.membersTitle"))
        VStack(spacing: 0) {
            ForEach(c.memberList) { m in
                MemberRow(c: c, m: m) { action in
                    Task {
                        switch action {
                        case "report": note = await Circles.report(id, m.id) ?? "circle.reported"
                        case "block": apply(await Circles.block(id, m.id, true))
                        case "unblock": apply(await Circles.block(id, m.id, false))
                        default: apply(await Circles.kick(id, m.id))
                        }
                    }
                }
            }
        }
        .card(padding: 8)
        Text(L("circle.privacyNote")).font(.footnote).foregroundStyle(Theme.muted)
    }
}

/// 30 cüz: boş, alındı, sizde, bitti; dokununca işler
private struct JuzGrid: View {
    let c: Circles.Circle
    let onAction: (Int, String) -> Void
    private let cols = Array(repeating: GridItem(.flexible(), spacing: 6), count: 6)

    private func fill(_ j: Circles.Juz) -> Color {
        if j.state == "bitti" { return Theme.sage.opacity(0.35) }
        if j.you { return Theme.accent }
        if j.state == "alindi" { return Theme.gold.opacity(0.35) }
        return Theme.surface2
    }

    private func label(_ j: Circles.Juz) -> String {
        if j.state == "bitti" { return L("circle.juzDoneLabel") }
        if j.you { return L("circle.juzMine") }
        return L(j.state == "alindi" ? "circle.juzTaken" : "circle.juzFree")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            LazyVGrid(columns: cols, spacing: 6) {
                ForEach(c.juz) { j in
                    Menu {
                        Text("\(j.n) · \(label(j))")
                        if j.state == "bos" {
                            Button(L("circle.juzTake")) { onAction(j.n, "al") }
                            Button(L("circle.juzFinish")) { onAction(j.n, "bitir") }
                        }
                        if j.you, j.state == "alindi" {
                            Button(L("circle.juzFinish")) { onAction(j.n, "bitir") }
                            Button(L("circle.juzRelease")) { onAction(j.n, "birak") }
                        }
                    } label: {
                        Text("\(j.n)").font(.subheadline.weight(.semibold)).monospacedDigit()
                            .foregroundStyle(j.you && j.state != "bitti" ? Theme.onAccent : Theme.text)
                            .frame(maxWidth: .infinity).aspectRatio(1, contentMode: .fit)
                            .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(fill(j)))
                    }
                    .accessibilityLabel("\(j.n), \(label(j))")
                }
            }
            HStack(spacing: 12) {
                legend(Theme.surface2, "circle.juzFree")
                legend(Theme.gold.opacity(0.35), "circle.juzTaken")
                legend(Theme.accent, "circle.juzMine")
                legend(Theme.sage.opacity(0.35), "circle.juzDoneLabel")
            }
        }
    }

    private func legend(_ color: Color, _ key: String) -> some View {
        HStack(spacing: 4) {
            RoundedRectangle(cornerRadius: 3).fill(color).frame(width: 12, height: 12)
            Text(L(key)).font(.caption).foregroundStyle(Theme.muted)
        }
    }
}

private struct MemberRow: View {
    let c: Circles.Circle
    let m: Circles.Member
    let onAction: (String) -> Void

    var body: some View {
        let row = HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(m.blocked ? L("circle.blocked") : m.name ?? L("circle.nameHidden"))
                    .foregroundStyle(m.blocked || m.name == nil ? Theme.muted : Theme.text)
                let tags = [m.you ? L("circle.you") : nil, m.owner ? L("circle.owner") : nil].compactMap { $0 }
                if !tags.isEmpty { Text(tags.joined(separator: " · ")).font(.caption).foregroundStyle(Theme.muted) }
            }
            Spacer()
            if c.kind == "zikir" {
                Text(Circles.num(m.count)).monospacedDigit().foregroundStyle(Theme.text)
            } else {
                let n = c.juz.filter { $0.member == m.id && $0.state == "bitti" }.count
                if n > 0 { Text("\(n)").monospacedDigit().foregroundStyle(Theme.text) }
            }
        }
        .padding(.horizontal, 10)
        .frame(minHeight: 48)
        .contentShape(Rectangle())
        if m.you {
            row
        } else {
            Menu {
                Button(L("circle.report")) { onAction("report") }
                Button(L(m.blocked ? "circle.unblock" : "circle.block")) { onAction(m.blocked ? "unblock" : "block") }
                if c.owner { Button(L("circle.kick"), role: .destructive) { onAction("kick") } }
            } label: { row }
            .buttonStyle(.plain)
        }
    }
}
