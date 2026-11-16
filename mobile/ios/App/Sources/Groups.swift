import SwiftUI

/*
 * Ortak gruplar (üyelik aşama 2d, 15 Kasım 2026; sahibin onayı "tüm önerilerin uygun"; docs/uyelik-ortak-ozellikler-2026-11.md):
 * ortak geri sayım, ortak dünya saati, toplantı oylaması, odak odası, alarm önerisi, gözlem sayacı. Üyelik sunucusundaki
 * `/v1/grup…` uçları (server/src/groups.mjs). Giriş ister; din verisi olmadığı için ayrı rıza yok. Kullanıcı metni yalnız
 * grup adı, şehir adı ve alarm etiketi (sunucuda süzgeç); Bildir, Engelle, kurucu Çıkar. Sunucuda kapalıyken uçlar 404
 * verir: ekran "yakında" der. Android'deki Groups.kt ile aynı.
 */
enum Groups {
    /// Davet bağlantısı: halkalarla aynı sayfa, kod "g:" önekiyle (aşama 2c)
    static let link = "https://time.bumba.tr/davet/#g:"
    static let kinds = ["geri-sayim", "dunya", "oylama", "odak", "alarm", "gozlem"]
    static let icons = ["🎉", "🎂", "💍", "🎓", "✈️", "🏖️", "🎄", "🌙", "⭐", "❤️", "🏠", "👶", "⚽", "🎵", "📚", "🕌"]
    static var pendingCode: String?

    typealias Obj = [String: Any]
    struct Res<T> {
        var value: T?
        var err: String?
    }

    static func icon(_ kind: String) -> String {
        switch kind {
        case "geri-sayim": return "hourglass"
        case "dunya": return "globe"
        case "oylama": return "checklist"
        case "odak": return "tomato"
        case "alarm": return "alarm"
        default: return "telescope"
        }
    }

    static func errOf(_ r: Account.Resp) -> String {
        let o = Account.json(r.body)
        if r.code == 404, (o?["hata"] as? String) == "yok" { return "closed" }
        switch o?["hata"] as? String ?? "" {
        case "kod": return "group.err.code"
        case "ad", "ad-icerik", "sehir", "etiket": return "circle.err.name"
        case "grup-sinir": return "group.err.limit"
        case "dolu": return "group.full"
        case "alarm-sinir": return "group.a.limit"
        case "engelli": return "group.a.blocked"
        case "suruyor": return "group.f.none"
        case "uzaklastirildi": return "account.banned"
        case "bitti": return "group.p.closed"
        default: return Account.errorOf(r)
        }
    }

    private static func obj(_ method: String, _ path: String, _ body: Obj? = nil) async -> Res<Obj> {
        let r = await Account.shared.call(method, path, body, auth: true)
        guard r.code == 200, let o = Account.json(r.body) else { return Res(err: errOf(r)) }
        return Res(value: o)
    }

    static func list() async -> Res<[Obj]> {
        let r = await obj("GET", "/v1/gruplar")
        guard let v = r.value else { return Res<[Obj]>(err: r.err) }
        return Res<[Obj]>(value: v["gruplar"] as? [Obj] ?? [])
    }
    static func detail(_ id: String) async -> Res<Obj> { await obj("GET", "/v1/grup/\(id)") }
    static func create(_ body: Obj) async -> Res<Obj> { await obj("POST", "/v1/grup", body) }
    static func preview(_ code: String) async -> Res<Obj> { await obj("POST", "/v1/grup/onizle", ["kod": code]) }
    static func join(_ code: String) async -> Res<Obj> { await obj("POST", "/v1/grup/katil", ["kod": code]) }
    static func op(_ id: String, _ op: String, _ body: Obj = [:]) async -> Res<Obj> { await obj("POST", "/v1/grup/\(id)/\(op)", body) }
    static func weatherNear(_ lat: Double, _ lon: Double) async -> Res<Obj> { await obj("GET", "/v1/hava-bildirim?enlem=\(lat)&boylam=\(lon)") }
    static func weatherReport(_ kind: String, _ lat: Double, _ lon: Double) async -> Res<Obj> {
        await obj("POST", "/v1/hava-bildirim", ["tur": kind, "enlem": lat, "boylam": lon])
    }

    static func str(_ v: Any?) -> String? { (v as? String).flatMap { $0.isEmpty ? nil : $0 } }
    static func int(_ v: Any?) -> Int { (v as? NSNumber)?.intValue ?? 0 }
    static func int64(_ v: Any?) -> Int64 { (v as? NSNumber)?.int64Value ?? 0 }
    static func name(_ o: Obj) -> String { str(o["ad"]) ?? L("circle.nameHidden") }
    static func kindTitle(_ k: String) -> String { L("group.k.\(k)") }

    /// Geri sayım: yıllık ise bir sonraki yıl dönümü
    static func target(_ o: Obj, now: Date = Date()) -> Date {
        var t = Date(timeIntervalSince1970: Double(int64(o["hedef"])) / 1000)
        if o["yillik"] as? Bool == true {
            while t.addingTimeInterval(86_400) < now { t = Calendar.current.date(byAdding: .year, value: 1, to: t) ?? t.addingTimeInterval(365 * 86_400) }
        }
        return t
    }

