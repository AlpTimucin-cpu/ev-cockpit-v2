package app.evcockpit.planner.model

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Prevent release of an ungrounded or absent M8 gate report. */
class M8GateReportTest {
    @Test fun gateDocumentHasTenLineSummaryAndEvidence() {
        val report=File("../docs/M8_DURMA_NOKTASI_A.md")
        assertTrue("M8 A report absent",report.isFile)
        val text=report.readText(Charsets.UTF_8)
        val lines=text.lines().filter { Regex("""^\d{1,2}\. """).containsMatchIn(it) }
        assertEquals("Exactly ten summary lines required",10,lines.size)
        assertTrue(text.contains("## Kanıt tablosu"))
        assertTrue(text.contains("DURMA NOKTASI A"))
        assertTrue(text.contains("DOĞRULANMADI"))
        assertTrue(text.contains("M8.6"))
        assertTrue(text.contains("https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44748718220"))
    }
}
