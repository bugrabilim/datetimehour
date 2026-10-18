package tr.bumba.time

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Dönen dünya (iOS'taki GlobeView). fullscreen: kapatma düğmesi; interactive = false: saat ekranındaki küçük kopya
 * (dokunuş haritaya gitmez, düğme yok).
 */
@Composable
fun GlobeView(modifier: Modifier = Modifier, fullscreen: Boolean = false, interactive: Boolean = true, onClose: () -> Unit = {}) {
    Box(modifier)
}
