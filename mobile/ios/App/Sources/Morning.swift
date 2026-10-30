import SwiftUI
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

    var body: some View {
        let place = PlaceModel.shared.place
        let today = CalendarData.shared.events(on: CalendarDay(now)).map(\.name)
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
                            Image(systemName: day.symbolName).symbolRenderingMode(.multicolor).font(.title)
                            VStack(alignment: .leading) {
                                Text(day.condition.description)
                                Text("\(Units.temp(day.highTemperature)) / \(Units.temp(day.lowTemperature))")
                                    .foregroundStyle(Theme.muted)
                            }
                        }
                        AppleWeatherMark(compact: true)
                    }
                }
                if let next = place.schedule.next(after: now) {
                    Section(L("home.item.next")) {
                        Text("\(next.prayer.localizedName) \(place.hm(next.at))").font(.headline)
                    }
                }
                if !today.isEmpty {
                    Section(L("morning.today")) {
                        ForEach(today, id: \.self) { Text($0) }
                    }
                }
            }
            .themedBackground()
            .toolbar { Button(L("common.done")) { dismiss() } }
        }
        .task { weather = try? await WeatherCache.weather(lat: place.lat, lon: place.lon) }
    }
}
