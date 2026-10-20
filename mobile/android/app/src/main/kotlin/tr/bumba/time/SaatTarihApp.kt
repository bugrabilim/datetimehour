package tr.bumba.time

import android.app.Application

class SaatTarihApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Strings.init(this)
        Prefs.init(this)
        AppData.init(this)
        // Vakit bildirimleri: kanallar ve plan (her açılışta baştan kurulur, iOS'taki gibi)
        PrayerNotifier.start(this)
    }
}
