package app.evcockpit.integration

import org.junit.Assert.*
import org.junit.Test

class MolaLinkCoordinatorTest {
    /** Faked PackageManager-facing gateway: no Android runtime or actual Mola APK required. */
    private class FakeGateway(
        val installed: Set<String> = emptySet(),
        val launchable: Set<String> = installed,
        var launchAllowed: Boolean = true,
        var officialGuideAllowed: Boolean = true,
    ): MolaPackageGateway {
        val launches = mutableListOf<String>()
        val settings = mutableListOf<String>()
        override fun isInstalled(packageName: String): Boolean = packageName in installed
        override fun isLaunchable(packageName: String): Boolean = packageName in launchable
        override fun launch(packageName: String): Boolean {
            launches += packageName
            return launchAllowed
        }
        override fun showApplicationSettings(packageName: String): Boolean {
            settings += packageName
            return true
        }
        override fun showOfficialInstallGuide(): Boolean = officialGuideAllowed
    }

    @Test fun installedOfficialPackageUsesOnlyPublicLauncher() {
        val fake = FakeGateway(setOf(MolaPackages.ORIGINAL))
        val status = MolaLinkCoordinator.inspect(fake)
        assertEquals(MolaStatus.READY, status.status)
        assertEquals(MolaPackages.ORIGINAL, status.packageName)
        val result = MolaLinkCoordinator.launch(fake,status)
        assertEquals(listOf(MolaPackages.ORIGINAL), fake.launches)
        assertEquals(MolaStatus.READY, result.status)
        assertTrue(result.message.contains("DOĞRULANMADI"))
    }

    @Test fun installedLegacyDebugPackageIsExplicitlyDistinguished() {
        val fake = FakeGateway(setOf(MolaPackages.DEBUG))
        assertEquals(MolaPackages.DEBUG, MolaLinkCoordinator.inspect(fake).packageName)
    }

    @Test fun missingPackageYieldsInstallGuideAndCannotLaunch() {
        val fake = FakeGateway()
        val status = MolaLinkCoordinator.inspect(fake)
        assertEquals(MolaStatus.NOT_INSTALLED,status.status)
        assertNull(status.packageName)
        assertTrue(status.message.contains("Mola kurulu değil"))
        MolaLinkCoordinator.launch(fake,status)
        assertTrue(fake.launches.isEmpty())
    }

    @Test fun existingPackageWithoutPublicLauncherRequiresSettings() {
        val fake = FakeGateway(installed=setOf(MolaPackages.ORIGINAL),
            launchable=emptySet())
        val status = MolaLinkCoordinator.inspect(fake)
        assertEquals(MolaStatus.CANNOT_LAUNCH,status.status)
        assertEquals(MolaPackages.ORIGINAL,status.packageName)
        assertFalse(MolaLinkCoordinator.launch(fake,status).status==MolaStatus.READY)
        assertTrue(fake.launches.isEmpty())
    }

    @Test fun launchFailureKeepsUserInEVCockpitWithError() {
        val fake = FakeGateway(setOf(MolaPackages.ORIGINAL),launchAllowed=false)
        val status = MolaLinkCoordinator.inspect(fake)
        val result = MolaLinkCoordinator.launch(fake,status)
        assertEquals(MolaStatus.ERROR,result.status)
        assertTrue(result.message.contains("Mola açılamadı"))
        assertEquals(1,fake.launches.size)
    }

    @Test fun launchRejectsUntrustedPackageAndDoesNotTouchExternalApp() {
        val fake = FakeGateway()
        val status = MolaLinkState(MolaStatus.READY,"com.example.private",
            "test")
        assertEquals(MolaStatus.ERROR,MolaLinkCoordinator.launch(fake,status).status)
        assertTrue(fake.launches.isEmpty())
        assertTrue(MolaPackages.RELEASES.startsWith("https://github.com/"))
    }
}
