package it.iterapp.app.planning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * Proportional segment strip: each leg gets width by duration (with a floor
 * so short walks stay visible and transit slots can host their line label),
 * walk legs on surfaceVariant, transit legs in their line color with the
 * badge inside.
 */
@Composable
fun ItinerarySegments(itinerary: Itinerary, modifier: Modifier = Modifier) {
  val total = itinerary.legs.sumOf { it.durationSeconds }.coerceAtLeast(1)
  val walkBg = MaterialTheme.colorScheme.surfaceVariant
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(30.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    itinerary.legs.forEach { leg ->
      val weight = (leg.durationSeconds.toFloat() / total)
        .coerceAtLeast(if (leg.isTransit) 0.16f else 0.08f)
      val color = if (leg.isTransit) lineColor(leg.routeColor, leg.mode) else walkBg
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(weight)
          .fillMaxHeight()
          .clip(RoundedCornerShape(6.dp))
          .background(color),
      ) {
        if (leg.isTransit) {
          Text(
            text = leg.routeShortName ?: "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = it.iterapp.app.ui.theme.onLineColor(color),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        } else {
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
 * Vertical leg timeline for the detail page: colored rail (dotted for walks),
 * times, badges, headsigns, stop counts and the historical-delay hint when
 * the gateway annotated the leg.
 */
@Composable
fun LegTimeline(itinerary: Itinerary, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
    itinerary.legs.forEach { leg ->
      LegRow(leg = leg)
    }
    itinerary.legs.lastOrNull()?.let { last ->
      val railColor = if (last.isTransit) lineColor(last.routeColor, last.mode) else LineColors.Walk
      Row {
        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.width(52.dp).padding(top = 2.dp),
        ) {
          Text(
            text = formatClock(last.endMs),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        // 9dp side padding centers the 16dp terminal node on the 14dp leg nodes.
        TimelineNode(
          color = railColor,
          terminal = true,
          modifier = Modifier.padding(horizontal = 9.dp),
        )
        Text(
          text = last.to.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun TimelineNode(color: Color, terminal: Boolean = false, modifier: Modifier = Modifier) {
  Box(
    modifier
      .size(if (terminal) 16.dp else 14.dp)
      .clip(CircleShape)
      .background(color),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .size(if (terminal) 6.dp else 5.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface),
    )
  }
}

@Composable
private fun LegRow(leg: Leg) {
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
      TimelineNode(railColor)
      if (leg.isTransit) {
        Box(
          Modifier
            .width(4.dp)
            .weight(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(railColor),
        )
      } else {
        Canvas(Modifier.width(4.dp).weight(1f).padding(vertical = 2.dp)) {
          val x = size.width / 2f
          drawLine(
            color = railColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = size.width,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.5f, size.width * 2.2f), 0f),
          )
        }
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
              text = it,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
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
        var expanded by remember(leg) { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(2.dp),
        ) {
          Text(
            text = stringResource(R.string.planning_stops_count, leg.intermediateStops.size + 1),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.width(2.dp))
          Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .size(18.dp)
              .graphicsLayer { rotationZ = if (expanded) 90f else 0f },
          )
        }
        AnimatedVisibility(visible = expanded) {
          Column(Modifier.padding(top = 4.dp, start = 2.dp)) {
            leg.intermediateStops.forEach { stop ->
              Text(
                text = stop.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 3.dp),
              )
            }
          }
        }
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
    }
  }
}

/** Walk mode helper referenced by overlays. */
val Leg.isWalkLike: Boolean
  get() = mode == LegMode.WALK || mode == LegMode.BICYCLE
