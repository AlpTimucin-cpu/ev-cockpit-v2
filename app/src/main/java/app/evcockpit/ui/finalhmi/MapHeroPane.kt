package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import android.view.Gravity
import android.util.Log
import app.evcockpit.BuildConfig
import kotlin.math.roundToInt
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.maps.Style
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.MapLayer
import app.evcockpit.ui.unified.map.LocalSharedMapHost
import app.evcockpit.ui.unified.map.MapHostLoadState
import app.evcockpit.ui.unified.map.MapHostViewport
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

/** F1 yalnız temel haritayı açar; GPS, rota ve şarj istasyonu TAKLİDİ yapmaz. */
private enum class MapLoadState { LOADING, READY, ERROR }

@Composable
fun MapHeroPane(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onOpenRouteTab: () -> Unit = {},
) {
    val t = CockpitDesignTokens
    val inspection = LocalInspectionMode.current
    val host = LocalSharedMapHost.current
    var layerMenu by remember { mutableStateOf(false) }
    val layer = host?.layer ?: MapLayer.DARK
    val map = host?.map
    val loadState = host?.loadState ?: MapHostLoadState.LOADING
    val issue = host?.issue

    Surface(
        modifier = modifier,
        shape = t.corner,
        color = t.mapFallback,
        border = BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        // F1 önceki sürümde özet haritanın üzerine biniyor, OSM atfı ile
        // ve uzun fiyat etiketiyle çakışıyordu. Header/harita/özet ayrı alan.
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(t.panel)
                    .padding(t.spaceXS),
                horizontalArrangement = Arrangement.spacedBy(t.spaceXS),
            ) {
                listOf("Nereden  —", "Nereye  —").forEach { label ->
                    OutlinedButton(
                        onClick = onOpenRouteTab,
                        modifier = Modifier.weight(1f).heightIn(min = t.target),
                        shape = t.cornerSmall,
                        contentPadding = PaddingValues(horizontal = t.spaceXS),
                    ) {
                        Text(label, color = t.text, fontSize = t.bodyText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).background(t.mapFallback),
            ) {
                MapHostViewport(
                    modifier = Modifier.fillMaxSize(),
                    unavailableMessage = "Harita gösterilemiyor • yalnız gerçek harita verisi",
                )

                // Kısa harita: 48dp × 3 düğme, tek satır ve SAĞ üst köşe.
                // F3 konum eylemi devre dışı olduğundan kısa moddan çıkarılır.
                // Bu yerleşim Ankara gibi merkez harita etiketlerini örtmez.
                if (maxHeight < t.mapControlsVerticalMinHeight) {
                    Row(
                        Modifier.align(Alignment.TopEnd)
                            .padding(horizontal = t.mapControlSpacing)
                            .background(t.mutedScrim, t.cornerSmall),
                        horizontalArrangement = Arrangement.spacedBy(t.mapControlSpacing),
                    ) {
                        MapToolButton("+", "Yakınlaştır", map != null, compact = true) {
                            map?.animateCamera(CameraUpdateFactory.zoomIn())
                        }
                        MapToolButton("−", "Uzaklaştır", map != null, compact = true) {
                            map?.animateCamera(CameraUpdateFactory.zoomOut())
                        }
                        MapLayerSelectButton(
                            compact = true,
                            layerMenu = layerMenu,
                            onMenuChange = { layerMenu = it },
                            onChoose = { candidate ->
                                host?.selectLayer(candidate)
                            },
                        )
                    }
                } else {
                    Column(
                        Modifier.align(Alignment.CenterStart)
                            .padding(t.spaceXS)
                            .background(t.mutedScrim, t.cornerSmall),
                        verticalArrangement = Arrangement.spacedBy(t.spaceXS),
                    ) {
                        MapToolButton("+", "Yakınlaştır", map != null) {
                            map?.animateCamera(CameraUpdateFactory.zoomIn())
                        }
                        MapToolButton("−", "Uzaklaştır", map != null) {
                            map?.animateCamera(CameraUpdateFactory.zoomOut())
                        }
                        MapToolButton("◎", "Konum (F3 aşamasında)", false) { }
                        MapLayerSelectButton(
                            compact = false,
                            layerMenu = layerMenu,
                            onMenuChange = { layerMenu = it },
                            onChoose = { candidate ->
                                host?.selectLayer(candidate)
                            },
                        )
                    }
                }

                if (!inspection && loadState != MapHostLoadState.READY) {
                    Surface(
                        color = t.mutedScrim,
                        shape = t.cornerSmall,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .padding(t.spaceS),
                    ) {
                        Column(
                            Modifier.padding(t.spaceS),
                            verticalArrangement = Arrangement.spacedBy(t.spaceXXS),
                        ) {
                            Text(
                                if (loadState == MapHostLoadState.LOADING)
                                    "Harita yükleniyor…" else "Harita gösterilemiyor",
                                color = t.text, fontSize = t.bodyText,
                            )
                            if (loadState == MapHostLoadState.ERROR) {
                                OutlinedButton(
                                    onClick = {
                                        host?.retry()
                                    },
                                    modifier = Modifier.heightIn(min = t.target),
                                ) { Text("Yeniden dene", fontSize = t.bodyText) }
                            }
                        }
                    }
                }

                issue?.let { message ->
                    Text(
                        message,
                        color = t.caution,
                        fontSize = t.labelText,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(t.spaceXS)
                            .background(t.mutedScrim, t.cornerSmall)
                            .padding(t.spaceXS),
                    )
                }

            }

            // Doğrudan harita üstüne çizilmez: uzun maliyet bilgisi satır atlayabilir.
            Column(
                Modifier.fillMaxWidth().background(t.panel)
                    .padding(t.spaceS),
                verticalArrangement = Arrangement.spacedBy(t.spaceS),
            ) {
                Text(
                    "Türkiye genel haritası • GPS konumu değil",
                    color = t.secondary,
                    fontSize = t.tinyText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceM)) {
                    MapSummaryValue(
                        "Varış (tahmini)",
                        DashboardFormat.number(state.arrivalMinutes, " dk"),
                        Modifier.weight(1f),
                    )
                    MapSummaryValue(
                        "Kalan mesafe",
                        DashboardFormat.number(state.remainingDistanceKm, " km"),
                        Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceM)) {
                    MapSummaryValue(
                        "Varış SoC (tahmini)",
                        DashboardFormat.number(state.arrivalSocPercent, " %"),
                        Modifier.weight(1f),
                    )
                    MapSummaryValue(
                        "Şarj ücreti (tahmini)",
                        state.chargeTotalLabel.value ?: "—",
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 48dp kompakt dokunma hedefi, 56dp standart hedef.
 * Dış yarı saydam kaplamayla karanlık karo üstünde de okunabilir.
 */
@Composable
private fun MapToolButton(
    label: String,
    description: String,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val t = CockpitDesignTokens
    val targetSize = if (compact) t.mapControlTouchTarget else t.target
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = t.mutedScrim,
            disabledContainerColor = t.mutedScrim,
        ),
        contentPadding = PaddingValues(t.spaceXXS),
        shape = if (compact) t.mapControlVisualRadius else t.cornerSmall,
        modifier = Modifier.size(targetSize)
            .semantics { contentDescription = description },
    ) {
        Text(label, color = if (enabled) t.text else t.secondary,
            fontSize = t.headText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MapLayerSelectButton(
    compact: Boolean,
    layerMenu: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onChoose: (MapLayer) -> Unit,
) {
    val t = CockpitDesignTokens
    Box {
        MapToolButton(
            "▤", "Harita katmanı", true, compact = compact,
        ) { onMenuChange(true) }
        DropdownMenu(
            expanded = layerMenu,
            onDismissRequest = { onMenuChange(false) },
        ) {
            listOf(
                "Sokak" to MapLayer.LIBERTY,
                "Uydu" to MapLayer.SATELLITE,
                "Arazi" to MapLayer.TOPOGRAPHIC,
                "Koyu" to MapLayer.DARK,
            ).forEach { (name, candidate) ->
                DropdownMenuItem(
                    text = {
                        Text(if (candidate.styleUrl.isBlank())
                            "$name • F2'de etkinleşecek" else name)
                    },
                    enabled = candidate.styleUrl.isNotBlank(),
                    modifier = Modifier.heightIn(min = t.target),
                    onClick = {
                        onMenuChange(false)
                        onChoose(candidate)
                    },
                )
            }
        }
    }
}

/**
 * Kontrast eşlemesi saf veridir: katman ID'si ve renk yalnız token dosyasından.
 * OpenFreeMap sunucusunun stil JSON'u değiştirilmez.
 */
internal data class F1RoadContrastSpec(val layerId: String, val color: Color)

internal object F1DarkRoadContrast {
    val mappings: List<F1RoadContrastSpec> get() = listOf(
        F1RoadContrastSpec(
            "highway_major_subtle", CockpitDesignTokens.mapRoadMajorContrast),
        F1RoadContrastSpec(
            "highway_minor", CockpitDesignTokens.mapRoadMinorContrast),
        F1RoadContrastSpec(
            "highway_motorway_subtle", CockpitDesignTokens.mapRoadMinorContrast),
    )
}

/** Sadece gerçekten bulunan ve güncellenen LineLayer ID'lerini döndürür. */
private fun applyDarkRoadContrast(style: Style): List<String> =
    F1DarkRoadContrast.mappings.mapNotNull { spec ->
        runCatching {
            val line = style.getLayer(spec.layerId) as? LineLayer
                ?: return@runCatching null
            line.setProperties(PropertyFactory.lineColor(spec.color.toArgb()))
            spec.layerId
        }.getOrNull()
    }

/** Yalnız DEBUG derlemesinde katman/fallback/kontrast tanısı. */
private fun f1MapLog(message: String) {
    if (BuildConfig.DEBUG) Log.d("F1Map", message)
}

@Composable
private fun MapSummaryValue(label: String, value: String, modifier: Modifier) {
    val t = CockpitDesignTokens
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(t.spaceXXS),
    ) {
        Text(
            label, color = t.secondary, fontSize = t.labelText, maxLines = 2,
        )
        Text(
            value, color = t.text, fontSize = t.bodyText,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
    }
}
