import Foundation
import IterCore
import SwiftUI
import UIKit

// MARK: - Sheet navigation

/// Pages hosted by the universal sheet's internal stack (ADR 0008).
enum SheetRoute: Hashable {
  case search
  case placeDetail
  case planning
  case planningDetail
  case planningPicker(fromField: Bool)
  case boards(initialQuery: String?)
  case offline
  case settings
  case attribution
}

enum SheetMetrics {
  /// `sheet.anchor.peek` (docs/design/tokens.md): the home resting height.
  static let peek: CGFloat = 180
}

/// Sheet stack + map selection state, the iOS twin of the Android host screen.
@MainActor
final class AppModel: ObservableObject {
  @Published var path: [SheetRoute] = []
  @Published var detent: PresentationDetent = .height(SheetMetrics.peek)
  /// The place currently highlighted on the map (from search).
  @Published var selectedPlace: SearchResult?

  func push(_ route: SheetRoute) {
    // Dismiss the keyboard BEFORE the detent flips so the IME collapses ahead
    // of the sheet resize instead of overlapping it (see keyboard-timing).
    resignFirstResponder()
    path.append(route)
    detent = preferredDetent(for: route)
  }

  func pop() {
    guard !path.isEmpty else { return }
    // Same ordering guarantee as push: resign first responder, then mutate the
    // path (which drives the sheet resize back down).
    resignFirstResponder()
    path.removeLast()
  }

  private func resignFirstResponder() {
    UIApplication.shared.sendAction(
      #selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil
    )
  }

  private func preferredDetent(for route: SheetRoute) -> PresentationDetent {
    switch route {
    case .search, .planningPicker: return .large
    default: return .medium
    }
  }
}

// MARK: - Search

/// Debounced Photon autocomplete, biased to the user's position. Shared by
/// the Search page and the planning endpoint picker.
@MainActor
final class SearchModel: ObservableObject {
  @Published var query = "" {
    didSet { scheduleSearch() }
  }
  @Published private(set) var results: [SearchResult] = []
  @Published private(set) var isSearching = false
  /// Typed failure so a network error is never shown as "Nothing found".
  @Published private(set) var failure: AppFailure?

  private let core: IterCore
  private let location: LocationProvider
  private var searchTask: Task<Void, Never>?

  init(core: IterCore, location: LocationProvider) {
    self.core = core
    self.location = location
  }

  private func scheduleSearch() {
    searchTask?.cancel()
    let trimmed = query.trimmingCharacters(in: .whitespaces)
    guard trimmed.count >= 2 else {
      results = []
      failure = nil
      isSearching = false
      return
    }
    searchTask = Task { [weak self] in
      try? await Task.sleep(for: .milliseconds(300))
      guard let self, !Task.isCancelled else { return }
      self.isSearching = true
      do {
        let found = try await self.core.search.search(
          query: trimmed,
          lang: Locale.current.language.languageCode?.identifier,
          bias: self.location.lastKnownPoint,
          limit: 15
        )
        if !Task.isCancelled {
          self.results = found
          self.failure = nil
        }
      } catch {
        if !Task.isCancelled {
          self.results = []
          self.failure = AppFailure.from(error, isOnline: self.core.isOnline())
        }
      }
      if !Task.isCancelled { self.isSearching = false }
    }
  }

  /// Re-runs the last query after a failure.
  func retry() {
    scheduleSearch()
  }

  func reset() {
    searchTask?.cancel()
    query = ""
    results = []
    failure = nil
    isSearching = false
  }
}

// MARK: - Planning

/// One endpoint of a plan: a named point, possibly "my location".
struct PlanEndpoint: Equatable {
  var name: String
  var lat: Double
  var lon: Double
  var isUserLocation = false
}

/// Ranking profile choices; wireValue nil = OTP's own order (mirrors Android).
enum PlanProfile: CaseIterable {
  case fastest, reliable, balanced, eco, comfort

  var wireValue: String? {
    switch self {
    case .fastest: return nil
    case .reliable: return "reliability"
    case .balanced: return "balanced"
    case .eco: return "eco"
    case .comfort: return "comfort"
    }
  }

