package app.evcockpit.ui.tesla

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

/**
 * Single source of truth for the EV Cockpit visual system.
 * Values are presentation constants, never asserted as actual vehicle readings.
 */
object TeslaTokens {
    val background = Color(0xFF0B0D10)
    val surface = Color(0xFF151A20)
    val surfaceRaised = Color(0xFF1B2026)
    val outline = Color(0xFF22272E)
    val text = Color(0xFFFFFFFF)
    val secondaryText = Color(0xFF8A949E)
    val accent = Color(0xFF3DDC84)
    val warning = Color(0xFFFFB300)
    val error = Color(0xFFFF5252)
    val estimate = Color(0xFFFF8A4C)

    val cornerRadius = 18.dp
    val contentPadding = 16.dp
    val spacing = 12.dp
    val touchTarget = 56.dp
    val speedLarge = 112.sp
    val speedCompact = 96.sp
    val batterySize = 56.sp
    val labelSize = 14.sp
    val bodySize = 16.sp
    val heroTitle = 20.sp

    const val drivingThresholdKmh = 5.0
    const val staleObdAfterMs = 3_000L
    const val animationMillis = 150
    const val referencePowerKw = 125.0

    const val leftPaneWeight = 0.26f
    const val mapWeight = 0.54f
    const val actionWeight = 0.20f
}

/** Ordinary system font, minimum semantic/body text size 14sp. */
val TeslaTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 16.sp,
        lineHeight = 23.sp, fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 16.sp,
        lineHeight = 22.sp, fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 14.sp,
        lineHeight = 20.sp, fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 16.sp,
        lineHeight = 20.sp, fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 14.sp,
        lineHeight = 18.sp, fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 14.sp,
        lineHeight = 18.sp, fontWeight = FontWeight.Medium,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 20.sp,
        lineHeight = 26.sp, fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontSize = 18.sp,
        lineHeight = 23.sp, fontWeight = FontWeight.Medium,
    ),
)

enum class SourceKind(val label: String) {
    OBD("OBD"), GPS("GPS"), MANUEL("MANUEL"), DEMO("DEMO"), YOK("YOK"), ESKI("ESKİ")
}

enum class PanelStatus {
    NORMAL, EMPTY, LOADING, ERROR
}

/** Pure, testable display policy; never fabricate a missing vehicle reading. */
object TeslaDisplayPolicy {
    fun speed(kmh: Double?): String =
        kmh?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { String.format(Locale.ROOT, "%.0f", it) } ?: "--"

    fun soc(percent: Double?): String =
        percent?.takeIf { it.isFinite() && it in 0.0..100.0 }
            ?.let { String.format(Locale.ROOT, "%.0f", it) } ?: "--"

    fun estimatedEnergy(kwh: Double?): String =
        kwh?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { String.format(Locale.ROOT, "≈ %.1f kWh • TAHMİN", it) }
            ?: "Enerji verisi yok"

    fun estimatedRange(km: Double?): String =
        km?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { String.format(Locale.ROOT, "~%.0f km • TAHMİN", it) }
            ?: "Menzil hesaplanamadı"

    fun powerFraction(powerKw: Double?, referenceKw: Double = TeslaTokens.referencePowerKw): Float {
        if (powerKw == null || !powerKw.isFinite() ||
            !referenceKw.isFinite() || referenceKw <= 0.0) return 0f
        return (abs(powerKw) / referenceKw).coerceIn(0.0, 1.0).toFloat()
    }
}
