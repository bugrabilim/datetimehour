package tr.bumba.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.IsoFields
import java.util.Locale

/** Biçimler: saatler her dilde Latin rakamıyla (sitedeki gibi), tarihler uygulamanın dilinde ve Gregoryen */
object Fmt {
    private val hm = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val hms = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
    private val h12m = DateTimeFormatter.ofPattern("h:mm", Locale.ROOT)
    private val h12ms = DateTimeFormatter.ofPattern("h:mm:ss", Locale.ROOT)

    fun time(t: ZonedDateTime, seconds: Boolean = false): String =
        t.format(if (Prefs.h12) (if (seconds) h12ms else h12m) else (if (seconds) hms else hm))

    fun time(i: Instant, zone: ZoneId, seconds: Boolean = false): String = time(i.atZone(zone), seconds)

    /** 24 saat, her zaman (vakit listeleri) */
    fun hm(i: Instant, zone: ZoneId): String = i.atZone(zone).format(hm)

    fun ampm(t: ZonedDateTime): String = if (!Prefs.h12) "" else if (t.hour < 12) "AM" else "PM"

    /** "18 Ekim 2026 Pazar" */
    fun longDate(d: LocalDate): String =
        d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Strings.locale))

    /** "18 Eki 2026" */
    fun shortDate(d: LocalDate): String =
        d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Strings.locale))

    fun shortDateTime(i: Instant, zone: ZoneId): String = shortDate(i.atZone(zone).toLocalDate()) + " " + hm(i, zone)

    fun month(m: Int): String = L("calendar.months.${m - 1}")

    /** Pazartesi 0 … Pazar 6 (sitedeki weekdaysShort sırası) */
    fun weekdayShort(d: LocalDate): String = L("calendar.weekdaysShort.${d.dayOfWeek.value - 1}")

    fun isoWeek(d: LocalDate): Int = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    /** Saat:dakika:saniye süre */
    fun duration(seconds: Long): String {
        val s = maxOf(0, seconds)
        return if (s >= 3600) "%d:%02d:%02d".format(Locale.ROOT, s / 3600, (s % 3600) / 60, s % 60)
        else "%02d:%02d".format(Locale.ROOT, s / 60, s % 60)
    }

    /** Geri sayım: her zaman SS:DD:ss */
    fun countdown(seconds: Long): String {
        val s = maxOf(0, seconds)
        return "%02d:%02d:%02d".format(Locale.ROOT, s / 3600, (s % 3600) / 60, s % 60)
    }

    /** "+3 sa" gibi fark */
    /** Yüzde: Türkçe ve Azericede "%40", diğerlerinde "40%" */
    fun percent(n: Int): String = if (Strings.lang == "tr" || Strings.lang == "az") "%$n" else "$n%"

    fun hm(seconds: Int): String {
        val m = kotlin.math.abs(seconds) / 60
        return if (m % 60 == 0) "${m / 60} ${L("client.hourShort")}"
        else "${m / 60} ${L("client.hourShort")} ${m % 60} ${L("client.minuteShort")}"
    }
}
