package it.iterapp.core.api

import it.iterapp.core.wire.ApiErrorCodes
import kotlin.test.Test
import kotlin.test.assertEquals

class AppFailureTest {

  private fun transport(kind: TransportKind) =
    IterTransportException("gateway unreachable: x", cause = null, kind = kind)

  private fun api(status: Int, code: String?) =
    IterApiException(status, code, "HTTP $status")

  @Test
  fun timeoutMapsToTimeoutRegardlessOfConnectivity() {
    assertEquals(AppFailure.Timeout, transport(TransportKind.TIMEOUT).toAppFailure(isOnline = true))
    assertEquals(AppFailure.Timeout, transport(TransportKind.TIMEOUT).toAppFailure(isOnline = false))
  }

  @Test
  fun unreachableSplitsOnConnectivity() {
    assertEquals(
      AppFailure.NoConnection,
      transport(TransportKind.UNREACHABLE).toAppFailure(isOnline = false),
    )
    assertEquals(
      AppFailure.ServerUnreachable(),
      transport(TransportKind.UNREACHABLE).toAppFailure(isOnline = true),
    )
  }

  @Test
  fun notFoundAndBadRequest() {
    assertEquals(AppFailure.NotFound, api(404, null).toAppFailure())
    assertEquals(AppFailure.BadRequest(ApiErrorCodes.BAD_REQUEST), api(400, ApiErrorCodes.BAD_REQUEST).toAppFailure())
  }

  @Test
  fun serverFaultsFromStatusOrUpstreamCode() {
    assertEquals(AppFailure.Server(null), api(500, null).toAppFailure())
    assertEquals(
      AppFailure.Server(ApiErrorCodes.UPSTREAM_UNAVAILABLE),
      api(503, ApiErrorCodes.UPSTREAM_UNAVAILABLE).toAppFailure(),
    )
  }

  @Test
  fun rateLimitedFrom429OrBusy() {
    assertEquals(AppFailure.RateLimited(), api(429, null).toAppFailure())
    assertEquals(AppFailure.RateLimited(), api(503, ApiErrorCodes.BUSY).toAppFailure())
  }

  @Test
  fun offlineBundleCodesWinOverStatus() {
    assertEquals(
      AppFailure.OfflineBundle(ApiErrorCodes.AREA_TOO_LARGE),
      api(400, ApiErrorCodes.AREA_TOO_LARGE).toAppFailure(),
    )
    assertEquals(
      AppFailure.OfflineBundle(ApiErrorCodes.STORE_FULL),
      api(507, ApiErrorCodes.STORE_FULL).toAppFailure(),
    )
  }

  @Test
  fun graphqlBodyErrorIsServerFault() {
    assertEquals(AppFailure.Server("GRAPHQL_ERROR"), api(200, "GRAPHQL_ERROR").toAppFailure())
  }

  @Test
  fun unknownForUnclassifiedThrowable() {
    assertEquals(AppFailure.Unknown, RuntimeException("boom").toAppFailure())
  }
}
