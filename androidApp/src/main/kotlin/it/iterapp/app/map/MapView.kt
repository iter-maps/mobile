package it.iterapp.app.map

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.iterapp.core.model.GeoPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** One polyline of the route overlay. Color is ARGB; walk legs are dashed. */
data class RouteLine(
  val points: List<GeoPoint>,
  val color: Int,
  val dashed: Boolean,
)

/** A dot marker (stops, endpoints). */
data class RouteDot(
  val point: GeoPoint,
  val color: Int,
  /** Radius in dp. */
  val radius: Float = 4.5f,
)

/**
 * Imperative handle for camera moves; safe to call before the map is ready
 * (the last request is replayed on readiness).
 */
@Stable
class MapCameraController {
  internal var map: MapLibreMap? = null
    set(value) {
      field = value
      value?.let { pending?.invoke(it) }
      pending = null
    }
  private var pending: ((MapLibreMap) -> Unit)? = null

  /** True after the user drags the camera; cleared by [animateTo]/[fitTo]. */
  var userMovedCamera by mutableStateOf(false)
    internal set

  /** Current map bearing, degrees; drives the compass control. */
  var bearing by mutableStateOf(0.0)
    internal set

  // Surface geometry and the sheet inset, needed to project the band the user
  // actually sees (the map view is GPU-translated up by insetPx/2, invisibly
  // to MapLibre's projection).
  internal var surfaceWidth = 0
  internal var surfaceHeight = 0
  internal var insetPx = 0

  fun animateTo(point: GeoPoint, zoom: Double = 15.5) {
    userMovedCamera = false
    run { map ->
      map.animateCamera(
        CameraUpdateFactory.newLatLngZoom(LatLng(point.lat, point.lon), zoom),
        900,
      )
    }
  }

  fun fitTo(points: List<GeoPoint>, paddingPx: Int = 140) {
    if (points.isEmpty()) return
    if (points.size == 1) return animateTo(points.first())
    userMovedCamera = false
    run { map ->
      val bounds = LatLngBounds.Builder().apply {
        points.forEach { include(LatLng(it.lat, it.lon)) }
      }.build()
      // Extra bottom padding keeps the fitted route clear of the sheet.
      map.animateCamera(
        CameraUpdateFactory.newLatLngBounds(
          bounds, paddingPx, paddingPx, paddingPx, paddingPx + insetPx,
        ),
        900,
      )
    }
  }

  fun resetNorth() {
    run { it.animateCamera(CameraUpdateFactory.bearingTo(0.0), 500) }
  }

  /**
   * The bbox of the map band the user actually sees (`minLon,minLat,maxLon,
   * maxLat`), excluding the area behind the sheet. Falls back to the full
   * visible region before the surface is measured. Null before readiness.
   */
  fun viewportBBox(): String? {
    val m = map ?: return null
    if (surfaceWidth == 0 || surfaceHeight == 0) {
      val bounds = m.projection.visibleRegion.latLngBounds
      return "${bounds.longitudeWest},${bounds.latitudeSouth}," +
        "${bounds.longitudeEast},${bounds.latitudeNorth}"
    }
    val proj = m.projection
    val shift = insetPx / 2f
    // Uncovered band: screen-top to (height - sheet coverage), mapped into the
    // untranslated view space MapLibre projects from.
    val bandBottom = (surfaceHeight - insetPx).toFloat().coerceAtLeast(surfaceHeight * 0.3f)
    val w = surfaceWidth.toFloat()
    val corners = listOf(
      PointF(0f, shift),
      PointF(w, shift),
      PointF(0f, bandBottom + shift),
      PointF(w, bandBottom + shift),
    ).map { proj.fromScreenLocation(it) }
    val minLon = corners.minOf { it.longitude }
    val maxLon = corners.maxOf { it.longitude }
    val minLat = corners.minOf { it.latitude }
    val maxLat = corners.maxOf { it.latitude }
    if (minLon >= maxLon || minLat >= maxLat) return null
    return "$minLon,$minLat,$maxLon,$maxLat"
  }

  private fun run(block: (MapLibreMap) -> Unit) {
    val m = map
    if (m != null) block(m) else pending = block
  }
}