    /// Gözlem sayacının olay anahtarı (Android'deki eventKey ile aynı)
    static func eventKey(_ e: SkyEvent) -> String {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "UTC")!
        let c = cal.dateComponents([.year, .month, .day], from: e.date)
        let d = String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
        let who = (e.id_ ?? e.p ?? [e.a, e.b].compactMap { $0 }.joined(separator: "-")).lowercased().filter { $0.isASCII && ($0.isLetter || $0.isNumber || $0 == "-") }
        return String("\(e.k)-\(d)-\(who)".lowercased().filter { $0.isASCII && ($0.isLetter || $0.isNumber || $0 == "-") }.prefix(60))
    }

    static func daysOf(_ mask: Int) -> [Int] { (0...6).filter { mask & (1 << $0) != 0 }.map { $0 + 1 } }

    static func dateTime(_ d: Date, tz: TimeZone = .current) -> String { Fmt.longDate(d, tz: tz) + " · " + Fmt.time(d, tz: tz) }
}

/// Gruplar: liste, kur, kodla katıl; seçilince ayrıntı
struct GroupsView: View {
    @State private var groups: [Groups.Obj]?
    @State private var err: String?
    @State private var signedIn = Account.shared.signedIn
    @State private var joining = Groups.pendingCode != nil && Account.shared.signedIn

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(L("group.intro")).font(.subheadline).foregroundStyle(Theme.muted)
                if !signedIn {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(L("group.signIn")).foregroundStyle(Theme.text)
                        NavigationLink { AccountView() } label: { GroupPill(title: L("circle.signInButton"), prominent: true) }
                    }
                    .card()
                } else if err == "closed" {
                    EmptyState(icon: "person.3", title: L("group.closed"))
                } else if let err {
                    Text(L(err)).foregroundStyle(Theme.err)
                } else if let groups {
                    if groups.isEmpty { EmptyState(icon: "person.3", title: L("group.empty")) }
                    ForEach(groups.indices, id: \.self) { i in
                        let g = groups[i]
                        NavigationLink { GroupDetailView(id: g["id"] as? String ?? "") } label: { GroupCard(g: g) }.buttonStyle(.plain)
                    }
                    HStack(spacing: 10) {
                        NavigationLink { GroupJoinView() } label: { GroupPill(title: L("circle.join"), prominent: false) }
                        NavigationLink { GroupCreateView() } label: { GroupPill(title: L("group.create"), prominent: true) }
                    }
                    Text(L("group.privacyNote")).font(.footnote).foregroundStyle(Theme.muted)
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
        .navigationTitle(L("group.title"))
        .navigationDestination(isPresented: $joining) { GroupJoinView() }
        .onAppear {
            signedIn = Account.shared.signedIn
            Task { await reload() }
        }
    }

    private func reload() async {
        guard Account.shared.signedIn else { return }
        let r = await Groups.list()
        err = r.err
        if let v = r.value { groups = v }
    }
}

private struct GroupPill: View {
    let title: String
    let prominent: Bool
    var body: some View {
        Text(title).font(.body.weight(.medium)).frame(maxWidth: .infinity, minHeight: 50)
            .background(Capsule().fill(prominent ? Theme.accent : Theme.surface2))
            .foregroundStyle(prominent ? Theme.onAccent : Theme.text)
    }
}

private struct GroupCard: View {
    let g: Groups.Obj
    var body: some View {
        let kind = g["tur"] as? String ?? ""
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                DuoChip(name: Groups.icon(kind), color: Theme.sky)
                VStack(alignment: .leading, spacing: 2) {
                    Text(Groups.name(g)).font(.headline).foregroundStyle(Theme.text)
                    Text(Groups.kindTitle(kind) + " · " + T("circle.members", Groups.int(g["uyeSayisi"]))).font(.footnote).foregroundStyle(Theme.muted)
                }
                Spacer()
                Image(systemName: "chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
            }
            TimelineView(.periodic(from: .now, by: 1)) { ctx in
                if let line = line(kind, ctx.date) { Text(line).monospacedDigit().foregroundStyle(Theme.text) }
            }
        }
        .card()
    }

    private func line(_ kind: String, _ now: Date) -> String? {
        switch kind {
        case "geri-sayim":
            let days = Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: now),
                                                       to: Calendar.current.startOfDay(for: Groups.target(g, now: now))).day ?? 0
            return (g["simge"] as? String ?? "") + " " + (days <= 0 ? L("group.cd.today") : T("group.cd.days", days))
        case "odak":
            guard let s = g["oturum"] as? Groups.Obj else { return nil }
            let left = (Double(Groups.int64(s["bas"])) / 1000 + Double(Groups.int(s["dk"]) * 60)) - now.timeIntervalSince1970
            return L("group.f.left") + ": " + groupHms(Int(max(0, left)))
        case "alarm":
            let n = Groups.int(g["bekleyen"])
            return n > 0 ? L("group.a.incoming") + ": \(n)" : nil
        default: return nil
        }
    }
}

struct GroupCreateView: View {
    @State private var kind = "geri-sayim"
    @State private var name = ""
    @State private var target = Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    @State private var yearly = false
    @State private var icon = "🎉"
    @State private var slots: [Date] = []
    @State private var slot = Calendar.current.date(byAdding: .day, value: 1, to: Date()) ?? Date()
    @State private var duration = 60
    @State private var err: String?
    @State private var busy = false
    @State private var created: String?

