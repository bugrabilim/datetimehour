import SwiftUI
import SaatCore

/// Vakitler sekmesi: il, sıradaki vakte kalan süre, bugünün vakitleri, aylık çizelge, imsakiye, bildirimler.
struct PrayerTab: View {
    @AppStorage(NotificationPrefs.enabledKey) private var notifyOn = true
    /// Ekran görüntüsü için: -openNotifications ile ayar penceresi açık başlar
    @State private var showNotify = ProcessInfo.processInfo.arguments.contains("-openNotifications")
    @State private var sharing = false
    /// "…" menüsünden açılan sayfalar (2. analiz #199)
    @State private var route: PrayerRoute?

    enum PrayerRoute: Hashable { case monthly, method, imsakiye, worship }

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let place = PlaceModel.shared.place
                ScrollView {
                    VStack(spacing: 24) {
                        PrayerPanel(now: context.date, place: place)
                        PrayerVisual(now: context.date, place: place)
                            // Yer değişince ya da vakit girince kilit ekranındaki sayaç ileri taşınır
                            .onChange(of: place.name) { LiveActivities.sync() }
                            .onChange(of: place.schedule.next(after: context.date)?.at) { LiveActivities.sync() }
                        // İbadet satırı (2. analiz #199): dört kare, hepsi "İbadet" sayfasında
                        WorshipRow { route = .worship }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 20)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                }
                .background(AppBackdrop().ignoresSafeArea())
            }
            .navigationTitle(L("tab.prayer"))
            .navigationBarTitleDisplayMode(.large)
            .navigationDestination(item: $route) { r in
                let place = PlaceModel.shared.place
                switch r {
                case .monthly: MonthlyPrayerView(place: place)
                case .method: PrayerMethodView()
                case .worship: WorshipLinks().navigationTitle(L("worship.title"))
                case .imsakiye:
                    if let ramadan = CalendarData.shared.nextRamadan(from: CalendarDay(Date())) {
                        ImsakiyeView(place: place, ramadan: ramadan)
                    }
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showNotify = true } label: {
                        Image(systemName: notifyOn ? "bell.fill" : "bell")
                    }
                    .accessibilityLabel(L("notify.heading"))
                }
                // Seyrek kullanılanlar "…" menüsünde (2. analiz #199): paylaş, aylık çizelge, hesap yöntemi, imsakiye
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button { sharing = true } label: { Label(L("share.prayer"), systemImage: "square.and.arrow.up") }
                        Button { route = .monthly } label: { Label(L("prayer.monthly"), systemImage: "tablecells") }
                        Button { route = .method } label: {
                            Label(L("prayer.method.app") + (PrayerMethod.isCustom ? " · " + PrayerMethod.name(PrayerMethod.id) : ""),
                                  systemImage: "slider.horizontal.3")
                        }
                        if let ramadan = CalendarData.shared.nextRamadan(from: CalendarDay(Date())) {
                            Button { route = .imsakiye } label: { Label(T("imsakiye.title", ramadan.from.year), systemImage: "moon.haze") }
                        }
                    } label: { Image(systemName: "ellipsis.circle") }
                    .accessibilityLabel(L("common.more"))
                }
            }
            .sheet(isPresented: $showNotify) { NotificationSettingsView() }
            .sheet(isPresented: $sharing) {
                let place = PlaceModel.shared.place, now = Date()
                ShareCardSheet(title: L("share.prayer")) { PrayerShareCard(place: place, now: now) }
            }
        }
    }
}

/// Konum seçimi, sıradaki vakte kalan süre ve bugünün vakitleri
struct PrayerPanel: View {
    let now: Date
    let place: AppPlace
    @State private var trackVersion = 0
    @AppStorage(TrackX.inlineKey) private var inline = true

