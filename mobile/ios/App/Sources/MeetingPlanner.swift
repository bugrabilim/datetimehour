import SwiftUI
import SaatCore
import EventKit

/// Toplantı planlayıcı (rakip analizi listesi #85; sitedeki /toplanti-planlayici/ ile aynı kural): bulunduğunuz saat dilimi ve
/// en çok 5 şehir; seçilen günün 24 saati yan yana, mesai (09–18) yeşil, erken/geç (07–09, 18–22) turuncu, gece boş.
/// Satıra dokununca saat seçilir: takvime ekle ya da paylaş. Şehirler `meet.cities` (Android'le aynı).
struct MeetingPlannerView: View {
    @AppStorage("meet.cities") private var keysRaw = ""
    @AppStorage("meet.h12") private var h12Raw = ""
    @AppStorage("meet.exchanges") private var showEx = false
    @State private var day = Date()
    /// Seçili saat aralığı (burası saatiyle, ilk ve son saat dahil)
    @State private var picked: ClosedRange<Int>?
    @State private var adding = false
    @State private var editing = false
    @State private var device = DeviceCalendar.shared

    /// Sütunlar: önce burası (cihazın saat dilimi), sonra seçili şehirler
    private var cities: [City] {
        let keys = keysRaw.isEmpty ? MeetingPlanner.defaultKeys() : keysRaw.split(separator: ",").map(String.init)
        return keys.compactMap { k in City.all.first { $0.key == k } }
    }

    private var zones: [PlannerZone] {
        [PlannerZone(id: "here", name: L("meet.here"), tz: .current)] + cities.map { PlannerZone(id: $0.key, name: $0.displayName, tz: $0.zone) }
    }

    private var h12: Set<String> { Set(h12Raw.split(separator: ",").map(String.init)) }

    /// Kendi takvimindeki etkinliklerin kapladığı saatler (izin verilmişse; tüm gün etkinlikler hariç)
    private func busy(_ start: Date) -> Set<Int> {
        guard device.authorized else { return [] }
        let d = CalendarDay(start)
        let events = device.events(month: d)[d.dayNumber] ?? []
        var out = Set<Int>()
        for e in events where !e.isAllDay {
            for h in 0..<24 {
                let a = start.addingTimeInterval(Double(h) * 3600), b = a.addingTimeInterval(3600)
                if e.startDate < b && e.endDate > a { out.insert(h) }
            }
        }
        return out
    }

