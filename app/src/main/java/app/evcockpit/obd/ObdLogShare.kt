package app.evcockpit.obd

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Only shares this application's own log files through a time-limited URI grant. */
object ObdLogShare {
    fun launch(context: Context, logFilePath: String?): Boolean {
        val file = logFilePath?.let(::File)?.takeIf { it.isFile && it.length() > 0L }
            ?: return false
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "OBD kaydı", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "OBD kaydını paylaş"))
        return true
    }
}
