package it.iterapp.app.offline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.core.offline.OfflineArea
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
  var pendingDelete by remember { mutableStateOf<OfflineArea?>(null) }
  val listState = rememberLazyListState()

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(
      title = stringResource(R.string.offline_title),
      onBack = onBack,
      scrolledUnder = listState.canScrollBackward,
    )

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      // Keyed by state class: per-byte progress updates in place; only real
      // state changes (button → bar → saved) animate.
      AnimatedContent(
        targetState = download,
        contentKey = { it::class },
        transitionSpec = { (fadeIn() togetherWith fadeOut()).using(SizeTransform()) },
        label = "offline-download",
      ) { d ->
        when (d) {
          is DownloadState.Downloading -> {
            val progress = d.total?.let { total ->
              if (total > 0) d.bytesRead.toFloat() / total else null
            }
            Column {
              if (progress != null) {
                val animated by animateFloatAsState(progress, label = "dl-progress")
                LinearProgressIndicator(
                  progress = { animated },
                  modifier = Modifier.fillMaxWidth(),
                )
              } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              }
              Text(
                text = if (d.total != null) {
                  stringResource(
                    R.string.offline_downloading_of,
                    formatBytes(d.bytesRead),
                    formatBytes(d.total),
                  )
                } else {
                  stringResource(R.string.offline_downloading, formatBytes(d.bytesRead))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
              )
            }
          }
          DownloadState.Installing -> Column {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
              text = stringResource(R.string.offline_installing),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 8.dp),
            )
          }
          DownloadState.Done -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
          ) {
            Icon(
              Icons.Rounded.Check,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.offline_done),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          is DownloadState.Failed -> Column {
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
    }

    if (areas.isEmpty()) {
      Text(
        text = stringResource(R.string.offline_empty),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
      )
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
      ) {
        items(areas, key = { it.id }) { area ->
          AreaRow(
            area = area,
            onDelete = { pendingDelete = area },
            modifier = Modifier.animateItem(),
          )
        }
      }
    }
  }

  // Deleting throws away megabytes the user waited for — confirm first.
  pendingDelete?.let { area ->
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text(stringResource(R.string.offline_delete_title)) },
      text = { Text(areaLabel(area)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.delete(area.id)
            pendingDelete = null
          },
        ) {
          Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun DownloadButton(
  currentViewportBBox: () -> String?,
  viewModel: OfflineViewModel,
  label: Int = R.string.offline_download_current,
) {
  Button(
    onClick = { viewModel.downloadArea(currentViewportBBox()) },
    modifier = Modifier
      .fillMaxWidth()
      .height(52.dp),
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
private fun AreaRow(area: OfflineArea, onDelete: () -> Unit, modifier: Modifier = Modifier) {
  Surface(color = androidx.compose.ui.graphics.Color.Transparent, modifier = modifier) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
      val createdDate = remember(area.manifest.createdAt) { formatAreaDate(area.manifest.createdAt) }
      Column(Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.offline_downloaded_on, createdDate),
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = areaLabel(area) + "  ·  " +
            stringResource(R.string.offline_area_zoom, area.manifest.maxzoom),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          Icons.Rounded.Delete,
          contentDescription = stringResource(R.string.offline_delete_area_cd, areaLabel(area)),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/** Localized medium date from the manifest's ISO timestamp; raw prefix on failure. */
private fun formatAreaDate(createdAt: String): String = runCatching {
  LocalDate.parse(createdAt.take(10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}.getOrDefault(createdAt.take(10))

private fun areaLabel(area: OfflineArea): String {
  val bbox = area.manifest.bbox
  return if (bbox.size == 4) {
    // Locale-pinned: default-locale decimal commas collide with the commas
    // separating the coordinates on it-IT devices.
    String.format(Locale.US, "%.2f, %.2f – %.2f, %.2f", bbox[1], bbox[0], bbox[3], bbox[2])
  } else {
    area.id
  }
}

private fun formatBytes(bytes: Long): String = when {
  bytes < 1024 * 1024 -> "${bytes / 1024} KB"
  else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
