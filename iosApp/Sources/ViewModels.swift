import Foundation
import IterCore
import SwiftUI

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
    path.append(route)
    detent = preferredDetent(for: route)
  }

  func pop() {
    if !path.isEmpty { path.removeLast() }
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
        if !Task.isCancelled { self.results = found }
      } catch {
        if !Task.isCancelled { self.results = [] }
      }
      if !Task.isCancelled { self.isSearching = false }
    }
  }

  func reset() {
    searchTask?.cancel()
    query = ""
    results = []
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
  case error
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
        if !Task.isCancelled { self.state = .error }
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
  case loaded([BoardEntry])
  case error
}

@MainActor
final class BoardsModel: ObservableObject {
  @Published var stationQuery = "" {
    didSet { scheduleStationSearch() }
  }
  @Published private(set) var stations: [Station] = []
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
      return
    }
    searchTask = Task { [weak self] in
      try? await Task.sleep(for: .milliseconds(300))
      guard let self, !Task.isCancelled else { return }
      do {
        let found = try await self.core.trains.searchStations(query: trimmed)
        if !Task.isCancelled { self.stations = found }
      } catch {
        if !Task.isCancelled { self.stations = [] }
      }
    }
  }

  private func restartPolling() {
    pollTask?.cancel()
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
          self.board = .loaded(entries)
        } catch {
          if Task.isCancelled { return }
          self.board = .error
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
    selectedStation = nil
    tab = .departures
    board = .idle
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

  /// Style name from the shared whitelist (wire StyleNames).
  func styleUrl(dark: Bool) -> String {
    let name: String
    if mapMode == "TRANSIT" {
      name = dark ? "transit-dark" : "transit-light"
    } else {
      name = dark ? "dark" : "light"
    }
    return core.styleUrl(name: name)
  }
}

// MARK: - Kotlin interop conveniences

extension SearchResult: Identifiable {}

extension Station: Identifiable {}
