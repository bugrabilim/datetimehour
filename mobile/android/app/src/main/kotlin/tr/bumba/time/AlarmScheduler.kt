package tr.bumba.time

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Alarm: saat, etiket, günler (ISO 1 pazartesi … 7 pazar; boş = bir kez), erteleme süresi (dk; 0 = yok) ve sınırı
 * (`snoozeMax`; 0 = sınırsız), "sonraki alarmı atla" (`skipUntil`, ms), ses (zil sesi adresi; boş = varsayılan alarm
 * sesi), titreşim, kademeli ses (`rise`) ve "resmî tatillerde çalmasın" (`skipHolidays`; yalnız tekrarlayan alarmda,
 * tam gün resmî tatiller calendar.json'dan; yarım gün arifelerde çalar). iOS'taki AlarmItem ile aynı alanlar (ses ve
 * titreşim yalnız Android'de). Eski kayıtlardaki `repeats` okunurken günlere çevrilir.
 */
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val days: List<Int> = emptyList(),
    val enabled: Boolean = true,
    val snooze: Int = 5,
    val skipUntil: Long = 0,
    val sound: String = "",
    val vibrate: Boolean = true,
    val snoozeMax: Int = 0,
    val rise: Boolean = false,
    val skipHolidays: Boolean = false,
    /** Gün doğumu ışığı: alarmdan bu kadar dakika önce tam ekran saat yavaşça aydınlanır (0 = kapalı; rakip analizi #17) */
    val sunrise: Int = 0,
    /** Kapatma görevi (rakip analizi #13): none, math, shake, steps, code; zorluk (matematik 1–3); kayıtlı kod (QR/barkod) */
    val task: String = "none",
    val taskLevel: Int = 2,
    val code: String = "",
    /** Uyanma denetimi: kapattıktan 5 dk sonra "Uyanık mısınız?"; cevap gelmezse yeniden çalar */
    val wakeCheck: Boolean = false,
    /** Çalmadan bu kadar dakika önce sessiz "yaklaşan alarm" bildirimi, içinden atlanabilir (0 = kapalı; rakip analizi #19).
     *  Yeni alarmda 30, önceki sürümden gelen alarmda kapalı (iOS'taki AlarmItem ile aynı). */
    val preNotify: Int = 30,
    /** Çalarken saati telefonun konuşma motoruyla söyler (rakip analizi #20; AlarmRingService) */
    val speak: Boolean = false,
    /** Esnek tekrar (rakip analizi listesi #74; iOS ile aynı alanlar): bir kezlikte belirli tarih (ISO), tekrarlayanda kaç
     *  haftada bir (1–2; başlangıç haftası `anchor`, ISO pazartesi) ve bitiş tarihi (ISO, o gün dahil) */
    val date: String = "",
    val every: Int = 1,
    val anchor: String = "",
    val until: String = "",
) {
    /** Kapatırken istenecek görev (kod kaydedilmediyse matematik); yoksa "none" */
    val effectiveTask: String get() = when {
        task == "code" && (code.isEmpty() || code.startsWith(PhotoHash.PREFIX)) -> "math"
        task == "photo" && !code.startsWith(PhotoHash.PREFIX) -> "math"
        else -> task
    }

    val title: String get() = label.ifEmpty { L("tools.alarm.defaultLabel") }
    val timeText: String get() = Fmt.time(LocalDateTime.now().withHour(hour).withMinute(minute).atZone(ZoneId.systemDefault()))

    /** "Bir kez", "Her gün", "Hafta içi" ya da gün kısaltmaları; belirli tarih, iki haftada bir ve bitiş tarihi eklenir */
    val repeatText: String get() {
        var text = when {
            days.isEmpty() -> parse(date)?.let { Fmt.dayMonth(it) } ?: L("tools.alarm.once")
            days.toSet().size == 7 -> L("alarm.everyDay")
            days.toSet() == WEEKDAYS.toSet() -> L("tools.alarm.weekdays")
            else -> days.sorted().joinToString(" ") { L("tools.alarm.dayNames.${it - 1}") }
        }
        if (days.isNotEmpty() && every > 1) text += " · " + L("alarm.biweekly")
        if (days.isNotEmpty()) parse(until)?.let { text += " · " + T("alarm.untilShort", Fmt.dayMonth(it)) }
        return text
    }

    /** Esnek tekrar kuralı: bu gün çalar mı */
    fun allows(d: LocalDate): Boolean {
        parse(until)?.let { if (d.isAfter(it)) return false }
        if (days.isEmpty()) return parse(date)?.let { it == d } ?: true
        if (d.dayOfWeek.value !in days) return false
        val a = parse(anchor)
        if (every > 1 && a != null) {
            val weeks = java.time.temporal.ChronoUnit.WEEKS.between(a.minusDays(a.dayOfWeek.value - 1L), d.minusDays(d.dayOfWeek.value - 1L))
            return Math.floorMod(weeks, every.toLong()) == 0L
        }
        return true
    }

    /** `afterMs` sonrasındaki ilk çalma (atlama hesaba katılmaz); bir daha çalmayacaksa NEVER */
    fun occurrenceAfter(afterMs: Long): Long {
        val zone = ZoneId.systemDefault()
        if (days.isEmpty()) parse(date)?.let { d ->
            val ms = d.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
            return if (ms > afterMs && allows(d)) ms else NEVER
        }
        var t = java.time.Instant.ofEpochMilli(afterMs).atZone(zone).toLocalDateTime().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        repeat(16) {
            val ms = t.atZone(zone).toInstant().toEpochMilli()
            if (ms > afterMs && allows(t.toLocalDate())) return ms
            t = t.plusDays(1)
        }
        return NEVER
    }

    /** Bitiş tarihi geçti ya da belirli tarih geçmişte kaldı */
    fun expired(nowMs: Long = System.currentTimeMillis()): Boolean = nextAt(nowMs) == NEVER

    /** `beforeMs` öncesindeki son çalma (günaydın kartı için) */
    fun occurrenceBefore(beforeMs: Long): Long? {
        val zone = ZoneId.systemDefault()
        var t = java.time.Instant.ofEpochMilli(beforeMs).atZone(zone).toLocalDateTime().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        repeat(9) {
            val ms = t.atZone(zone).toInstant().toEpochMilli()
            if (ms <= beforeMs && ((days.isEmpty() && date.isEmpty()) || allows(t.toLocalDate()))) return ms
            t = t.minusDays(1)
        }
        return null
    }

    /** Sıradaki çalma (atlanan çalma ve istenirse resmî tatiller hariç) */
    fun nextAt(nowMs: Long = System.currentTimeMillis()): Long {
        var t = occurrenceAfter(maxOf(nowMs, skipUntil))
        if (!skipHolidays || days.isEmpty()) return t
        val zone = ZoneId.systemDefault()
        // Bayram en çok 4,5 gün sürer; 60 deneme her durumda yeter (bütün günler tatilse ilk çalma kalır)
        repeat(60) {
            if (t == NEVER || !isHoliday(java.time.Instant.ofEpochMilli(t).atZone(zone).toLocalDate())) return t
            t = occurrenceAfter(t)
        }
        return occurrenceAfter(maxOf(nowMs, skipUntil))
    }

    /** Sıradaki çalma bir resmî tatile denk geldiği için kaydırıldı mı (listede not için) */
    fun holidaySkipped(nowMs: Long = System.currentTimeMillis()): Boolean =
        skipHolidays && days.isNotEmpty() && nextAt(nowMs) != occurrenceAfter(maxOf(nowMs, skipUntil))

    val skipping: Boolean get() = skipUntil > System.currentTimeMillis()

    fun toJson(): JSONObject = JSONObject().put("id", id).put("hour", hour).put("minute", minute).put("label", label)
        .put("days", JSONArray(days)).put("enabled", enabled).put("snooze", snooze).put("skipUntil", skipUntil)
        .put("sound", sound).put("vibrate", vibrate).put("snoozeMax", snoozeMax).put("rise", rise).put("skipHolidays", skipHolidays)
        .put("sunrise", sunrise).put("task", task).put("taskLevel", taskLevel).put("code", code).put("wakeCheck", wakeCheck)
        .put("preNotify", preNotify).put("speak", speak).put("date", date).put("every", every).put("anchor", anchor).put("until", until)

    companion object {
        private const val KEY = "alarms"
        /** Bir daha çalmayacak alarmın "sıradaki çalma"sı */
        const val NEVER = Long.MAX_VALUE
        fun parse(iso: String): LocalDate? = if (iso.isEmpty()) null else runCatching { LocalDate.parse(iso) }.getOrNull()
        val WEEKDAYS = listOf(1, 2, 3, 4, 5)
        val SNOOZE_CHOICES = listOf(0, 5, 10, 15, 20, 30)
        val SNOOZE_MAX_CHOICES = listOf(0, 1, 2, 3, 5)
        val SUNRISE_CHOICES = listOf(0, 10, 20, 30)
        val PRE_NOTIFY_CHOICES = listOf(0, 15, 30, 60)

        /** Gün doğumu ışığı seçili sıradaki alarm (tam ekran saat için): an ve pencere (dakika). Alarmdan 15 dk sonrasına
         *  kadar ışık yanık kaldığı için biraz geriden bakılır. */
        fun nextSunrise(nowMs: Long = System.currentTimeMillis()): Pair<Long, Int>? =
            load().filter { it.enabled && it.sunrise > 0 }
                .map { it.nextAt(nowMs - tr.bumba.time.core.SunriseLight.AFTER_MS) to it.sunrise }.filter { it.first != NEVER }
                .minByOrNull { it.first }

        /** Tam gün resmî tatil mi (takvim verisi; testte değiştirilebilir) */
        var isHoliday: (java.time.LocalDate) -> Boolean = { d ->
            runCatching { AppData.events[d]?.any { it.type == "holiday" } == true }.getOrDefault(false)
        }

        fun fromJson(o: JSONObject): AlarmItem {
            val days = o.optJSONArray("days")?.let { a -> (0 until a.length()).map { a.getInt(it) } } ?: when (o.optString("repeats", "once")) {
                "every" -> (1..7).toList()
                "weekdays" -> WEEKDAYS
                else -> emptyList()
            }
            return AlarmItem(
                o.getString("id"), o.getInt("hour"), o.getInt("minute"), o.optString("label"), days,
                o.optBoolean("enabled", true), o.optInt("snooze", 5), o.optLong("skipUntil", 0),
                o.optString("sound", ""), o.optBoolean("vibrate", true), o.optInt("snoozeMax", 0), o.optBoolean("rise", false),
                o.optBoolean("skipHolidays", false), o.optInt("sunrise", 0),
                o.optString("task", "none"), o.optInt("taskLevel", 2), o.optString("code", ""), o.optBoolean("wakeCheck", false),
                o.optInt("preNotify", 0), o.optBoolean("speak", false),
                o.optString("date", ""), o.optInt("every", 1), o.optString("anchor", ""), o.optString("until", ""),
            )
        }

        /** Arayüzsüz yeni alarm (asistan ve başka uygulamalar, ClockIntentActivity): kayıt + kurulum */
        fun addNew(c: android.content.Context, item: AlarmItem): List<AlarmItem> {
            val list = (load() + item).sortedWith(compareBy({ it.hour }, { it.minute }))
            save(list)
            AlarmScheduler.sync(c, list)
            return list
        }

        fun load(): List<AlarmItem> = runCatching {
            val a = JSONArray(Prefs.store.getString(KEY, "[]"))
            (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())

        fun save(items: List<AlarmItem>) {
            Prefs.store.edit().putString(KEY, JSONArray(items.map { it.toJson() }).toString()).apply()
        }

        /** Ertelenmiş alarmın kimliğinden asıl alarm */
        fun find(id: String): AlarmItem? = load().firstOrNull { it.id == id.removePrefix(AlarmScheduler.SNOOZE) }
    }
}

/**
 * Alarm, zamanlayıcı ve pomodoro kurulumu (iOS'taki AlarmCenter). AlarmManager.setAlarmClock ile tam zamanında,
 * uygulama kapalıyken de çalar; telefon yeniden başlayınca ya da saat/dilim değişince yeniden kurulur
 * (RescheduleReceiver). Tam zamanlı alarm izni (Android 12+) yoksa yaklaşık zamanlı kurulur ve ekranda uyarı çıkar.
 * Çalma: alarm kanalında, alarm sesiyle ve sürekli (FLAG_INSISTENT) bildirim + kilit ekranında AlarmActivity.
 * Kademeli seste bildirim sessiz kanaldan gelir, ses AlarmRingService'te yükselerek çalar (kanal sesi yükseltilemez).
 * Erteleme sınırı: kullanılan erteleme sayısı "alarm.snoozed.<id>" kaydında; kapatınca ya da yeni çalmada sıfırlanır.
 */
object AlarmScheduler {
    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_TOOLS = "tools"
    const val ACTION_FIRE = "tr.bumba.time.ALARM_FIRE"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_KIND = "kind"
    /** Ertelenen alarmın kimlik öneki */
    const val SNOOZE = "snooze-"
    const val SNOOZE_MIN = 5

    /** Tek seferlik kayıtlar: zamanlayıcı ve pomodoro bitişi */
    const val TIMER = "timer"
    const val POMODORO = "pomodoro"

    fun canExact(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun canNotify(c: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ALARM) == null) {
            val ch = NotificationChannel(CHANNEL_ALARM, L("tools.hub.cards.alarm.name"), NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            ch.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CHANNEL_TOOLS) == null) {
            val ch = NotificationChannel(CHANNEL_TOOLS, L("tab.tools"), NotificationManager.IMPORTANCE_HIGH)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    /** Kademeli seste bildirimin sessiz kanalı (ses ve titreşim AlarmRingService'te) */
    private fun silentChannel(c: Context): String {
        val id = "alarm-silent"
        val nm = c.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(id) == null) {
            val ch = NotificationChannel(id, L("tools.hub.cards.alarm.name") + " · " + L("alarm.rise"), NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
        return id
    }

    /** Alarmın kanalı: varsayılan ses ve titreşimde "alarm"; başka ses ya da titreşimsiz için ayrı kanal */
    fun channelFor(c: Context, item: AlarmItem?): String {
        if (item == null || (item.sound.isEmpty() && item.vibrate) || Build.VERSION.SDK_INT < 26) return CHANNEL_ALARM
        val id = "alarm-" + Integer.toHexString((item.sound + "|" + item.vibrate).hashCode())
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            val uri = item.sound.takeIf { it.isNotEmpty() }?.let(android.net.Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ch = NotificationChannel(id, L("tools.hub.cards.alarm.name") + " · " + soundTitle(c, item.sound), NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(uri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            ch.enableVibration(item.vibrate)
            if (item.vibrate) ch.vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            ch.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
        return id
    }

    /** Zil sesinin adı (boşsa "Varsayılan alarm sesi") */
    fun soundTitle(c: Context, sound: String): String =
        if (sound.isEmpty()) L("alarm.soundDefault")
        else runCatching { RingtoneManager.getRingtone(c, android.net.Uri.parse(sound))?.getTitle(c) }.getOrNull() ?: L("alarm.soundDefault")

    private fun pending(c: Context, id: String, title: String, kind: String, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            c, id.hashCode(),
            Intent(c, AlarmReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_KIND, kind),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun set(c: Context, atMs: Long, pi: PendingIntent, clock: Boolean) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (!canExact(c)) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
            return
        }
        if (clock) {
            val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(atMs, show), pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    /** Alarm listesini kurar: kapalı ya da silinmiş olanlar iptal edilir */
    fun sync(c: Context, items: List<AlarmItem> = AlarmItem.load()) {
        val am = c.getSystemService(AlarmManager::class.java)
        val known = Prefs.store.getStringSet("alarms.scheduled", emptySet())!!
        for (id in known) pending(c, id, "", "alarm", PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
        val now = System.currentTimeMillis()
        for (it in items.filter { it.enabled }) {
            val at = it.nextAt(now)
            if (at == AlarmItem.NEVER) continue
            set(c, at, pending(c, it.id, it.title, "alarm", PendingIntent.FLAG_UPDATE_CURRENT)!!, clock = true)
        }
        Prefs.store.edit().putStringSet("alarms.scheduled", items.filter { it.enabled }.map { it.id }.toSet()).apply()
        AlarmPre.sync(c, items, known)
    }

    /** Tek seferlik çalma (zamanlayıcı, pomodoro, erteleme) */
    fun schedule(c: Context, id: String, atMs: Long, title: String, kind: String) {
        set(c, atMs, pending(c, id, title, kind, PendingIntent.FLAG_UPDATE_CURRENT)!!, clock = kind != POMODORO)
    }

    fun cancel(c: Context, id: String) {
        pending(c, id, "", "", PendingIntent.FLAG_NO_CREATE)?.let { c.getSystemService(AlarmManager::class.java).cancel(it) }
        NotificationManagerCompat.from(c).cancel(id.hashCode())
    }

    /** Uygulama ya da telefon yeniden başlayınca: alarmlar ve süren zamanlayıcı/pomodoro yeniden kurulur */
    fun restoreAll(c: Context) {
        sync(c)
        LiveNotifications.sync(c)
        val now = System.currentTimeMillis()
        for (t in TimerItem.load()) if (t.end > now) schedule(c, t.alarmId, t.end, t.doneTitle, TIMER)
        Prefs.store.getLong("hiit.start", 0).takeIf { it > 0 && Prefs.store.getLong("hiit.paused", 0) == 0L }?.let {
            val end = it + HiitConfig.load().totalSeconds * 1000L
            if (end > now) schedule(c, "hiit", end, L("hiit.done"), TIMER)
        }
        Prefs.store.getLong("pomo.end", 0).takeIf { it > now }?.let {
            val phase = Prefs.store.getString("pomo.phase", "work")!!
            schedule(c, POMODORO, it, T("tools.pomodoro.phaseDone", L("tools.pomodoro.$phase")), POMODORO)
        }
    }

    private fun snoozeKey(id: String) = "alarm.snoozed." + id.removePrefix(SNOOZE)

    /** Bu çalmada erteleme süresi (dk); erteleme kapalıysa ya da sınır dolduysa 0 */
    fun snoozeMinutes(id: String, kind: String): Int {
        if (kind != "alarm") return 0
        val item = AlarmItem.find(id)
        val minutes = item?.snooze ?: SNOOZE_MIN
        val max = item?.snoozeMax ?: 0
        return if (minutes > 0 && (max == 0 || Prefs.store.getInt(snoozeKey(id), 0) < max)) minutes else 0
    }

    /** Çalan alarmın bildirimi: alarm sesi sürekli çalar; kilit ekranında AlarmActivity açılır.
     *  Kademeli seste ses hizmeti başlar (başlatılamazsa normal sesli bildirime dönülür). */
    fun ring(c: Context, id: String, title: String, kind: String) {
        ensureChannels(c)
        if (!canNotify(c)) return
        if (kind == WakeCheck.KIND) { WakeCheck.ask(c, id); return }
        val item = if (kind == "alarm") AlarmItem.find(id) else null
        // Kademeli ses ya da saati sesli söyleme: ses hizmette çalar
        if ((item?.rise == true || item?.speak == true) && AlarmRingService.start(c, id, title, kind)) return
        runCatching { NotificationManagerCompat.from(c).notify(id.hashCode(), notification(c, id, title, kind, silent = false)) }
    }

    /** Çalan alarmın bildirimi (sessiz = kademeli seste, sesi hizmet çalar) */
    fun notification(c: Context, id: String, title: String, kind: String, silent: Boolean): android.app.Notification {
        val nid = id.hashCode()
        val full = PendingIntent.getActivity(
            c, nid,
            Intent(c, AlarmActivity::class.java).putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_KIND, kind)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String) = PendingIntent.getBroadcast(
            c, nid + a.hashCode(),
            Intent(c, AlarmReceiver::class.java).setAction(a).putExtra(EXTRA_ID, id).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val time = Fmt.time(java.time.ZonedDateTime.now())
        val item = if (kind == "alarm") AlarmItem.find(id) else null
        val snoozeMin = snoozeMinutes(id, kind)
        return if (kind == POMODORO) {
            NotificationCompat.Builder(c, CHANNEL_TOOLS)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title)
                .setContentText(L("tools.hub.cards.pomodoro.name"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(c, nid,
                    Intent(c, MainActivity::class.java).putExtra("startTab", Tab.TOOLS.ordinal),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                .build()
        } else {
            val b = NotificationCompat.Builder(c, if (silent) silentChannel(c) else channelFor(c, item))
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setContentTitle(title)
                .setContentText(time)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .setOngoing(true)
            // Görevli alarm bildirimden kapatılamaz: dokununca alarm ekranı (görev) açılır
            if (item != null && item.effectiveTask != "none") b.addAction(0, L("wake.title"), full)
            else b.addAction(0, L("tools.alarm.stop"), action(ACTION_STOP))
            if (silent) b.setSilent(true)
            else b.setSound(item?.sound?.takeIf { it.isNotEmpty() }?.let(android.net.Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                android.media.AudioManager.STREAM_ALARM)
            if (snoozeMin > 0) b.addAction(0, T("alarm.snoozeFor", snoozeMin), action(ACTION_SNOOZE))
            b.build().also { if (!silent) it.flags = it.flags or android.app.Notification.FLAG_INSISTENT }
        }
    }

    const val ACTION_STOP = "tr.bumba.time.ALARM_STOP"
    const val ACTION_SNOOZE = "tr.bumba.time.ALARM_SNOOZE"

    /** Kapat: bildirim ve kademeli ses durur, erteleme sayacı sıfırlanır */
    fun stop(c: Context, id: String) {
        silence(c, id)
        Prefs.store.edit().remove(snoozeKey(id)).apply()
        // Uyanma denetimi: 5 dk sonra soru, 2 dk içinde cevap yoksa yedek alarm
        if (AlarmItem.find(id)?.wakeCheck == true) WakeCheck.schedule(c, id.removePrefix(SNOOZE))
    }

    private fun silence(c: Context, id: String) {
        NotificationManagerCompat.from(c).cancel(id.hashCode())
        AlarmRingService.stop(c, id)
    }

    /** Ertele: sınır dolmadıysa erteleme sayacı artar ve `snooze` dk sonrasına tek seferlik alarm kurulur */
    fun snooze(c: Context, id: String, title: String) {
        val minutes = snoozeMinutes(id, "alarm")
        silence(c, id)
        if (minutes == 0) return
        val key = snoozeKey(id)
        Prefs.store.edit().putInt(key, Prefs.store.getInt(key, 0) + 1).apply()
        val sid = if (id.startsWith(SNOOZE)) id else SNOOZE + id
        schedule(c, sid, System.currentTimeMillis() + minutes * 60_000L, title, "alarm")
    }
}

/** Alarm, zamanlayıcı ve pomodoro anı geldi; bildirim düğmeleri (kapat, ertele) */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: L("tools.alarm.defaultLabel")
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: "alarm"
        when (intent.action) {
            AlarmScheduler.ACTION_STOP -> AlarmScheduler.stop(c, id)
            AlarmScheduler.ACTION_SNOOZE -> AlarmScheduler.snooze(c, id, title)
            WakeCheck.ACTION_AWAKE -> WakeCheck.awake(c, id)
            else -> {
                if (kind == "alarm" && !id.startsWith(AlarmScheduler.SNOOZE)) {
                    // Yeni çalma: erteleme sayacı sıfırlanır
                    Prefs.store.edit().remove("alarm.snoozed.$id").apply()
                    // Bir kezlik alarm kapanır; tekrarlı olanın sonraki çalması kurulur
                    val items = AlarmItem.load().map { if (it.id == id && it.days.isEmpty()) it.copy(enabled = false, skipUntil = 0) else it }
                    AlarmItem.save(items)
                    AlarmScheduler.sync(c, items)
                }
                AlarmScheduler.ring(c, id, title, kind)
                if (kind == PrayerAlarms.KIND) PrayerAlarms.sync(c, java.time.Instant.now().plusSeconds(60))
            }
        }
    }
}

/**
 * Yaklaşan alarm bildirimi (rakip analizi #19; iOS'taki AlarmPreNotify): alarmdan `preNotify` dakika önce sessiz bildirim,
 * "Bu alarmı atla" düğmesi o çalmayı atlar (skipUntil). Her sync'te sıradaki çalma için yeniden kurulur.
 */
object AlarmPre {
    const val ACTION_PRE = "tr.bumba.time.ALARM_PRE"
    const val ACTION_SKIP = "tr.bumba.time.ALARM_PRE_SKIP"
    const val EXTRA_AT = "at"
    private const val CHANNEL = "alarm-pre"

    private fun pending(c: Context, id: String, at: Long, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        c, ("pre-$id").hashCode(),
        Intent(c, AlarmPreReceiver::class.java).setAction(ACTION_PRE).putExtra(AlarmScheduler.EXTRA_ID, id).putExtra(EXTRA_AT, at),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    fun sync(c: Context, items: List<AlarmItem>, known: Set<String>) {
        val am = c.getSystemService(AlarmManager::class.java)
        for (id in known + items.map { it.id }) pending(c, id, 0, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it) }
        val now = System.currentTimeMillis()
        for (it in items) {
            if (!it.enabled || it.preNotify <= 0) continue
            val at = it.nextAt(now)
            if (at == AlarmItem.NEVER) continue
            val fire = at - it.preNotify * 60_000L
            if (fire <= now) continue
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pending(c, it.id, at, PendingIntent.FLAG_UPDATE_CURRENT)!!)
        }
    }

    /** Sessiz bildirim: başlıkta alarmın saati, içinden atlama */
    fun show(c: Context, id: String, at: Long) {
        val item = AlarmItem.find(id) ?: return
        if (!item.enabled || !AlarmScheduler.canNotify(c)) return
        val nm = c.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, L("alarm.pre"), NotificationManager.IMPORTANCE_LOW))
        }
        val skip = PendingIntent.getBroadcast(
            c, ("preskip-$id").hashCode(),
            Intent(c, AlarmPreReceiver::class.java).setAction(ACTION_SKIP).putExtra(AlarmScheduler.EXTRA_ID, id).putExtra(EXTRA_AT, at),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val time = Fmt.time(java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()))
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(T("alarm.preTitle", time))
            .setContentText(item.title + " · " + L("alarm.preBody"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setTimeoutAfter(maxOf(60_000L, at - System.currentTimeMillis()))
            .addAction(0, L("alarm.preSkip"), skip)
            .build()
        runCatching { NotificationManagerCompat.from(c).notify(("pre-$id").hashCode(), n) }
    }

    /** O çalmayı atlar ve alarmları yeniden kurar */
    fun skip(c: Context, id: String, at: Long) {
        val items = AlarmItem.load().map { if (it.id == id) it.copy(skipUntil = at + 60_000L) else it }
        AlarmItem.save(items)
        AlarmScheduler.sync(c, items)
        NotificationManagerCompat.from(c).cancel(("pre-$id").hashCode())
    }
}

class AlarmPreReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val at = intent.getLongExtra(AlarmPre.EXTRA_AT, 0L)
        when (intent.action) {
            AlarmPre.ACTION_PRE -> AlarmPre.show(c, id, at)
            AlarmPre.ACTION_SKIP -> AlarmPre.skip(c, id, at)
        }
    }
}

/** Telefon açılınca, uygulama güncellenince, saat ya da saat dilimi değişince alarmlar yeniden kurulur */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        AlarmScheduler.restoreAll(c)
    }
}