  var label: String {
    switch self {
    case .fastest: return Strings.planningProfileFastest
    case .reliable: return Strings.planningProfileReliable
    case .balanced: return Strings.planningProfileBalanced
    case .eco: return Strings.planningProfileEco
    case .comfort: return Strings.planningProfileComfort
    }
  }
}

enum PlanState {
  case idle
  case loading
  case results([Itinerary])
  case failure(AppFailure)
}

@MainActor
final class PlanningModel: ObservableObject {
  @Published var from: PlanEndpoint?
  @Published var to: PlanEndpoint?
  @Published private(set) var profile: PlanProfile = .fastest
  @Published private(set) var state: PlanState = .idle
  @Published var selected: Itinerary?

  private let core: IterCore
  private let location: LocationProvider
  private var planTask: Task<Void, Never>?

  // Kotlin default arguments do not cross the ObjC boundary: PlanParams and
  // plan() take every argument explicitly. TODO(macos-ci): verify the
  // PlanTransitMode member names (bus/subway/tram/rail) in the generated header.
  private static let allTransitModes: Set<PlanTransitMode> = [.bus, .subway, .tram, .rail]

  init(core: IterCore, location: LocationProvider) {
    self.core = core
    self.location = location
  }

  var hasContent: Bool {
    from != nil || to != nil || selected != nil
  }

  /// Entry point from a place page: destination set, origin = user location.
  func directionsTo(name: String, lat: Double, lon: Double) {
    to = PlanEndpoint(name: name, lat: lat, lon: lon)
    if from == nil, let point = location.lastKnownPoint {
      from = PlanEndpoint(name: "", lat: point.lat, lon: point.lon, isUserLocation: true)
    }
    replan()
  }

  func setEndpoint(fromField: Bool, name: String, lat: Double, lon: Double) {
    let endpoint = PlanEndpoint(name: name, lat: lat, lon: lon)
    if fromField { from = endpoint } else { to = endpoint }
    replan()
  }

  func useMyLocation(fromField: Bool) {
    guard let point = location.lastKnownPoint else { return }
    let endpoint = PlanEndpoint(name: "", lat: point.lat, lon: point.lon, isUserLocation: true)
    if fromField { from = endpoint } else { to = endpoint }
    replan()
  }

  func swap() {
    let previousFrom = from
    from = to
    to = previousFrom
    replan()
  }

  func setProfile(_ value: PlanProfile) {
    guard profile != value else { return }
    profile = value
    replan()
  }

  func replan() {
    guard let origin = from, let destination = to else { return }
    planTask?.cancel()
    planTask = Task { [weak self] in
      guard let self else { return }
      self.state = .loading
      self.selected = nil
      do {
        let params = PlanParams(
          fromLat: origin.lat,
          fromLon: origin.lon,
          toLat: destination.lat,
          toLon: destination.lon,
          date: nil,
          time: nil,
          arriveBy: false,
          numItineraries: 5,
          wheelchair: false,
          walkReluctance: 2.0,
          walkSpeed: 1.33,
          maxTransfers: nil,
          transitModes: Self.allTransitModes
        )
        let itineraries = try await self.core.plan.plan(
          params: params,
          rerank: self.profile.wireValue,
          predictHistorical: true
        )
        guard !Task.isCancelled else { return }
        self.state = .results(itineraries)
        self.selected = itineraries.first
      } catch {
        if !Task.isCancelled {
          self.state = .failure(AppFailure.from(error, isOnline: self.core.isOnline()))
        }
      }
    }
  }

  func reset() {
    planTask?.cancel()
    from = nil
    to = nil
    selected = nil
    profile = .fastest
    state = .idle
  }
}

// MARK: - Train boards

enum BoardTab {
  case departures, arrivals
}

enum BoardState {
  case idle
  case loading
  /// `stale` marks a last-good board kept on screen after a poll failed.
  case loaded([BoardEntry], stale: Bool)
  case failure(AppFailure)
}