    private var ready: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty && (kind != "oylama" || !slots.isEmpty) }

    var body: some View {
        Form {
            Section(L("group.kindTitle")) {
                ForEach(Groups.kinds, id: \.self) { k in
                    Button { kind = k } label: {
                        HStack(spacing: 12) {
                            Image(systemName: kind == k ? "largecircle.fill.circle" : "circle").foregroundStyle(Theme.accent)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(L("group.k.\(k)")).foregroundStyle(Theme.text)
                                Text(L("group.d.\(k)")).font(.footnote).foregroundStyle(Theme.muted)
                            }
                        }
                    }
                }
            }
            Section {
                TextField(L("group.name"), text: $name).onChange(of: name) { name = String(name.prefix(40)) }
            }
            if kind == "geri-sayim" {
                Section {
                    DatePicker(L("group.cd.target"), selection: $target)
                    Toggle(L("group.cd.yearly"), isOn: $yearly).tint(Theme.accent)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 4) {
                            ForEach(Groups.icons, id: \.self) { e in
                                Button { icon = e } label: {
                                    Text(e).font(.title2).padding(6)
                                        .background(RoundedRectangle(cornerRadius: 10).fill(e == icon ? Theme.accentSoft : .clear))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                } header: { Text(L("group.cd.icon")) }
            }
            if kind == "oylama" {
                Section(L("group.p.slots")) {
                    Picker(T("group.p.duration", duration), selection: $duration) {
                        ForEach([15, 30, 45, 60, 90, 120], id: \.self) { Text(T("group.p.duration", $0)).tag($0) }
                    }
                    ForEach(slots, id: \.self) { s in Text(Groups.dateTime(s)) }
                        .onDelete { slots.remove(atOffsets: $0) }
                    if slots.count < 12 {
                        DatePicker("", selection: $slot).labelsHidden()
                        Button(L("group.p.add")) {
                            let s = Calendar.current.date(bySetting: .second, value: 0, of: slot) ?? slot
                            if !slots.contains(s) { slots = (slots + [s]).sorted() }
                        }
                        .tint(Theme.accent)
                    }
                }
            }
            Section { CommunityRules() }.listRowBackground(Color.clear)
            if let err { Text(L(err)).foregroundStyle(Theme.err) }
            Section {
                RoundButton(title: L("group.create"), prominent: ready) {
                    guard ready, !busy else { return }
                    busy = true
                    var b: Groups.Obj = ["tur": kind, "ad": name.trimmingCharacters(in: .whitespaces)]
                    if kind == "geri-sayim" { b["hedef"] = Int64(target.timeIntervalSince1970 * 1000); b["yillik"] = yearly; b["simge"] = icon }
                    if kind == "oylama" { b["sure"] = duration; b["saatler"] = slots.map { Int64($0.timeIntervalSince1970 / 60) } }
                    Task {
                        let r = await Groups.create(b)
                        busy = false
                        err = r.err
                        created = r.value?["id"] as? String
                    }
                }
            }
            .listRowBackground(Color.clear).listRowInsets(EdgeInsets())
        }
        .navigationTitle(L("group.create"))
        .themedBackground()
        .navigationDestination(item: $created) { GroupDetailView(id: $0) }
    }
}

struct GroupJoinView: View {
    @State private var code = Groups.pendingCode ?? ""
    @State private var preview: Groups.Obj?
    @State private var err: String?
    @State private var joined: String?

    private var ready: Bool { preview != nil && preview?["dolu"] as? Bool != true }

    var body: some View {
        Form {
            Section {
                TextField(L("circle.codeField"), text: $code)
                    .textInputAutocapitalization(.characters).autocorrectionDisabled()
                    .onChange(of: code) {
                        let c = String(code.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(8))
                        if c != code { code = c }
                    }
            }
            if let p = preview {
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(Groups.name(p)).font(.headline)
                        Text(Groups.kindTitle(p["tur"] as? String ?? "") + " · " + T("circle.members", Groups.int(p["uyeSayisi"])))
                            .font(.subheadline).foregroundStyle(Theme.muted)
                        if p["dolu"] as? Bool == true { Text(L("group.full")).foregroundStyle(Theme.err) }
                    }
                }
            }
            Section { CommunityRules() }.listRowBackground(Color.clear)
            if let err { Text(L(err)).foregroundStyle(Theme.err) }
            Section {
                RoundButton(title: L("circle.joinButton"), prominent: ready) {
                    guard ready else { return }
                    Task {
                        let r = await Groups.join(code)
                        err = r.err
                        joined = r.value?["id"] as? String
                    }
                }
            }
            .listRowBackground(Color.clear).listRowInsets(EdgeInsets())
        }
        .navigationTitle(L("circle.join"))
        .themedBackground()
        .navigationDestination(item: $joined) { GroupDetailView(id: $0) }
        .task(id: code) {
            Groups.pendingCode = nil
            preview = nil
            guard code.count == 8 else { err = nil; return }
            let r = await Groups.preview(code)
            preview = r.value
            err = r.err
        }
    }
}

/// Grup ayrıntısı: türüne göre bölüm, davet, üyeler ve kurucu işleri
struct GroupDetailView: View {
    let id: String
    @Environment(\.dismiss) private var dismiss
    @State private var g: Groups.Obj?
    @State private var err: String?
    @State private var note: String?
    @State private var ask: String?
    @State private var renaming = false
    @State private var newName = ""

