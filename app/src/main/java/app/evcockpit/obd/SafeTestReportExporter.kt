package app.evcockpit.obd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Restricted input: OBD UI log only. No navigation/GPS repository and no key store access.
 * All potentially sensitive lines are completely removed, including VIN (0902).
 */
object SafeTestReportText {
    private val sensitive = Regex(
        """(?i)(?:0902|VIN|authorization|bearer|api[\s_-]*key|token|secret|password|latitude|longitude|koordinat|konum\s*:)"""
    )
    private val coordinates = Regex(
        """(?<![0-9])[-+]?[0-9]{1,3}[.,][0-9]{4,}\s*[,;/]\s*[-+]?[0-9]{1,3}[.,][0-9]{4,}"""
    )

    fun sanitizeLine(line: String): String {
        if (sensitive.containsMatchIn(line) || coordinates.containsMatchIn(line)) {
            return "[HASSAS SATIR GİZLENDİ]"
        }
        return line.take(512).replace("\u0000", "")
    }

    fun build(
        appVersion: String,
        androidVersion: String,
        deviceModel: String,
        permissionStatuses: List<String>,
        diagnosisSummary: String,
        lastObdLogLines: List<String>,
    ): String = buildString {
        appendLine("ID.4 YOL ARKADAŞI — GÜVENLİ TEST RAPORU")
        appendLine("Saha doğrulaması kullanıcı ham kanıtına bağlıdır; rapor bir başarı sertifikası değildir.")
        appendLine("Uygulama: " + sanitizeLine(appVersion))
        appendLine("Android: " + sanitizeLine(androidVersion))
        appendLine("Cihaz modeli: " + sanitizeLine(deviceModel))
        appendLine("İzinler:")
        permissionStatuses.forEach { appendLine("- " + sanitizeLine(it)) }
        appendLine("Son Tanı özeti:")
        diagnosisSummary.lines().take(12).forEach { appendLine(sanitizeLine(it)) }
        appendLine("Son 200 OBD log satırı (hassas alanlar gizlendi):")
        lastObdLogLines.takeLast(200).forEach { appendLine(sanitizeLine(it)) }
        appendLine("DİKKAT: Orijinal ham tanı dosyasında VIN bulunabilir; bu güvenli özet ham VIN içermez.")
    }
}

object SafeTestReportExporter {
    fun create(
        context: Context,
        diagnosisSummary: String,
        obdLogLines: List<String>,
    ): String {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        val requested = listOf(
            "BLUETOOTH_CONNECT" to Manifest.permission.BLUETOOTH_CONNECT,
            "ACCESS_FINE_LOCATION" to Manifest.permission.ACCESS_FINE_LOCATION,
            "ACCESS_COARSE_LOCATION" to Manifest.permission.ACCESS_COARSE_LOCATION,
            "POST_NOTIFICATIONS" to Manifest.permission.POST_NOTIFICATIONS,
        )
        val permissions = requested.map { (label, permission) ->
            label + ": " + if (Build.VERSION.SDK_INT < 31 &&
                permission == Manifest.permission.BLUETOOTH_CONNECT
            ) "bu Android'de izin farklı" else if (Build.VERSION.SDK_INT < 33 &&
                permission == Manifest.permission.POST_NOTIFICATIONS
            ) "bu Android'de gerekmiyor" else if (
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            ) "izin verildi" else "izin yok"
        }
        val text = SafeTestReportText.build(
            appVersion = (pkg.versionName ?: "bilinmiyor") + " / " + context.packageName,
            androidVersion = Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT,
            deviceModel = Build.MANUFACTURER + " " + Build.MODEL,
            permissionStatuses = permissions,
            diagnosisSummary = diagnosisSummary,
            lastObdLogLines = obdLogLines,
        )
        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(directory, "id4_test_report_" + stamp + ".txt")
        file.writeText(text, Charsets.UTF_8)
        return file.absolutePath
    }
}
