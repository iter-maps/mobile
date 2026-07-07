package it.iterapp.app.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** One shared pulse per screen, so every skeleton breathes together. */
@Composable
fun rememberSkeletonPulse(): Float {
  val transition = rememberInfiniteTransition(label = "skeleton-pulse")
  val alpha by transition.animateFloat(
    initialValue = 1f,
    targetValue = 0.4f,
    animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
    label = "skeleton-alpha",
  )
  return alpha
}

/**
 * Solid placeholder block; size it with [modifier] and pass the shared
 * [rememberSkeletonPulse] value as [alpha].
 */
@Composable
fun SkeletonBlock(
  modifier: Modifier = Modifier,
  alpha: Float = 1f,
  shape: Shape = MaterialTheme.shapes.small,
  color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
  Box(
    modifier
      .clip(shape)
      .background(color.copy(alpha = color.alpha * alpha)),
  )
}
