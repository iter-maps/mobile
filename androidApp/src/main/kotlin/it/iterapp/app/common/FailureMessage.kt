package it.iterapp.app.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import it.iterapp.app.R
import it.iterapp.app.sheet.SheetStatusMessage
import it.iterapp.core.api.AppFailure

/**
 * The single "fancy" error surface (ADR 0015): every content-area failure
 * renders as one centered icon + title (+ hint + Retry) via
 * [SheetStatusMessage], so no screen shows a bare red string or a system
 * alert. The copy is honest about offline zones — [offlineZonesAvailable]
 * switches the no-connection hint between "downloaded maps still work" and
 * "reconnect to continue".
 */
@Composable
fun FailureMessage(
  failure: AppFailure,
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
  offlineZonesAvailable: Boolean = false,
) {
  val visual = failure.visual(offlineZonesAvailable)
  SheetStatusMessage(
    icon = visual.icon,
    message = stringResource(visual.titleRes),
    modifier = modifier,
    hint = visual.hintRes?.let { stringResource(it) },
    action = if (onRetry != null && visual.retryable) {
      { FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) } }
    } else {
      null
    },
  )
}

/** Icon + copy + retry-affordance for a failure category. */
data class FailureVisual(
  val icon: ImageVector,
  val titleRes: Int,
  val hintRes: Int?,
  val retryable: Boolean,
)

private fun AppFailure.visual(offlineZonesAvailable: Boolean): FailureVisual = when (this) {
  AppFailure.NoConnection -> FailureVisual(
    icon = Icons.Rounded.WifiOff,
    titleRes = R.string.error_offline_title,
    hintRes = if (offlineZonesAvailable) {
      R.string.error_offline_zones_hint
    } else {
      R.string.error_offline_hint
    },
    retryable = true,
  )

  is AppFailure.ServerUnreachable, is AppFailure.Server -> FailureVisual(
    icon = Icons.Rounded.CloudOff,
    titleRes = R.string.error_unreachable_title,
    hintRes = R.string.error_unreachable_hint,
    retryable = true,
  )

  AppFailure.Timeout -> FailureVisual(
    icon = Icons.Rounded.HourglassEmpty,
    titleRes = R.string.error_timeout_title,
    hintRes = R.string.error_timeout_hint,
    retryable = true,
  )

  is AppFailure.RateLimited -> FailureVisual(
    icon = Icons.Rounded.HourglassEmpty,
    titleRes = R.string.error_busy_title,
    hintRes = R.string.error_busy_hint,
    retryable = true,
  )

  AppFailure.NotFound -> FailureVisual(
    icon = Icons.Rounded.SearchOff,
    titleRes = R.string.error_not_found_title,
    hintRes = null,
    retryable = false,
  )

  is AppFailure.OfflineBundle, is AppFailure.BadRequest, AppFailure.Unknown -> FailureVisual(
    icon = Icons.Rounded.ErrorOutline,
    titleRes = R.string.error_generic,
    hintRes = R.string.error_generic_hint,
    retryable = true,
  )
}
