package app.evcockpit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.DestinationsStore
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.PhotonPlace
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.navigation.PlaceField
import app.evcockpit.navigation.RouteAlternativePreview
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.navigation.SavedDestination
import app.evcockpit.navigation.VoiceUiState
import app.evcockpit.planner.model.VehicleProfile
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * B aşaması: adres seçimi -> gerçek OSRM alternatifleri -> enerji/SoC karşılaştırması.
 * Enlem/boylam kullanıcı alanı değildir; seçimler öneriden veya haritadan yapılır.
 */
@Composable
fun RouteScreen(
    state: RouteUiState,
    gps: GpsSnapshot,
    socPct: Double?,
    socSource: String,
    vehicleProfile: VehicleProfile,
    voice: VoiceUiState,
    onToggleVoice: () -> Unit,
    onSpeakNextManeuver: () -> Unit,
    onCalculate: () -> Unit,
    onSelectDestination: (GeoPoint) -> Unit,
    onSelectNamedDestination: (GeoPoint, String) -> Unit,
    onSelectOrigin: (GeoPoint, String) -> Unit,
    onUseGpsOrigin: () -> Unit,
    onClearDestinationSelection: () -> Unit,
    onSelectAlternative: (Int) -> Unit,
    onManualSoc: (Double) -> Unit,
    searchVm: PlaceAutocompleteViewModel,
    routeWeather: String,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { DestinationsStore(context.applicationContext) }
    val saved by store.entries.collectAsState(initial = emptyList())
    val suggestions by searchVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var manualSocText by remember { mutableStateOf("") }
    var minArrivalText by remember { mutableStateOf("15") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val minimumArrival = minArrivalText.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it in 0.0..100.0 }
    val gpsFresh = rememberGpsFreshness(gps)
    val position = state.route?.takeIf { gpsFresh && gps.point != null }
        ?.let { route -> gps.point?.let { RouteProgress.locate(route, it) } }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("EV ROTA PLANLAYICI", style = MaterialTheme.typography.titleLarge)
        Text("OSRM gerçek yol ağı • Türkiye öncelikli Photon • trafik verisi yok",
            style = MaterialTheme.typography.bodySmall)
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
        Text(routeWeather, style = MaterialTheme.typography.bodySmall)

        Text("NEREDEN", style = MaterialTheme.typography.titleMedium)
        Text("Seçili başlangıç: ${state.originLabel}")
        OutlinedTextField(
            value = suggestions.originText,
            onValueChange = {
                onUseGpsOrigin()
                searchVm.onTextChanged(PlaceField.ORIGIN, it)
            },
            label = { Text("Şehir, ilçe, cadde veya yer adı") },
            placeholder = { Text("Örn. Niğde, Bor veya Kayseri Otogar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.searchingOrigin) Text("Adres kaynakları sorgulanıyor…")
        else Text(suggestions.originStatus)
        suggestions.originResults.forEach { place ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    searchVm.selected(PlaceField.ORIGIN, place)
                    onSelectOrigin(place.point, place.label)
                },
            ) { Text(place.label) }
        }
        OutlinedButton(onClick = {
            searchVm.useGpsOrigin()
            onUseGpsOrigin()
        }) { Text("Başlangıç: konumum (GPS)") }

        Text("NEREYE", style = MaterialTheme.typography.titleMedium)
        Text("Seçili hedef: ${state.destinationLabel.ifBlank { "Henüz seçilmedi" }}")
        OutlinedTextField(
            value = suggestions.destinationText,
            onValueChange = {
                onClearDestinationSelection()
                searchVm.onTextChanged(PlaceField.DESTINATION, it)
            },
            label = { Text("Şehir, otel, işletme veya açık adres") },
            placeholder = { Text("Örn. Antalya, Konyaaltı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.searchingDestination) Text("Adres kaynakları sorgulanıyor…")
        else Text(suggestions.destinationStatus)
        suggestions.destinationResults.forEach { place ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    searchVm.selected(PlaceField.DESTINATION, place)
                    onSelectNamedDestination(place.point, place.label)
                    scope.launch {
                        store.save(SavedDestination("RECENT", place.label, place.point))
                    }
                },
            ) { Text(place.label) }
        }
        Text("İl/ilçe merkezleri 2 harften itibaren çevrim dışı gösterilir. " +
            "Tam adres için 250 ms yazma beklemesi vardır; Photon genel sunucusuna " +
            "en fazla 10 saniyede bir istek gönderilir. Öneriyi haritada doğrula.",
            style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("HOME", "Ev", point)) }
                }
            }) { Text("Ev olarak kaydet") }
            OutlinedButton(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("WORK", "İş", point)) }
                }
            }) { Text("İş olarak kaydet") }
        }
        saved.forEach { item ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    searchVm.selected(PlaceField.DESTINATION, PhotonPlace(item.label, item.point))
                    onSelectNamedDestination(item.point, item.label)
                },
            ) {
                Text((if (item.type == "HOME") "Ev" else if (item.type == "WORK") "İş"
                    else "Son arama") + ": " + item.label)
            }
        }

        Text("BATARYA VE HEDEF REZERVİ", style = MaterialTheme.typography.titleMedium)
        Text("Araç: ID.4 Pure • kullanılabilir kapasite: " +
            fmt(vehicleProfile.capacityAtSohKwh) + " kWh")
        Text("Başlangıç SoC: ${socPct?.let { fmt(it) + " %" } ?: "bilinmiyor"} • kaynak: $socSource")
        OutlinedTextField(
            value = manualSocText,
            onValueChange = { manualSocText = it.take(6) },
            label = { Text("Manuel başlangıç SoC (%) — OBD gerekmez") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = {
            val soc = manualSocText.replace(',', '.').toDoubleOrNull()
            if (soc == null || soc !in 0.0..100.0) {
                inputError = "SoC için 0 ile 100 arasında sayı gir."
            } else {
                onManualSoc(soc)
                manualSocText = ""
                inputError = null
            }
        }) { Text("Başlangıç SoC kaydet") }
        OutlinedTextField(
            value = minArrivalText,
            onValueChange = { minArrivalText = it.take(6) },
            label = { Text("Minimum varış SoC (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        if (minimumArrival == null) {
            Text("Minimum varış yüzdesi 0–100 arasında olmalı. Ön hesap yapılmayacak.",
                color = MaterialTheme.colorScheme.error)
        }
        inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.destination != null && !state.busy, onClick = {
                onCalculate()
                state.destination?.let { dest ->
                    scope.launch {
                        store.save(SavedDestination("RECENT",
                            state.destinationLabel.ifBlank { "Son hedef" }, dest))
                    }
                }
            }) { Text(if (state.busy) "Rotalar hesaplanıyor…" else "Rotaları karşılaştır") }
            OutlinedButton(onClick = onClear) { Text("Temizle") }
        }

        if (state.route != null) {
            Text("HARİTADAKİ GERÇEK ALTERNATİFLER",
                style = MaterialTheme.typography.titleMedium)
            Text(state.alternativesNote)
            OnlineMap(
                modifier = Modifier.fillMaxWidth().height(310.dp),
                route = state.route,
                destination = state.destination,
                gps = gps,
                onSelectDestination = onSelectDestination,
                alternativeRoutes = state.alternatives,
                canChangeLayer = true,
            )
            state.alternatives.forEachIndexed { index, candidate ->
                val selected = index == state.activeAlternativeIndex
                val preview = RouteAlternativePreview.calculate(
                    candidate, vehicleProfile, socPct, minimumArrival ?: Double.NaN)
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text((if (selected) "✓ SEÇİLİ • " else "") + "Rota ${index + 1}",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Mesafe: ${fmt(candidate.distanceMeters / 1000.0)} km" +
                            "  •  Süre: ${time(candidate.durationSeconds)} (trafiksiz)")
                        Text("Enerji: ${preview?.let { fmt(it.routeEnergyKwh) + " kWh" } ?: "—"}" +
                            "  •  Ort. tüketim: " +
                            (preview?.let { fmt(it.averageKwhPer100Km) + " kWh/100 km" } ?: "—"))
                        val arrival = when {
                            preview == null || preview.startSocPct == null -> "Başlangıç SoC bilinmiyor"
                            preview.arrivalSocPct == null -> "Şarj olmadan varış mümkün görünmüyor"
                            else -> "${fmt(preview.arrivalSocPct)} %"
                        }
                        Text("Tahmini varış SoC: $arrival")
                        val charge = when (preview?.requiresCharging) {
                            false -> "Ön modele göre durak gerekmiyor"
                            true -> "Şarj gerekli • gereken ek enerji en az " +
                                "${fmt(preview?.extraKwhForMinimum ?: 0.0)} kWh"
                            null -> "SoC bilinmiyor; durak planı hesaplanamaz"
                        }
                        Text("Şarj: $charge")
                        Text(if (preview?.requiresCharging == false)
                            "Yol üzerinde şarj: 0 dk • 0 TL (önden tahmin)"
                            else "Şarj süresi: — • Şarj ücreti: fiyat/istasyon planı yok",
                            style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled = !state.busy && !selected,
                            onClick = { onSelectAlternative(index) },
                        ) { Text(if (selected) "Seçili rota" else "Bu rotayı seç") }
                    }
                }
            }
            Text("Enerji: 20°C, düz yol, ortalama hız ve otomatik klima ile ÖN TAHMİN. " +
                "Yükselti, rüzgâr, şarj istasyonu ve tarifeler D/C/E aşamalarında eklenir. " +
                "Yetersiz SoC hiçbir zaman gerçek varış garantisi değildir.",
                style = MaterialTheme.typography.bodySmall)
            Text("GPS: " + (position?.let {
                "Kalan mesafe ~${fmt(it.remainingMeters / 1000.0)} km"
            } ?: "Rota üzerinde doğrulanmış güncel konum yok"))
            Text("© OpenStreetMap katkıcıları • OSRM/FOSSGIS; canlı trafik yok",
                style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Başlangıç ve hedef seçildikten sonra gerçek alternatifler haritada " +
                "karşılaştırılır. Sunucu yalnız tek yol döndürürse diğer yollar uydurulmaz.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onToggleVoice) {
                Text(if (voice.enabled) "Sesli tarifi kapat" else "Türkçe sesli tarifi aç")
            }
            OutlinedButton(onClick = onSpeakNextManeuver, enabled = voice.enabled) {
                Text("Sıradaki manevrayı oku")
            }
        }
        Text("Sesli tarif: ${voice.status}", style = MaterialTheme.typography.bodySmall)
    }
}

private fun fmt(value: Double): String =
    String.format(Locale.forLanguageTag("tr-TR"), "%.1f", value)

private fun time(seconds: Double): String {
    val minutes = (seconds / 60.0).toInt().coerceAtLeast(0)
    return if (minutes < 60) "$minutes dk"
    else "${minutes / 60} sa ${minutes % 60} dk"
}