    var body: some View {
        let schedule = place.schedule
        let next = schedule.next(after: now)
        VStack(spacing: 16) {
            PlacePicker()
            SavedPlaceChips()
            // Bugünün miladi ve hicri tarihi (Diyanet takvimi)
            if let h = Hijri.date(now, timeZone: place.zone) {
                Text(Fmt.longDate(now, tz: place.zone) + " · " + h.text)
                    .font(.footnote)
                    .foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
            }

            UpcomingReligiousLine(today: CalendarDay(now, timeZone: place.zone))
            if let next {
                SkyCountdown(entries: schedule.entries(on: now), next: next, now: now)
            }

            VStack(spacing: 0) {
                SectionTitle(text: String(localized: "prayer.today"))
                    .padding(.bottom, 8)
                ForEach(schedule.entries(on: now)) { entry in
                    let isNext = entry.at == next?.at
                    HStack(spacing: 10) {
                        Image(systemName: prayerSymbol(entry.prayer))
                            .foregroundStyle(isNext ? Theme.accent : Theme.muted)
                            .frame(width: 22)
                            .accessibilityHidden(true)
                        Text(entry.prayer.localizedName)
                        Spacer()
                        Text(place.hm(entry.at))
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                        if inline {
                            PrayedMark(day: CalendarDay(now, timeZone: place.zone), entry: entry, now: now, version: $trackVersion)
                        }
                    }
                    .font(.body.weight(isNext ? .bold : .regular))
                    .foregroundStyle(Theme.text)
                    .padding(.vertical, inline ? 7 : 12)
                    .padding(.horizontal, 14)
                    .background(isNext ? Theme.accentSoft : .clear, in: RoundedRectangle(cornerRadius: 10))
                }
                if inline {
                    PrayedSummary(day: CalendarDay(now, timeZone: place.zone), entries: schedule.entries(on: now), now: now, version: $trackVersion)
                        .padding(.horizontal, 14)
                }
            }
            BayramRow(place: place, now: now)
            EzaniRow(place: place, now: now)
            RamadanCountdown(place: place)
            KerahatBox(entries: schedule.entries(on: now), place: place)
            ExtraTimesBox(place: place, day: place.today)
        }
    }
}

/// Kerahat vakitleri (rakip analizi #12; sitedeki namaz sayfasıyla aynı): güneş vaktinden sonra, öğleden ve akşamdan önce
/// yaklaşık 45 dk (`PrayerTimes.kerahatMinutes`; Diyanet'in yayımladığı bir dakika ölçüsü yok, notta yazar).
struct KerahatBox: View {
    let entries: [PrayerSchedule.Entry]
    let place: AppPlace
    @State private var open = false

    var body: some View {
        let at = { (p: Prayer) in entries.first(where: { $0.prayer == p })?.at }
        if let sr = at(.sunrise), let dh = at(.dhuhr), let mg = at(.maghrib) {
            let k = TimeInterval(PrayerTimes.kerahatMinutes * 60)
            let rows: [(String, Date, Date)] = [("israk", sr, sr + k), ("istiva", dh - k, dh), ("isfirar", mg - k, mg)]
            DisclosureGroup(isExpanded: $open) {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                        HStack {
                            Text(L("prayer.kerahatNames." + row.0))
                            Spacer()
                            Text(place.hm(row.1) + "–" + place.hm(row.2))
                                .monospacedDigit()
                                .environment(\.layoutDirection, .leftToRight)
                        }
                    }
                    Text(L("prayer.kerahatNote")).font(.footnote).foregroundStyle(Theme.muted)
                }
                .padding(.top, 8)
            } label: {
                Text(L("prayer.kerahatHeading")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
            }
            .tint(Theme.accent)
            .card()
        }
    }
}

/// Yerel gün + ondalık saat (gece yarısından) → an
func localInstant(_ d: CalendarDay, _ hours: Double, _ zone: TimeZone) -> Date {
    var cal = Calendar(identifier: .gregorian)
    cal.timeZone = zone
    let start = cal.date(from: DateComponents(year: d.year, month: d.month, day: d.day)) ?? Date()
    return start.addingTimeInterval((hours * 60).rounded() * 60)
}

/// Ramazan'da ikinci sayaç (2. analiz #55): iftara ya da sahura (imsak) kalan süre, saniyede bir
struct RamadanCountdown: View {
    let place: AppPlace

    private func target(_ now: Date) -> (String, Date)? {
        let entries = place.schedule.entries(on: now) + place.schedule.entries(on: now.addingTimeInterval(86_400))
        guard let next = entries.first(where: { ($0.prayer == .maghrib || $0.prayer == .fajr) && $0.at > now }),
              Hijri.date(next.at, timeZone: place.zone)?.month == 9 else { return nil }
        return (next.prayer.ramadanName(at: next.at, zone: place.zone), next.at)
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            if let t = target(ctx.date) {
                Text(T("prayer.ramadanLeft", t.0, Fmt.countdown(Int(t.1.timeIntervalSince(ctx.date)))))
                    .font(.headline.monospacedDigit())
                    .foregroundStyle(Theme.accent)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .card()
            }
        }
    }
}

/// Ezani (alaturka) saat (2. analiz #121): son akşam vakti 12.00 sayılır (sitedeki namaz sayfasıyla aynı)
struct EzaniRow: View {
    let place: AppPlace
    let now: Date

