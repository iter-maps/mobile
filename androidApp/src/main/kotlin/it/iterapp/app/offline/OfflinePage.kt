package it.iterapp.app.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.core.offline.OfflineArea

/**
 * Offline areas: download the current viewport as a bundle, list and delete
 * installed areas. Errors map the contract's codes to friendly copy.
 */
@Composable
fun OfflinePage(
  viewModel: OfflineViewModel,
  currentViewportBBox: () -> String?,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val areas by viewModel.areas.collectAsStateWithLifecycle()
  val download by viewModel.download.collectAsStateWithLifecycle()

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(title = stringResource(R.string.offline_title), onBack = onBack)

    Column(Modifier.padding(horizontal = 16.dp)) {
      when (val d = download) {
        is DownloadState.Downloading -> {
          val progress = d.total?.let { total ->
            if (total > 0) d.bytesRead.toFloat() / total else null
          }
          if (progress != null) {
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxWidth(),
            )
          } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          }
          Text(
            text = stringResource(
              R.string.offline_downloading,
              formatBytes(d.bytesRead),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        }
        DownloadState.Installing -> {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Text(
            text = stringResource(R.string.offline_installing),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        }
        is DownloadState.Failed -> {
          // Error plus a retry button, so a failure is never a dead end.
          Text(
            text = stringResource(
              when {
                OfflineViewModel.isTooLarge(d.code) -> R.string.offline_error_too_large
                OfflineViewModel.isInvalidArea(d.code) -> R.string.offline_error_invalid_area
                OfflineViewModel.isBusy(d.code) -> R.string.offline_error_busy
                else -> R.string.error_generic
              },
            ),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
          )
          DownloadButton(currentViewportBBox, viewModel, label = R.string.action_retry)
        }
        DownloadState.Idle -> DownloadButton(currentViewportBBox, viewModel)
      }
    }

    if (areas.isEmpty()) {
      Text(
        text = stringResource(R.string.offline_empty),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
      )
    } else {
      LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
        items(areas, key = { it.id }) { area ->
          AreaRow(area = area, onDelete = { viewModel.delete(area.id) })
        }
      }
    }
  }
}

@Composable
private fun DownloadButton(
  currentViewportBBox: () -> String?,
  viewModel: OfflineViewModel,
  label: Int = R.string.offline_download_current,
) {
  Button(
    onClick = { currentViewportBBox()?.let { viewModel.downloadArea(it) } },
    shape = MaterialTheme.shapes.medium,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Icon(
      Icons.Rounded.CloudDownload,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(text = stringResource(label))
  }
}

@Composable
private fun AreaRow(area: OfflineArea, onDelete: () -> Unit) {
  Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          text = areaLabel(area),
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = stringResource(R.string.offline_area_zoom, area.manifest.maxzoom) +
            "  ·  " + area.manifest.createdAt.take(10),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = stringResource(R.string.action_delete),
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

private fun areaLabel(area: OfflineArea): String {
  val bbox = area.manifest.bbox
  return if (bbox.size == 4) {
    "%.2f, %.2f → %.2f, %.2f".format(bbox[1], bbox[0], bbox[3], bbox[2])
  } else {
    area.id
  }
}

private fun formatBytes(bytes: Long): String = when {
  bytes < 1024 * 1024 -> "${bytes / 1024} KB"
  else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
