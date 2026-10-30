import SwiftUI
import SaatCore

/// Vakitler sekmesi: il, sıradaki vakte kalan süre, bugünün vakitleri, aylık çizelge, imsakiye, bildirimler.
struct PrayerTab: View {
    @AppStorage(NotificationPrefs.enabledKey) private var notifyOn = true
    /// Ekran görüntüsü için: -openNotifications ile ayar penceresi açık başlar
    @State private var showNotify = ProcessInfo.processInfo.arguments.contains("-openNotifications")

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let place = PlaceModel.shared.place
                ScrollView {
                    VStack(spacing: 24) {
                        PrayerPanel(now: context.date, place: place)
                        VStack(spacing: 10) {
                            NavigationLink {
                                MonthlyPrayerView(place: place)
                            } label: {
                                linkRow(L("prayer.monthly"), "tablecells")
                            }
                            if let ramadan = CalendarData.shared.nextRamadan(from: CalendarDay(context.date)) {
                                NavigationLink {
                                    ImsakiyeView(place: place, ramadan: ramadan)
                                } label: {
                                    linkRow(T("imsakiye.title", ramadan.from.year), "moon.haze")
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 20)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
                .background(Theme.bg.ignoresSafeArea())
            }
            .navigationTitle(L("tab.prayer"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showNotify = true } label: {
                        Image(systemName: notifyOn ? "bell.fill" : "bell")
                    }
                    .accessibilityLabel(L("notify.heading"))
                }
            }
            .sheet(isPresented: $showNotify) { NotificationSettingsView() }
        }
    }

    private func linkRow(_ title: String, _ icon: String) -> some View {
        HStack {
            Label(title, systemImage: icon)
                .foregroundStyle(Theme.text)
            Spacer()
            Image(systemName: "chevron.forward")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Theme.muted)
        }
        .card()
    }
}

/// Konum seçimi, sıradaki vakte kalan süre ve bugünün vakitleri
struct PrayerPanel: View {
    let now: Date
    let place: AppPlace

