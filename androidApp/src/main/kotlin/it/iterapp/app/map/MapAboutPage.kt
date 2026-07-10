package it.iterapp.app.map

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.app.sheet.SheetSectionHeader

/**
 * "About the map": the map-data attribution the basemap legally requires
 * (OpenStreetMap under ODbL, OpenMapTiles schema), plus the open-source
 * credits — reached from the on-map "© OpenStreetMap" chip and from Settings
 * (ADR 0016), since the on-map "i" control was removed.
 */
@Composable
fun MapAboutPage(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  Column(modifier.fillMaxSize()) {
    val scroll = rememberScrollState()
    SheetPageHeader(
      title = stringResource(R.string.settings_attribution),
      onBack = onBack,
      scrolledUnder = scroll.canScrollBackward,
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scroll)
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
    ) {
      SheetSectionHeader(
        stringResource(R.string.attribution_data_title),
        Modifier.padding(top = 16.dp, bottom = 8.dp),
      )
      CreditRow(
        title = stringResource(R.string.attribution_osm),
        detail = stringResource(R.string.attribution_osm_desc),
        onClick = {
          context.startActivity(
            Intent(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright".toUri()),
          )
        },
      )
      CreditRow(
        title = stringResource(R.string.attribution_openmaptiles),
        detail = stringResource(R.string.attribution_openmaptiles_desc),
      )
      CreditRow(
        title = stringResource(R.string.attribution_render),
        detail = stringResource(R.string.attribution_render_desc),
      )

      SheetSectionHeader(
        stringResource(R.string.settings_licenses),
        Modifier.padding(top = 24.dp, bottom = 8.dp),
      )
      Text(
        text = stringResource(R.string.attribution_licenses_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CreditRow(
  title: String,
  detail: String,
  onClick: (() -> Unit)? = null,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(vertical = 10.dp),
  ) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
    Text(
      text = detail,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
