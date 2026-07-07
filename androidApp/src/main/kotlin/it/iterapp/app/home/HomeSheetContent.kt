package it.iterapp.app.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.app.common.IconListRow
import it.iterapp.app.common.SearchPillButton
import it.iterapp.app.common.SkeletonBlock
import it.iterapp.app.common.formatDistance
import it.iterapp.app.common.formatStationName
import it.iterapp.app.common.placeIcon
import it.iterapp.app.common.rememberSkeletonPulse
import it.iterapp.app.sheet.SheetSectionHeader
import it.iterapp.app.sheet.sheetSpring
import it.iterapp.core.model.SearchResult

/**
 * Home page of the universal sheet (Apple-Maps essential style, ADR 0008):
 * big search pill + settings button on top, quick pill-chips, then Recent and
 * Nearby sections as icon-led rows sharing the search-result anatomy. At peek
 * only search + chips are visible; expanding reveals the sections.
 */
@Composable
fun HomeSheetContent(
  recentPlaces: List<SearchResult>,
  nearbyState: NearbyUiState,
  onSearch: () -> Unit,
  onDirections: () -> Unit,
  onTrains: () -> Unit,
  onOffline: () -> Unit,
  onSettings: () -> Unit,
  onRecent: (SearchResult) -> Unit,
  onStation: (NearbyStation) -> Unit,
  modifier: Modifier = Modifier,
  onPeekContentHeight: (Int) -> Unit = {},
  collapsed: Boolean = true,
  gestureClearance: Dp = 0.dp,
) {
  Column(
    // Horizontal padding lives on each section, not the root: scrolling chip
    // rows must bleed to the true sheet edge instead of clipping at 16dp.
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // The measured search+chips block defines the sheet's peek height; the
    // animated clearance keeps it clear of the gesture bar while collapsed.
    Column {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .onSizeChanged { onPeekContentHeight(it.height) },
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
          SearchPillButton(
            hint = stringResource(R.string.home_search_hint),
            onClick = onSearch,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(10.dp))
          SettingsButton(onClick = onSettings)
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          QuickChip(
            icon = Icons.Rounded.Directions,
            label = stringResource(R.string.home_quick_planning),
            filled = true,
            onClick = onDirections,
          )
          QuickChip(
            icon = Icons.Rounded.Train,
            label = stringResource(R.string.home_quick_boards),
            filled = false,
            onClick = onTrains,
          )
          QuickChip(
            icon = Icons.Rounded.CloudDownload,
            label = stringResource(R.string.home_quick_offline),
            filled = false,
            onClick = onOffline,
          )
        }
      }
      val clearance by animateDpAsState(
        if (collapsed) gestureClearance else 0.dp,
        animationSpec = sheetSpring(),
        label = "gestureClearance",
      )
      Spacer(Modifier.height(clearance))
    }

    HorizontalDivider()

    if (recentPlaces.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SheetSectionHeader(
          stringResource(R.string.home_section_recents),
          Modifier.padding(start = 16.dp, top = 10.dp),
        )
        Column(Modifier.padding(horizontal = 4.dp)) {
          recentPlaces.take(4).forEach { place ->
            IconListRow(
              icon = placeIcon(place),
              title = place.name,
              subtitle = place.detail,
              onClick = { onRecent(place) },
              trailing = {
                Icon(
                  Icons.Rounded.History,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SheetSectionHeader(
        stringResource(R.string.home_section_nearby),
        Modifier.padding(start = 16.dp, top = 10.dp),
      )
      when (nearbyState) {
        NearbyUiState.NoPermission -> NearbyCaption(stringResource(R.string.home_nearby_empty))
        NearbyUiState.Unavailable -> NearbyCaption(stringResource(R.string.home_nearby_unavailable))
        NearbyUiState.Locating -> {
          val locating = stringResource(R.string.home_nearby_locating)
          val pulse = rememberSkeletonPulse()
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .padding(horizontal = 16.dp)
              .semantics { contentDescription = locating },
          ) {
            repeat(3) {
              SkeletonBlock(
                Modifier
                  .fillMaxWidth()
                  .height(56.dp),
                pulse,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              )
            }
          }
        }
        is NearbyUiState.Loaded -> {
          if (nearbyState.stations.isEmpty()) {
            NearbyCaption(stringResource(R.string.home_nearby_none))
          } else {
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(horizontal = 16.dp),
            ) {
              nearbyState.stations.forEach { nearby ->
                IconListRow(
                  icon = Icons.Rounded.Train,
                  title = formatStationName(nearby.station.name),
                  subtitle = formatDistance(nearby.distanceMeters),
                  onClick = { onStation(nearby) },
                  rowColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                  iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                  trailing = {
                    Icon(
                      Icons.Rounded.ChevronRight,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(20.dp),
                    )
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                )
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(8.dp))
  }
}

@Composable
private fun NearbyCaption(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 16.dp),
  )
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = Modifier.size(48.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        Icons.Rounded.Settings,
        contentDescription = stringResource(R.string.home_settings_cd),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

@Composable
private fun QuickChip(
  icon: ImageVector,
  label: String,
  filled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = if (filled) {
      MaterialTheme.colorScheme.primaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    contentColor = if (filled) {
      MaterialTheme.colorScheme.onPrimaryContainer
    } else {
      MaterialTheme.colorScheme.onSurface
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 180.dp),
      )
    }
  }
}
