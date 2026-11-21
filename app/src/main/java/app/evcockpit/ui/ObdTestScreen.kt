package app.evcockpit.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.obd.VehicleState
import app.evcockpit.planui.ObdEvidence

@SuppressLint("MissingPermission")
@Composable
fun ObdTestScreen(
    state: VehicleState,
    log: List<String>,
    logFilePath: String?,
    diagnosisFilePath: String?,
    diagnosisSummary: String,
    devices: List<BluetoothDevice>,
    message: String?,
    onRefresh: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit,
    onDiagnose: () -> Unit,
    onShareDiagnosis: () -> Unit,
    onExportTestReport: (String) -> Unit,
    onDemo: () -> Unit,
    onStop: () -> Unit,
    onShareLog: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var carScannerNote by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(log.size) { if (log.isNotEmpty()) listState.scrollToItem(log.lastIndex) }

    Row(modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("OBD bağlantı testi", fontSize = 18.sp)
            Text(state.statusText, fontSize = 14.sp)
            SystemCompatibilityPanel()
            Text("ELM327 / ECU veri güveni", fontSize = 14.sp)
            ObdEvidence.describe(state.connected,state.demo,state.socPct,state.speedKmh)
                .forEach { text ->
                    Text(text, fontSize = 11.sp)
                }
            message?.let { Text(it, color = Color(0xFFFFB300), fontSize = 12.sp) }
            Text("Cihazı telefonun Bluetooth ayarlarında eşleştirin (PIN genellikle 1234 veya 0000).", fontSize = 12.sp)
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Eşleşmiş cihazları yenile")
            }
            if (devices.isEmpty()) {
                Text("Eşleşmiş cihaz yok", fontSize = 13.sp)
                OutlinedButton(onClick = onOpenBluetoothSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Bluetooth ayarlarını aç")
                }
            } else {
                devices.forEach { device ->
                    Button(onClick = { onConnect(device) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${device.name ?: "?"} (${device.address})", fontSize = 13.sp)
                    }
                }
            }
            Button(onClick = onDiagnose, enabled = devices.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()) {
                Text("Tanı modunu çalıştır")
            }
            Text("Araç park halinde Ready/kontak açıkken dene. Tanı, normal sorgulamayı durdurur.",
                fontSize = 12.sp)
            Text("Tanı özeti (ham ECU sonucu): " + diagnosisSummary,
                fontSize = 12.sp)
            OutlinedButton(onClick = onShareDiagnosis,
                enabled = diagnosisFilePath != null,
                modifier = Modifier.fillMaxWidth()) {
                Text("Ayrı tanı logunu paylaş")
            }
            diagnosisFilePath?.let { Text("Tanı kaydı: " + it, fontSize = 10.sp) }
            OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) { Text("Demo verisi") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Bağlantıyı kes") }
            OutlinedButton(onClick = onShareLog, modifier = Modifier.fillMaxWidth()) {
                Text("OBD logunu paylaş")
            }
            Text("Car Scanner karşılaştırması (isteğe bağlı, elle girilir)", fontSize = 13.sp)
            Text("Aynı adaptörle okunan protokol/010D/SoC değerlerini not et. VIN, konum, MAC veya API anahtarı yazma.", fontSize = 11.sp)
            OutlinedTextField(value = carScannerNote,
                onValueChange = { carScannerNote = it.take(600) },
                modifier = Modifier.fillMaxWidth(), maxLines = 5,
                label = { Text("Car Scanner gözlem notu (maks. 600 karakter)") })
            Button(onClick = { onExportTestReport(carScannerNote) }, modifier = Modifier.fillMaxWidth()) {
                Text("Güvenli test raporunu dışa aktar")
            }
            Text("Test raporunda API anahtarı, koordinat ve VIN gizlenir.", fontSize = 12.sp)
            logFilePath?.let { Text("Kayıt: $it", fontSize = 10.sp) }
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
