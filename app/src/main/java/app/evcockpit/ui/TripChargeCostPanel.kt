package app.evcockpit.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.planui.TripChargeCostViewModel
import app.evcockpit.planner.charging.TripChargeCost
import app.evcockpit.planner.charging.ManualChargeTariff
import app.evcockpit.planner.model.PlanRequest
import app.evcockpit.planner.model.PlanResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val OFFICIAL_PRICE_GUIDE =
    "https://www.epdk.gov.tr/Detay/Icerik/1-3428/enerji-donusumuserbest-erisim-platformu--sarj%40tr"

@Composable
fun TripChargeCostPanel(plan: PlanResult.Success, request: PlanRequest?) {
    val model: TripChargeCostViewModel = viewModel()
    val ui by model.state.collectAsState()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(plan, request) {
        model.setTrip(plan, request)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("ENERJİ & ŞARJ ÜCRETİ • TAHMİN",
                style = MaterialTheme.typography.titleLarge)
            Text("Yolda satın alınan şarj enerjisi tahmini; başlangıçta bataryada " +
                "bulunan elektriğin bedeli dahil değildir.",
                style = MaterialTheme.typography.bodySmall)
            val expense = ui.estimate
            if (expense == null) {
                Text("Önce gerçek rota ve şarj planını hesapla.")
            } else {
                Text("Rota tüketimi (model): " +
                    (expense.routeEnergyKwh?.let {
                        String.format(Locale.forLanguageTag("tr-TR"), "~%.1f kWh", it)
                    } ?: "-- kWh • hesaplanamadı"))
                Text("Yolda bataryaya eklenecek enerji: " +
                    String.format(Locale.forLanguageTag("tr-TR"),
                        "~%.1f kWh", expense.totalBatteryAddedKwh))
                Text("Tahmini yolda ödenecek toplam: " +
                    TripChargeCost.formatTl(expense.totalCostTl),
                    style = MaterialTheme.typography.titleMedium)
                if (expense.unknownPriceStops > 0) Text(
                    "Fiyatı bilinmeyen " + expense.unknownPriceStops +
                        " durak var. Eksik fiyatlar 0 TL sayılmadı.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
                if (expense.stops.isEmpty()) Text(
                    "Planda yol üstünde şarj gerekmiyor: yolda istasyon ödemesi 0 TL; " +
                        "başlangıç enerjisi bedeli hesaplanmadı.",
                    style = MaterialTheme.typography.bodySmall)
                expense.stops.forEachIndexed { index, stop ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Durak " + (index + 1) + " • " + stop.chargerName,
                            style = MaterialTheme.typography.labelLarge)
                        Text("Operatör: " + stop.operatorName +
                            " • batarya ~" + String.format(Locale.ROOT,
                                "%.1f kWh", stop.batteryAddedKwh) +
                            " • doluluk bilinmiyor")
                        Text("Tahmini durak bedeli: " + TripChargeCost.formatTl(stop.costTl))
                        val tariff = stop.tariff
                        if (tariff == null) {
                            Text("Bu işletmeci için doğrulanmış TL/kWh fiyatı yok.",
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                "Fiyat: " + String.format(Locale.forLanguageTag("tr-TR"),
                                    "%.2f TL/kWh", tariff.priceTlPerKwh) +
                                    " • MANUEL • " + tariffDate(tariff) +
                                    (if (TripChargeCost.isStale(tariff,
                                        System.currentTimeMillis())) " • ESKİ" else "") +
                                    " • DOĞRULANMADI",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Fiyat girişini gizle" else
                    "Operatör fiyatlarını gir • TL/kWh")
            }
            if (expanded) {
                val operators = plan.stops.map { it.charger.operator.trim() }.distinct()
                operators.forEach { operator ->
                    val tariff = ui.tariffs[TripChargeCost.normalizedOperator(operator)]
                    ManualTariffEntry(
                        operator = operator,
                        tariff = tariff,
                        onSave = { model.saveManualTariff(operator, it) },
                        onRemove = { model.clearTariff(operator) },
                    )
                }
                Text("Fiyatları operatörün kendi sayfasından veya EPDK Şarj@TR'den " +
                    "kontrol edip kendin gir. Uygulama fiyat toplamaz ve otomatik güncellemez.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(OFFICIAL_PRICE_GUIDE)))
                    }
                }) { Text("EPDK Şarj@TR resmî fiyat rehberini aç") }
            }
            if (ui.message.isNotBlank()) Text(ui.message,
                style = MaterialTheme.typography.bodySmall)
            Text("Hesap: kullanılabilir kWh × (durak çıkış SoC − giriş SoC)/100 × " +
                "MANUEL TL/kWh. Kayıp, işgaliye, zaman tarifesi ve canlı doluluk " +
                "bu modelde yok; ödeme öncesi istasyonda doğrula.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun tariffDate(tariff: ManualChargeTariff): String =
    Instant.ofEpochMilli(tariff.observedAtEpochMs)
        .atZone(ZoneId.of("Europe/Istanbul"))
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("tr-TR")))

@Composable
private fun ManualTariffEntry(
    operator: String,
    tariff: ManualChargeTariff?,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val label = operator.ifBlank { "Operatör bilinmiyor" }
    var typed by remember(operator) { mutableStateOf("") }
    LaunchedEffect(operator, tariff?.priceTlPerKwh) {
        if (tariff != null) typed = tariff.priceTlPerKwh.toString()
    }
    Text(label, style = MaterialTheme.typography.labelMedium)
    OutlinedTextField(
        value = typed,
        onValueChange = { typed = it.take(12) },
        singleLine = true,
        label = { Text("MANUEL TL/kWh") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onSave(typed) },
            enabled = operator.isNotBlank()) { Text("Fiyatı kaydet") }
        if (tariff != null) OutlinedButton(onClick = onRemove) { Text("Sil") }
    }
}
