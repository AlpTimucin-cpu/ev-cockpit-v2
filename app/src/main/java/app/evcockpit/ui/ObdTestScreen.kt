package app.evcockpit.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.evcockpit.obd.ObdRepository

@SuppressLint("MissingPermission")
@Composable
fun ObdTestScreen(
    repo: ObdRepository,
    devices: List<BluetoothDevice>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val log by repo.log.collectAsStateWithLifecycle()
    val path by repo.logFilePath.collectAsStateWithLifecycle()
    val state by repo.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(log.size) { if (log.isNotEmpty()) listState.scrollToItem(log.lastIndex) }

    Row(modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("OBD bağlantı testi", fontSize = 18.sp)
            Text(if (state.connected) "Durum: bağlı" else "Durum: bağlı değil", fontSize = 14.sp)
            Text("Önce cihazı telefonun Bluetooth ayarlarından eşleştir (PIN: 1234 veya 0000).", fontSize = 12.sp)
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Eşleşmiş cihazları yenile") }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(devices) { d ->
                    Button(onClick = { repo.connect(d) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${d.name ?: "?"}  (${d.address})", fontSize = 13.sp)
                    }
                }
            }
            OutlinedButton(onClick = { repo.startDemo() }, modifier = Modifier.fillMaxWidth()) { Text("Demo verisi") }
            OutlinedButton(onClick = { repo.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Bağlantıyı kes") }
            path?.let { Text("Log: $it", fontSize = 11.sp) }
        }
        LazyColumn(Modifier.weight(0.6f).fillMaxSize(), state = listState) {
            items(log) { line ->
                val color = when {
                    line.contains("HATA") || line.contains("!") -> Color(0xFFFF8A80)
                    line.contains("= ") -> Color(0xFF69F0AE)
                    else -> Color.Unspecified
                }
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = color)
            }
        }
    }
}