@Composable
fun rememberMapCameraController(): MapCameraController = remember { MapCameraController() }

/**
 * MapLibre map as a Compose node. The view is a TextureView translated up by
 * [insetPx]/2 on the GPU so map content stays centered in the band the sheet
 * leaves visible — zero camera moves while dragging (ADR 0008).
 */
@Composable
fun IterMapView(
  styleUrl: String,
  controller: MapCameraController,
  modifier: Modifier = Modifier,
  insetPx: Int = 0,
  routeLines: List<RouteLine> = emptyList(),
  routeDots: List<RouteDot> = emptyList(),
  userLocation: GeoPoint? = null,
  onMapTap: (GeoPoint) -> Unit = {},
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
  val currentInset = remember { mutableIntStateOf(insetPx) }
  currentInset.intValue = insetPx
  controller.insetPx = insetPx

  val mapView = remember {
    // TextureView so the graphicsLayer translation composites on the GPU.
    MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true))
      .apply { onCreate(null) }
  }

  LaunchedEffect(mapView) {
    mapView.getMapAsync { map ->
      map.uiSettings.isCompassEnabled = false
      map.uiSettings.isAttributionEnabled = true
      map.uiSettings.isLogoEnabled = false
      map.addOnMapClickListener { latLng ->
        onMapTap(GeoPoint(latLng.latitude, latLng.longitude))
        false
      }
      map.addOnCameraMoveStartedListener { reason ->
        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
          controller.userMovedCamera = true
        }
      }
      map.addOnCameraIdleListener {
        controller.bearing = map.cameraPosition.bearing
      }
      mapRef = map
      controller.map = map
    }
  }

  AndroidView(
    modifier = modifier
      .graphicsLayer { translationY = -currentInset.intValue / 2f }
      .onSizeChanged {
        controller.surfaceWidth = it.width
        controller.surfaceHeight = it.height
      },
    factory = { mapView },
  )

  // Style: (re)load when the URL changes, then re-apply overlays.
  LaunchedEffect(mapRef, styleUrl) {
    val map = mapRef ?: return@LaunchedEffect
    map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
      applyTransitOverlayLayers(style)
      applyOverlayLayers(style, routeLines, routeDots, userLocation)
    }
  }

  // Overlay data: refresh sources on change (cheap once layers exist).
  LaunchedEffect(mapRef, routeLines, routeDots, userLocation) {
    val style = mapRef?.style ?: return@LaunchedEffect
    if (style.isFullyLoaded) {
      applyOverlayLayers(style, routeLines, routeDots, userLocation)
    }
  }

  DisposableEffect(lifecycleOwner, mapView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.onDestroy()
    }
  }
}

/**
 * Transit styles pre-wire `overlay-*` GeoJSON sources but deliberately emit
 * no layers — drawing them is the client's job. Reference the existing
 * sources by id; on standard styles (no such sources) draw nothing.
 */