    private func maghrib(_ d: CalendarDay) -> Date? {
        guard let m = PrayerTimes.times(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon, timeZone: place.zone).maghrib else { return nil }
        return localInstant(d, m, place.zone)
    }
    private var text: String? {
        let today = CalendarDay(now, timeZone: place.zone)
        var ref = maghrib(today)
        if let r = ref, now < r { ref = maghrib(today.adding(days: -1)) }
        guard let start = ref else { return nil }
        let m = max(0, Int(now.timeIntervalSince(start) / 60))
        let h = (m / 60) % 12
        return T("prayer.ezani", String(format: "%02d:%02d", h == 0 ? 12 : h, m % 60))
    }

    var body: some View {
        if let text {
            Text(text)
                .font(.footnote)
                .foregroundStyle(Theme.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityHint(L("prayer.ezaniNote"))
        }
    }
}

/// Bayram namazı (2. analiz #56): bayramın ilk günü öğleye kadar, arefe günü "yarın"; Diyanet kuralı güneş + 38 dk
struct BayramRow: View {
    let place: AppPlace
    let now: Date

    private func isBayram(_ d: CalendarDay) -> Bool {
        CalendarData.shared.events(on: d).contains { $0.key == "rb1" || $0.key == "kb1" }
    }
    private func day(_ d: CalendarDay) -> PrayerDay {
        PrayerTimes.times(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon, timeZone: place.zone)
    }
    private var text: String? {
        let today = CalendarDay(now, timeZone: place.zone)
        if isBayram(today) {
            let pd = day(today)
            guard let dh = pd.dhuhr, now < localInstant(today, dh, place.zone), let b = PrayerTimes.bayram(pd) else { return nil }
            return L("prayer.bayram") + ": " + place.hm(localInstant(today, b, place.zone))
        }
        let tom = today.adding(days: 1)
        if isBayram(tom), let b = PrayerTimes.bayram(day(tom)) {
            return T("prayer.bayramTomorrow", place.hm(localInstant(tom, b, place.zone)))
        }
        return nil
    }

    var body: some View {
        if let text {
            VStack(alignment: .leading, spacing: 4) {
                Text(text).font(.headline).foregroundStyle(Theme.accent)
                Text(L("prayer.bayramNote")).font(.footnote).foregroundStyle(Theme.muted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .card()
        }
    }
}

/// Ek vakitler (2. analiz #59; Türkiye Takvimi tanımlarıyla yaklaşık, sitedeki namaz sayfasıyla aynı): açılır kutu
struct ExtraTimesBox: View {
    let place: AppPlace
    let day: CalendarDay
    @State private var open = false

    var body: some View {
        DisclosureGroup(isExpanded: $open) {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(rows, id: \.0) { row in
                    HStack {
                        Text(L("prayer.extraNames." + row.0))
                        Spacer()
                        Text(row.1.map { place.hm(localInstant(day, $0, place.zone)) } ?? "–")
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                    }
                }
                Text(L("prayer.extraNote")).font(.footnote).foregroundStyle(Theme.muted)
            }
            .padding(.top, 8)
        } label: {
            Text(L("prayer.extraHeading")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
        }
        .tint(Theme.accent)
        .card()
    }

    private var rows: [(String, Double?)] {
        guard open else { return [] }
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(secondsFromGMT: 0)!
        let noon = utc.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: 12)) ?? Date()
        let off = Double(place.zone.secondsFromGMT(for: noon)) / 3600
        let pd = PrayerTimes.times(year: day.year, month: day.month, day: day.day, latitude: place.lat, longitude: place.lon, utcOffset: off)
        let e = PrayerTimes.extraTimes(year: day.year, month: day.month, day: day.day, latitude: place.lat, longitude: place.lon, utcOffset: off, times: pd)
        let asr2 = PrayerTimes.times(year: day.year, month: day.month, day: day.day, latitude: place.lat, longitude: place.lon,
                                     utcOffset: off, parameters: .method("diyanet", hanafi: true)).asr
        return [("israk", e.israk), ("duha", e.duha), ("asrSani", asr2), ("isfirar", e.isfirar), ("istibak", e.istibak),
                ("isaSani", e.isaSani), ("midnight", e.midnight), ("tahajjud", e.tahajjud), ("seher", e.seher)]
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
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("prayer.monthly"))
        .navigationBarTitleDisplayMode(.large)
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
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(T("imsakiye.title", ramadan.from.year))
        .navigationBarTitleDisplayMode(.large)
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
