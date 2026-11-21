package app.evcockpit.planner.model

/**
 * Independent, advisory audit of EV charging-curve provenance and consistency.
 *
 * Motivated by a quality-control pattern observed while inspecting a third-party
 * EV application. No third-party records, source code, curve points or proprietary
 * datasets are incorporated here.
 *
 * Peak power can intentionally exceed a nominal nameplate in some EV profiles:
 * the mismatch is a warning, NEVER an automatic correction or a proof of error.
 * ChargingTimeCalculator already applies the vehicle and station power caps.
 */
object ChargingCurveConsistency {
    fun warnings(profile: VehicleProfile): List<String> = buildList {
        val peakKw = profile.chargingCurve.points.maxOf { it.y }
        val nominalKw = profile.maxDcKw.value
        if (peakKw > nominalKw + 0.01) {
            add(
                "Şarj eğrisi tepe değeri (${peakKw.toInt()} kW), profildeki DC azami " +
                    "gücü (${nominalKw.toInt()} kW) aşıyor; geçici tepe/nominal ayrımı " +
                    "DOĞRULANMADI. Plan hesaplamasında güç sınırı uygulanır."
            )
        }
        if (!profile.chargingCurve.verified) {
            add("Şarj eğrisi TAHMİN — üretici/araç ölçümüyle doğrulanmadı")
        }
        if (profile.observedDcKw != null) {
            add(
                "Kullanıcının DC güç gözlemiyle ölçekli eğri: tek ölçüm, " +
                    "araç tipi ve tüm SOC aralığı için doğrulama değildir"
            )
        }
    }
}
