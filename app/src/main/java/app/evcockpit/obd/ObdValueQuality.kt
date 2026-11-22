package app.evcockpit.obd

import kotlin.math.abs

/**
 * Sanity check for UNVERIFIED MEB PID scaling and low-cost ELM327 responses.
 *
 * A plausible number is NOT evidence that a PID, ECU address or scale is correct.
 * This gate only prevents obviously impossible values from entering live state.
 * It never marks a PID verified, never generates a replacement value, and never
 * sends commands to the vehicle.
 */
enum class ObdReadingVerdict {
    ACCEPT, INVALID_NUMBER, OUT_OF_BOUNDS, SOC_JUMP, UNMAPPED_ROLE
}

object ObdValueQuality {
    // Deliberately broad boundaries: reject decoding noise, NOT normal driving.
    // These are engineering sanity limits, not an ID.4 manufacturer specification.
    private const val MAX_PLAUSIBLE_VOLTAGE_V = 1200.0
    private const val MAX_PLAUSIBLE_CURRENT_A = 5000.0
    private const val MAX_PLAUSIBLE_SPEED_KMH = 300.0

    // More than 20 percentage points of BMS SoC in 10 seconds is suspicious.
    // A reconnect/stale interval is exempted; a new valid reference can be read.
    private const val RECENT_SOC_WINDOW_MS = 10_000L
    private const val MAX_RECENT_SOC_CHANGE_PCT = 20.0

    fun assess(
        role: String,
        value: Double,
        previousSocPct: Double? = null,
        lastSocReadElapsedMs: Long = 0L,
        nowElapsedMs: Long = 0L,
    ): ObdReadingVerdict {
        if (!value.isFinite()) return ObdReadingVerdict.INVALID_NUMBER

        val withinBounds = when (role) {
            "soc" -> value in 0.0..100.0
            "speed" -> value in 0.0..MAX_PLAUSIBLE_SPEED_KMH
            "volt" -> value in 0.0..MAX_PLAUSIBLE_VOLTAGE_V
            "amp" -> value in -MAX_PLAUSIBLE_CURRENT_A..MAX_PLAUSIBLE_CURRENT_A
            else -> return ObdReadingVerdict.UNMAPPED_ROLE
        }
        if (!withinBounds) return ObdReadingVerdict.OUT_OF_BOUNDS

        val freshPreviousSoc = role == "soc" &&
            previousSocPct != null && previousSocPct.isFinite() &&
            lastSocReadElapsedMs > 0L &&
            nowElapsedMs >= lastSocReadElapsedMs &&
            nowElapsedMs - lastSocReadElapsedMs <= RECENT_SOC_WINDOW_MS
        if (freshPreviousSoc &&
            abs(value - requireNotNull(previousSocPct)) > MAX_RECENT_SOC_CHANGE_PCT) {
            return ObdReadingVerdict.SOC_JUMP
        }
        return ObdReadingVerdict.ACCEPT
    }
}
