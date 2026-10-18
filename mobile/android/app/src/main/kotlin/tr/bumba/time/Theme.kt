package tr.bumba.time

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Sitedeki renk token'ları (src/styles.css): açık "kâğıt" ve koyu "gece" paleti. iOS'taki Theme.swift ile aynı:
 * her tema bir ana renk, bir ikinci renk ve bir ton gücüyle gelir; zemin, kartlar, kadran ve kenarlık ana renge
 * doğru karışır. Varsayılan "amber" tonsuzdur (sitedeki renkler).
 */
@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    /** Saat kadranı ve ikincil yüzeyler */
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accent2: Color,
    /** Saat rakamları ve akrep */
    val ink: Color,
    val dark: Boolean,
) {
    val accentSoft: Color get() = accent.copy(alpha = 0.16f)
}

data class AccentPalette(
    val id: String,
    val group: String,
    val light: Long,
    val dark: Long,
    val secondLight: Long,
    val secondDark: Long,
    val tint: Double,
) {
    companion object {
        const val DEFAULT_ID = "amber"
        val GROUPS = listOf("general", "kids", "elegant", "business")

        val ALL = listOf(
            AccentPalette("amber", "general", 0x9A3F00, 0xF2A65A, 0x9A3F00, 0xF2A65A, 0.0),
            AccentPalette("blue", "general", 0x0A66C2, 0x5AA9FF, 0xC2410A, 0xFF9E5A, 0.8),
            AccentPalette("green", "general", 0x1B7A43, 0x5BD38A, 0xA15C00, 0xF5C04A, 0.8),
            AccentPalette("indigo", "general", 0x3949AB, 0x9FA8DA, 0xC2185B, 0xF48FB1, 0.8),
            AccentPalette("orange", "general", 0xB85400, 0xFFB74D, 0x00695C, 0x4DB6AC, 0.8),
            AccentPalette("cyan", "general", 0x00708A, 0x4DD0E1, 0xAD1457, 0xF06292, 0.8),
            AccentPalette("kid-red", "kids", 0xC62828, 0xFF6B6B, 0x1565C0, 0x64B5F6, 1.0),
            AccentPalette("kid-orange", "kids", 0xD84315, 0xFF9E5E, 0x6A1B9A, 0xCE93D8, 1.0),
            AccentPalette("kid-yellow", "kids", 0x8A6D00, 0xFFD54F, 0xC62828, 0xFF6B6B, 1.0),
            AccentPalette("kid-green", "kids", 0x2E7D32, 0x7BD88F, 0xD84315, 0xFF9E5E, 1.0),
            AccentPalette("kid-blue", "kids", 0x1565C0, 0x64B5F6, 0x8A6D00, 0xFFD54F, 1.0),
            AccentPalette("kid-purple", "kids", 0x7B1FA2, 0xCE93D8, 0x2E7D32, 0x7BD88F, 1.0),
            AccentPalette("rose", "elegant", 0xAD1457, 0xF48FB1, 0x8D6E63, 0xD7B8A8, 0.9),
            AccentPalette("coral", "elegant", 0xB4442E, 0xFF8A75, 0x00796B, 0x80CBC4, 0.9),
            AccentPalette("mauve", "elegant", 0x8E4A6E, 0xE0A3C4, 0x5C6BC0, 0xB3BCF5, 0.9),
            AccentPalette("lavender", "elegant", 0x6A4FA3, 0xB9A5F0, 0xAD1457, 0xF48FB1, 0.9),
            AccentPalette("plum", "elegant", 0x6D2E5B, 0xD69ACB, 0xA1887F, 0xE6C9B8, 0.9),
            AccentPalette("rosegold", "elegant", 0x9C5A4A, 0xE8B4A0, 0x6D4C41, 0xD7B8A8, 0.9),
            AccentPalette("navy", "business", 0x1F3A68, 0x8AB4F8, 0xA67C00, 0xE6C35C, 0.6),
            AccentPalette("slate", "business", 0x3E4C5E, 0xA9B7C8, 0x0A66C2, 0x5AA9FF, 0.6),
            AccentPalette("graphite", "business", 0x333333, 0xD0D0D0, 0xC62828, 0xFF6B6B, 0.5),
            AccentPalette("teal", "business", 0x00695C, 0x4DB6AC, 0x37474F, 0xB0BEC5, 0.6),
            AccentPalette("burgundy", "business", 0x7A1F2B, 0xE08A95, 0x8D6E63, 0xD7B8A8, 0.6),
            AccentPalette("forest", "business", 0x2F5233, 0x8FC79A, 0x8A6D00, 0xE6C35C, 0.6),
        )

        fun find(id: String?): AccentPalette = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }

    fun colors(dark: Boolean): AppColors {
        val a = if (dark) this.dark else light
        val k = tint
        fun t(l: Long, d: Long, lt: Double, dt: Double) =
            rgb(if (dark) mix(d, a, dt * k) else mix(l, a, lt * k))
        return AppColors(
            bg = t(0xF7F4EE, 0x11141A, 0.09, 0.11),
            surface = t(0xFFFFFF, 0x1A1E26, 0.04, 0.07),
            surface2 = t(0xEFEBE2, 0x232834, 0.18, 0.20),
            text = rgb(if (dark) 0xEEF0F4 else 0x1B1F27),
            muted = rgb(if (dark) 0xA5ACB9 else 0x565C68),
            border = t(0xD6CFBF, 0x343B49, 0.35, 0.35),
            accent = rgb(a),
            accent2 = rgb(if (dark) secondDark else secondLight),
            ink = t(0x1B1F27, 0xEEF0F4, 0.45, 0.30),
            dark = dark,
        )
    }

    /** Seçicideki örnek: kadran rengi */
    fun face(dark: Boolean): Color =
        rgb(if (dark) mix(0x232834, this.dark, 0.20 * tint) else mix(0xEFEBE2, light, 0.18 * tint))
}

fun rgb(v: Long): Color = Color(0xFF000000 or v)

fun mix(a: Long, b: Long, t: Double): Long {
    fun ch(v: Long, s: Int) = ((v shr s) and 0xFF).toDouble()
    fun m(s: Int) = Math.round(ch(a, s) * (1 - t) + ch(b, s) * t) shl s
    return m(16) or m(8) or m(0)
}

val LocalColors = staticCompositionLocalOf { AccentPalette.ALL[0].colors(false) }

/** Kısa yol: Theme.bg, Theme.accent… (iOS'taki gibi) */
object Theme {
    val c: AppColors @Composable get() = LocalColors.current
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = when (Prefs.appearance) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colors = AccentPalette.find(Prefs.accent).colors(dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.bg, background = colors.bg, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text, surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.muted, outline = colors.border, secondary = colors.accent2,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface, surfaceContainerHighest = colors.surface2,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = Color.White, background = colors.bg, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text, surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.muted, outline = colors.border, secondary = colors.accent2,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface, surfaceContainerHighest = colors.surface2,
        )
    }
    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
