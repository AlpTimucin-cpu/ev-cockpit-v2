package app.evcockpit.obd

/** Deterministic capped exponential backoff; failures start counting at 1. */
internal object ReconnectBackoff {
    fun delayMs(failures: Int): Long {
        val exponent = (failures - 1).coerceIn(0, 5)
        return (1_000L shl exponent).coerceAtMost(30_000L)
    }
}
