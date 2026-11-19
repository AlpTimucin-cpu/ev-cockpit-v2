package app.evcockpit.location

data class SpeedReading(val kmh: Double?, val source: String)

object SpeedSelector {
    /** OBD PID is still verified=false: a label is mandatory even when a value exists. */
    fun select(
        obdConnected: Boolean,
        demo: Boolean,
        obdKmh: Double?,
        gpsKmh: Double?,
        gpsFresh: Boolean,
    ): SpeedReading {
        val validObd = obdKmh?.takeIf { it.isFinite() && it in 0.0..300.0 }
        val validGps = gpsKmh?.takeIf { it.isFinite() && it in 0.0..300.0 }
        return when {
            demo -> SpeedReading(validObd, "DEMO — gerçek değil")
            obdConnected && validObd != null -> SpeedReading(validObd, "OBD — PID doğrulanmadı")
            gpsFresh && validGps != null -> SpeedReading(validGps, "GPS — yedek")
            else -> SpeedReading(null, "Hız verisi yok")
        }
    }
}