    var body: some View {
        let cols = zones
        let start = MeetingPlanner.startOfDay(day)
        let busyHours = busy(start)
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
                    legend(busy: !busyHours.isEmpty)
                } footer: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L("meet.rangeHint"))
                        if !device.authorized { Text(L("meet.calendarHint")) }
                    }
                }
                Section {
                    header(cols)
                    ForEach(0..<24, id: \.self) { h in
                        let at = start.addingTimeInterval(Double(h) * 3600)
                        Button { tap(h) } label: { row(cols, at, selected: picked?.contains(h) == true, busy: busyHours.contains(h)) }
                            .buttonStyle(.plain)
                            .listRowInsets(EdgeInsets(top: 0, leading: 8, bottom: 0, trailing: 8))
                    }
                }
                Section {
                    Toggle(L("meet.exchanges"), isOn: $showEx).tint(Theme.accent)
                    if showEx {
                        exchangeStrip(start)
                    }
                } footer: {
                    if showEx { Text(L("meet.exchangesNote")) }
                }
                if let r = picked {
                    let a = start.addingTimeInterval(Double(r.lowerBound) * 3600)
                    let b = start.addingTimeInterval(Double(r.upperBound + 1) * 3600)
                    Section {
                        Text(T("planner.picked", MeetingPlanner.hm(a, TimeZone.current) + "–" + MeetingPlanner.hm(b, TimeZone.current), L("meet.here"))).font(.headline)
                        if showEx {
                            let open = MeetingPlanner.exchanges.filter { e in r.contains { MeetingPlanner.openInHour(e, start.addingTimeInterval(Double($0) * 3600)) } }.map(\.code)
                            Text(open.isEmpty ? L("meet.noneOpen") : T("meet.openAt", open.joined(separator: ", "))).font(.footnote).foregroundStyle(Theme.muted)
                        }
                        Button { editing = true } label: { Label(L("calendar.addToCalendar"), systemImage: "calendar.badge.plus") }
                        ShareLink(item: MeetingPlanner.summary(cols, a, end: b)) { Label(L("share.card"), systemImage: "square.and.arrow.up") }
                    }
                }
            }
        }
        .themedBackground()
        .navigationTitle(L("planner.h1"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar { EditButton() }
        .onAppear { device.reload() }
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
                .navigationBarTitleDisplayMode(.large)
                .toolbar { Button(L("alarm.cancel")) { adding = false } }
            }
        }
        .sheet(isPresented: $editing) {
            if let r = picked {
                let a = start.addingTimeInterval(Double(r.lowerBound) * 3600)
                let b = start.addingTimeInterval(Double(r.upperBound + 1) * 3600)
                EventEditor(configure: { e in
                    e.title = L("planner.meeting")
                    e.startDate = a
                    e.endDate = b
                    e.notes = MeetingPlanner.summary(cols, a, end: b)
                }, onDone: { editing = false })
                .ignoresSafeArea()
            }
        }
    }

    /// Dokunma: ilk dokunuş tek saat, ikincisi aralık, üçüncüsü yeniden başlatır; aynı saate dokunmak seçimi kaldırır
    private func tap(_ h: Int) {
        guard let p = picked else { picked = h...h; return }
        if p.lowerBound == p.upperBound {
            picked = h == p.lowerBound ? nil : min(h, p.lowerBound)...max(h, p.lowerBound)
        } else {
            picked = h...h
        }
    }

    private func header(_ cols: [PlannerZone]) -> some View {
        HStack(spacing: 4) {
            ForEach(cols.indices, id: \.self) { i in
                Button {
                    var s = h12
                    if s.contains(cols[i].id) { s.remove(cols[i].id) } else { s.insert(cols[i].id) }
                    h12Raw = s.sorted().joined(separator: ",")
                } label: {
                    Text(cols[i].name + (h12.contains(cols[i].id) ? " ·12" : "")).font(.caption.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
        }
        .listRowInsets(EdgeInsets(top: 6, leading: 8, bottom: 6, trailing: 8))
    }

    private func row(_ cols: [PlannerZone], _ at: Date, selected: Bool, busy: Bool) -> some View {
        HStack(spacing: 4) {
            ForEach(cols.indices, id: \.self) { i in
                let tz = cols[i].tz
                let kind = MeetingPlanner.kind(at, tz)
                let shift = MeetingPlanner.dayShift(at, tz)
                let bg = MeetingPlanner.weekend(at, tz) && kind != 2 ? MeetingPlanner.weekendColor : MeetingPlanner.color(kind)
                Text((i == 0 && busy ? "● " : "") + MeetingPlanner.hm(at, tz, h12: h12.contains(cols[i].id)) + (shift == 0 ? "" : shift > 0 ? " +1" : " −1"))
                    .font(.footnote.monospacedDigit())
                    .lineLimit(1).minimumScaleFactor(0.7)
                    .foregroundStyle(i == 0 && busy ? Theme.accent : Theme.text)
                    .frame(maxWidth: .infinity, minHeight: 30)
                    .background(bg, in: RoundedRectangle(cornerRadius: 6))
            }
        }
        .padding(.vertical, 2)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(selected ? Theme.accent : .clear, lineWidth: 2))
        .environment(\.layoutDirection, .leftToRight)
        .contentShape(Rectangle())
    }

    /// Borsa saatleri: her borsa için burası saatiyle 24 hücre
    private func exchangeStrip(_ start: Date) -> some View {
        VStack(spacing: 3) {
            ForEach(MeetingPlanner.exchanges, id: \.code) { e in
                HStack(spacing: 6) {
                    Text(e.code).font(.caption.weight(.semibold)).frame(width: 60, alignment: .leading)
                    HStack(spacing: 1) {
                        ForEach(0..<24, id: \.self) { h in
                            RoundedRectangle(cornerRadius: 2)
                                .fill(MeetingPlanner.openInHour(e, start.addingTimeInterval(Double(h) * 3600)) ? Theme.accent.opacity(0.75) : Theme.surface2)
                                .frame(height: 14)
                        }
                    }
                }
            }
            HStack(spacing: 0) {
                Color.clear.frame(width: 66, height: 1)
                ForEach([0, 6, 12, 18], id: \.self) { h in
                    Text(String(format: "%02d", h)).font(.system(size: 10)).foregroundStyle(Theme.muted).frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .environment(\.layoutDirection, .leftToRight)
    }

    private func legend(busy: Bool) -> some View {
        let items: [(Color, String)] = [(MeetingPlanner.color(0), "planner.legendWork"), (MeetingPlanner.color(1), "planner.legendEdge"),
                                        (MeetingPlanner.color(2), "planner.legendNight"), (MeetingPlanner.weekendColor, "meet.weekend")]
        return VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 12) {
                ForEach(items.indices, id: \.self) { k in
                    HStack(spacing: 4) {
                        RoundedRectangle(cornerRadius: 3).fill(items[k].0).frame(width: 12, height: 12)
                            .overlay(RoundedRectangle(cornerRadius: 3).stroke(Theme.border))
                        Text(L(items[k].1)).font(.caption2).foregroundStyle(Theme.muted)
                    }
                }
            }
            if busy { Text("● " + L("meet.busy")).font(.caption2).foregroundStyle(Theme.accent) }
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
    var id = ""
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

    /// Paylaşım ve takvim notu: "Toplantı · 30 Ekim 2026" + her şehrin saati (aralıkta bitişiyle)
    static func summary(_ cols: [PlannerZone], _ at: Date, end: Date? = nil) -> String {
        let rows = cols.map { (z: PlannerZone) -> String in
            var line = z.name + ": " + hm(at, z.tz)
            if let end { line += "–" + hm(end, z.tz) }
            return line
        }
        return ([L("planner.meeting") + " · " + Fmt.longDate(at)] + rows).joined(separator: "\n")
    }

    // 2. analiz #72: hafta sonu, borsa saatleri, sütun başına 12/24 saat
    static let weekendColor = Color.gray.opacity(0.22)

    static func weekend(_ d: Date, _ tz: TimeZone) -> Bool {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = tz
        let w = c.component(.weekday, from: d)  // 1 pazar, 7 cumartesi
        return w == 1 || w == 7
    }

    static func hm(_ d: Date, _ tz: TimeZone, h12: Bool) -> String {
        guard h12 else { return hm(d, tz) }
        var c = Calendar(identifier: .gregorian)
        c.timeZone = tz
        let p = c.dateComponents([.hour, .minute], from: d)
        let h = p.hour ?? 0
        return String(format: "%d:%02d %@", h % 12 == 0 ? 12 : h % 12, p.minute ?? 0, h < 12 ? "AM" : "PM")
    }

    /// Borsa: kısa ad, saat dilimi, seanslar (yerel dakika [başlangıç, bitiş)), işlem günleri (Calendar weekday: 1 pazar … 7 cumartesi)
    struct Exchange { let code: String; let tz: TimeZone; let sessions: [(Int, Int)]; var days: Set<Int> = [2, 3, 4, 5, 6] }
    static let exchanges: [Exchange] = [
        Exchange(code: "BIST", tz: TimeZone(identifier: "Europe/Istanbul")!, sessions: [(600, 1080)]),
        Exchange(code: "LSE", tz: TimeZone(identifier: "Europe/London")!, sessions: [(480, 990)]),
        Exchange(code: "Xetra", tz: TimeZone(identifier: "Europe/Berlin")!, sessions: [(540, 1050)]),
        Exchange(code: "NYSE", tz: TimeZone(identifier: "America/New_York")!, sessions: [(570, 960)]),
        Exchange(code: "TSE", tz: TimeZone(identifier: "Asia/Tokyo")!, sessions: [(540, 690), (750, 930)]),
        Exchange(code: "HKEX", tz: TimeZone(identifier: "Asia/Hong_Kong")!, sessions: [(570, 720), (780, 960)]),
        Exchange(code: "SSE", tz: TimeZone(identifier: "Asia/Shanghai")!, sessions: [(570, 690), (780, 900)]),
        Exchange(code: "Tadawul", tz: TimeZone(identifier: "Asia/Riyadh")!, sessions: [(600, 900)], days: [1, 2, 3, 4, 5]),
    ]

    static func isOpen(_ e: Exchange, _ d: Date) -> Bool {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = e.tz
        let p = c.dateComponents([.weekday, .hour, .minute], from: d)
        guard e.days.contains(p.weekday ?? 0) else { return false }
        let m = (p.hour ?? 0) * 60 + (p.minute ?? 0)
        return e.sessions.contains { m >= $0.0 && m < $0.1 }
    }

    /// Saat diliminde (çeyrek saatlik adımla) en az bir an açık mı
    static func openInHour(_ e: Exchange, _ start: Date) -> Bool {
        stride(from: 0, to: 60, by: 15).contains { isOpen(e, start.addingTimeInterval(Double($0) * 60)) }
    }
}
