package it.iterapp.app.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Accessible
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.core.model.SearchResult

/**
 * Place card: name, address, primary Directions action; train stations offer
 * the live board; Wikimedia enrichment (summary, facets) appears when it
 * resolves — the card never waits for it.
 */
@Composable
fun PlaceDetailPage(
  place: SearchResult,
  viewModel: PlaceDetailViewModel,
  onBack: () -> Unit,
  onDirections: (SearchResult) -> Unit,
  onTrainBoard: (SearchResult) -> Unit,
  modifier: Modifier = Modifier,
) {
  val enriched by viewModel.enriched.collectAsStateWithLifecycle()

  LaunchedEffect(place.id) { viewModel.load(place) }

  Column(modifier.fillMaxSize()) {
    SheetPageHeader(title = place.name, onBack = onBack)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      place.detail?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Button(
        onClick = { onDirections(place) },
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        shape = MaterialTheme.shapes.medium,
      ) {
        Icon(Icons.Rounded.Directions, contentDescription = null)
        Text(
          text = stringResource(R.string.place_directions),
          modifier = Modifier.padding(start = 8.dp),
        )
      }

      if (place.isTrainStation) {
        OutlinedButton(
          onClick = { onTrainBoard(place) },
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
        ) {
          Icon(Icons.Rounded.Train, contentDescription = null)
          Text(
            text = stringResource(R.string.place_train_board),
            modifier = Modifier.padding(start = 8.dp),
          )
        }
      }

      enriched?.let { placeInfo ->
        placeInfo.summary?.let { summary ->
          Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
        placeInfo.facets?.let { facets ->
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            facets.website?.let { FacetRow(Icons.Rounded.Language, it) }
            facets.phone?.let { FacetRow(Icons.Rounded.Phone, it) }
            facets.openingHours?.let { FacetRow(Icons.Rounded.Schedule, it) }
            facets.wheelchair?.let { FacetRow(Icons.AutoMirrored.Rounded.Accessible, it) }
          }
        }
        placeInfo.image?.attribution?.let { attribution ->
          Text(
            text = stringResource(R.string.place_photo_attribution, attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun FacetRow(icon: ImageVector, value: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(end = 10.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 2,
    )
  }
}
