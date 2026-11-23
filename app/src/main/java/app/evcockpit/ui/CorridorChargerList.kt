package app.evcockpit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerConfidence
import app.evcockpit.planner.charging.ChargerEvidence
import java.util.Locale

/**
 * Search-result coverage only. Overpass may be stale/incomplete; there is no
 * nationwide station count or real-time availability in this UI.
 */
object CorridorStationPolicy {
    const val PAGE_SIZE = 30
    const val MAX_MAP_MARKERS = 100

    fun sorted(items: List<ChargerCandidate>): List<ChargerCandidate> =
        items.sortedWith(
            compareBy<ChargerCandidate> { it.distanceAlongMeters }
                .thenBy { it.name.lowercase(Locale.forLanguageTag("tr-TR")) }
                .thenBy { it.id }
        )

    fun slice(items: List<ChargerCandidate>, count: Int): List<ChargerCandidate> =
        sorted(items).take(count.coerceIn(0, items.size))

    fun mapMarkers(items: List<ChargerCandidate>): List<ChargerCandidate> =
        slice(items, MAX_MAP_MARKERS)

    fun confidenceLabel(item: ChargerCandidate): String = when (item.confidence) {
        ChargerConfidence.OSM_TAGGED_CCS2 -> "OSM'de CCS2 etiketi var (cihaz doğrulanmadı)"
        ChargerConfidence.PROBABLE_CCS2 -> "CCS2 olası • DOĞRULANMADI"
        ChargerConfidence.UNKNOWN -> "Bağlantı tipi bilinmiyor"
    }
}

@Composable
fun CorridorChargerList(
    allCandidates: List<ChargerCandidate>,
    stationStatus: String,
    selectedStationIds: Set<String>,
    onFocusStation: (ChargerCandidate) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var visible by remember(allCandidates) {
        mutableIntStateOf(CorridorStationPolicy.PAGE_SIZE)
    }
    val sorted = remember(allCandidates) { CorridorStationPolicy.sorted(allCandidates) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ROTA ÜZERİNDEKİ ŞARJ ADAYLARI",
                style = MaterialTheme.typography.titleMedium)
            Text("OSM/Overpass yanıtı: " + allCandidates.size +
                " aday istasyon • TAHMİN / DOĞRULANMADI")
            Text(stationStatus, style = MaterialTheme.typography.bodySmall)
            Text("Bu sayı Türkiye'deki bütün istasyonlar değildir. Eksik OSM kayıtları, " +
                "kapalı soketler, güncel kW ve fiyatlar bilinmeyebilir. " +
                "Planlanan durak sayısı: " + selectedStationIds.size,
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(if (expanded) "Aday istasyon listesini gizle"
                    else "Harita koridorundaki tüm dönen adayları incele")
            }
            if (expanded) {
                if (sorted.isEmpty()) {
                    Text("Güzergâh araması henüz sonuç vermedi. İnternet, " +
                        "Overpass kota ve koridor filtresini kontrol et.")
                }
                sorted.take(visible).forEachIndexed { index, station ->
                    var detail by remember(station.id) { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text((index + 1).toString() + ". " + station.name +
                            if (station.id in selectedStationIds) " • PLANLANAN DURAK"
                            else "", style = MaterialTheme.typography.labelLarge)
                        Text("Operatör: " + station.operator +
                            " • ~" + station.powerKw.value.toInt() + " kW (TAHMİN)" +
                            " • " + CorridorStationPolicy.confidenceLabel(station),
                            style = MaterialTheme.typography.bodySmall)
                        Text("Güzergâh üzerindeki tahmini konum: ~" +
                            String.format(Locale.forLanguageTag("tr-TR"),
                                "%.1f km", station.distanceAlongMeters / 1000.0) +
                            " • doluluk bilinmiyor • fiyat DOĞRULANMADI",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onFocusStation(station) }) {
                                Text("Haritada göster")
                            }
                            OutlinedButton(onClick = { detail = !detail }) {
                                Text(if (detail) "Detayı kapat" else "Veri kaynağı")
                            }
                        }
                        if (detail) ChargerEvidence.details(station).forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (sorted.size > visible) OutlinedButton(onClick = {
                    visible = (visible + CorridorStationPolicy.PAGE_SIZE)
                        .coerceAtMost(sorted.size)
                }) {
                    Text("30 aday daha göster (" + visible + "/" + sorted.size + ")")
                }
            }
        }
    }
}
