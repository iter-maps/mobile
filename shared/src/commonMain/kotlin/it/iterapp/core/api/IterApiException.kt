package it.iterapp.core.api

/**
 * A gateway call failed. [code] is the envelope's stable machine code when the
 * response carried the JSON error envelope, null otherwise (static-file 404s,
 * disabled-feature bare 404s, passthrough upstream bodies). Branch on
 * [status] + [code]; [message] wording is not a contract.
 */
class IterApiException(
  val status: Int,
  val code: String?,
  override val message: String,
) : Exception(message) {

  /** True when this may simply mean "feature turned off" (`/telemetry`, `/sync`, `/stats`). */
  val isBareNotFound: Boolean get() = status == 404 && code == null
}

/** How a transport failure presented, as far as the client can tell. */
enum class TransportKind {
  /** The request timed out (connect/socket/request timeout fired). */
  TIMEOUT,

  /** DNS failure, connection refused/reset — no response arrived. Whether this
   *  is "no network" or "server down" is decided later against connectivity. */
  UNREACHABLE,
}

/** Transport-level failure (DNS, refused, timeout before any response). */
class IterTransportException(
  override val message: String,
  override val cause: Throwable?,
  val kind: TransportKind = TransportKind.UNREACHABLE,
) : Exception(message, cause)
