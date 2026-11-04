import XCTest
@testable import SaatCore

/// Hava açıklamaları ve özet: Android WeatherWordsTest ve sitedeki npm test ile aynı girdiler
final class WeatherWordsTests: XCTestCase {
    private func last(_ s: String) -> String { String(s.split(separator: ".").last!) }

    func testWords() {
        XCTAssertEqual([3.0, 12, 25, 45, 80].map { last(WeatherWords.wind($0)) }, ["calm", "light", "breezy", "strong", "gale"])
        XCTAssertEqual([1.0, 4, 7, 9.5, 12].map { last(WeatherWords.uv($0)) }, ["low", "moderate", "high", "veryHigh", "extreme"])
        XCTAssertEqual([20.0, 45, 70, 90].map { last(WeatherWords.humidity($0)) }, ["dry", "comfy", "humid", "veryHumid"])
        XCTAssertEqual([500.0, 2500, 8000, 24000].map { last(WeatherWords.visibility($0)) }, ["fog", "haze", "good", "clear"])
        XCTAssertEqual([5.0, 12, 18, 22].map { last(WeatherWords.dew($0)) }, ["dry", "comfy", "muggy", "oppressive"])
        XCTAssertEqual(WeatherWords.feels(10, 6), "weather.fw.colder")
        XCTAssertEqual(WeatherWords.feels(30, 34), "weather.fw.warmer")
        XCTAssertNil(WeatherWords.feels(20, 21))
        XCTAssertEqual([10.0, 50, 80, 190, 300, 350].map { WeatherWords.compass8($0) }, [0, 1, 2, 4, 7, 0])
    }

    func testSummary() {
        let calm = (8...19).map { WeatherWords.Hour(hour: $0, code: 1, rain: 10, wind: 10) }
        XCTAssertEqual(WeatherWords.summary(calm, todayMax: 20, tomorrowMax: 22), .init("weather.sum.default"))
        XCTAssertEqual(WeatherWords.summary(calm, todayMax: 20, tomorrowMax: 13), .init("weather.sum.colder", n: 7))
        XCTAssertEqual(WeatherWords.summary(calm, todayMax: 20, tomorrowMax: 25.6), .init("weather.sum.warmer", n: 6))
        let windy = calm.map { $0.hour == 15 ? WeatherWords.Hour(hour: 15, code: 1, rain: 10, wind: 35) : $0 }
        XCTAssertEqual(WeatherWords.summary(windy, todayMax: 20, tomorrowMax: 13), .init("weather.sum.windyAfternoon"))
        let rain = windy.map { $0.hour == 11 ? WeatherWords.Hour(hour: 11, code: 61, rain: 70, wind: 10) : $0 }
        XCTAssertEqual(WeatherWords.summary(rain, todayMax: 20, tomorrowMax: 13), .init("weather.sum.rain", hour: 11))
        let snow = rain.map { $0.hour == 11 ? WeatherWords.Hour(hour: 11, code: 73, rain: 70, wind: 10) : $0 }
        XCTAssertEqual(WeatherWords.summary(snow, todayMax: 20, tomorrowMax: 13), .init("weather.sum.snow", hour: 11))
        let storm = snow.map { $0.hour == 17 ? WeatherWords.Hour(hour: 17, code: 95, rain: 10, wind: 10) : $0 }
        XCTAssertEqual(WeatherWords.summary(storm, todayMax: 20, tomorrowMax: 13), .init("weather.sum.storm", hour: 17))
        let late = (8...23).map { WeatherWords.Hour(hour: $0, code: $0 == 21 ? 95 : 1, rain: 0, wind: 0) }
        XCTAssertEqual(WeatherWords.summary(late, todayMax: 20, tomorrowMax: nil), .init("weather.sum.default"))
        XCTAssertEqual(WeatherWords.tomorrowAlert(todayMax: 18, tomorrowMax: 10), .init("weather.alert.colder", n: 8))
        XCTAssertNil(WeatherWords.tomorrowAlert(todayMax: 18, tomorrowMax: 15))
    }
}
