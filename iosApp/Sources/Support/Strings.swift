import Foundation

/// UI copy, hardcoded English defaults mirroring the Android base strings.
/// Localization (including Italian) is roadmapped.
enum Strings {
  // Generic
  static let actionDelete = "Delete"
  static let errorNetwork = "Can't reach the server"
  static let errorGeneric = "Something went wrong"

  // Home sheet
  static let homeSearchHint = "Search places and addresses"
  static let homeQuickPlanning = "Directions"
  static let homeQuickBoards = "Trains"
  static let homeQuickOffline = "Offline"
  static let homeQuickSettings = "Settings"

  // Search
  static let searchPlaceholder = "Search places and addresses"
  static let searchNoResults = "Nothing found"

  // Place detail
  static let placeDirections = "Directions"
  static let placeTrainBoard = "Train board"

  // Planning
  static let planningTitle = "Directions"
  static let planningFrom = "From"
  static let planningTo = "To"
  static let planningSwap = "Swap origin and destination"
  static let planningMyLocation = "My location"
  static let planningChooseOnSearch = "Search an address"
  static let planningProfileFastest = "Fastest"
  static let planningProfileReliable = "Reliable"
  static let planningProfileBalanced = "Balanced"
  static let planningProfileEco = "Eco"
  static let planningProfileComfort = "Comfort"
  static let planningTransfersNone = "Direct"
  static let planningTransfersOne = "1 transfer"
  static func planningTransfers(_ count: Int) -> String { "\(count) transfers" }
  static func planningWalkDistance(_ distance: String) -> String { "\(distance) walk" }
  static let planningNoResults = "No routes found"
  static let planningDetailTitle = "Route detail"
  static func planningUsuallyDelayed(_ delay: String) -> String { "Usually +\(delay) here" }
  static let planningReliabilityHigh = "Usually on time"
  static let planningReliabilityLow = "Often delayed"
  static func planningStopsCount(_ count: Int) -> String { "\(count) stops" }

  // Train boards
  static let trainsTitle = "Train boards"
  static let trainsSearchHint = "Search a station"
  static let trainsDepartures = "Departures"
  static let trainsArrivals = "Arrivals"
  static func trainsPlatform(_ platform: String) -> String { "Plat. \(platform)" }
  static let trainsOnTime = "On time"
  static let trainsEmpty = "No trains right now"
  static let trainsChangeStation = "Change station"

  // Offline
  static let offlineTitle = "Offline maps"
  static let offlineDownloadCurrent = "Download current area"
  static func offlineDownloading(_ bytes: String) -> String { "Downloading… \(bytes)" }
  static let offlineEmpty = "No offline areas yet"
  static let offlineErrorTooLarge = "Area too large — zoom in and retry"
  static let offlineErrorBusy = "Server busy — try again in a moment"
  static func offlineAreaZoom(_ zoom: Int) -> String { "up to zoom \(zoom)" }

  // Settings
  static let settingsTitle = "Settings"
  static let settingsTheme = "Theme"
  static let settingsThemeSystem = "System"
  static let settingsThemeLight = "Light"
  static let settingsThemeDark = "Dark"
  static let settingsMapStyle = "Map style"
  static let settingsMapStandard = "Standard"
  static let settingsMapTransit = "Transit"
  static let settingsServer = "Server"
  static let settingsServerOrigin = "Gateway address"
  static let settingsServerDesc = "The Iter Maps server this app talks to"
  static let settingsServerSave = "Apply"

  // Map controls
  static let mapMyLocation = "My position"
  static let mapLayers = "Map style"
}
