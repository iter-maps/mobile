import CoreLocation
import MapLibre
import SwiftUI
import UIKit

/// One polyline of the route overlay: transit legs solid in their line color,
/// walk legs dashed gray.
struct RouteLine: Equatable {
  let coordinates: [CLLocationCoordinate2D]
  let color: UIColor
  let dashed: Bool

  static func == (lhs: RouteLine, rhs: RouteLine) -> Bool {
    lhs.dashed == rhs.dashed && lhs.color == rhs.color
      && lhs.coordinates.count == rhs.coordinates.count
      && zip(lhs.coordinates, rhs.coordinates).allSatisfy {
        $0.latitude == $1.latitude && $0.longitude == $1.longitude
      }
  }
}

/// Imperative camera handle. Each request carries a fresh id so the
/// representable applies it exactly once. Main-thread only by convention
/// (deliberately not actor-isolated: it is called from plain UI closures).
final class MapController: ObservableObject {
  enum Command {
    case center(CLLocationCoordinate2D, zoom: Double)
    case fit([CLLocationCoordinate2D])
  }

  struct Request: Equatable {
    let id: UUID
    let command: Command

    static func == (lhs: Request, rhs: Request) -> Bool { lhs.id == rhs.id }
  }

  @Published private(set) var request: Request?
  fileprivate weak var mapView: MLNMapView?

  func setCenter(latitude: Double, longitude: Double, zoom: Double = 15.5) {
    let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    request = Request(id: UUID(), command: .center(coordinate, zoom: zoom))
  }

  /// Fits the camera to the coordinates, with edge padding that keeps the
  /// geometry inside the map band the sheet leaves uncovered.
  func fit(_ coordinates: [CLLocationCoordinate2D]) {
    guard let first = coordinates.first else { return }
    guard coordinates.count > 1 else {
      return setCenter(latitude: first.latitude, longitude: first.longitude)
    }
    request = Request(id: UUID(), command: .fit(coordinates))
  }

  /// The current viewport as the wire bbox string `minLon,minLat,maxLon,maxLat`.
  func viewportBBox() -> String? {
    guard let mapView else { return nil }
    let bounds = mapView.visibleCoordinateBounds
    return "\(bounds.sw.longitude),\(bounds.sw.latitude),\(bounds.ne.longitude),\(bounds.ne.latitude)"
  }
}

/// MLNMapView as a SwiftUI node: style from the gateway, user location dot,
/// route overlay re-applied on every style (re)load, single-tap callback.
struct MapLibreView: UIViewRepresentable {
  let styleURL: URL?
  let routeLines: [RouteLine]
  let marker: CLLocationCoordinate2D?
  let showsUserLocation: Bool
  /// Pixels covered by the sheet at its resting height; feeds fit padding.
  let bottomInset: CGFloat
  @ObservedObject var controller: MapController
  var onTap: () -> Void = {}

  func makeCoordinator() -> Coordinator {
    Coordinator()
  }

  func makeUIView(context: Context) -> MLNMapView {
    let mapView = MLNMapView(frame: .zero, styleURL: styleURL)
    mapView.delegate = context.coordinator
    mapView.logoView.isHidden = true
    // The on-map "i" button is replaced by an always-visible "© OpenStreetMap"
    // credit over the map that opens the About-the-map screen (ODbL/OpenMapTiles
    // attribution stays visible / one-tap accessible — never removed outright).
    mapView.attributionButton.isHidden = true
    mapView.setCenter(
      CLLocationCoordinate2D(latitude: 41.9028, longitude: 12.4964),
      zoomLevel: 5,
      animated: false
    )

    let tap = UITapGestureRecognizer(
      target: context.coordinator,
      action: #selector(Coordinator.handleTap)
    )
    // Single taps must wait for the map's own double-tap zoom gesture.
    for recognizer in mapView.gestureRecognizers ?? [] {
      if let doubleTap = recognizer as? UITapGestureRecognizer, doubleTap.numberOfTapsRequired == 2 {
        tap.require(toFail: doubleTap)
      }
    }
    mapView.addGestureRecognizer(tap)

    context.coordinator.lastStyleURL = styleURL
    controller.mapView = mapView
    return mapView
  }

  func updateUIView(_ mapView: MLNMapView, context: Context) {
    let coordinator = context.coordinator
    coordinator.onTap = onTap
    coordinator.bottomInset = bottomInset

    if let styleURL, coordinator.lastStyleURL != styleURL {
      coordinator.lastStyleURL = styleURL
      mapView.styleURL = styleURL
    }
    if mapView.showsUserLocation != showsUserLocation {
      mapView.showsUserLocation = showsUserLocation
    }
    coordinator.syncRoute(routeLines, on: mapView)
    coordinator.syncMarker(marker, on: mapView)
    if let request = controller.request {
      coordinator.apply(request, on: mapView)
    }
  }

