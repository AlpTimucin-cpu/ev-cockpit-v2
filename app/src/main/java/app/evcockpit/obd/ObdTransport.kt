package app.evcockpit.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface ObdTransport {
    suspend fun open()
    suspend fun write(text: String)

    /** ELM327 '>' istemi gelene kadar okur. Zaman aşımında o ana kadar gelen metni döndürür. */
    suspend fun readUntilPrompt(timeoutMs: Long): String
    fun close()
}

/** Klasik Bluetooth (SPP/RFCOMM). Ucuz ELM327 cihazlarının büyük çoğunluğu böyle çalışır. */
class BluetoothSppTransport(private val device: BluetoothDevice) : ObdTransport {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    @SuppressLint("MissingPermission")
    override suspend fun open() {
        withContext(Dispatchers.IO) {
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            s.connect()
            socket = s
            input = s.inputStream
            output = s.outputStream
        }
    }

    override suspend fun write(text: String) {
        withContext(Dispatchers.IO) {
            val ins = input
            // Eski artık verileri temizle
            while (ins != null && ins.available() > 0) ins.read()
            output?.write((text + "\r").toByteArray())
            output?.flush()
        }
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val ins = input ?: return@withContext ""
        val sb = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (ins.available() > 0) {
                val b = ins.read()
                if (b < 0) break
                val c = b.toChar()
                if (c == '>') break
                sb.append(c)
            } else {
                delay(5)
            }
        }
        sb.toString()
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null; input = null; output = null
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
