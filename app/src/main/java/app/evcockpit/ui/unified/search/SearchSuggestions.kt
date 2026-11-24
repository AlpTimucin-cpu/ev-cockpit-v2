package app.evcockpit.ui.unified.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.Place

/**
 * Yalnız gerçek kaynaktan gelen öneriler; C3 boşken hiçbir sahte adres yok.
 * C4'te Photon/il-ilçe kaynakları bağlanacak.
 */
@Composable
fun SearchSuggestions(
    options: List<Place>,
    onSelect: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    if (options.isEmpty()) {
        Text(stringResource(R.string.a0b_unified_no_recent),
            modifier = modifier, color = t.secondary, fontSize = t.bodyText)
    } else {
        LazyColumn(modifier = modifier) {
            items(options, key = { it.id }) { option ->
                OutlinedButton(onClick = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = t.target),
                    shape = t.cornerSmall) {
                    Text(option.name, fontSize = t.bodyText)
                }
            }
        }
    }
}