    var body: some View {
        let schedule = place.schedule
        let next = schedule.next(after: now)
        VStack(spacing: 16) {
            PlacePicker()
            SavedPlaceChips()

            if let next {
                VStack(spacing: 4) {
                    Text(String(format: String(localized: "prayer.nextIn"), next.prayer.localizedName))
                        .font(.subheadline)
                        .foregroundStyle(Theme.muted)
                    Text(Fmt.countdown(Int(next.at.timeIntervalSince(now).rounded(.up))))
                        .font(.system(size: 40, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(Theme.accent)
                        .environment(\.layoutDirection, .leftToRight)
                }
            }

            VStack(spacing: 0) {
                SectionTitle(text: String(localized: "prayer.today"))
                    .padding(.bottom, 8)
                ForEach(schedule.entries(on: now)) { entry in
                    let isNext = entry.at == next?.at
                    HStack {
                        Text(entry.prayer.localizedName)
                        Spacer()
                        Text(place.hm(entry.at))
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    .font(.body.weight(isNext ? .bold : .regular))
                    .foregroundStyle(Theme.text)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 14)
                    .background(isNext ? Theme.accentSoft : .clear, in: RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }
}

/// Aylık vakit çizelgesi (sitedeki gibi): her gün altı vakit, bugün vurgulu
struct MonthlyPrayerView: View {
    let place: AppPlace
    @State private var month = CalendarDay(Date()).firstOfMonth

    var body: some View {
        let schedule = place.schedule
        let today = place.today
        let days = (0..<CalendarDay.daysIn(month.year, month.month)).map { month.adding(days: $0) }
        ScrollView {
            VStack(spacing: 12) {
                HStack {
                    Button { month = month.adding(months: -1) } label: { Image(systemName: "chevron.backward") }
                        .accessibilityLabel(L("prayer.prevMonth"))
                    Spacer()
                    Text(Fmt.monthTitle(month.year, month.month)).font(.headline)
                    Spacer()
                    Button { month = month.adding(months: 1) } label: { Image(systemName: "chevron.forward") }
                        .accessibilityLabel(L("prayer.nextMonth"))
                }
                .foregroundStyle(Theme.accent)
                Text(place.name)
                    .font(.subheadline)
                    .foregroundStyle(Theme.muted)
                Grid(horizontalSpacing: 6, verticalSpacing: 0) {
                    GridRow {
                        Text(L("prayer.colDay")).gridColumnAlignment(.leading)
                        ForEach(Prayer.allCases, id: \.self) { p in
                            Text(p.localizedName).lineLimit(1).minimumScaleFactor(0.6)
                        }
                    }
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(Theme.muted)
                    .padding(.vertical, 6)
                    ForEach(days, id: \.self) { d in
                        let entries = schedule.entries(on: d.noon(in: place.zone))
                        let isToday = d == today
                        GridRow {
                            Text("\(d.day) " + L("calendar.weekdaysShort.\(d.weekday)"))
                                .gridColumnAlignment(.leading)
                            ForEach(Prayer.allCases, id: \.self) { p in
                                Text(entries.first { $0.prayer == p }.map { place.hm($0.at) } ?? "–")
                            }
                        }
                        .font(.caption.weight(isToday ? .bold : .regular).monospacedDigit())
                        .foregroundStyle(Theme.text)
                        .padding(.vertical, 7)
                        .background(isToday ? Theme.accentSoft : .clear)
                    }
                }
                .environment(\.layoutDirection, .leftToRight)
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(L("prayer.monthly"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// İmsakiye: Ramazan boyunca imsak ve iftar; görsel olarak paylaşılabilir
struct ImsakiyeView: View {
    let place: AppPlace
    let ramadan: CalendarData.Ramadan
    @Environment(\.displayScale) private var displayScale
    @State private var image: UIImage?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                ImsakiyeCard(place: place, ramadan: ramadan)
                if let image {
                    ShareLink(item: Image(uiImage: image),
                              preview: SharePreview(T("imsakiye.title", ramadan.from.year), image: Image(uiImage: image))) {
                        Label(L("imsakiye.share"), systemImage: "square.and.arrow.up")
                            .font(.headline)
                    }
                    .tint(Theme.accent)
                }
            }
            .padding(16)
        }
        .background(Theme.bg.ignoresSafeArea())
        .navigationTitle(T("imsakiye.title", ramadan.from.year))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            let renderer = ImageRenderer(content: ImsakiyeCard(place: place, ramadan: ramadan).frame(width: 390))
            renderer.scale = max(displayScale, 2)
            image = renderer.uiImage
        }
    }
}

/// İmsakiye görseli: başlık, il, gün gün imsak ve iftar, imza
struct ImsakiyeCard: View {
    let place: AppPlace
    let ramadan: CalendarData.Ramadan

    var body: some View {
        let schedule = place.schedule
        let today = place.today
        VStack(spacing: 10) {
            Image(systemName: "moon.stars.fill")
                .font(.largeTitle)
                .foregroundStyle(Theme.accent)
            Text(T("imsakiye.title", ramadan.from.year))
                .font(.title2.weight(.bold))
            Text(place.name)
                .font(.headline)
                .foregroundStyle(Theme.accent)
            Grid(horizontalSpacing: 10, verticalSpacing: 0) {
                GridRow {
                    Text(L("prayer.colDay")).gridColumnAlignment(.leading)
                    Text("").gridColumnAlignment(.leading)
                    Text(Prayer.fajr.localizedName)
                    Text(L("imsakiye.iftar"))
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.muted)
                .padding(.vertical, 6)
                ForEach(Array(ramadan.days.enumerated()), id: \.offset) { i, d in
                    let entries = schedule.entries(on: d.noon(in: place.zone))
                    GridRow {
                        Text(T("imsakiye.day", i + 1))
                        Text(Fmt.dayMonth(d.noon(in: place.zone), tz: place.zone) + " " + L("calendar.weekdaysShort.\(d.weekday)"))
                        Text(entries.first { $0.prayer == .fajr }.map { place.hm($0.at) } ?? "–")
                        Text(entries.first { $0.prayer == .maghrib }.map { place.hm($0.at) } ?? "–")
                    }
                    .font(.footnote.weight(d == today ? .bold : .regular).monospacedDigit())
                    .padding(.vertical, 4)
                    .background(d == today ? Theme.accentSoft : (i % 2 == 0 ? Theme.surface2.opacity(0.6) : .clear))
                }
            }
            Text(L("brand.product") + " · time.bumba.tr")
                .font(.caption2)
                .foregroundStyle(Theme.muted)
                .padding(.top, 6)
        }
        .foregroundStyle(Theme.text)
        .padding(18)
        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 18))
    }
}
