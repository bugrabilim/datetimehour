package tr.bumba.time.wear

import android.app.PendingIntent
import android.content.Intent
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant

private const val RESOURCES = "1"

/**
 * Kutucuk: sıradaki vakit (Ramazan'da iftar/sahur), saati ve yer; altında bugünün vakitleri. Sıradaki vakitte yenilenir.
 */
class PrayerTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val now = Instant.now()
        val place = Wear.place()
        val next = place.next(now)
        val col = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        if (next != null) {
            col.addContent(text(Wear.label(next, place.zone), 15f, 0xFFF59E0B.toInt(), bold = true))
            col.addContent(text(place.hm(next.at), 34f, 0xFFF5F1E8.toInt(), bold = true))
        }
        col.addContent(text(place.name, 12f, 0xFFA8A29E.toInt()))
        // Bugünün vakitleri tek satırda iki sütun: "İmsak 06:00 · Güneş 07:25 …" kısaltılmadan alt alta
        val today = place.entries(now.atZone(place.zone).toLocalDate())
        for (row in today.chunked(2)) {
            col.addContent(text(row.joinToString("   ") { Wear.prayerName(it.prayer) + " " + place.hm(it.at) }, 11f, 0xFFF5F1E8.toInt()))
        }
        val root = LayoutElementBuilders.Box.Builder()
            .setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setClickable(ModifiersBuilders.Clickable.Builder().setId("open")
                    .setOnClick(ActionBuilders.launchApp(packageName)).build())
                .build())
            .addContent(col.build())
            .build()
        val fresh = next?.let { maxOf(60_000L, it.at.toEpochMilli() - now.toEpochMilli() + 1000) } ?: 3_600_000L
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES)
            .setFreshnessIntervalMillis(fresh)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
            .build()
        return ResolvableFuture.create<TileBuilders.Tile>().also { it.set(tile) }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        ResolvableFuture.create<ResourceBuilders.Resources>().also { it.set(ResourceBuilders.Resources.Builder().setVersion(RESOURCES).build()) }

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false): LayoutElementBuilders.Text =
        LayoutElementBuilders.Text.Builder()
            .setText(s)
            .setMaxLines(1)
            .setFontStyle(LayoutElementBuilders.FontStyle.Builder()
                .setSize(sp(size))
                .setColor(argb(color))
                .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                .build())
            .setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setPadding(ModifiersBuilders.Padding.Builder().setTop(dp(1f)).build()).build())
            .build()
}

/** Kutucukta dokununca uygulama açılır */
private object ActionBuilders {
    fun launchApp(pkg: String): androidx.wear.protolayout.ActionBuilders.LaunchAction =
        androidx.wear.protolayout.ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(androidx.wear.protolayout.ActionBuilders.AndroidActivity.Builder()
                .setPackageName(pkg).setClassName(WearActivity::class.java.name).build())
            .build()
}

private fun ComplicationDataSourceTap(service: android.app.Service): PendingIntent = PendingIntent.getActivity(
    service, 0, Intent(service, WearActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private fun plain(s: String) = PlainComplicationText.Builder(s).build()

/** Komplikasyon: sıradaki vakit; kısa metinde saat + vakit adı, uzun metinde ad, saat ve akan geri sayım */
class NextPrayerComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? = data(type, Wear.text("prayer.dhuhr"), "12:53", null)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val place = Wear.place()
        val next = place.next(Instant.now()) ?: return null
        return data(request.complicationType, Wear.label(next, place.zone), place.hm(next.at), next.at)
    }

    private fun data(type: ComplicationType, name: String, time: String, at: Instant?): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(plain(time), plain("$name $time"))
            .setTitle(plain(name))
            .setTapAction(ComplicationDataSourceTap(this))
            .build()
        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
            if (at != null) TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.SHORT_DUAL_UNIT, CountDownTimeReference(at)).build()
            else plain("1 h 20 m"),
            plain("$name $time"),
        )
            .setTitle(plain("$name $time"))
            .setTapAction(ComplicationDataSourceTap(this))
            .build()
        else -> null
    }
}

/** Komplikasyon: hicri tarih (Diyanet takvimi); kısa metinde gün ve ay, uzun metinde tam tarih */
class HijriComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? = data(type, Triple(1448, 5, 19))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val h = Wear.hijri(java.time.LocalDate.now()) ?: return null
        return data(request.complicationType, h)
    }

    private fun data(type: ComplicationType, h: Triple<Int, Int, Int>): ComplicationData? {
        val (y, m, d) = h
        val month = Wear.text("hijri.month.$m")
        val full = Wear.text("hijri.fmt", d, month, y)
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(plain("$d"), plain(full))
                .setTitle(plain(month.take(4)))
                .setTapAction(ComplicationDataSourceTap(this))
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(plain(full), plain(full))
                .setTitle(plain(Wear.text("widget.hijri.name")))
                .setTapAction(ComplicationDataSourceTap(this))
                .build()
            else -> null
        }
    }
}
