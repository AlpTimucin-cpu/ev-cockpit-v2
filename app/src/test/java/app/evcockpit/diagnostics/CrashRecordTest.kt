package app.evcockpit.diagnostics

import app.evcockpit.obd.SafeTestReportText
import org.junit.Assert.*
import org.junit.Test

class CrashRecordTest {
    @Test fun exceptionMessagesNeverEnterCrashEnvelope() {
        val e = IllegalStateException("VIN WVW123SECRET 38.267123,34.687321 api_key=TOKEN")
        e.stackTrace = arrayOf(StackTraceElement("app.evcockpit.MainActivity", "onCreate", "MainActivity.kt", 42))
        val record = CrashRecord.format(Thread.currentThread(), e, "2026-11-20")
        assertTrue(record.contains("IllegalStateException"))
        assertTrue(record.contains("MainActivity.onCreate:42"))
        assertFalse(record.contains("WVW123SECRET"))
        assertFalse(record.contains("TOKEN"))
        assertFalse(record.contains("38.267123"))
    }
    @Test fun boundedLogKeepsFinalRecord() {
        val record = CrashRecord.retainTail("x".repeat(100_000), "UNCAUGHT latest")
        assertTrue(record.length <= CrashRecord.MAX_FILE_BYTES)
        assertTrue(CrashRecord.lastEntry(record)!!.contains("latest"))
    }
    @Test fun safeReportContainsSanitizedCrashRecord() {
        val result = SafeTestReportText.build("1", "17", "POCO", emptyList(), "none",
            emptyList(), "UNCAUGHT\nVIN: SECRET-987")
        assertTrue(result.contains("Son yakalanmamış hata"))
        assertFalse(result.contains("SECRET-987"))
        assertTrue(result.contains("[HASSAS SATIR GİZLENDİ]"))
    }
}
