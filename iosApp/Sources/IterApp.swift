import IterCore
import SwiftUI

/// Starts the shared KMP core exactly once (Koin graph behind the facade,
/// ADR 0002) and hands the shell plain repository objects.
final class CoreProvider {
  static let shared = CoreProvider()

  let core: IterCore

  private init() {
    core = IterCore.companion.start()
  }
}

@main
struct IterApp: App {
  @StateObject private var location: LocationProvider
  @StateObject private var app: AppModel
  @StateObject private var search: SearchModel
  @StateObject private var planning: PlanningModel
  @StateObject private var boards: BoardsModel
  @StateObject private var offline: OfflineModel
  @StateObject private var place: PlaceModel
  @StateObject private var settings: SettingsModel
  @StateObject private var connectivity: ConnectivityModel

  init() {
    let core = CoreProvider.shared.core
    let location = LocationProvider()
    _location = StateObject(wrappedValue: location)
    _app = StateObject(wrappedValue: AppModel())
    _search = StateObject(wrappedValue: SearchModel(core: core, location: location))
    _planning = StateObject(wrappedValue: PlanningModel(core: core, location: location))
    _boards = StateObject(wrappedValue: BoardsModel(core: core))
    _offline = StateObject(wrappedValue: OfflineModel(core: core))
    _place = StateObject(wrappedValue: PlaceModel(core: core))
    _settings = StateObject(wrappedValue: SettingsModel(core: core))
    _connectivity = StateObject(wrappedValue: ConnectivityModel(core: core))
  }

  var body: some Scene {
    WindowGroup {
      AppView()
        .environmentObject(location)
        .environmentObject(app)
        .environmentObject(search)
        .environmentObject(planning)
        .environmentObject(boards)
        .environmentObject(offline)
        .environmentObject(place)
        .environmentObject(settings)
        .environmentObject(connectivity)
    }
  }
}
