package it.iterapp.app.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.common.formatClock
import it.iterapp.app.common.formatDistance
import it.iterapp.app.common.formatDuration
import it.iterapp.app.sheet.SheetPageHeader

/**
 * Leg-by-leg detail of the selected itinerary. Times lead the header — this
 * is the execution surface: when do I leave, when do I arrive — while the
 * results list stays duration-first for comparison.
 */
@Composable
fun PlanningDetailPage(
  viewModel: PlanningViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selected by viewModel.selected.collectAsStateWithLifecycle()
  val fromEndpoint by viewModel.from.collectAsStateWithLifecycle()
  val toEndpoint by viewModel.to.collectAsStateWithLifecycle()

  val itinerary = selected
  if (itinerary == null) {
    // Selection can clear underneath this page (reset, replan); navigate out
    // from an effect — composition must not call back synchronously.
    LaunchedEffect(Unit) { onBack() }
    return
  }

  val originLabel = fromEndpoint?.let {
    if (it.isUserLocation) {
      stringResource(R.string.planning_my_location)
    } else {
      it.name.takeIf(String::isNotBlank)
    }
  }
  val destinationLabel = toEndpoint?.let {
    if (it.isUserLocation) {
      stringResource(R.string.planning_my_location)
    } else {
      it.name.takeIf(String::isNotBlank)
    }
  }

  Column(modifier.fillMaxSize()) {
    val scroll = rememberScrollState()
    SheetPageHeader(
      title = stringResource(R.string.planning_detail_title),
      onBack = onBack,
      scrolledUnder = scroll.canScrollBackward,
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scroll)
        .padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
      Row(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
          text = "${formatClock(itinerary.startMs)} – ${formatClock(itinerary.endMs)}",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f).alignByBaseline(),
        )
        Text(
          text = formatDuration(itinerary.durationSeconds),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.alignByBaseline(),
        )
      }
      ItinerarySegments(itinerary, Modifier.padding(bottom = 10.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 16.dp),
      ) {
        Text(
          text = when (itinerary.numberOfTransfers) {
            0 -> stringResource(R.string.planning_transfers_none)
            1 -> stringResource(R.string.planning_transfers_one)
            else -> stringResource(R.string.planning_transfers, itinerary.numberOfTransfers)
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = stringResource(
            R.string.planning_walk_distance,
            formatDistance(itinerary.walkDistanceMeters),
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      LegTimeline(
        itinerary = itinerary,
        originLabel = originLabel,
        destinationLabel = destinationLabel,
      )
    }
  }
}
