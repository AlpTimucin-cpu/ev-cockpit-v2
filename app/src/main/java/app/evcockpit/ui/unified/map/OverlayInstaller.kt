package app.evcockpit.ui.unified.map

import android.content.Context
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import app.evcockpit.domain.nav.PolylineGeometry
import app.evcockpit.domain.nav.WgsPoint
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * C2: yalnız gerçek OverlayModel'i çiz. Boş model hiçbir çizgi/pin üretmez.
 * Tüm MapLibre işlemleri ana iş parçacığında gerçekleştirilir.
 * Stil yeniden yüklenince kaynaklar otomatik olarak geri kurulur.
 */
class OverlayInstaller(context: Context) {
    private val icons = MarkerIcons(context.applicationContext)
    private val t = CockpitDesignTokens

    /** Kaynak/katman zaten varsa yeniden eklemek yerine verisini günceller. */
    fun install(style: Style, ui: OverlayModel) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "MapLibre stilleri yalnız ana iş parçacığında güncellenebilir."
        }
        icons.all().forEach { (id, bmp) -> style.addImage(id, bmp) }

        repeat(t.maxAlternativeOverlayRoutes) { index ->
            updateSource(style, OverlayIds.alternativeSource(index),
                lineCollection(ui.alternativeRoutes.getOrNull(index), ui.zoomLevel))
            if (style.getLayer(OverlayIds.alternativeLayer(index)) == null) {
                style.addLayer(LineLayer(OverlayIds.alternativeLayer(index),
                    OverlayIds.alternativeSource(index)).withProperties(
                    PropertyFactory.lineColor(t.routeAltColor.toArgb()),
                    PropertyFactory.lineWidth(t.routeAlternativeWidth.value),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                ))
            }
        }

        updateSource(style, OverlayIds.SELECTED_ROUTE_SOURCE,
            lineCollection(ui.selectedRoute, ui.zoomLevel))
        if (style.getLayer(OverlayIds.SELECTED_ROUTE_CASING) == null) {
            style.addLayer(LineLayer(OverlayIds.SELECTED_ROUTE_CASING,
                OverlayIds.SELECTED_ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(t.routeCasingColor.toArgb()),
                PropertyFactory.lineWidth(t.routeCasingWidth.value),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ))
        }
        if (style.getLayer(OverlayIds.SELECTED_ROUTE_LAYER) == null) {
            style.addLayer(LineLayer(OverlayIds.SELECTED_ROUTE_LAYER,
                OverlayIds.SELECTED_ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(t.accent.toArgb()),
                PropertyFactory.lineWidth(t.routeSelectedWidth.value),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ))
        }

        updateSource(style, OverlayIds.WEATHER_SOURCE,
            FeatureCollection.fromFeatures(ui.weatherAlerts
                .filter { it.point.valid() && it.rainExpected == true }
                .take(t.maxOverlayWeatherAlerts)
                .map { warning ->
                    Feature.fromGeometry(point(warning.point)).apply {
                        addStringProperty("icon", OverlayIds.WEATHER_ICON)
                        addStringProperty("label",
                            warning.text?.takeIf { it.isNotBlank() }
                                ?: "Yağış uyarısı var")
                    }
                }))
        if (style.getLayer(OverlayIds.WEATHER_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.WEATHER_LAYER,
                OverlayIds.WEATHER_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textColor(t.caution.toArgb()),
                PropertyFactory.textSize(t.mapLabelSize.value),
                PropertyFactory.textAllowOverlap(true),
            ))
        }

        updateSource(style, OverlayIds.STATIONS_SOURCE,
            FeatureCollection.fromFeatures(ui.stations
                .filter { it.point.valid() }.take(t.maxOverlayStations).map { station ->
                    Feature.fromGeometry(point(station.point)).apply {
                        addStringProperty("id", station.id)
                        addStringProperty("icon", stationIcon(station))
                        addNumberProperty("scale", if (station.selected)
                            t.mapMarkerSelectedScale else 1.0)
                    }
                }))
        if (style.getLayer(OverlayIds.STATIONS_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.STATIONS_LAYER,
                OverlayIds.STATIONS_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconSize(Expression.get("scale")),
                PropertyFactory.iconAllowOverlap(true),
            ))
        }

        val ends = buildList {
            ui.origin?.takeIf { it.valid() }?.let {
                add(iconFeature(it, OverlayIds.START_ICON))
            }
            ui.destination?.takeIf { it.valid() }?.let {
                add(iconFeature(it, OverlayIds.FINISH_ICON))
            }
        }
        updateSource(style, OverlayIds.ENDPOINTS_SOURCE,
            FeatureCollection.fromFeatures(ends))
        if (style.getLayer(OverlayIds.ENDPOINTS_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.ENDPOINTS_LAYER,
                OverlayIds.ENDPOINTS_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconAllowOverlap(true),
            ))
        }

        val vehicle = ui.vehicle?.takeIf { it.point.valid() }
        val accuracy = vehicle?.accuracyMeters?.takeIf {
            it.isFinite() && it > 0.0 && it <= 50_000.0
        }
        updateSource(style, OverlayIds.VEHICLE_ACCURACY_SOURCE,
            FeatureCollection.fromFeatures(if (vehicle != null && accuracy != null)
                listOf(Feature.fromGeometry(accuracyCircle(vehicle.point, accuracy)))
                else emptyList()))
        if (style.getLayer(OverlayIds.VEHICLE_ACCURACY_LAYER) == null) {
            style.addLayer(FillLayer(OverlayIds.VEHICLE_ACCURACY_LAYER,
                OverlayIds.VEHICLE_ACCURACY_SOURCE).withProperties(
                PropertyFactory.fillColor(t.vehicleAccuracyFill.toArgb()),
            ))
        }

        updateSource(style, OverlayIds.VEHICLE_SOURCE,
            FeatureCollection.fromFeatures(vehicle?.let { fix ->
                val validHeading = fix.bearingDegrees?.takeIf { it.isFinite() }
                listOf(Feature.fromGeometry(point(fix.point)).apply {
                    addStringProperty("icon",
                        if (validHeading == null) OverlayIds.VEHICLE_UNKNOWN_ICON
                        else OverlayIds.VEHICLE_ICON)
                    addNumberProperty("bearing", validHeading ?: 0.0)
                })
            } ?: emptyList()))
        if (style.getLayer(OverlayIds.VEHICLE_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.VEHICLE_LAYER,
                OverlayIds.VEHICLE_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconRotate(Expression.get("bearing")),
                PropertyFactory.iconRotationAlignment(
                    Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
            ))
        }
    }

    private fun stationIcon(station: OverlayStation): String {
        val kw = station.powerKw?.takeIf { it.isFinite() && it >= 0.0 }
        return when {
            kw == null -> OverlayIds.STATION_UNKNOWN_ICON
            !station.dc -> OverlayIds.STATION_AC_ICON
            kw >= 150.0 -> OverlayIds.STATION_150_ICON
            kw >= 100.0 -> OverlayIds.STATION_100_ICON
            kw >= 50.0 -> OverlayIds.STATION_50_ICON
            else -> OverlayIds.STATION_AC_ICON
        }
    }

    private fun iconFeature(point: OverlayPoint, iconId: String) =
        Feature.fromGeometry(point(point)).apply { addStringProperty("icon", iconId) }

    private fun point(p: OverlayPoint): Point = Point.fromLngLat(p.longitude, p.latitude)

    private fun updateSource(style: Style, id: String, collection: FeatureCollection) {
        val current = style.getSource(id) as? GeoJsonSource
        if (current != null) current.setGeoJson(collection)
        else style.addSource(GeoJsonSource(id, collection))
    }

    private fun lineCollection(
        geometry: List<OverlayPoint>?,
        zoom: Double?,
    ): FeatureCollection {
        val points = geometry.orEmpty().filter { it.valid() }
        if (points.size < 2) return FeatureCollection.fromFeatures(emptyList())
        // Zoom eşiği yalnız sadeleştirme kopyasını etkiler; orijinal rota saklanır.
        val scale = when {
            zoom == null || !zoom.isFinite() -> 1.0
            zoom < 8.0 -> 4.0
            zoom < 12.0 -> 2.0
            else -> 1.0
        }
        val input = points.map { WgsPoint(it.latitude, it.longitude) }
        val reduced = PolylineGeometry.simplify(
            input, t.overlaySimplificationToleranceMeters * scale)
        val stride = (reduced.size / t.overlayMaxSampledRoutePoints).coerceAtLeast(1)
        val sampled = reduced.filterIndexed { i, _ ->
            i % stride == 0 || i == reduced.lastIndex
        }
        val feature = Feature.fromGeometry(LineString.fromLngLats(
            sampled.map { Point.fromLngLat(it.lon, it.lat) },
        ))
        return FeatureCollection.fromFeatures(listOf(feature))
    }

    private fun accuracyCircle(center: OverlayPoint, meters: Double): Polygon {
        val earth = PolylineGeometry.EARTH_RADIUS_M
        val angle = meters / earth
        val cosLatitude = cos(Math.toRadians(center.latitude)).coerceAtLeast(0.00001)
        val ring = (0..t.accuracyCircleVertices).map { i ->
            val theta = 2.0 * PI * i / t.accuracyCircleVertices
            Point.fromLngLat(
                center.longitude + Math.toDegrees(angle * cos(theta) / cosLatitude),
                center.latitude + Math.toDegrees(angle * sin(theta)),
            )
        }
        return Polygon.fromLngLats(listOf(ring))
    }
}
