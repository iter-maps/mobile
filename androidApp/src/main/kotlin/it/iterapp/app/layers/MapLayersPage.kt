package it.iterapp.app.layers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.core.settings.MapMode

/** Compact basemap picker (content-fit sheet anchor, transient page). */
@Composable
fun MapLayersPage(
  current: MapMode,
  onSelect: (MapMode) -> Unit,
  onContentHeight: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .onSizeChanged { onContentHeight(it.height) }
      .padding(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = stringResource(R.string.layers_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    color = if (selected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(vertical = 16.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = if (selected) {
          MaterialTheme.colorScheme.onSecondaryContainer
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
      )
      Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
  }
}
