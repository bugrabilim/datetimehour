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

    /** Ay'ın yer merkezli konumu (derece): sağ açıklık, dik açıklık, yatay paralaks. */
    data class MoonPosition(val rightAscension: Double, val declination: Double, val parallax: Double)

    /** Ay'ın doğuşu ve batışı (UTC milisaniye; o aralıkta olmuyorsa null). */
    data class MoonRiseSet(val rise: Long?, val set: Long?)

    /** Meeus bölüm 47, kısaltılmış seri: D, M, M', F, boylam (1e-6°), uzaklık (1e-3 km) */
    private val MOON_LR = arrayOf(
        doubleArrayOf(0.0, 0.0, 1.0, 0.0, 6288774.0, -20905355.0), doubleArrayOf(2.0, 0.0, -1.0, 0.0, 1274027.0, -3699111.0),
        doubleArrayOf(2.0, 0.0, 0.0, 0.0, 658314.0, -2955968.0), doubleArrayOf(0.0, 0.0, 2.0, 0.0, 213618.0, -569925.0),
        doubleArrayOf(0.0, 1.0, 0.0, 0.0, -185116.0, 48888.0), doubleArrayOf(0.0, 0.0, 0.0, 2.0, -114332.0, -3149.0),
        doubleArrayOf(2.0, 0.0, -2.0, 0.0, 58793.0, 246158.0), doubleArrayOf(2.0, -1.0, -1.0, 0.0, 57066.0, -152138.0),
        doubleArrayOf(2.0, 0.0, 1.0, 0.0, 53322.0, -170733.0), doubleArrayOf(2.0, -1.0, 0.0, 0.0, 45758.0, -204586.0),
        doubleArrayOf(0.0, 1.0, -1.0, 0.0, -40923.0, -129620.0), doubleArrayOf(1.0, 0.0, 0.0, 0.0, -34720.0, 108743.0),
        doubleArrayOf(0.0, 1.0, 1.0, 0.0, -30383.0, 104755.0), doubleArrayOf(2.0, 0.0, 0.0, -2.0, 15327.0, 10321.0),
        doubleArrayOf(0.0, 0.0, 1.0, 2.0, -12528.0, 0.0), doubleArrayOf(0.0, 0.0, 1.0, -2.0, 10980.0, 79661.0),
        doubleArrayOf(4.0, 0.0, -1.0, 0.0, 10675.0, -34782.0), doubleArrayOf(0.0, 0.0, 3.0, 0.0, 10034.0, -23210.0),
        doubleArrayOf(4.0, 0.0, -2.0, 0.0, 8548.0, -21636.0), doubleArrayOf(2.0, 1.0, -1.0, 0.0, -7888.0, 24208.0),
        doubleArrayOf(2.0, 1.0, 0.0, 0.0, -6766.0, 30824.0), doubleArrayOf(1.0, 0.0, -1.0, 0.0, -5163.0, -8379.0),
        doubleArrayOf(1.0, 1.0, 0.0, 0.0, 4987.0, -16675.0), doubleArrayOf(2.0, -1.0, 1.0, 0.0, 4036.0, -12831.0),
        doubleArrayOf(2.0, 0.0, 2.0, 0.0, 3994.0, -10445.0), doubleArrayOf(4.0, 0.0, 0.0, 0.0, 3861.0, -11650.0),
        doubleArrayOf(2.0, 0.0, -3.0, 0.0, 3665.0, 14403.0), doubleArrayOf(0.0, 1.0, -2.0, 0.0, -2689.0, -7003.0),
        doubleArrayOf(2.0, 0.0, -1.0, 2.0, -2602.0, 0.0), doubleArrayOf(2.0, -1.0, -2.0, 0.0, 2390.0, 10056.0),
        doubleArrayOf(1.0, 0.0, 1.0, 0.0, -2348.0, 6322.0), doubleArrayOf(2.0, -2.0, 0.0, 0.0, 2236.0, -9884.0),
    )
    /** D, M, M', F, enlem (1e-6°) */
    private val MOON_B = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.0, 1.0, 5128122.0), doubleArrayOf(0.0, 0.0, 1.0, 1.0, 280602.0), doubleArrayOf(0.0, 0.0, 1.0, -1.0, 277693.0),
        doubleArrayOf(2.0, 0.0, 0.0, -1.0, 173237.0), doubleArrayOf(2.0, 0.0, -1.0, 1.0, 55413.0), doubleArrayOf(2.0, 0.0, -1.0, -1.0, 46271.0),
        doubleArrayOf(2.0, 0.0, 0.0, 1.0, 32573.0), doubleArrayOf(0.0, 0.0, 2.0, 1.0, 17198.0), doubleArrayOf(2.0, 0.0, 1.0, -1.0, 9266.0),
        doubleArrayOf(0.0, 0.0, 2.0, -1.0, 8822.0), doubleArrayOf(2.0, -1.0, 0.0, -1.0, 8216.0), doubleArrayOf(2.0, 0.0, -2.0, -1.0, 4324.0),
        doubleArrayOf(2.0, 0.0, 1.0, 1.0, 4200.0), doubleArrayOf(2.0, 1.0, 0.0, -1.0, -3359.0), doubleArrayOf(2.0, -1.0, -1.0, 1.0, 2463.0),
        doubleArrayOf(2.0, -1.0, 0.0, 1.0, 2211.0), doubleArrayOf(2.0, -1.0, -1.0, -1.0, 2065.0), doubleArrayOf(0.0, 1.0, -1.0, -1.0, -1870.0),
        doubleArrayOf(4.0, 0.0, -1.0, -1.0, 1828.0), doubleArrayOf(0.0, 1.0, 0.0, 1.0, -1794.0), doubleArrayOf(0.0, 0.0, 0.0, 3.0, -1749.0),
        doubleArrayOf(0.0, 1.0, -1.0, 1.0, -1565.0), doubleArrayOf(1.0, 0.0, 0.0, 1.0, -1491.0), doubleArrayOf(0.0, 1.0, 1.0, 1.0, -1475.0),
        doubleArrayOf(0.0, 1.0, 1.0, -1.0, -1410.0), doubleArrayOf(0.0, 1.0, 0.0, -1.0, -1344.0), doubleArrayOf(1.0, 0.0, 0.0, -1.0, -1335.0),
        doubleArrayOf(0.0, 0.0, 3.0, 1.0, 1107.0),
    )

    /** ms anında Ay'ın konumu (sitedeki moonPos ile aynı işlem sırası) */
    fun moonPosition(ms: Double): MoonPosition {
        val T = (ms / DAY_MS + UNIX_JD + DELTA_T / 86400 - 2451545) / 36525
        val T2 = T * T
        val T3 = T2 * T
        val T4 = T3 * T
        val Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T2 + T3 / 538841 - T4 / 65194000
        val D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T2 + T3 / 545868 - T4 / 113065000
        val M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000
        val Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T2 + T3 / 69699 - T4 / 14712000
        val F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T2 - T3 / 3526000 + T4 / 863310000
        val A1 = 119.75 + 131.849 * T
        val A2 = 53.09 + 479264.290 * T
        val A3 = 313.45 + 481266.484 * T
        val E = 1 - 0.002516 * T - 0.0000074 * T2
        var sl = 0.0
        var sr = 0.0
        var sb = 0.0
        for (t in MOON_LR) {
            val arg = t[0] * D + t[1] * M + t[2] * Mp + t[3] * F
            val e = if (t[1] == 0.0) 1.0 else if (t[1] == 2.0 || t[1] == -2.0) E * E else E
            sl += t[4] * e * dsin(arg)
            sr += t[5] * e * dcos(arg)
        }
        for (t in MOON_B) {
            val arg = t[0] * D + t[1] * M + t[2] * Mp + t[3] * F
            val e = if (t[1] == 0.0) 1.0 else if (t[1] == 2.0 || t[1] == -2.0) E * E else E
            sb += t[4] * e * dsin(arg)
        }
        sl += 3958 * dsin(A1) + 1962 * dsin(Lp - F) + 318 * dsin(A2)
        sb += -2235 * dsin(Lp) + 382 * dsin(A3) + 175 * dsin(A1 - F) + 175 * dsin(A1 + F) + 127 * dsin(Lp - Mp) - 115 * dsin(Lp + Mp)
        val lam = Lp + sl / 1000000
        val bet = sb / 1000000
        val dist = 385000.56 + sr / 1000
        val eps = 23.4392911 - 0.0130042 * T
        val ra = StrictMath.atan2(dsin(lam) * dcos(eps) - (dsin(bet) / dcos(bet)) * dsin(eps), dcos(lam)) / DEG2RAD
        val dec = StrictMath.asin(dsin(bet) * dcos(eps) + dcos(bet) * dsin(eps) * dsin(lam)) / DEG2RAD
        return MoonPosition(ra, dec, StrictMath.asin(6378.14 / dist) / DEG2RAD)
    }

    /** Ufka göre yükseklik eksi doğuş/batış eşiği (Meeus bölüm 15); 0'dan büyükse Ay ufkun üstünde */
    fun moonAltitude(ms: Double, lat: Double, lon: Double): Double {
        val p = moonPosition(ms)
        val d = ms / DAY_MS + UNIX_JD - 2451545
        val T = d / 36525
        val gmst = 280.46061837 + 360.98564736629 * d + 0.000387933 * T * T
        val sh = dsin(lat) * dsin(p.declination) + dcos(lat) * dcos(p.declination) * dcos(gmst + lon - p.rightAscension)
        return StrictMath.asin(sh) / DEG2RAD - (0.7275 * p.parallax - 0.5667)
    }

    /** [start]tan başlayan [span] (varsayılan 24 saat) içinde Ay'ın ilk doğuşu ve batışı */
    fun moonRiseSet(start: Long, lat: Double, lon: Double, span: Long = 86_400_000): MoonRiseSet {
        val step = 600000.0
        val n = jsRound(span / step).toInt()
        var rise: Long? = null
        var set: Long? = null
        var t0 = start.toDouble()
        var f0 = moonAltitude(t0, lat, lon)
        for (i in 1..n) {
            val t1 = start.toDouble() + i * step
            val f1 = moonAltitude(t1, lat, lon)
            if ((f0 < 0) != (f1 < 0)) {
                val isRise = f0 < 0
                if ((if (isRise) rise else set) == null) {
                    var a = t0
                    var b = t1
                    var fa = f0
                    repeat(12) {
                        val m = (a + b) / 2
                        val fm = moonAltitude(m, lat, lon)
                        if ((fa < 0) == (fm < 0)) { a = m; fa = fm } else b = m
                    }
                    val at = jsRound((a + b) / 2).toLong()
                    if (isRise) rise = at else set = at
                }
            }
            t0 = t1
            f0 = f1
        }
        return MoonRiseSet(rise, set)
    }
}