    private var members: [Groups.Obj] { g?["uyeler"] as? [Groups.Obj] ?? [] }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if let err { Text(L(err)).foregroundStyle(Theme.err) }
                if let note { Text(L(note)).foregroundStyle(Theme.ok) }
                if let g { content(g) }
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 20)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(g.map(Groups.name) ?? L("group.title"))
        .toolbar {
            if let g { ToolbarItem(placement: .topBarTrailing) { MoreMenu(items: menu(g)) } }
        }
        .task { apply(await Groups.detail(id)) }
        .confirmationDialog(L(ask == "leave" ? "group.leaveConfirm" : "group.deleteConfirm"),
                            isPresented: Binding(get: { ask != nil }, set: { if !$0 { ask = nil } }), titleVisibility: .visible) {
            let what = ask
            Button(L(what == "leave" ? "group.leave" : "group.delete"), role: .destructive) {
                Task {
                    let r = await Groups.op(id, what == "leave" ? "ayril" : "sil")
                    if r.err == nil { dismiss() } else { err = r.err }
                }
            }
        }
        .alert(L("circle.rename"), isPresented: $renaming) {
            TextField(L("group.name"), text: $newName)
            Button(L("common.save")) { run("duzenle", ["ad": newName.trimmingCharacters(in: .whitespaces)]) }
            Button(L("common.cancel"), role: .cancel) {}
        }
    }

    private func apply(_ r: Groups.Res<Groups.Obj>, ok: String? = nil) {
        if let v = r.value { g = v; note = ok }
        err = r.err
    }

    private func run(_ op: String, _ body: Groups.Obj = [:], ok: String? = nil) {
        Task { apply(await Groups.op(id, op, body), ok: ok) }
    }

    private func report(_ body: Groups.Obj) {
        Task {
            let r = await Groups.op(id, "bildir", body)
            note = r.err == nil ? "circle.reported" : nil
            err = r.err
            if let v = await Groups.detail(id).value { g = v }
        }
    }

    private func menu(_ g: Groups.Obj) -> [MoreItem] {
        var items = [MoreItem(title: L("group.reportName"), icon: "flag") { report([:]) }]
        if g["sahip"] as? Bool == true {
            items.append(MoreItem(title: L("circle.rename"), icon: "pencil") { newName = g["ad"] as? String ?? ""; renaming = true })
            items.append(MoreItem(title: L("circle.renewCode"), icon: "arrow.triangle.2.circlepath") { run("duzenle", ["kodYenile": true]) })
            items.append(MoreItem(title: L("group.delete"), icon: "trash", destructive: true) { ask = "delete" })
        }
        items.append(MoreItem(title: L("group.leave"), icon: "rectangle.portrait.and.arrow.right", destructive: true) { ask = "leave" })
        return items
    }

    @ViewBuilder private func content(_ g: Groups.Obj) -> some View {
        let kind = g["tur"] as? String ?? ""
        Text(Groups.kindTitle(kind) + " · " + T("circle.members", members.count)).font(.subheadline).foregroundStyle(Theme.muted)
        switch kind {
        case "geri-sayim":
            GroupCountdownSection(g: g, onAdded: { note = "group.cd.added" }) { run("duzenle", ["hedef": $0]) }
        case "dunya":
            GroupWorldSection(members: members) { run("yerim", $0) }
        case "oylama":
            GroupPollSection(g: g, members: members, onVote: { run("oy", ["oylar": $0], ok: "group.p.saved") }) { run("duzenle", ["kapali": true]) }
        case "odak":
            GroupFocusSection(g: g, members: members) { op, dk in run("odak", ["islem": op, "dk": dk]) }
        case "alarm":
            GroupAlarmSection(g: g, members: members, onSend: { run("alarm", $0, ok: "group.a.sent") }, onAnswer: { req, ok in
                if ok { Task { await GroupAlarm.install(req) } }
                run("alarm-yanit", ["istek": Groups.int(req["id"]), "kabul": ok], ok: ok ? "group.a.installed" : nil)
            }) { report(["istek": Groups.int($0["id"])]) }
        default:
            GroupWatchSection(g: g) { key, on in run("izledim", ["olay": key, "kaldir": !on]) }
        }
        if let code = Groups.str(g["kod"]) {
            VStack(alignment: .leading, spacing: 8) {
                Text(L("circle.code")).font(.footnote).foregroundStyle(Theme.muted)
                Text(code).font(.system(size: 24, weight: .semibold)).monospacedDigit().tracking(3).foregroundStyle(Theme.text).textSelection(.enabled)
                ShareLink(item: T("group.shareText", Groups.name(g), code, Groups.link + code)) { GroupPill(title: L("circle.share"), prominent: false) }
            }
            .card()
        }
        SectionTitle(text: L("circle.membersTitle"))
        VStack(spacing: 0) {
            ForEach(members.indices, id: \.self) { i in
                let m = members[i]
                GroupMemberRow(g: g, m: m) { action in
                    let mid = Groups.int64(m["id"])
                    switch action {
                    case "report": report(["uye": mid])
                    case "block": run("engelle", ["uye": mid])
                    case "unblock": run("engelle", ["uye": mid, "kaldir": true])
                    default: run("cikar", ["uye": mid])
                    }
                }
            }
        }
        .card(padding: 8)
        Text(L("group.privacyNote")).font(.footnote).foregroundStyle(Theme.muted)
    }
}

