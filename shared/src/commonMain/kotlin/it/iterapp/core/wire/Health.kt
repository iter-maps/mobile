package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * `GET /health` / `GET /health.json` — the "update app" banner document.
 * The two timestamp fields are always present and null until the pipeline
 * bootstraps; `gtfsLoaded` is ISO-8601 or the literal `"unknown"`.
 */
@Serializable
data class StaticHealth(
  val status: String,
  val version: String,
  val gtfsLoaded: String,
  val tilesBuiltAt: String?,
  val bootstrappedAt: String?,
)

/** `GET /manifest` — per-artifact freshness in one request. */
@Serializable
data class FreshnessManifest(
  val apiVersion: String,
  val generatedAt: String,
  /** Keyed by surface name: `tiles`, `styles`, `glyphs`, `sprite`, `overlays`. */
  val artifacts: Map<String, ArtifactFreshness> = emptyMap(),
)

/** A missing artifact yields `{}` — both keys absent, never an error. */
@Serializable
data class ArtifactFreshness(
  val updatedAt: String? = null,
  /** Weak ETag `W/"<len>-<epoch>"`. */
  val etag: String? = null,
)

/** `GET /stats` — curated public counters since process start. */
@Serializable
data class StatsSummary(
  val service: String,
  val version: String,
  val uptimeS: Long,
  val requests: RequestStats,
  /** Keyed by HTTP method. */
  val latency: Map<String, LatencySummary> = emptyMap(),
  val upstreamErrors: UpstreamErrorStats,
  val weatherCache: CacheStats,
)

@Serializable
data class RequestStats(
  val total: Long,
  /** Keys are `"1xx"`…`"5xx"`. */
  val byStatusClass: Map<String, Long> = emptyMap(),
)

@Serializable
data class LatencySummary(
  val avgMs: Double,
  val p50Ms: Double,
  val p90Ms: Double,
  val p99Ms: Double,
  val count: Long,
)

@Serializable
data class UpstreamErrorStats(
  val total: Long,
  /** Keys are `otp` | `photon` | `viaggiatreno`. */
  val byUpstream: Map<String, Long> = emptyMap(),
)

@Serializable
data class CacheStats(
  val hit: Long,
  val miss: Long,
)

/** `GET /readyz` body. */
@Serializable
data class Readiness(
  val status: String,
  val checks: List<ReadinessCheck> = emptyList(),
)

@Serializable
data class ReadinessCheck(
  val name: String,
  val status: String,
  val detail: String? = null,
)
