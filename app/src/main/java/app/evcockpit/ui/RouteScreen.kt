package app.evcockpit.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.ArrivalEnergyEstimator
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.navigation.RouteUiState

@Composable
fun RouteScreen(
    state: RouteUiState,
    gps: GpsSnapshot,
    socPct: Double?,
    usableKwh: Double,
    onCalculate: (String, String) -> Unit,
    onClear: () -> Unit,
) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var averageText by remember { mutableStateOf("") }

    val route = state.route
    val position = if (route != null && gps.isFresh(SystemClock.elapsedRealtime()) &&
        gps.point != null
    ) RouteProgress.locate(route, gps.point) else null
    val remainingKm = position?.remainingMeters?.div(1000.0)
    val average = averageText.trim().replace(',', '.').toDoubleOrNull()
    val expectedKwh = ArrivalEnergyEstimator.estimate(usableKwh, socPct, remainingKm, average)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Rota: openrouteservice • Türkçe manevralar • canlı trafik YOK")
        Text(state.message)
        Text("Konum: ${gps.status}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latitude, onValueChange = { latitude = it },
                label = { Text("Hedef enlem") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = longitude, onValueChange = { longitude = it },
                label = { Text("Hedef boylam") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !state.busy, onClick = { onCalculate(latitude, longitude) }) {
                Text(if (state.busy) "Hesaplanıyor" else "Rota hesapla")
            }
            OutlinedButton(onClick = onClear) { Text("Rotayı temizle") }
        }
        if (route != null) {
            Text("Son rota: %.1f km • yaklaşık %.0f dk (trafiksiz)".format(
                route.distanceMeters / 1000.0, route.durationSeconds / 60.0))
            Text("Rota çizgisi noktaları: ${route.geometry.size} • sağlayıcı: ${route.provider}")
            if (position == null) {
                Text("Rota üzerindeki güncel GPS konumu doğrulanamadı; kalan mesafe hesaplanmıyor.")
            } else {
                Text("Kalan mesafe (yaklaşık): %.1f km".format(position.remainingMeters / 1000.0))
                val step = position.nextManeuverIndex?.let { route.maneuvers.getOrNull(it) }
                Text("Sonraki manevra: " + (step?.instructionTr ?: "Yönlendirme verisi yok"))
                Text("Manevraya yaklaşık: " +
                    (position.distanceToManeuverMeters?.let { "%.0f m".format(it) } ?: "--"))
            }
            OutlinedTextField(
                value = averageText, onValueChange = { averageText = it },
                label = { Text("Ortalama tüketim (kWh/100 km) — kendin gir") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Varış enerjisi (TAHMİN): " +
                (expectedKwh?.let { "%.1f kWh".format(it) } ?: "-- kWh — veri eksik"))
            Text("Formül: ${usableKwh.toInt()} × SoC%/100 − kalan_km × ort_tüketim/100")
            Text("SoC OBD PID henüz doğrulanmadı. Tüketim kullanıcı girdisidir; " +
                "eğim, rüzgâr, sıcaklık ve batarya kayıpları hesaba katılmaz.")
            Text("© openrouteservice • OpenStreetMap katkıcıları")
        } else {
            Text("Önce Rota Ayarları'ndan HeiGIT anahtarını ve GPS'i etkinleştir.")
            Text("Hedef koordinatlarını haritadan veya bir kaynaktan gir; otomatik adres araması yok.")
        }
    }
}
