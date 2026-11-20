package app.evcockpit.passenger

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Opens the official Mola installation without repackaging its APK or bytecode.
 *
 * An Android launch intent cannot embed Mola in our Compose UI or provide
 * an Android Auto video surface. No elevated or accessibility permissions.
 */
object MolaCompanionLauncher {
    const val PACKAGE_NAME = "me.app.fermataX.auto.mola"

    enum class Result { OPENED, NOT_INSTALLED, UNAVAILABLE }

    fun open(context: Context): Result {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?: return Result.NOT_INSTALLED
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result.OPENED
        } catch (_: ActivityNotFoundException) {
            Result.UNAVAILABLE
        } catch (_: SecurityException) {
            Result.UNAVAILABLE
        }
    }
}
