package it.iterapp.app.planning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.iterapp.app.R
import it.iterapp.app.common.LineBadge
import it.iterapp.app.common.formatClock
import it.iterapp.app.common.formatDelay
import it.iterapp.app.common.formatStationName
import it.iterapp.app.ui.theme.delayColor
import it.iterapp.app.ui.theme.delayMinorColor
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
 * times (live-delay tinted when the feed is realtime), badges, headsigns,
 * expandable stops and the historical-delay hint. [originLabel] and
 * [destinationLabel] replace the wire's placeholder endpoint names
 * ("Origin"/"Destination") with the user's own picks.
 */
@Composable
fun LegTimeline(
  itinerary: Itinerary,
  modifier: Modifier = Modifier,
  originLabel: String? = null,
  destinationLabel: String? = null,
) {
  Column(modifier.fillMaxWidth()) {
    itinerary.legs.forEachIndexed { index, leg ->
      val prev = itinerary.legs.getOrNull(index - 1)
      LegRow(
        leg = leg,
        nameOverride = if (index == 0) originLabel else null,
        nextLegStartMs = itinerary.legs.getOrNull(index + 1)?.startMs,
        // A stub above the node keeps transit rails visually continuous; the
        // dotted walk rail tolerates the small gap instead.
        prevRailColor = if (prev?.isTransit == true) {
          lineColor(prev.routeColor, prev.mode)
        } else {
          Color.Transparent
        },
      )
    }
    itinerary.legs.lastOrNull()?.let { last ->
      val railColor = if (last.isTransit) {
        lineColor(last.routeColor, last.mode)
      } else {
        MaterialTheme.colorScheme.outline
      }
      Row {
        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.widthIn(min = 52.dp),
        ) {
          TimelineClock(
            epochMs = last.endMs,
            live = last.isTransit && last.isRealTime,
            delaySeconds = last.arrivalDelaySeconds,
          )
        }
        // 9dp side padding centers the 16dp terminal node on the 14dp nodes.
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 9.dp),
        ) {
          // 4dp stub centers the node on the times' 24sp first-line box.
          Box(
            Modifier
              .width(4.dp)
              .height(4.dp)
              .background(if (last.isTransit) railColor else Color.Transparent),
          )
          TimelineNode(color = railColor, terminal = true)
        }
        Text(
          text = destinationLabel ?: formatStationName(last.to.name),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

/** Timeline clock: bodyMedium in a 24sp box so it centers on the 12dp node line. */
@Composable
private fun TimelineClock(epochMs: Long, live: Boolean, delaySeconds: Int) {
  Text(
    text = formatClock(epochMs),
    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    softWrap = false,
    // floorDiv: -30s must classify as early, not truncate to on-time.
    color = if (live) delayColor(delaySeconds.floorDiv(60)) else Color.Unspecified,
  )
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
private fun LegRow(
  leg: Leg,
  nameOverride: String?,
  nextLegStartMs: Long?,
  prevRailColor: Color,
) {
  val railColor = if (leg.isTransit) {
    lineColor(leg.routeColor, leg.mode)
  } else {
    MaterialTheme.colorScheme.outline
  }
  Row(Modifier.height(intrinsicSize = IntrinsicSize.Min)) {
    // Times column: departure on the node line; arrival at the leg's end when
    // a wait follows, so transfer time is visible instead of silently absorbed.
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .widthIn(min = 52.dp)
        .fillMaxHeight()
        .padding(bottom = 4.dp),
    ) {
      TimelineClock(
        epochMs = leg.startMs,
        live = leg.isTransit && leg.isRealTime,
        delaySeconds = leg.departureDelaySeconds,
      )
      if (nextLegStartMs != null && nextLegStartMs - leg.endMs >= 60_000L) {
        Text(
          text = formatClock(leg.endMs),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
    // Rail
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
    ) {
      // 5dp stub above the 14dp node centers it on the times' first line.
      Box(
        Modifier
          .width(4.dp)
          .height(5.dp)
          .background(prevRailColor),
      )
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
        text = nameOverride ?: formatStationName(leg.from.name),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
        val stateDesc = stringResource(
          if (expanded) R.string.state_expanded else R.string.state_collapsed,
        )
        val chevronRotation by animateFloatAsState(
          targetValue = if (expanded) 90f else 0f,
          animationSpec = tween(200),
          label = "stopsChevron",
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .padding(top = 2.dp)
            // The -8dp offset cancels the inner padding so the label stays
            // aligned with the leg body while the ripple pill gets real size.
            .offset(x = (-8).dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button) { expanded = !expanded }
            .heightIn(min = 36.dp)
            .padding(horizontal = 8.dp)
            .semantics { stateDescription = stateDesc },
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
              .graphicsLayer { rotationZ = chevronRotation },
          )
        }
        AnimatedVisibility(visible = expanded) {
          Column(Modifier.padding(top = 4.dp, start = 2.dp)) {
            leg.intermediateStops.forEach { stop ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp),
              ) {
                Box(
                  Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(railColor),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = formatStationName(stop.name),
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
      leg.predictedDelay?.let { delay ->
        if (delay.p85Seconds >= 60) {
          Text(
            text = stringResource(R.string.planning_usually_delayed, formatDelay(delay.p85Seconds)),
            style = MaterialTheme.typography.bodySmall,
            color = delayMinorColor(),
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
