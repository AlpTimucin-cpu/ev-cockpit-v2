package app.evcockpit.ui.unified.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Ağ/uzak görsel kullanılmaz. PNG/Bitmap, Android Canvas ile bellekte üretilir.
 * Aynı yoğunluk ve aynı tür için ikinci kez çizilmeyip önbellekten verilir.
 */
class MarkerIcons(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val cached = mutableMapOf<String, Bitmap>()
    private val t = CockpitDesignTokens

    fun all(): Map<String, Bitmap> = mapOf(
        OverlayIds.STATION_UNKNOWN_ICON to icon(OverlayIds.STATION_UNKNOWN_ICON),
        OverlayIds.STATION_AC_ICON to icon(OverlayIds.STATION_AC_ICON),
        OverlayIds.STATION_50_ICON to icon(OverlayIds.STATION_50_ICON),
        OverlayIds.STATION_100_ICON to icon(OverlayIds.STATION_100_ICON),
        OverlayIds.STATION_150_ICON to icon(OverlayIds.STATION_150_ICON),
        OverlayIds.START_ICON to icon(OverlayIds.START_ICON),
        OverlayIds.FINISH_ICON to icon(OverlayIds.FINISH_ICON),
        OverlayIds.WEATHER_ICON to icon(OverlayIds.WEATHER_ICON),
        OverlayIds.VEHICLE_ICON to icon(OverlayIds.VEHICLE_ICON),
        OverlayIds.VEHICLE_UNKNOWN_ICON to icon(OverlayIds.VEHICLE_UNKNOWN_ICON),
    )

    private fun icon(id: String): Bitmap = cached.getOrPut(id) {
        val dp = when (id) {
            OverlayIds.VEHICLE_ICON -> t.vehicleMarkerSize
            OverlayIds.START_ICON, OverlayIds.FINISH_ICON -> t.endpointMarkerSize
            OverlayIds.WEATHER_ICON -> t.weatherMarkerSize
            else -> t.stationMarkerSize
        }
        val size = max(18, (dp.value * density).roundToInt())
        val picture = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(picture)
        val brush = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f
        val radius = center * 0.80f
        val color = when (id) {
            OverlayIds.STATION_AC_ICON -> t.stationAcColor
            OverlayIds.STATION_50_ICON -> t.stationDc50Color
            OverlayIds.STATION_100_ICON -> t.stationDc100Color
            OverlayIds.STATION_150_ICON -> t.stationDc150Color
            OverlayIds.START_ICON -> t.startPinColor
            OverlayIds.FINISH_ICON -> t.finishPinColor
            OverlayIds.WEATHER_ICON -> t.weatherIconColor
            OverlayIds.VEHICLE_ICON -> t.accent
            else -> t.neutralTrack
        }
        if (id == OverlayIds.VEHICLE_ICON) {
            drawVehicle(canvas, brush, size, color)
        } else {
            brush.color = Color.White.toArgb()
            canvas.drawCircle(center, center, radius, brush)
            brush.color = color.toArgb()
            canvas.drawCircle(center, center, radius * 0.80f, brush)
            brush.color = Color.White.toArgb()
            when (id) {
                OverlayIds.WEATHER_ICON -> {
                    brush.style = Paint.Style.STROKE
                    brush.strokeWidth = max(2f, size * 0.075f)
                    val cloudY = center - radius * 0.12f
                    canvas.drawArc(
                        center - radius * 0.43f, cloudY - radius * 0.26f,
                        center + radius * 0.43f, cloudY + radius * 0.32f,
                        180f, 180f, false, brush,
                    )
                    brush.style = Paint.Style.FILL
                    canvas.drawCircle(center - radius * 0.16f,
                        center + radius * 0.40f, size * 0.07f, brush)
                    canvas.drawCircle(center + radius * 0.17f,
                        center + radius * 0.40f, size * 0.07f, brush)
                }
                OverlayIds.START_ICON, OverlayIds.FINISH_ICON ->
                    canvas.drawCircle(center, center, radius * 0.25f, brush)
                else -> {
                    val path = Path().apply {
                        moveTo(center + radius * 0.01f, center - radius * 0.60f)
                        lineTo(center - radius * 0.34f, center + radius * 0.06f)
                        lineTo(center - radius * 0.04f, center + radius * 0.05f)
                        lineTo(center - radius * 0.20f, center + radius * 0.60f)
                        lineTo(center + radius * 0.40f, center - radius * 0.13f)
                        lineTo(center + radius * 0.13f, center - radius * 0.12f)
                        close()
                    }
                    canvas.drawPath(path, brush)
                }
            }
        }
        picture
    }

    private fun drawVehicle(canvas: Canvas, brush: Paint, size: Int, color: Color) {
        val c = size / 2f
        val p = Path().apply {
            moveTo(c, size * 0.10f)
            lineTo(size * 0.84f, size * 0.87f)
            lineTo(c, size * 0.68f)
            lineTo(size * 0.16f, size * 0.87f)
            close()
        }
        brush.color = Color.White.toArgb()
        canvas.drawCircle(c, c, size * 0.44f, brush)
        brush.color = color.toArgb()
        canvas.drawPath(p, brush)
    }
}
