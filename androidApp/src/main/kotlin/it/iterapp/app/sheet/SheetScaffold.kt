package it.iterapp.app.sheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/**
 * Anchors of the universal sheet (ADR 0008): [Bottom] shows the page's peek,
 * [Half] leaves a map band above, [Full] is near-fullscreen, [Content] is a
 * content-fit height offered only when a page asks for it.
 */
enum class SheetAnchor { Bottom, Half, Full, Content }

private const val HALF_FRACTION = 0.57f
private const val FULL_FRACTION = 0.88f

/** One soft spring for every movement — drag release, fling, page change. */
private val SheetSpring: AnimationSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 140f)

@Stable
class SheetState(val draggable: AnchoredDraggableState<SheetAnchor>) {
  val currentAnchor: SheetAnchor get() = draggable.currentValue

  suspend fun animateTo(anchor: SheetAnchor) {
    if (draggable.anchors.hasPositionFor(anchor)) {
      draggable.animateTo(anchor, SheetSpring)
    }
  }
}

@Composable
fun rememberSheetState(initial: SheetAnchor = SheetAnchor.Bottom): SheetState {
  val draggable = remember { AnchoredDraggableState(initial) }
  return remember(draggable) { SheetState(draggable) }
}

/**
 * The draggable sheet over a persistent map. Non-scrollable page chrome drags
 * the sheet; inner lists are inert until the sheet is at [SheetAnchor.Full]
 * (see [sheetNestedScroll]). [onMapInset] continuously reports the visible
 * sheet height (clamped to the Half band) so the map can keep its focus
 * centered in the uncovered band.
 */
@Composable
fun SheetScaffold(
  state: SheetState,
  peekHeight: Dp,
  sheetContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  onMapInset: (Int) -> Unit = {},
  contentAnchorPx: Float? = null,
  openAnchor: SheetAnchor = SheetAnchor.Bottom,
  openKey: Any? = openAnchor,
  mapContent: @Composable BoxScope.() -> Unit,
) {
  val density = LocalDensity.current
  BoxWithConstraints(modifier.fillMaxSize()) {
    val fullPx = constraints.maxHeight.toFloat()
    val peekPx = with(density) { peekHeight.toPx() }
    val halfVisible = fullPx * HALF_FRACTION
    val fullVisible = fullPx * FULL_FRACTION
    val fullOffset = fullPx - fullVisible

    val anchors = remember(fullPx, peekPx, halfVisible, fullVisible, contentAnchorPx) {
      DraggableAnchors {
        SheetAnchor.Bottom at (fullPx - peekPx)
        SheetAnchor.Half at (fullPx - halfVisible)
        SheetAnchor.Full at fullOffset
        if (contentAnchorPx != null) {
          SheetAnchor.Content at (fullPx - contentAnchorPx.coerceIn(peekPx, fullVisible))
        }
      }
    }
    // Order matters: anchors are applied by the first effect, the open
    // animation below targets them afterwards — never a stale position.
    LaunchedEffect(anchors) { state.draggable.updateAnchors(anchors) }
    LaunchedEffect(openKey) {
      if (!state.draggable.anchors.hasPositionFor(openAnchor)) {
        snapshotFlow { state.draggable.anchors.hasPositionFor(openAnchor) }.first { it }
      }
      state.animateTo(openAnchor)
    }

    Box(Modifier.fillMaxSize(), content = mapContent)

    LaunchedEffect(state, fullPx, halfVisible) {
      snapshotFlow { state.draggable.offset }.collect { off ->
        if (off.isNaN()) return@collect
        val visible = fullPx - off
        onMapInset(visible.coerceAtMost(halfVisible).roundToInt())
      }
    }

    val nestedScroll = remember(state) { sheetNestedScroll(state.draggable) }

    Surface(
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      color = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .align(Alignment.TopStart)
        .fillMaxWidth()
        .height(with(density) { fullVisible.toDp() })
        .offset {
          val o = state.draggable.offset
          IntOffset(0, if (o.isNaN()) (fullPx - peekPx).roundToInt() else o.roundToInt())
        }
        .nestedScroll(nestedScroll)
        .anchoredDraggable(state.draggable, Orientation.Vertical),
    ) {
      Column(Modifier.fillMaxSize()) {
        DragHandle()
        // Inset on the content, not the Surface: anchor math stays untouched
        // and at Full every page bottoms out above the gesture pill.
        Box(
          Modifier
            .fillMaxWidth()
            .weight(1f)
            .windowInsetsPadding(WindowInsets.navigationBars),
        ) { sheetContent() }
      }
    }
  }
}

/**
 * Scroll↔sheet bridge: inner lists stay inert until the sheet is at Full;
 * below Full every drag moves the sheet. At Full, residual downward drag
 * (list already at its top) collapses the sheet; flings settle to the
 * closest anchor carrying their velocity into one spring.
 */
private fun sheetNestedScroll(
  state: AnchoredDraggableState<SheetAnchor>,
): NestedScrollConnection = object : NestedScrollConnection {

  private fun atFull(): Boolean {
    val o = state.offset
    return !o.isNaN() && o <= state.anchors.minPosition() + 1f
  }

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    if (source != NestedScrollSource.UserInput) return Offset.Zero
    return if (!atFull()) Offset(0f, state.dispatchRawDelta(available.y)) else Offset.Zero
  }

  override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
    return if (source == NestedScrollSource.UserInput) {
      Offset(0f, state.dispatchRawDelta(available.y))
    } else {
      Offset.Zero
    }
  }

  override suspend fun onPreFling(available: Velocity): Velocity {
    return if (!atFull() && !state.offset.isNaN()) {
      settleToAnchor(state, available.y)
      available
    } else {
      Velocity.Zero
    }
  }

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
    settleToAnchor(state, available.y)
    return available
  }
}

private suspend fun settleToAnchor(state: AnchoredDraggableState<SheetAnchor>, velocity: Float) {
  val current = state.offset
  if (current.isNaN()) return
  val projected = current + velocity * 0.12f
  val target = state.anchors.closestAnchor(projected) ?: state.anchors.closestAnchor(current) ?: return
  state.anchoredDrag(targetValue = target) { anchors, latestTarget ->
    val to = anchors.positionOf(latestTarget)
    if (!to.isNaN()) {
      val minP = anchors.minPosition()
      val maxP = anchors.maxPosition()
      animate(
        initialValue = state.offset,
        targetValue = to,
        initialVelocity = velocity,
        animationSpec = SheetSpring,
      ) { value, _ -> dragTo(value.coerceIn(minP, maxP)) }
    }
  }
}

@Composable
private fun DragHandle() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(width = 34.dp, height = 4.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
    )
  }
}
