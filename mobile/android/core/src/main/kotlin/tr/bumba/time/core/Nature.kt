package tr.bumba.time.core

import kotlin.math.PI
import kotlin.math.floor

/**
 * Doğa hesapları (dış veri yok): ekinoks/gündönümü anları (Meeus, Astronomical Algorithms bölüm 27)
 * ve yeni ay/dolunay anları (bölüm 49). Sitedeki `src/js/nature-calc.js` dosyasının birebir karşılığıdır;
 * sonuçlar `mobile/shared/testdata/nature.json` ile doğrulanır. Zamanlar UTC milisaniyedir (Unix epoch).
 */
object Nature {
    /** Yılın dört mevsim anı (UTC milisaniye). */
    data class Seasons(val march: Long, val june: Long, val september: Long, val december: Long)

    /** Bir andaki Ay durumu. */
    data class Moon(
        /** Son yeni aydan bu yana geçen gün */
        val age: Double,
        /** Döngüde konum (0 = yeni ay, 0,5 = dolunay) */
        val fraction: Double,
        /** Aydınlanan kısım (0…1) */
        val illumination: Double,
        /** Evre: 0 yeni ay, 1 büyüyen hilal, 2 ilk dördün, 3 büyüyen şişkin, 4 dolunay, 5 küçülen şişkin, 6 son dördün, 7 küçülen hilal */
        val phaseIndex: Int,
        val nextNewMoon: Long,
        val nextFullMoon: Long,
    )

    private const val UNIX_JD = 2440587.5

    /** saniye (2026 dolayı; yalnız TT→UT için) */
    private const val DELTA_T = 69.0
    private const val DAY_MS = 86400000.0

    private fun jdToMs(jd: Double): Double = (jd - UNIX_JD) * 86400000

    private val SEASON_TERMS = arrayOf(
        doubleArrayOf(485.0, 324.96, 1934.136), doubleArrayOf(203.0, 337.23, 32964.467),
        doubleArrayOf(199.0, 342.08, 20.186), doubleArrayOf(182.0, 27.85, 445267.112),
        doubleArrayOf(156.0, 73.14, 45036.886), doubleArrayOf(136.0, 171.52, 22518.443),
        doubleArrayOf(77.0, 222.54, 65928.934), doubleArrayOf(74.0, 296.72, 3034.906),
        doubleArrayOf(70.0, 243.58, 9037.513), doubleArrayOf(58.0, 119.81, 33718.147),
        doubleArrayOf(52.0, 297.17, 150.678), doubleArrayOf(50.0, 21.02, 2281.226),
        doubleArrayOf(45.0, 247.54, 29929.562), doubleArrayOf(44.0, 325.15, 31555.956),
        doubleArrayOf(29.0, 60.93, 4443.417), doubleArrayOf(18.0, 155.12, 67555.328),
        doubleArrayOf(17.0, 288.79, 4562.452), doubleArrayOf(16.0, 198.04, 62894.029),
        doubleArrayOf(14.0, 199.76, 31436.921), doubleArrayOf(12.0, 95.39, 14577.848),
        doubleArrayOf(12.0, 287.11, 31931.756), doubleArrayOf(12.0, 320.81, 34777.259),
        doubleArrayOf(9.0, 227.73, 1222.114), doubleArrayOf(8.0, 15.45, 16859.074),
    )

    /** Verilen yılın dört mevsim anı. */
    fun seasons(year: Int): Seasons {
        val y = (year - 2000) / 1000.0
        val bases = doubleArrayOf(
            2451623.80984 + 365242.37404 * y + 0.05169 * y * y - 0.00411 * y * y * y - 0.00057 * y * y * y * y,
            2451716.56767 + 365241.62603 * y + 0.00325 * y * y + 0.00888 * y * y * y - 0.00030 * y * y * y * y,
            2451810.21715 + 365242.01767 * y - 0.11575 * y * y + 0.00337 * y * y * y + 0.00078 * y * y * y * y,
            2451900.05952 + 365242.74049 * y - 0.06223 * y * y - 0.00823 * y * y * y + 0.00032 * y * y * y * y,
        )
        val out = bases.map { jde0 ->
            val t = (jde0 - 2451545.0) / 36525
            val w = 35999.373 * t - 2.47
            val dl = 1 + 0.0334 * dcos(w) + 0.0007 * dcos(2 * w)
            var s = 0.0
            for (term in SEASON_TERMS) s += term[0] * dcos(term[1] + term[2] * t)
            val jde = jde0 + (0.00001 * s) / dl
            jsRound(jdToMs(jde) - DELTA_T * 1000).toLong()
        }
        return Seasons(out[0], out[1], out[2], out[3])
    }