/// Onaylanan alarm önerisi telefona kurulur
@MainActor
enum GroupAlarm {
    static func install(_ req: Groups.Obj) async {
        let parts = (req["saat"] as? String ?? "07:00").split(separator: ":").compactMap { Int($0) }
        guard parts.count == 2 else { return }
        var item = AlarmItem(hour: parts[0], minute: parts[1])
        item.label = Groups.str(req["etiket"]) ?? ""
        item.days = Groups.daysOf(Groups.int(req["gunler"]))
        var items = AlarmItem.load()
        items.append(item)
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
    }
}

private func memberName(_ m: Groups.Obj) -> String {
    m["engelli"] as? Bool == true ? L("circle.blocked") : Groups.str(m["ad"]) ?? L("circle.nameHidden")
}

private struct GroupCountdownSection: View {
    let g: Groups.Obj
    let onAdded: () -> Void
    let onEdit: (Int64) -> Void
    @State private var editing = false
    @State private var newTarget = Date()

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            let t = Groups.target(g, now: ctx.date)
            let left = max(0, Int(t.timeIntervalSince(ctx.date)))
            VStack(alignment: .leading, spacing: 8) {
                Text(g["simge"] as? String ?? "🎉").font(.system(size: 34))
                Text(left == 0 ? L("group.cd.today") : T("group.cd.days", left / 86_400)).font(.system(size: 28, weight: .light)).monospacedDigit()
                if left > 0 { Text(groupHms(left % 86_400)).monospacedDigit().foregroundStyle(Theme.muted) }
                Text(Groups.dateTime(t)).font(.subheadline).foregroundStyle(Theme.muted)
                RoundButton(title: L("group.cd.add"), prominent: true) {
                    var list = CountdownItem.load()
                    let title = Groups.name(g)
                    let ms = Groups.int64(g["hedef"])
                    if !list.contains(where: { $0.target == ms && $0.title == title }) {
                        var item = CountdownItem(target: ms)
                        item.title = title
                        item.emoji = g["simge"] as? String ?? "🎉"
                        item.yearly = g["yillik"] as? Bool == true
                        list.append(item)
                        CountdownItem.save(list)
                    }
                    onAdded()
                }
                if g["sahip"] as? Bool == true {
                    Button(L("group.cd.target")) { newTarget = t; editing = true }.tint(Theme.accent)
                }
            }
            .card()
        }
        .sheet(isPresented: $editing) {
            NavigationStack {
                Form { DatePicker(L("group.cd.target"), selection: $newTarget) }
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button(L("common.save")) { editing = false; onEdit(Int64(newTarget.timeIntervalSince1970 * 1000)) }
                        }
                        ToolbarItem(placement: .cancellationAction) { Button(L("common.cancel")) { editing = false } }
                    }
            }
            .presentationDetents([.medium])
        }
    }
}

private struct GroupWorldSection: View {
    let members: [Groups.Obj]
    let onSet: (Groups.Obj) -> Void
    @State private var from = 9
    @State private var to = 18

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { ctx in
            VStack(spacing: 0) {
                ForEach(members.indices, id: \.self) { i in row(members[i], ctx.date) }
            }
            .card(padding: 8)
        }
        VStack(alignment: .leading, spacing: 10) {
            Picker(L("group.w.workFrom"), selection: $from) { ForEach(0..<24, id: \.self) { Text(String(format: "%02d:00", $0)).tag($0) } }
            Picker(L("group.w.workTo"), selection: $to) { ForEach(1..<25, id: \.self) { Text(String(format: "%02d:00", $0)).tag($0) } }
            RoundButton(title: L("group.w.share"), prominent: true) {
                let p = PlaceModel.shared.place
                var b: Groups.Obj = ["sehir": String(p.name.prefix(40)), "tz": p.zone.identifier]
                if from != to { b["mesaiBas"] = from; b["mesaiSon"] = to }
                onSet(b)
            }
            if members.first(where: { $0["sen"] as? Bool == true })?["tz"] != nil {
                Button(L("group.w.remove"), role: .destructive) { onSet([:]) }
            }
        }
        .tint(Theme.accent)
        .card()
        .onAppear {
            if let w = members.first(where: { $0["sen"] as? Bool == true })?["mesai"] as? [Int], w.count == 2 { from = w[0]; to = w[1] }
        }
    }

    @ViewBuilder private func row(_ m: Groups.Obj, _ now: Date) -> some View {
        let tz = Groups.str(m["tz"]).flatMap(TimeZone.init(identifier:))
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(memberName(m)).foregroundStyle(Theme.text)
                Text(sub(m, tz, now)).font(.footnote).foregroundStyle(Theme.muted)
            }
            Spacer()
            if let tz {
                VStack(alignment: .trailing, spacing: 2) {
                    Text(Fmt.time(now, tz: tz)).font(.title3).monospacedDigit().foregroundStyle(Theme.text)
                    let diff = (tz.secondsFromGMT(for: now) - TimeZone.current.secondsFromGMT(for: now)) / 60
                    if diff != 0 {
                        Text((diff > 0 ? "+" : "−") + "\(abs(diff) / 60)" + (abs(diff) % 60 != 0 ? String(format: ":%02d", abs(diff) % 60) : ""))
                            .font(.caption).monospacedDigit().foregroundStyle(Theme.muted)
                    }
                }
            }
        }
        .padding(.horizontal, 10)
        .frame(minHeight: 52)
    }

    private func sub(_ m: Groups.Obj, _ tz: TimeZone?, _ now: Date) -> String {
        guard let tz else { return L("group.w.none") }
        var parts: [String] = []
        if let s = Groups.str(m["sehir"]) { parts.append(s) }
        if let w = m["mesai"] as? [Int], w.count == 2 {
            var cal = Calendar.current
            cal.timeZone = tz
            let h = cal.component(.hour, from: now)
            let on = w[0] < w[1] ? (h >= w[0] && h < w[1]) : (h >= w[0] || h < w[1])
            parts.append(L(on ? "group.w.working" : "group.w.off"))
        }
        return parts.joined(separator: " · ")
    }
}

