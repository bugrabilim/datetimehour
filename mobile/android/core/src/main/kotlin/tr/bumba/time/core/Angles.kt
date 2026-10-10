package tr.bumba.time.core

import kotlin.math.PI
import kotlin.math.floor

/* Derece tabanlı trigonometri ve sitedeki JavaScript ile aynı yuvarlama.
   İşlem sırası JS'tekiyle aynı tutulur: sonuçlar ortak test verisiyle karşılaştırılır.
   StrictMath (fdlibm) her cihazda aynı sonucu verir. */

internal const val DEG2RAD = PI / 180
internal const val RAD2DEG = 180 / PI

internal fun dsin(d: Double): Double = StrictMath.sin(d * DEG2RAD)
internal fun dcos(d: Double): Double = StrictMath.cos(d * DEG2RAD)
internal fun dtan(d: Double): Double = StrictMath.tan(d * DEG2RAD)
internal fun dasin(x: Double): Double = StrictMath.asin(x) * RAD2DEG
internal fun dacos(x: Double): Double = StrictMath.acos(x) * RAD2DEG
internal fun dacot(x: Double): Double = StrictMath.atan(1 / x) * RAD2DEG
internal fun datan2(y: Double, x: Double): Double = StrictMath.atan2(y, x) * RAD2DEG

/** 0..360 aralığına indirger (JS: a - 360 * Math.floor(a / 360)) */
internal fun fixAngle(a: Double): Double = a - 360 * floor(a / 360)

/** 0..24 aralığına indirger */
internal fun fixHour(h: Double): Double = h - 24 * floor(h / 24)

/** JavaScript Math.round: en yakın tam sayı, yarımda yukarı (+∞ yönüne) */
internal fun jsRound(x: Double): Double = floor(x + 0.5)
