import SwiftUI
import EventKit
import WeatherKit
import SaatCore

/// Takvim sekmesi (Apple Takvim'e benzer): ay ızgarası, seçili günün özel günleri (resmî tatil, arife, dini ve
/// özel günler, Ramazan, okul tatili) ve telefondaki takvimlerin etkinlikleri. "+" ile etkinlik eklenir (sistem
/// düzenleyicisi, istenen takvime), etkinliğe dokununca ayrıntı/düzenleme, sola kaydırınca silme.
struct CalendarTab: View {
    @State private var month = CalendarDay(Date()).firstOfMonth
    @State private var selected = CalendarDay(Date())
    @State private var adding: AddRequest?
    @State private var showing: EKEvent?
    @State private var deleting: EKEvent?
    @State private var googleGuide = false
    @State private var router = Router.shared
    @State private var device = DeviceCalendar.shared
    private let data = CalendarData.shared
    @AppStorage("cal.weekNo") private var showWeek = true
    @AppStorage("cal.moon") private var showMoon = true
    @AppStorage("cal.dst") private var showDst = true
    @AppStorage("cal.hijri") private var showHijri = false
    @AppStorage("cal.names") private var showNames = false
    /// 2. analiz #116, #119, #122: görünüm, hicri aylarla gezme, ülke
    @AppStorage(CalPrefs.modeKey) private var mode = "month"
    @AppStorage(CalPrefs.hijriNavKey) private var hijriNav = false
    // Ülke ve türler yalnız seçilince yazılır (CalPrefs: yoksa bölge ve Vakitler'e bağlı varsayılan); burada değişince yenilensin diye
    @AppStorage(CalPrefs.countryKey) private var countryRaw = ""
    @AppStorage(CalPrefs.hiddenKey) private var hiddenRaw: String?
    @AppStorage(Features.key) private var featuresRaw = ""
    /// Kapsayıcı takvimlerin seçimi (değişince takvim yenilensin)
    @AppStorage(ExtraCals.key) private var extraRaw: String?
    private var country: String { CalPrefs.country }
    @State private var countrySheet = false
    @State private var leafOpen = false

    /// Yeni etkinlik: boş (seçili gün 09:00) ya da bir özel günden (tüm gün)
    struct AddRequest: Identifiable {
        let id = UUID()
        let day: CalendarDay
        var title: String?
    }

