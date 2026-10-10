package tr.bumba.time.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Ortak test verisi: mobile/shared/testdata (sitedeki JS hesaplarından üretilir: node mobile/shared/gen-vectors.mjs). */
internal object Vectors {
    private val dir = File(System.getProperty("vectors.dir") ?: error("vectors.dir sistem özelliği yok"))

    fun load(name: String): JSONObject = JSONObject(File(dir, name).readText())
}

internal fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
