package app.evcockpit.navigation

data class VoiceCue(val id: String, val text: String)

object VoiceAnnouncementPolicy {
    /**
     * Only announce a real upcoming maneuver on a matched route at <= 200m / <= 50m.
     * Duplicates are prevented by index+tier. No cue for unknown/negative distances.
     */
    fun select(
        maneuverIndex: Int,
        distanceMeters: Double?,
        instruction: String,
        delivered: Set<String>,
    ): VoiceCue? {
        if (maneuverIndex < 0 || instruction.isBlank()) return null
        val metres = distanceMeters?.takeIf { it.isFinite() && it >= 0.0 } ?: return null
        val tier = when {
            metres <= 50.0 -> 50
            metres <= 200.0 -> 200
            else -> return null
        }
        val id = "$maneuverIndex:$tier"
        if (id in delivered) return null
        return VoiceCue(id, "$tier metre sonra $instruction")
    }
}