    /** Yeni ay / dolunay JDE değeri: k tam sayı = yeni ay, k + 0,5 = dolunay */
    internal fun phaseJde(k: Double, full: Boolean): Double {
        val t = k / 1236.85
        val jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * t * t - 0.00000015 * t * t * t + 0.00000000073 * t * t * t * t
        val e = 1 - 0.002516 * t - 0.0000074 * t * t
        val m = 2.5534 + 29.10535670 * k - 0.0000014 * t * t - 0.00000011 * t * t * t
        val mp = 201.5643 + 385.81693528 * k + 0.0107582 * t * t + 0.00001238 * t * t * t - 0.000000058 * t * t * t * t
        val f = 160.7108 + 390.67050284 * k - 0.0016118 * t * t - 0.00000227 * t * t * t + 0.000000011 * t * t * t * t
        val om = 124.7746 - 1.56375588 * k + 0.0020672 * t * t + 0.00000215 * t * t * t
        // Düzeltme terimleri JS'teki sırayla soldan sağa toplanır (x - y ile x + (-y) aynı sonucu verir)
        val a = if (full) {
            doubleArrayOf(-0.40614, 0.17302, 0.01614, 0.01043, 0.00734, -0.00515, 0.00209)
        } else {
            doubleArrayOf(-0.40720, 0.17241, 0.01608, 0.01039, 0.00739, -0.00514, 0.00208)
        }
        val terms = doubleArrayOf(
            a[0] * dsin(mp), a[1] * e * dsin(m), a[2] * dsin(2 * mp), a[3] * dsin(2 * f),
            a[4] * e * dsin(mp - m), a[5] * e * dsin(mp + m), a[6] * e * e * dsin(2 * m),
            -0.00111 * dsin(mp - 2 * f), -0.00057 * dsin(mp + 2 * f), 0.00056 * e * dsin(2 * mp + m),
            -0.00042 * dsin(3 * mp), 0.00042 * e * dsin(m + 2 * f), 0.00038 * e * dsin(m - 2 * f),
            -0.00024 * e * dsin(2 * mp - m), -0.00017 * dsin(om),
        )
        var c = 0.0
        for (term in terms) c += term
        return jde + c
    }

    /** k. yeni ayın (ya da dolunayın) anı, UTC milisaniye. */
    fun phaseMillis(k: Int, full: Boolean): Long =
        jsRound(jdToMs(phaseJde(k + if (full) 0.5 else 0.0, full)) - DELTA_T * 1000).toLong()

    private fun kFor(ms: Long): Int = floor((ms / DAY_MS + UNIX_JD - 2451550.09766) / 29.530588861).toInt()

    /** Verilen andaki Ay durumu. */
    fun moon(ms: Long): Moon {
        var k = kFor(ms) - 1
        var prevNew = 0L
        var nextNew = 0L
        for (i in 0 until 4) {
            val t = phaseMillis(k, false)
            if (t <= ms) prevNew = t else { nextNew = t; break }
            k++
        }
        val len = (nextNew - prevNew).toDouble()
        val frac = (ms - prevNew).toDouble() / len
        val illum = (1 - StrictMath.cos(2 * PI * frac)) / 2
        val idx = floor(frac * 8 + 0.5).toInt() % 8
        var kk = kFor(ms) - 1
        var nextFull = 0L
        for (j in 0 until 4) {
            val f = phaseMillis(kk, true)
            if (f > ms) { nextFull = f; break }
            kk++
        }
        return Moon((ms - prevNew) / DAY_MS, frac, illum, idx, nextNew, nextFull)
    }
}
