package tr.bumba.time

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import tr.bumba.time.core.PrayerTimes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Kapsayıcı takvimler (9 Kasım 2026, sahibin onayı; sözleşme v1): Hristiyan, Yahudi, Çin, İran, Hindu… takvimlerinin
 * özel günleri ve ikinci tarihleri. Hesap yapım anında sitedeki JS modüllerinde yapılır (lib/calendars/<id>.mjs);
 * gen-strings.mjs `assets/calendars.json`'u ve `cals.<id>.*` metinlerini yazar, burada yalnız okunur. Dosya yoksa özellik
 * görünmez. Kod takvimden bağımsızdır (yeni takvim yalnız veriyle gelir); yalnız konuma bağlı küçük eklemeler `hooks`'ta.
 *
 * Seçim `cal.extra` (virgülle açık takvim kimlikleri; iOS ile aynı anahtar, yalnız kullanıcı değiştirince yazılır).
 * Kayıt yoksa: dinî olmayan takvimler telefonun (sistemin) bölgesi `regions` içindeyse açık, dinî takvimler kapalı:
 * kimsenin inancı tahmin edilmez. Açık takvimlerin olayları `calEvents`'e tür `cal:<id>` ile eklenir; böylece takvim
 * ızgarası, gün ayrıntısı, ajanda, yıl görünümü, bildirim, günaydın kartı ve widget kendiliğinden gösterir.
 *
 * Metinler: ad `cals.<id>.name`, açıklama `.desc`, olay `.ev.<anahtar>`, ay adları `.month.<n>` (1'den; dizi 0'dan
 * yazılmışsa `.month.0` vardır ve ona göre okunur), artık ay `.leapFmt` ({m}), ikinci tarih `.fmt` ({d} {m} {y}; Türkçe
 * metinde bu sırayla, gen-strings sırayı Türkçeden alır), yıl etiketi `.yearFmt` (tek yer tutucu), günlük kod
 * `.daily.<kod>` (yoksa "t5n12" gibi bileşik kodun parçaları ayrı ayrı), kaynak notu `.note`.
 */
object ExtraCals {
    const val KEY = "cal.extra"
    private val DEFAULT_COLOR = Color(0xFF6B7C93)

    /** Olay: başlangıç, ad anahtarı, isteğe bağlı bitiş (çok günlü; her günde görünür) */
    class Ev(val start: LocalDate, val key: String, val end: LocalDate?)

    /** Ay tablosu satırı: miladi ay başı, yıl, ay (1'den), artık/tekrar ay */
    class MonthRow(val start: LocalDate, val year: Int, val month: Int, val leap: Boolean)

    /** İkinci tarih: gün, ay, yıl ve artık (tekrar) ay işareti */
    data class SecondDate(val day: Int, val month: Int, val year: Int, val leap: Boolean)

    class Cal(
        val id: String, val group: String, val regions: Set<String>, val color: Color,
        val byDay: Map<LocalDate, List<Ev>>, val months: List<MonthRow>?,
        private val dailyByDate: Map<LocalDate, String>, private val dailyByYear: Map<Int, List<String>>,
        val years: Map<Int, String>,
    ) {
        val religious: Boolean get() = group == "religious"

        /** Günlük kod: tarih → kod sözlüğünden ya da yıl başına diziden (yılın gününe göre, 0'dan) */
        fun daily(d: LocalDate): String? =
            (dailyByDate[d] ?: dailyByYear[d.year]?.getOrNull(d.dayOfYear - 1))?.ifEmpty { null }
    }

    /** Bildirim, widget gibi takvim ekranı dışındaki yerler için (AppData.init yazar) */
    var appContext: Context? = null
    private var loaded = false
    private var cals: List<Cal> = emptyList()
    /** Kullanıcının seçimi (yoksa null: bölgeye göre varsayılan); değişince ekranlar yeniden çizilir */
    private var chosen by mutableStateOf<Set<String>?>(null)

    /** Testler için: telefonun bölgesi ve `cals.*` metinleri (platform biçiminde, %1$s) */
    var regionOverride: String? = null
    var textOverride: Map<String, String>? = null

    @Synchronized
    fun ensure() {
        if (loaded) return
        val c = appContext ?: WorldHol.appContext ?: return
        loaded = true
        cals = runCatching { c.assets.open("calendars.json").bufferedReader().use { parse(it.readText()) } }.getOrDefault(emptyList())
        chosen = readChosen()
    }

    /** Veriyi metinden yükler (testlerde örnek dosya) */
    fun loadJson(text: String) {
        loaded = true
        cals = parse(text)
        chosen = readChosen()
    }