@MainActor
final class BoardsModel: ObservableObject {
  @Published var stationQuery = "" {
    didSet { scheduleStationSearch() }
  }
  @Published private(set) var stations: [Station] = []
  /// Typed failure for the station search, so a network error isn't shown as
  /// an empty list.
  @Published private(set) var stationsFailure: AppFailure?
  @Published var selectedStation: Station? {
    didSet { restartPolling() }
  }
  @Published var tab: BoardTab = .departures {
    didSet { restartPolling() }
  }
  @Published private(set) var board: BoardState = .idle

  private let core: IterCore
  private var searchTask: Task<Void, Never>?
  private var pollTask: Task<Void, Never>?
  /// The last board successfully fetched; kept on screen (marked stale) when a
  /// subsequent poll fails, mirroring Android's boardPollFlow.
  private var lastGoodEntries: [BoardEntry]?

  /// Boards are server-cached with max-age=20 (TrainsRepository contract);
  /// never poll faster.
  private static let pollSeconds = 20

  init(core: IterCore) {
    self.core = core
  }

  private func scheduleStationSearch() {
    searchTask?.cancel()
    let trimmed = stationQuery.trimmingCharacters(in: .whitespaces)
    guard trimmed.count >= 2 else {
      stations = []
      stationsFailure = nil
      return
    }
    searchTask = Task { [weak self] in
      try? await Task.sleep(for: .milliseconds(300))
      guard let self, !Task.isCancelled else { return }
      do {
        let found = try await self.core.trains.searchStations(query: trimmed)
        if !Task.isCancelled {
          self.stations = found
          self.stationsFailure = nil
        }
      } catch {
        if !Task.isCancelled {
          self.stations = []
          self.stationsFailure = AppFailure.from(error, isOnline: self.core.isOnline())
        }
      }
    }
  }

  /// Re-runs the station search after a failure.
  func retryStationSearch() {
    scheduleStationSearch()
  }

  /// Re-runs the board poll after a failure with nothing to show.
  func retryBoard() {
    restartPolling()
  }

  private func restartPolling() {
    pollTask?.cancel()
    lastGoodEntries = nil
    guard let station = selectedStation else {
      board = .idle
      return
    }
    let tab = tab
    board = .loading
    pollTask = Task { [weak self] in
      while !Task.isCancelled {
        guard let self else { return }
        do {
          let entries: [BoardEntry]
          switch tab {
          case .departures: entries = try await self.core.trains.departures(stationId: station.id)
          case .arrivals: entries = try await self.core.trains.arrivals(stationId: station.id)
          }
          if Task.isCancelled { return }
          self.lastGoodEntries = entries
          self.board = .loaded(entries, stale: false)
        } catch {
          if Task.isCancelled { return }
          // Keep the last good board (marked stale) rather than blanking it;
          // only surface a full StatusView when there's nothing to show.
          if let last = self.lastGoodEntries {
            self.board = .loaded(last, stale: true)
          } else {
            self.board = .failure(AppFailure.from(error, isOnline: self.core.isOnline()))
          }
        }
        try? await Task.sleep(for: .seconds(BoardsModel.pollSeconds))
      }
    }
  }

  func reset() {
    searchTask?.cancel()
    pollTask?.cancel()
    pollTask = nil
    stationQuery = ""
    stations = []
    stationsFailure = nil
    selectedStation = nil
    tab = .departures
    board = .idle
    lastGoodEntries = nil
  }
}

// MARK: - Offline areas

enum DownloadState {
  case idle
  case downloading(read: Int64, total: Int64?)
  case installing
  case failed(code: String?)
}

@MainActor
final class OfflineModel: ObservableObject {
  @Published private(set) var areas: [OfflineArea] = []
  @Published private(set) var download: DownloadState = .idle

  private let core: IterCore

  init(core: IterCore) {
    self.core = core
  }

  func refresh() {
    areas = core.offline.list()
  }

