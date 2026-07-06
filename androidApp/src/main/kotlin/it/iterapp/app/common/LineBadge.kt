package it.iterapp.app.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.iterapp.app.ui.theme.lineColor
import it.iterapp.app.ui.theme.onLineColor
import it.iterapp.core.model.Leg

/** Compact colored pill with the line's short name (metro "B", bus "170"…). */
@Composable
fun LineBadge(leg: Leg, modifier: Modifier = Modifier) {
  val background = lineColor(leg.routeColor, leg.mode)
  Text(
    text = leg.routeShortName ?: leg.mode.name.take(3),
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.Bold,
    color = onLineColor(background),
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(background)
      .padding(horizontal = 8.dp, vertical = 3.dp),
  )
}
