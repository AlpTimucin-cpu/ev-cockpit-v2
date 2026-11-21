package app.evcockpit.ui.tesla

import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.model.PlanResult
import java.util.Locale

enum class GuidanceKind { NONE, NO_OBD, GPS_MISSING, ROUTE_KEY, STALE,
    NO_CHARGER, NO_ROUTE, ROUTE_LOADING }

enum class GuidanceAction { NONE, SETTINGS, LOCATION_PERMISSION, OBD_TEST, ROUTE, PLAN }

data class DashboardGuidance(
    val kind: GuidanceKind,
    val title: String,
    val detail: String,
    val action: GuidanceAction = GuidanceAction.NONE,
    val actionText: String = "",
) {
    companion object {
        val NONE = DashboardGuidance(GuidanceKind.NONE, "", "")
    }
}

/** Display-only policy. No GPS coordinates, credentials, network or BMS calls. */
object DashboardGuidancePolicy {
    fun evaluate(
        vehicle: VehicleState,
        soc: SocReading,
        gps: GpsSnapshot,
        gpsFresh: Boolean,
        route: RouteUiState,
        plan: PlanResult?,
    ): DashboardGuidance {
        if (plan is PlanResult.Failure) {
            val distance = plan.nearestReachableKm.takeIf {
                it.isFinite() && it > 0.0
            }?.let { String.format(Locale.ROOT, "~%.1f km", it) } ?: "bilinmiyor"
            return DashboardGuidance(
                GuidanceKind.NO_CHARGER,
                "Bu rota için durak bulunamadı",
                plan.reason.take(180) + " • En yakın tahmini ulaşılabilir nokta: " +
                    distance + " • TAHMİN / DOĞRULANMADI",
                GuidanceAction.PLAN, "Plan filtrelerine git",
            )
        }
        if (route.busy) return DashboardGuidance(
            GuidanceKind.ROUTE_LOADING, "Rota hesaplanıyor",
            "Rota sağlayıcısının sonucu bekleniyor; tahmin henüz hazır değil.",
        )
        if (route.message.contains("anahtar",ignoreCase = true) &&
            (route.message.contains("yok",ignoreCase = true) ||
                route.message.contains("hata",ignoreCase = true))) {
            return DashboardGuidance(GuidanceKind.ROUTE_KEY,
                "Rota anahtarını Ayarlar'a gir",
                "HeiGIT / openrouteservice anahtarı gerekli. Anahtar cihazda şifreli saklanır.",
                GuidanceAction.SETTINGS,"Anahtar ayarlarına git")
        }
        if (!gpsFresh || gps.point == null) return DashboardGuidance(
            GuidanceKind.GPS_MISSING,"Konum alınamıyor",
            "Konum iznini ve telefonun GPS ayarını kontrol et.",
            GuidanceAction.LOCATION_PERMISSION,"Konum iznini aç")
        if (soc.source.startsWith("ESKİ",ignoreCase = true)) return DashboardGuidance(
            GuidanceKind.STALE,"ESKİ • OBD verisi yenilenmeli",
            "Son SoC örneği üç saniyeden eski. Manuel SoC kullanılabilir.",
            GuidanceAction.OBD_TEST,"OBD Test'e git")
        if (soc.pct == null || !vehicle.connected && !vehicle.demo &&
            !soc.source.startsWith("MANUEL")) return DashboardGuidance(
            GuidanceKind.NO_OBD,"OBD bağlı değil",
            "Araç verisi doğrulanmadı. Ayarlar'dan manuel SoC girebilirsin.",
            GuidanceAction.SETTINGS,"Manuel SoC gir")
        if (route.route == null) return DashboardGuidance(
            GuidanceKind.NO_ROUTE,"Rota henüz oluşturulmadı",
            "Hedefini seçip Rota ekranında hesapla.",
            GuidanceAction.ROUTE,"Rota ekranına git")
        return DashboardGuidance.NONE
    }
}
