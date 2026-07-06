package it.iterapp.core.repo

import it.iterapp.core.api.IterGatewayClient
import it.iterapp.core.geo.gtfsLocalId
import it.iterapp.core.model.Leg
import it.iterapp.core.wire.ReliabilityResponse

class ReliabilityRepository(
  private val client: IterGatewayClient,
) {

  /** Ids are passed verbatim; empty `cells` means "no history yet". */
  suspend fun forStop(route: String, direction: String, stop: String): ReliabilityResponse =
    client.reliability(route, direction, stop)

  /**
   * Convenience for a transit leg. The archive keys on feed-local ids (the
   * recorder consumes GTFS-RT, which carries no OTP feed prefix), so OTP's
   * `FEED:id` values are stripped here; absent direction defaults to 0 like
   * everywhere else in the stack.
   */
  suspend fun forLeg(leg: Leg): ReliabilityResponse? {
    val route = leg.routeGtfsId ?: return null
    val stop = leg.from.stopGtfsId ?: return null
    val direction = leg.directionId?.takeIf { it.isNotBlank() } ?: "0"
    return forStop(gtfsLocalId(route), direction, gtfsLocalId(stop))
  }
}
