import SwiftUI

/// Sabah günlüğü (rakip analizi listesi #76): her gün için ruh hâli, uyku, günün niyeti ve not.
/// Kayıt `journal` (JSON [{d, m, s, i, n}], en yeni üstte, en çok 366 gün; Android'le aynı); yalnız cihazda (iCloud eşitlemesi açıksa
/// Apple hesabındaki cihazlarda), hiçbir yere gönderilmez.
struct JournalEntry: Codable, Equatable {
    /// Gün (yerel, YYYY-AA-GG), ruh hâli ve uyku (1–5, 0 = seçilmedi), niyet, not
    var d: String
    var m = 0
    var s = 0
    var i = ""
    var n = ""

    var isEmpty: Bool { m == 0 && s == 0 && i.isEmpty && n.isEmpty }
}

enum Journal {
    static let key = "journal"
    static let moods = ["😞", "🙁", "😐", "🙂", "😄"]

    static func load() -> [JournalEntry] {
        guard let raw = UserDefaults.standard.string(forKey: key), let d = raw.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([JournalEntry].self, from: d)) ?? []
    }

    static func save(_ list: [JournalEntry]) {
        let kept = Array(list.filter { !$0.isEmpty }.sorted { $0.d > $1.d }.prefix(366))
        if let d = try? JSONEncoder().encode(kept) { UserDefaults.standard.set(String(data: d, encoding: .utf8), forKey: key) }
    }

    static var todayKey: String { BadgeStore.day(Date()) }

    static func today() -> JournalEntry { load().first { $0.d == todayKey } ?? JournalEntry(d: todayKey) }

    static func put(_ e: JournalEntry) { save([e] + load().filter { $0.d != e.d }) }
}

/// Bugünün ruh hâli ve niyeti (günaydın kartında ve günlük sayfasında)
struct JournalQuick: View {
    @Binding var entry: JournalEntry
    var showSleep = true

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(L("journal.mood")).font(.subheadline.weight(.semibold))
            HStack {
                ForEach(1...5, id: \.self) { v in
                    Button {
                        entry.m = entry.m == v ? 0 : v
                    } label: {
                        Text(Journal.moods[v - 1]).font(.system(size: 32))
                            .opacity(entry.m == 0 || entry.m == v ? 1 : 0.35)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(L("journal.level.\(v)"))
                    .accessibilityAddTraits(entry.m == v ? .isSelected : [])
                }
            }
            if showSleep {
                Text(L("journal.sleep")).font(.subheadline.weight(.semibold))
                Picker(L("journal.sleep"), selection: $entry.s) {
                    ForEach(1...5, id: \.self) { Text(L("journal.level.\($0)")).tag($0) }
                }
                .pickerStyle(.segmented)
            }
            TextField(L("journal.intentionHint"), text: $entry.i)
                .textFieldStyle(.roundedBorder)
                .accessibilityLabel(L("journal.intention"))
        }
    }
}

struct JournalView: View {
    @State private var entry = Journal.today()
    @State private var list = Journal.load()
    @State private var confirm = false

    var body: some View {
        List {
            Section {
                JournalQuick(entry: $entry)
                VStack(alignment: .leading, spacing: 4) {
                    Text(L("journal.note")).font(.subheadline.weight(.semibold))
                    TextEditor(text: $entry.n)
                        .frame(minHeight: 90)
                        .scrollContentBackground(.hidden)
                        .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 8))
                }
            } header: {
                Text(Fmt.longDate(Date()))
            }
            Section(L("journal.recent")) {
                recentStrip
            }
            Section(L("journal.history")) {
                let older = list.filter { $0.d != Journal.todayKey }
                if older.isEmpty { Text(L("journal.empty")).foregroundStyle(Theme.muted) }
                ForEach(older.prefix(60), id: \.d) { e in
                    HStack(alignment: .top, spacing: 10) {
                        Text(e.m > 0 ? Journal.moods[e.m - 1] : "·").font(.title2)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(dateText(e.d)).font(.footnote).foregroundStyle(Theme.muted)
                            if !e.i.isEmpty { Text(e.i) }
                            if !e.n.isEmpty { Text(e.n).font(.footnote).foregroundStyle(Theme.muted).lineLimit(3) }
                            if e.s > 0 { Text(T("journal.sleepShort", L("journal.level.\(e.s)"))).font(.caption).foregroundStyle(Theme.muted) }
                        }
                    }
                }
            }
            Section {
                Text(L("journal.privacy")).font(.footnote).foregroundStyle(Theme.muted)
                if !list.isEmpty {
                    Button(L("journal.clear"), role: .destructive) { confirm = true }
                }
            }
        }
        .themedBackground()
        .navigationTitle(L("journal.title"))
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: entry) {
            Journal.put(entry)
            list = Journal.load()
        }
        .confirmationDialog(L("journal.clearConfirm"), isPresented: $confirm, titleVisibility: .visible) {
            Button(L("journal.clear"), role: .destructive) {
                UserDefaults.standard.removeObject(forKey: Journal.key)
                list = []
                entry = JournalEntry(d: Journal.todayKey)
            }
            Button(L("alarm.cancel"), role: .cancel) {}
        }
    }

    /// Son 14 günün ruh hâli (boş gün nokta)
    private var recentStrip: some View {
        let byDay = Dictionary(list.map { ($0.d, $0) }, uniquingKeysWith: { a, _ in a })
        let days = (0..<14).reversed().compactMap { Calendar.current.date(byAdding: .day, value: -$0, to: Date()) }
        return HStack(spacing: 0) {
            ForEach(days, id: \.self) { day in
                let e = byDay[BadgeStore.day(day)]
                VStack(spacing: 2) {
                    Text(e.map { $0.m > 0 ? Journal.moods[$0.m - 1] : "•" } ?? "·").font(.system(size: 18))
                    Text("\(Calendar.current.component(.day, from: day))").font(.caption2).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    private func dateText(_ d: String) -> String {
        let p = d.split(separator: "-").compactMap { Int($0) }
        guard p.count == 3, let date = Calendar(identifier: .gregorian).date(from: DateComponents(year: p[0], month: p[1], day: p[2])) else { return d }
        return Fmt.longDate(date)
    }
}

/// Günaydın kartındaki bölüm: ruh hâli, uyku ve niyet; tamamı günlük sayfasında
struct JournalMorningSection: View {
    @State private var entry = Journal.today()

    var body: some View {
        Section(L("journal.title")) {
            JournalQuick(entry: $entry)
            NavigationLink(L("journal.title")) { JournalView() }
        }
        .onChange(of: entry) { Journal.put(entry) }
    }
}