private struct GroupPollSection: View {
    let g: Groups.Obj
    let members: [Groups.Obj]
    let onVote: ([Int]) -> Void
    let onClose: () -> Void
    @State private var votes: [Int] = []

    private var slots: [Int64] { (g["saatler"] as? [Any] ?? []).map { Groups.int64($0) } }
    private var tally: [Groups.Obj] { g["sonuc"] as? [Groups.Obj] ?? [] }
    private var best: Int { (g["enIyi"] as? NSNumber)?.intValue ?? -1 }
    private var closed: Bool { g["kapali"] as? Bool == true }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(T("group.p.duration", Groups.int(g["sure"]))).font(.footnote).foregroundStyle(Theme.muted)
            ForEach(slots.indices, id: \.self) { i in
                let at = Date(timeIntervalSince1970: Double(slots[i]) * 60)
                let t = i < tally.count ? tally[i] : [:]
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(Groups.dateTime(at)).fontWeight(i == best ? .semibold : .regular).foregroundStyle(Theme.text)
                        Spacer()
                        Text(T("group.p.count", Groups.int(t["uygun"]), Groups.int(t["olabilir"]))).font(.caption).foregroundStyle(Theme.muted)
                    }
                    if !closed, i < votes.count {
                        Picker("", selection: $votes[i]) {
                            Text(L("group.p.yes")).tag(2)
                            Text(L("group.p.maybe")).tag(1)
                            Text(L("group.p.no")).tag(0)
                        }
                        .pickerStyle(.segmented)
                    }
                }
                .padding(8)
                .background(RoundedRectangle(cornerRadius: 14).fill(i == best ? Theme.accentSoft : .clear))
            }
            if best >= 0, best < slots.count {
                Text(L("group.p.best") + ": " + Groups.dateTime(Date(timeIntervalSince1970: Double(slots[best]) * 60))).fontWeight(.semibold)
            }
            if closed {
                Text(L("group.p.closed")).foregroundStyle(Theme.muted)
            } else {
                RoundButton(title: L("group.p.save"), prominent: true) { onVote(votes) }
                if g["sahip"] as? Bool == true { Button(L("group.p.close"), role: .destructive, action: onClose) }
            }
        }
        .card()
        .onAppear { reset() }
        .onChange(of: slots) { reset() }
    }

    private func reset() {
        let mine = members.first(where: { $0["sen"] as? Bool == true })?["oy"] as? [Int]
        votes = slots.indices.map { i in mine.map { m in i < m.count ? m[i] : 0 } ?? 0 }
    }
}

private struct GroupFocusSection: View {
    let g: Groups.Obj
    let members: [Groups.Obj]
    let onOp: (String, Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let s = g["oturum"] as? Groups.Obj {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    let left = Double(Groups.int64(s["bas"])) / 1000 + Double(Groups.int(s["dk"]) * 60) - ctx.date.timeIntervalSince1970
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("group.f.left")).font(.footnote).foregroundStyle(Theme.muted)
                        Text(groupHms(Int(max(0, left)))).font(.system(size: 40, weight: .light)).monospacedDigit()
                    }
                }
                let me = members.first(where: { $0["sen"] as? Bool == true })
                if me?["katildi"] as? Bool != true {
                    RoundButton(title: L("group.f.join"), prominent: true) { onOp("katil", 0) }
                } else if me?["bitti"] as? Bool != true {
                    RoundButton(title: L("group.f.done"), prominent: true) { onOp("bitti", 0) }
                }
                ForEach(members.indices.filter { members[$0]["katildi"] as? Bool == true }, id: \.self) { i in
                    Text(memberName(members[i]) + " · " + L(members[i]["bitti"] as? Bool == true ? "group.f.finished" : "group.f.joined"))
                        .font(.subheadline)
                }
            } else {
                Text(L("group.f.none")).foregroundStyle(Theme.muted)
                HStack(spacing: 8) {
                    RoundButton(title: T("group.f.start", 25), prominent: true) { onOp("baslat", 25) }
                    RoundButton(title: T("group.f.start", 50)) { onOp("baslat", 50) }
                }
            }
            Text(L("group.f.note")).font(.footnote).foregroundStyle(Theme.muted)
        }
        .card()
    }
}