  final class Coordinator: NSObject, MLNMapViewDelegate {
    var onTap: () -> Void = {}
    var bottomInset: CGFloat = 0
    var lastStyleURL: URL?

    private var currentLines: [RouteLine] = []
    private var addedLayerIds: [String] = []
    private var addedSourceIds: [String] = []
    private var markerAnnotation: MLNPointAnnotation?
    private var lastRequestId: UUID?

    @objc func handleTap() {
      onTap()
    }

    func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
      // A fresh style dropped any previously added overlay layers.
      addedLayerIds = []
      addedSourceIds = []
      applyRoute(to: style)
    }

    func syncRoute(_ lines: [RouteLine], on mapView: MLNMapView) {
      guard lines != currentLines else { return }
      currentLines = lines
      guard let style = mapView.style else { return }
      applyRoute(to: style)
    }

    private func applyRoute(to style: MLNStyle) {
      for id in addedLayerIds {
        if let layer = style.layer(withIdentifier: id) { style.removeLayer(layer) }
      }
      for id in addedSourceIds {
        if let source = style.source(withIdentifier: id) { style.removeSource(source) }
      }
      addedLayerIds = []
      addedSourceIds = []

      var sources: [(line: RouteLine, source: MLNShapeSource)] = []
      for (index, line) in currentLines.enumerated() {
        var coordinates = line.coordinates
        let polyline = MLNPolyline(coordinates: &coordinates, count: UInt(coordinates.count))
        let source = MLNShapeSource(identifier: "iter-route-src-\(index)", shape: polyline, options: nil)
        style.addSource(source)
        addedSourceIds.append(source.identifier)
        sources.append((line, source))
      }

      // All casings first so no leg's casing sits above another leg's line.
      for (index, entry) in sources.enumerated() where !entry.line.dashed {
        let casing = MLNLineStyleLayer(identifier: "iter-route-casing-\(index)", source: entry.source)
        casing.lineColor = NSExpression(forConstantValue: UIColor.white)
        casing.lineWidth = NSExpression(forConstantValue: 9)
        casing.lineCap = NSExpression(forConstantValue: "round")
        casing.lineJoin = NSExpression(forConstantValue: "round")
        style.addLayer(casing)
        addedLayerIds.append(casing.identifier)
      }
      for (index, entry) in sources.enumerated() {
        let layer = MLNLineStyleLayer(identifier: "iter-route-line-\(index)", source: entry.source)
        layer.lineColor = NSExpression(forConstantValue: entry.line.color)
        layer.lineCap = NSExpression(forConstantValue: "round")
        if entry.line.dashed {
          layer.lineWidth = NSExpression(forConstantValue: 4.5)
          layer.lineDashPattern = NSExpression(forConstantValue: [0.2, 1.8])
        } else {
          layer.lineWidth = NSExpression(forConstantValue: 5.5)
          layer.lineJoin = NSExpression(forConstantValue: "round")
        }
        style.addLayer(layer)
        addedLayerIds.append(layer.identifier)
      }
    }

    func syncMarker(_ coordinate: CLLocationCoordinate2D?, on mapView: MLNMapView) {
      guard let coordinate else {
        if let existing = markerAnnotation {
          mapView.removeAnnotation(existing)
          markerAnnotation = nil
        }
        return
      }
      if let existing = markerAnnotation {
        if existing.coordinate.latitude != coordinate.latitude
          || existing.coordinate.longitude != coordinate.longitude {
          existing.coordinate = coordinate
        }
      } else {
        let annotation = MLNPointAnnotation()
        annotation.coordinate = coordinate
        mapView.addAnnotation(annotation)
        markerAnnotation = annotation
      }
    }

    func apply(_ request: MapController.Request, on mapView: MLNMapView) {
      guard request.id != lastRequestId else { return }
      lastRequestId = request.id
      switch request.command {
      case .center(let coordinate, let zoom):
        mapView.setCenter(coordinate, zoomLevel: zoom, animated: true)
      case .fit(let coordinates):
        var minLat = coordinates[0].latitude, maxLat = coordinates[0].latitude
        var minLon = coordinates[0].longitude, maxLon = coordinates[0].longitude
        for coordinate in coordinates {
          minLat = min(minLat, coordinate.latitude)
          maxLat = max(maxLat, coordinate.latitude)
          minLon = min(minLon, coordinate.longitude)
          maxLon = max(maxLon, coordinate.longitude)
        }
        let bounds = MLNCoordinateBounds(
          sw: CLLocationCoordinate2D(latitude: minLat, longitude: minLon),
          ne: CLLocationCoordinate2D(latitude: maxLat, longitude: maxLon)
        )
        let padding = UIEdgeInsets(top: 80, left: 48, bottom: bottomInset + 48, right: 48)
        mapView.setVisibleCoordinateBounds(
          bounds,
          edgePadding: padding,
          animated: true,
          completionHandler: nil
        )
      }
    }
  }
}
