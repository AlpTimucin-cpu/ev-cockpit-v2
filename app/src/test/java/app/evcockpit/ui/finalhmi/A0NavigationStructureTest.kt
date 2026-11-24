package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class A0NavigationStructureTest {
    @Test fun anaSekmeHaritaVeSadeceBesSekmeVardir() {
        assertEquals(FinalDashboardTab.HARITA, FinalDashboardTab.entries.first())
        assertEquals(5, FinalDashboardTab.entries.size)
        assertEquals(listOf("Harita","Rota","Şarj","Enerji","Ayarlar"),
            FinalDashboardTab.entries.map { it.titleTr })
        assertEquals(FinalDashboardTab.entries.size,
            FinalDashboardTab.entries.map { it.name }.distinct().size)
    }
}
