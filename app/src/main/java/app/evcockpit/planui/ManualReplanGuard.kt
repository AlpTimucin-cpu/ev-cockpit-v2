package app.evcockpit.planui

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.obd.SocReading

/** Pure policy for user-triggered (never automatic OBD) route replanning. */
object ManualReplanGuard {
    fun reason(origin:GeoPoint?,destination:GeoPoint?,gpsFresh:Boolean,
        gpsAccuracyMeters:Float?,soc:SocReading,options:PlannerOptions):String? {
        if(origin==null || !gpsFresh || gpsAccuracyMeters==null ||
            !gpsAccuracyMeters.isFinite() || gpsAccuracyMeters>100f || gpsAccuracyMeters<0f)
            return "Yeniden planlama için taze ve hassas GPS gerekli. Ayarlar'dan GPS'i aç."
        if(destination==null || origin==destination)
            return "Hedef bulunamadı veya mevcut konumla aynı. Rota ekranında hedef seç."
        return PlannerGuard.reason(true,soc.pct,soc.source,options)
    }
}
