package it.iterapp.core.wire

import kotlinx.serialization.Serializable

/**
 * `manifest.json` inside an `/offline/bundle` ZIP. `bbox` is the JSON 4-tuple
 * `[minLon, minLat, maxLon, maxLat]`. All fields always present.
 */
@Serializable
data class OfflineManifest(
  val generator: String,
  val createdAt: String,
  val bbox: List<Double>,
  val minzoom: Int,
  val maxzoom: Int,
  /** Always `"area.pmtiles"`. */
  val pmtiles: String,
  /** Styles actually included, subset of the whitelist. */
  val styles: List<String>,
  val glyphs: Boolean,
  val sprite: Boolean,
  /** Overlay filenames actually written. */
  val overlays: List<String>,
  val note: String,
)

/** The style whitelist shared by `/styles/{file}` and offline bundles. */
object StyleNames {
  const val LIGHT = "light"
  const val DARK = "dark"
  const val TRANSIT_LIGHT = "transit-light"
  const val TRANSIT_DARK = "transit-dark"
  val ALL = listOf(LIGHT, DARK, TRANSIT_LIGHT, TRANSIT_DARK)
}

/**
 * The literal token stored in style documents; the gateway rewrites it per
 * request, and offline bundles keep it for the client to rewrite to a
 * `file://` directory.
 */
const val BASE_URL_TOKEN = "__BASE_URL__"
