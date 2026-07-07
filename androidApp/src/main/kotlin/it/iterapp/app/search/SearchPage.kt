package it.iterapp.app.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.common.IconListRow
import it.iterapp.app.common.SheetSearchField
import it.iterapp.app.common.placeIcon
import it.iterapp.core.model.SearchResult

/**
 * Full search page: pill text field with auto-focus, debounced results.
 * Also reused by the planning endpoint picker via [onPick].
 */
@Composable
fun SearchPage(
  viewModel: SearchViewModel,
  onBack: () -> Unit,
  onPick: (SearchResult) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = stringResource(R.string.search_placeholder),
) {
  val query by viewModel.query.collectAsStateWithLifecycle()
  val results by viewModel.results.collectAsStateWithLifecycle()
  val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  Column(modifier.fillMaxSize()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
    ) {
      IconButton(onClick = onBack) {
        Icon(
          Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.action_back),
        )
      }
      SheetSearchField(
        query = query,
        onQueryChange = viewModel::onQueryChange,
        placeholder = placeholder,
        focusRequester = focusRequester,
        isLoading = isSearching,
        modifier = Modifier.weight(1f),
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp)
        .imePadding(),
      contentPadding = PaddingValues(vertical = 8.dp),
    ) {
      items(results, key = { it.id }) { result ->
        IconListRow(
          icon = placeIcon(result),
          title = result.name,
          subtitle = result.detail,
          onClick = { onPick(result) },
        )
      }
      if (results.isEmpty() && query.length >= 2 && !isSearching) {
        item {
          Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
          )
        }
      }
    }
  }
}
