package app.evcockpit.charging.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.charging.ocm.OcmStation
import app.evcockpit.charging.tariffs.*
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.ui.OnlineMap
import java.util.Locale

private val ink = Color(0xFF0B1120)
private val panel = Color(0xFF141E30)
private val soft = Color(0xFF94A8BD)
private val highlight = Color(0xFF31D9B5)
private val outline = Color(0xFF2B3B50)

private enum class StationFilter(val label: String) {
    ALL("Tümü"), CCS2("CCS2"), DC("DC hızlı"), AC("AC")
}

/**
 * Harita odaklı şarj keşfi: tablet yatayda harita + istasyon listesi,
 * telefonda harita üstte, filtreler ve kartlar altta görünür.
 * Mola arayüzüne ya da Mola APK'sına müdahale edilmez.
 */
@Composable
fun ChargingExplorerScreen(
    route: NavigationRoute?,
    gps: GpsSnapshot,
    onSelectDestination: (GeoPoint) -> Unit,
    onNavigateViaStops: (List<GeoPoint>, GeoPoint) -> Unit,
    vm: ChargingExplorerViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var filter by remember { mutableStateOf(StationFilter.ALL) }
    var keyInput by remember { mutableStateOf("") }
    var openKey by remember { mutableStateOf(false) }
    var openTariffs by remember { mutableStateOf(false) }
    var activeOperator by remember { mutableStateOf("zes") }
    var kind by remember { mutableStateOf(ChargingKind.DC) }
    var priceInput by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    val filtered = remember(state.stations, filter) {
        state.stations.filter { station ->
            when (filter) {
                StationFilter.ALL -> true
                StationFilter.CCS2 -> station.hasCcs2
                StationFilter.DC -> station.hasDc || (station.powerKw ?: 0.0) >= 43.0
                StationFilter.AC -> station.hasAc
            }
        }
    }
    val chosen = state.selected
    val official = state.catalog.operators.firstOrNull { it.id == activeOperator }
    val manual = state.manual[activeOperator]

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.horizontalGradient(listOf(Color(0xFF173F50), panel, ink)),
                    RoundedCornerShape(19.dp),
                ).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("ŞARJ KEŞFİ", color = highlight,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Rotandaki şarj noktaları", color = Color.White,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("OCM envanteri · şarj gücü, soket ve operatör · doluluk canlı değil",
                    color = soft, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChargingMetric("${state.stations.size}", "BULUNAN", Modifier.weight(1f))
                    ChargingMetric("${state.stations.count { it.hasCcs2 }}",
                        "CCS2", Modifier.weight(1f))
                    ChargingMetric("${state.stations.count { (it.powerKw ?: 0.0) >= 150.0 }}",
                        "150+ kW", Modifier.weight(1f))
                }
            }

            if (!state.hasOcmKey || openKey) {
                Surface(color = panel, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Open Charge Map anahtarı", color = Color.White,
                            style = MaterialTheme.typography.titleSmall)
                        Text("Ücretsiz API anahtarını profilindeki My Apps alanından al.",
                            color = soft, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = keyInput, onValueChange = { keyInput = it.take(256) },
                            label = { Text("OCM API anahtarı") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.saveKey(keyInput); keyInput = ""; openKey = false },
                                enabled = keyInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = highlight,
                                    contentColor = ink),
                            ) { Text("Şifreli kaydet") }
                            OutlinedButton(onClick = {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://openchargemap.org/profile"))) }
                            }) { Text("OCM hesabım") }
                        }
                    }
                }
            } else {
                TextButton(onClick = { openKey = true }) {
                    Text("OCM anahtarını değiştir", color = soft)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.search(route) },
                    enabled = !state.loading && state.hasOcmKey && route != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = highlight, contentColor = ink),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(if (state.loading) "Taranıyor…" else "Rota istasyonlarını tara",
                        fontWeight = FontWeight.Bold)
                }
                if (state.loading) {
                    OutlinedButton(onClick = vm::cancel) { Text("İptal") }
                }
            }
            if (state.loading) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(), color = highlight)
            Text(state.stationMessage, color = if (state.incomplete) Color(0xFFFFCF85) else soft,
                style = MaterialTheme.typography.bodySmall)
            if (route == null) Text("Önce Rota ekranından güzergâh oluştur.",
                color = soft, style = MaterialTheme.typography.bodySmall)

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StationFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = highlight,
                            selectedLabelColor = ink,
                            labelColor = Color.White,
                        ),
                    )
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 750.dp
                val map: @Composable (Modifier) -> Unit = { modifier ->
                    OnlineMap(
                        modifier = modifier,
                        route = route,
                        destination = route?.geometry?.lastOrNull(),
                        gps = gps,
                        onSelectDestination = onSelectDestination,
                        ocmStations = filtered,
                        onOcmStationSelected = vm::select,
                        focusedOcmStation = chosen,
                    )
                }
                val entries: @Composable () -> Unit = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(470.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (filtered.isEmpty()) {
                            item {
                                Surface(color = panel, shape = RoundedCornerShape(16.dp)) {
                                    Text("Henüz istasyon yok. Rota seçip OCM anahtarıyla tara.",
                                        color = soft, modifier = Modifier.padding(15.dp))
                                }
                            }
                        }
                        items(filtered, key = { it.id }) { station ->
                            StationCard(station, state.selected?.id == station.id,
                                state.catalog, state.manual,
                                onPick = { vm.select(station) },
                                onAdd = { vm.addStop(station) })
                        }
                    }
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        map(Modifier.weight(1.8f).height(470.dp))
                        Box(Modifier.weight(1f)) { entries() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        map(Modifier.fillMaxWidth().height(340.dp))
                        Text("BULUNAN İSTASYONLAR • ${filtered.size}",
                            color = soft, style = MaterialTheme.typography.labelLarge)
                        entries()
                    }
                }
                if (filtered.size > 250) {
                    Text("Performans için haritada ilk 250 nokta işaretli; " +
                        "istasyonların tamamı kaydırılabilir listede.",
                        color = soft, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (chosen != null) {
                StationDetails(chosen, state.catalog, state.manual,
                    onAdd = { vm.addStop(chosen) })
            }
            if (state.addedStops.isNotEmpty()) {
                Surface(color = panel, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SEÇİLEN ŞARJ DURAKLARI • ${state.addedStops.size}",
                            color = highlight, fontWeight = FontWeight.Bold)
                        state.addedStops.forEach { station ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(station.name, Modifier.weight(1f),
                                    color = Color.White, maxLines = 1)
                                TextButton(onClick = { vm.removeStop(station.id) }) {
                                    Text("Kaldır", color = Color(0xFFFFA6A6))
                                }
                            }
                        }
                        Button(
                            enabled = route?.geometry?.lastOrNull() != null,
                            onClick = {
                                val destination = route?.geometry?.lastOrNull()
                                if (destination != null) onNavigateViaStops(
                                    state.addedStops.map { it.point }, destination)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = highlight, contentColor = ink),
                        ) { Text("Duraklarla yeni rota iste") }
                        Text("Ara duraklı rota yeniden hesaplama mevcut ORS bağlantısına bağlıdır; " +
                            "GPS ve HeiGIT anahtarı gerekebilir. Duraklar otomatik şarj planı değildir.",
                            color = soft, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedButton(onClick = { openTariffs = !openTariffs },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (openTariffs) "Tarife yönetimini kapat" else
                    "AC / DC fiyat kataloğunu ve manuel tarifeleri aç",
                    color = Color.White)
            }
            if (openTariffs) {
                Column(
                    Modifier.fillMaxWidth().background(panel, RoundedCornerShape(16.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Operatör tarifeleri", color = Color.White,
                        style = MaterialTheme.typography.titleMedium)
                    Text(state.tariffMessage, color = soft,
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.catalog.operators.forEach { operator ->
                            FilterChip(
                                selected = activeOperator == operator.id,
                                onClick = { activeOperator = operator.id },
                                label = { Text(operator.operator) },
                            )
                        }
                    }
                    if (official != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = kind == ChargingKind.AC,
                                onClick = { kind = ChargingKind.AC },
                                label = { Text("AC") })
                            FilterChip(selected = kind == ChargingKind.DC,
                                onClick = { kind = ChargingKind.DC },
                                label = { Text("DC") })
                        }
                        val price = TariffResolver.resolve(
                            state.catalog, state.manual, official.operator, kind)
                        Text("Fiyat: " + priceLabel(price) +
                            " • ${price.source} • tarih ${price.updatedAt ?: "yok"}",
                            color = if (price.priceTlPerKwh == null) soft else highlight)
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it.take(9) },
                            label = { Text("Kendi gözlemim — TL/kWh") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val input = priceInput.replace(',', '.').toDoubleOrNull()
                                if (input != null) {
                                    vm.savePrice(official.id, kind, input)
                                    priceInput = ""
                                }
                            }, enabled = priceInput.replace(',', '.').toDoubleOrNull()
                                ?.let { it.isFinite() && it in 0.01..150.0 } == true,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = highlight, contentColor = ink)) {
                                Text("Manuel fiyatı kaydet")
                            }
                            if (manual != null) {
                                OutlinedButton(onClick = { vm.clearPrice(official.id) }) {
                                    Text("Manueli sıfırla")
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { vm.refreshTariffs(true) }) {
                        Text("GitHub tarifelerini yenile")
                    }
                    Text("Tarife dosyası GitHub raw URL'sinden alınır; 24 saat önbelleğe " +
                        "kaydedilir. Resmî sayfada tek tutar bulunabilen bazı operatör fiyatları " +
                        "kaynak ve tarih ile gösterilir; diğerleri bilinmiyor (null). " +
                        "Lokasyon, kampanya ve soket bazında ödeme öncesi doğrula.",
                        color = soft, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("© Open Charge Map • © OpenStreetMap katkıcıları • Fiyatlar canlı değildir. " +
                "İstasyon listesi eksik/eski olabilir; ödeme öncesi operatör uygulamasında doğrula.",
                color = soft, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChargingMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text(label, color = soft, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StationCard(
    station: OcmStation,
    active: Boolean,
    catalog: TariffCatalog,
    manual: Map<String, ManualPrice>,
    onPick: () -> Unit,
    onAdd: () -> Unit,
) {
    val price = TariffResolver.resolve(catalog, manual, station.operator,
        stationKind(station))
    val tone = when {
        station.powerKw == null -> soft
        station.powerKw >= 150 -> Color(0xFFC48EFF)
        station.hasDc || station.powerKw >= 43 -> highlight
        else -> Color(0xFF60ABFF)
    }
    Surface(
        onClick = onPick,
        shape = RoundedCornerShape(16.dp),
        color = if (active) Color(0xFF203B46) else panel,
        modifier = Modifier.fillMaxWidth().border(
            width = if (active) 2.dp else 1.dp,
            color = if (active) highlight else outline,
            shape = RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(station.operator, color = soft,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(station.powerKw?.let { "${it.toInt()} kW" } ?: "— kW",
                    color = tone, fontWeight = FontWeight.Bold)
            }
            Text(station.powerGroup + "   ·   " +
                (if (station.hasCcs2) "CCS2" else
                    station.connectors.joinToString(", ").ifBlank { "Soket bilinmiyor" }),
                color = tone, style = MaterialTheme.typography.labelSmall)
            Text("${String.format(Locale.US, "%.1f", station.alongKm)} km rota boyunca" +
                "  ·  ${String.format(Locale.US, "%.1f", station.awayKm)} km kuş uçuşu sapma",
                color = soft, style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(priceLabel(price), color = if (price.priceTlPerKwh == null ||
                    price.stale) soft else Color.White, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onAdd) { Text("+ Durak ekle", color = highlight) }
            }
        }
    }
}

@Composable
private fun StationDetails(
    station: OcmStation,
    catalog: TariffCatalog,
    manual: Map<String, ManualPrice>,
    onAdd: () -> Unit,
) {
    val price = TariffResolver.resolve(catalog, manual, station.operator,
        stationKind(station))
    Surface(color = panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("SEÇİLEN İSTASYON", color = highlight,
                style = MaterialTheme.typography.labelMedium)
            Text(station.name, color = Color.White,
                style = MaterialTheme.typography.titleLarge)
            Text("Operatör: ${station.operator}", color = soft)
            Text("Bağlantılar: ${station.connectors.joinToString(", ").ifBlank { "bilinmiyor" }}",
                color = Color.White)
            Text("Bildirilen güç: ${station.powerKw?.let { "$it kW" } ?: "bilinmiyor"}" +
                " · erişim: ${station.access}", color = Color.White)
            Text("Envanter durum etiketi: ${station.reportedStatus ?: "bilinmiyor"}" +
                " (CANLI DOLULUK DEĞİL)", color = soft)
            Text("Fiyat: " + priceLabel(price) +
                " · ${price.source} · ${price.updatedAt ?: "tarih yok"}",
                color = Color.White)
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(
                containerColor = highlight, contentColor = ink)) {
                Text("Rota durağına ekle")
            }
        }
    }
}

private fun stationKind(station: OcmStation): ChargingKind = when {
    station.hasDc -> ChargingKind.DC
    station.hasAc -> ChargingKind.AC
    else -> ChargingKind.UNKNOWN
}

private fun priceLabel(price: DisplayTariff): String =
    price.priceTlPerKwh?.let {
        val label = String.format(Locale.forLanguageTag("tr-TR"), "%.2f TL/kWh", it)
        label + if (price.stale) " • ESKİ / KONTROL ET" else " • KAYNAĞI KONTROL ET"
    } ?: "Fiyat bilinmiyor"
