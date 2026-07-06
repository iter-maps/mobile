import CoreLocation
import Foundation
import IterCore

/// When-in-use location via CoreLocation (ADR 0011: platform location, no
/// proprietary SDKs). Published state is always mutated on the main queue.
final class LocationProvider: NSObject, ObservableObject, CLLocationManagerDelegate {
  private let manager = CLLocationManager()

  @Published private(set) var isAuthorized = false
  @Published private(set) var lastLocation: CLLocation?

  override init() {
    super.init()
    manager.delegate = self
    manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
  }

  func requestPermission() {
    if manager.authorizationStatus == .notDetermined {
      manager.requestWhenInUseAuthorization()
    }
  }

  var lastKnownPoint: GeoPoint? {
    lastLocation.map { GeoPoint(lat: $0.coordinate.latitude, lon: $0.coordinate.longitude) }
  }

  func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
    let status = manager.authorizationStatus
    let authorized = status == .authorizedWhenInUse || status == .authorizedAlways
    DispatchQueue.main.async {
      self.isAuthorized = authorized
      if authorized { self.manager.startUpdatingLocation() }
    }
  }

  func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    guard let location = locations.last else { return }
    DispatchQueue.main.async { self.lastLocation = location }
  }

  func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}
}
