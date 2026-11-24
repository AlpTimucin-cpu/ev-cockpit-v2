package app.evcockpit.ui.unified.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens

/** C3 girdi kabuğu. Arama ağı yalnız C4'te gerçek SearchRepository ile bağlanır. */
@Composable
fun SearchBar(
    query: String,
    onQuery: (String) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text(stringResource(R.string.a0b_unified_search_where)) },
        placeholder = {
            Text(stringResource(R.string.a0b_unified_search_hint),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        shape = t.searchBarCorner,
        singleLine = true,
        modifier = modifier.heightIn(min = t.searchBarHeight)
            .onFocusChanged { if (it.isFocused) onFocus() },
    )
}
