package app.evcockpit.integration

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import app.evcockpit.MainActivity

/**
 * Separate Android launcher activity for EV Cockpit's existing route page.
 *
 * An external app picker MAY list this launcher entry. Whether Mola's picker
 * lists it is a device/Mola-version-specific acceptance test, not a guarantee.
 *
 * No privileged access, location sharing, vehicle action or playback is
 * triggered. MainActivity's existing drive-mode and navigation guards apply.
 */
class RouteLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(RouteLauncherContract.routeLink())).apply {
                    setClass(this@RouteLauncherActivity, MainActivity::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
        finish()
    }
}
