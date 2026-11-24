package app.evcockpit.ui.finalhmi

/** Release üretiminde DEMO mapper veya örnek veriye hiçbir referans yoktur. */
internal object BuildTypeDemoProvider {
    const val available = false
    fun state(@Suppress("UNUSED_PARAMETER") nowEpochMs: Long): DashboardUiState? = null
}
