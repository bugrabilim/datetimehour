package tr.bumba.time

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/*
 * Ortak görünüm parçaları (2. analiz #96, #98, #99; 3 Kasım 2026). iOS'taki Components.swift ile aynı ölçüler.
 */

/**
 * Boş durum (#96): 64 dp çizgi simge (ana renk %60), 17 sp başlık, soluk açıklama ve hap biçimli eylem düğmesi.
 * Başlık ya da eylem verilmezse o satır çizilmez.
 */
@Composable
fun EmptyState(
    icon: ImageVector, title: String?, text: String?, modifier: Modifier = Modifier,
    action: String? = null, onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Küçük resim (doğal tasarım madde 18): çakıl taşı zemin, çizgi simge, köşede küçük güneş
        Box(Modifier.size(104.dp)) {
            Box(Modifier.align(Alignment.Center).size(88.dp).clip(PebbleShape).background(Theme.c.accentSoft))
            Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 6.dp).size(18.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(Theme.c.gold.copy(alpha = 0.55f)))
            Box(Modifier.align(Alignment.Center)) {
                val line = LineIconMap.of(icon.name)
                if (line != null) LineIcon(line, 48.dp, c1 = Theme.c.accent, c2 = Theme.c.sage)
                else Icon(icon, null, tint = Theme.c.accent, modifier = Modifier.size(44.dp))
            }
        }
        if (!title.isNullOrBlank()) Text(title, color = Theme.c.text, fontSize = 18.sp, fontFamily = AppFonts.heading,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (!text.isNullOrBlank()) Text(text, color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.size(4.dp))
            Text(
                action,
                Modifier.clip(RoundedCornerShape(50)).background(Theme.c.accent).clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = Theme.c.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Liste satırlarındaki renkli simge kutusu (#99; doğal tasarım madde 4, 4 Kasım 2026): "çakıl taşı" biçimi (köşeler
 * hafif farklı yuvarlak), zemin simge renginin açık tonu, içinde kendi çizgi simgemiz (karşılığı yoksa Material simgesi).
 * [line] verilirse o çizgi simge çizilir.
 */
@Composable
fun IconBox(icon: ImageVector, tint: Color = Theme.c.accent, modifier: Modifier = Modifier, line: String? = null) {
    val name = line ?: LineIconMap.of(icon.name)
    Box(
        modifier.size(36.dp).clip(PebbleShape).background(tint.copy(alpha = if (Theme.c.dark) 0.20f else 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        if (name != null) LineIcon(name, 21.dp, c1 = tint, c2 = tint.copy(alpha = 0.72f))
        else Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Çakıl taşı biçimi (doğal tasarım bölüm 3.3) */
val PebbleShape = RoundedCornerShape(topStartPercent = 44, topEndPercent = 50, bottomEndPercent = 46, bottomStartPercent = 48)

/**
 * Sekme sayfalarının büyük başlığı (#98; doğal tasarım 4 Kasım 2026): Literata 30/36, 600 (Latin ve Kiril dillerinde),
 * diğer dillerde sistem yazısı 28/34, 600. Büyük yazı boyutunda iki satıra kırılır.
 */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    val serif = AppFonts.serif
    Text(
        text, modifier, color = Theme.c.text, fontFamily = AppFonts.heading, fontWeight = FontWeight.SemiBold,
        fontSize = if (serif) 30.sp else 28.sp, lineHeight = if (serif) 36.sp else 34.sp,
        letterSpacing = if (serif) (-0.3).sp else 0.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Araç ekranı kalıbı (2. analiz #200): üstte isteğe bağlı başlık satırı ve sağda "…" menüsü (ikincil işler), ortada kayan
 * içerik, altta başparmak bölgesinde tam genişlikte tek ana düğme (isteğe bağlı solda ikincil, çerçeveli düğme).
 */
@Composable
fun ToolFrame(
    primary: String?, onPrimary: () -> Unit,
    secondary: String? = null, onSecondary: () -> Unit = {},
    menu: List<Pair<String, () -> Unit>> = emptyList(),
    header: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    scroll: Boolean = true,
    center: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { header() }
            if (menu.isNotEmpty()) MoreMenu(menu)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp),
            horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { content() }
        if (primary != null) {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().background(Theme.c.bg.copy(alpha = 0.92f)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (secondary != null) RoundButton(secondary, false, Modifier.weight(1f), onSecondary)
                RoundButton(primary, true, Modifier.weight(if (secondary != null) 1.4f else 1f), onPrimary)
            }
        }
    }
}

/** "…" menüsü: ikincil işler (paylaş, dışa aktar, sıfırla, ayarlar) */
@Composable
fun MoreMenu(items: List<Pair<String, () -> Unit>>) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        androidx.compose.material3.IconButton(onClick = { open = true }) {
            Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, L("timers.more"), tint = Theme.c.accent)
        }
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            for ((label, action) in items) {
                androidx.compose.material3.DropdownMenuItem({ Text(label) }, { open = false; action() })
            }
        }
    }
}

/**
 * Yumuşak bölmeli seçici (doğal tasarım madde 5, 4 Kasım 2026): onay işareti ve kenarlık yok; iz `surface2` hap, seçili
 * parça açık temada `surface` + hafif gölge, koyu temada izden biraz açık. Material SegmentedButton yerine.
 */
@Composable
fun SoftSegmentedRow(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(Theme.c.surface2).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically, content = content,
    )
}

@Composable
fun androidx.compose.foundation.layout.RowScope.SoftSegment(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Theme.c
    val shape = RoundedCornerShape(50)
    val selBg = if (c.dark) Color.White.copy(alpha = 0.10f).compositeOver(c.surface2) else c.surface
    Box(
        Modifier.weight(1f).heightIn(min = 38.dp)
            .then(
                if (!selected) Modifier
                else if (c.dark) Modifier.background(selBg, shape)
                else Modifier.shadow(2.dp, shape, ambientColor = Color(0x334A341E), spotColor = Color(0x224A341E)).background(selBg, shape),
            )
            .clip(shape)
            .selectable(selected, onClick = onClick, role = androidx.compose.ui.semantics.Role.Tab)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (selected) c.text else c.muted,
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(
                fontSize = 14.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
        ) { content() }
    }
}

/**
 * Yumuşak anahtar (doğal tasarım madde 5): kenarlıksız iz; açıkken ana renk, kapalıyken `surface2` iz ve soluk düğme.
 * Eski çağrılardaki `colors` yok sayılır (her yerde aynı görünüm).
 */
@Composable
fun SoftSwitch(
    checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") colors: androidx.compose.material3.SwitchColors? = null,
) {
    val c = Theme.c
    androidx.compose.material3.Switch(
        checked, onCheckedChange, modifier, enabled = enabled,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = c.surface2, uncheckedThumbColor = c.muted, uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = c.accent.copy(alpha = 0.4f), disabledUncheckedTrackColor = c.surface2.copy(alpha = 0.6f),
            disabledUncheckedBorderColor = Color.Transparent, disabledCheckedBorderColor = Color.Transparent,
        ),
    )
}

/**
 * Hareket belirteçleri (doğal tasarım madde 12, 4 Kasım 2026): kısa 150, orta 250, uzun 400 ms, sahne 1200 ms; yumuşak
 * eğri cubic-bezier(0.2, 0.8, 0.2, 1). Animasyonlar kapalıysa (sistem ayarı) Compose zaten süreyi sıfırlar.
 */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 250
    const val LONG = 400
    const val SCENE = 1200
    val ease = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
}

/** Basınca hafif küçülme (0,97): kartlar ve kareler için; etkileşim kaynağı tıklamayla paylaşılır */
@Composable
fun Modifier.pressScale(interaction: androidx.compose.foundation.interaction.MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.97f else 1f, androidx.compose.animation.core.tween(Motion.SHORT, easing = Motion.ease), label = "press",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}
