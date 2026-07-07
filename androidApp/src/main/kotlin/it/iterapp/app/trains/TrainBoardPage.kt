package it.iterapp.app.trains

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.common.IconListRow
import it.iterapp.app.common.SheetSearchField
import it.iterapp.app.common.SkeletonBlock
import it.iterapp.app.common.formatStationName
import it.iterapp.app.common.rememberSkeletonPulse
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.app.sheet.SheetStatusMessage
import it.iterapp.app.ui.theme.delayColor
import it.iterapp.app.ui.theme.delayEarlyColor
import it.iterapp.app.ui.theme.delayOnTimeColor
import it.iterapp.core.wire.BoardEntry

/**
 * Live departures/arrivals. Station autocomplete first; boards poll at the
 * contract cadence while visible, keeping the last good board through
 * transient failures.
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
  val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
  val searchError by viewModel.searchError.collectAsStateWithLifecycle()
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
    val boardListState = rememberLazyListState()
    SheetPageHeader(
      title = selected?.name?.let(::formatStationName) ?: stringResource(R.string.trains_title),
      onBack = {
        if (selected != null) viewModel.selectStation(null) else onBack()
      },
      scrolledUnder = selected != null && boardListState.canScrollBackward,
      trailing = {
        if (selected != null) {
          IconButton(onClick = viewModel::refresh) {
            Icon(
              Icons.Rounded.Refresh,
              contentDescription = stringResource(R.string.action_refresh),
            )
          }
        }
      },
    )

    if (selected == null) {
      SheetSearchField(
        query = query,
        onQueryChange = viewModel::onQueryChange,
        placeholder = stringResource(R.string.trains_search_hint),
        focusRequester = focusRequester,
        isLoading = isSearching,
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
      )
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp)
          .imePadding(),
        contentPadding = PaddingValues(vertical = 8.dp),
      ) {
        items(stations, key = { it.id }) { station ->
          IconListRow(
            icon = Icons.Rounded.Train,
            title = formatStationName(station.name),
            onClick = { viewModel.selectStation(station) },
          )
        }
        when {
          query.isBlank() -> item {
            Text(
              text = stringResource(R.string.trains_search_prompt),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            )
          }
          searchError && query.length >= 2 && !isSearching -> item {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
              Text(
                text = stringResource(R.string.error_network),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
              )
              TextButton(onClick = viewModel::retryStationSearch) {
                Text(stringResource(R.string.action_retry))
              }
            }
          }
          // isSearching covers the debounce window (set in onQueryChange),
          // so the empty hint can't flash before the fetch starts.
          stations.isEmpty() && query.length >= 2 && !isSearching -> item {
            Text(
              text = stringResource(R.string.trains_no_stations),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            )
          }
        }
      }
    } else {
      SingleChoiceSegmentedButtonRow(
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
      ) {
        BoardTab.entries.forEachIndexed { index, t ->
          SegmentedButton(
            selected = tab == t,
            onClick = { viewModel.setTab(t) },
            shape = SegmentedButtonDefaults.itemShape(index, BoardTab.entries.size),
          ) {
            Text(
              stringResource(
                when (t) {
                  BoardTab.DEPARTURES -> R.string.trains_departures
                  BoardTab.ARRIVALS -> R.string.trains_arrivals
                },
              ),
            )
          }
        }
      }

      // Keyed by state class: poll updates refresh the list in place (scroll
      // preserved); only Loading/Loaded/Error changes animate.
      AnimatedContent(
        targetState = board,
        contentKey = { it::class },
        transitionSpec = {
          (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false))
        },
        label = "board-state",
      ) { s ->
        when (s) {
          BoardState.Idle -> {}
          BoardState.Loading -> BoardSkeletons()
          BoardState.Error -> SheetStatusMessage(
            icon = Icons.Rounded.CloudOff,
            message = stringResource(R.string.error_network),
            action = {
              FilledTonalButton(onClick = viewModel::refresh) {
                Text(stringResource(R.string.action_retry))
              }
            },
          )
          is BoardState.Loaded -> {
            if (s.entries.isEmpty()) {
              SheetStatusMessage(
                icon = Icons.Rounded.Train,
                message = stringResource(R.string.trains_empty),
              )
            } else {
              Column {
                AnimatedVisibility(visible = s.stale) {
                  Text(
                    text = stringResource(R.string.trains_board_stale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                  )
                }
                LazyColumn(
                  state = boardListState,
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                ) {
                  items(s.entries, key = { "${it.trainNumber}-${it.scheduledTime}" }) { entry ->
                    // s.forTab, not the outer tab: outgoing content must keep
                    // its own labeling while it animates out after a switch.
                    BoardRow(entry = entry, showOrigin = s.forTab == BoardTab.ARRIVALS)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BoardSkeletons() {
  val pulse = rememberSkeletonPulse()
  Column(Modifier.padding(top = 4.dp)) {
    repeat(6) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
      ) {
        Column {
          SkeletonBlock(Modifier.size(width = 48.dp, height = 18.dp), pulse::value)
          Spacer(Modifier.height(4.dp))
          SkeletonBlock(Modifier.size(width = 40.dp, height = 10.dp), pulse::value)
        }
        Column(Modifier.weight(1f).padding(start = 28.dp, end = 8.dp)) {
          SkeletonBlock(Modifier.size(width = 72.dp, height = 10.dp), pulse::value)
          Spacer(Modifier.height(4.dp))
          SkeletonBlock(
            Modifier
              .fillMaxWidth(0.7f)
              .height(16.dp),
            pulse::value,
          )
        }
        SkeletonBlock(Modifier.size(width = 48.dp, height = 24.dp), pulse::value)
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
      .padding(horizontal = 16.dp, vertical = 10.dp)
      // One TalkBack node per departure instead of five loose fragments.
      .semantics(mergeDescendants = true) {},
  ) {
    Column(Modifier.widthIn(min = 64.dp)) {
      Text(
        text = entry.scheduledTime,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
      )
      DelayLabel(entry.delayMinutes)
    }
    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (entry.category.isNotBlank()) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = entry.category,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
          Spacer(Modifier.width(6.dp))
        }
        Text(
          // trainNumber repeats the category ("REG 22815"); the badge owns it.
          text = entry.trainNumber.removePrefix(entry.category).trim()
            .ifEmpty { entry.trainNumber },
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = if (showOrigin) {
          entry.origin?.let(::formatStationName) ?: "—"
        } else {
          formatStationName(entry.destination)
        },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    entry.platform?.let {
      Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
      ) {
        Text(
          text = stringResource(R.string.trains_platform, it),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun DelayLabel(delayMinutes: Int) {
  val (text, color) = when {
    delayMinutes > 0 -> stringResource(R.string.trains_delay_min, delayMinutes) to
      delayColor(delayMinutes)
    delayMinutes < 0 -> stringResource(R.string.trains_early_min, -delayMinutes) to
      delayEarlyColor()
    else -> stringResource(R.string.trains_on_time) to delayOnTimeColor()
  }
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Medium,
    color = color,
    maxLines = 1,
    softWrap = false,
  )
}
