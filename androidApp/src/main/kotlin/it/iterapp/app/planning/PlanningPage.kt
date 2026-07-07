package it.iterapp.app.planning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import it.iterapp.core.model.Itinerary

/**
 * Planning page: from/to fields, ranking profile chips, itinerary results.
 * Selecting an itinerary draws it on the map behind the sheet (ADR 0008);
 * the trailing chevron button on each card opens the leg detail.
 */
@Composable
fun PlanningPage(
  viewModel: PlanningViewModel,
  onBack: () -> Unit,
  onPickEndpoint: (fromField: Boolean) -> Unit,
  onOpenDetail: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val from by viewModel.from.collectAsStateWithLifecycle()
  val to by viewModel.to.collectAsStateWithLifecycle()
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val state by viewModel.state.collectAsStateWithLifecycle()
  val selected by viewModel.selected.collectAsStateWithLifecycle()

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(title = stringResource(R.string.planning_title), onBack = onBack)

    // From / To card with swap.
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = 16.dp, top = 6.dp, bottom = 6.dp)) {
          EndpointField(
            isFrom = true,
            value = from?.let {
              if (it.isUserLocation) stringResource(R.string.planning_my_location) else it.name
            },
            onClick = { onPickEndpoint(true) },
          )
          androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 32.dp),
          )
          EndpointField(
            isFrom = false,
            value = to?.let {
              if (it.isUserLocation) stringResource(R.string.planning_my_location) else it.name
            },
            onClick = { onPickEndpoint(false) },
          )
        }
        IconButton(onClick = viewModel::swap, modifier = Modifier.padding(end = 4.dp)) {
          Icon(
            Icons.Rounded.SwapVert,
            contentDescription = stringResource(R.string.planning_swap),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // Ranking profile chips.
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      PlanProfile.entries.forEach { p ->
        FilterChip(
          selected = profile == p,
          onClick = { viewModel.setProfile(p) },
          label = { Text(profileLabel(p)) },
          leadingIcon = if (profile == p) {
            {
              Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
              )
            }
          } else {
            null
          },
        )
      }
    }

    when (val s = state) {
      PlanState.Idle -> {}
      PlanState.Loading -> Box(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
      is PlanState.Error -> Text(
        text = stringResource(if (s.network) R.string.error_network else R.string.error_generic),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
      )
      is PlanState.Results -> {
        if (s.itineraries.isEmpty()) {
          Text(
            text = stringResource(R.string.planning_no_results),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
          )
        } else {
          LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 4.dp,
          )) {
            items(s.itineraries) { itinerary ->
              ItineraryCard(
                itinerary = itinerary,
                isSelected = itinerary === selected,
                onClick = { viewModel.select(itinerary) },
                onOpenDetail = onOpenDetail,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EndpointField(isFrom: Boolean, value: String?, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp),
  ) {
    Icon(
      if (isFrom) Icons.Rounded.MyLocation else Icons.Rounded.PinDrop,
      contentDescription = stringResource(
        if (isFrom) R.string.planning_from else R.string.planning_to,
      ),
      tint = if (value != null) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(12.dp))
    if (value != null) {
      Text(text = value, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
    } else {
      Text(
        text = stringResource(R.string.planning_choose_on_search),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun profileLabel(profile: PlanProfile): String = stringResource(
  when (profile) {
    PlanProfile.FASTEST -> R.string.planning_profile_default
    PlanProfile.RELIABLE -> R.string.planning_profile_reliability
    PlanProfile.BALANCED -> R.string.planning_profile_balanced
    PlanProfile.ECO -> R.string.planning_profile_eco
    PlanProfile.COMFORT -> R.string.planning_profile_comfort
  },
)

@Composable
private fun ItineraryCard(
  itinerary: Itinerary,
  isSelected: Boolean,
  onClick: () -> Unit,
  onOpenDetail: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = if (isSelected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(
        Modifier.weight(1f).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = formatDuration(itinerary.durationSeconds),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "${formatClock(itinerary.startMs)} – ${formatClock(itinerary.endMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        ItinerarySegments(itinerary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
          ReliabilityHint(itinerary)
        }
      }
      FilledTonalIconButton(onClick = onOpenDetail, modifier = Modifier.padding(end = 10.dp)) {
        Icon(
          Icons.Rounded.ChevronRight,
          contentDescription = stringResource(R.string.planning_detail_title),
        )
      }
    }
  }
}

@Composable
private fun ReliabilityHint(itinerary: Itinerary) {
  val ranking = itinerary.ranking ?: return
  val (textRes, color) = when {
    ranking.reliabilityScore >= 0.8 ->
      R.string.planning_reliability_high to it.iterapp.app.ui.theme.DelayColors.OnTime
    ranking.reliabilityScore <= 0.35 ->
      R.string.planning_reliability_low to it.iterapp.app.ui.theme.DelayColors.Severe
    else -> return
  }
  Text(
    text = stringResource(textRes),
    style = MaterialTheme.typography.bodySmall,
    color = color,
  )
}
