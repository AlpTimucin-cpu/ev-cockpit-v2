package app.evcockpit.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.evcockpit.navigation.DestinationsStore
import app.evcockpit.navigation.SavedDestination
import app.evcockpit.navigation.PhotonPlace
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.navigation.PlaceField
import app.evcockpit.navigation.GeoPoint
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.LaunchedEffect
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
import app.evcockpit.navigation.VoiceUiState

@Composable
fun RouteScreen(
    state: RouteUiState,
    gps: GpsSnapshot,
    socPct: Double?,
    socSource: String,
    usableKwh: Double,
    voice: VoiceUiState,
    onToggleVoice: () -> Unit,
    onSpeakNextManeuver: () -> Unit,
    onCalculate: (String, String) -> Unit,
    onSelectDestination: (GeoPoint) -> Unit,
    onSelectNamedDestination: (GeoPoint, String) -> Unit,
    onSelectOrigin: (GeoPoint, String) -> Unit,
    onUseGpsOrigin: () -> Unit,
    onClearDestinationSelection: () -> Unit,
    onSelectAlternative: (Int) -> Unit,
    searchVm: PlaceAutocompleteViewModel,
    onClear: () -> Unit,
) {
    val ctx = LocalContext.current
    val store = remember(ctx) { DestinationsStore(ctx.applicationContext) }
    val saved by store.entries.collectAsState(initial = emptyList())
    val searchUi by searchVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var advanced by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var averageText by remember { mutableStateOf("") }

    LaunchedEffect(state.destination) {
        state.destination?.let {
            latitude = it.latitude.toString()
            longitude = it.longitude.toString()
        }
    }

    val gpsFresh = rememberGpsFreshness(gps)
    val route = state.route
    val position = if (route != null && gpsFresh &&
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
        Text("NEREDEN → NEREYE", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text("Başlangıç: ${state.originLabel}" +
            if (state.origin == null) " • ${gps.status}" else " • MANUEL SEÇİM")
        OutlinedTextField(
            value = searchUi.originText,
            onValueChange = {
                onUseGpsOrigin()
                searchVm.onTextChanged(PlaceField.ORIGIN, it)
            },
            label = { Text("Nereden? Şehir, ilçe, adres") },
            placeholder = { Text("Örn. Kayseri veya Antalya Otogar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (searchUi.searchingOrigin) Text("Başlangıç önerileri aranıyor…")
        else Text(searchUi.originStatus)
        searchUi.originResults.forEach { suggestion ->
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                searchVm.selected(PlaceField.ORIGIN, suggestion)
                onSelectOrigin(suggestion.point, suggestion.label)
            }) { Text("↗ " + suggestion.label) }
        }
        OutlinedButton(onClick = {
            searchVm.useGpsOrigin()
            onUseGpsOrigin()
        }) { Text("Başlangıç için mevcut GPS konumumu kullan") }

        Text("Hedef: " + (state.destinationLabel.ifBlank {
            if (state.destination == null) "Henüz seçilmedi" else "Haritadan seçilen yer"
        }))
        OutlinedTextField(
            value = searchUi.destinationText,
            onValueChange = {
                onClearDestinationSelection()
                searchVm.onTextChanged(PlaceField.DESTINATION, it)
            },
            label = { Text("Nereye? Şehir, adres, otel veya yer") },
            placeholder = { Text("Örn. Antalya, Konyaaltı, otel adı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (searchUi.searchingDestination) Text("Hedef önerileri aranıyor…")
        else Text(searchUi.destinationStatus)
        searchUi.destinationResults.forEach { suggestion ->
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                searchVm.selected(PlaceField.DESTINATION, suggestion)
                onSelectNamedDestination(suggestion.point, suggestion.label)
                scope.launch {
                    store.save(SavedDestination("RECENT", suggestion.label, suggestion.point))
                }
            }) { Text("● " + suggestion.label) }
        }
        Text("Yazarken öneri: Open-Meteo şehirler + Photon adresleri (ücretsiz, hız sınırlı). " +
            "Girdiler arama sağlayıcılarına gönderilir; adresi haritada kontrol et.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("HOME", "Ev", point)) }
                }
            }) { Text("Ev olarak kaydet") }
            Button(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("WORK", "İş", point)) }
                }
            }) { Text("İş olarak kaydet") }
        }
        saved.forEach { item ->
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                searchVm.selected(PlaceField.DESTINATION, PhotonPlace(item.label, item.point))
                onSelectNamedDestination(item.point, item.label)
            }) { Text((if (item.type == "HOME") "Ev" else if (item.type == "WORK") "İş"
                else "Son hedef") + ": " + item.label) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onToggleVoice) {
                Text(if (voice.enabled) "Sesli tarifi kapat" else "Türkçe sesli tarifi aç")
            }
            OutlinedButton(onClick = onSpeakNextManeuver, enabled = voice.enabled) {
                Text("Sıradaki manevrayı oku")
            }
        }
        Text("Ses: ${voice.status} • Ses odağı: geçici MAY_DUCK")
        OutlinedButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) "Gelişmiş koordinat girişini gizle" else
                "Gelişmiş: hedef koordinatı gir")
        }
        if (advanced) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = latitude, onValueChange = { latitude = it },
                    label = { Text("Enlem") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
                OutlinedTextField(value = longitude, onValueChange = { longitude = it },
                    label = { Text("Boylam") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
            }
            OutlinedButton(onClick = {
                val parsed = RouteTargetValidator.parse(latitude, longitude)
                if (parsed == null) targetError = "Geçersiz koordinat (-90..90 / -180..180)"
                else {
                    targetError = null
                    onSelectNamedDestination(parsed, "Manuel koordinat")
                }
            }) { Text("Gelişmiş hedefi seç") }
        }
        targetError?.let { Text(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("HOME", "Ev", point)) }
                }
            }) { Text("Ev olarak kaydet") }
            Button(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("WORK", "İş", point)) }
                }
            }) { Text("İş olarak kaydet") }
        }
        saved.forEach { item ->
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                onSelectDestination(item.point)
            }) { Text((if (item.type == "HOME") "Ev" else if (item.type == "WORK") "İş"
                else "Son hedef") + ": " + item.label) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onToggleVoice) {
                Text(if (voice.enabled) "Sesli tarifi kapat" else "Türkçe sesli tarifi aç")
            }
            OutlinedButton(onClick = onSpeakNextManeuver, enabled = voice.enabled) {
                Text("Sıradaki manevrayı oku")
            }
        }
        Text("Ses: ${voice.status} • Ses odağı: geçici MAY_DUCK")
        OutlinedButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) "Gelişmiş koordinat girişini gizle" else "Gelişmiş: koordinat gir")
        }
        if (advanced) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = latitude, onValueChange = { latitude = it },
                    label = { Text("Enlem") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
                OutlinedTextField(value = longitude, onValueChange = { longitude = it },
                    label = { Text("Boylam") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f))
            }
            OutlinedButton(onClick = {
                val parsed = RouteTargetValidator.parse(latitude, longitude)
                if (parsed == null) targetError = "Geçersiz koordinat (-90..90 / -180..180)"
                else {
                    targetError = null
                    onSelectDestination(parsed)
                }
            }) { Text("Gelişmiş hedefi seç") }
        }
        targetError?.let { Text(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !state.busy && state.destination != null, onClick = {
                state.destination?.let { point ->
                    onCalculate(point.latitude.toString(), point.longitude.toString())
                    scope.launch { store.save(SavedDestination("RECENT", "Son hedef", point)) }
                }
            }) {
                Text(if (state.busy) "Hesaplanıyor" else "Rota hesapla")
            }
            OutlinedButton(onClick = onClear) { Text("Rotayı temizle") }
        }
        if (route != null) {
            Text("GÜZERGÂH SEÇENEKLERİ", style =
                androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(state.alternativesNote)
            state.alternatives.forEachIndexed { index, candidate ->
                val selected = index == state.activeAlternativeIndex
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectAlternative(index) },
                ) {
                    Text((if (selected) "✓ " else "") + "Rota ${index + 1} • " +
                        "%.1f km".format(candidate.distanceMeters / 1000.0) + " • " +
                        "~%.0f dk".format(candidate.durationSeconds / 60.0) +
                        " • TAHMİN / trafik yok")
                }
            }
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
            Text("SoC kaynağı: " + socSource)
            Text("Varış enerjisi (TAHMİN): " +
                (expectedKwh?.let { "%.1f kWh".format(it) } ?: "-- kWh — veri eksik"))
            Text("Formül: ${usableKwh.toInt()} × SoC%/100 − kalan_km × ort_tüketim/100")
            Text("SoC OBD PID henüz doğrulanmadı. Tüketim kullanıcı girdisidir; " +
                "eğim, rüzgâr, sıcaklık ve batarya kayıpları hesaba katılmaz.")
            Text("© openrouteservice • OpenStreetMap katkıcıları")
        } else {
            Text("Önce Ayarlar sekmesinden HeiGIT anahtarını ve GPS'i etkinleştir.")
            Text("Adres arama, Ev/İş, son hedef veya gösterge haritasına uzun basarak hedef seç.")
        }
    }
}
