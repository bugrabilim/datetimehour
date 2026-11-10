import SwiftUI
import SaatCore

/*
 Panchang ve Rahu Kaal (10 Kasım 2026, Hindistan araştırması #9; sitedeki /panchang/ sayfası ve Android PanchangScreen ile
 aynı): seçili yer ve gün için gün doğumu/batımı, geleneksel olarak kaçınılan (Rahu Kaal, Yamaganda, Gulika) ve uygun
 sayılan (Abhijit, Brahma muhurta) dilimler, yedi günün Rahu Kaal saatleri. Hesap telefonda (SaatCore PrayerTimes.hinduKaal).
 Hindu takviminin günlük satırı (tithi, nakşatra, ay, Vikram Samvat) Yeni Delhi gün doğumuna göre calendars.json'dan;
 Hindu takvimi Takvim'de açık olmasa da gösterilir. Yorum yok: dilimler gelenek olarak, tarafsız sözlerle yazılır.
 */

/// Bir yer ve günün dilimleri; ondalık saatler o günün yerel öğlesinden sayılır (yaz saati geçişinde de doğru an)
struct PanchangDay {
    let date: CalendarDay
    let place: AppPlace
    let kaal: PrayerTimes.HinduKaal?
    private let noon: Date

    init(_ date: CalendarDay, _ place: AppPlace) {
        self.date = date
        self.place = place
        noon = date.noon(in: place.zone)
        let off = Double(place.zone.secondsFromGMT(for: noon)) / 3600
        kaal = PrayerTimes.hinduKaal(year: date.year, month: date.month, day: date.day, latitude: place.lat, longitude: place.lon, utcOffset: off)
    }

    func at(_ h: Double) -> Date { noon.addingTimeInterval(((h * 60).rounded() - 720) * 60) }

    /// Saat (kullanıcının 24/12 seçimiyle)
    func clock(_ h: Double?) -> String {
        guard let h else { return "—" }
        let d = at(h)
        return [Fmt.time(d, tz: place.zone), Fmt.ampm(d, tz: place.zone)].filter { !$0.isEmpty }.joined(separator: " ")
    }
    func span(_ r: (Double, Double)?) -> String { r.map { clock($0.0) + "–" + clock($0.1) } ?? "—" }
    var label: String { L("calendar.weekdaysShort.\(date.weekday)") + " \(date.day)" }
}

enum Panchang {
    /// Hindu takviminin o günkü satırı (calendars.json; Yeni Delhi gün doğumu); veri yoksa nil
    static func hinduLine(_ d: CalendarDay) -> String? {
        guard let c = ExtraCals.find("hindu") else { return nil }
        return ExtraCals.dailyText(c, d)
    }

    /// "Nasıl çalışır?" bölümleri (Android'dekiyle aynı)
    static var howSections: [(String?, String)] {
        [
            (L("panchang.defsHeading"), L("panchang.defs.0")),
            (nil, L("panchang.defs.1")),
            (nil, L("panchang.defs.2")),
            (L("panchang.hindu"), L("panchang.hinduNote")),
        ]
    }
}

struct PanchangView: View {
    @State private var model = PlaceModel.shared
    @State private var selected: CalendarDay?

