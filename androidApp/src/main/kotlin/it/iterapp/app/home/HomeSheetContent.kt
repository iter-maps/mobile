package it.iterapp.app.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.app.common.SearchPillButton
import it.iterapp.app.common.formatDistance
import it.iterapp.app.common.formatStationName
import it.iterapp.app.sheet.SheetSectionHeader
import it.iterapp.app.sheet.sheetSpring
import it.iterapp.core.model.SearchResult

/**
 * Home page of the universal sheet (Apple-Maps essential style, ADR 0008):
 * big search pill + avatar on top, quick pill-chips, then Recent and Nearby
 * sections. At peek only search + chips are visible; expanding reveals the
 * sections.
 */
@Composable
fun HomeSheetContent(
  recentPlaces: List<SearchResult>,
  nearbyStations: List<NearbyStation>,
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
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          SearchPillButton(
            hint = stringResource(R.string.home_search_hint),
            onClick = onSearch,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(10.dp))
          AvatarButton(onClick = onSettings)
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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
      SheetSectionHeader(stringResource(R.string.home_section_recents))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        recentPlaces.forEach { place ->
          QuickChip(
            icon = Icons.Rounded.History,
            label = place.name,
            filled = false,
            onClick = { onRecent(place) },
          )
        }
      }
    }

    SheetSectionHeader(stringResource(R.string.home_section_nearby))
    if (nearbyStations.isEmpty()) {
      Text(
        text = stringResource(R.string.home_nearby_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        nearbyStations.forEach { nearby ->
          StationRow(nearby = nearby, onClick = { onStation(nearby) })
        }
      }
    }

    Spacer(Modifier.height(8.dp))
  }
}

@Composable
private fun AvatarButton(onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = Modifier.size(48.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        Icons.Rounded.Person,
        contentDescription = stringResource(R.string.home_profile_cd),
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
      )
    }
  }
}

@Composable
private fun StationRow(nearby: NearbyStation, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Rounded.Train,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
      Column(Modifier.weight(1f)) {
        Text(
          text = formatStationName(nearby.station.name),
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = formatDistance(nearby.distanceMeters),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
