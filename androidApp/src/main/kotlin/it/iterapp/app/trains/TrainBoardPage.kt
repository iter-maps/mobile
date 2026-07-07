package it.iterapp.app.trains

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.app.ui.theme.DelayColors
import it.iterapp.core.wire.BoardEntry

/**
 * Live departures/arrivals. Station autocomplete first; boards poll at the
 * contract cadence while visible.
 */
@Composable
fun TrainBoardPage(
  viewModel: TrainBoardViewModel,
  initialQuery: String?,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  initialStationId: String? = null,
) {
  val query by viewModel.stationQuery.collectAsStateWithLifecycle()
  val stations by viewModel.stationResults.collectAsStateWithLifecycle()
  val selected by viewModel.selectedStation.collectAsStateWithLifecycle()
  val tab by viewModel.tab.collectAsStateWithLifecycle()
  val board by viewModel.board.collectAsStateWithLifecycle()
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(initialQuery, initialStationId) {
    if (initialStationId != null) {
      // A station-typed search result already carries the board id; switch to
      // it even if a different station was previously selected.
      if (selected?.id != initialStationId) {
        viewModel.selectStation(
          it.iterapp.core.wire.Station(id = initialStationId, name = initialQuery ?: initialStationId),
        )
      }
    } else if (selected == null && !initialQuery.isNullOrBlank()) {
      viewModel.onQueryChange(initialQuery)
    }
  }

  // Focus the field on first entry and when backing out of a station, but not
  // when a station id was passed in (avoids a keyboard flash before
  // auto-selection).
  LaunchedEffect(selected) {
    if (selected == null && initialStationId == null) focusRequester.requestFocus()
  }

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(
      title = selected?.name ?: stringResource(R.string.trains_title),
      onBack = {
        if (selected != null) viewModel.selectStation(null) else onBack()
      },
    )

    if (selected == null) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
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
                text = stringResource(R.string.trains_search_hint),
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
          if (query.isNotEmpty()) {
            IconButton(
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
      LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(stations, key = { it.id }) { station ->
          Surface(
            onClick = { viewModel.selectStation(station) },
            color = Color.Transparent,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            ) {
              Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Rounded.Train,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp),
              )
            }
          }
        }
        when {
          query.isBlank() -> item {
            Text(
              text = stringResource(R.string.trains_search_prompt),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            )
          }
          // The >= 2 gate mirrors SearchPage and keeps the hint from flashing
          // during the station-search debounce.
          stations.isEmpty() && query.length >= 2 -> item {
            Text(
              text = stringResource(R.string.trains_no_stations),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            )
          }
        }
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = tab == BoardTab.DEPARTURES,
          onClick = { viewModel.setTab(BoardTab.DEPARTURES) },
          label = { Text(stringResource(R.string.trains_departures)) },
        )
        FilterChip(
          selected = tab == BoardTab.ARRIVALS,
          onClick = { viewModel.setTab(BoardTab.ARRIVALS) },
          label = { Text(stringResource(R.string.trains_arrivals)) },
        )
      }

      when (val s = board) {
        BoardState.Idle -> {}
        BoardState.Loading -> Box(
          Modifier.fillMaxWidth().padding(vertical = 32.dp),
          contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        BoardState.Error -> Text(
          text = stringResource(R.string.error_network),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        is BoardState.Loaded -> {
          if (s.entries.isEmpty()) {
            Text(
              text = stringResource(R.string.trains_empty),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
          } else {
            LazyColumn(Modifier.fillMaxSize()) {
              items(s.entries, key = { "${it.trainNumber}-${it.scheduledTime}" }) { entry ->
                BoardRow(entry = entry, showOrigin = tab == BoardTab.ARRIVALS)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BoardRow(entry: BoardEntry, showOrigin: Boolean) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 10.dp),
  ) {
    Column(Modifier.width(64.dp)) {
      Text(
        text = entry.scheduledTime,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      DelayLabel(entry.delayMinutes)
    }
    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
      Text(
        text = entry.trainNumber,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = if (showOrigin) entry.origin ?: "—" else entry.destination,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    entry.platform?.let {
      Text(
        text = stringResource(R.string.trains_platform, it),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun DelayLabel(delayMinutes: Int) {
  val (text, color) = when {
    delayMinutes > 0 -> stringResource(R.string.trains_delay_min, delayMinutes) to
      DelayColors.forMinutes(delayMinutes)
    delayMinutes < 0 -> stringResource(R.string.trains_early_min, -delayMinutes) to DelayColors.Early
    else -> stringResource(R.string.trains_on_time) to DelayColors.OnTime
  }
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Medium,
    color = color,
  )
}
