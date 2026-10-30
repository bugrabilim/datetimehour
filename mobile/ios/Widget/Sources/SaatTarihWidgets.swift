import SwiftUI
import WidgetKit

/// Ana ekran ve kilit ekranı widget'ları. Vakit hesabı uygulamadakiyle aynı (SaatCore), internet gerekmez.
@main
struct SaatTarihWidgets: WidgetBundle {
    var body: some Widget {
        NextPrayerWidget()
        TodayPrayersWidget()
        AnalogClockWidget()
        DigitalClockWidget()
        ClockDateWidget()
        MoonWidget()
        SecondsClockWidget()
        WorldClockWidget()
        CountdownWidget()
        HijriWidget()
    }
}
