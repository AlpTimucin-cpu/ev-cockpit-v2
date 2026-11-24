package app.evcockpit.ui.unified.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * C1 kabul kapısı: src/main/java altında tek MapView oluşturulabilir.
 * Kod inceleme testi; POCO F5 native GPU/saha kanıtı değildir.
 */
class SingleMapHostSourceTest {
    @Test
    fun mapViewYalnizMapHostDosyasindaOlusturulur() {
        val cwd = File(System.getProperty("user.dir"))
        val roots = listOf(
            File(cwd, "src/main/java"),
            File(cwd, "app/src/main/java"),
        )
        val root = roots.firstOrNull { it.isDirectory }
        assertTrue("CI kaynak dizini bulunamadı: ${cwd.absolutePath}", root != null)

        val expression = Regex("""\bMapView\s*\(""")
        val matches = root!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { expression.containsMatchIn(it.value) }
                    .map { line -> "${file.relativeTo(root).path}:${line.index + 1}" }
            }
            .toList()

        assertEquals(
            "Yeni MapView yapıcıları yalnız MapHost.kt dosyasında olmalı: $matches",
            1, matches.size,
        )
        assertTrue(
            "Tek MapView kaynağı MapHost.kt olmalı: $matches",
            matches.single().startsWith(
                "app/evcockpit/ui/unified/map/MapHost.kt:",
            ),
        )
    }
}