private fun applyTransitOverlayLayers(style: Style) {
  if (style.getSource("overlay-transit-lines") != null &&
    style.getLayer("iter-overlay-lines") == null
  ) {
    style.addLayer(
      LineLayer("iter-overlay-lines", "overlay-transit-lines").withProperties(
        PropertyFactory.lineColor("#7B4EA3"),
        PropertyFactory.lineWidth(2.5f),
        PropertyFactory.lineOpacity(0.75f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      ),
    )
  }
  if (style.getSource("overlay-metro-stations") != null &&
    style.getLayer("iter-overlay-stations") == null
  ) {
    style.addLayer(
      CircleLayer("iter-overlay-stations", "overlay-metro-stations").withProperties(
        PropertyFactory.circleColor("#FFFFFF"),
        PropertyFactory.circleRadius(3.5f),
        PropertyFactory.circleStrokeColor("#4248C9"),
        PropertyFactory.circleStrokeWidth(2f),
      ),
    )
  }
}

private const val ROUTE_SOURCE = "iter-route"
private const val ROUTE_CASING_LAYER = "iter-route-casing"
private const val ROUTE_LAYER = "iter-route-line"
private const val ROUTE_WALK_LAYER = "iter-route-walk"
private const val DOTS_SOURCE = "iter-dots"
private const val DOTS_LAYER = "iter-dots-layer"
private const val USER_SOURCE = "iter-user"
private const val USER_HALO_LAYER = "iter-user-halo"
private const val USER_DOT_LAYER = "iter-user-dot"

private fun applyOverlayLayers(
  style: Style,
  lines: List<RouteLine>,
  dots: List<RouteDot>,
  user: GeoPoint?,
) {
  val isDashed = Expression.eq(Expression.get("dashed"), Expression.literal(true))
  val isSolid = Expression.eq(Expression.get("dashed"), Expression.literal(false))

  val lineFc = FeatureCollection.fromFeatures(
    lines.map { line ->
      Feature.fromGeometry(
        LineString.fromLngLats(line.points.map { Point.fromLngLat(it.lon, it.lat) }),
      ).apply {
        addStringProperty("color", colorHex(line.color))
        addBooleanProperty("dashed", line.dashed)
      }
    },
  )
  (style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE))?.setGeoJson(lineFc)
    ?: style.addSource(GeoJsonSource(ROUTE_SOURCE, lineFc))

  if (style.getLayer(ROUTE_CASING_LAYER) == null) {
    style.addLayer(
      LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE).withProperties(
        PropertyFactory.lineColor("#FFFFFF"),
        PropertyFactory.lineWidth(9f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      ).withFilter(isSolid),
    )
    style.addLayer(
      LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
        PropertyFactory.lineColor(Expression.get("color")),
        PropertyFactory.lineWidth(5.5f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      ).withFilter(isSolid),
    )
    style.addLayer(
      LineLayer(ROUTE_WALK_LAYER, ROUTE_SOURCE).withProperties(
        PropertyFactory.lineColor(Expression.get("color")),
        PropertyFactory.lineWidth(4.5f),
        PropertyFactory.lineDasharray(arrayOf(0.2f, 1.8f)),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      ).withFilter(isDashed),
    )
  }

  val dotFc = FeatureCollection.fromFeatures(
    dots.map { dot ->
      Feature.fromGeometry(Point.fromLngLat(dot.point.lon, dot.point.lat)).apply {
        addStringProperty("color", colorHex(dot.color))
        addNumberProperty("radius", dot.radius)
      }
    },
  )
  (style.getSourceAs<GeoJsonSource>(DOTS_SOURCE))?.setGeoJson(dotFc)
    ?: style.addSource(GeoJsonSource(DOTS_SOURCE, dotFc))
  if (style.getLayer(DOTS_LAYER) == null) {
    style.addLayer(
      CircleLayer(DOTS_LAYER, DOTS_SOURCE).withProperties(
        PropertyFactory.circleColor(Expression.get("color")),
        PropertyFactory.circleRadius(Expression.get("radius")),
        PropertyFactory.circleStrokeColor("#FFFFFF"),
        PropertyFactory.circleStrokeWidth(2f),
      ),
    )
  }

  val userFc = FeatureCollection.fromFeatures(
    user?.let { arrayOf(Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat))) } ?: emptyArray(),
  )
  (style.getSourceAs<GeoJsonSource>(USER_SOURCE))?.setGeoJson(userFc)
    ?: style.addSource(GeoJsonSource(USER_SOURCE, userFc))
  if (style.getLayer(USER_DOT_LAYER) == null) {
    style.addLayer(
      CircleLayer(USER_HALO_LAYER, USER_SOURCE).withProperties(
        PropertyFactory.circleColor("#4248C9"),
        PropertyFactory.circleOpacity(0.18f),
        PropertyFactory.circleRadius(18f),
      ),
    )
    style.addLayer(
      CircleLayer(USER_DOT_LAYER, USER_SOURCE).withProperties(
        PropertyFactory.circleColor("#4248C9"),
        PropertyFactory.circleRadius(7f),
        PropertyFactory.circleStrokeColor("#FFFFFF"),
        PropertyFactory.circleStrokeWidth(2.5f),
      ),
    )
  }
}

private fun colorHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)
