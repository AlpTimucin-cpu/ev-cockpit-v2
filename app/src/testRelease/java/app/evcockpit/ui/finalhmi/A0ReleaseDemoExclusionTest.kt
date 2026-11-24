package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class A0ReleaseDemoExclusionTest {
    @Test fun releaseDemoModunuAslaSunmaz() {
        assertFalse(BuildTypeDemoProvider.available)
        assertNull(BuildTypeDemoProvider.state(1_795_460_000_000L))
    }
}
