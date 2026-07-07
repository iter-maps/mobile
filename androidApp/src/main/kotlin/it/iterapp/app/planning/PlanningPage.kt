package it.iterapp.app.planning

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.common.SkeletonBlock
import it.iterapp.app.common.formatClock
import it.iterapp.app.common.formatDistance
import it.iterapp.app.common.formatDuration
import it.iterapp.app.common.rememberSkeletonPulse
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.app.sheet.SheetStatusMessage
import it.iterapp.core.model.Itinerary

/**
 * Planning page: from/to fields, departure-time chip, ranking profile chips,
 * itinerary results. Selecting an itinerary draws it on the map behind the
 * sheet (ADR 0008); the trailing chevron button on each card opens the leg
 * detail.
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
  val departureMs by viewModel.departureMs.collectAsStateWithLifecycle()
  val arriveBy by viewModel.arriveBy.collectAsStateWithLifecycle()
  var showTimeDialog by remember { mutableStateOf(false) }

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
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
          EndpointField(
            isFrom = true,
            value = from?.let {
              if (it.isUserLocation) stringResource(R.string.planning_my_location) else it.name
            },
            onClick = { onPickEndpoint(true) },
          )
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 48.dp),
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

    // When to leave: opens the depart-at / arrive-by picker.
    DepartureTimeChip(
      departureMs = departureMs,
      arriveBy = arriveBy,
      onClick = { showTimeDialog = true },
      modifier = Modifier.padding(start = 16.dp, top = 10.dp),
    )
    if (showTimeDialog) {
      DepartureTimeDialog(
        initialMs = departureMs,
        initialArriveBy = arriveBy,
        onDismiss = { showTimeDialog = false },
        onConfirm = { ms, arrive ->
          showTimeDialog = false
          viewModel.setDeparture(ms, arrive)
        },
      )
    }

    // Ranking profile chips.
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
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

    AnimatedContent(
      targetState = state,
      transitionSpec = {
        (fadeIn(spring(stiffness = 260f)) togetherWith fadeOut(spring(stiffness = 260f)))
          .using(SizeTransform(clip = false))
      },
      label = "plan-state",
    ) { s ->
      when (s) {
        PlanState.Idle -> {}
        is PlanState.Loading -> if (s.previous != null) {
          // Replan: keep the outgoing list, dimmed, under a thin progress bar.
          Column {
            LinearProgressIndicator(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            )
            ItineraryList(
              itineraries = s.previous,
              selected = selected,
              enabled = false,
              onSelect = {},
              onOpenDetail = {},
              modifier = Modifier.padding(top = 8.dp),
            )
          }
        } else {
          ItinerarySkeletons()
        }
        is PlanState.Error -> SheetStatusMessage(
          icon = if (s.network) Icons.Rounded.CloudOff else Icons.Rounded.ErrorOutline,
          message = stringResource(if (s.network) R.string.error_network else R.string.error_generic),
          action = {
            FilledTonalButton(onClick = { viewModel.replan() }) {
              Text(stringResource(R.string.action_retry))
            }
          },
        )
        is PlanState.Results -> {
          if (s.itineraries.isEmpty()) {
            SheetStatusMessage(
              icon = Icons.Rounded.SearchOff,
              message = stringResource(R.string.planning_no_results),
            )
          } else {
            ItineraryList(
              itineraries = s.itineraries,
              selected = selected,
              enabled = true,
              onSelect = viewModel::select,
              onOpenDetail = onOpenDetail,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ItineraryList(
  itineraries: List<Itinerary>,
  selected: Itinerary?,
  enabled: Boolean,
  onSelect: (Itinerary) -> Unit,
  onOpenDetail: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    modifier = if (enabled) modifier else modifier.alpha(0.55f),
  ) {
    items(itineraries) { itinerary ->
      ItineraryCard(
        itinerary = itinerary,
        isSelected = itinerary === selected,
        enabled = enabled,
        onClick = { onSelect(itinerary) },
        onOpenDetail = onOpenDetail,
      )
    }
  }
}

/** First-load placeholders shaped like the real cards — no layout jump. */
@Composable
private fun ItinerarySkeletons() {
  val pulse = rememberSkeletonPulse()
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(horizontal = 16.dp),
  ) {
    repeat(3) {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(width = 88.dp, height = 24.dp), pulse)
            Spacer(Modifier.weight(1f))
            SkeletonBlock(Modifier.size(width = 96.dp, height = 14.dp), pulse)
          }
          SkeletonBlock(
            Modifier
              .fillMaxWidth()
              .height(30.dp),
            pulse,
          )
          SkeletonBlock(Modifier.size(width = 180.dp, height = 12.dp), pulse)
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
      .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
      Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    } else {
      Text(
        text = stringResource(R.string.planning_choose_on_search),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItineraryCard(
  itinerary: Itinerary,
  isSelected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  onOpenDetail: () -> Unit,
) {
  val containerColor by animateColorAsState(
    if (isSelected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    tween(200),
    label = "itineraryContainer",
  )
  val contentColor by animateColorAsState(
    if (isSelected) {
      MaterialTheme.colorScheme.onSecondaryContainer
    } else {
      MaterialTheme.colorScheme.onSurface
    },
    tween(200),
    label = "itineraryContent",
  )
  val borderColor by animateColorAsState(
    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
    tween(200),
    label = "itineraryBorder",
  )
  Surface(
    selected = isSelected,
    onClick = onClick,
    enabled = enabled,
    shape = MaterialTheme.shapes.large,
    color = containerColor,
    contentColor = contentColor,
    border = BorderStroke(1.5.dp, borderColor),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(
        Modifier.weight(1f).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row {
          Text(
            text = formatDuration(itinerary.durationSeconds),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).alignByBaseline(),
          )
          Text(
            text = "${formatClock(itinerary.startMs)} – ${formatClock(itinerary.endMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
          )
        }
        ItinerarySegments(itinerary)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
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
          ReliabilityHint(itinerary)
        }
      }
      FilledTonalIconButton(
        onClick = onOpenDetail,
        enabled = enabled,
        modifier = Modifier.padding(end = 12.dp),
      ) {
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
      R.string.planning_reliability_high to it.iterapp.app.ui.theme.delayOnTimeColor()
    ranking.reliabilityScore <= 0.35 ->
      R.string.planning_reliability_low to it.iterapp.app.ui.theme.delaySevereColor()
    else -> return
  }
  Text(
    text = stringResource(textRes),
    style = MaterialTheme.typography.bodySmall,
    color = color,
  )
}
