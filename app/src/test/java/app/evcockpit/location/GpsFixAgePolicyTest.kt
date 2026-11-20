package app.evcockpit.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GpsFixAgePolicyTest {
    private fun nanos(ms: Long): Long = ms * 1_000_000L

    @Test fun freshFixRetainsOriginalAcquisitionTime() {
        assertEquals(90_000L, GpsFixAgePolicy.acceptedTimestampMs(100_000L, nanos(90_000L)))
        assertEquals(85_000L, GpsFixAgePolicy.acceptedTimestampMs(100_000L, nanos(85_000L)))
    }

    @Test fun delayedCallbacksCannotBecomeFreshLocations() {
        assertNull(GpsFixAgePolicy.acceptedTimestampMs(100_000L, nanos(84_999L)))
        assertNull(GpsFixAgePolicy.acceptedTimestampMs(100_000L, nanos(15_000L)))
        assertNull(GpsFixAgePolicy.acceptedTimestampMs(100_000L, 0L))
        assertNull(GpsFixAgePolicy.acceptedTimestampMs(0L, nanos(20L)))
    }

    @Test fun futureFixesAreBoundedAndClamped() {
        assertEquals(100_000L, GpsFixAgePolicy.acceptedTimestampMs(100_000L, nanos(101_000L)))
        assertNull(GpsFixAgePolicy.acceptedTimestampMs(100_000L, nanos(102_001L)))
    }

    @Test fun expiryTimerUsesSameFifteenSecondRule() {
        assertEquals(15_001L, GpsFixAgePolicy.timeUntilStaleMs(10_000L, 10_000L))
        assertEquals(1L, GpsFixAgePolicy.timeUntilStaleMs(25_000L, 10_000L))
        assertEquals(0L, GpsFixAgePolicy.timeUntilStaleMs(25_001L, 10_000L))
        assertEquals(0L, GpsFixAgePolicy.timeUntilStaleMs(10_000L, 0L))
    }
}
