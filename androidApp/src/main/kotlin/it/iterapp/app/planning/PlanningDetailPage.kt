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

/** Leg-by-leg detail of the selected itinerary. */
@Composable
fun PlanningDetailPage(
  viewModel: PlanningViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selected by viewModel.selected.collectAsStateWithLifecycle()
  val itinerary = selected ?: run {
    onBack()
    return
  }

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(title = stringResource(R.string.planning_detail_title), onBack = onBack)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp),
    ) {
      Row(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
          text = "${formatClock(itinerary.startMs)} – ${formatClock(itinerary.endMs)}",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = formatDuration(itinerary.durationSeconds),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
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
      LegTimeline(itinerary)
    }
  }
}
