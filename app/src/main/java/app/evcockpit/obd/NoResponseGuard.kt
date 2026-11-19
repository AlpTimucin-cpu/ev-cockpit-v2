package app.evcockpit.obd

/** Stops continuous polling after exactly five fully unanswered cycles. */
class NoResponseGuard(private val limit: Int = 5) {
    var consecutiveUnansweredCycles: Int = 0
        private set

    fun recordFullCycle(anyPositiveResponse: Boolean): Boolean {
        consecutiveUnansweredCycles =
            if (anyPositiveResponse) 0 else (consecutiveUnansweredCycles + 1).coerceAtMost(limit)
        return consecutiveUnansweredCycles >= limit
    }
}

/** Communication path only, never changes the PID verified flag or numeric equations. */
class ObdProfileStore(private val context: android.content.Context) {
    private val prefs get() = context.getSharedPreferences("obd_comm_profile", 0)
    fun read(): DiagnosticProfile = runCatching {
        DiagnosticProfile.valueOf(prefs.getString("selected", "NONE") ?: "NONE")
    }.getOrDefault(DiagnosticProfile.NONE)

    fun saveFromDiagnostic(outcome: DiagnosticOutcome): DiagnosticProfile {
        if (outcome.steps.any { it.timedOut }) return DiagnosticProfile.NONE
        val profile = outcome.proposedProfile
        if (profile != DiagnosticProfile.NONE) {
            prefs.edit().putString("selected", profile.name).apply()
        }
        return profile
    }
}
