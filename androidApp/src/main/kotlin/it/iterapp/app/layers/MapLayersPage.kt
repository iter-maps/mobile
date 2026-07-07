package it.iterapp.app.layers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.core.settings.MapMode

/** Compact basemap picker (content-fit sheet anchor, transient page). */
@Composable
fun MapLayersPage(
  current: MapMode,
  onSelect: (MapMode) -> Unit,
  onContentHeight: (Int) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .onSizeChanged { onContentHeight(it.height) }
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = stringResource(R.string.layers_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onClose) {
        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
      }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.selectableGroup(),
    ) {
      LayerChoice(
        icon = Icons.Rounded.Map,
        label = stringResource(R.string.settings_map_standard),
        selected = current == MapMode.STANDARD,
        modifier = Modifier.weight(1f),
      ) { onSelect(MapMode.STANDARD) }
      LayerChoice(
        icon = Icons.Rounded.Tram,
        label = stringResource(R.string.settings_map_transit),
        selected = current == MapMode.TRANSIT,
        modifier = Modifier.weight(1f),
      ) { onSelect(MapMode.TRANSIT) }
    }
  }
}

@Composable
private fun LayerChoice(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val container by animateColorAsState(
    if (selected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    label = "layerContainer",
  )
  val content by animateColorAsState(
    if (selected) {
      MaterialTheme.colorScheme.onSecondaryContainer
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant
    },
    label = "layerContent",
  )
  Surface(
    selected = selected,
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    color = container,
    contentColor = content,
    // Container tones can land close together under dynamic color; the
    // border keeps the active basemap unambiguous. 1.5dp matches the
    // itinerary-card selection stroke.
    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    modifier = modifier.semantics { role = Role.RadioButton },
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(vertical = 16.dp),
    ) {
      Icon(icon, contentDescription = null, tint = content)
      Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
  }
}