    /** Testler için: yüklenen veriyi ve geçersiz kılmaları unutur (sonraki ensure() varlık dosyasını okur) */
    fun reset() {
        loaded = false; cals = emptyList(); chosen = null; regionOverride = null; textOverride = null
    }

    private fun readChosen(): Set<String>? =
        if (Prefs.ready && Prefs.store.contains(KEY)) (Prefs.store.getString(KEY, "") ?: "").split(",").filter { it.isNotEmpty() }.toSet() else null

    fun parse(text: String): List<Cal> {
        val cs = JSONObject(text).optJSONObject("cals") ?: return emptyList()
        return cs.keys().asSequence().mapNotNull { id -> runCatching { parseCal(id, cs.getJSONObject(id)) }.getOrNull() }.sortedBy { it.id }.toList()
    }

    private fun date(s: String?): LocalDate? = s?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun parseCal(id: String, c: JSONObject): Cal {
        val regions = c.optJSONArray("regions")?.let { a -> (0 until a.length()).map { a.getString(it).uppercase() }.toSet() } ?: emptySet()
        val color = runCatching { Color(android.graphics.Color.parseColor(c.getString("color"))) }.getOrDefault(DEFAULT_COLOR)
        val byDay = HashMap<LocalDate, MutableList<Ev>>()
        c.optJSONArray("events")?.let { a ->
            for (i in 0 until a.length()) {
                val r = a.optJSONArray(i) ?: continue
                val start = date(r.optString(0)) ?: continue
                val end = if (r.length() > 2 && !r.isNull(2)) date(r.optString(2))?.takeIf { it.isAfter(start) } else null
                val ev = Ev(start, r.optString(1), end)
                var d = start
                var n = 0
                // Çok günlü olay her gününde (en çok 60 gün; bozuk veride sonsuz döngü olmasın)
                while (!d.isAfter(end ?: start) && n++ < 60) {
                    byDay.getOrPut(d) { ArrayList() }.add(ev)
                    d = d.plusDays(1)
                }
            }
        }
        val months = c.optJSONArray("months")?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                val r = a.optJSONArray(i) ?: return@mapNotNull null
                val start = date(r.optString(0)) ?: return@mapNotNull null
                MonthRow(start, r.optInt(1), r.optInt(2), r.optInt(3, 0) != 0)
            }.sortedBy { it.start }
        }?.takeIf { it.size >= 2 }
        val byDate = HashMap<LocalDate, String>()
        val byYear = HashMap<Int, List<String>>()
        c.optJSONObject("daily")?.let { o ->
            for (k in o.keys()) {
                val arr = o.optJSONArray(k)
                if (arr != null) k.toIntOrNull()?.let { y -> byYear[y] = (0 until arr.length()).map { arr.optString(it) } }
                else date(k)?.let { byDate[it] = o.optString(k) }
            }
        }
        val years = HashMap<Int, String>()
        c.optJSONObject("years")?.let { o ->
            for (k in o.keys()) {
                val y = k.toIntOrNull() ?: continue
                val v = o.opt(k)
                years[y] = when (v) {
                    is Int, is Long -> v.toString()
                    is Number -> if (v.toDouble() % 1.0 == 0.0) v.toLong().toString() else v.toString()
                    else -> v.toString()
                }
            }
        }
        return Cal(id, c.optString("group", "cultural"), regions, color, byDay, months, byDate, byYear, years)
    }

    /** Bütün takvimler (kimliğe göre sıralı; olayların sırası iOS ile aynı) */
    val all: List<Cal> get() { ensure(); return cals }

    /** Ayarlarda gösterim sırası: resmî, kültürel, dinî; her grupta ada göre */
    fun sorted(): List<Cal> {
        val order = listOf("civil", "cultural", "religious")
        val collator = java.text.Collator.getInstance(Strings.locale)
        return all.sortedWith(compareBy<Cal> { order.indexOf(it.group).let { i -> if (i < 0) order.size else i } }
            .thenComparator { a, b -> collator.compare(name(a.id), name(b.id)) })
    }

    fun find(id: String): Cal? = all.firstOrNull { it.id == id }

    /** Telefonun (sistemin) bölgesi; uygulama içi dil seçimi etkilemez */
    fun systemRegion(): String = regionOverride
        ?: runCatching { android.content.res.Resources.getSystem().configuration.locales.get(0).country.uppercase() }.getOrDefault("")

    /** Kayıt yokken açık takvimler: dinî olmayanlar bölgesinde; dinîler hiçbir yerde */
    fun defaults(region: String = systemRegion()): Set<String> =
        all.filter { !it.religious && region.isNotEmpty() && region in it.regions }.map { it.id }.toSet()

    /** Açık takvimler (yalnız verisi olanlar) */
    val enabled: Set<String>
        get() {
            ensure()
            val ids = chosen ?: return defaults()
            return cals.map { it.id }.filter { it in ids }.toSet()
        }

    fun isOn(id: String): Boolean = id in enabled

    /** Aç/kapa: seçimin tamamı yazılır (bilinmeyen kimlikler korunur; ileride gelen takvim kaybolmasın) */
    fun set(id: String, on: Boolean) {
        ensure()
        val base = chosen ?: defaults()
        val next = if (on) base + id else base - id
        chosen = next
        if (Prefs.ready) Prefs.store.edit().putString(KEY, next.sorted().joinToString(",")).apply()
    }

    /** Açık takvimlerin bu günkü olayları (çok günlüler her gününde; `date` başlangıç günüdür) */
    fun events(d: LocalDate): List<CalEvent> {
        ensure()
        if (cals.isEmpty()) return emptyList()
        val on = enabled
        if (on.isEmpty()) return emptyList()
        return cals.filter { it.id in on }.flatMap { c ->
            c.byDay[d].orEmpty().map { e -> CalEvent(e.start, "cal:${c.id}:${e.key}", "cal:${c.id}", e.end, eventName(c.id, e.key)) }
        }
    }

    fun color(id: String): Color = find(id)?.color ?: DEFAULT_COLOR

    // --- Metinler ---

    /** `cals.*` metni; yoksa null */
    fun text(key: String): String? {
        textOverride?.let { return it[key] }
        return if (Strings.id(key) != 0) L(key) else null
    }

    /** Yer tutuculu `cals.*` metni (sıra Türkçedeki gibi); yoksa null */
    fun format(key: String, vararg args: Any): String? {
        textOverride?.let { m -> return m[key]?.let { String.format(Strings.locale, it, *args.map { a -> a.toString() }.toTypedArray()) } }
        return if (Strings.id(key) != 0) T(key, *args) else null
    }

    fun name(id: String): String = text("cals.$id.name") ?: id
    fun desc(id: String): String? = text("cals.$id.desc")
    fun note(id: String): String? = text("cals.$id.note")
    fun eventName(id: String, key: String): String = text("cals.$id.ev.$key") ?: key

    /** Ay adı: ay numarası 1'den; metin dizisi 0'dan yazılmışsa (`.month.0` varsa) bir eksiği */
    fun monthName(id: String, month: Int): String? {
        val zeroBased = text("cals.$id.month.0") != null
        return text("cals.$id.month.${if (zeroBased) month - 1 else month}")
    }

    /** Ay tablosundan ikinci tarih; son satır yalnız bitiş sınırıdır (son ayın uzunluğu bilinmez) */
    fun secondDate(c: Cal, d: LocalDate): SecondDate? {
        val m = c.months ?: return null
        var lo = 0
        var hi = m.size - 1
        var idx = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (!m[mid].start.isAfter(d)) { idx = mid; lo = mid + 1 } else hi = mid - 1
        }
        if (idx < 0 || idx == m.size - 1) return null
        val r = m[idx]
        return SecondDate((d.toEpochDay() - r.start.toEpochDay()).toInt() + 1, r.month, r.year, r.leap)
    }

    /** "15 Nisan 5787" gibi (takvimin `fmt` biçimiyle; artık ay `leapFmt` ile) */
    fun dateText(c: Cal, s: SecondDate): String {
        val base = monthName(c.id, s.month) ?: s.month.toString()
        val m = if (s.leap) format("cals.${c.id}.leapFmt", base) ?: base else base
        return format("cals.${c.id}.fmt", s.day, m, s.year) ?: "${s.day} $m ${s.year}"
    }

    /** Günlük kodun adı: önce kodun kendisi, yoksa bileşik kodun parçaları ("t5n12" → t5, n12) */
    fun dailyText(c: Cal, d: LocalDate): String? {
        val code = c.daily(d) ?: return null
        text("cals.${c.id}.daily.$code")?.let { return it }
        val parts = Regex("[A-Za-z]+\\d+").findAll(code).map { it.value }.toList()
        if (parts.size < 2) return null
        return parts.mapNotNull { text("cals.${c.id}.daily.$it") }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** Yıl etiketi ("Reiwa 9" gibi; `yearFmt` yoksa gösterilmez) */
    fun yearText(c: Cal, d: LocalDate): String? = c.years[d.year]?.let { format("cals.${c.id}.yearFmt", it) }

    /**
     * Konuma bağlı küçük eklemeler (takvim başına isteğe bağlı; yeni takvim için gerekmez). Metin anahtarı yoksa
     * hiçbir şey gösterilmez. Yahudi takviminde cuma Şabat mumu (gün batımı − 18 dk), cumartesi Havdala (Güneş −8,5°).
     */
    val hooks: Map<String, (LocalDate, AppPlace) -> List<String>> = mapOf(
        "jewish" to { d, place ->
            when (d.dayOfWeek) {
                DayOfWeek.FRIDAY -> listOfNotNull(sunTime(d, place, -0.833, -18)?.let { format("cals.jewish.candles", it) })
                DayOfWeek.SATURDAY -> listOfNotNull(sunTime(d, place, -8.5, 0)?.let { format("cals.jewish.havdalah", it) })
                else -> emptyList()
            }
        },
    )

    /** Güneş'in akşam [altitude] dereceye indiği an (+ dakika), yerin saatiyle "HH:mm"; oluşmuyorsa null */
    fun sunTime(d: LocalDate, place: AppPlace, altitude: Double, plusMinutes: Int): String? {
        val offset = place.zone.rules.getOffset(d.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC)).totalSeconds / 3600.0
        val h = PrayerTimes.sunAltitudeTimes(d.year, d.monthValue, d.dayOfMonth, place.lat, place.lon, offset, altitude).set ?: return null
        val at = d.atStartOfDay(place.zone).plusSeconds(Math.round(h * 3600) + plusMinutes * 60L)
        return Fmt.time(at)
    }

    /** Gün ayrıntısındaki tek satır: ikinci tarih · günlük kod · yıl etiketi · konuma bağlı eklemeler */
    fun dayLine(c: Cal, d: LocalDate, place: AppPlace? = null): String? {
        val parts = listOfNotNull(
            secondDate(c, d)?.let { dateText(c, it) },
            dailyText(c, d),
            yearText(c, d),
        ) + (place?.let { p -> runCatching { hooks[c.id]?.invoke(d, p) }.getOrNull() }.orEmpty())
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** Açık takvimlerin gün satırları (takvim sırasıyla) */
    fun dayLines(d: LocalDate, place: AppPlace? = null): List<Pair<Cal, String>> {
        val on = enabled
        if (on.isEmpty()) return emptyList()
        return sorted().filter { it.id in on }.mapNotNull { c -> dayLine(c, d, place)?.let { c to it } }
    }
}

