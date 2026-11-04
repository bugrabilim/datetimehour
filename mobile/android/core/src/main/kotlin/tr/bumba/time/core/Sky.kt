package tr.bumba.time.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Gezegenler ve tutulma ayrıntısı (3 Kasım 2026, 2. analiz #157, #159). Sitedeki `src/js/sky-calc.js` (JPL'in yaklaşık
 * Kepler öğeleri, Standish) ve `src/js/nature-calc.js` (`solarLocal`, `lunarContacts`) dosyalarının karşılığı;
 * `mobile/shared/testdata/sky.json` ile doğrulanır. Zamanlar UTC milisaniye.
 */
object Sky {
    private const val UNIX_JD = 2440587.5
    private const val DELTA_T = 69.0
    val PLANETS = listOf("mercury", "venus", "mars", "jupiter", "saturn")
    private val EL = mapOf(
        "mercury" to doubleArrayOf(0.38709927, 0.00000037, 0.20563593, 0.00001906, 7.00497902, -0.00594749, 252.25032350, 149472.67411175, 77.45779628, 0.16047689, 48.33076593, -0.12534081),
        "venus" to doubleArrayOf(0.72333566, 0.00000390, 0.00677672, -0.00004107, 3.39467605, -0.00078890, 181.97909950, 58517.81538729, 131.60246718, 0.00268329, 76.67984255, -0.27769418),
        "earth" to doubleArrayOf(1.00000261, 0.00000562, 0.01671123, -0.00004392, -0.00001531, -0.01294668, 100.46457166, 35999.37244981, 102.93768193, 0.32327364, 0.0, 0.0),
        "mars" to doubleArrayOf(1.52371034, 0.00001847, 0.09339410, 0.00007882, 1.84969142, -0.00813131, -4.55343205, 19140.30268499, -23.94362959, 0.44441088, 49.55953891, -0.29257343),
        "jupiter" to doubleArrayOf(5.20288700, -0.00011607, 0.04838624, -0.00013253, 1.30439695, -0.00183714, 34.39644051, 3034.74612775, 14.72847983, 0.21252668, 100.47390909, 0.20469106),
        "saturn" to doubleArrayOf(9.53667594, -0.00125060, 0.05386179, -0.00050991, 2.48599187, 0.00193609, 49.95424423, 1222.49362201, 92.59887831, -0.41897216, 113.66242448, -0.28867794),
    )

    private fun helio(name: String, T: Double): DoubleArray {
        val q = EL.getValue(name)
        val a = q[0] + q[1] * T; val e = q[2] + q[3] * T; val I = q[4] + q[5] * T; val L = q[6] + q[7] * T; val w = q[8] + q[9] * T; val O = q[10] + q[11] * T
        var M = L - w; val om = w - O
        M = ((M + 180) % 360 + 360) % 360 - 180
        var E = M + (e * RAD2DEG) * dsin(M)
        for (i in 0 until 10) { val dE = (M - (E - (e * RAD2DEG) * dsin(E))) / (1 - e * dcos(E)); E += dE; if (abs(dE) < 1e-8) break }
        val xp = a * (dcos(E) - e); val yp = a * sqrt(1 - e * e) * dsin(E)
        return doubleArrayOf(
            (dcos(om) * dcos(O) - dsin(om) * dsin(O) * dcos(I)) * xp + (-dsin(om) * dcos(O) - dcos(om) * dsin(O) * dcos(I)) * yp,
            (dcos(om) * dsin(O) + dsin(om) * dcos(O) * dcos(I)) * xp + (-dsin(om) * dsin(O) + dcos(om) * dcos(O) * dcos(I)) * yp,
            (dsin(om) * dsin(I)) * xp + (dcos(om) * dsin(I)) * yp,
        )
    }

    private fun mag(name: String, i: Double): Double = when (name) {
        "mercury" -> -0.42 + 0.0380 * i - 0.000273 * i * i + 0.000002 * i * i * i
        "venus" -> -4.40 + 0.0009 * i + 0.000239 * i * i - 0.00000065 * i * i * i
        "mars" -> -1.52 + 0.016 * i
        "jupiter" -> -9.40 + 0.005 * i
        else -> -8.88
    }

