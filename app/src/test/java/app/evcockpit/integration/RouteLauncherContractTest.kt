package app.evcockpit.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteLauncherContractTest {
    @Test fun routeLauncherTargetsExistingRouteTabOnly() {
        val uri = RouteLauncherContract.routeLink()
        assertEquals("evcockpit://open/route", uri)
        assertEquals(2, MolaOpenContract.tabFor(uri))
        assertEquals("Rota • EV Cockpit", RouteLauncherContract.DISPLAY_NAME)
    }

    @Test fun unauthorizedCommandsStillRejected() {
        assertNull(MolaOpenContract.tabFor("evcockpit://open/route?autoplay=true"))
        assertNull(MolaOpenContract.tabFor("evcockpit://open/route#gps"))
        assertNull(MolaOpenContract.tabFor("evcockpit://open/route/ecu"))
    }
}
