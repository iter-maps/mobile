package it.iterapp.app.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * The router's timezone. OTP interprets the `date`/`time` plan arguments in the
 * network's local zone, so departure times must be formatted there, not in the
 * device zone. The served network is Italian; revisit when coverage widens.
 */
private val ROUTER_ZONE: TimeZone = TimeZone.getTimeZone("Europe/Rome")

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

/**
 * GTFS station names arrive ALL-CAPS ("MILANO PORTA VENEZIA"); title-case them
 * for display, keeping short railway suffixes ("FS", "FN") intact. Mixed-case
 * names pass through untouched, so the transform is idempotent and safe if the
 * feed is ever fixed upstream. Locale pinned: default-locale lowercase would
 * mangle "MILANO" on Turkish-locale devices.
 */
fun formatStationName(raw: String): String {
  val name = raw.trim()
  if (name != name.uppercase(Locale.ITALIAN)) return name
  return name.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
    if (word.length <= 2) word
    else word.lowercase(Locale.ITALIAN).replaceFirstChar { it.titlecase(Locale.ITALIAN) }
  }
}

/** Seconds → compact `+3 min` / `+45 s` delay label. */
fun formatDelay(seconds: Double): String {
  val s = seconds.roundToInt()
  return if (s >= 90) "${(s / 60.0).roundToInt()} min" else "$s s"
}

/** Hour and minute of [epochMs] on the router's wall clock. */
fun routerHourMinute(epochMs: Long): Pair<Int, Int> {
  val cal = Calendar.getInstance(ROUTER_ZONE).apply { timeInMillis = epochMs }
  return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
}

/** Epoch millis of today at [hour]:[minute] on the router's wall clock. */
fun routerTimeTodayAt(hour: Int, minute: Int): Long =
  Calendar.getInstance(ROUTER_ZONE).apply {
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.timeInMillis

/** OTP `YYYY-MM-DD` + `HH:MM` for a plan departure, in the router's zone. */
fun planDateTime(epochMs: Long): Pair<String, String> {
  val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ROUTER_ZONE }
  val timeFmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = ROUTER_ZONE }
  return dateFmt.format(Date(epochMs)) to timeFmt.format(Date(epochMs))
}