    /** ra, dec (derece, tarih ekinoksu), dist (AB), r (Güneş'e AB), elong (derece), phase, illum (0..1), mag */
    data class Planet(val ra: Double, val dec: Double, val dist: Double, val r: Double, val elong: Double, val phase: Double, val illum: Double, val mag: Double)

    fun planet(name: String, ms: Double): Planet {
        val T = (ms / 86400000 + UNIX_JD + DELTA_T / 86400 - 2451545) / 36525
        val ea = helio("earth", T)
        var p = helio(name, T)
        var dx = p[0] - ea[0]; var dy = p[1] - ea[1]; var dz = p[2] - ea[2]
        var dist = sqrt(dx * dx + dy * dy + dz * dz)
        val T2 = T - dist * 0.0057755183 / 36525
        p = helio(name, T2)
        dx = p[0] - ea[0]; dy = p[1] - ea[1]; dz = p[2] - ea[2]; dist = sqrt(dx * dx + dy * dy + dz * dz)
        val r = sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]); val R = sqrt(ea[0] * ea[0] + ea[1] * ea[1] + ea[2] * ea[2])
        val lam = datan2(dy, dx) + 1.3969713 * T; val bet = dasin(dz / dist)
        val eps = 23.4392911 - 0.0130042 * T
        val ra = datan2(dsin(lam) * dcos(eps) - dtan(bet) * dsin(eps), dcos(lam))
        val dec = dasin(dsin(bet) * dcos(eps) + dcos(bet) * dsin(eps) * dsin(lam))
        val ci = (r * r + dist * dist - R * R) / (2 * r * dist); val i = dacos(max(-1.0, min(1.0, ci)))
        val ce = (R * R + dist * dist - r * r) / (2 * R * dist); val elong = dacos(max(-1.0, min(1.0, ce)))
        return Planet(((ra % 360) + 360) % 360, dec, dist, r, elong, i, (1 + ci) / 2, mag(name, i) + 5 * StrictMath.log10(r * dist))
    }

    /** Parlak yıldızlar (J2000; ad, sağ açıklık, dik açıklık, kadir): sitedeki listeyle aynı */
    val STARS: List<Triple<String, Pair<Double, Double>, Double>> = listOf(
        Triple("Sirius", 101.287 to -16.716, -1.46), Triple("Canopus", 95.988 to -52.696, -0.74), Triple("Arcturus", 213.915 to 19.182, -0.05),
        Triple("Vega", 279.234 to 38.784, 0.03), Triple("Capella", 79.172 to 45.998, 0.08), Triple("Rigel", 78.634 to -8.202, 0.13),
        Triple("Procyon", 114.825 to 5.225, 0.34), Triple("Achernar", 24.429 to -57.237, 0.46), Triple("Betelgeuse", 88.793 to 7.407, 0.50),
        Triple("Hadar", 210.956 to -60.373, 0.61), Triple("Altair", 297.696 to 8.868, 0.76), Triple("Acrux", 186.650 to -63.099, 0.76),
        Triple("Aldebaran", 68.980 to 16.509, 0.86), Triple("Antares", 247.352 to -26.432, 0.96), Triple("Spica", 201.298 to -11.161, 0.97),
        Triple("Pollux", 116.329 to 28.026, 1.14), Triple("Fomalhaut", 344.413 to -29.622, 1.16), Triple("Deneb", 310.358 to 45.280, 1.25),
        Triple("Mimosa", 191.930 to -59.689, 1.25), Triple("Regulus", 152.093 to 11.967, 1.35), Triple("Adhara", 104.656 to -28.972, 1.50),
        Triple("Castor", 113.650 to 31.888, 1.58), Triple("Shaula", 263.402 to -37.104, 1.62), Triple("Bellatrix", 81.283 to 6.350, 1.64),
        Triple("Elnath", 81.573 to 28.608, 1.65), Triple("Alnilam", 84.053 to -1.202, 1.69), Triple("Alnitak", 85.190 to -1.943, 1.77),
        Triple("Alioth", 193.507 to 55.960, 1.77), Triple("Dubhe", 165.932 to 61.751, 1.79), Triple("Mirfak", 51.081 to 49.861, 1.79),
        Triple("Alkaid", 206.885 to 49.313, 1.86), Triple("Polaris", 37.955 to 89.264, 1.98), Triple("Mintaka", 83.002 to -0.299, 2.23),
        Triple("Mizar", 200.981 to 54.925, 2.23), Triple("Merak", 165.460 to 56.383, 2.37), Triple("Phecda", 178.458 to 53.695, 2.44),
        Triple("Megrez", 183.857 to 57.033, 3.31), Triple("Schedar", 10.127 to 56.537, 2.24), Triple("Caph", 2.295 to 59.150, 2.28),
    )

    /* ---- Tutulma ayrıntısı ---- */
    private const val AU_KM = 149597870.7
    private const val R_EARTH = 6378.14
    private fun vec3(ra: Double, dec: Double, r: Double) = doubleArrayOf(r * dcos(dec) * dcos(ra), r * dcos(dec) * dsin(ra), r * dsin(dec))
    private fun gmst(ms: Double): Double { val d = ms / 86400000 + UNIX_JD - 2451545; val t = d / 36525; return 280.46061837 + 360.98564736629 * d + 0.000387933 * t * t }

    data class Disks(val d: Double, val rs: Double, val rm: Double, val alt: Double)

    /** Bir yerden Güneş ile Ay diskleri (derece) ve Güneş yüksekliği */
    fun solarDisks(ms: Double, lat: Double, lon: Double): Disks {
        val m = Nature.moonPosition(ms); val sp = Nature.sunPosition(ms)
        val mdist = R_EARTH / StrictMath.sin(m.parallax * DEG2RAD)
        val M = vec3(m.rightAscension, m.declination, mdist); val S = vec3(sp.rightAscension, sp.declination, sp.distance * AU_KM)
        val th = gmst(ms) + lon; val cl = dcos(lat)
        val o = doubleArrayOf(R_EARTH * cl * dcos(th), R_EARTH * cl * dsin(th), R_EARTH * dsin(lat))
        val sx = S[0] - o[0]; val sy = S[1] - o[1]; val sz = S[2] - o[2]; val mx = M[0] - o[0]; val my = M[1] - o[1]; val mz = M[2] - o[2]
        val ds = sqrt(sx * sx + sy * sy + sz * sz); val dm = sqrt(mx * mx + my * my + mz * mz)
        val c = (sx * mx + sy * my + sz * mz) / (ds * dm)
        return Disks(dacos(max(-1.0, min(1.0, c))), dasin(696000 / ds), dasin(1737.4 / dm), Nature.altAz(ms, sp.rightAscension, sp.declination, lat, lon).alt)
    }

    private fun overlapFrac(d: Double, rs: Double, rm: Double): Double {
        if (d >= rs + rm) return 0.0
        if (d <= abs(rs - rm)) return if (rm >= rs) 1.0 else (rm * rm) / (rs * rs)
        val a1 = StrictMath.acos((d * d + rs * rs - rm * rm) / (2 * d * rs)); val a2 = StrictMath.acos((d * d + rm * rm - rs * rs) / (2 * d * rm))
        val area = rs * rs * (a1 - StrictMath.sin(2 * a1) / 2) + rm * rm * (a2 - StrictMath.sin(2 * a2) / 2)
        return area / (Math.PI * rs * rs)
    }

    /** Güneş tutulmasının bir yerden görünüşü; o yerden görülmezse null */
    data class SolarLocal(val c1: Long?, val max: Long, val c4: Long?, val mag: Double, val obsc: Double, val central: String, val altMax: Double)

    fun solarLocal(ut: Double, lat: Double, lon: Double): SolarLocal? {
        val f = { t: Double -> val x = solarDisks(t, lat, lon); x.d - x.rs - x.rm }
        var best: Double? = null; var bt = 0.0
        var t = ut - 4 * 3600000
        while (t <= ut + 4 * 3600000) { val x = solarDisks(t, lat, lon); val g = x.d - x.rs - x.rm; if (best == null || g < best) { best = g; bt = t }; t += 60000 }
        if (best == null || best >= 0) return null
        var a = bt - 60000; var b = bt + 60000
        for (i in 0 until 30) { val m1 = a + (b - a) * 0.382; val m2 = a + (b - a) * 0.618; if (solarDisks(m1, lat, lon).d < solarDisks(m2, lat, lon).d) b = m2 else a = m1 }
        val tm = (a + b) / 2; val X = solarDisks(tm, lat, lon)
        fun edge(dir: Int): Long? {
            var lo = tm; var hi = tm + dir * 4 * 3600000.0
            if (f(hi) < 0) return null
            for (k in 0 until 40) { val mid = (lo + hi) / 2; if (f(mid) < 0) lo = mid else hi = mid }
            return jsRound((lo + hi) / 2).toLong()
        }
        val central = if (X.d < abs(X.rm - X.rs)) (if (X.rm > X.rs) "total" else "annular") else ""
        return SolarLocal(edge(-1), jsRound(tm).toLong(), edge(1), (X.rs + X.rm - X.d) / (2 * X.rs), overlapFrac(X.d, X.rs, X.rm), central, X.alt)
    }

    data class PathPoint(val t: Double, val x: Double, val y: Double)
    data class Lunar(val p1: Long?, val u1: Long?, val u2: Long?, val max: Long, val u3: Long?, val u4: Long?, val p4: Long?, val umag: Double, val pmag: Double, val rU: Double, val rP: Double, val rM: Double, val path: List<PathPoint>)

    private class Geo(val x: Double, val y: Double, val d: Double, val rU: Double, val rP: Double, val rM: Double)
    private fun geo(t: Double): Geo {
        val m = Nature.moonPosition(t); val s = Nature.sunPosition(t)
        val ra0 = s.rightAscension + 180; val dec0 = -s.declination
        val x = ((m.rightAscension - ra0 + 540) % 360 - 180) * dcos(dec0); val y = m.declination - dec0
        val pm = m.parallax; val ps = 8.794 / 3600 / s.distance; val ss = 0.2666 / s.distance; val rm = 0.2725 * pm
        return Geo(x, y, sqrt(x * x + y * y), 1.0118 * pm + ps - ss, 1.0118 * pm + ps + ss, rm)
    }

    /** Ay tutulması temas anları (Danjon kuralı) ve gölgeye göre Ay'ın yolu */
    fun lunarContacts(ut: Double): Lunar {
        var a = ut - 3 * 3600000; var b = ut + 3 * 3600000
        for (i in 0 until 40) { val m1 = a + (b - a) * 0.382; val m2 = a + (b - a) * 0.618; if (geo(m1).d < geo(m2).d) b = m2 else a = m1 }
        val tm = (a + b) / 2; val G = geo(tm)
        fun cross(lim: (Geo) -> Double, dir: Int): Long? {
            val g = { t: Double -> val q = geo(t); q.d - lim(q) }
            if (g(tm) >= 0) return null
            var lo = tm; var hi = tm + dir * 4 * 3600000.0
            for (k in 0 until 40) { val mid = (lo + hi) / 2; if (g(mid) < 0) lo = mid else hi = mid }
            return jsRound((lo + hi) / 2).toLong()
        }
        val P = { q: Geo -> q.rP + q.rM }; val U = { q: Geo -> q.rU + q.rM }; val T = { q: Geo -> q.rU - q.rM }
        val p1 = cross(P, -1); val p4 = cross(P, 1)
        val from = p1?.toDouble() ?: (tm - 3 * 3600000); val to = p4?.toDouble() ?: (tm + 3 * 3600000)
        val path = ArrayList<PathPoint>()
        var t = from - 1800000
        while (t <= to + 1800000) { val q = geo(t); path.add(PathPoint(t, q.x, q.y)); t += 600000 }
        return Lunar(p1, cross(U, -1), cross(T, -1), jsRound(tm).toLong(), cross(T, 1), cross(U, 1), p4,
            (G.rU + G.rM - G.d) / (2 * G.rM), (G.rP + G.rM - G.d) / (2 * G.rM), G.rU, G.rP, G.rM, path)
    }
}
