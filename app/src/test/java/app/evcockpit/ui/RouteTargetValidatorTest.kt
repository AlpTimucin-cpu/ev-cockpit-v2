package app.evcockpit.ui

import org.junit.Assert.*
import org.junit.Test

class RouteTargetValidatorTest {
    @Test fun acceptsTurkishDecimal() {
        val result = RouteTargetValidator.parse("38,72", "35,48")!!
        assertEquals(38.72, result.latitude, 0.0001)
        assertEquals(35.48, result.longitude, 0.0001)
    }
    @Test fun rejectsEmptyNanAndOutsideEarth() {
        assertNull(RouteTargetValidator.parse("", "35"))
        assertNull(RouteTargetValidator.parse("NaN", "35"))
        assertNull(RouteTargetValidator.parse("91", "35"))
        assertNull(RouteTargetValidator.parse("30", "181"))
    }
}
