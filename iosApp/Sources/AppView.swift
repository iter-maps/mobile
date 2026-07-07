import CoreLocation
import IterCore
import SwiftUI

/// The single full-screen destination (ADR 0008): persistent map canvas plus
/// the universal bottom sheet — a permanently presented native sheet hosting
/// a NavigationStack of pages, so it inherits Liquid Glass materials for free
/// (ADR 0009).
struct AppView: View {
  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var settings: SettingsModel
  @EnvironmentObject private var planning: PlanningModel
  @EnvironmentObject private var location: LocationProvider
  @Environment(\.colorScheme) private var colorScheme

  @StateObject private var mapController = MapController()
  @State private var sheetVisible = true

  var body: some View {
    GeometryReader { geo in
      MapLibreView(
        styleURL: URL(string: settings.styleUrl(dark: effectiveDark)),
        routeLines: routeLines,
        marker: markerCoordinate,
        showsUserLocation: location.isAuthorized,
        bottomInset: geo.size.height * 0.45,
        controller: mapController,
        onTap: handleMapTap
      )
      .ignoresSafeArea()
    }
    .overlay(alignment: .topTrailing) { mapChrome }
    .sheet(isPresented: $sheetVisible) {
      SheetRoot(bboxProvider: { mapController.viewportBBox() })
        .presentationDetents([.height(SheetMetrics.peek), .medium, .large], selection: $app.detent)
        .presentationBackgroundInteraction(.enabled)
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(true)
    }
    .onAppear { location.requestPermission() }
    .onChange(of: app.selectedPlace) { _, place in
      if let place {
        mapController.setCenter(latitude: place.point.lat, longitude: place.point.lon, zoom: 16)
      }
    }
    .onChange(of: planning.selected) { _, itinerary in
      if let itinerary {
        mapController.fit(fitCoordinates(itinerary))
      }
    }
    .preferredColorScheme(settings.preferredColorScheme)
    // brand.seed doubles as the iOS accent (docs/design/tokens.md).
    .tint(Color(rgb: 0x888FFA))
  }

  private var effectiveDark: Bool {
    switch settings.themeMode {
    case "LIGHT": return false
    case "DARK": return true
    default: return colorScheme == .dark
    }
  }

  private var markerCoordinate: CLLocationCoordinate2D? {
    app.selectedPlace.map {
      CLLocationCoordinate2D(latitude: $0.point.lat, longitude: $0.point.lon)
    }
  }

  /// Route overlay from the selected itinerary: transit legs solid in their
  /// line color, walk legs dashed gray.
  private var routeLines: [RouteLine] {
    guard let itinerary = planning.selected else { return [] }
    return itinerary.legs.compactMap { leg in
      let geometry: [GeoPoint] = leg.geometry.isEmpty ? [leg.from.point, leg.to.point] : leg.geometry
      guard geometry.count >= 2 else { return nil }
      let coordinates = geometry.map {
        CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon)
      }
      return RouteLine(
        coordinates: coordinates,
        color: leg.isTransit ? lineUIColor(routeColor: leg.routeColor, mode: leg.mode) : LineColors.walk,
        dashed: !leg.isTransit
      )
    }
  }

  private func fitCoordinates(_ itinerary: Itinerary) -> [CLLocationCoordinate2D] {
    itinerary.legs.flatMap { leg -> [CLLocationCoordinate2D] in
      let geometry: [GeoPoint] = leg.geometry.isEmpty ? [leg.from.point, leg.to.point] : leg.geometry
      return geometry.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon) }
    }
  }

  /// Map tap dismisses the current place selection (ADR 0008: the map is the
  /// primary surface, the sheet a conversation over it).
  private func handleMapTap() {
    if app.path.last == .placeDetail {
      app.pop()
    }
  }

  // MARK: - Floating map chrome (Liquid Glass, ADR 0009)

  private var mapChrome: some View {
    VStack(spacing: 10) {
      Menu {
        Picker(Strings.mapLayers, selection: $settings.mapMode) {
          Label(Strings.settingsMapStandard, systemImage: "map").tag("STANDARD")
          Label(Strings.settingsMapTransit, systemImage: "tram.fill").tag("TRANSIT")
        }
      } label: {
        ChromeIcon(systemName: "square.3.stack.3d")
      }
      .accessibilityLabel(Strings.mapLayers)

      locateButton
        .accessibilityLabel(Strings.mapMyLocation)
    }
    .padding(.trailing, 14)
    .padding(.top, 4)
  }

  @ViewBuilder
  private var locateButton: some View {
    let label = Image(systemName: "location")
      .font(.system(size: 17, weight: .semibold))
      .frame(width: 44, height: 44)
    // Glass APIs need the iOS 26 SDK at build time too (Xcode 26 / Swift
    // 6.2+); availability alone can't guard symbols older SDKs don't have.
    #if compiler(>=6.2)
    if #available(iOS 26.0, *) {
      Button(action: locate) { label }
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
    } else {
      Button(action: locate) {
        label.background(.regularMaterial, in: Circle())
      }
    }
    #else
    Button(action: locate) {
      label.background(.regularMaterial, in: Circle())
    }
    #endif
  }

  private func locate() {
    guard let point = location.lastKnownPoint else { return }
    mapController.setCenter(latitude: point.lat, longitude: point.lon)
  }
}

/// Circular glass chrome icon with the pre-26 material fallback (ADR 0009:
/// the real glass or the system material, never a re-implementation).
private struct ChromeIcon: View {
  let systemName: String

  var body: some View {
    let icon = Image(systemName: systemName)
      .font(.system(size: 17, weight: .semibold))
      .foregroundStyle(.primary)
      .frame(width: 44, height: 44)
      .contentShape(Circle())
    #if compiler(>=6.2)
    if #available(iOS 26.0, *) {
      icon.glassEffect(in: Circle())
    } else {
      icon.background(.regularMaterial, in: Circle())
    }
    #else
    icon.background(.regularMaterial, in: Circle())
    #endif
  }
}

/// The sheet's internal page stack; system back pops it (ADR 0008).
private struct SheetRoot: View {
  let bboxProvider: () -> String?

  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var planning: PlanningModel
  @EnvironmentObject private var boards: BoardsModel

  var body: some View {
    NavigationStack(path: $app.path) {
      HomePage()
        .navigationDestination(for: SheetRoute.self) { route in
          switch route {
          case .search:
            SearchPage(mode: .explore)
          case .placeDetail:
            PlaceDetailPage()
          case .planning:
            PlanningPage()
          case .planningDetail:
            PlanningDetailPage()
          case .planningPicker(let fromField):
            SearchPage(mode: .picker(fromField: fromField))
          case .boards(let initialQuery):
            BoardsPage(initialQuery: initialQuery)
          case .offline:
            OfflinePage(currentBBox: bboxProvider)
          case .settings:
            SettingsPage()
          }
        }
    }
    .onChange(of: app.path) { _, path in
      housekeeping(path)
    }
  }

  /// Page state resets when its route leaves the stack (ADR 0008: ViewModels
  /// hang off one host and must be reset deliberately).
  private func housekeeping(_ path: [SheetRoute]) {
    if !path.contains(.placeDetail) {
      app.selectedPlace = nil
    }
    let planningActive = path.contains { route in
      switch route {
      case .planning, .planningDetail, .planningPicker: return true
      default: return false
      }
    }
    if !planningActive && planning.hasContent {
      planning.reset()
    }
  }
}
