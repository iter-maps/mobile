package it.iterapp.app.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import it.iterapp.app.sheet.SheetNavigator
import it.iterapp.app.sheet.SheetPage
import it.iterapp.app.sheet.SheetScaffold
import it.iterapp.app.sheet.rememberSheetState
import it.iterapp.app.trains.TrainBoardPage
import it.iterapp.app.trains.TrainBoardViewModel
import it.iterapp.app.ui.theme.LineColors
import it.iterapp.app.ui.theme.lineColor
import it.iterapp.core.model.Itinerary
import it.iterapp.core.settings.MapMode
import kotlinx.coroutines.launch
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
  val scope = rememberCoroutineScope()

  val styleUrl by homeViewModel.styleUrl.collectAsStateWithLifecycle()
  val userLocation by homeViewModel.userLocation.collectAsStateWithLifecycle()
  val selectedPlace by homeViewModel.selectedPlace.collectAsStateWithLifecycle()
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

  BackHandler(enabled = nav.canPop) { nav.pop() }

  // Camera follows selections: place at half-sheet zoom, itinerary fitted.
  LaunchedEffect(selectedPlace) {
    selectedPlace?.let { camera.animateTo(it.point, 16.0) }
  }
  LaunchedEffect(selectedItinerary) {
    selectedItinerary?.let { itinerary ->
      camera.fitTo(itinerary.legs.flatMap { leg -> leg.geometry.ifEmpty { listOf(leg.from.point, leg.to.point) } })
    }
  }

  val walkColor = LineColors.Walk.toArgb()
  val routeLines = remember(selectedItinerary) {
    selectedItinerary?.toRouteLines(walkColor) ?: emptyList()
  }
  val routeDots = remember(selectedItinerary, selectedPlace) {
    buildList {
      selectedItinerary?.let { itinerary ->
        itinerary.legs.filter { it.isTransit }.forEach { leg ->
          val color = lineColor(leg.routeColor, leg.mode).toArgb()
          add(RouteDot(leg.from.point, color))
          add(RouteDot(leg.to.point, color))
        }
      }
      selectedPlace?.let { add(RouteDot(it.point, 0xFF5A5DE0.toInt(), radius = 7f)) }
    }
  }

  val mapInset = remember { mutableIntStateOf(0) }
  val layersContentPx = remember { mutableFloatStateOf(0f) }
  val page = nav.current

  SheetScaffold(
    state = sheetState,
    peekHeight = page.peek,
    onMapInset = { mapInset.intValue = it },
    contentAnchorPx = if (page is SheetPage.MapLayers) {
      // handle + measured content + bottom clearance
      layersContentPx.floatValue + 140f
    } else {
      null
    },
    openAnchor = page.openAnchor,
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
          (slideInHorizontally { if (forward) it else -it })
            .togetherWith(slideOutHorizontally { if (forward) -it else it })
        },
        label = "sheet-page",
      ) { current ->
        when (current) {
          SheetPage.Home -> HomeSheetContent(onNavigate = { nav.push(it) })

          SheetPage.Search -> SearchPage(
            viewModel = searchViewModel,
            onBack = {
              searchViewModel.reset()
              nav.pop()
            },
            onPick = { result ->
              homeViewModel.select(result)
              searchViewModel.reset()
              nav.push(SheetPage.PlaceDetail(result))
            },
          )

          is SheetPage.PlaceDetail -> PlaceDetailPage(
            place = current.place,
            viewModel = placeViewModel,
            onBack = {
              homeViewModel.select(null)
              placeViewModel.clear()
              nav.pop()
            },
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
            onBack = {
              planningViewModel.reset()
              nav.pop()
            },
            onPickEndpoint = { fromField -> nav.push(SheetPage.PlanningPicker(fromField)) },
            onOpenDetail = { nav.push(SheetPage.PlanningDetail) },
          )

          SheetPage.PlanningDetail -> PlanningDetailPage(
            viewModel = planningViewModel,
            onBack = { nav.pop() },
          )

          is SheetPage.PlanningPicker -> SearchPage(
            viewModel = searchViewModel,
            onBack = {
              searchViewModel.reset()
              nav.pop()
            },
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
            onBack = {
              trainsViewModel.reset()
              nav.pop()
            },
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
            onBack = { nav.pop() },
          )

          SheetPage.Settings -> SettingsPage(
            viewModel = settingsViewModel,
            onBack = { nav.pop() },
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
      .padding(end = 14.dp, bottom = 18.dp),
  ) {
    SmallFloatingActionButton(
      onClick = onLayers,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      Icon(Icons.Rounded.Layers, contentDescription = stringResourceSafe(R.string.layers_title))
    }
    if (camera.bearing != 0.0) {
      SmallFloatingActionButton(
        onClick = { camera.resetNorth() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
      ) {
        Icon(Icons.Rounded.Explore, contentDescription = stringResourceSafe(R.string.map_compass))
      }
    }
    SmallFloatingActionButton(
      onClick = onMyLocation,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary,
    ) {
      Icon(Icons.Rounded.MyLocation, contentDescription = stringResourceSafe(R.string.map_my_location))
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
      color = if (leg.isTransit) {
        android.graphics.Color.parseColor(
          "#" + (leg.routeColor?.takeIf { it.length == 6 } ?: defaultHexFor(leg)),
        )
      } else {
        walkColor
      },
      dashed = !leg.isTransit,
    )
  }

private fun defaultHexFor(leg: it.iterapp.core.model.Leg): String = when (leg.mode) {
  it.iterapp.core.model.LegMode.SUBWAY -> "0570B5"
  it.iterapp.core.model.LegMode.TRAM -> "7A9E4E"
  it.iterapp.core.model.LegMode.BUS -> "3E7CB1"
  else -> "7B4EA3"
}
