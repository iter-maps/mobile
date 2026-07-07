package it.iterapp.app.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import it.iterapp.app.R

/**
 * The one search-pill spec (52dp extraLarge pill on surfaceContainerHighest,
 * 20dp lead icon, 10dp gap): [SheetSearchField] is the live text field,
 * [SearchPillButton] the tap-to-open stand-in on Home. Shared so the pill
 * doesn't shift by a few dp when navigating between surfaces.
 */
@Composable
fun SheetSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  focusRequester: FocusRequester,
  modifier: Modifier = Modifier,
  isLoading: Boolean = false,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  Surface(
    // The whole pill focuses the field; show() covers the field-already-
    // focused-but-IME-dismissed case where requestFocus() is a no-op.
    onClick = {
      focusRequester.requestFocus()
      keyboard?.show()
    },
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = modifier.height(52.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 16.dp, end = 4.dp),
    ) {
      Icon(
        Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.width(10.dp))
      BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        // The placeholder lives in the decoration box so the field carries it
        // as its semantic label and the tap target spans the pill's height.
        decorationBox = { innerTextField ->
          Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
              Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
            }
            innerTextField()
          }
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .focusRequester(focusRequester),
      )
      // Fixed trailing slot: the text area never reflows when the spinner or
      // clear button appears, and the clear button keeps a 48dp target.
      Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        when {
          isLoading -> CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
          )
          query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) {
            Icon(
              Icons.Rounded.Close,
              contentDescription = stringResource(R.string.action_clear),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

/** Non-editing twin of [SheetSearchField]: Home's tap-to-search pill. */
@Composable
fun SearchPillButton(hint: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = modifier.height(52.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Icon(
        Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Spacer(Modifier.width(10.dp))
      Text(
        text = hint,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}
