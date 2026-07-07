package it.iterapp.app.planning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.iterapp.app.R
import it.iterapp.app.common.formatRouterClock
import it.iterapp.app.common.routerHourMinute
import it.iterapp.app.common.routerTimeNextAt

/**
 * "Leave now / Depart at HH:mm / Arrive by HH:mm" chip; opens
 * [DepartureTimeDialog]. Tinted while a custom time is active so the page
 * never silently plans for a non-obvious moment.
 */
@Composable
fun DepartureTimeChip(
  departureMs: Long?,
  arriveBy: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val custom = departureMs != null
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = if (custom) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    contentColor = if (custom) {
      MaterialTheme.colorScheme.onSecondaryContainer
    } else {
      MaterialTheme.colorScheme.onSurface
    },
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
      Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        // Router-zone clock: the label must echo exactly what was picked in
        // the dialog, which also works on the router's wall clock.
        text = when {
          departureMs == null -> stringResource(R.string.planning_now)
          arriveBy ->
            stringResource(R.string.planning_arrive_by_time, formatRouterClock(departureMs))
          else ->
            stringResource(R.string.planning_depart_at_time, formatRouterClock(departureMs))
        },
        style = MaterialTheme.typography.labelLarge,
      )
      Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
    }
  }
}

/**
 * Depart-at / arrive-by picker on the router's wall clock (the plan endpoint
 * interprets times there — see planDateTime); a time already past rolls to
 * its next occurrence. "Leave now" clears back to live planning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartureTimeDialog(
  initialMs: Long?,
  initialArriveBy: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (departureMs: Long?, arriveBy: Boolean) -> Unit,
) {
  val (initHour, initMinute) = routerHourMinute(initialMs ?: System.currentTimeMillis())
  val timeState = rememberTimePickerState(
    initialHour = initHour,
    initialMinute = initMinute,
    is24Hour = true,
  )
  var arriveBy by remember { mutableStateOf(initialArriveBy) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = !arriveBy,
            onClick = { arriveBy = false },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
          ) {
            Text(stringResource(R.string.planning_depart_at))
          }
          SegmentedButton(
            selected = arriveBy,
            onClick = { arriveBy = true },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
          ) {
            Text(stringResource(R.string.planning_arrive_by))
          }
        }
        Spacer(Modifier.height(20.dp))
        TimePicker(state = timeState)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          TextButton(onClick = { onConfirm(null, false) }) {
            Text(stringResource(R.string.planning_now))
          }
          Spacer(Modifier.weight(1f))
          TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_cancel))
          }
          TextButton(
            onClick = { onConfirm(routerTimeNextAt(timeState.hour, timeState.minute), arriveBy) },
          ) {
            Text(stringResource(R.string.action_ok))
          }
        }
      }
    }
  }
}
