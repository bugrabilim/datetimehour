package tr.bumba.time

import android.app.Application

class SaatTarihApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Strings.init(this)
        Prefs.init(this)
        AppData.init(this)
    }
}
