package app.evcockpit.planner.data

import java.io.IOException
import java.io.Reader

/**
 * Avoid String.readText(): that creates an unbounded allocation before truncating.
 * Oversize JSON is rejected as a whole, never truncated into an invalid cache entry.
 */
object BoundedResponseReader {
    const val MAX_OVERPASS_CHARS = 2_000_000

    fun read(reader: Reader, maxChars: Int = MAX_OVERPASS_CHARS): String {
        require(maxChars > 0)
        val out = StringBuilder()
        val buffer = CharArray(8192)
        while (true) {
            // Read one extra character at the boundary to distinguish EOF from overflow.
            val count = reader.read(buffer, 0, minOf(buffer.size, maxChars - out.length + 1))
            if (count < 0) return out.toString()
            if (count == 0) continue
            if (count > maxChars - out.length) {
                throw IOException("Overpass yanıtı boyut sınırını aştı; eksik JSON kullanılmaz")
            }
            out.append(buffer, 0, count)
        }
    }
}