/** Gün ayrıntısında açık takvimlerin ikinci tarihleri: her takvim tek satır, küçük, takvimin rengiyle nokta */
@Composable
fun ExtraCalDayLines(d: LocalDate) {
    val place = AppPlace.current()
    for ((c, line) in ExtraCals.dayLines(d, place)) {
        val name = ExtraCals.name(c.id)
        Row(Modifier.semantics { contentDescription = "$name: $line" }, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(c.color, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(line, color = Theme.c.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Takvimler" bölümü (Takvimler ve ülke penceresinde): her takvim için ad, bir cümle açıklama ve aç/kapa */
@Composable
fun ExtraCalsSection() {
    val list = ExtraCals.sorted()
    if (list.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(L("calendars.heading"), color = Theme.c.text, fontSize = if (AppFonts.serif) 19.sp else 18.sp,
            fontFamily = AppFonts.heading, fontWeight = FontWeight.Medium)
        for (c in list) {
            val on = ExtraCals.isOn(c.id)
            Row(
                Modifier.fillMaxWidth().clickable { ExtraCals.set(c.id, !on) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    // Renk noktası adın hizasında (açıklama altında girintili)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(c.color, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(ExtraCals.name(c.id), color = Theme.c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    ExtraCals.desc(c.id)?.let {
                        Text(it, Modifier.padding(start = 20.dp), color = Theme.c.muted, fontSize = 12.sp, lineHeight = 16.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(8.dp))
                SoftSwitch(on, { ExtraCals.set(c.id, it) })
            }
        }
        Text(L("calendars.note"), color = Theme.c.muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
        // Takvimlerin kaynak ve bölge notları "Nasıl çalışır?" alt sayfasında (ekranda iki satırdan uzun not kalmaz)
        val notes = list.mapNotNull { c -> ExtraCals.note(c.id)?.let { ExtraCals.name(c.id) to it } }
        if (notes.isNotEmpty()) HowItWorks(notes.map { (a, b) -> Pair<String?, String>(a, b) })
    }
}
