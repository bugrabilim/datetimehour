import SwiftUI
import SaatCore

/// Takvim sekmesi: ay görünümü (tatiller, arifeler, dini ve özel günler, Ramazan, okul tatilleri), ayın günleri, gün ayrıntısı
struct CalendarTab: View {
    @State private var month = CalendarDay(Date()).firstOfMonth
    @State private var selected: CalendarDay?
    private let data = CalendarData.shared

    var body: some View {
        let today = CalendarDay(Date())
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    header
                    MonthGrid(month: month, today: today) { selected = $0 }
                    legend
                    if let y = data.years[month.year], y.projected {
                        Text(L("calendar.projected"))
                            .font(.footnote)
                            .foregroundStyle(Theme.muted)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    monthEvents(today: today)
                }
                .padding(16)
                .frame(maxWidth: 640)
                .frame(maxWidth: .infinity)
            }
            .background(Theme.bg.ignoresSafeArea())
            .gesture(DragGesture(minimumDistance: 30).onEnded { v in
                if abs(v.translation.width) > abs(v.translation.height) {
                    let forward = (v.translation.width < 0) != AppLocale.isRTL
                    withAnimation { month = month.adding(months: forward ? 1 : -1) }
                }
            })
            .navigationTitle(L("tab.calendar"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(L("client.today")) { withAnimation { month = today.firstOfMonth } }
                }
            }
            .sheet(item: $selected) { day in
                DayDetail(day: day, today: today)
                    .presentationDetents([.medium, .large])
            }
        }
    }

    private var header: some View {
        HStack {
            Button { withAnimation { month = month.adding(months: -1) } } label: { Image(systemName: "chevron.backward") }
            Spacer()
            Text(Fmt.monthTitle(month.year, month.month)).font(.title3.weight(.semibold))
            Spacer()
            Button { withAnimation { month = month.adding(months: 1) } } label: { Image(systemName: "chevron.forward") }
        }
        .foregroundStyle(Theme.accent)
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

    @ViewBuilder
    private func monthEvents(today: CalendarDay) -> some View {
        let events = data.events(year: month.year, month: month.month)
        VStack(alignment: .leading, spacing: 8) {
            if data.years[month.year] == nil {
                Text(L("calendar.noYear")).foregroundStyle(Theme.muted)
            } else if events.isEmpty {
                Text(L("calendar.noEvents")).foregroundStyle(Theme.muted)
            }
            ForEach(events, id: \.self) { e in
                if let d = e.day {
                    Button { selected = d } label: {
                        HStack(alignment: .top, spacing: 10) {
                            Circle().fill(e.color).frame(width: 10, height: 10).padding(.top, 5)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(e.name).foregroundStyle(Theme.text)
                                Text(Fmt.day(d)).font(.caption).foregroundStyle(Theme.muted)
                            }
                            Spacer()
                            Text(relative(d, today))
                                .font(.caption)
                                .foregroundStyle(Theme.muted)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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

/// Ay ızgarası: pazartesi başlar; renkler sitedeki göstergeyle aynı
struct MonthGrid: View {
    let month: CalendarDay
    let today: CalendarDay
    let onSelect: (CalendarDay) -> Void
    private let data = CalendarData.shared

    var body: some View {
        let first = month.firstOfMonth
        let count = CalendarDay.daysIn(month.year, month.month)
        let cells: [CalendarDay?] = Array(repeating: nil, count: first.weekday) + (0..<count).map { first.adding(days: $0) }
        let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
        LazyVGrid(columns: columns, spacing: 6) {
            ForEach(0..<7, id: \.self) { i in
                Text(L("calendar.weekdaysShort.\(i)"))
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(i >= 5 ? Theme.accent : Theme.muted)
            }
            ForEach(Array(cells.enumerated()), id: \.offset) { _, day in
                if let day {
                    cell(day).onTapGesture { onSelect(day) }
                } else {
                    Color.clear.frame(height: 44)
                }
            }
        }
    }

    private func cell(_ d: CalendarDay) -> some View {
        let events = data.events(on: d)
        let main = events.first { $0.type == "holiday" } ?? events.first { $0.type == "half" } ?? events.first
        let isHoliday = events.contains { $0.type == "holiday" }
        return VStack(spacing: 3) {
            Text("\(d.day)")
                .font(.body.weight(d == today || isHoliday ? .bold : .regular))
                .foregroundStyle(isHoliday ? CalendarData.color("holiday") : (d.isWeekend ? Theme.muted : Theme.text))
            HStack(spacing: 2) {
                if let main { Circle().fill(main.color).frame(width: 5, height: 5) }
                if data.isSchoolBreak(d) && main?.type != "school" {
                    Circle().fill(CalendarData.color("school")).frame(width: 5, height: 5)
                }
            }
            .frame(height: 5)
        }
        .frame(maxWidth: .infinity, minHeight: 44)
        .background(data.isRamadan(d) ? CalendarData.color("religious").opacity(0.12) : .clear, in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            if d == today { RoundedRectangle(cornerRadius: 8).stroke(Theme.accent, lineWidth: 2) }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(([Fmt.day(d)] + events.map(\.name)).joined(separator: ", "))
    }
}

/// Güne dokununca: tarih, özel günler, hafta, yılın günü, kalan gün
struct DayDetail: View {
    let day: CalendarDay
    let today: CalendarDay

    var body: some View {
        let events = CalendarData.shared.events(on: day)
        NavigationStack {
            List {
                Section {
                    Text(Fmt.day(day)).font(.headline)
                    Text(relative(day, today)).foregroundStyle(Theme.muted)
                }
                if !events.isEmpty {
                    Section {
                        ForEach(events, id: \.self) { e in
                            HStack(spacing: 10) {
                                Circle().fill(e.color).frame(width: 10, height: 10)
                                VStack(alignment: .leading) {
                                    Text(e.name)
                                    Text(L("calendar.types.\(e.type)")).font(.caption).foregroundStyle(Theme.muted)
                                }
                            }
                        }
                    }
                }
                if CalendarData.shared.isRamadan(day) {
                    Section { Text(L("calendar.types.ramadan")) }
                }
                Section {
                    Text(T("info.week.big", day.isoWeek.week))
                    Text(T("calendar.dayOfYear", day.dayOfYear))
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
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