private struct GroupAlarmSection: View {
    let g: Groups.Obj
    let members: [Groups.Obj]
    let onSend: (Groups.Obj) -> Void
    let onAnswer: (Groups.Obj, Bool) -> Void
    let onReport: (Groups.Obj) -> Void
    @State private var composing = false

    private var reqs: [Groups.Obj] { g["istekler"] as? [Groups.Obj] ?? [] }

    private func who(_ id: Any?) -> String {
        guard let n = (id as? NSNumber)?.int64Value, let m = members.first(where: { Groups.int64($0["id"]) == n }) else { return L("circle.formerMember") }
        return memberName(m)
    }

    private func days(_ mask: Int) -> String {
        mask == 0 ? L("group.a.once") : Groups.daysOf(mask).map { L("calendar.weekdaysShort.\($0 - 1)") }.joined(separator: " ")
    }

    var body: some View {
        let incoming = reqs.filter { $0["gelen"] as? Bool == true }
        if !incoming.isEmpty {
            SectionTitle(text: L("group.a.incoming"))
            ForEach(incoming.indices, id: \.self) { i in
                let r = incoming[i]
                VStack(alignment: .leading, spacing: 6) {
                    Text((r["saat"] as? String ?? "") + "  " + days(Groups.int(r["gunler"]))).font(.title3).monospacedDigit()
                    if let l = Groups.str(r["etiket"]) { Text(l) }
                    Text(T("group.a.from", who(r["kimden"]))).font(.footnote).foregroundStyle(Theme.muted)
                    if r["durum"] as? String == "bekliyor" {
                        HStack(spacing: 8) {
                            RoundButton(title: L("group.a.decline")) { onAnswer(r, false) }
                            RoundButton(title: L("group.a.accept"), prominent: true) { onAnswer(r, true) }
                        }
                    } else {
                        Text(L(r["durum"] as? String == "kuruldu" ? "group.a.set" : "group.a.declined")).font(.footnote).foregroundStyle(Theme.muted)
                    }
                    if Groups.str(r["etiket"]) != nil { Button(L("circle.report")) { onReport(r) }.font(.footnote).tint(Theme.muted) }
                }
                .card()
            }
        }
        VStack(alignment: .leading, spacing: 10) {
            Text(L("group.a.note")).font(.footnote).foregroundStyle(Theme.muted)
            if members.contains(where: { $0["sen"] as? Bool != true }) {
                RoundButton(title: L("group.a.send"), prominent: true) { composing = true }
            }
        }
        .card()
        let outgoing = reqs.filter { $0["gelen"] as? Bool != true }
        if !outgoing.isEmpty {
            SectionTitle(text: L("group.a.outgoing"))
            VStack(spacing: 6) {
                ForEach(outgoing.indices, id: \.self) { i in
                    let r = outgoing[i]
                    HStack {
                        Text((r["saat"] as? String ?? "") + " · " + who(r["kime"]))
                        Spacer()
                        Text(L(r["durum"] as? String == "kuruldu" ? "group.a.set" : r["durum"] as? String == "reddedildi" ? "group.a.declined" : "group.a.pending"))
                            .font(.footnote).foregroundStyle(Theme.muted)
                    }
                }
            }
            .card(padding: 12)
        }
        EmptyView()
            .sheet(isPresented: $composing) { AlarmSuggestSheet(members: members.filter { $0["sen"] as? Bool != true && $0["engelli"] as? Bool != true }, onSend: onSend) }
    }
}

private struct AlarmSuggestSheet: View {
    let members: [Groups.Obj]
    let onSend: (Groups.Obj) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var to: Int64 = 0
    @State private var time = Calendar.current.date(bySettingHour: 7, minute: 0, second: 0, of: Date()) ?? Date()
    @State private var mask = 0
    @State private var label = ""

    var body: some View {
        NavigationStack {
            Form {
                Picker(L("group.a.to"), selection: $to) {
                    ForEach(members.indices, id: \.self) { i in Text(memberName(members[i])).tag(Groups.int64(members[i]["id"])) }
                }
                DatePicker(L("group.a.time"), selection: $time, displayedComponents: .hourAndMinute)
                Section(L("group.a.days")) {
                    HStack(spacing: 4) {
                        ForEach(0..<7, id: \.self) { d in
                            Button { mask ^= 1 << d } label: {
                                Text(L("calendar.weekdaysShort.\(d)")).font(.footnote).frame(maxWidth: .infinity, minHeight: 32)
                                    .background(RoundedRectangle(cornerRadius: 10).fill(mask & (1 << d) != 0 ? Theme.accentSoft : Theme.surface2))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                TextField(L("group.a.label"), text: $label).onChange(of: label) { label = String(label.prefix(40)) }
            }
            .navigationTitle(L("group.a.send"))
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("group.a.send")) {
                        let c = Calendar.current.dateComponents([.hour, .minute], from: time)
                        var b: Groups.Obj = ["uye": to, "saat": String(format: "%02d:%02d", c.hour ?? 7, c.minute ?? 0), "gunler": mask]
                        let l = label.trimmingCharacters(in: .whitespaces)
                        if !l.isEmpty { b["etiket"] = l }
                        dismiss()
                        onSend(b)
                    }
                    .disabled(to == 0)
                }
                ToolbarItem(placement: .cancellationAction) { Button(L("common.cancel")) { dismiss() } }
            }
            .onAppear { if to == 0, let f = members.first { to = Groups.int64(f["id"]) } }
        }
    }
}

private struct GroupWatchSection: View {
    let g: Groups.Obj
    let onToggle: (String, Bool) -> Void

