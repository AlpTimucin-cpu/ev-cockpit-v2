package app.evcockpit.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MolaOpenContractTest {
    @Test fun allSixExistingTabsCanBeOpened() {
        val names = listOf("dashboard", "obd", "route", "plan", "passenger", "settings")
        names.forEachIndexed { index, name ->
            val url = "evcockpit://open/$name"
            assertEquals(index, MolaOpenContract.tabFor(url))
            assertEquals(url, MolaOpenContract.linkFor(index))
        }
        assertNull(MolaOpenContract.linkFor(6))
    }

    @Test fun externalParametersCannotTriggerPlaybackOrSensitiveCommands() {
        listOf(
            "https://open/plan",
            "evcockpit://open/reset",
            "evcockpit://open/plan?token=secret",
            "evcockpit://open/plan#frag",
            "evcockpit://user@open/plan",
            "evcockpit://open:8443/plan",
            "evcockpit://open/%70lan",
            "evcockpit://open/plan/extra",
            "evcockpit://evil/route",
            "evcockpit://open/obd?connect=true",
            "evcockpit://open/passenger?autoplay=1",
        ).forEach {
            assertNull("Must reject: $it", MolaOpenContract.tabFor(it))
        }
        assertNull(MolaOpenContract.tabFor(null))
    }

    @Test fun protocolIsCaseInsensitiveButPathsAreExact() {
        assertEquals(3, MolaOpenContract.tabFor("EVCockpit://OPEN/plan"))
        assertNull(MolaOpenContract.tabFor("evcockpit://open/Plan"))
    }
}
