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
        DualClockWidget()
        MoonWidget()
        SecondsClockWidget()
        WorldClockWidget()
        CountdownWidget()
        HijriWidget()
        ZikirWidget()
        StopwatchWidget()
        // 2. analiz K-4: yıl ilerlemesi, yazıyla tarih, gün doğumu/batımı, günün içeriği
        YearProgressWidget()
        WordsDateWidget()
        SunWidget()
        DailyContentWidget()
        SaatLiveActivity()
        // Denetim Merkezi düğmeleri (iOS 18+)
        if #available(iOSApplicationExtension 18.0, *) {
            ClockControl()
            TimerControl()
            PrayerControl()
        }
    }
}
