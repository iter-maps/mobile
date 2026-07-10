package it.iterapp.app.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
  onOpenOffline: () -> Unit,
  onOpenAttribution: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
  val mapMode by viewModel.mapMode.collectAsStateWithLifecycle()
  val origin by viewModel.gatewayOrigin.collectAsStateWithLifecycle()
  var originField by rememberSaveable(origin) { mutableStateOf(origin) }

  val context = LocalContext.current
  val versionName = remember {
    runCatching {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
  }
  val hasLocation = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION,
  ) == PackageManager.PERMISSION_GRANTED

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
      // ── Appearance (essential — kept first) ──
      SettingsGroup(stringResource(R.string.settings_appearance)) {
        SegmentedGroup {
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
        }
        // Dynamic color only exists on Android 12+ (Theme.kt gates on S);
        // below that the row would be a lying control.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .toggleable(
                value = dynamicColor,
                role = Role.Switch,
                onValueChange = viewModel::setDynamicColor,
              )
              .padding(horizontal = 16.dp, vertical = 12.dp),
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
      }

      // ── Map & location ──
      SettingsGroup(stringResource(R.string.settings_map_location)) {
        SegmentedGroup {
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
        }
        SettingsRow(
          title = stringResource(R.string.settings_offline),
          onClick = onOpenOffline,
        )
      }

      // ── About & legal ──
      SettingsGroup(stringResource(R.string.settings_about)) {
        SettingsRow(
          title = stringResource(R.string.settings_version),
          value = versionName,
        )
        SettingsRow(
          title = stringResource(R.string.settings_attribution),
          onClick = onOpenAttribution,
        )
      }

      // ── Help ──
      SettingsGroup(stringResource(R.string.settings_help)) {
        SettingsRow(
          title = stringResource(R.string.settings_replay_onboarding),
          subtitle = stringResource(R.string.settings_replay_onboarding_desc),
          onClick = viewModel::replayOnboarding,
        )
        SettingsRow(
          title = stringResource(R.string.settings_permission),
          value = if (hasLocation) stringResource(R.string.settings_permission_granted) else null,
          subtitle = if (hasLocation) null else stringResource(R.string.settings_permission_denied),
          onClick = if (hasLocation) {
            null
          } else {
            {
              context.startActivity(
                Intent(
                  AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                  Uri.fromParts("package", context.packageName, null),
                ),
              )
            }
          },
        )
      }

      // ── Advanced (dev field, de-emphasized at the bottom) ──
      SettingsGroup(stringResource(R.string.settings_advanced)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
  }
}

/** A titled, card-grouped settings section. */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
  SheetSectionHeader(title, Modifier.padding(top = 24.dp, bottom = 10.dp))
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(content = content)
  }
}

/** Segmented controls carry their own container, so pad them inside the card. */
@Composable
private fun SegmentedGroup(content: @Composable () -> Unit) {
  Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
}

/** A single settings entry: title (+ subtitle) with an optional trailing value,
 *  clickable when [onClick] is set. */
@Composable
private fun SettingsRow(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  value: String? = null,
  onClick: (() -> Unit)? = null,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(horizontal = 16.dp, vertical = 14.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      subtitle?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    value?.let {
      Spacer(Modifier.width(16.dp))
      Text(
        text = it,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
