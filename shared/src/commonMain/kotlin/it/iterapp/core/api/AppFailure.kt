package it.iterapp.core.api

import it.iterapp.core.wire.ApiErrorCodes

/**
 * The single, platform-neutral classification of a failed operation (ADR 0015).
 * Repositories still throw [IterApiException] / [IterTransportException]; the
 * UI layer maps the caught throwable to exactly one of these via [toAppFailure]
 * so both shells render the same category the same way, and so "no connection"
 * (offline zones may still work) is never confused with "server unreachable".
 */
sealed interface AppFailure {

  /** The device has no network path at all. Downloaded offline zones still work. */
  data object NoConnection : AppFailure

  /** Network is up, but the gateway can't be reached (DNS/refused/reset). */
  data class ServerUnreachable(val code: String? = null) : AppFailure

  /** The request took too long (a client or server timeout fired). */
  data object Timeout : AppFailure

  /** The gateway asked us to slow down (HTTP 429 / `BUSY`). */
  data class RateLimited(val retryAfterSeconds: Int? = null) : AppFailure

  /** A non-swallowed 404 — the resource genuinely does not exist. */
  data object NotFound : AppFailure

  /** The request was rejected as invalid (4xx with a machine [code]). */
  data class BadRequest(val code: String? = null) : AppFailure

  /** The gateway returned a server-side fault envelope (5xx / upstream / GraphQL). */
  data class Server(val code: String? = null) : AppFailure

  /** An offline-bundle operation failed (area too large, storage full, corrupt). */
  data class OfflineBundle(val code: String? = null) : AppFailure

  /** Anything the client could not classify. */
  data object Unknown : AppFailure

  /** Stable id for logging / cross-language mapping; never localized. */
  val id: String
    get() = when (this) {
      NoConnection -> "no_connection"
      is ServerUnreachable -> "server_unreachable"
      Timeout -> "timeout"
      is RateLimited -> "rate_limited"
      NotFound -> "not_found"
      is BadRequest -> "bad_request"
      is Server -> "server"
      is OfflineBundle -> "offline_bundle"
      Unknown -> "unknown"
    }
}

/** Offline-surface codes that mean "the bundle request itself was the problem". */
private val OFFLINE_BUNDLE_CODES = setOf(
  ApiErrorCodes.AREA_TOO_LARGE,
  ApiErrorCodes.PAYLOAD_TOO_LARGE,
  ApiErrorCodes.STORE_FULL,
  ApiErrorCodes.EXTRACT_FAILED,
  ApiErrorCodes.BBOX_REQUIRED,
  ApiErrorCodes.BBOX_INVALID,
  ApiErrorCodes.BBOX_OUT_OF_RANGE,
  ApiErrorCodes.BBOX_DEGENERATE,
  ApiErrorCodes.ZOOM_INVALID,
)

/** Codes that mean the gateway reached a broken/absent upstream. */
private val UPSTREAM_CODES = setOf(
  ApiErrorCodes.UPSTREAM_UNAVAILABLE,
  ApiErrorCodes.UPSTREAM_ERROR,
  ApiErrorCodes.INTERNAL,
)

/**
 * Maps a caught throwable to its [AppFailure]. [isOnline] splits a bare
 * transport failure into [AppFailure.NoConnection] vs
 * [AppFailure.ServerUnreachable]; pass the current connectivity reading.
 */
fun Throwable.toAppFailure(isOnline: Boolean = true): AppFailure = when (this) {
  is IterTransportException -> when (kind) {
    TransportKind.TIMEOUT -> AppFailure.Timeout
    TransportKind.UNREACHABLE ->
      if (isOnline) AppFailure.ServerUnreachable() else AppFailure.NoConnection
  }

  is IterApiException -> classifyApi(status, code)

  else -> AppFailure.Unknown
}

private fun classifyApi(status: Int, code: String?): AppFailure = when {
  code != null && code in OFFLINE_BUNDLE_CODES -> AppFailure.OfflineBundle(code)
  status == 429 || code == ApiErrorCodes.BUSY -> AppFailure.RateLimited()
  status == 404 -> AppFailure.NotFound
  status in 500..599 || (code != null && code in UPSTREAM_CODES) -> AppFailure.Server(code)
  code == ApiErrorCodes.TIMEOUT -> AppFailure.Timeout
  // GraphQL body errors are synthesized as IterApiException(200, code, …).
  status == 200 && code != null -> AppFailure.Server(code)
  status in 400..499 -> AppFailure.BadRequest(code)
  else -> AppFailure.Unknown
}
