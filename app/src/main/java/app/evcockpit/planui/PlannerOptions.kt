package app.evcockpit.planui

import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.model.SourcedValue
import app.evcockpit.planner.model.VehicleProfile

data class PlannerOptions(
    val minArrivalSoc: Double=10.0, val minStopSoc: Double=10.0,
    val maxChargeSoc: Double=80.0, val maxCruiseKmh: Double=120.0,
    val temperatureC: Double=20.0, val hvac: HvacMode=HvacMode.AUTO,
    val extraLoadKg: Double=0.0, val consumptionAdjust: Double=0.0,
    val minStationKw: Double=30.0, val searchRadiusMeters: Int=3000,
    val maxStops: Int=8, val onlyTaggedCcs2: Boolean=false,
    val allowSlowCharging: Boolean=false, val excludedNetworks: String="",
    val destinationChargeKw: Double?=null,
) {
    fun errors():List<String> = buildList {
        if(!minArrivalSoc.isFinite() || minArrivalSoc !in 0.0..100.0 ||
           !minStopSoc.isFinite() || minStopSoc !in 0.0..100.0) add("Alt SoC 0–100 olmalı")
        if(!maxChargeSoc.isFinite() || maxChargeSoc !in 1.0..100.0) add("Şarj hedefi 1–100 olmalı")
        if(!maxCruiseKmh.isFinite() || maxCruiseKmh !in 20.0..150.0) add("Hız 20–150 km/sa olmalı")
        if(!temperatureC.isFinite() || temperatureC !in -40.0..60.0) add("Sıcaklık −40..60 °C olmalı")
        if(!extraLoadKg.isFinite() || extraLoadKg !in 0.0..1500.0) add("Ek yük 0–1500 kg olmalı")
        if(!consumptionAdjust.isFinite() || consumptionAdjust !in -0.30..0.30) add("Tüketim düzeltme ±%30 olmalı")
        if(!minStationKw.isFinite() || minStationKw !in 0.0..350.0) add("Güç 0–350 kW olmalı")
        if(searchRadiusMeters !in 1000..10000) add("Yarıçap 1–10 km olmalı")
        if(maxStops !in 0..8) add("En çok 8 durak olmalı")
        if(excludedNetworks.length>250) add("Ağ listesi çok uzun")
        if(destinationChargeKw!=null && (!destinationChargeKw.isFinite() ||
            destinationChargeKw !in 1.0..350.0)) add("Hedefte şarj 1–350 kW olmalı")
    }
    fun applyTo(profile:VehicleProfile)=profile.copy(
        extraLoadKg=SourcedValue(extraLoadKg,"KULLANICI PLAN AYARI",false,"DOĞRULANMADI"),
        consumptionAdjust=SourcedValue(consumptionAdjust,"KULLANICI PLAN AYARI",false,"DOĞRULANMADI"))
}
enum class PlannerStage { IDLE, ELEVATION, STATIONS, OPTIMIZING, READY, FAILURE, CANCELLED, VERIFYING }
object PlannerGuard {
    fun reason(hasRoute:Boolean,soc:Double?,source:String,options:PlannerOptions):String? {
        if(!hasRoute)return "Önce Rota sekmesinde GPS, hedef ve HeiGIT anahtarıyla rota oluştur."
        if(source.startsWith("DEMO",true))return "DEMO SoC ile plan yapılmaz. Ayarlar'dan manuel SoC gir."
        if(soc==null || !soc.isFinite() || soc !in 0.0..100.0)
            return "SoC verisi yok. Ayarlar > Manuel SoC bölümünden değer gir."
        return options.errors().firstOrNull()
    }
    fun label(s:PlannerStage):String=when(s) {
        PlannerStage.IDLE->"Hazır — planı sürüşten önce başlat"
        PlannerStage.ELEVATION->"Rota ve eğim hazırlanıyor (Open-Meteo)..."
        PlannerStage.STATIONS->"OSM/Overpass istasyonları sorgulanıyor..."
        PlannerStage.OPTIMIZING->"Şarj durakları optimize ediliyor..."
        PlannerStage.READY->"Plan hazır — TAHMİN, sahada DOĞRULANMADI"
        PlannerStage.FAILURE->"Plan oluşturulamadı — açıklamayı kontrol et"
        PlannerStage.CANCELLED->"Plan iptal edildi"
        PlannerStage.VERIFYING->"Ara durak rotası doğrulanıyor..."
    }
}
