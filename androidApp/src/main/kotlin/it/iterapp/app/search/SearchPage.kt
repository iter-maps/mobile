package it.iterapp.app.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
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
      modifier = Modifier.padding(start = 4.dp, end = 16.dp),
    ) {
      IconButton(onClick = onBack) {
        Icon(
          Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.action_back),
        )
      }
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
          .weight(1f)
          .height(52.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
          Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.size(10.dp))
          Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
              Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            BasicTextField(
              value = query,
              onValueChange = viewModel::onQueryChange,
              singleLine = true,
              textStyle = TextStyle.Default.merge(
                MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.onSurface,
                ),
              ),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            )
          }
          when {
            isSearching -> CircularProgressIndicator(
              strokeWidth = 2.dp,
              modifier = Modifier.size(20.dp),
            )
            query.isNotEmpty() -> IconButton(
              onClick = { viewModel.onQueryChange("") },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_clear),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp)
        .imePadding(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
      items(results, key = { it.id }) { result ->
        SearchResultRow(result = result, onClick = { onPick(result) })
      }
      if (results.isEmpty() && query.length >= 2 && !isSearching) {
        item {
          Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    color = androidx.compose.ui.graphics.Color.Transparent,
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 10.dp),
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = iconFor(result),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(
        Modifier
          .weight(1f)
          .padding(start = 14.dp),
      ) {
        Text(
          text = result.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        result.detail?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

// Train stations first: stationId-backed results must keep the Train icon,
// which signals the live-boards entry point.
private fun iconFor(r: SearchResult): ImageVector {
  if (r.isTrainStation) return Icons.Rounded.Train
  val k = r.osmKey?.lowercase()?.trim()
  val v = r.osmValue?.lowercase()?.trim()
  return when {
    k == "highway" && v == "bus_stop" -> Icons.Rounded.DirectionsBus
    k == "amenity" && v == "bus_station" -> Icons.Rounded.DirectionsBus
    k == "railway" && v == "tram_stop" -> Icons.Rounded.Tram
    k == "railway" -> Icons.Rounded.Train
    k == "station" -> when (v) {
      "subway" -> Icons.Rounded.Subway
      "light_rail", "tram" -> Icons.Rounded.Tram
      else -> Icons.Rounded.Train
    }
    k == "public_transport" -> Icons.Rounded.Train
    k == "highway" -> Icons.Rounded.Route
    k == "place" -> Icons.Rounded.LocationCity
    k == "shop" -> Icons.Rounded.Storefront
    k == "amenity" && v in setOf("bar", "pub", "biergarten", "nightclub") -> Icons.Rounded.LocalBar
    k == "amenity" && v in setOf("restaurant", "fast_food", "food_court", "cafe", "ice_cream") ->
      Icons.Rounded.Restaurant
    else -> Icons.Rounded.Place
  }
}
