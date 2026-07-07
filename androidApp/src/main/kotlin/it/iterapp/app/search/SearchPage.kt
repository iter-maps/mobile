package it.iterapp.app.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.common.IconListRow
import it.iterapp.app.common.SheetSearchField
import it.iterapp.app.common.formatDistance
import it.iterapp.app.common.placeIcon
import it.iterapp.app.sheet.SheetSectionHeader
import it.iterapp.app.sheet.SheetStatusMessage
import it.iterapp.core.model.SearchResult

/**
 * Full search page: pill text field with auto-focus, debounced results, and
 * the session's recent places while the field is empty. Also reused by the
 * planning endpoint picker via [onPick]; the picker passes [onMyLocation] to
 * pin a "My location" row above the results.
 */
@Composable
fun SearchPage(
  viewModel: SearchViewModel,
  onBack: () -> Unit,
  onPick: (SearchResult) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = stringResource(R.string.search_placeholder),
  recentPlaces: List<SearchResult> = emptyList(),
  onMyLocation: (() -> Unit)? = null,
) {
  val query by viewModel.query.collectAsStateWithLifecycle()
  val results by viewModel.results.collectAsStateWithLifecycle()
  val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
  val error by viewModel.error.collectAsStateWithLifecycle()
  val focusRequester = remember { FocusRequester() }
  val listState = rememberLazyListState()
  val keyboard = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  // Scrolling the results means reading them — get the keyboard out of the
  // way (it hides the bottom ~40% of the list).
  LaunchedEffect(listState) {
    snapshotFlow { listState.isScrollInProgress }.collect { if (it) keyboard?.hide() }
  }

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

    // Trimmed-length gate shared by every branch below, so a cleared field
    // can't flash the previous query's list or empty state.
    val showResults = query.trim().length >= 2

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp)
        .imePadding(),
      contentPadding = PaddingValues(vertical = 8.dp),
    ) {
      if (query.isBlank()) {
        if (onMyLocation != null) {
          item {
            IconListRow(
              icon = Icons.Rounded.MyLocation,
              title = stringResource(R.string.planning_my_location),
              onClick = onMyLocation,
              iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
              iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
        if (recentPlaces.isNotEmpty()) {
          item {
            SheetSectionHeader(
              stringResource(R.string.home_section_recents),
              Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
            )
          }
          items(recentPlaces, key = { "recent-${it.id}" }) { place ->
            IconListRow(
              icon = placeIcon(place),
              title = place.name,
              subtitle = place.detail,
              onClick = { onPick(place) },
              trailing = {
                Icon(
                  Icons.Rounded.History,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
              },
            )
          }
        } else if (onMyLocation == null) {
          item {
            Text(
              text = stringResource(R.string.search_prompt),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            )
          }
        }
      }
      if (showResults) {
        items(results, key = { it.id }) { result ->
          IconListRow(
            icon = placeIcon(result),
            title = result.name,
            subtitle = result.detail,
            onClick = { onPick(result) },
            trailing = result.distanceM?.let { distance ->
              {
                Text(
                  text = formatDistance(distance),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  modifier = Modifier.padding(start = 8.dp),
                )
              }
            },
          )
        }
        when {
          error && !isSearching -> item {
            SheetStatusMessage(
              icon = Icons.Rounded.CloudOff,
              message = stringResource(R.string.error_network),
            )
          }
          results.isEmpty() && !isSearching -> item {
            SheetStatusMessage(
              icon = Icons.Rounded.SearchOff,
              message = stringResource(R.string.search_no_results),
              hint = stringResource(R.string.search_no_results_hint),
            )
          }
        }
      }
    }
  }
}
