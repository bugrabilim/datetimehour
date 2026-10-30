import SwiftUI
import EventKit

/// Toplantı planlayıcı (rakip analizi listesi #85; sitedeki /toplanti-planlayici/ ile aynı kural): bulunduğunuz saat dilimi ve
/// en çok 5 şehir; seçilen günün 24 saati yan yana, mesai (09–18) yeşil, erken/geç (07–09, 18–22) turuncu, gece boş.
/// Satıra dokununca saat seçilir: takvime ekle ya da paylaş. Şehirler `meet.cities` (Android'le aynı).
struct MeetingPlannerView: View {
    @AppStorage("meet.cities") private var keysRaw = ""
    @State private var day = Date()
    @State private var picked: Int?
    @State private var adding = false
    @State private var editing = false

    /// Sütunlar: önce burası (cihazın saat dilimi), sonra seçili şehirler
    private var cities: [City] {
        let keys = keysRaw.isEmpty ? MeetingPlanner.defaultKeys() : keysRaw.split(separator: ",").map(String.init)
        return keys.compactMap { k in City.all.first { $0.key == k } }
    }

    private var zones: [PlannerZone] {
        [PlannerZone(name: L("meet.here"), tz: .current)] + cities.map { PlannerZone(name: $0.displayName, tz: $0.zone) }
    }

