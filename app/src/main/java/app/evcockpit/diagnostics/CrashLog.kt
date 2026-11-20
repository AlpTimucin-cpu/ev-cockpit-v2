package app.evcockpit.diagnostics

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Privacy by construction: no Throwable.message, no parameters, no input values. */
object CrashRecord {
    const val MAX_FILE_BYTES = 65_536
    private val safeName = Regex("[^A-Za-z0-9_.$]")
    fun format(thread: Thread, error: Throwable, timestamp: String): String {
        val kind = error.javaClass.name.replace(safeName, "_").take(128)
        val threadLabel = thread.name.replace(safeName, "_").take(40)
        val frames = error.stackTrace.filter { it.className.startsWith("app.evcockpit.") }
            .take(8).map {
                val clazz = it.className.replace(safeName, "_").take(100)
                val method = it.methodName.replace(safeName, "_").take(80)
                "at $clazz.$method:" + it.lineNumber.coerceAtLeast(-1)
            }
        return (listOf("UNCAUGHT $timestamp $threadLabel $kind") +
            if (frames.isEmpty()) listOf("application stack frames unavailable") else frames)
            .joinToString("\n").take(2048)
    }

    fun retainTail(existing: String, incoming: String, limit: Int = MAX_FILE_BYTES): String =
        (existing + "\n---\n" + incoming).takeLast(limit)

    fun lastEntry(fileText: String): String? =
        fileText.substringAfterLast("\n---\n").trim().takeIf { it.isNotBlank() }
}

object CrashLog {
    private const val FILE = "crash.log"
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val payload = CrashRecord.format(thread, error, stamp)
                val file = File(context.filesDir, FILE)
                val existing = if (file.isFile) file.readText(Charsets.UTF_8)
                    .takeLast(CrashRecord.MAX_FILE_BYTES) else ""
                file.writeText(CrashRecord.retainTail(existing, payload), Charsets.UTF_8)
            } catch (_: Throwable) {
                // Never mask the original crash.
            } finally {
                previous?.uncaughtException(thread, error)
            }
        }
    }
    fun lastRecord(context: Context): String? = runCatching {
        val file = File(context.filesDir, FILE)
        if (!file.isFile) null else CrashRecord.lastEntry(file.readText(Charsets.UTF_8))
    }.getOrNull()
}
