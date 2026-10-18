package tr.bumba.time

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId

/**
 * Çalan alarm ekranı: kilit ekranının üstünde açılır, ekranı yakar. Kapat (bildirim ve ses kapanır) ya da
 * 5 dakika ertele (yalnız alarmlarda). Ses bildirim kanalından gelir (alarm sesi, sürekli).
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: run { finish(); return }
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: L("tools.alarm.defaultLabel")
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"
        setContent {
            AppTheme {
                val now = rememberNow()
                Column(
                    Modifier.fillMaxSize().background(Theme.c.bg).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Alarm, null, tint = Theme.c.accent, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Ltr {
                        Text(Fmt.time(now.atZone(ZoneId.systemDefault())), color = Theme.c.ink, fontSize = 72.sp,
                            fontWeight = FontWeight.Bold, style = Tnum)
                    }
                    Text(title, color = Theme.c.text, fontSize = 24.sp, textAlign = TextAlign.Center)
                    Text(L("tools.alarm.ringing"), color = Theme.c.muted, fontSize = 15.sp)
                    Spacer(Modifier.height(40.dp))
                    RoundButton(L("tools.alarm.stop"), true, Modifier.fillMaxWidth()) {
                        AlarmScheduler.stop(this@AlarmActivity, id)
                        finish()
                    }
                    if (kind == "alarm") {
                        Spacer(Modifier.height(12.dp))
                        RoundButton(L("tools.alarm.snooze"), false, Modifier.fillMaxWidth()) {
                            AlarmScheduler.snooze(this@AlarmActivity, id, title)
                            finish()
                        }
                    }
                }
            }
        }
    }
}
