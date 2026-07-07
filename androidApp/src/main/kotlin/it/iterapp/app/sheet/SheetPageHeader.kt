package it.iterapp.app.sheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.iterapp.app.R

/**
 * Standard back-arrow + title header for sheet pages. Pass [scrolledUnder]
 * from the page's scroll state so the header tints subtly while content
 * scrolls beneath it (the sheet analog of scrolledUnderElevation).
 */
@Composable
fun SheetPageHeader(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  scrolledUnder: Boolean = false,
  trailing: @Composable () -> Unit = {},
) {
  val containerColor by animateColorAsState(
    targetValue = if (scrolledUnder) {
      MaterialTheme.colorScheme.surfaceContainer
    } else {
      MaterialTheme.colorScheme.surface
    },
    animationSpec = tween(150),
    label = "sheetHeaderScrolledUnder",
  )
  Surface(color = containerColor, modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(
          Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.action_back),
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(start = 4.dp),
      )
      trailing()
    }
  }
}
