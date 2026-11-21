package app.evcockpit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.evcockpit.planui.CalibrationSuggestion
import app.evcockpit.planui.ConsumptionCalibration
import app.evcockpit.planner.model.PlanRequest
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.storage.CalibrationJournalCodec
import app.evcockpit.storage.CalibrationJournalStore
import kotlinx.coroutines.launch
import java.util.Locale

/** A parked-vehicle, user-entered calibration note; no CAN reads or automatic model updates. */
@Composable
fun CalibrationPanel(plan: PlanResult.Success, request: PlanRequest?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val journal = remember(context) { CalibrationJournalStore(context) }
    val records by journal.records.collectAsState(initial = emptyList())
    val model = remember(request) { request?.let(ConsumptionCalibration::modelKwhPer100Km) }
    var expanded by remember { mutableStateOf(false) }
    var consumptionText by remember(request) { mutableStateOf("") }
    var arrivalSocText by remember(request) { mutableStateOf("") }
    var suggestion by remember(request, plan) { mutableStateOf<CalibrationSuggestion?>(null) }
    var feedback by remember { mutableStateOf("") }
    fun format(x: Double) = String.format(Locale.forLanguageTag("tr-TR"), "%.1f", x)

    OutlinedButton(onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()) {
        Text(if (expanded) "Saha kalibrasyonunu kapat" else
            "Saha tüketim / varış SoC kalibrasyonu")
    }
    if (!expanded) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Park hâlinde, yolculuktan sonra manuel gözlem gir. Model/telefon/ECU verisi DOĞRULANMADI.")
        Text("Model tüketim: " +
            (model?.let { format(it) + " kWh/100 km" } ?: "HESAPLANAMADI") +
            " • Plan varış SoC: ~%" + plan.arrivalSocPct + " (TAHMİN)")
        OutlinedTextField(value = consumptionText,
            onValueChange = { consumptionText = it.take(9) },
            label = { Text("Araç ekranında gördüğüm tüketim (kWh/100 km)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = arrivalSocText,
            onValueChange = { arrivalSocText = it.take(7) },
            label = { Text("Gerçek varış SoC (%) — manuel") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            val observedKwh = consumptionText.replace(',','.').toDoubleOrNull()
            val observedSoc = arrivalSocText.replace(',','.').toDoubleOrNull()
            suggestion = if (model != null && observedKwh != null && observedSoc != null &&
                request != null) runCatching {
                    ConsumptionCalibration.suggest(
                        model, observedKwh, plan.arrivalSocPct.toDouble(),
                        observedSoc, request.car.consumptionAdjust.value,
                    )
                }.getOrNull() else null
            feedback = if (suggestion == null)
                "Girdi geçersiz veya model yok. Tüketim 1–100 kWh/100 km, SoC 0–100 olmalı."
            else "Öneri hesaplandı — henüz kaydedilmedi; otomatik uygulanmaz."
        }, enabled = model != null) { Text("Tüketim düzeltme önerisini hesapla") }
        suggestion?.let { value ->
            Text("Önerilen tüketim düzeltmesi: " +
                format(value.suggestedConsumptionAdjust * 100.0) +
                "% (TAHMİN, otomatik uygulanmaz)")
            Text("Tüketim farkı: " + format(value.relativeConsumptionDifferencePercent) +
                "% • Varış SoC farkı: " + format(value.arrivalSocDifferencePoints) + " puan")
            Text(value.confidenceNote)
            OutlinedButton(onClick = {
                scope.launch {
                    journal.add(CalibrationJournalCodec.fromSuggestion(value))
                    feedback = "Koordinatsız gözlem defterine kaydedildi. Araç profili değişmedi."
                }
            }) { Text("Sadece doğrulama defterine kaydet") }
        }
        if (feedback.isNotBlank()) Text(feedback)
        Text("Doğrulama defteri (cihazda yalnız sayılar, en çok 10 kayıt)")
        if (records.isEmpty()) Text("Henüz gözlem yok — DOĞRULANMADI.")
        records.take(10).forEachIndexed { index, item ->
            Text(
                "#${index + 1} Model " + format(item.modelKwhPer100Km) +
                ", gerçek " + format(item.actualKwhPer100Km) + " kWh/100 km; " +
                "SoC " + format(item.plannedArrivalSoc) + "→" +
                format(item.actualArrivalSoc) + "%; öneri " +
                format(item.recommendedAdjustmentPercent) + "% • DOĞRULANMADI",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (records.isNotEmpty()) {
            OutlinedButton(onClick = {
                scope.launch {
                    journal.clear()
                    feedback = "Yalnız kalibrasyon defteri temizlendi; rota ve araç ayarları değişmedi."
                }
            }) { Text("Kalibrasyon defterini temizle") }
        }
        Text("Bu kayıtlar otomatik kalibrasyon, doğrulanmış PID veya menzil garantisi değildir. " +
            "Düzeltmeyi kullanmak istersen Plan parametrelerinde kendin gir.",
            style = MaterialTheme.typography.bodySmall)
    }
}
