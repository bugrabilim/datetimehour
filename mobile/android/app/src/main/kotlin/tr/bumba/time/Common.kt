package tr.bumba.time

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
fun AppCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Theme.c.surface, RoundedCornerShape(16.dp))
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
 * Konum seçici: "Konumumu kullan" (izin ister; en yakın il ya da tam konum) ve 81 il. Seçim bütün uygulamada ortak
 * (vakitler, mevsimler). iOS'taki PlacePicker ile aynı.
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
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text(L("common.done")) } },
            title = { Text(L("settings.location")) },
            text = {
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
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
                        HorizontalDivider()
                    }
                    items(Province.all.sortedWith(compareBy(java.text.Collator.getInstance(Strings.locale)) { it.displayName })) { p ->
                        val selected = !place.isAuto && p.slug == place.id
                        Text(
                            p.displayName,
                            Modifier.fillMaxWidth().clickable { Prefs.saveProvince(p.slug); open = false }.padding(vertical = 10.dp),
                            color = if (selected) Theme.c.accent else Theme.c.text,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
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
