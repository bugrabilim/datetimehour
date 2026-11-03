package tr.bumba.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
        if (!title.isNullOrBlank()) Text(title, color = Theme.c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (!text.isNullOrBlank()) Text(text, color = Theme.c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.size(4.dp))
            Text(
                action,
                Modifier.clip(RoundedCornerShape(50)).background(Theme.c.accent).clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = if (Theme.c.dark) Color(0xFF11141A) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
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

/** Sekme sayfalarının büyük başlığı (#98): 28 sp kalın (Material headlineMedium) */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Theme.c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
}
