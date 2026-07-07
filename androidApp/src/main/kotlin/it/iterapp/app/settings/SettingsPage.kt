package it.iterapp.app.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetPageHeader
import it.iterapp.app.sheet.SheetSectionHeader
import it.iterapp.core.settings.MapMode
import it.iterapp.core.settings.ThemeMode

@Composable
fun SettingsPage(
  viewModel: SettingsViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
  val mapMode by viewModel.mapMode.collectAsStateWithLifecycle()
  val origin by viewModel.gatewayOrigin.collectAsStateWithLifecycle()
  var originField by rememberSaveable(origin) { mutableStateOf(origin) }

  Column(modifier.fillMaxSize()) {
    val scroll = rememberScrollState()
    SheetPageHeader(
      title = stringResource(R.string.settings_title),
      onBack = onBack,
      scrolledUnder = scroll.canScrollBackward,
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scroll)
        .padding(horizontal = 16.dp)
        // After verticalScroll so both scroll with the content — imePadding
        // is what lets bring-into-view lift the gateway field over the IME.
        .padding(bottom = 24.dp)
        .imePadding(),
    ) {
      SectionLabel(stringResource(R.string.settings_theme))
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = themeMode == mode,
            onClick = { viewModel.setThemeMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
          ) {
            Text(
              stringResource(
                when (mode) {
                  ThemeMode.SYSTEM -> R.string.settings_theme_system
                  ThemeMode.LIGHT -> R.string.settings_theme_light
                  ThemeMode.DARK -> R.string.settings_theme_dark
                },
              ),
            )
          }
        }
      }

      // Dynamic color only exists on Android 12+ (Theme.kt gates on S);
      // below that the row would be a lying control.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .toggleable(
              value = dynamicColor,
              role = Role.Switch,
              onValueChange = viewModel::setDynamicColor,
            )
            .padding(vertical = 12.dp),
        ) {
          Column(Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.settings_dynamic_color),
              style = MaterialTheme.typography.bodyLarge,
            )
            Text(
              text = stringResource(R.string.settings_dynamic_color_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.width(16.dp))
          // Passive indicator: the whole row is the (TalkBack-labeled) toggle.
          Switch(checked = dynamicColor, onCheckedChange = null)
        }
      }

      SectionLabel(stringResource(R.string.settings_map_style))
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        MapMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = mapMode == mode,
            onClick = { viewModel.setMapMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(index, MapMode.entries.size),
          ) {
            Text(
              stringResource(
                when (mode) {
                  MapMode.STANDARD -> R.string.settings_map_standard
                  MapMode.TRANSIT -> R.string.settings_map_transit
                },
              ),
            )
          }
        }
      }

      SectionLabel(stringResource(R.string.settings_server))
      Text(
        text = stringResource(R.string.settings_server_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
      )
      OutlinedTextField(
        value = originField,
        onValueChange = { originField = it },
        label = { Text(stringResource(R.string.settings_server_origin)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Uri,
          autoCorrectEnabled = false,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (originField.trim().trimEnd('/') != origin) {
              viewModel.setGatewayOrigin(originField)
            }
          },
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = { viewModel.setGatewayOrigin(originField) },
        enabled = originField.trim().trimEnd('/') != origin,
        modifier = Modifier
          .align(Alignment.End)
          .padding(top = 10.dp),
      ) {
        Text(stringResource(R.string.settings_server_save))
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  SheetSectionHeader(text, Modifier.padding(top = 24.dp, bottom = 10.dp))
}