  /// Downloads the given viewport bbox (`minLon,minLat,maxLon,maxLat`) as a
  /// new offline area.
  func downloadArea(bbox: String) {
    if case .downloading = download { return }
    // Pre-flight the server's area cap so an over-large viewport fails fast.
    if Self.exceedsAreaCap(bbox) {
      download = .failed(code: "AREA_TOO_LARGE")
      return
    }
    download = .downloading(read: 0, total: nil)
    Task { [weak self] in
      guard let self else { return }
      do {
        let areaId = "area-\(Int64(Date().timeIntervalSince1970 * 1000))"
        _ = try await self.core.offline.install(
          areaId: areaId,
          bbox: bbox,
          maxzoom: KotlinInt(int: 14),
          onProgress: { read, total in
            // Kotlin invokes this off the main thread.
            let readValue = read.int64Value
            let totalValue = total?.int64Value
            Task { @MainActor in
              // Once bytes are in, the remaining work is the on-device unpack.
              if let totalValue, readValue >= totalValue {
                self.download = .installing
              } else {
                self.download = .downloading(read: readValue, total: totalValue)
              }
            }
          }
        )
        self.download = .idle
        self.refresh()
      } catch {
        self.download = .failed(code: Self.apiErrorCode(error))
      }
    }
  }

  private static func exceedsAreaCap(_ bbox: String) -> Bool {
    let parts = bbox.split(separator: ",").compactMap { Double($0.trimmingCharacters(in: .whitespaces)) }
    guard parts.count == 4 else { return false }
    let area = BBoxFormat.shared.areaDeg2(minLon: parts[0], minLat: parts[1], maxLon: parts[2], maxLat: parts[3])
    return area > BBoxFormat.shared.OFFLINE_AREA_CAP_DEG2
  }

  func delete(areaId: String) {
    core.offline.delete(areaId: areaId)
    refresh()
  }

  func dismissError() {
    if case .failed = download { download = .idle }
  }

  static func isTooLarge(_ code: String?) -> Bool {
    code == "AREA_TOO_LARGE"
  }

  static func isInvalidArea(_ code: String?) -> Bool {
    code == "BBOX_INVALID" || code == "BBOX_OUT_OF_RANGE" ||
      code == "BBOX_DEGENERATE" || code == "BBOX_REQUIRED"
  }

  static func isBusy(_ code: String?) -> Bool {
    code == "BUSY"
  }

  /// A Kotlin exception crossing the @Throws boundary arrives as an NSError
  /// carrying the original throwable under the "KotlinException" userInfo key.
  private static func apiErrorCode(_ error: Error) -> String? {
    let kotlinException = (error as NSError).userInfo["KotlinException"]
    return (kotlinException as? IterApiException)?.code
  }
}

// MARK: - Connectivity & offline-map fallback

/// Tracks reachability from the shared `Connectivity` monitor and decides,
/// given the current viewport and the installed offline areas, whether to fall
/// back to a downloaded map style when the device is offline (coherence with
/// offline zones — item 7).
@MainActor
final class ConnectivityModel: ObservableObject {
  @Published private(set) var isOnline = true

  private let core: IterCore

  init(core: IterCore) {
    self.core = core
    // Fires with the current value immediately and on every change.
    core.observeOnline { [weak self] online in
      let value = online.boolValue
      Task { @MainActor in self?.isOnline = value }
    }
  }

  /// Point-in-time reachability, for classifying a caught error at the call site.
  var isOnlineNow: Bool { core.isOnline() }

  /// The offline style URL to use for the given viewport, or nil to keep the
  /// live style. Returns a downloaded style only when offline AND an installed
  /// area covers the viewport center.
  func offlineStyleURL(styleName: String, bbox: String?, areas: [OfflineArea]) -> String? {
    guard !isOnline, let bbox else { return nil }
    guard let area = Self.areaCovering(bbox: bbox, in: areas) else { return nil }
    // Only offer an offline style the bundle actually contains.
    guard area.manifest.styles.contains(styleName) else {
      // Fall back to a base style the bundle does carry, if any.
      guard let fallback = area.manifest.styles.first else { return nil }
      return area.styleUrl(styleName: fallback)
    }
    return area.styleUrl(styleName: styleName)
  }

  /// True when we are offline and a downloaded area covers the viewport — the
  /// signal for the "Offline — downloaded map" chip.
  func isServingOfflineMap(bbox: String?, areas: [OfflineArea]) -> Bool {
    guard !isOnline, let bbox else { return false }
    return Self.areaCovering(bbox: bbox, in: areas) != nil
  }

