package tr.bumba.time

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
        Icon(icon, null, tint = Theme.c.accent.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
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

/** Liste satırlarındaki renkli simge kutusu (#99): 36 dp, köşe 10 dp, zemin ana rengin açık tonu */
@Composable
fun IconBox(icon: ImageVector, tint: Color = Theme.c.accent, modifier: Modifier = Modifier) {
    Box(
        modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = if (Theme.c.dark) 0.22f else 0.14f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
}

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
