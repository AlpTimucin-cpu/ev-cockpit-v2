package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteManeuver
import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.domain.nav.WgsPoint
import org.junit.Assert.*
import org.junit.Test

/** C3: tam saf mod/geri/yükleme/durum testleri. */
class SheetReducerTest {
    private val origin = Place("nigde", "Niğde", 37.97, 34.68, "TEST")
    private val destination = Place("antalya", "Antalya", 36.89, 30.70, "TEST")
    private val route = RouteModel(
        id = "r1", geometry = listOf(
            WgsPoint(origin.latitude, origin.longitude),
            WgsPoint(destination.latitude, destination.longitude),
        ), distanceMeters = 460_000.0, durationSeconds = 17_000.0,
        source = "TEST",
    )

    @Test fun ilkDurumBosVeSoCHayalEdilmez() {
        val s = UnifiedUiState()
        assertEquals(SheetMode.Idle, s.mode)
        assertEquals(SheetDetent.PEEK, s.detent)
        assertNull(s.soc.value)
        assertNull(s.currentRoute)
        assertEquals(Loadable.Idle, s.routes)
    }

    @Test fun hedefSecilincePanelYariyaAcilirAmaRotaUydurulmaz() {
        val s = SheetReducer.reduce(UnifiedUiState(),
            UnifiedEvent.ChooseDestination(destination))
        assertEquals(destination, s.destination)
        assertEquals(SheetDetent.HALF, s.detent)
        assertEquals(Loadable.Idle, s.routes)
        assertNull(s.currentRoute)
    }

    @Test fun rotaOlmadanHesaplamadaUyariVerilir() {
        val s = SheetReducer.reduce(UnifiedUiState(), UnifiedEvent.LoadRoutes)
        assertEquals(SheetMode.Idle, s.mode)
        assertTrue(s.banners.last().message.textTr.contains("hedef"))
    }

    @Test fun rotaYuklemeVeAlternatifSecimi() {
        var s = UnifiedUiState(destination = destination)
        s = SheetReducer.reduce(s, UnifiedEvent.LoadRoutes)
        assertEquals(Loadable.Loading, s.routes)
        s = SheetReducer.reduce(s, UnifiedEvent.RoutesReady(
            listOf(route, route.copy(id = "r2"))))
        assertEquals(SheetMode.RoutePreview(0), s.mode)
        assertEquals("r1", s.currentRoute?.id)
        s = SheetReducer.reduce(s, UnifiedEvent.ChooseAlternative(1))
        assertEquals(SheetMode.RoutePreview(1), s.mode)
        assertEquals("r2", s.currentRoute?.id)
        val rejected = SheetReducer.reduce(s, UnifiedEvent.ChooseAlternative(3))
        assertEquals(SheetMode.RoutePreview(1), rejected.mode)
        assertTrue(rejected.banners.isNotEmpty())
    }

    @Test fun hazirRotaYokkenNavigasyonBaslamaz() {
        val s = SheetReducer.reduce(UnifiedUiState(),
            UnifiedEvent.StartNavigation)
        assertEquals(SheetMode.Idle, s.mode)
        assertTrue(s.banners.isNotEmpty())
    }

    @Test fun navigasyonVeBitir() {
        var s = UnifiedUiState(
            routes = Loadable.Ready(listOf(route)),
            mode = SheetMode.RoutePreview(0),
        )
        s = SheetReducer.reduce(s, UnifiedEvent.StartNavigation)
        assertEquals(SheetMode.Navigating, s.mode)
        assertEquals(SheetDetent.PEEK, s.detent)
        s = SheetReducer.reduce(s, UnifiedEvent.StopNavigation)
        assertEquals(SheetMode.RoutePreview(0), s.mode)
    }

    @Test fun panelGeriZinciriFullHalfPeekIdleCikis() {
        var s = UnifiedUiState(
            mode = SheetMode.Energy, detent = SheetDetent.FULL)
        s = SheetReducer.reduce(s, UnifiedEvent.Back)
        assertEquals(SheetDetent.HALF, s.detent)
        s = SheetReducer.reduce(s, UnifiedEvent.Back)
        assertEquals(SheetDetent.PEEK, s.detent)
        assertEquals(SheetMode.Energy, s.mode)
        s = SheetReducer.reduce(s, UnifiedEvent.Back)
        assertEquals(SheetMode.Idle, s.mode)
        s = SheetReducer.reduce(s, UnifiedEvent.Back)
        assertTrue(s.exitRequested)
        s = SheetReducer.reduce(s, UnifiedEvent.ExitConsumed)
        assertFalse(s.exitRequested)
    }

    @Test fun modDurakSecimiYalnizGercekListede() {
        val station = StationOnRoute("s1", "Gerçek OCM", 38.0, 34.8,
            100.0, true, 25000.0, 400.0)
        var s = UnifiedUiState(stations = Loadable.Ready(listOf(station)))
        s = SheetReducer.reduce(s, UnifiedEvent.ShowStation("bad"))
        assertEquals(SheetMode.Idle, s.mode)
        s = SheetReducer.reduce(s, UnifiedEvent.ShowStation("s1"))
        assertEquals(SheetMode.StopDetail("s1"), s.mode)
    }

    @Test fun kaynakliSOCsinirlariVeSifirGecerliligi() {
        var s = SheetReducer.reduce(UnifiedUiState(),
            UnifiedEvent.UpdateSoc(SourcedValue(0.0, SourcedOrigin.MANUAL)))
        assertEquals(0.0, s.soc.value!!, 0.0)
        s = SheetReducer.reduce(s,
            UnifiedEvent.UpdateSoc(SourcedValue(101.0, SourcedOrigin.MANUAL)))
        assertEquals(0.0, s.soc.value!!, 0.0)
        assertTrue(s.banners.last().message.textTr.contains("Geçersiz"))
    }

    @Test fun bulunamayanRotaYanitindaAlternatifUydurulmaz() {
        val s = SheetReducer.reduce(
            UnifiedUiState(destination = destination, routes = Loadable.Loading),
            UnifiedEvent.RoutesReady(emptyList()),
        )
        assertTrue(s.routes is Loadable.Failed)
        assertNull(s.currentRoute)
    }
}