    var body: some View {
        let place = model.place
        let today = CalendarDay(Date(), timeZone: place.zone)
        let date = selected ?? today
        let p = PanchangDay(date, place)
        let week = (0..<7).map { PanchangDay(date.adding(days: $0), place) }
        let k = p.kaal
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                // Uzun açıklamalar ekranda değil "Nasıl çalışır?" alt sayfasında (doğal tasarım madde 13)
                HowItWorks(sections: Panchang.howSections)
                PlacePicker()
                dayNav(date, today: today, place: place)
                if k == nil { Text(L("panchang.polar")).font(.footnote).foregroundStyle(Theme.muted) }
                VStack(alignment: .leading, spacing: 6) {
                    SectionTitle(text: L("panchang.hindu"))
                    Text(Panchang.hinduLine(date) ?? L("panchang.hinduNone")).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text)
                    PanchangRow(icon: "sunrise", tint: Theme.terra, label: L("nature.sunrise"), value: p.clock(k?.sunrise))
                    PanchangRow(icon: "sunset", tint: Theme.terra, label: L("nature.sunset"), value: p.clock(k?.sunset))
                    Text(L("panchang.hinduNote")).font(.footnote).foregroundStyle(Theme.muted)
                }
                .card()
                VStack(alignment: .leading, spacing: 6) {
                    SectionTitle(text: L("panchang.avoid"))
                    PanchangRow(icon: "hourglass", tint: Theme.dusk, label: L("panchang.rahu"), value: p.span(k?.rahu))
                    PanchangRow(icon: "hourglass", tint: Theme.dusk, label: L("panchang.yama"), value: p.span(k?.yamaganda))
                    PanchangRow(icon: "hourglass", tint: Theme.dusk, label: L("panchang.gulika"), value: p.span(k?.gulika))
                }
                .card()
                VStack(alignment: .leading, spacing: 6) {
                    SectionTitle(text: L("panchang.good"))
                    PanchangRow(icon: "sun", tint: Theme.gold, label: L("panchang.abhijit"), value: p.span(k?.abhijit))
                    PanchangRow(icon: "sunrise", tint: Theme.gold, label: L("panchang.brahma"), value: p.span(k?.brahma))
                    // Bazı geleneklerde çarşamba (CalendarDay.weekday: pazartesi 0)
                    if date.weekday == 2 {
                        Text(L("panchang.abhijit") + ": " + L("panchang.wed")).font(.footnote).foregroundStyle(Theme.muted)
                    }
                }
                .card()
                VStack(alignment: .leading, spacing: 4) {
                    SectionTitle(text: L("panchang.week")).padding(.bottom, 4)
                    ForEach(week, id: \.date) { d in
                        Button { selected = d.date } label: {
                            HStack {
                                Text(d.label).font(.subheadline.weight(d.date == date ? .semibold : .regular))
                                    .foregroundStyle(d.date == date ? Theme.accent : Theme.text)
                                Spacer()
                                Text(d.span(d.kaal?.rahu)).font(.subheadline).monospacedDigit().foregroundStyle(Theme.text)
                            }
                            .padding(.vertical, 5)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .card()
                Text(L("panchang.note")).font(.footnote).foregroundStyle(Theme.muted)
            }
            .padding(16)
        }
        .themedBackground()
        .navigationTitle(L("panchang.h1"))
        .navigationBarTitleDisplayMode(.large)
    }

    private func dayNav(_ date: CalendarDay, today: CalendarDay, place: AppPlace) -> some View {
        HStack {
            Button {
                if date > today.adding(days: -366) { selected = date.adding(days: -1) }
            } label: {
                Image(systemName: "chevron.backward").font(.headline).frame(width: 44, height: 44)
            }
            .accessibilityLabel(L("photoApp.prevDay"))
            Spacer()
            VStack(spacing: 2) {
                Text(Fmt.longDate(date.noon(in: place.zone), tz: place.zone)).font(.headline).multilineTextAlignment(.center)
                if date != today {
                    Button(L("client.today")) { selected = today }.font(.subheadline.weight(.medium))
                }
            }
            Spacer()
            Button {
                if date < today.adding(days: 366) { selected = date.adding(days: 1) }
            } label: {
                Image(systemName: "chevron.forward").font(.headline).frame(width: 44, height: 44)
            }
            .accessibilityLabel(L("photoApp.nextDay"))
        }
        .buttonStyle(.borderless)
        .tint(Theme.accent)
    }
}

private struct PanchangRow: View {
    let icon: String
    let tint: Color
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 8) {
            DuoIcon(name: icon, size: 18, color: tint)
            Text(label).foregroundStyle(Theme.text)
            Spacer(minLength: 8)
            Text(value).monospacedDigit().foregroundStyle(Theme.text).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .frame(minHeight: 30)
    }
}
