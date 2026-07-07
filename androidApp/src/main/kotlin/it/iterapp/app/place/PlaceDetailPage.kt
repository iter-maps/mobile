package it.iterapp.app.place

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.core.model.SearchResult
import it.iterapp.core.wire.Place

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
    val scroll = rememberScrollState()
    SheetPageHeader(
      title = place.name,
      onBack = onBack,
      scrolledUnder = scroll.canScrollBackward,
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scroll)
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp),
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
          .height(52.dp),
      ) {
        Icon(
          Icons.Rounded.Directions,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.place_directions))
      }

      if (place.isTrainStation) {
        OutlinedButton(
          onClick = { onTrainBoard(place) },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        ) {
          Icon(
            Icons.Rounded.Train,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(R.string.place_train_board))
        }
      }

      // Enrichment eases in as one block and stays composed once resolved,
      // so a late clear can't snap 300dp of content out mid-read. Gated on
      // the source place id: a stale flow value from the previous place
      // (map-tap replaces the page before load() nulls it) composes nothing.
      val current = enriched?.takeIf { it.first == place.id }?.second
      var retained by remember { mutableStateOf<Place?>(null) }
      current?.let { retained = it }
      AnimatedVisibility(
        visible = current != null,
        enter = fadeIn(tween(250)) + expandVertically(),
      ) {
        retained?.let { placeInfo ->
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val imageUrl = viewModel.imageUrl(placeInfo)
            if (imageUrl != null) {
              var imageState by remember(imageUrl) {
                mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
              }
              // A failed load collapses the slot instead of leaving a void.
              if (imageState !is AsyncImagePainter.State.Error) {
                AsyncImage(
                  model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                  contentDescription = placeInfo.name,
                  contentScale = ContentScale.Crop,
                  onState = { imageState = it },
                  modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
              }
              // Attribution is only meaningful once the image is actually shown.
              if (imageState is AsyncImagePainter.State.Success) {
                placeInfo.image?.attribution?.let { attribution ->
                  Text(
                    text = stringResource(R.string.place_photo_attribution, attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            placeInfo.summary?.let { summary ->
              Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
            placeInfo.facets?.let { facets ->
              val context = LocalContext.current
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                facets.website?.let { site ->
                  FacetRow(
                    icon = Icons.Rounded.Language,
                    label = R.string.place_website,
                    value = site,
                    onClick = {
                      val url = if ("://" in site) site else "https://$site"
                      runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                      }
                    },
                  )
                }
                facets.phone?.let { phone ->
                  FacetRow(
                    icon = Icons.Rounded.Phone,
                    label = R.string.place_phone,
                    value = phone,
                    onClick = {
                      runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                      }
                    },
                  )
                }
                facets.openingHours?.let {
                  FacetRow(icon = Icons.Rounded.Schedule, label = R.string.place_hours, value = it)
                }
                facets.wheelchair?.let { raw ->
                  FacetRow(
                    icon = Icons.AutoMirrored.Rounded.Accessible,
                    label = R.string.place_wheelchair,
                    value = when (raw.lowercase()) {
                      "yes" -> stringResource(R.string.place_wheelchair_yes)
                      "limited" -> stringResource(R.string.place_wheelchair_limited)
                      "no" -> stringResource(R.string.place_wheelchair_no)
                      else -> raw
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * One facet line: labeled for TalkBack (the wire values alone read as bare
 * words), tappable when the value has a natural action (website, phone).
 */
@Composable
private fun FacetRow(
  icon: ImageVector,
  @StringRes label: Int,
  value: String,
  onClick: (() -> Unit)? = null,
) {
  val base = Modifier
    .fillMaxWidth()
    .semantics(mergeDescendants = true) {}
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = if (onClick != null) {
      base
        .clip(MaterialTheme.shapes.small)
        .clickable(onClick = onClick, role = Role.Button)
        .heightIn(min = 48.dp)
    } else {
      base.heightIn(min = 40.dp)
    },
  ) {
    Icon(
      icon,
      contentDescription = stringResource(label),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = if (onClick != null) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurface
      },
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
