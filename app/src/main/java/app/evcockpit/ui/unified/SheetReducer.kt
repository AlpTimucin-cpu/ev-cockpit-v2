package app.evcockpit.ui.unified

/**
 * Sadece durum indirger: GPS, OBD, ağ, TTS, DataStore çağrısı YAPMAZ.
 * Tüm kararlar deterministiktir, JVM testleri gerçek Android'e gerek duymaz.
 */
object SheetReducer {
    private const val MAX_BANNERS = 3

    fun reduce(before: UnifiedUiState, event: UnifiedEvent): UnifiedUiState {
        val s = before.copy(exitRequested = false)
        return when (event) {
            is UnifiedEvent.ChooseOrigin -> {
                if (event.place != null && !event.place.valid)
                    s.warn("Başlangıç koordinatı geçersiz.", "Yeniden seç")
                else s.copy(
                    origin = event.place,
                    routes = Loadable.Idle,
                    mode = SheetMode.Idle,
                )
            }
            is UnifiedEvent.ChooseDestination -> {
                if (event.place != null && !event.place.valid)
                    s.warn("Hedef koordinatı geçersiz.", "Yeniden seç")
                else s.copy(
                    destination = event.place,
                    routes = Loadable.Idle,
                    mode = SheetMode.Idle,
                    detent = if (event.place != null) SheetDetent.HALF else s.detent,
                )
            }
            UnifiedEvent.LoadRoutes ->
                if (s.destination == null)
                    s.warn("Önce bir hedef seç.", "Hedef ara")
                else s.copy(routes = Loadable.Loading, detent = SheetDetent.HALF)
            is UnifiedEvent.RoutesReady -> {
                if (event.values.isEmpty()) {
                    s.copy(routes = Loadable.Failed(
                        UserMessage("Rota bulunamadı.", "Yeniden dene")),
                        mode = SheetMode.Idle)
                } else s.copy(
                    routes = Loadable.Ready(event.values),
                    mode = SheetMode.RoutePreview(0),
                    detent = SheetDetent.HALF,
                )
            }
            is UnifiedEvent.RoutesFailed ->
                s.copy(routes = Loadable.Failed(event.reason), mode = SheetMode.Idle)
            is UnifiedEvent.ChooseAlternative -> {
                val routes = (s.routes as? Loadable.Ready)?.value
                if (routes != null && event.index in routes.indices)
                    s.copy(mode = SheetMode.RoutePreview(event.index))
                else s.warn("Seçilen alternatif rota bulunamadı.", "Rotaları yenile")
            }
            UnifiedEvent.StartNavigation -> {
                if (s.currentRoute == null)
                    s.warn("Başlatılabilecek geçerli rota yok.", "Rota hesapla")
                else s.copy(mode = SheetMode.Navigating, detent = SheetDetent.PEEK)
            }
            UnifiedEvent.StopNavigation ->
                s.copy(mode = if (s.currentRoute != null)
                    SheetMode.RoutePreview(0) else SheetMode.Idle,
                    detent = SheetDetent.HALF)
            is UnifiedEvent.ShowStation -> {
                val stations = (s.stations as? Loadable.Ready)?.value
                if (stations?.any { it.id == event.id } == true)
                    s.copy(mode = SheetMode.StopDetail(event.id), detent = SheetDetent.HALF)
                else s.warn("Seçilen şarj istasyonu bulunamadı.", "İstasyonları yenile")
            }
            UnifiedEvent.ShowEnergy ->
                s.copy(mode = SheetMode.Energy, detent = SheetDetent.HALF)
            is UnifiedEvent.SetDetent -> s.copy(detent = event.value)
            is UnifiedEvent.SetFollow -> s.copy(followMode = event.value)
            is UnifiedEvent.UpdateVehicle -> s.copy(vehicle = event.fix)
            is UnifiedEvent.UpdateSoc -> {
                val percent = event.sourced.value
                if (percent != null && (!percent.isFinite() || percent !in 0.0..100.0))
                    s.warn("Geçersiz batarya SoC değeri.", "SoC gir")
                else s.copy(soc = event.sourced)
            }
            is UnifiedEvent.UpdateStations -> s.copy(stations = event.result)
            is UnifiedEvent.UpdateWeather -> s.copy(weather = event.result)
            is UnifiedEvent.AddBanner -> s.copy(
                banners = (s.banners + event.value).takeLast(MAX_BANNERS))
            UnifiedEvent.ExitConsumed -> s.copy(exitRequested = false)
            UnifiedEvent.ResetToIdle -> s.copy(
                mode = SheetMode.Idle, detent = SheetDetent.PEEK,
                exitRequested = false)
            UnifiedEvent.Back -> when {
                s.detent == SheetDetent.FULL ->
                    s.copy(detent = SheetDetent.HALF)
                s.detent == SheetDetent.HALF ->
                    s.copy(detent = SheetDetent.PEEK)
                s.mode != SheetMode.Idle ->
                    s.copy(mode = SheetMode.Idle, detent = SheetDetent.PEEK)
                else -> s.copy(exitRequested = true)
            }
        }
    }

    private fun UnifiedUiState.warn(text: String, action: String): UnifiedUiState =
        copy(banners = (banners + Banner(
            UserMessage(text, action), BannerKind.WARNING)).takeLast(MAX_BANNERS))
}
