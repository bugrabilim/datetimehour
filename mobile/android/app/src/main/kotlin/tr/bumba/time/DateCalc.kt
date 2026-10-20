package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Tarih hesaplama kuralları (iOS'taki SaatCore/DateCalc.swift ve sitedeki src/js/dates.js ile aynı):
 * iş günü hafta sonu 0, resmî tatil 0, arife (yarım gün) 0,5, diğer günler 1.
 */
object DateCalc {
    data class Ymd(val years: Int, val months: Int, val days: Int)

    fun days(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()

    /** Ay ekler; hedef ayda o gün yoksa ayın son günü (LocalDate.plusMonths da böyle davranır) */
    fun addMonths(d: LocalDate, n: Int): LocalDate = d.plusMonths(n.toLong())

    /** a'dan b'ye tamamlanan yıl, ay, gün (a ≤ b): önce sığan en çok ay eklenir, kalan gündür (31 Ocak → 1 Mart = 1 ay 1 gün) */
    fun ymd(a: LocalDate, b: LocalDate): Ymd {
        var total = (b.year - a.year) * 12 + (b.monthValue - a.monthValue)
        if (addMonths(a, total).isAfter(b)) total -= 1
        return Ymd(total / 12, total % 12, days(addMonths(a, total), b))
    }

    /** Tatil ve yarım gün ağırlıkları (ISO tarih → 0 ya da 0,5); takvim verisinden */
    val offDays: Map<LocalDate, Double> by lazy {
        val out = HashMap<LocalDate, Double>()
        for ((day, list) in AppData.events) {
            for (e in list) {
                if (e.date != day) continue
                if (e.type == "holiday") out[day] = 0.0 else if (e.type == "half" && day !in out) out[day] = 0.5
            }
        }
        out
    }

    fun workWeight(d: LocalDate, off: Map<LocalDate, Double> = offDays): Double =
        if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) 0.0 else off[d] ?: 1.0

    /** [a, b) arasındaki iş günü (başlangıç dahil, bitiş hariç) */
    fun workdays(a: LocalDate, b: LocalDate, off: Map<LocalDate, Double> = offDays): Double {
        var w = 0.0
        var d = a
        while (d.isBefore(b)) {
            w += workWeight(d, off)
            d = d.plusDays(1)
        }
        return w
    }

    /** Tarihe n iş günü ekler (negatifse çıkarır); ağırlığı sıfır olmayan her gün bir sayılır */
    fun addWorkdays(a: LocalDate, n: Int, off: Map<LocalDate, Double> = offDays): LocalDate {
        var cur = a
        var left = kotlin.math.abs(n)
        val step = if (n < 0) -1L else 1L
        while (left > 0) {
            cur = cur.plusDays(step)
            if (workWeight(cur, off) > 0) left--
        }
        return cur
    }

    /** Sonraki doğum günü (bugünse bugün); 29 Şubat artık olmayan yılda 28 Şubat */
    fun nextBirthday(birth: LocalDate, today: LocalDate): LocalDate {
        fun inYear(y: Int) = LocalDate.of(y, birth.monthValue, 1).let { it.withDayOfMonth(minOf(birth.dayOfMonth, it.lengthOfMonth())) }
        val thisYear = inYear(today.year)
        return if (!thisYear.isBefore(today)) thisYear else inYear(today.year + 1)
    }
}

/** Binlik ayraçlı sayı (Arapçada da Latin rakamı) */
private fun num(n: Int): String = NumberFormat.getIntegerInstance(if (Strings.lang == "ar") java.util.Locale.US else Strings.locale).format(n)

/** Tarih hesaplama (iOS'taki DateCalcView): iki tarih arası, tarihe ekle/çıkar, yaş */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalcScreen() {
    val today = LocalDate.now()
    var start by rememberSaveable { mutableStateOf(today.toString()) }
    var finish by rememberSaveable { mutableStateOf(today.plusDays(30).toString()) }
    var base by rememberSaveable { mutableStateOf(today.toString()) }
    var amount by rememberSaveable { mutableStateOf("30") }
    var unit by rememberSaveable { mutableStateOf("days") }
    var birth by rememberSaveable { mutableStateOf(today.minusYears(30).toString()) }
    var picking by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AppCard {
            SectionTitle(L("datecalc.diffHeading"))
            DateRow(L("datecalc.startLabel"), LocalDate.parse(start)) { picking = "start" }
            DateRow(L("datecalc.endLabel"), LocalDate.parse(finish)) { picking = "finish" }
            HorizontalDivider(color = Theme.c.border.copy(alpha = 0.5f))
            val x = LocalDate.parse(start)
            val y = LocalDate.parse(finish)
            val a = if (x.isBefore(y)) x else y
            val b = if (x.isBefore(y)) y else x
            val n = DateCalc.days(a, b)
            val r = DateCalc.ymd(a, b)
            Text(T("datecalc.diffDays", num(n)), color = Theme.c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(T("datecalc.diffWeeks", n / 7, n % 7), color = Theme.c.text)
            Text(T("datecalc.diffYmd", r.years, r.months, r.days), color = Theme.c.text)
            Text(T("datecalc.diffWork", workText(DateCalc.workdays(a, b))), color = Theme.c.text)
            Text(L("datecalc.diffNote"), color = Theme.c.muted, fontSize = 12.sp)
        }
        AppCard {
            SectionTitle(L("datecalc.addHeading"))
            DateRow(L("datecalc.startLabel"), LocalDate.parse(base)) { picking = "base" }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L("datecalc.amountLabel"), Modifier.weight(1f), color = Theme.c.text)
                IconButton(onClick = { amount = ((amount.toIntOrNull() ?: 0) - 1).coerceIn(-3650, 3650).toString() }) {
                    Icon(Icons.Filled.Remove, null, tint = Theme.c.accent)
                }
                OutlinedTextField(
                    amount, { v -> amount = v.filterIndexed { i, ch -> ch.isDigit() || (i == 0 && ch == '-') }.take(5) },
                    Modifier.width(96.dp), singleLine = true,
                    textStyle = Tnum.copy(textAlign = TextAlign.Center, color = Theme.c.text),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                IconButton(onClick = { amount = ((amount.toIntOrNull() ?: 0) + 1).coerceIn(-3650, 3650).toString() }) {
                    Icon(Icons.Filled.Add, null, tint = Theme.c.accent)
                }
            }
            Text(L("datecalc.unitLabel"), color = Theme.c.muted, fontSize = 13.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (u in listOf("days", "weeks", "months", "years", "workdays")) {
                    val on = u == unit
                    Box(
                        Modifier.background(if (on) Theme.c.accent else Theme.c.surface2, RoundedCornerShape(50))
                            .clickable { unit = u }.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(L("datecalc.units.$u"), color = if (on) Theme.c.bg else Theme.c.text, fontSize = 14.sp) }
                }
            }
            val n = (amount.toIntOrNull() ?: 0).coerceIn(-3650, 3650)
            val d = LocalDate.parse(base)
            val result = when (unit) {
                "weeks" -> d.plusDays(n * 7L)
                "months" -> DateCalc.addMonths(d, n)
                "years" -> DateCalc.addMonths(d, n * 12)
                "workdays" -> DateCalc.addWorkdays(d, n)
                else -> d.plusDays(n.toLong())
            }
            Text(T("datecalc.addResult", Fmt.longDate(result)), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(L("datecalc.addHint"), color = Theme.c.muted, fontSize = 12.sp)
        }
        AppCard {
            SectionTitle(L("datecalc.ageHeading"))
            val b = LocalDate.parse(birth)
            DateRow(L("datecalc.birthLabel"), b) { picking = "birth" }
            val r = DateCalc.ymd(b, today)
            val next = DateCalc.nextBirthday(b, today)
            Text(T("datecalc.ageResult", r.years, r.months, r.days), color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(T("datecalc.ageDays", num(DateCalc.days(b, today))), color = Theme.c.text)
            Text(
                if (next == today) L("datecalc.ageToday") else T("datecalc.ageNext", Fmt.longDate(next), DateCalc.days(today, next)),
                color = Theme.c.text,
            )
        }
        val years = AppData.events.keys.map { it.year }
        Text(T("datecalc.range", years.minOrNull() ?: 2026, years.maxOrNull() ?: 2026), color = Theme.c.muted, fontSize = 12.sp)
    }

    picking?.let { which ->
        val current = LocalDate.parse(when (which) { "start" -> start; "finish" -> finish; "base" -> base; else -> birth })
        val state = rememberDatePickerState(initialSelectedDateMillis = current.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        val v = (if (which == "birth" && d.isAfter(today)) today else d).toString()
                        when (which) { "start" -> start = v; "finish" -> finish = v; "base" -> base = v; else -> birth = v }
                    }
                    picking = null
                }) { Text(L("common.done")) }
            },
        ) { DatePicker(state) }
    }
}

@Composable
private fun DateRow(title: String, d: LocalDate, onClick: () -> Unit) {
    Row2(title, Fmt.shortDate(d), Modifier.clickable(onClick = onClick), color = Theme.c.accent)
}

private fun workText(w: Double): String =
    if (w == Math.rint(w)) num(w.toInt())
    else String.format(java.util.Locale.ROOT, "%.1f", w).replace(".", if (Strings.lang == "en") "." else ",")
