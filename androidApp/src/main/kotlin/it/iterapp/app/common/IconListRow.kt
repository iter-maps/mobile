package it.iterapp.app.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The app's one list-row anatomy: 40dp icon circle, medium title with an
 * optional supporting line, optional trailing slot. Used by search results,
 * station pickers and the home lists so the same object never renders with
 * three different geometries.
 */
@Composable
fun IconListRow(
  icon: ImageVector,
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  trailing: (@Composable () -> Unit)? = null,
  rowColor: Color = Color.Transparent,
  rowShape: Shape = MaterialTheme.shapes.medium,
  iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) {
  Surface(
    onClick = onClick,
    color = rowColor,
    shape = rowShape,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(contentPadding),
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(iconContainerColor, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
      }
      Column(
        Modifier
          .weight(1f)
          .padding(start = 14.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      trailing?.let {
        Spacer(Modifier.width(8.dp))
        it()
      }
    }
  }
}
