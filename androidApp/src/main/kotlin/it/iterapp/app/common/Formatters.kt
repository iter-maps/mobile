package it.iterapp.app.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** `14:05` in the device locale's 24h clock. */
fun formatClock(epochMs: Long): String =
  SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

/** `7 min` / `1 h 12 min`. */
fun formatDuration(seconds: Long): String {
  val minutes = (seconds / 60).toInt()
  return when {
    minutes < 60 -> "$minutes min"
    else -> "${minutes / 60} h ${minutes % 60} min"
  }
}

/** `350 m` / `2,4 km` style distance. */
fun formatDistance(meters: Double): String = when {
  meters < 950 -> "${((meters / 10).roundToInt() * 10)} m"
  else -> String.format(Locale.getDefault(), "%.1f km", meters / 1000)
}

/** Seconds → compact `+3 min` / `+45 s` delay label. */
fun formatDelay(seconds: Double): String {
  val s = seconds.roundToInt()
  return if (s >= 90) "${(s / 60.0).roundToInt()} min" else "$s s"
}

/** OTP `YYYY-MM-DD` + `HH:MM` for a plan departure, from epoch millis. */
fun planDateTime(epochMs: Long): Pair<String, String> {
  val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))
  val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(epochMs))
  return date to time
}
