import Foundation

/// UI copy, hardcoded English defaults mirroring the Android base strings.
/// Localization (including Italian) is roadmapped.
enum Strings {
  // Generic
  static let actionDelete = "Delete"
  static let actionRetry = "Retry"
  static let errorNetwork = "Can't reach the server"
  static let errorGeneric = "Something went wrong"

  // Error states (mirror the shared AppFailure taxonomy)
  static let errorOfflineTitle = "You're offline"
  static let errorOfflineBody = "Reconnect to plan trips"
  static let errorOfflineBodyWithMaps = "Downloaded maps still work, but planning needs a connection"
  static let errorUnreachableTitle = "Iter Maps is unreachable"
  static let errorUnreachableBody = "We couldn't reach the server. Try again in a moment."
  static let errorTimeoutTitle = "Taking too long"
  static let errorTimeoutBody = "The server took too long to respond."
  static let errorBusyTitle = "Server busy"
  static let errorBusyBody = "Iter Maps is busy right now — try again shortly."
  static let errorNotFoundTitle = "Nothing found"
  static let errorNotFoundBody = "We couldn't find what you were looking for."
  static let errorGenericTitle = "Something went wrong"
  static let errorGenericBody = "An unexpected error occurred. Please try again."

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
  static func placePhotoAttribution(_ author: String) -> String { "Photo: \(author)" }

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
  static let trainsStale = "Offline — showing the last update"

  // Offline
  static let offlineTitle = "Offline maps"
  static let offlineDownloadCurrent = "Download current area"
  static func offlineDownloading(_ bytes: String) -> String { "Downloading… \(bytes)" }
  static let offlineInstalling = "Installing…"
  static let offlineEmpty = "No offline areas yet"
  static let offlineErrorTooLarge = "Area too large — zoom in and retry"
  static let offlineErrorInvalidArea = "Invalid map area — adjust and retry"
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

  // Settings sections
  static let settingsAppearance = "Appearance"
  static let settingsMapLocation = "Map & Location"
  static let settingsOffline = "Offline maps"
  static let settingsAbout = "About & Legal"
  static let settingsVersion = "Version"
  static let settingsAttribution = "Map data & attribution"
  static let settingsLicenses = "Open-source licenses"
  static let settingsHelp = "Help"
  static let settingsReplayOnboarding = "Replay onboarding"
  static let settingsLocationPermission = "Location permission"
  static let settingsLocationGranted = "Granted"
  static let settingsLocationDenied = "Open Settings"
  static let settingsAdvanced = "Advanced"

  // Attribution / About the map
  static let attributionTitle = "Map data & attribution"
  static let attributionOsm = "© OpenStreetMap contributors"
  static let attributionOsmDesc = "Basemap data is © OpenStreetMap contributors, available under the Open Database License (ODbL)."
  static let attributionOpenMapTiles = "© OpenMapTiles"
  static let attributionOpenMapTilesDesc = "Vector tile schema by OpenMapTiles."
  static let attributionLicensesPlaceholder = "Open-source license details are bundled with the app."
  static let attributionOpenOsm = "Open openstreetmap.org/copyright"

  // Map controls
  static let mapMyLocation = "My position"
  static let mapLayers = "Map style"
  static let mapCreditOsm = "© OpenStreetMap"
  static let mapOfflineChip = "Offline — downloaded map"
}
