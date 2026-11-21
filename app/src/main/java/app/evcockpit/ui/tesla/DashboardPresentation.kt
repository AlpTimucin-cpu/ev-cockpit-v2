package app.evcockpit.ui.tesla

import app.evcockpit.location.SpeedReading
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RoutePosition
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.PlannedStop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Pure presentation adapter; no I/O, no API requests, no fabricated measurements. */
object DashboardPresentation {
    fun source(label: String): SourceKind = when {
        label.startsWith("OBD", ignoreCase = true) -> SourceKind.OBD
        label.startsWith("MANUEL", ignoreCase = true) -> SourceKind.MANUEL
        label.startsWith("DEMO", ignoreCase = true) -> SourceKind.DEMO
        label.startsWith("ESKİ", ignoreCase = true) -> SourceKind.ESKI
        else -> SourceKind.YOK
    }

    fun vehicle(
        state: VehicleState,
        soc: SocReading,
        speed: SpeedReading,
        usableKwhAtSoh: Double,
    ): VehiclePanelUiState {
        val pct = soc.pct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        // Existing battery energy identity; capacity must originate from the saved vehicle profile.
        val energy = usableKwhAtSoh.takeIf { it.isFinite() && it > 0.0 }
            ?.let { usable -> pct?.let { usable * it / 100.0 } }
        val source = if (state.demo) SourceKind.DEMO else source(soc.source)
        return VehiclePanelUiState(
            speedKmh = speed.kmh?.takeIf { it.isFinite() && it >= 0.0 },
            powerKw = state.powerKw?.takeIf { it.isFinite() && !state.demo },
            socPercent = pct,
            estimatedKwh = energy,
            // Do not invent a range without a validated ConsumptionModel output.
            estimatedRangeKm = null,
            source = source,
            status = if (pct == null) PanelStatus.EMPTY else PanelStatus.NORMAL,
            message = when {
                state.demo -> "DEMO • Gerçek araç verisi değil"
                source == SourceKind.OBD -> "OBD • PID ölçekleri DOĞRULANMADI"
                source == SourceKind.MANUEL -> "MANUEL • SoC araç ekranından"
                else -> "OBD bağlı değil • Ayarlar'dan manuel SoC gir"
            },
        )
    }
}

data class MapHeadline(
    val title: String,
    val distance: String,
    val eta: String,
    val arrivalSoc: String,
)

/** No live traffic, occupancy, or travel-time claim; ETA is model/provider-only. */
object MapHeadlinePolicy {
    fun create(
        route: NavigationRoute?,
        hasDestination: Boolean,
        position: RoutePosition?,
        plan: PlanResult.Success?,
        epochMs: Long,
        zone: TimeZone = TimeZone.getDefault(),
    ): MapHeadline {
        if (route == null) return MapHeadline(
            if (hasDestination) "Hedef seçildi • Rota hesapla" else "Rota oluştur",
            "-- km", "ETA yok", "SoC yok",
        )
        val remaining = position?.remainingMeters?.takeIf { it.isFinite() && it >= 0.0 }
        val displayedMetres = remaining ?: route.distanceMeters
        val distance = if (!displayedMetres.isFinite() || displayedMetres < 0.0) "-- km"
            else String.format(Locale.ROOT, "~%.1f km • TAHMİN", displayedMetres / 1000.0)
        val duration = route.durationSeconds.takeIf { it.isFinite() && it > 0.0 }
            ?.let { total -> if (remaining == null || route.distanceMeters <= 0.0) total
                else total * (remaining / route.distanceMeters).coerceIn(0.0, 1.0) }
        val eta = if (duration == null || epochMs < 0L) "ETA yok"
            else {
                val format = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR"))
                format.timeZone = zone
                "Varış ~" + format.format(Date(epochMs + (duration * 1000).toLong())) +
                    " • TAHMİN"
            }
        return MapHeadline(
            if (position == null) "Toplam rota • konum DOĞRULANMADI" else
                "HEDEFE KALAN MESAFE",
            distance,
            eta,
            plan?.let { "Varış ~%" + it.arrivalSocPct + " • TAHMİN" } ?: "Varış SoC yok",
        )
    }

    fun nextStop(
        plan: PlanResult.Success?,
        route: NavigationRoute?,
        position: RoutePosition?,
    ): PlannedStop? {
        if (plan == null) return null
        val covered = if (route != null && position != null &&
            route.distanceMeters > 0.0)
            (route.distanceMeters - position.remainingMeters).coerceAtLeast(0.0)
            else null
        return if (covered == null) plan.stops.firstOrNull()
            else plan.stops.firstOrNull { it.charger.distanceAlongMeters + 250.0 >= covered }
    }
}
