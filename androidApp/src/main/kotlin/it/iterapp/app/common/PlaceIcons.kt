package it.iterapp.app.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.ui.graphics.vector.ImageVector
import it.iterapp.core.model.SearchResult

/**
 * Category glyph for a search result, shared by search results and recents.
 * Train stations first: stationId-backed results must keep the Train icon,
 * which signals the live-boards entry point.
 */
fun placeIcon(r: SearchResult): ImageVector {
  if (r.isTrainStation) return Icons.Rounded.Train
  val k = r.osmKey?.lowercase()?.trim()
  val v = r.osmValue?.lowercase()?.trim()
  return when {
    k == "highway" && v == "bus_stop" -> Icons.Rounded.DirectionsBus
    k == "amenity" && v == "bus_station" -> Icons.Rounded.DirectionsBus
    k == "railway" && v == "tram_stop" -> Icons.Rounded.Tram
    k == "railway" -> Icons.Rounded.Train
    k == "station" -> when (v) {
      "subway" -> Icons.Rounded.Subway
      "light_rail", "tram" -> Icons.Rounded.Tram
      else -> Icons.Rounded.Train
    }
    k == "public_transport" -> Icons.Rounded.Train
    k == "highway" -> Icons.Rounded.Route
    k == "place" -> Icons.Rounded.LocationCity
    k == "shop" -> Icons.Rounded.Storefront
    k == "amenity" && v in setOf("bar", "pub", "biergarten", "nightclub") -> Icons.Rounded.LocalBar
    k == "amenity" && v in setOf("restaurant", "fast_food", "food_court", "cafe", "ice_cream") ->
      Icons.Rounded.Restaurant
    else -> Icons.Rounded.Place
  }
}
