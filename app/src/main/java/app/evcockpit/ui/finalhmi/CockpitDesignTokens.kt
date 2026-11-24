package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * F1 ve sonraki F ekranları için TEK tasarım token kaynağı.
 * Buradaki renk ve ölçüler fiziksel araçtan okunan veri değildir.
 */
object CockpitDesignTokens {
    val background = Color(0xFF080E19)
    val panel = Color(0xFF121E30)
    val panelRaised = Color(0xFF1C2B41)
    val mapFallback = Color(0xFF0D1C2B)
    val outline = Color(0xFF30425A)
    val text = Color(0xFFF4F8FF)
    val secondary = Color(0xFFACC0D4)
    val accent = Color(0xFF1677FF)
    val accentSoft = Color(0xFF14345C)
    val regenerate = Color(0xFF32CF99)
    val traction = Color(0xFF1677FF)
    val caution = Color(0xFFFFC878)
    val error = Color(0xFFFF7B84)
    val neutralTrack = Color(0xFF42536A)
    val demoBanner = Color(0xFF4C2A06)
    val mutedScrim = Color(0xCC0A1423)

    val corner = RoundedCornerShape(18.dp)
    val cornerSmall = RoundedCornerShape(16.dp)
    val cornerTiny = RoundedCornerShape(10.dp)
    val cardOutlineWidth = 1.dp
    val spaceXXS = 3.dp
    val spaceXS = 6.dp
    val spaceS = 9.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXL = 20.dp
    val spaceXXL = 28.dp
    val target = 56.dp
    val topBarMinHeight = 56.dp
    val tabBarHeight = 70.dp
    val bannerMinHeight = 36.dp
    val phoneMapHeight = 388.dp
    val compactMapHeight = 312.dp
    val wideWeatherHeight = 112.dp
    val speedCardMinHeight = 116.dp
    val metricCardMinHeight = 72.dp
    val chargingCardMinHeight = 140.dp
    val mapSummaryHeight = 116.dp
    val mapControlsVerticalMinHeight = 280.dp
    // Kısa haritada sağ üstte üç düğme; 48dp asgari erişilebilir dokunma hedefi.
    // A0b/C2: harita GeoJSON katman ve çizim değerleri tek kaynaktan.
    val routeAltColor = Color(0xFF8292A8)
    val routeCasingColor = Color(0xFF102033)
    val stationAcColor = Color(0xFF4FA0EF)
    val stationDc50Color = Color(0xFF20BFA4)
    val stationDc100Color = Color(0xFFAA76E6)
    val stationDc150Color = Color(0xFFDA83D9)
    val vehicleAccuracyFill = Color(0x331677FF)
    val startPinColor = Color(0xFF25CF8F)
    val finishPinColor = Color(0xFFFFB364)
    val weatherIconColor = caution
    val routeSelectedWidth = 6.dp
    val routeCasingWidth = 9.dp
    val routeAlternativeWidth = 5.dp
    val stationMarkerSize = 42.dp
    val vehicleMarkerSize = 34.dp
    val endpointMarkerSize = 36.dp
    val weatherMarkerSize = 30.dp
    val mapLabelSize = 12.sp
    val mapMarkerSelectedScale = 1.3f
    const val maxAlternativeOverlayRoutes = 3
    const val accuracyCircleVertices = 64
    const val maxOverlayStations = 250
    const val maxOverlayWeatherAlerts = 120
    const val overlayMaxSampledRoutePoints = 2500
    const val overlaySimplificationToleranceMeters = 12.0
    const val overlayCarHeadingFreezeMetersPerSecond = 1.5
    const val overlayMaxPositionHz = 2.0
    const val cameraAnimationMs = 300
    // C3: üç duraklı ekran; Android dp alanı çalışma anında ölçülür.
    val sheetPeekHeight = 120.dp
    const val sheetHalfFraction = 0.45f
    val sheetCorner = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val sheetHandleWidth = 42.dp
    val sheetHandleHeight = 5.dp
    val sheetHandleTouchHeight = 48.dp
    val sheetHandleColor = outline
    val sheetAnchorMinGap = 24.dp
    val sheetVelocityThreshold = 125.dp
    const val sheetPositionalThresholdFraction = 0.5f
    val sheetContentPadding = 16.dp
    val searchBarHeight = 56.dp
    val searchBarCorner = RoundedCornerShape(18.dp)
    val sheetDefaultFullTopInset = 8.dp
    val panelCardSpacing = 9.dp
    val mapControlTouchTarget = 48.dp
    val mapControlVisualRadius = RoundedCornerShape(10.dp)
    val mapControlSpacing = 4.dp
    val mapAttributionSafeInset = 12.dp
    val mapRoadMajorContrast = Color(0xFF435160)
    val mapRoadMinorContrast = Color(0xFF303940)
    // 393dp telefonda 3 örnek yan yana; 4+ örnekte eşik aşılıyorsa kaydır.
    val weatherCellMinWidth = 96.dp
    // Kaydırılabilir kart içinde uzun Türkçe uyarı metni satır kırabilsin.
    val weatherScrollCellWidth = 144.dp
    val weatherScrollFadeWidth = 20.dp
    val transparent = Color.Transparent
    val batteryBarHeight = 6.dp
    val mapBorderRadius = 18.dp
    val mapAttributionPadding = 4.dp

    const val mapWeight = 0.65f
    const val dataWeight = 0.35f
    val expandedMinWidth = 840.dp
    val compactMaxWidth = 600.dp

    val speedText = 48.sp
    val metricNumber = 25.sp
    val headText = 21.sp
    val titleText = 17.sp
    val bodyText = 14.sp
    val labelText = 12.sp
    val tinyText = 11.sp
    val tabText = 13.sp
    val fontWeightStrong = FontWeight.SemiBold
}

private val finalColors = darkColorScheme(
    primary = CockpitDesignTokens.accent,
    background = CockpitDesignTokens.background,
    surface = CockpitDesignTokens.panel,
    onBackground = CockpitDesignTokens.text,
    onSurface = CockpitDesignTokens.text,
    onPrimary = CockpitDesignTokens.text,
    secondary = CockpitDesignTokens.secondary,
    outline = CockpitDesignTokens.outline,
    error = CockpitDesignTokens.error,
)

private val finalType = Typography(
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.bodyText,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.bodyText,
        fontWeight = FontWeight.Normal,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.titleText,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.titleText,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.headText,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.labelText,
        fontWeight = FontWeight.Normal,
    ),
)

@Composable
fun FinalCockpitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = finalColors, typography = finalType, content = content)
}