    var body: some View {
        let now = Date()
        let events = SkyEvent.all.filter { $0.date > now.addingTimeInterval(-7 * 86_400) && $0.date < now.addingTimeInterval(30 * 86_400) }
        let counts = g["sayilar"] as? [String: Any] ?? [:]
        let mine = Set(g["benim"] as? [String] ?? [])
        VStack(alignment: .leading, spacing: 8) {
            Text(L("group.o.note")).font(.footnote).foregroundStyle(Theme.muted)
            if events.isEmpty { Text(L("group.o.none")).foregroundStyle(Theme.muted) }
            ForEach(events) { e in
                let key = Groups.eventKey(e)
                let on = mine.contains(key)
                Button { onToggle(key, !on) } label: {
                    HStack(spacing: 10) {
                        Image(systemName: on ? "checkmark.square.fill" : "square").foregroundStyle(Theme.accent)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(e.title).foregroundStyle(Theme.text)
                            Text(Fmt.longDate(e.date)).font(.caption).foregroundStyle(Theme.muted)
                        }
                        Spacer()
                        let n = Groups.int(counts[key])
                        if n > 0 { Text(T("group.o.count", n)).font(.caption).foregroundStyle(Theme.muted) }
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .card()
    }
}

private struct GroupMemberRow: View {
    let g: Groups.Obj
    let m: Groups.Obj
    let onAction: (String) -> Void

    var body: some View {
        let you = m["sen"] as? Bool == true
        let blocked = m["engelli"] as? Bool == true
        let row = HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(memberName(m)).foregroundStyle(blocked || Groups.str(m["ad"]) == nil ? Theme.muted : Theme.text)
                let tags = [you ? L("circle.you") : nil, m["sahip"] as? Bool == true ? L("circle.owner") : nil].compactMap { $0 }
                if !tags.isEmpty { Text(tags.joined(separator: " · ")).font(.caption).foregroundStyle(Theme.muted) }
            }
            Spacer()
        }
        .padding(.horizontal, 10)
        .frame(minHeight: 48)
        .contentShape(Rectangle())
        if you {
            row
        } else {
            Menu {
                Button(L("circle.report")) { onAction("report") }
                Button(L(blocked ? "circle.unblock" : "circle.block")) { onAction(blocked ? "unblock" : "block") }
                if g["sahip"] as? Bool == true { Button(L("group.kick"), role: .destructive) { onAction("kick") } }
            } label: { row }
            .buttonStyle(.plain)
        }
    }
}

/// Hava ekranındaki anonim hava bildirimi kartı (girişliyken; sunucuda kapalıysa görünmez)
struct WeatherReportCard: View {
    let lat: Double
    let lon: Double
    @State private var counts: [String: Any]?
    @State private var hidden = false
    @State private var msg: String?
    private let kinds = ["acik", "bulutlu", "yagmur", "saganak", "kar", "dolu", "sis", "ruzgar", "firtina"]

    var body: some View {
        if Account.shared.signedIn, !hidden {
            VStack(alignment: .leading, spacing: 10) {
                Text(L("wxr.title")).font(.headline).foregroundStyle(Theme.text)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 84), spacing: 6)], spacing: 6) {
                    ForEach(kinds, id: \.self) { k in
                        Button { send(k) } label: {
                            Text(L("wxr.k.\(k)")).font(.subheadline).frame(maxWidth: .infinity, minHeight: 34)
                                .background(Capsule().fill(Theme.surface2)).foregroundStyle(Theme.text)
                        }
                        .buttonStyle(.plain)
                    }
                }
                if let msg { Text(L(msg)).font(.footnote).foregroundStyle(msg == "wxr.thanks" ? Theme.ok : Theme.muted) }
                if let c = counts {
                    if c.isEmpty {
                        Text(L("wxr.none")).font(.subheadline).foregroundStyle(Theme.muted)
                    } else {
                        Text(L("wxr.near") + " " + c.keys.sorted { Groups.int(c[$0]) > Groups.int(c[$1]) }.map { L("wxr.k.\($0)") + " \(Groups.int(c[$0]))" }.joined(separator: ", "))
                            .font(.subheadline)
                    }
                }
                Text(L("wxr.note")).font(.caption).foregroundStyle(Theme.muted)
            }
            .card()
            .task(id: "\(lat),\(lon)") {
                let r = await Groups.weatherNear(lat, lon)
                if r.err == "closed" { hidden = true }
                counts = r.value?["sayilar"] as? [String: Any]
            }
        }
    }

    private func send(_ k: String) {
        Task {
            let r = await Groups.weatherReport(k, lat, lon)
            if let v = r.value {
                counts = v["sayilar"] as? [String: Any]
                msg = "wxr.thanks"
            } else {
                msg = r.err == "account.err.tooMany" ? "wxr.wait" : r.err
            }
        }
    }
}

/// Kalan süre "1:02:03" ya da "02:03"
private func groupHms(_ sec: Int) -> String {
    let s = max(0, sec)
    return s >= 3600 ? String(format: "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60) : String(format: "%02d:%02d", s / 60, s % 60)
}
