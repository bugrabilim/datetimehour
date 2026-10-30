package tr.bumba.time

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

/** Saniye başında yenilenen "şimdi" */
@Composable
fun rememberNow(): Instant {
    val now by produceState(Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(1000 - System.currentTimeMillis() % 1000)
        }
    }
    return now
}

/** Yuvarlak köşeli kart (sitedeki kartlar gibi) */
@Composable
fun AppCard(
    modifier: Modifier = Modifier, padding: Int = 16, background: androidx.compose.ui.graphics.Color = Theme.c.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .border(1.dp, Theme.c.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(padding.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Theme.c.muted)
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    color: androidx.compose.ui.graphics.Color = Theme.c.text,
) {
    Text(text, modifier, fontSize = size, fontWeight = weight, color = color)
}

/** Liste satırı: başlık solda, değer sağda */
@Composable
fun Row2(title: String, value: String, modifier: Modifier = Modifier, bold: Boolean = false, color: androidx.compose.ui.graphics.Color? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = color ?: Theme.c.text, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(value, color = color ?: Theme.c.text, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** Alt sayfa başlığı: geri oku ve başlık */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Theme.c.bg, titleContentColor = Theme.c.text),
    )
}

/**
 * Konum seçici: "Konumumu kullan" (izin ister; en yakın il ya da tam konum), kayıtlı yerler, 81 il ve 973 ilçe (arama ya
 * da ile dokunup "İlçeler"). Seçim bütün uygulamada ortak (vakitler, mevsimler). iOS'taki PlacePicker ile aynı.
 */
@Composable
fun PlacePicker(modifier: Modifier = Modifier, size: TextUnit = 17.sp) {
    val place = AppPlace.current()
    var open by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun locate() {
        locating = true
        scope.launch {
            val loc = AppPlace.locate(context)
            locating = false
            if (loc != null) AppPlace.save(loc.latitude, loc.longitude) else failed = true
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) locate() else failed = true
    }

    Row(
        modifier.clickable { open = true }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (locating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        else Icon(if (place.isAuto) Icons.Filled.MyLocation else Icons.Filled.LocationOn, null, tint = Theme.c.accent, modifier = Modifier.size(18.dp))
        Text(place.name, color = Theme.c.accent, fontSize = size, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Filled.UnfoldMore, null, tint = Theme.c.accent, modifier = Modifier.size(16.dp))
    }

    if (open) {
        // Arama, kayıtlı yerler, iller; ile dokununca ilçeleri (il merkezi en üstte)
        var query by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf<String?>(null) }
        val coll = remember { java.text.Collator.getInstance(Strings.locale) }
        fun pick(id: String) { Prefs.saveProvince(id); open = false }
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text(L("common.done")) } },
            title = { Text(L("settings.location")) },
            text = {
                Column {
                    OutlinedTextField(query, { query = it; expanded = null }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text(L("place.search")) })
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        val q = District.fold(query.trim())
                        when {
                            q.isNotEmpty() -> {
                                val provinces = Province.all.filter { District.fold(it.displayName).contains(q) || District.fold(it.name).contains(q) }
                                val districts = Province.all.flatMap { District.all(it.slug) }.filter { !it.isCentre && District.fold(it.name).contains(q) }
                                items(provinces) { p -> PlaceRow(p.displayName, p.slug == place.id) { pick(p.slug) } }
                                items(districts.take(60)) { d -> PlaceRow(d.label, d.id == place.id) { pick(d.id) } }
                            }
                            expanded != null -> {
                                val slug = expanded!!
                                item {
                                    PlaceRow("‹ " + L("prayer.province"), false, accent = true) { expanded = null }
                                    HorizontalDivider()
                                    PlaceRow(T("place.wholeProvince", Province.find(slug).displayName), place.id == slug) { pick(slug) }
                                }
                                items(District.all(slug).filter { !it.isCentre }) { d -> PlaceRow(d.name, d.id == place.id) { pick(d.id) } }
                            }
                            else -> {
                                item {
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            open = false
                                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                                PackageManager.PERMISSION_GRANTED
                                            if (granted) locate() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }.padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.MyLocation, null, tint = Theme.c.accent)
                                        Spacer(Modifier.width(10.dp))
                                        Text(L("place.useLocation"), color = Theme.c.accent, fontWeight = FontWeight.SemiBold)
                                    }
                                    // Kayıtlı yerler: dokununca seçilir, × ile çıkar; şu anki yer kaydedilebilir
                                    if (Prefs.savedPlaces.isNotEmpty()) {
                                        HorizontalDivider()
                                        Text(L("place.saved"), Modifier.padding(top = 10.dp), color = Theme.c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        for (id in Prefs.savedPlaces) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.weight(1f)) { PlaceRow("★ " + AppPlace.byId(id).name, id == place.id) { pick(id) } }
                                                IconButton(onClick = { Prefs.saveSavedPlaces(Prefs.savedPlaces - id) }) {
                                                    Icon(Icons.Filled.Close, L("place.remove"), tint = Theme.c.muted, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    if (!place.isAuto && place.id !in Prefs.savedPlaces) {
                                        PlaceRow("☆ " + L("place.save") + " (" + place.name + ")", false, accent = true) {
                                            Prefs.saveSavedPlaces(Prefs.savedPlaces + place.id)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                                items(Province.all.sortedWith(compareBy(coll) { it.displayName })) { p ->
                                    val inside = place.id == p.slug || place.id.startsWith("d:${p.slug}:")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) { PlaceRow(p.displayName, inside) { pick(p.slug) } }
                                        // İlçeler
                                        if (District.all(p.slug).size > 1) TextButton(onClick = { expanded = p.slug }) {
                                            Text(L("place.districts") + " ›", color = Theme.c.accent, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                        item { Text(L("place.districtSource"), Modifier.padding(top = 10.dp), color = Theme.c.muted, fontSize = 11.sp) }
                    }
                }
            },
        )
    }
    if (failed) {
        AlertDialog(
            onDismissRequest = { failed = false },
            confirmButton = { TextButton(onClick = { failed = false }) { Text(L("common.done")) } },
            text = { Text(L("place.locationFailed")) },
        )
    }
}

/** Otomatik konumdaysa uygulama açılınca konum sessizce yenilenir (izin varsa) */
@Composable
fun RefreshAutoPlace() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (Prefs.province != AppPlace.AUTO) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return@LaunchedEffect
        AppPlace.locate(context)?.let { AppPlace.save(it.latitude, it.longitude) }
    }
}

/** Sabit genişlikli rakamlar (saat ve süreler oynamasın), normal yazı tipiyle */
val Tnum = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun PlaceRow(text: String, selected: Boolean, accent: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        color = if (selected || accent) Theme.c.accent else Theme.c.text,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
    )
}

/** Kayıtlı yerler arasında hızlı geçiş (Vakitler); kayıtlı yer yoksa görünmez */
@Composable
fun SavedPlaceChips(modifier: Modifier = Modifier) {
    val saved = Prefs.savedPlaces
    if (saved.isEmpty()) return
    val current = Prefs.province
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (id in saved) {
            val on = id == current
            Text(
                AppPlace.byId(id).name,
                Modifier.clip(RoundedCornerShape(50)).background(if (on) Theme.c.accentSoft else Theme.c.surface2)
                    .clickable { Prefs.saveProvince(id) }.padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (on) Theme.c.accent else Theme.c.text, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
