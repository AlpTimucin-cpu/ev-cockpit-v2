package app.evcockpit

import android.app.Application
import app.evcockpit.obd.ObdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Process-scoped state holder so Activity recreation does not cancel the OBD session. */
class CockpitApplication : Application() {
    val obdRepository: ObdRepository by lazy {
        ObdRepository(this, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }
}
