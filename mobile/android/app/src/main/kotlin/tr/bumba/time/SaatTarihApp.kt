package tr.bumba.time

import android.app.Application

class SaatTarihApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Strings.init(this)
        Prefs.init(this)
        AppData.init(this)
        // Araçlar: alarmlar ve süren zamanlayıcı/pomodoro yeniden kurulur (uygulama zorla durdurulmuş olabilir)
        runCatching { AlarmScheduler.restoreAll(this) }
    }
}