    var body: some View {
        let today = CalendarDay(Date())
        let deviceEvents = device.events(month: month)
        NavigationStack {
            List {
                Section {
                    // Görünüm: ay, hafta, gün, ajanda, yıl (2. analiz #116)
                    CalModeBar()
                        .listRowInsets(EdgeInsets(top: 0, leading: 8, bottom: 4, trailing: 8))
                        .listRowSeparator(.hidden)
                    modeContent(today: today, deviceEvents: deviceEvents)
                        .gesture(DragGesture(minimumDistance: 30).onEnded { v in
                            if abs(v.translation.width) > abs(v.translation.height) {
                                let forward = (v.translation.width < 0) != AppLocale.isRTL
                                step(forward ? 1 : -1)
                            }
                        })
                        .listRowInsets(EdgeInsets(top: 4, leading: 8, bottom: 8, trailing: 8))
                        .listRowSeparator(.hidden)
                } header: {
                    header
                }
                if mode == "month" { dayList(today: today, deviceEvents: deviceEvents[selected.dayNumber] ?? []) }
            }
            .themedBackground()
            .listStyle(.plain)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(L("client.today")) { go(to: today) }
                }
                // Başlıkta az simge (doğal tasarım, madde 13): takvim yaprağı "…" menüsünde
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button { leafOpen = true } label: { Label(L("leaf.title"), systemImage: "doc.text.image") }
                        Divider()
                        Toggle(L("cal.weekNo"), isOn: $showWeek)
                        Toggle(L("cal.moon"), isOn: $showMoon)
                        Toggle(L("cal.dst"), isOn: $showDst)
                        Toggle(L("cal.hijriDays"), isOn: $showHijri)
                        Toggle(L("cal.names"), isOn: $showNames)
                        Toggle(L("cal.hijriNav"), isOn: $hijriNav)
                        // Takvimler ve ülke (kapsayıcı takvimler, 9 Kasım 2026): altında seçili ülke
                        Button { countrySheet = true } label: {
                            Text(L("calendars.title"))
                            Text(WorldHol.countryName(country))
                        }
                    } label: { Image(systemName: "ellipsis.circle") }
                        .accessibilityLabel(L("cal.view"))
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { adding = AddRequest(day: selected) } label: { Image(systemName: "plus") }
                        .accessibilityLabel(L("calendar.add"))
                }
            }
            .navigationDestination(isPresented: $leafOpen) { CalendarLeafView() }
            .sheet(item: $adding) { req in
                EventEditor(configure: { e in
                    if let title = req.title {
                        e.title = title
                        e.isAllDay = true
                        e.startDate = req.day.noon(in: .current)
                        e.endDate = req.day.noon(in: .current)
                    } else {
                        let start = req.day.noon(in: .current).addingTimeInterval(-3 * 3600)
                        e.startDate = start
                        e.endDate = start.addingTimeInterval(3600)
                    }
                }, onDone: {
                    adding = nil
                    device.reload()
                })
                .ignoresSafeArea()
            }
            .sheet(item: $showing) { e in
                EventDetail(event: e) {
                    showing = nil
                    device.reload()
                }
                .ignoresSafeArea()
            }
            .confirmationDialog(L("calendar.deleteConfirm"), isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }),
                                titleVisibility: .visible) {
                Button(L("calendar.delete"), role: .destructive) {
                    if let e = deleting { device.remove(e) }
                    deleting = nil
                }
            }
            .sheet(isPresented: $countrySheet) { CalCountrySheet() }
            .sheet(isPresented: $googleGuide) {
                NavigationStack { GoogleCalendarGuide() }
                    .onDisappear { device.reload() }
            }
            .onChange(of: router.calendarDay, initial: true) { _, day in
                guard let day else { return }
                go(to: day)
                router.calendarDay = nil
            }
        }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(L("calendar.months.\(month.month - 1)"))
                .font(AppFonts.page)
                .foregroundStyle(Theme.text)
                .lineLimit(1).minimumScaleFactor(0.6)
            Text(String(month.year))
                .lineLimit(1).fixedSize()
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.accent)
            Spacer()
            Button { step(-1) } label: { Image(systemName: "chevron.backward") }
                .accessibilityLabel(L("prayer.prevMonth"))
                .padding(.trailing, 14)
            Button { step(1) } label: { Image(systemName: "chevron.forward") }
                .accessibilityLabel(L("prayer.nextMonth"))
        }
        .font(.title3.weight(.semibold))
        .foregroundStyle(Theme.accent)
        .buttonStyle(.borderless)
        .textCase(nil)
        .padding(.vertical, 4)
    }

    private func move(_ d: Int) {
        withAnimation {
            month = month.adding(months: d)
            let today = CalendarDay(Date())
            selected = today.firstOfMonth == month ? today : month
        }
    }

    /// Önceki/sonraki: görünüme göre gün, hafta, ay (hicri ay), yıl
    private func step(_ d: Int) {
        switch mode {
        case "week": go(to: selected.adding(days: 7 * d))
        case "day": go(to: selected.adding(days: d))
        case "year": go(to: selected.adding(months: 12 * d))
        default:
            if hijriNav { moveHijri(d) } else { move(d) }
        }
    }

    /// Hicri aylarla gezme (2. analiz #119)
    private func moveHijri(_ d: Int) {
        guard let h = Hijri.date(year: selected.year, month: selected.month, day: selected.day) else { return }
        var y = h.year, m = h.month + d
        if m < 1 { m = 12; y -= 1 } else if m > 12 { m = 1; y += 1 }
        guard let first = Hijri.month(year: y, month: m)?.first else { return }
        go(to: CalendarDay(first.0, first.1, first.2))
    }

    private func pick(_ d: CalendarDay) {
        selected = d
        month = d.firstOfMonth
        BadgeStore.shared.hit(.takvim)
    }

    @ViewBuilder
    private func modeContent(today: CalendarDay, deviceEvents: [Int: [EKEvent]]) -> some View {
        switch mode {
        case "week":
            WeekListView(selected: selected, today: today) { pick($0); mode = "day" }
        case "day":
            VStack(alignment: .leading, spacing: 8) {
                Text(Fmt.day(selected)).font(.headline)
                DayTimelineView(day: selected)
            }
        case "agenda":
            AgendaListView(from: selected) { pick($0); mode = "day" }
        case "year":
            YearHeatmapView(year: selected.year, today: today, deviceCounts: yearCounts(selected.year)) { m in
                pick(m)
                mode = "month"
            }
        default:
            monthContent(today: today, deviceEvents: deviceEvents)
        }
    }

    @ViewBuilder
    private func monthContent(today: CalendarDay, deviceEvents: [Int: [EKEvent]]) -> some View {
        if hijriNav, let h = Hijri.date(year: selected.year, month: selected.month, day: selected.day) {
            VStack(alignment: .leading, spacing: 6) {
                Text(h.monthName + " " + String(h.year)).font(.headline).foregroundStyle(Theme.accent)
                HijriMonthGridView(year: h.year, month: h.month, today: today, selected: selected) { pick($0) }
            }
        } else {
            MonthGrid(month: month, today: today, selected: selected, deviceEvents: deviceEvents) { pick($0) }
        }
    }

    private func yearCounts(_ y: Int) -> [Int: Int] {
        var out: [Int: Int] = [:]
        for m in 1...12 {
            for (k, v) in device.events(month: CalendarDay(y, m, 1)) { out[k] = v.count }
        }
        return out
    }

    private func go(to day: CalendarDay) {
        withAnimation {
            month = day.firstOfMonth
            selected = day
        }
    }

    @ViewBuilder
    private func dayList(today: CalendarDay, deviceEvents: [EKEvent]) -> some View {
        let special = calEvents(selected)
        Section {
            if Features.isOn(.weather) { DayWeatherRow(day: selected) }
            ForEach(special, id: \.self) { e in
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 2).fill(e.color).frame(width: 4, height: 34)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.name).foregroundStyle(Theme.text)
                        Text(calendarTypeLabel(e.type)).font(.caption).foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Text(L("calendar.allDay")).font(.caption).foregroundStyle(Theme.muted)
                }
                .swipeActions(edge: .trailing) {
                    Button { adding = AddRequest(day: selected, title: e.name) } label: {
                        Label(L("calendar.addToCalendar"), systemImage: "calendar.badge.plus")
                    }
                    .tint(Theme.accent)
                }
                .contextMenu {
                    Button { adding = AddRequest(day: selected, title: e.name) } label: {
                        Label(L("calendar.addToCalendar"), systemImage: "calendar.badge.plus")
                    }
                }
            }
            if CalPrefs.religious && data.isRamadan(selected) {
                Label(L("calendar.types.ramadan"), systemImage: "moon.stars")
                    .foregroundStyle(CalendarData.color("religious"))
            }
            if CalPrefs.country == "TR" && !CalPrefs.hidden.contains("school") && data.isSchoolBreak(selected) && !special.contains(where: { $0.type == "school" }) {
                Label(L("calendar.types.school"), systemImage: "graduationcap")
                    .foregroundStyle(CalendarData.color("school"))
            }
            ForEach(deviceEvents) { e in
                Button { showing = e } label: { DeviceEventRow(event: e) }
                    .swipeActions(edge: .trailing) {
                        if e.calendar?.allowsContentModifications == true {
                            Button(role: .destructive) { deleting = e } label: {
                                Label(L("calendar.delete"), systemImage: "trash")
                            }
                        }
                    }
            }
            if special.isEmpty && deviceEvents.isEmpty && !(CalPrefs.religious && data.isRamadan(selected)) {
                Text(L("calendar.noDayEvents")).foregroundStyle(Theme.muted)
            }
            if !device.authorized && device.canAsk {
                Button {
                    Task { await device.requestAccess() }
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Label(L("calendar.showDevice"), systemImage: "calendar.badge.checkmark")
                            .font(.headline)
                        Text(L("calendar.showDeviceNote")).font(.caption).foregroundStyle(Theme.muted)
                    }
                }
                .tint(Theme.accent)
            }
        } header: {
            VStack(alignment: .leading, spacing: 2) {
                Text(Fmt.day(selected)).font(.headline).foregroundStyle(Theme.text)
                Text([relative(selected, today), T("info.week.big", selected.isoWeek.week), T("calendar.dayOfYear", selected.dayOfYear)]
                        .joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(Theme.muted)
                // Hicri tarih (Diyanet takvimi); dini günler görünürken
                if CalPrefs.religious, let h = Hijri.date(year: selected.year, month: selected.month, day: selected.day) {
                    Label(h.text, systemImage: "moon")
                        .font(.caption)
                        .foregroundStyle(Theme.accent)
                }
                // Açık kapsayıcı takvimlerin ikinci tarihi (her takvim tek satır, takvimin rengiyle nokta)
                ExtraCalDayLines(day: selected)
            }
            .textCase(nil)
            .padding(.vertical, 4)
        } footer: {
            VStack(alignment: .leading, spacing: 8) {
                legend
                Button {
                    googleGuide = true
                } label: {
                    Label(L("google.button"), systemImage: "link.badge.plus")
                        .font(.subheadline.weight(.semibold))
                }
                .tint(Theme.accent)
                .padding(.top, 4)
                if let y = data.years[month.year], y.projected {
                    Text(L("calendar.projected")).font(.footnote).foregroundStyle(Theme.muted)
                }
                if CalPrefs.religious { Text(L("hijri.note")).font(.footnote).foregroundStyle(Theme.muted) }
            }
            .padding(.top, 8)
        }
    }

    private var legend: some View {
        let types = ["holiday", "half", "religious", "special", "school"] + ExtraCals.sorted().filter { ExtraCals.isOn($0.id) }.map { "cal:\($0.id)" }
        return FlowRow(items: types) { t in
            HStack(spacing: 4) {
                Circle().fill(CalendarData.color(t)).frame(width: 8, height: 8)
                Text(calendarTypeLabel(t)).font(.caption2).foregroundStyle(Theme.muted)
            }
        }
    }
}

