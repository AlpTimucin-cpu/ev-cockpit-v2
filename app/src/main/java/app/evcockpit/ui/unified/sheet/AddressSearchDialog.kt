package app.evcockpit.ui.unified.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.evcockpit.R
import app.evcockpit.navigation.AutocompletePolicy
import app.evcockpit.navigation.PhotonPlace
import app.evcockpit.ui.finalhmi.CockpitDesignTokens

/** Dedicated viewport: keyboard and sheet dragging cannot obscure suggestion list. */
@Composable
fun AddressSearchDialog(
    title: String, query: String, options: List<PhotonPlace>, status: String,
    loading: Boolean, onQuery: (String) -> Unit, onRetry: () -> Unit,
    onChoose: (PhotonPlace) -> Unit, onDismiss: () -> Unit,
    onMapPick: () -> Unit = {},
) {
    val t = CockpitDesignTokens
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(), color = t.panel) {
            Column(Modifier.fillMaxSize().padding(t.spaceM),
                verticalArrangement = Arrangement.spacedBy(t.spaceXS)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, color = t.text, fontSize = t.titleText)
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.a0b_c7_cancel)) }
                }
                OutlinedTextField(value = query, onValueChange = onQuery,
                    label = { Text(stringResource(R.string.a0b_search_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(requester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        if (AutocompletePolicy.eligible(query) && !loading) onRetry()
                    }),
                    trailingIcon = {
                        if (query.isNotEmpty()) TextButton(onClick = { onQuery("") }) {
                            Text(stringResource(R.string.a0b_search_clear))
                        }
                    })
                Text(status, color = t.secondary, fontSize = t.labelText)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (!loading && AutocompletePolicy.eligible(query)) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.a0b_search_retry)) }
                }
                OutlinedButton(onClick = { keyboard?.hide(); onMapPick() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.a0b_map_pick_action))
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(t.spaceXS)) {
                    items(options) { option ->
                        OutlinedButton(onClick = { keyboard?.hide(); onChoose(option) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
                            Text(option.label, modifier = Modifier.fillMaxWidth(), color = t.text)
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { requester.requestFocus(); keyboard?.show() }
    }
}
