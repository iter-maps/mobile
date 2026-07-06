package it.iterapp.core.repo

import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.wire.BoardEntry
import it.iterapp.core.wire.Station

/**
 * Live train boards. The contract is polling-only; the boards surface is
 * cached server-side with `max-age=20` — poll no faster than that.
 *
 * The `@Throws(Exception::class)` on each suspend entry point is load-bearing
 * for iOS: without it Kotlin/Native terminates the process on a thrown
 * exception instead of bridging it to a catchable Swift error.
 */
class TrainsRepository(
  private val client: IterGatewayClient,
) {

  /** Autocomplete; results never carry coordinates by contract. Min 2 chars. */
  @Throws(Exception::class)
  suspend fun searchStations(query: String): List<Station> {
    if (query.trim().length < 2) return emptyList()
    return client.searchStations(query.trim())
  }

  @Throws(Exception::class)
  suspend fun departures(stationId: String): List<BoardEntry> = client.departures(stationId)

  @Throws(Exception::class)
  suspend fun arrivals(stationId: String): List<BoardEntry> = client.arrivals(stationId)

  companion object {
    /** Server cache `max-age` on boards; the minimum sensible poll period. */
    const val BOARD_POLL_SECONDS = 20
  }
}