/// Telefondaki bir etkinlik: saat sütunu, takvim rengi, başlık
private struct DeviceEventRow: View {
    let event: EKEvent

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .trailing, spacing: 2) {
                if event.isAllDay {
                    Text(L("calendar.allDay")).font(.caption)
                } else {
                    Text(Fmt.time(event.startDate)).font(.caption.weight(.semibold))
                    Text(Fmt.time(event.endDate)).font(.caption).foregroundStyle(Theme.muted)
                }
            }
            .monospacedDigit()
            .frame(width: 54, alignment: .trailing)
            .environment(\.layoutDirection, .leftToRight)
            RoundedRectangle(cornerRadius: 2)
                .fill(Color(cgColor: event.calendar?.cgColor ?? CGColor(gray: 0.5, alpha: 1)))
                .frame(width: 4, height: 34)
            VStack(alignment: .leading, spacing: 2) {
                Text(event.title ?? "").foregroundStyle(Theme.text).lineLimit(2)
                if let loc = event.location, !loc.isEmpty {
                    Text(loc).font(.caption).foregroundStyle(Theme.muted).lineLimit(1)
                } else if let cal = event.calendar?.title {
                    Text(cal).font(.caption).foregroundStyle(Theme.muted).lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

/// "Bugün", "5 gün kaldı", "3 gün önce"
func relative(_ d: CalendarDay, _ today: CalendarDay) -> String {
    let n = DateCalc.days(from: today, to: d)
    if n == 0 { return L("client.today") }
    if n == 1 { return L("client.tomorrow") }
    return n > 0 ? T("client.daysLeft", num(n)) : T("calendar.daysAgo", num(-n))
}

extension CalendarDay: Identifiable {
    public var id: Int { dayNumber }
}

/// Ay ızgarası (Apple Takvim gibi): pazartesi başlar, haftalar ince çizgiyle ayrılır; bugün renkli daire,
/// seçili gün koyu daire; altında noktalar (özel gün rengi, telefondaki etkinlikler gri). Ramazan günleri hafif yeşil.
struct MonthGrid: View {
    let month: CalendarDay
    let today: CalendarDay
    let selected: CalendarDay
    var deviceEvents: [Int: [EKEvent]] = [:]
    let onSelect: (CalendarDay) -> Void
    private let data = CalendarData.shared
    // Görünüm seçenekleri (2. analiz #123, #119, #117): Takvim › Görünüm menüsü
    @AppStorage("cal.weekNo") private var showWeek = true
    @AppStorage("cal.moon") private var showMoon = true
    @AppStorage("cal.dst") private var showDst = true
    @AppStorage("cal.hijri") private var showHijri = false
    @AppStorage("cal.names") private var showNames = false
    /// Kapsayıcı takvimlerin seçimi (değişince hücreler yenilensin)
    @AppStorage(ExtraCals.key) private var extraRaw: String?

    /// Bu ayın yeni ay ve dolunay günleri (telefonun saat diliminde): gün numarası → emoji
    private var moonMarks: [Int: String] {
        guard showMoon else { return [:] }
        let start = month.firstOfMonth.noon(in: .current).timeIntervalSince1970 * 1000
        let k0 = Int((start / 86400000 + 2440587.5 - 2451550.09766) / 29.530588861) - 1
        var out: [Int: String] = [:]
        for k in k0...(k0 + 3) {
            for full in [false, true] {
                let d = CalendarDay(Date(timeIntervalSince1970: Double(Nature.phaseMilliseconds(k: k, full: full)) / 1000))
                if d.year == month.year && d.month == month.month { out[d.dayNumber] = full ? "🌕" : "🌑" }
            }
        }
        return out
    }
    /// Telefonun saat diliminde yaz saati değişimi olan günler
    private var dstDays: Set<Int> {
        guard showDst else { return [] }
        let start = month.firstOfMonth.noon(in: .current).addingTimeInterval(-13 * 3600)
        let end = start.addingTimeInterval(Double(CalendarDay.daysIn(month.year, month.month)) * 86400)
        var out = Set<Int>()
        var t = start
        while let n = TimeZone.current.nextDaylightSavingTimeTransition(after: t), n < end {
            out.insert(CalendarDay(n).dayNumber)
            t = n
        }
        return out
    }

    var body: some View {
        let first = month.firstOfMonth
        let count = CalendarDay.daysIn(month.year, month.month)
        let cells: [CalendarDay?] = Array(repeating: nil, count: first.weekday) + (0..<count).map { first.adding(days: $0) }
        let weeks = stride(from: 0, to: cells.count, by: 7).map { Array(cells[$0..<min($0 + 7, cells.count)]) }
        let moons = moonMarks
        let dst = dstDays
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                if showWeek {
                    Text(L("cal.wk")).font(.caption2.weight(.semibold)).foregroundStyle(Theme.muted.opacity(0.7)).frame(width: 26)
                }
                ForEach(0..<7, id: \.self) { i in
                    Text(L("calendar.weekdaysShort.\(i)"))
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(i >= 5 ? Theme.muted.opacity(0.7) : Theme.muted)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.bottom, 6)
            ForEach(Array(weeks.enumerated()), id: \.offset) { _, week in
                Divider()
                HStack(spacing: 0) {
                    if showWeek {
                        Text(week.compactMap { $0 }.first.map { String($0.isoWeek.week) } ?? "")
                            .font(.caption2).monospacedDigit().foregroundStyle(Theme.muted.opacity(0.8)).frame(width: 26)
                    }
                    ForEach(0..<7, id: \.self) { i in
                        if i < week.count, let d = week[i] {
                            cell(d, moon: moons[d.dayNumber], dst: dst.contains(d.dayNumber)).onTapGesture { onSelect(d) }
                        } else {
                            Color.clear.frame(maxWidth: .infinity, minHeight: 52)
                        }
                    }
                }
            }
        }
    }

    private func cell(_ d: CalendarDay, moon: String?, dst: Bool) -> some View {
        let events = calEvents(d)
        let isHoliday = events.contains { $0.type == "holiday" }
        let isToday = d == today
        let isSelected = d == selected
        var types: [String] = Array(Set(events.map(\.type))).sorted()
        if CalPrefs.country == "TR" && !CalPrefs.hidden.contains("school") && data.isSchoolBreak(d) && !types.contains("school") { types.append("school") }
        let colors: [Color] = types.map { CalendarData.color($0) }
        let mine = deviceEvents[d.dayNumber]?.count ?? 0
        return VStack(spacing: 4) {
            Text("\(d.day)")
                .font(.system(size: 18, weight: isToday || isSelected ? .semibold : .regular))
                .foregroundStyle(isToday ? Theme.surface : isSelected ? Theme.bg
                                 : isHoliday ? CalendarData.color("holiday") : d.isWeekend ? Theme.muted : Theme.text)
                .frame(width: 34, height: 34)
                .background {
                    if isToday { Circle().fill(Theme.accent) } else if isSelected { Circle().fill(Theme.text) }
                }
            if showHijri, let h = Hijri.date(year: d.year, month: d.month, day: d.day) {
                Text(String(h.day)).font(.system(size: 9)).monospacedDigit().foregroundStyle(Theme.muted)
            }
            if showNames, let e = events.first {
                // Etkinlik adı; aynı rengin art arda günleri boşluksuz şerit gibi birleşir (bayram tatili gibi)
                Text(e.name).font(.system(size: 8.5, weight: .medium)).lineLimit(1).minimumScaleFactor(0.8)
                    .foregroundStyle(Theme.text)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 1)
                    .background(e.color.opacity(0.22))
            } else {
                HStack(spacing: 3) {
                    ForEach(Array(colors.prefix(3).enumerated()), id: \.offset) { _, c in Circle().fill(c).frame(width: 5, height: 5) }
                    if mine > 0 { Circle().fill(Theme.muted).frame(width: 5, height: 5) }
                }
                .frame(height: 5)
            }
        }
        .overlay(alignment: .topTrailing) {
            if let moon { Text(moon).font(.system(size: 9)).padding(.trailing, 1) }
        }
        .overlay(alignment: .topLeading) {
            // Saat değişimi günü: "⏱" emojisinin yerine iki tonlu simge (8 Kasım 2026)
            if dst { DuoIcon(name: "dst", size: 10, color: Theme.terra).padding(.leading, 1) }
        }
        .padding(.vertical, 5)
        .frame(maxWidth: .infinity)
        .background(CalPrefs.religious && data.isRamadan(d) ? CalendarData.color("religious").opacity(0.08) : .clear)
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(([Fmt.day(d)] + events.map(\.name)).joined(separator: ", "))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

/// Yan yana sığmayan öğeleri alt satıra geçiren basit düzen
struct FlowRow<Item: Hashable, Content: View>: View {
    let items: [Item]
    @ViewBuilder let content: (Item) -> Content

    var body: some View {
        FlowLayout(spacing: 10) {
            ForEach(items, id: \.self) { content($0) }
        }
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowH: CGFloat = 0, maxX: CGFloat = 0
        for s in subviews {
            let size = s.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width { x = 0; y += rowH + spacing; rowH = 0 }
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
            rowH = max(rowH, size.height)
        }
        // Önerilen genişliğin tamamı alınır: daha dar dönülürse yerleşimde satırlar farklı kırılır ve alttakiyle çakışır
        return CGSize(width: proposal.width ?? maxX, height: y + rowH)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowH: CGFloat = 0
        for s in subviews {
            let size = s.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX { x = bounds.minX; y += rowH + spacing; rowH = 0 }
            s.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowH = max(rowH, size.height)
        }
    }
}

/// Takvim türünün adı. Türkçe dışındaki arayüzde resmî tatil, arife ve okul türlerine ülke adı eklenir
/// ("Public holiday · Türkiye"): takvimdeki tatiller Türkiye'nindir (2. analiz #92).
func calendarTypeLabel(_ t: String) -> String {
    // Kapsayıcı takvimlerde takvimin adı
    if t.hasPrefix("cal:") { return ExtraCals.name(String(t.dropFirst(4))) }
    let base = L("calendar.types.\(t)")
    // Takvimde başka ülke seçiliyse (2. analiz #122) resmî tatil o ülkenindir
    let cc = t == "holiday" && CalPrefs.country != "TR" ? CalPrefs.country : "TR"
    guard !(AppLocale.code == "tr" && cc == "TR"), ["holiday", "half", "school"].contains(t),
          let country = AppLocale.locale.localizedString(forRegionCode: cc) else { return base }
    return base + " · " + country
}

/// Seçili günün havası (2. analiz #118): WeatherKit günlük tahmininde (10 gün) varsa en yüksek/en düşük ve simge;
/// Apple Hava Durumu işaretiyle (App Review 5.2.5). Yalnız Hava özelliği açıkken sorulur.
private struct DayWeatherRow: View {
    let day: CalendarDay
    @State private var text: String?
    @State private var symbol: String?

    var body: some View {
        Group {
            if let text {
                HStack(spacing: 8) {
                    if let symbol { WeatherIcon3D(symbol: symbol, size: 24) }
                    Text(T("cal.weather", text)).foregroundStyle(Theme.text)
                    Spacer()
                    AppleWeatherMark(compact: true)
                }
            }
        }
        .task(id: day) { await load() }
    }

    @MainActor private func load() async {
        let place = PlaceModel.shared.place
        guard let w = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) else { text = nil; return }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        let target = day.noon(in: place.zone)
        if let f = w.dailyForecast.forecast.first(where: { cal.isDate($0.date, inSameDayAs: target) }) {
            text = Units.temp(f.highTemperature) + " / " + Units.temp(f.lowTemperature)
            symbol = f.symbolName
        } else {
            text = nil
            symbol = nil
        }
    }
}
