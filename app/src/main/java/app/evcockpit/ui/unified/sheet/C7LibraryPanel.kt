package app.evcockpit.ui.unified.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.evcockpit.R
import app.evcockpit.navigation.SavedDestination
import app.evcockpit.navigation.PlaceField
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.*

@Composable
fun C7LibraryPanel(
    state: UnifiedUiState,
    library: C7LibraryViewModel,
    onChoose: (PlaceField, SavedDestination) -> Unit,
    onOpen: (C7SavedRoute) -> Unit,
) {
    val t = CockpitDesignTokens
    val addresses by library.entries.collectAsStateWithLifecycle()
    val routes by library.savedRoutes.collectAsStateWithLifecycle()
    val notice by library.notice.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<(() -> Unit)?>(null) }
    var naming by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    OutlinedButton(onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
        Text(stringResource(R.string.a0b_c7_library))
    }
    if (expanded) {
        Text(stringResource(R.string.a0b_c7_local_note), color = t.secondary)
        state.destination?.let { destination ->
            listOf("HOME" to R.string.a0b_c7_save_home,
                "WORK" to R.string.a0b_c7_save_work,
                "FAVORITE" to R.string.a0b_c7_save_favorite).forEach { (type, label) ->
                OutlinedButton(onClick = { library.saveAddress(type, destination) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                    Text(stringResource(label))
                }
            }
        }
        listOf("HOME" to R.string.a0b_c7_home, "WORK" to R.string.a0b_c7_work,
            "FAVORITE" to R.string.a0b_c7_favorites, "RECENT" to R.string.a0b_c7_recent)
            .forEach { (type, label) ->
                Text(stringResource(label), color = t.text, fontSize = t.titleText)
                val group = addresses.filter { it.type == type }
                if (group.isEmpty()) Text(stringResource(R.string.a0b_c7_empty), color = t.secondary)
                group.forEach { item ->
                    Text(item.label, color = t.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                        OutlinedButton(onClick = { onChoose(PlaceField.ORIGIN, item) },
                            modifier = Modifier.weight(1f).heightIn(min = t.target)) {
                            Text(stringResource(R.string.a0b_c7_as_origin))
                        }
                        OutlinedButton(onClick = { onChoose(PlaceField.DESTINATION, item) },
                            modifier = Modifier.weight(1f).heightIn(min = t.target)) {
                            Text(stringResource(R.string.a0b_c7_as_destination))
                        }
                        TextButton(onClick = { deleting = { library.removeAddress(item) } },
                            modifier = Modifier.heightIn(min = t.target)) {
                            Text(stringResource(R.string.a0b_c7_delete))
                        }
                    }
                }
            }
        if (addresses.any { it.type == "RECENT" }) {
            OutlinedButton(onClick = { deleting = { library.clearRecent() } }) {
                Text(stringResource(R.string.a0b_c7_clear_recent))
            }
        }
        Text(stringResource(R.string.a0b_c7_routes), color = t.text, fontSize = t.titleText)
        if (routes.isEmpty()) Text(stringResource(R.string.a0b_c7_empty), color = t.secondary)
        routes.forEach { item ->
            Text(item.title, color = t.text)
            Text(stringResource(R.string.a0b_c7_summary, item.distanceMeters / 1000.0,
                (item.durationSeconds / 60).toInt()), color = t.secondary)
            Text(java.time.Instant.ofEpochMilli(item.savedAtMs).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) +
                " · " + item.source, color = t.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                OutlinedButton(onClick = { onOpen(item) },
                    modifier = Modifier.weight(1f).heightIn(min = t.target)) {
                    Text(stringResource(R.string.a0b_c7_open))
                }
                TextButton(onClick = { deleting = { library.removeRoute(item) } },
                    modifier = Modifier.heightIn(min = t.target)) {
                    Text(stringResource(R.string.a0b_c7_delete))
                }
            }
        }
    }
    if (state.currentRoute != null && state.origin != null && state.destination != null) {
        OutlinedButton(onClick = {
            title = (state.origin.name + " → " + state.destination.name).take(180)
            naming = true
        }, modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
            Text(stringResource(R.string.a0b_c7_save_route))
        }
    }
    notice?.let { Text(stringResource(it), color = t.secondary) }
    if (naming) AlertDialog(
        onDismissRequest = { naming = false },
        title = { Text(stringResource(R.string.a0b_c7_route_title)) },
        text = { OutlinedTextField(value = title, onValueChange = { title = it.take(180) },
            singleLine = true, label = { Text(stringResource(R.string.a0b_c7_route_title)) }) },
        confirmButton = { TextButton(enabled = title.isNotBlank() && state.currentRoute != null,
            onClick = { library.saveRoute(state, title); naming = false }) {
            Text(stringResource(R.string.a0b_c7_save))
        } },
        dismissButton = { TextButton(onClick = { naming = false }) {
            Text(stringResource(R.string.a0b_c7_cancel))
        } },
    )
    if (deleting != null) AlertDialog(
        onDismissRequest = { deleting = null },
        text = { Text(stringResource(R.string.a0b_c7_delete_confirm)) },
        confirmButton = { TextButton(onClick = { deleting?.invoke(); deleting = null }) {
            Text(stringResource(R.string.a0b_c7_delete))
        } },
        dismissButton = { TextButton(onClick = { deleting = null }) {
            Text(stringResource(R.string.a0b_c7_cancel))
        } },
    )
}
