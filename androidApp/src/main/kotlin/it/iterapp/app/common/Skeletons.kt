package it.iterapp.app.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * One shared pulse per screen, so every skeleton breathes together. Returned
 * as [State] and consumed in the draw phase ([SkeletonBlock] takes a lambda):
 * reading the value during composition would recompose the whole screen on
 * every animation frame for as long as skeletons are visible.
 */
@Composable
fun rememberSkeletonPulse(): State<Float> {
  val transition = rememberInfiniteTransition(label = "skeleton-pulse")
  return transition.animateFloat(
    initialValue = 1f,
    targetValue = 0.4f,
    animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
    label = "skeleton-alpha",
  )
}

/**
 * Solid placeholder block; size it with [modifier] and pass the shared
 * pulse as `pulse::value` so the animation stays in the draw phase.
 */
@Composable
fun SkeletonBlock(
  modifier: Modifier = Modifier,
  alpha: () -> Float = { 1f },
  shape: Shape = MaterialTheme.shapes.small,
  color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
  Box(
    modifier
      .clip(shape)
      .drawBehind { drawRect(color = color, alpha = alpha()) },
  )
}