    var body: some View {
        let cols = zones
        let start = MeetingPlanner.startOfDay(day)
        List {
            Section {
                DatePicker(L("planner.dateLabel"), selection: $day, displayedComponents: .date)
                    .onChange(of: day) { picked = nil }
                ForEach(cities) { c in
                    HStack {
                        Text(c.displayName)
                        Spacer()
                        Text(c.displayCountry).font(.caption).foregroundStyle(Theme.muted)
                    }
                }
                .onDelete { idx in
                    var list = cities.map(\.key)
                    list.remove(atOffsets: idx)
                    keysRaw = list.joined(separator: ",")
                    picked = nil
                }
                if cities.count < 5 {
                    Button { adding = true } label: { Label(L("planner.addLabel"), systemImage: "plus") }
                }
            }
            if cities.isEmpty {
                Text(L("planner.minCities")).foregroundStyle(Theme.muted)
            } else {
                Section {
                    Text(commonText(cols, start)).font(.footnote)
                    legend
                } footer: {
                    Text(L("meet.pickHint"))
                }
                Section {
                    header(cols)
                    ForEach(0..<24, id: \.self) { h in
                        let at = start.addingTimeInterval(Double(h) * 3600)
                        Button { picked = picked == h ? nil : h } label: { row(cols, at, selected: picked == h) }
                            .buttonStyle(.plain)
                            .listRowInsets(EdgeInsets(top: 0, leading: 8, bottom: 0, trailing: 8))
                    }
                }
                if let h = picked {
                    let at = start.addingTimeInterval(Double(h) * 3600)
                    Section {
                        Text(T("planner.picked", MeetingPlanner.hm(at, TimeZone.current), L("meet.here"))).font(.headline)
                        Button { editing = true } label: { Label(L("calendar.addToCalendar"), systemImage: "calendar.badge.plus") }
                        ShareLink(item: MeetingPlanner.summary(cols, at)) { Label(L("share.card"), systemImage: "square.and.arrow.up") }
                    }
                }
            }
        }
        .themedBackground()
        .navigationTitle(L("planner.h1"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { EditButton() }
        .sheet(isPresented: $adding) {
            NavigationStack {
                List(City.all.filter { c in !cities.contains(c) }) { c in
                    Button {
                        keysRaw = (cities.map(\.key) + [c.key]).joined(separator: ",")
                        adding = false
                    } label: {
                        VStack(alignment: .leading) {
                            Text(c.displayName).foregroundStyle(Theme.text)
                            Text(c.displayCountry).font(.caption).foregroundStyle(Theme.muted)
                        }
                    }
                }
                .themedBackground()
                .navigationTitle(L("planner.addLabel"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar { Button(L("alarm.cancel")) { adding = false } }
            }
        }
        .sheet(isPresented: $editing) {
            if let h = picked {
                let at = start.addingTimeInterval(Double(h) * 3600)
                EventEditor(configure: { e in
                    e.title = L("planner.meeting")
                    e.startDate = at
                    e.endDate = at.addingTimeInterval(3600)
                    e.notes = MeetingPlanner.summary(cols, at)
                }, onDone: { editing = false })
                .ignoresSafeArea()
            }
        }
    }

    private func header(_ cols: [PlannerZone]) -> some View {
        HStack(spacing: 4) {
            ForEach(cols.indices, id: \.self) { i in
                Text(cols[i].name).font(.caption.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
                    .frame(maxWidth: .infinity)
            }
        }
        .listRowInsets(EdgeInsets(top: 6, leading: 8, bottom: 6, trailing: 8))
    }

    private func row(_ cols: [PlannerZone], _ at: Date, selected: Bool) -> some View {
        HStack(spacing: 4) {
            ForEach(cols.indices, id: \.self) { i in
                let tz = cols[i].tz
                let kind = MeetingPlanner.kind(at, tz)
                let shift = MeetingPlanner.dayShift(at, tz)
                Text(MeetingPlanner.hm(at, tz) + (shift == 0 ? "" : shift > 0 ? " +1" : " −1"))
                    .font(.footnote.monospacedDigit())
                    .frame(maxWidth: .infinity, minHeight: 30)
                    .background(MeetingPlanner.color(kind), in: RoundedRectangle(cornerRadius: 6))
            }
        }
        .padding(.vertical, 2)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(selected ? Theme.accent : .clear, lineWidth: 2))
        .environment(\.layoutDirection, .leftToRight)
        .contentShape(Rectangle())
    }

    private var legend: some View {
        HStack(spacing: 12) {
            ForEach(0..<3, id: \.self) { k in
                HStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 3).fill(MeetingPlanner.color(k)).frame(width: 12, height: 12)
                        .overlay(RoundedRectangle(cornerRadius: 3).stroke(Theme.border))
                    Text(L(["planner.legendWork", "planner.legendEdge", "planner.legendNight"][k])).font(.caption2).foregroundStyle(Theme.muted)
                }
            }
        }
    }

    private func commonText(_ cols: [PlannerZone], _ start: Date) -> String {
        let hours = (0..<24).filter { h in cols.allSatisfy { MeetingPlanner.kind(start.addingTimeInterval(Double(h) * 3600), $0.tz) == 0 } }
        guard !hours.isEmpty else { return L("planner.noCommon") }
        let text = hours.map { MeetingPlanner.hm(start.addingTimeInterval(Double($0) * 3600), TimeZone.current) }.joined(separator: ", ")
        return T("planner.common", L("meet.here"), text)
    }
}

struct PlannerZone {
    let name: String
    let tz: TimeZone
}

enum MeetingPlanner {
    /// Varsayılan: dünya saatleri listesinden burayla aynı dilimde olmayan ilk iki şehir
    static func defaultKeys() -> [String] {
        let world = (UserDefaults.standard.string(forKey: "world") ?? City.defaultKeys).split(separator: ",").map(String.init)
        let now = Date()
        let here = TimeZone.current.secondsFromGMT(for: now)
        return Array(world.filter { k in City.all.first { $0.key == k }.map { $0.zone.secondsFromGMT(for: now) != here } ?? false }.prefix(2))
    }

    static func startOfDay(_ d: Date) -> Date { Calendar.current.startOfDay(for: d) }

    static func hm(_ d: Date, _ tz: TimeZone) -> String {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = tz
        let p = c.dateComponents([.hour, .minute], from: d)
        return String(format: "%02d:%02d", p.hour ?? 0, p.minute ?? 0)
    }

    /// 0 mesai (başlangıç 09:00–17:00), 1 erken/geç (07–09, 18–22), 2 gece
    static func kind(_ d: Date, _ tz: TimeZone) -> Int {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = tz
        let p = c.dateComponents([.hour, .minute], from: d)
        let m = (p.hour ?? 0) * 60 + (p.minute ?? 0)
        if m >= 540 && m <= 1020 { return 0 }
        if m >= 420 && m < 1320 { return 1 }
        return 2
    }

    /// Şehirdeki gün buradaki güne göre: −1, 0, +1
    static func dayShift(_ d: Date, _ tz: TimeZone) -> Int {
        var a = Calendar(identifier: .gregorian), b = a
        a.timeZone = .current
        b.timeZone = tz
        let x = a.dateComponents([.year, .month, .day], from: d), y = b.dateComponents([.year, .month, .day], from: d)
        let dx = (x.year ?? 0) * 400 + (x.month ?? 0) * 32 + (x.day ?? 0)
        let dy = (y.year ?? 0) * 400 + (y.month ?? 0) * 32 + (y.day ?? 0)
        return dy == dx ? 0 : dy > dx ? 1 : -1
    }

    static func color(_ kind: Int) -> Color {
        switch kind {
        case 0: Color.green.opacity(0.28)
        case 1: Color.orange.opacity(0.2)
        default: Color.clear
        }
    }

    /// Paylaşım ve takvim notu: "Toplantı · 30 Ekim 2026" + her şehrin saati
    static func summary(_ cols: [PlannerZone], _ at: Date) -> String {
        ([L("planner.meeting") + " · " + Fmt.longDate(at)] + cols.map { "\($0.name): \(hm(at, $0.tz))" }).joined(separator: "\n")
    }
}
