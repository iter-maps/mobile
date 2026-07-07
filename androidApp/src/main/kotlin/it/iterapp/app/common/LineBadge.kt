package it.iterapp.app.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.app.ui.theme.lineColor
import it.iterapp.app.ui.theme.onLineColor
import it.iterapp.core.model.Leg
import it.iterapp.core.model.LegMode

/** Compact colored pill with the line's short name (metro "B", bus "170"…). */
@Composable
fun LineBadge(leg: Leg, modifier: Modifier = Modifier) {
  val background = lineColor(leg.routeColor, leg.mode)
  Text(
    text = leg.routeShortName ?: modeLabel(leg.mode),
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.Bold,
    color = onLineColor(background),
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(background)
      .padding(horizontal = 8.dp, vertical = 3.dp),
  )
}

/** Localized fallback when the feed carries no short name. */
@Composable
private fun modeLabel(mode: LegMode): String = stringResource(
  when (mode) {
    LegMode.BUS -> R.string.line_mode_bus
    LegMode.SUBWAY -> R.string.line_mode_subway
    LegMode.TRAM -> R.string.line_mode_tram
    LegMode.RAIL -> R.string.line_mode_rail
    LegMode.FERRY -> R.string.line_mode_ferry
    LegMode.FUNICULAR -> R.string.line_mode_funicular
    LegMode.GONDOLA -> R.string.line_mode_gondola
    else -> R.string.line_mode_other
  },
)
