package it.iterapp.app.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.iterapp.app.R
import it.iterapp.app.layers.MapLayersPage
import it.iterapp.app.map.IterMapView
import it.iterapp.app.map.RouteDot
import it.iterapp.app.map.RouteLine
import it.iterapp.app.map.rememberMapCameraController
import it.iterapp.app.offline.OfflinePage
import it.iterapp.app.offline.OfflineViewModel
import it.iterapp.app.place.PlaceDetailPage
import it.iterapp.app.place.PlaceDetailViewModel
import it.iterapp.app.planning.PlanEndpoint
import it.iterapp.app.planning.PlanningDetailPage
import it.iterapp.app.planning.PlanningPage
import it.iterapp.app.planning.PlanningViewModel
import it.iterapp.app.search.SearchPage
import it.iterapp.app.search.SearchViewModel
import it.iterapp.app.settings.SettingsPage
import it.iterapp.app.settings.SettingsViewModel
import it.iterapp.app.sheet.SheetAnchor
import it.iterapp.app.sheet.SheetNavigator
import it.iterapp.app.sheet.SheetPage
import it.iterapp.app.sheet.SheetScaffold
import it.iterapp.app.sheet.rememberSheetState
import it.iterapp.app.trains.TrainBoardPage
import it.iterapp.app.trains.TrainBoardViewModel
import it.iterapp.app.ui.theme.LineColors
import it.iterapp.app.ui.theme.lineColor
import it.iterapp.core.model.Itinerary
import org.koin.androidx.compose.koinViewModel

/**
 * The single full-screen destination (ADR 0008): persistent map + universal
 * sheet. All pages share this screen's ViewModelStore.
 */
