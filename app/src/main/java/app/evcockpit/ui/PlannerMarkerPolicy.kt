package app.evcockpit.ui

import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerConfidence

object PlannerMarkerPolicy {
    fun label(c:ChargerCandidate):String = when(c.confidence) {
        ChargerConfidence.OSM_TAGGED_CCS2 ->
            "CCS2 etiketli • "+c.name+" • cihaz DOĞRULANMADI"
        ChargerConfidence.PROBABLE_CCS2 ->
            "OLASI CCS2 • "+c.name+" • DOĞRULANMADI"
        ChargerConfidence.UNKNOWN ->
            "BAĞLANTI BİLİNMİYOR • "+c.name+" • DOĞRULANMADI"
    }
    // Green indicates OSM-tag confidence, NEVER on-site operation.
    fun color(c:ChargerCandidate):Int = when(c.confidence) {
        ChargerConfidence.OSM_TAGGED_CCS2->0xFF16A085.toInt()
        ChargerConfidence.PROBABLE_CCS2->0xFFE5A22B.toInt()
        ChargerConfidence.UNKNOWN->0xFF87929B.toInt()
    }
}
