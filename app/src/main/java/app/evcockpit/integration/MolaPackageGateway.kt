package app.evcockpit.integration

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Public launcher package IDs; never refer to Mola's private components or URIs. */
object MolaPackages {
    const val ORIGINAL = "me.app.fermataX.auto.mola"
    const val DEBUG = "me.app.fermataX.auto.mola.debug"
    val ALLOWED = listOf(ORIGINAL, DEBUG)
    const val RELEASES = "https://github.com/moris545454/mola-apk/releases"
}

enum class MolaStatus { CHECKING, NOT_INSTALLED, READY, CANNOT_LAUNCH, ERROR }

data class MolaLinkState(
    val status: MolaStatus = MolaStatus.CHECKING,
    val packageName: String? = null,
    val message: String = "Mola kurulum durumu kontrol ediliyor",
)

/** The gateway is faked in JVM tests; production talks only to the public Android API. */
interface MolaPackageGateway {
    fun isInstalled(packageName: String): Boolean
    fun isLaunchable(packageName: String): Boolean
    fun launch(packageName: String): Boolean
    fun showApplicationSettings(packageName: String): Boolean
    fun showOfficialInstallGuide(): Boolean
}

class AndroidMolaPackageGateway(context: Context): MolaPackageGateway {
    private val app = context.applicationContext
    private val pm get() = app.packageManager

    override fun isInstalled(packageName: String): Boolean =
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }

    override fun isLaunchable(packageName: String): Boolean =
        MolaPackages.ALLOWED.contains(packageName) &&
            runCatching { pm.getLaunchIntentForPackage(packageName) != null }.getOrDefault(false)

    override fun launch(packageName: String): Boolean {
        if (packageName !in MolaPackages.ALLOWED) return false
        return runCatching {
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
            app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    override fun showApplicationSettings(packageName: String): Boolean {
        if (packageName !in MolaPackages.ALLOWED) return false
        return runCatching {
            app.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
            true
        }.getOrDefault(false)
    }

    /** Only after a direct user gesture. No APK downloads or installations. */
    override fun showOfficialInstallGuide(): Boolean = runCatching {
        app.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(MolaPackages.RELEASES))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)
}

object MolaLinkCoordinator {
    fun inspect(gateway: MolaPackageGateway): MolaLinkState {
        for (pkg in MolaPackages.ALLOWED) {
            if (gateway.isInstalled(pkg)) return if (gateway.isLaunchable(pkg))
                MolaLinkState(MolaStatus.READY, pkg,
                    "Mola kurulu. Android'in uygulama başlatıcısıyla açılabilir.")
            else MolaLinkState(MolaStatus.CANNOT_LAUNCH, pkg,
                "Mola kurulu ancak açılış etkinliği bulunamadı. Uygulama ayarlarını kontrol et.")
        }
        return MolaLinkState(MolaStatus.NOT_INSTALLED, null,
            "Mola kurulu değil. Resmî yayın sayfasını açarak kurulum rehberini inceleyebilirsin.")
    }

    fun launch(gateway: MolaPackageGateway, current: MolaLinkState): MolaLinkState {
        val pkg = current.packageName ?: return inspect(gateway)
        if (pkg !in MolaPackages.ALLOWED || current.status != MolaStatus.READY)
            return current.copy(status = MolaStatus.ERROR,
                message = "Mola açılamadı. Önce kurulum ve uygulama durumunu kontrol et.")
        return if (gateway.launch(pkg))
            current.copy(message = "Android üzerinden Mola açılışı istendi. Gerçek çalışma DOĞRULANMADI.")
        else current.copy(status = MolaStatus.ERROR,
            message = "Mola açılamadı. Android uygulama ayarlarını kontrol et.")
    }
}

/** No private Mola APIs. All state updates are explicit, never from a Composable network call. */
class MolaLinkViewModel(application: Application): AndroidViewModel(application) {
    private val gateway: MolaPackageGateway = AndroidMolaPackageGateway(application)
    private val mutable = MutableStateFlow(MolaLinkState())
    val state = mutable.asStateFlow()

    fun refresh() {
        mutable.value = runCatching { MolaLinkCoordinator.inspect(gateway) }.getOrElse {
            MolaLinkState(MolaStatus.ERROR, message =
                "Mola durumu kontrol edilemedi. Uygulama ayarlarını kontrol et.")
        }
    }
    fun openMola() {
        mutable.value = MolaLinkCoordinator.launch(gateway, mutable.value)
    }
    fun openSettings() {
        val pkg = mutable.value.packageName
        val ok = pkg != null && gateway.showApplicationSettings(pkg)
        if (!ok) mutable.value = mutable.value.copy(
            message = "Mola uygulama ayarları açılamadı. Telefon Ayarlar > Uygulamalar'a git.")
    }
    fun openInstallGuide() {
        if (!gateway.showOfficialInstallGuide()) {
            mutable.value = mutable.value.copy(message =
                "Kurulum rehberi açılamadı. Tarayıcıdan github.com/moris545454/mola-apk/releases adresini aç.")
        }
    }
}