@Composable
fun HomeScreen() {
  val homeViewModel: HomeViewModel = koinViewModel()
  val searchViewModel: SearchViewModel = koinViewModel()
  val planningViewModel: PlanningViewModel = koinViewModel()
  val trainsViewModel: TrainBoardViewModel = koinViewModel()
  val offlineViewModel: OfflineViewModel = koinViewModel()
  val placeViewModel: PlaceDetailViewModel = koinViewModel()
  val settingsViewModel: SettingsViewModel = koinViewModel()

  val nav = remember { SheetNavigator() }
  val sheetState = rememberSheetState()
  val camera = rememberMapCameraController()

  val styleUrl by homeViewModel.styleUrl.collectAsStateWithLifecycle()
  val userLocation by homeViewModel.userLocation.collectAsStateWithLifecycle()
  val selectedPlace by homeViewModel.selectedPlace.collectAsStateWithLifecycle()
  val recentPlaces by homeViewModel.recentPlaces.collectAsStateWithLifecycle()
  val nearbyStations by homeViewModel.nearbyStations.collectAsStateWithLifecycle()
  val selectedItinerary by planningViewModel.selected.collectAsStateWithLifecycle()
  val mapMode by settingsViewModel.mapMode.collectAsStateWithLifecycle()

  val dark = isSystemInDarkTheme()
  LaunchedEffect(dark) { homeViewModel.onDarkThemeChange(dark) }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { homeViewModel.onLocationPermissionGranted() }
  LaunchedEffect(Unit) {
    permissionLauncher.launch(
      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    )
  }

  // System/gesture back must run the same per-page teardown as the in-sheet
  // arrows, or the shared ViewModels keep stale state (and the map keeps stale
  // overlays) on re-entry. Both paths funnel through popCurrent().
  fun popCurrent() {
    when (nav.current) {
      SheetPage.Search -> searchViewModel.reset()
      is SheetPage.PlaceDetail -> {
        homeViewModel.select(null)
        placeViewModel.clear()
      }
      SheetPage.Planning -> planningViewModel.reset()
      is SheetPage.PlanningPicker -> searchViewModel.reset()
      is SheetPage.TrainBoard -> trainsViewModel.reset()
      else -> {}
    }
    nav.pop()
  }

  BackHandler(enabled = nav.canPop) { popCurrent() }

  // Camera follows selections: place at half-sheet zoom, itinerary fitted.
  LaunchedEffect(selectedPlace) {
    selectedPlace?.let { camera.animateTo(it.point, 16.0) }
  }
  LaunchedEffect(selectedItinerary) {
    selectedItinerary?.let { itinerary ->
      camera.fitTo(itinerary.legs.flatMap { leg -> leg.geometry.ifEmpty { listOf(leg.from.point, leg.to.point) } })
    }
  }
  // First launch lands on the user's area, never the world view — but only
  // until the user or a selection has moved the camera.
  LaunchedEffect(userLocation) {
    val here = userLocation ?: return@LaunchedEffect
    if (!camera.userMovedCamera && selectedPlace == null && selectedItinerary == null &&
      !homeViewModel.initialCameraDone
    ) {
      homeViewModel.initialCameraDone = true
      camera.animateTo(here, 12.0)
    }
  }

  val walkColor = LineColors.Walk.toArgb()
  val placeColor = MaterialTheme.colorScheme.primary.toArgb()
  val routeLines = remember(selectedItinerary) {
    selectedItinerary?.toRouteLines(walkColor) ?: emptyList()
  }
  val routeDots = remember(selectedItinerary, selectedPlace, placeColor) {
    buildList {
      selectedItinerary?.let { itinerary ->
        itinerary.legs.filter { it.isTransit }.forEach { leg ->
          val color = lineColor(leg.routeColor, leg.mode).toArgb()
          add(RouteDot(leg.from.point, color))
          add(RouteDot(leg.to.point, color))
        }
      }
      selectedPlace?.let { add(RouteDot(it.point, placeColor, radius = 7f)) }
    }
  }

  val density = LocalDensity.current
  // Safe distance from the system gesture bar for collapsed-sheet content.
  val gestureClearance = WindowInsets.navigationBars.asPaddingValues()
    .calculateBottomPadding().coerceAtLeast(24.dp) + 12.dp
  val mapInset = remember { mutableIntStateOf(0) }
  val layersContentPx = remember { mutableFloatStateOf(0f) }
  val peekContentPx = remember { mutableIntStateOf(0) }
  val page = nav.current

  // Peek = drag handle + the measured search+chips block + gesture clearance;
  // the 156dp fallback covers the first frame before measurement.
  val computedPeek = if (peekContentPx.intValue > 0) {
    24.dp + with(density) { peekContentPx.intValue.toDp() } + gestureClearance
  } else {
    156.dp
  }

  // Remember the anchor each page was left at, so back restores the height.
  val savedAnchors = remember { mutableStateMapOf<SheetPage, SheetAnchor>() }
  LaunchedEffect(Unit) {
    var prev = nav.current
    snapshotFlow { nav.current }.collect { cur ->
      if (cur != prev) {
        savedAnchors[prev] = sheetState.currentAnchor
        prev = cur
      }
    }
  }

  SheetScaffold(
    state = sheetState,
    peekHeight = computedPeek,
    onMapInset = { mapInset.intValue = it },
    contentAnchorPx = if (page is SheetPage.MapLayers && layersContentPx.floatValue > 0f) {
      // drag handle (24dp) + measured content + gesture-bar clearance, in px
      with(density) { (24.dp + gestureClearance).toPx() } + layersContentPx.floatValue
    } else {
      null
    },
    openAnchor = savedAnchors[page] ?: page.openAnchor,
    openKey = page,
    mapContent = {
      IterMapView(
        styleUrl = styleUrl,
        controller = camera,
        insetPx = mapInset.intValue,
        routeLines = routeLines,
        routeDots = routeDots,
        userLocation = userLocation,
        onMapTap = { point ->
          // Identify only from the browsing pages, never mid-planning.
          if (nav.current is SheetPage.Home || nav.current is SheetPage.PlaceDetail) {
            homeViewModel.identify(point) { result ->
              if (nav.current is SheetPage.PlaceDetail) nav.pop()
              nav.push(SheetPage.PlaceDetail(result))
            }
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
      MapControls(
        camera = camera,
        onLayers = { nav.push(SheetPage.MapLayers) },
        onMyLocation = {
          homeViewModel.lastKnownLocation()?.let { camera.animateTo(it, 15.5) }
        },
        insetPx = mapInset.intValue,
      )
    },
    sheetContent = {
      AnimatedContent(
        targetState = page,
        transitionSpec = {
          val forward = !nav.lastWasPop
          val enter = slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 260f)) { w ->
            if (forward) w else -w
          } + fadeIn(spring(stiffness = 260f))
          val exit = slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = 260f)) { w ->
            if (forward) -w else w
          } + fadeOut(spring(stiffness = 260f))
          // The sheet's rounded Surface clips overflow; pages glide past bounds.
          (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        label = "sheet-page",
      ) { current ->
        when (current) {
          SheetPage.Home -> HomeSheetContent(
            recentPlaces = recentPlaces,
            nearbyStations = nearbyStations,
            onSearch = { nav.push(SheetPage.Search) },
            onDirections = { nav.push(SheetPage.Planning) },
            onTrains = { nav.push(SheetPage.TrainBoard()) },
            onOffline = { nav.push(SheetPage.Offline) },
            onSettings = { nav.push(SheetPage.Settings) },
            onRecent = { place ->
              homeViewModel.select(place)
              nav.push(SheetPage.PlaceDetail(place))
            },
            onStation = { nearby ->
              nav.push(SheetPage.TrainBoard(nearby.station.name, nearby.station.id))
            },
            onPeekContentHeight = { peekContentPx.intValue = it },
            collapsed = sheetState.draggable.targetValue == SheetAnchor.Bottom,
            gestureClearance = gestureClearance,
          )

          SheetPage.Search -> SearchPage(
            viewModel = searchViewModel,
            onBack = { popCurrent() },
            onPick = { result ->
              homeViewModel.select(result)
              searchViewModel.reset()
              nav.push(SheetPage.PlaceDetail(result))
            },
          )

          is SheetPage.PlaceDetail -> PlaceDetailPage(
            place = current.place,
            viewModel = placeViewModel,
            onBack = { popCurrent() },
            onDirections = { place ->
              planningViewModel.directionsTo(place)
              nav.push(SheetPage.Planning)
            },
            onTrainBoard = { place ->
              nav.push(SheetPage.TrainBoard(place.name, place.stationId))
            },
          )

          SheetPage.Planning -> PlanningPage(
            viewModel = planningViewModel,
            onBack = { popCurrent() },
            onPickEndpoint = { fromField -> nav.push(SheetPage.PlanningPicker(fromField)) },
            onOpenDetail = { nav.push(SheetPage.PlanningDetail) },
          )

          SheetPage.PlanningDetail -> PlanningDetailPage(
            viewModel = planningViewModel,
            onBack = { popCurrent() },
          )

          is SheetPage.PlanningPicker -> SearchPage(
            viewModel = searchViewModel,
            onBack = { popCurrent() },
            onPick = { result ->
              planningViewModel.setEndpoint(
                current.from,
                PlanEndpoint(result.name, result.point),
              )
              searchViewModel.reset()
              nav.pop()
            },
          )

          is SheetPage.TrainBoard -> TrainBoardPage(
            viewModel = trainsViewModel,
            initialQuery = current.stationQuery,
            initialStationId = current.stationId,
            onBack = { popCurrent() },
          )

          SheetPage.MapLayers -> MapLayersPage(
            current = mapMode,
            onSelect = { mode ->
              settingsViewModel.setMapMode(mode)
              nav.pop()
            },
            onContentHeight = { layersContentPx.floatValue = it.toFloat() },
          )

          SheetPage.Offline -> OfflinePage(
            viewModel = offlineViewModel,
            currentViewportBBox = { camera.viewportBBox() },
            onBack = { popCurrent() },
          )

          SheetPage.Settings -> SettingsPage(
            viewModel = settingsViewModel,
            onBack = { popCurrent() },
          )
        }
      }
    },
  )
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapControls(
  camera: it.iterapp.app.map.MapCameraController,
  onLayers: () -> Unit,
  onMyLocation: () -> Unit,
  insetPx: Int,
) {
  Column(
    modifier = Modifier
      .align(Alignment.BottomEnd)
      .offset { IntOffset(0, -insetPx) }
      .padding(end = 16.dp, bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    FloatingActionButton(
      onClick = onLayers,
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
      Icon(Icons.Rounded.Layers, contentDescription = stringResourceSafe(R.string.layers_title))
    }
    FloatingActionButton(onClick = onMyLocation) {
      Icon(Icons.Rounded.MyLocation, contentDescription = stringResourceSafe(R.string.map_my_location))
    }
  }
  // Compass at bottom-start so its appearance never shifts the action column.
  AnimatedVisibility(
    visible = kotlin.math.abs(camera.bearing) > 0.5,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier
      .align(Alignment.BottomStart)
      .offset { IntOffset(0, -insetPx) }
      .padding(start = 16.dp, bottom = 16.dp),
  ) {
    SmallFloatingActionButton(
      onClick = { camera.resetNorth() },
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      Icon(
        Icons.Rounded.Explore,
        contentDescription = stringResourceSafe(R.string.map_compass),
        // -45 compensates the Explore needle's natural northeast orientation.
        modifier = Modifier.graphicsLayer { rotationZ = -(camera.bearing + 45.0).toFloat() },
      )
    }
  }
}

@Composable
private fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)

/** Route overlay from an itinerary: transit legs solid in line color, walks dashed. */
private fun Itinerary.toRouteLines(walkColor: Int): List<RouteLine> =
  legs.mapNotNull { leg ->
    val points = leg.geometry.ifEmpty { listOf(leg.from.point, leg.to.point) }
    if (points.size < 2) return@mapNotNull null
    RouteLine(
      points = points,
      color = if (leg.isTransit) lineColor(leg.routeColor, leg.mode).toArgb() else walkColor,
      dashed = !leg.isTransit,
    )
  }
