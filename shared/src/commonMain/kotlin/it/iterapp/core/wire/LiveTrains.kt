package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * One row of a departures/arrivals board (`GET /trenitalia/departures|arrivals`).
 * `origin` is always present on the wire and null when upstream omits it
 * (departures boards always carry null).
 */
@Serializable
data class BoardEntry(
  /** Category + number, e.g. `"REG 22815"`. */
  val trainNumber: String,
  /** `REG` | `RV` | `IC` | `FR` | … */
  val category: String,
  val origin: String?,
  val destination: String,
  /** Scheduled wall-clock `"HH:MM"`. */
  val scheduledTime: String,
  /** Signed minutes; negative = early. */
  val delayMinutes: Int,
  /** Key omitted when unknown. */
  val platform: String? = null,
)

/**
 * A station (`GET /trenitalia/stations`, `/trenitalia/stations/search`).
 * Ids match `^S\d+$`. Search results never carry coordinates; the full list
 * does when upstream has them — keys omitted, never 0.
 */
@Serializable
data class Station(
  val id: String,
  val name: String,
  val lat: Double? = null,
  val lon: Double? = null,
)