  /// Picks an installed area whose bbox contains the viewport center. Manifest
  /// bbox is the JSON 4-tuple `[minLon, minLat, maxLon, maxLat]`.
  private static func areaCovering(bbox: String, in areas: [OfflineArea]) -> OfflineArea? {
    let parts = bbox.split(separator: ",").compactMap { Double($0.trimmingCharacters(in: .whitespaces)) }
    guard parts.count == 4 else { return nil }
    let centerLon = (parts[0] + parts[2]) / 2
    let centerLat = (parts[1] + parts[3]) / 2
    return areas.first { area in
      let box = area.manifest.bbox.map { $0.doubleValue }
      guard box.count == 4 else { return false }
      return centerLon >= box[0] && centerLon <= box[2]
        && centerLat >= box[1] && centerLat <= box[3]
    }
  }
}

// MARK: - Place enrichment

/// Optional Wikimedia enrichment for a selected place. Search results carry no
/// Wikidata seed, so this tries a title lookup for POI-ish results and stays
/// quiet on misses — the basic card never waits for it.
@MainActor
final class PlaceModel: ObservableObject {
  @Published private(set) var enriched: Place?

  private let core: IterCore
  private var task: Task<Void, Never>?

  init(core: IterCore) {
    self.core = core
  }

  private static let poiKeys: Set<String> = [
    "tourism", "historic", "amenity", "leisure", "man_made", "building",
  ]

  func load(_ place: SearchResult) {
    task?.cancel()
    enriched = nil
    guard let key = place.osmKey, Self.poiKeys.contains(key) else { return }
    task = Task { [weak self] in
      guard let self else { return }
      let lang = Locale.current.language.languageCode?.identifier ?? "en"
      let result = try? await self.core.places.enrichByTitle(title: place.name, lang: lang)
      if !Task.isCancelled { self.enriched = result }
    }
  }

  func imageURL(for place: Place) -> URL? {
    core.places.imageUrl(place: place, width: 640).flatMap(URL.init(string:))
  }

  func clear() {
    task?.cancel()
    enriched = nil
  }
}

// MARK: - Settings

@MainActor
final class SettingsModel: ObservableObject {
  private let core: IterCore

  /// ThemeMode name: SYSTEM | LIGHT | DARK.
  @Published var themeMode: String {
    didSet { core.setThemeMode(name: themeMode) }
  }
  /// MapMode name: STANDARD | TRANSIT.
  @Published var mapMode: String {
    didSet { core.setMapMode(name: mapMode) }
  }
  @Published private(set) var gatewayOrigin: String

  init(core: IterCore) {
    self.core = core
    themeMode = core.themeMode()
    mapMode = core.mapMode()
    gatewayOrigin = core.gatewayOrigin()
  }

  func applyGatewayOrigin(_ origin: String) {
    core.setGatewayOrigin(origin: origin)
    gatewayOrigin = core.gatewayOrigin()
  }

  var preferredColorScheme: ColorScheme? {
    switch themeMode {
    case "LIGHT": return .light
    case "DARK": return .dark
    default: return nil
    }
  }

  /// Resets the first-run flag so the intro can be shown again (onboarding is
  /// gated on `hasSeenOnboarding` in the shared settings).
  func replayOnboarding() {
    core.setOnboardingSeen(seen: false)
  }

  var hasSeenOnboarding: Bool {
    core.hasSeenOnboarding()
  }

  /// The app's marketing version from the bundle (CFBundleShortVersionString).
  var appVersion: String {
    Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
  }

  /// The style name for the current map mode + theme (wire StyleNames).
  func styleName(dark: Bool) -> String {
    if mapMode == "TRANSIT" {
      return dark ? "transit-dark" : "transit-light"
    } else {
      return dark ? "dark" : "light"
    }
  }

  /// Live style URL from the gateway.
  func styleUrl(dark: Bool) -> String {
    core.styleUrl(name: styleName(dark: dark))
  }
}

// MARK: - Kotlin interop conveniences

extension SearchResult: Identifiable {}

extension Station: Identifiable {}
