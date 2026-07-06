package it.iterapp.app.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPage

/** Home page: search pill + quick-action chips. */
@Composable
fun HomeSheetContent(
  onNavigate: (SheetPage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    SearchPill(onClick = { onNavigate(SheetPage.Search) })
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      QuickAction(
        icon = Icons.Rounded.Directions,
        label = stringResource(R.string.home_quick_planning),
        modifier = Modifier.weight(1f),
      ) { onNavigate(SheetPage.Planning) }
      QuickAction(
        icon = Icons.Rounded.Train,
        label = stringResource(R.string.home_quick_boards),
        modifier = Modifier.weight(1f),
      ) { onNavigate(SheetPage.TrainBoard()) }
      QuickAction(
        icon = Icons.Rounded.CloudDownload,
        label = stringResource(R.string.home_quick_offline),
        modifier = Modifier.weight(1f),
      ) { onNavigate(SheetPage.Offline) }
      QuickAction(
        icon = Icons.Rounded.Settings,
        label = stringResource(R.string.home_quick_settings),
        modifier = Modifier.weight(1f),
      ) { onNavigate(SheetPage.Settings) }
    }
  }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp),
    ) {
      Icon(
        Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = stringResource(R.string.home_search_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp),
      )
    }
  }
}

@Composable
private fun QuickAction(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier.clickable(onClick = onClick),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(vertical = 14.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
      )
    }
  }
}
