package it.iterapp.app.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Centered icon + message (+ optional hint and action) for a page slot's
 * empty and error states, so sibling states of the same slot always render
 * in one layout instead of scattered bare strings.
 */
@Composable
fun SheetStatusMessage(
  icon: ImageVector,
  message: String,
  modifier: Modifier = Modifier,
  hint: String? = null,
  action: (@Composable () -> Unit)? = null,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 32.dp, vertical = 32.dp),
  ) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(40.dp),
    )
    Text(
      text = message,
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    hint?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    action?.let {
      Spacer(Modifier.height(4.dp))
      it()
    }
  }
}
