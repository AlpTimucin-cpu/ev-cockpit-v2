package app.evcockpit.integration

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Source-level regression guard; device Intent delivery remains a manual acceptance test. */
class MolaProductionWiringTest {
    @Test fun activityAndNavHostConsumeExternalLaunchContract() {
        val cwd = File(System.getProperty("user.dir"))
        val root = listOf(File(cwd, "src/main"), File(cwd, "app/src/main")).first { it.isDirectory }
        val manifest = File(root, "AndroidManifest.xml").readText()
        val activity = File(root, "java/app/evcockpit/MainActivity.kt").readText()
        val shell = File(root, "java/app/evcockpit/ui/finalhmi/MapFirstDashboardScreen.kt").readText()
        assertTrue(manifest.contains("android:scheme=\"evcockpit\""))
        assertTrue(manifest.contains("android:launchMode=\"singleTop\""))
        assertTrue(activity.contains("override fun onNewIntent"))
        assertTrue(activity.contains("MolaOpenContract.tabFor"))
        assertTrue(activity.contains("externalOpenRequest = openRequest"))
        assertTrue(activity.contains("if (target == 4) return"))
        assertTrue(shell.contains("LaunchedEffect(externalOpenRequest"))
        assertTrue(shell.contains("onExternalOpenConsumed(request)"))
        assertTrue(shell.contains("R.string.mola_open_original"))
        assertTrue(activity.contains("getLaunchIntentForPackage"))
    }
}
