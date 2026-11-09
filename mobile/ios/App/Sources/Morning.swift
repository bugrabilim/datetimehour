import SwiftUI
import EventKit
import WeatherKit
import SaatCore

/// Alarmdan sonra "günaydın" kartı (30 Ekim 2026, rakip analizi): bir alarm son iki saat içinde çaldıysa uygulama
/// açılınca bir kez gösterilir. Selam, tarih, bugünün havası (WeatherKit, işaretiyle), sıradaki vakit, bugünün özel günleri.
enum MorningCheck {
    private static let shownKey = "morning.shown"

    /// Gösterilmesi gerekiyorsa true döner ve gösterildi diye işaretler
    static func due(now: Date = Date()) -> Bool {
        let last = AlarmItem.load().filter(\.enabled).compactMap { $0.occurrence(before: now) }.max()
        guard let last, now.timeIntervalSince(last) < 2 * 3600 else { return false }
        let shown = UserDefaults.standard.double(forKey: shownKey)
        guard last.timeIntervalSince1970 > shown else { return false }
        UserDefaults.standard.set(now.timeIntervalSince1970, forKey: shownKey)
        SleepLogStore.recordWake(last) // uyku düzeni (2. analiz #47): iPhone'da kalkış = alarmın çaldığı an
        WakeGame.record(wake: last, alarm: last, task: false) // uyanma oyunu (2. analiz #7)
        return true
    }

    static func greeting(_ date: Date = Date()) -> String {
        switch Calendar.current.component(.hour, from: date) {
        case 5..<11: L("morning.good")
        case 11..<17: L("morning.day")
        case 17..<22: L("morning.evening")
        default: L("morning.night")
        }
    }
}

struct MorningCard: View {
    @Environment(\.dismiss) private var dismiss
    @State private var weather: Weather?
    private let now = Date()
    private let wall = AlarmWalls.recent()

    var body: some View {
        let place = PlaceModel.shared.place
        // Takvimin ülke ve tür seçimiyle (9 Kasım 2026: dini günler yalnız Vakitler açıkken varsayılan)
        let today = calEvents(CalendarDay(now)).map(\.name)
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(MorningCheck.greeting(now)).font(.largeTitle.bold())
                        Text(Fmt.longDate(now)).foregroundStyle(Theme.muted)
                    }
                    .padding(.vertical, 6)
                }
                if let day = weather?.dailyForecast.first {
                    Section(L("weather.h1")) {
                        HStack(spacing: 12) {
                            WeatherIcon3D(symbol: day.symbolName, size: 40)
                            VStack(alignment: .leading) {
                                Text(day.condition.description)
                                Text("\(Units.temp(day.highTemperature)) / \(Units.temp(day.lowTemperature))")
                                    .foregroundStyle(Theme.muted)
                            }
                        }
                        AppleWeatherMark(compact: true)
                    }
                }
                // Sıradaki vakit yalnız Vakitler açıkken (9 Kasım 2026, Müslüman olmayanlar raporu A5)
                if Features.isOn(.prayer), let next = place.schedule.next(after: now) {
                    Section(L("home.item.next")) {
                        Text("\(next.prayer.localizedName) \(place.hm(next.at))").font(.headline)
                    }
                }
                // Bugünün özel günleri ve telefondaki takvimlerin etkinlikleri (2. analiz #144)
                let mine = DeviceCalendar.shared.events(on: CalendarDay(now))
                if !today.isEmpty || !mine.isEmpty {
                    Section(L("morning.today")) {
                        ForEach(today, id: \.self) { Text($0) }
                        ForEach(mine, id: \.eventIdentifier) { e in
                            HStack {
                                Circle().fill(Color(cgColor: e.calendar.cgColor)).frame(width: 8, height: 8)
                                Text(e.title ?? "")
                                Spacer()
                                Text(e.isAllDay ? L("calendar.allDay") : Fmt.time(e.startDate)).foregroundStyle(Theme.muted).monospacedDigit()
                            }
                        }
                    }
                }
                WakeGameMorningSection()
                JournalMorningSection()
            }
            // Alarm duvar kâğıdı (2. analiz #2): son çalan alarmınki
            .alarmWall(wall)
            .toolbar { Button(L("common.done")) { dismiss() } }
        }
        .task { weather = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) }
    }
}
