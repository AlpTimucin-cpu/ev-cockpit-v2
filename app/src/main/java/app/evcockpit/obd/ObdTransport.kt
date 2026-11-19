package app.evcockpit.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdTimeoutException(message: String) : IOException(message)

interface ObdTransport {
    suspend fun open()
    suspend fun write(text: String)

    /** Return only after '>' prompt; otherwise throw a recoverable timeout. */
    suspend fun readUntilPrompt(timeoutMs: Long): String
    fun close()

    /** Three-second passive CAN capture; implementations must send CR and regain prompt. */
    suspend fun monitorFor(durationMs: Long): String =
        throw UnsupportedOperationException("ATMA izleme desteklenmiyor")

    /** A lone CR resynchronizes the ELM prompt after a timeout (no input flush). */
    suspend fun sendBreak(): Unit = write("")
}

/** Classic Bluetooth SPP/RFCOMM. The socket can be closed from another thread to abort connect. */
class BluetoothSppTransport(private val device: BluetoothDevice) : ObdTransport {
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var input: InputStream? = null
    @Volatile private var output: OutputStream? = null

    @SuppressLint("MissingPermission")
    override suspend fun open() {
        withContext(Dispatchers.IO) {
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket = s // make it closeable while connect() is blocked
            try {
                s.connect()
                input = s.inputStream
                output = s.outputStream
            } catch (e: Exception) {
                close()
                throw e
            }
        }
    }

    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val ins = input ?: throw IOException("Bluetooth giriş akışı kapalı")
        val out = output ?: throw IOException("Bluetooth çıkış akışı kapalı")
        // Stale fragments must not be interpreted as the next command's response.
        while (ins.available() > 0) ins.read()
        out.write((text + "\r").toByteArray(Charsets.US_ASCII))
        out.flush()
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val ins = input ?: throw IOException("Bluetooth giriş akışı kapalı")
        val buffer = StringBuilder()
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        while (System.nanoTime() < deadline) {
            if (ins.available() > 0) {
                val byte = ins.read()
                if (byte < 0) throw IOException("Bluetooth bağlantısı kapandı")
                val char = byte.toChar()
                if (char == '>') return@withContext buffer.toString()
                buffer.append(char)
                if (buffer.length > 16_384) throw IOException("ELM327 yanıtı boyut sınırını aştı")
            } else {
                delay(10)
            }
        }
        throw ObdTimeoutException("ELM327 istem işareti (>) zaman aşımına uğradı")
    }

    override suspend fun sendBreak() = withContext(Dispatchers.IO) {
        val out = output ?: throw IOException("Bluetooth çıkış akışı kapalı")
        out.write(byteArrayOf(13))
        out.flush()
    }

    override suspend fun monitorFor(durationMs: Long): String = withContext(Dispatchers.IO) {
        val ins = input ?: throw IOException("Bluetooth giriş akışı kapalı")
        write("ATMA")
        val outputText = StringBuilder()
        val deadline = System.nanoTime() + durationMs * 1_000_000L
        var endedEarly = false
        while (System.nanoTime() < deadline) {
            if (ins.available() > 0) {
                val byte = ins.read()
                if (byte < 0) throw IOException("CAN izlemede Bluetooth kapandı")
                if (byte == '>'.code) {
                    endedEarly = true
                    break
                }
                if (outputText.length < 32_768) outputText.append(byte.toChar())
            } else delay(10)
        }
        if (!endedEarly) {
            sendBreak() // ATMA is a streaming command; do not flush pending CAN frames.
            try {
                val tail = readUntilPrompt(1_800)
                if (outputText.length + tail.length < 32_768) outputText.append(tail)
            } catch (failure: ObdTimeoutException) {
                close() // Never send another AT command to an unsynchronized clone.
                throw ObdTimeoutException("ATMA CR sonrasında istem işareti yok; yeniden bağlantı gerekli")
            }
        }
        outputText.toString()
    }

    override fun close() {
        // Closing an in-progress socket must interrupt connect() as well.
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
