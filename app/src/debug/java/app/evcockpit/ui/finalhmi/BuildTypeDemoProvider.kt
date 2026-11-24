package app.evcockpit.ui.finalhmi

/** Debug'da demo kullanıcı tarafından AÇIKÇA açılana kadar kapalıdır. */
internal object BuildTypeDemoProvider {
    const val available = true
    fun state(nowEpochMs: Long): DashboardUiState = DemoDashboardMapper.map(nowEpochMs)
}
