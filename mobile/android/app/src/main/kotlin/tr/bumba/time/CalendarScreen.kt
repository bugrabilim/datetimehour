package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Takvim: ay ızgarası (resmî tatil, arife, dini gün, özel gün, okul tatili renkleri; Ramazan gölgeli), seçili günün
 * özel günleri, hafta ve yılın günü. Veri sitedeki takvimle aynı (calendar.json). Kaydırarak ay değişir.
 */
@Composable
fun CalendarScreen() {
    val today = LocalDate.now()
    var ym by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var sel by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(ym)
    val selected = LocalDate.parse(sel)
    fun move(d: Long) {
        val m = month.plusMonths(d)
        ym = m.toString()
        sel = (if (YearMonth.from(today) == m) today else m.atDay(1)).toString()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.month(month.monthValue), color = Theme.c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("  ${month.year}", color = Theme.c.accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { ym = YearMonth.from(today).toString(); sel = today.toString() }) {
                Text(L("client.today"), color = Theme.c.accent)
            }
            IconButton(onClick = { move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Theme.c.accent) }
            IconButton(onClick = { move(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Theme.c.accent) }
        }
        MonthGrid(month, today, selected, Modifier.pointerInput(ym) {
            var total = 0f
            detectHorizontalDragGestures(onDragEnd = {
                if (total < -60) move(if (Strings.isRtl) -1 else 1) else if (total > 60) move(if (Strings.isRtl) 1 else -1)
                total = 0f
            }) { _, d -> total += d }
        }) { sel = it.toString() }
        Spacer(Modifier.height(12.dp))
        DayDetail(selected, today)
        Spacer(Modifier.height(12.dp))
        Legend()
        if (month.year in AppData.projectedYears) {
            Text(L("calendar.projected"), color = Theme.c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, selected: LocalDate, modifier: Modifier, onPick: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1 // Pazartesi başlar
    val cells = lead + month.lengthOfMonth()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                Text(L("calendar.weekdaysShort.$i"), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp,
                    color = if (i >= 5) Theme.c.accent else Theme.c.muted)
            }
        }
        for (row in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val n = row * 7 + col - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (n in 1..month.lengthOfMonth()) {
                            val d = month.atDay(n)
                            DayCell(d, d == today, d == selected) { onPick(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val ev = AppData.events[d].orEmpty()
    val holiday = ev.any { it.type == "holiday" }
    val ramadan = AppData.isRamadan(d)
    Column(
        Modifier.fillMaxSize().padding(2.dp)
            .background(if (ramadan) AppData.color("religious").copy(alpha = 0.10f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(34.dp).background(
                when {
                    isToday -> Theme.c.accent
                    isSelected -> Theme.c.accentSoft
                    else -> Color.Transparent
                }, CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                d.dayOfMonth.toString(), fontSize = 17.sp,
                color = when {
                    isToday -> Theme.c.bg
                    holiday -> AppData.color("holiday")
                    d.dayOfWeek.value >= 6 -> Theme.c.muted
                    else -> Theme.c.text
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (t in ev.map { it.type }.distinct().take(3)) {
                Box(Modifier.size(5.dp).background(AppData.color(t), CircleShape))
            }
        }
    }
}

@Composable
private fun DayDetail(d: LocalDate, today: LocalDate) {
    val ev = AppData.events[d].orEmpty()
    AppCard {
        Text(Fmt.longDate(d), color = Theme.c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        val diff = ChronoUnit.DAYS.between(today, d)
        val rel = when {
            diff == 0L -> L("client.today")
            diff == 1L -> L("client.tomorrow")
            diff > 0 -> T("client.daysLeft", diff)
            else -> T("calendar.daysAgo", -diff)
        }
        Text(listOf(rel, T("info.week.big", Fmt.isoWeek(d)), T("calendar.dayOfYear", d.dayOfYear)).joinToString(" · "),
            color = Theme.c.muted, fontSize = 13.sp)
        for (e in ev) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(AppData.color(e.type), CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(e.name, color = Theme.c.text, fontSize = 15.sp)
            }
        }
        if (AppData.isRamadan(d)) TypeRow("religious", L("calendar.types.ramadan"))
        if (AppData.isSchoolBreak(d) && ev.none { it.type == "school" }) TypeRow("school", L("calendar.types.school"))
        if (ev.isEmpty() && !AppData.isRamadan(d)) Text(L("calendar.noDayEvents"), color = Theme.c.muted)
    }
}

@Composable
private fun TypeRow(type: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(AppData.color(type), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(text, color = AppData.color(type), fontSize = 14.sp)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (t in listOf("holiday", "half", "religious", "special", "school")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(AppData.color(t), CircleShape))
                Text(" " + L("calendar.types.$t"), color = Theme.c.muted, fontSize = 12.sp)
            }
        }
    }
}
