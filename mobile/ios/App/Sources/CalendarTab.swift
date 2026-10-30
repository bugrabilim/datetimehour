import SwiftUI
import EventKit
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
                    MonthGrid(month: month, today: today, selected: selected, deviceEvents: deviceEvents) {
                        selected = $0
                        BadgeStore.shared.hit(.takvim)
                    }
                        .gesture(DragGesture(minimumDistance: 30).onEnded { v in
                            if abs(v.translation.width) > abs(v.translation.height) {
                                let forward = (v.translation.width < 0) != AppLocale.isRTL
                                move(forward ? 1 : -1)
                            }
                        })
                        .listRowInsets(EdgeInsets(top: 4, leading: 8, bottom: 8, trailing: 8))
                        .listRowSeparator(.hidden)
                } header: {
                    header
                }
                dayList(today: today, deviceEvents: deviceEvents[selected.dayNumber] ?? [])
            }
            .themedBackground()
            .listStyle(.plain)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(L("client.today")) { go(to: today) }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { adding = AddRequest(day: selected) } label: { Image(systemName: "plus") }
                        .accessibilityLabel(L("calendar.add"))
                }
            }
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
                .font(.largeTitle.weight(.bold))
                .foregroundStyle(Theme.text)
            Text(String(month.year))
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.accent)
            Spacer()
            Button { move(-1) } label: { Image(systemName: "chevron.backward") }
                .accessibilityLabel(L("prayer.prevMonth"))
                .padding(.trailing, 14)
            Button { move(1) } label: { Image(systemName: "chevron.forward") }
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

    private func go(to day: CalendarDay) {
        withAnimation {
            month = day.firstOfMonth
            selected = day
        }
    }

    @ViewBuilder
    private func dayList(today: CalendarDay, deviceEvents: [EKEvent]) -> some View {
        let special = data.events(on: selected)
        Section {
            ForEach(special, id: \.self) { e in
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 2).fill(e.color).frame(width: 4, height: 34)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.name).foregroundStyle(Theme.text)
                        Text(L("calendar.types.\(e.type)")).font(.caption).foregroundStyle(Theme.muted)
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
            if data.isRamadan(selected) {
                Label(L("calendar.types.ramadan"), systemImage: "moon.stars")
                    .foregroundStyle(CalendarData.color("religious"))
            }
            if data.isSchoolBreak(selected) && !special.contains(where: { $0.type == "school" }) {
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
            if special.isEmpty && deviceEvents.isEmpty && !data.isRamadan(selected) {
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
                // Hicri tarih (Diyanet takvimi)
                if let h = Hijri.date(year: selected.year, month: selected.month, day: selected.day) {
                    Label(h.text, systemImage: "moon")
                        .font(.caption)
                        .foregroundStyle(Theme.accent)
                }
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
                Text(L("hijri.note")).font(.footnote).foregroundStyle(Theme.muted)
            }
            .padding(.top, 8)
        }
    }

    private var legend: some View {
        let types = ["holiday", "half", "religious", "special", "school"]
        return FlowRow(items: types) { t in
            HStack(spacing: 4) {
                Circle().fill(CalendarData.color(t)).frame(width: 8, height: 8)
                Text(L("calendar.types.\(t)")).font(.caption2).foregroundStyle(Theme.muted)
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

    var body: some View {
        let first = month.firstOfMonth
        let count = CalendarDay.daysIn(month.year, month.month)
        let cells: [CalendarDay?] = Array(repeating: nil, count: first.weekday) + (0..<count).map { first.adding(days: $0) }
        let weeks = stride(from: 0, to: cells.count, by: 7).map { Array(cells[$0..<min($0 + 7, cells.count)]) }
        VStack(spacing: 0) {
            HStack(spacing: 0) {
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
                    ForEach(0..<7, id: \.self) { i in
                        if i < week.count, let d = week[i] {
                            cell(d).onTapGesture { onSelect(d) }
                        } else {
                            Color.clear.frame(maxWidth: .infinity, minHeight: 52)
                        }
                    }
                }
            }
        }
    }

    private func cell(_ d: CalendarDay) -> some View {
        let events = data.events(on: d)
        let isHoliday = events.contains { $0.type == "holiday" }
        let isToday = d == today
        let isSelected = d == selected
        var types: [String] = Array(Set(events.map(\.type))).sorted()
        if data.isSchoolBreak(d) && !types.contains("school") { types.append("school") }
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
            HStack(spacing: 3) {
                ForEach(Array(colors.prefix(3).enumerated()), id: \.offset) { _, c in Circle().fill(c).frame(width: 5, height: 5) }
                if mine > 0 { Circle().fill(Theme.muted).frame(width: 5, height: 5) }
            }
            .frame(height: 5)
        }
        .padding(.vertical, 5)
        .frame(maxWidth: .infinity)
        .background(data.isRamadan(d) ? CalendarData.color("religious").opacity(0.08) : .clear)
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
