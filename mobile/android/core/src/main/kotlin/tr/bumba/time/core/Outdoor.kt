package tr.bumba.time.core

/**
 * Dışarıdaki işler için saatlik uygunluk (2. analiz #113) ve hava kalitesi / polen düzeyleri (#111).
 * SaatCore/Outdoor.swift ile birebir aynı kurallar (testler iki tarafta aynı örneklerle).
 * Puan: 2 iyi, 1 orta, 0 uygun değil; bileşenlerin en kötüsü alınır.
 */
object Outdoor {
    enum class Activity(val key: String) { RUN("run"), BIKE("bike"), WALK("walk"), GARDEN("garden") }

    /** Bir saat: sıcaklık ve hissedilen °C, yağış ihtimali %, yağış mm, rüzgâr ve hamle km/sa, WMO kodu, gündüz mü, UV */
    data class Hour(
        val temp: Double, val feels: Double, val rain: Int, val mm: Double, val wind: Double, val gust: Double,
        val code: Int, val isDay: Boolean, val uv: Double = 0.0,
    )

    /** Hissedilen sıcaklık aralıkları: iyi [a, b], orta [c, a) ve (b, d] */
    private fun tempBands(a: Activity): DoubleArray = when (a) {
        Activity.RUN -> doubleArrayOf(5.0, 20.0, -5.0, 28.0)
        Activity.BIKE -> doubleArrayOf(10.0, 25.0, 3.0, 32.0)
        Activity.WALK -> doubleArrayOf(10.0, 26.0, 0.0, 32.0)
        Activity.GARDEN -> doubleArrayOf(10.0, 27.0, 5.0, 32.0)
    }

    /** Rüzgâr sınırları: orta (ortalama), uygun değil (ortalama), uygun değil (hamle) */
    private fun windBands(a: Activity): DoubleArray = when (a) {
        Activity.RUN -> doubleArrayOf(25.0, 40.0, 60.0)
        Activity.BIKE -> doubleArrayOf(20.0, 30.0, 45.0)
        Activity.WALK -> doubleArrayOf(30.0, 45.0, 65.0)
        Activity.GARDEN -> doubleArrayOf(25.0, 40.0, 60.0)
    }

    fun score(a: Activity, h: Hour): Int {
        // Gök gürültülü fırtına ve donan yağmur/çisenti her iş için uygun değil
        if (h.code in 95..99 || h.code in 56..57 || h.code in 66..67) return 0
        var s = 2
        // Yağış
        if (h.rain >= 60 || h.mm >= 1.0) return 0
        if (h.rain >= 30 || h.mm >= 0.2) s = minOf(s, 1)
        // Sıcaklık (hissedilen)
        val t = tempBands(a)
        val f = h.feels
        s = minOf(s, if (f >= t[0] && f <= t[1]) 2 else if (f >= t[2] && f <= t[3]) 1 else 0)
        // Rüzgâr ve hamle
        val w = windBands(a)
        if (h.wind > w[1] || (h.gust >= 0 && h.gust > w[2])) return 0
        if (h.wind > w[0]) s = minOf(s, 1)
        // Gece: bahçe işi uygun değil, diğerleri en çok orta
        if (!h.isDay) s = if (a == Activity.GARDEN) 0 else minOf(s, 1)
        // Güçlü UV: bisiklet dışında en çok orta
        if (h.uv >= 8 && a != Activity.BIKE) s = minOf(s, 1)
        return s
    }

    /** En iyi saatin sırası (en yüksek puan, eşitlikte en erken); hepsi uygun değilse null */
    fun best(a: Activity, hours: List<Hour>): Int? {
        var bi = -1
        var bs = 0
        for ((i, h) in hours.withIndex()) {
            val s = score(a, h)
            if (s > bs) { bs = s; bi = i }
        }
        return if (bi >= 0) bi else null
    }

    /** Avrupa hava kalitesi endeksi (EAQI) düzeyi: 0 iyi, 1 makul, 2 orta, 3 kötü, 4 çok kötü, 5 son derece kötü */
    fun aqiLevel(eaqi: Double): Int = when {
        eaqi < 20 -> 0
        eaqi < 40 -> 1
        eaqi < 60 -> 2
        eaqi < 80 -> 3
        eaqi < 100 -> 4
        else -> 5
    }

    /** Polen (tane/m³) düzeyi: 0 yok (< 1), 1 düşük (< 20), 2 orta (< 100), 3 yüksek */
    fun pollenLevel(grains: Double): Int = when {
        grains < 1 -> 0
        grains < 20 -> 1
        grains < 100 -> 2
        else -> 3
    }
}
