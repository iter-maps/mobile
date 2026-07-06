package it.iterapp.app.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.app.common.LineBadge
import it.iterapp.app.common.formatClock
import it.iterapp.app.common.formatDelay
import it.iterapp.app.ui.theme.DelayColors
import it.iterapp.app.ui.theme.LineColors
import it.iterapp.app.ui.theme.lineColor
import it.iterapp.core.model.Itinerary
import it.iterapp.core.model.Leg
import it.iterapp.core.model.LegMode

/**
 * Proportional segment strip: each leg gets width by duration (with a small
 * floor so short walks stay visible), walk legs as gray, transit legs in
 * their line color with the badge inside when it fits.
 */
@Composable
fun ItinerarySegments(itinerary: Itinerary, modifier: Modifier = Modifier) {
  val total = itinerary.legs.sumOf { it.durationSeconds }.coerceAtLeast(1)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(26.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    itinerary.legs.forEach { leg ->
      val weight = (leg.durationSeconds.toFloat() / total).coerceAtLeast(0.08f)
      val color = if (leg.isTransit) lineColor(leg.routeColor, leg.mode) else LineColors.Walk
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(weight)
          .fillMaxHeight()
          .clip(RoundedCornerShape(6.dp))
          .background(if (leg.isTransit) color else color.copy(alpha = 0.25f)),
      ) {
        if (leg.isTransit && weight > 0.14f) {
          Text(
            text = leg.routeShortName ?: "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = it.iterapp.app.ui.theme.onLineColor(color),
            maxLines = 1,
          )
        } else if (!leg.isTransit) {
          Icon(
            Icons.AutoMirrored.Rounded.DirectionsWalk,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

/**
 * Vertical leg timeline for the detail page: colored rail (dashed feel for
 * walks via reduced alpha), times, badges, headsigns, stop counts and the
 * historical-delay hint when the gateway annotated the leg.
 */
@Composable
fun LegTimeline(itinerary: Itinerary, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
    itinerary.legs.forEachIndexed { index, leg ->
      LegRow(leg = leg, isLast = index == itinerary.legs.lastIndex)
    }
  }
}

@Composable
private fun LegRow(leg: Leg, isLast: Boolean) {
  val railColor = if (leg.isTransit) lineColor(leg.routeColor, leg.mode) else LineColors.Walk
  Row(Modifier.height(intrinsicSize = androidx.compose.foundation.layout.IntrinsicSize.Min)) {
    // Times column
    Column(
      horizontalAlignment = Alignment.End,
      modifier = Modifier.width(52.dp).padding(top = 2.dp),
    ) {
      Text(
        text = formatClock(leg.startMs),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
    // Rail
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
    ) {
      Box(
        Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(railColor),
      )
      Box(
        Modifier
          .width(4.dp)
          .weight(1f)
          .background(if (leg.isTransit) railColor else railColor.copy(alpha = 0.35f)),
      )
      if (isLast) {
        Box(
          Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(railColor),
        )
      }
    }
    // Leg body
    Column(Modifier.weight(1f).padding(bottom = 18.dp)) {
      Text(
        text = leg.from.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp),
      ) {
        if (leg.isTransit) {
          LineBadge(leg)
          leg.headsign?.let {
            Text(
              text = "» $it",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              modifier = Modifier.weight(1f, fill = false),
            )
          }
        } else {
          Icon(
            Icons.AutoMirrored.Rounded.DirectionsWalk,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = it.iterapp.app.common.formatDistance(leg.distanceMeters),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = it.iterapp.app.common.formatDuration(leg.durationSeconds),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (leg.isTransit && leg.intermediateStops.isNotEmpty()) {
        Text(
          text = stringResource(R.string.planning_stops_count, leg.intermediateStops.size + 1),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
      leg.predictedDelay?.let { delay ->
        if (delay.p85Seconds >= 60) {
          Text(
            text = stringResource(R.string.planning_usually_delayed, formatDelay(delay.p85Seconds)),
            style = MaterialTheme.typography.bodySmall,
            color = DelayColors.Minor,
            modifier = Modifier.padding(top = 2.dp),
          )
        }
      }
      if (isLast) {
        Text(
          text = leg.to.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          modifier = Modifier.padding(top = 12.dp),
        )
      }
    }
  }
}

/** Walk mode helper referenced by overlays. */
val Leg.isWalkLike: Boolean
  get() = mode == LegMode.WALK || mode == LegMode.BICYCLE
