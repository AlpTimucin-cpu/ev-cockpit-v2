package app.evcockpit.ui.unified.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.C6Progress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NavigationPanel(
    progress: C6Progress,
    voiceEnabled: Boolean,
    voiceStatus: String?,
    onVoice: () -> Unit,
    onStop: () -> Unit,
    onRecenter: () -> Unit,
    onReroute: () -> Unit,
    canReroute: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    Surface(modifier = modifier, color = t.panel, shape = t.cornerSmall) {
        Column(Modifier.padding(t.spaceM), verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
            val distance = progress.remainingMeters
            val maneuver = progress.instruction
            val maneuverMeters = progress.maneuverMeters
            val label = when {
                progress.vehicle == null -> stringResource(R.string.a0b_c6_wait)
                progress.arrived -> stringResource(R.string.a0b_c6_arrived)
                progress.offRoute -> stringResource(R.string.a0b_c6_off_route)
                distance == null -> stringResource(R.string.a0b_c6_unmatched)
                maneuver != null && maneuverMeters != null ->
                    stringResource(R.string.a0b_c6_next, maneuver, maneuverMeters)
                else -> stringResource(R.string.a0b_c6_no_step)
            }
            Text(label, color = if (progress.offRoute) t.caution else t.text, fontSize = t.titleText)
            if (distance != null) Text(stringResource(R.string.a0b_c6_distance, distance / 1000.0))
            val eta = progress.remainingSeconds?.takeIf { it.isFinite() && it in 0.0..604800.0 }
            if (eta != null) {
                val time = Instant.now().plusSeconds(eta.toLong()).atZone(ZoneId.of("Europe/Istanbul"))
                    .format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))
                Text(stringResource(R.string.a0b_c6_eta, time), color = t.secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                OutlinedButton(onClick = onRecenter, enabled = progress.vehicle != null,
                    modifier = Modifier.weight(1f).heightIn(min = t.target)) {
                    Text(stringResource(R.string.a0b_c6_recenter))
                }
                OutlinedButton(onClick = onVoice, modifier = Modifier.weight(1f).heightIn(min = t.target)) {
                    Text(stringResource(if (voiceEnabled) R.string.a0b_c6_voice_off else R.string.a0b_c6_voice_on))
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f).heightIn(min = t.target)) {
                    Text(stringResource(R.string.a0b_c6_stop))
                }
            }
            if (voiceEnabled) voiceStatus?.let { Text(it, color = t.secondary, fontSize = t.labelText) }
            if (progress.offRoute) Button(onClick = onReroute, enabled = canReroute,
                modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                Text(stringResource(R.string.a0b_c6_reroute))
            }
        }
    }
}
