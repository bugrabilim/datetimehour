package tr.bumba.time.core

/**
 * Hava durumunun sözlü açıklaması (2. analiz #108, #110; 3 Kasım 2026): ölçüm kartlarındaki kısa açıklama ve günlük özet
 * cümlesi. Metin değil metin anahtarı döner (çeviriler uygulamada). Eşikler sitedeki weather-core.js `words`/`summary` ve
 * iOS'taki SaatCore WeatherWords ile aynıdır.
 */
object WeatherWords {
    /** Rüzgâr (km/sa): sakin, hafif esinti, rüzgârlı, kuvvetli, fırtına (Beaufort 0–1, 2–3, 4–5, 6–7, 8+) */
    fun wind(kmh: Double): String = "weather.wd." + when {
        kmh < 6 -> "calm"; kmh < 20 -> "light"; kmh < 39 -> "breezy"; kmh < 62 -> "strong"; else -> "gale"
    }

    /** UV indeksi (WHO ölçeği) */
    fun uv(uv: Double): String = "weather.uvw." + when {
        uv < 3 -> "low"; uv < 6 -> "moderate"; uv < 8 -> "high"; uv < 11 -> "veryHigh"; else -> "extreme"
    }

    /** Bağıl nem (%) */
    fun humidity(h: Double): String = "weather.hw." + when { h < 30 -> "dry"; h < 60 -> "comfy"; h < 80 -> "humid"; else -> "veryHumid" }

    /** Görüş uzaklığı (m) */
    fun visibility(m: Double): String = "weather.vw." + when { m < 1000 -> "fog"; m < 4000 -> "haze"; m < 10000 -> "good"; else -> "clear" }

    /** Çiy noktası (°C) */
    fun dew(c: Double): String = "weather.dw." + when { c < 10 -> "dry"; c < 16 -> "comfy"; c < 20 -> "muggy"; else -> "oppressive" }

    /** Hissedilen sıcaklık: 3° ve fazla farkta açıklama; yoksa null */
    fun feels(temp: Double, feels: Double): String? = when {
        feels <= temp - 3 -> "weather.fw.colder"; feels >= temp + 3 -> "weather.fw.warmer"; else -> null
    }

    /** Önümüzdeki saatlerden biri: yerel saat (0–23), WMO kodu, yağış ihtimali (%), rüzgâr (km/sa) */
    data class Hour(val hour: Int, val code: Int, val rain: Int, val wind: Double)

    /** Özet: anahtar; `hour` saatli anahtarlarda yerel saat, `n` derece farkında (yuvarlanmış, işaretsiz) */
    data class Summary(val key: String, val hour: Int = -1, val n: Int = 0)

    private fun isThunder(code: Int) = code in 95..99
    private fun isSnow(code: Int) = code in 71..77 || code in 85..86

    /**
     * Günlük özet cümlesi. `next`: şimdiki saatten başlayarak önümüzdeki saatler (en çok 12'si bakılır).
     * Öncelik: gök gürültüsü → yağış (ihtimal ≥ %60; kar kodu varsa kar) → öğleden sonra rüzgârlı (12–18 arası ≥ 30 km/sa)
     * → yarın en yüksek sıcaklık 5° ve fazla farklı → varsayılan (durum, en yüksek, en düşük).
     */
    fun summary(next: List<Hour>, todayMax: Double, tomorrowMax: Double?): Summary {
        val h = next.take(12)
        h.firstOrNull { isThunder(it.code) }?.let { return Summary("weather.sum.storm", it.hour) }
        h.firstOrNull { it.rain >= 60 }?.let { return Summary(if (isSnow(it.code)) "weather.sum.snow" else "weather.sum.rain", it.hour) }
        if (h.any { it.hour in 12..18 && it.wind >= 30 }) return Summary("weather.sum.windyAfternoon")
        if (tomorrowMax != null) {
            val d = tomorrowMax - todayMax
            if (d <= -5) return Summary("weather.sum.colder", n = Math.round(-d).toInt())
            if (d >= 5) return Summary("weather.sum.warmer", n = Math.round(d).toInt())
        }
        return Summary("weather.sum.default")
    }

    /** Akşam bildirimi (#110): yarın en yüksek sıcaklık bugünden 5° ve fazla farklıysa anahtar ve fark; yoksa null */
    fun tomorrowAlert(todayMax: Double, tomorrowMax: Double): Summary? {
        val d = tomorrowMax - todayMax
        return when {
            d <= -5 -> Summary("weather.alert.colder", n = Math.round(-d).toInt())
            d >= 5 -> Summary("weather.alert.warmer", n = Math.round(d).toInt())
            else -> null
        }
    }

    /** Rüzgâr yönü (geldiği yön, derece) → 8 yönden sırası (0 = kuzey, saat yönünde) */
    fun compass8(deg: Double): Int = (Math.floorMod(Math.round(deg / 45.0).toInt(), 8))
}
