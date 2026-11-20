package app.evcockpit.planner.model

data class SourcedValue(val value: Double, val source: String,
    val verified: Boolean = false, val note: String = "TAHMİN / DOĞRULANMADI") {
    init { require(value.isFinite() && source.isNotBlank()) }
}
data class CurvePoint(val x: Double, val y: Double)
data class SourcedCurve(val points: List<CurvePoint>, val source: String,
    val verified: Boolean = false, val note: String = "TAHMİN EĞRİ") {
    init {
        require(points.size >= 2 && source.isNotBlank())
        require(points.all { it.x.isFinite() && it.y.isFinite() })
        require(points.zipWithNext().all { it.first.x < it.second.x })
    }
    fun at(x: Double): Double {
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y
        val a = points.last { it.x <= x }
        val b = points.first { it.x > x }
        return a.y + (b.y - a.y) * (x - a.x) / (b.x - a.x)
    }
}
data class VehicleProfile(
    val usableKwh: SourcedValue, val sohPercent: SourcedValue,
    val massKg: SourcedValue, val extraLoadKg: SourcedValue,
    val cdA: SourcedValue, val crr: SourcedValue,
    val drivetrainEfficiency: SourcedValue, val regenEfficiency: SourcedValue,
    val maxRegenKw: SourcedValue, val maxDcKw: SourcedValue, val maxAcKw: SourcedValue,
    val auxBaseKw: SourcedValue, val hvacAutoKw: SourcedValue,
    val hvacHeatingKw: SourcedValue, val consumptionAdjust: SourcedValue,
    val chargingCurve: SourcedCurve, val coldDerateTable: SourcedCurve,
) {
    val capacityAtSohKwh get() = usableKwh.value * sohPercent.value / 100.0
    fun validate(): List<String> = buildList {
        if (usableKwh.value <= 0 || sohPercent.value !in 1.0..100.0) add("Batarya kapasitesi/SoH geçersiz")
        if (massKg.value <= 0 || extraLoadKg.value < 0 || cdA.value <= 0 || crr.value < 0) add("Fizik parametresi geçersiz")
        if (drivetrainEfficiency.value !in 0.01..1.0 || regenEfficiency.value !in 0.0..1.0) add("Verim geçersiz")
        if (maxRegenKw.value < 0 || maxDcKw.value <= 0 || maxAcKw.value <= 0) add("Güç geçersiz")
        if (auxBaseKw.value < 0 || hvacAutoKw.value < 0 || hvacHeatingKw.value < 0) add("Yük geçersiz")
        if (consumptionAdjust.value !in -0.3..0.3) add("Ayarlama ±%30 ile sınırlı")
        if (chargingCurve.points.first().x != 0.0 || chargingCurve.points.last().x != 100.0 ||
            chargingCurve.points.any { it.y <= 0 || it.x !in 0.0..100.0 }) add("Şarj eğrisi geçersiz")
        if (coldDerateTable.points.any { it.y !in 0.01..1.0 }) add("Soğuk sınırlaması geçersiz")
    }
    companion object {
        const val VW = "https://binekarac.vw.com.tr/tr/modeller-fiyatlar/arac-modelleri/id4.html"
        const val VW_TR = "https://autocatalogarchive.com/wp-content/uploads/2024/12/VW-ID.4-2024-TR.pdf"
        const val MODEL = "M8 master örnek varsayımları; sahada DOĞRULANMADI"
        fun defaults(): VehicleProfile {
            fun v(x: Double, source: String = MODEL) = SourcedValue(x, source)
            return VehicleProfile(
                v(52.0,VW),v(100.0),v(2000.0),v(0.0),v(0.28*2.7),v(0.009),
                v(0.90),v(0.65),v(65.0),v(145.0,VW_TR),v(11.0,VW_TR),
                v(0.5),v(0.5),v(2.0),v(0.0),
                SourcedCurve(listOf(CurvePoint(0.0,95.0),CurvePoint(10.0,100.0),
                    CurvePoint(50.0,90.0),CurvePoint(70.0,60.0),
                    CurvePoint(80.0,40.0),CurvePoint(90.0,22.0),
                    CurvePoint(100.0,5.0)),MODEL),
                SourcedCurve(listOf(CurvePoint(-20.0,0.30),CurvePoint(-5.0,0.50),
                    CurvePoint(10.0,0.80),CurvePoint(25.0,1.0),
                    CurvePoint(40.0,1.0)),MODEL))
        }
    }
}
