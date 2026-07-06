package it.iterapp.app.trains

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

  LaunchedEffect(initialQuery, initialStationId) {
    if (selected != null) return@LaunchedEffect
    if (initialStationId != null) {
      // A station-typed search result already carries the board id.
      viewModel.selectStation(
        it.iterapp.core.wire.Station(id = initialStationId, name = initialQuery ?: initialStationId),
      )
    } else if (!initialQuery.isNullOrBlank()) {
      viewModel.onQueryChange(initialQuery)
    }
  }

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(
      title = selected?.name ?: stringResource(R.string.trains_title),
      onBack = {
        if (selected != null) viewModel.selectStation(null) else onBack()
      },
    )

    if (selected == null) {
      OutlinedTextField(
        value = query,
        onValueChange = viewModel::onQueryChange,
        placeholder = { Text(stringResource(R.string.trains_search_hint)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      )
      LazyColumn(Modifier.fillMaxSize()) {
        items(stations, key = { it.id }) { station ->
          Surface(
            onClick = { viewModel.selectStation(station) },
            color = androidx.compose.ui.graphics.Color.Transparent,
          ) {
            Text(
              text = station.name,
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            )
          }
        }
      }
    } else {
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
      ) {
        SegmentedButton(
          selected = tab == BoardTab.DEPARTURES,
          onClick = { viewModel.setTab(BoardTab.DEPARTURES) },
          shape = SegmentedButtonDefaults.itemShape(0, 2),
        ) { Text(stringResource(R.string.trains_departures)) }
        SegmentedButton(
          selected = tab == BoardTab.ARRIVALS,
          onClick = { viewModel.setTab(BoardTab.ARRIVALS) },
          shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) { Text(stringResource(R.string.trains_arrivals)) }
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
    Text(
      text = entry.scheduledTime,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.width(56.dp),
    )
    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
      Text(
        text = if (showOrigin) entry.origin ?: "—" else entry.destination,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
      )
      Row {
        Text(
          text = entry.trainNumber,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.platform?.let {
          Text(
            text = "  ·  " + stringResource(R.string.trains_platform, it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    DelayLabel(entry.delayMinutes)
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
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.SemiBold,
    color = color,
  )
}
