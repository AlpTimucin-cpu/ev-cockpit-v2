package app.evcockpit.integration

/**
 * Public launcher entry for the existing route tab.
 *
 * This contract belongs solely to EV Cockpit. It does not depend on Mola's
 * classes, private intents, app resources or Android Auto media APIs.
 */
object RouteLauncherContract {
    const val ROUTE_TAB = 2
    const val DISPLAY_NAME = "Rota • EV Cockpit"

    fun routeLink(): String =
        checkNotNull(MolaOpenContract.linkFor(ROUTE_TAB)) {
            "EV Cockpit route deep link is missing"
        }
}
