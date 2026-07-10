import IterCore
import SwiftUI

/// Swift twin of the shared `AppFailure` taxonomy (ADR 0015). Repositories
/// still throw `IterApiException` / `IterTransportException`; the shell catches
/// and classifies once, so every surface renders the same category the same
/// way and "no connection" (offline zones may still work) is never confused
/// with "server unreachable".
enum AppFailure: Equatable {
  case noConnection
  case serverUnreachable(code: String?)
  case timeout
  case rateLimited
  case notFound
  case badRequest(code: String?)
  case server(code: String?)
  case offlineBundle(code: String?)
  case unknown

  /// Offline-surface codes that mean the bundle request itself was the problem.
  private static let offlineBundleCodes: Set<String> = [
    "AREA_TOO_LARGE", "PAYLOAD_TOO_LARGE", "STORE_FULL", "EXTRACT_FAILED",
    "BBOX_REQUIRED", "BBOX_INVALID", "BBOX_OUT_OF_RANGE", "BBOX_DEGENERATE",
    "ZOOM_INVALID",
  ]

  /// Codes that mean the gateway reached a broken/absent upstream.
  private static let upstreamCodes: Set<String> = [
    "UPSTREAM_UNAVAILABLE", "UPSTREAM_ERROR", "INTERNAL",
  ]

  /// Maps a caught error to its `AppFailure`. Mirrors the shared
  /// `Throwable.toAppFailure` / `classifyApi` exactly. A Kotlin exception
  /// crossing the `@Throws` boundary arrives as an `NSError` carrying the
  /// original throwable under the "KotlinException" userInfo key.
  static func from(_ error: Error, isOnline: Bool) -> AppFailure {
    let kotlinException = (error as NSError).userInfo["KotlinException"]

    if let transport = kotlinException as? IterTransportException {
      switch transport.kind {
      case .timeout:
        return .timeout
      case .unreachable:
        return isOnline ? .serverUnreachable(code: nil) : .noConnection
      default:
        return isOnline ? .serverUnreachable(code: nil) : .noConnection
      }
    }

    if let api = kotlinException as? IterApiException {
      return classifyApi(status: Int(api.status), code: api.code)
    }

    return .unknown
  }

  private static func classifyApi(status: Int, code: String?) -> AppFailure {
    if let code, offlineBundleCodes.contains(code) {
      return .offlineBundle(code: code)
    }
    if status == 429 || code == "BUSY" {
      return .rateLimited
    }
    if status == 404 {
      return .notFound
    }
    if (500...599).contains(status) || (code.map { upstreamCodes.contains($0) } ?? false) {
      return .server(code: code)
    }
    if code == "TIMEOUT" {
      return .timeout
    }
    // GraphQL body errors are synthesized as IterApiException(200, code, …).
    if status == 200, code != nil {
      return .server(code: code)
    }
    if (400...499).contains(status) {
      return .badRequest(code: code)
    }
    return .unknown
  }
}

/// The reusable "fancy" error surface, fed by an `AppFailure`. Renders as a
/// system `ContentUnavailableView` (icon + title + message + optional Retry).
/// `hasOfflineMaps` tailors the no-connection copy; `onRetry` renders a Retry
/// button when the failure is recoverable.
struct StatusView: View {
  let failure: AppFailure
  var hasOfflineMaps = false
  var onRetry: (() -> Void)?

  private var symbol: String {
    switch failure {
    case .noConnection: return "wifi.slash"
    case .serverUnreachable, .server: return "exclamationmark.icloud"
    case .timeout: return "clock.arrow.circlepath"
    case .rateLimited: return "hourglass"
    case .notFound: return "magnifyingglass"
    case .offlineBundle: return "arrow.down.circle.badge.xmark"
    case .badRequest, .unknown: return "exclamationmark.triangle"
    }
  }

  private var title: String {
    switch failure {
    case .noConnection: return Strings.errorOfflineTitle
    case .serverUnreachable, .server: return Strings.errorUnreachableTitle
    case .timeout: return Strings.errorTimeoutTitle
    case .rateLimited: return Strings.errorBusyTitle
    case .notFound: return Strings.errorNotFoundTitle
    case .offlineBundle(let code): return offlineBundleTitle(code)
    case .badRequest, .unknown: return Strings.errorGenericTitle
    }
  }

  private var message: String {
    switch failure {
    case .noConnection:
      return hasOfflineMaps ? Strings.errorOfflineBodyWithMaps : Strings.errorOfflineBody
    case .serverUnreachable, .server: return Strings.errorUnreachableBody
    case .timeout: return Strings.errorTimeoutBody
    case .rateLimited: return Strings.errorBusyBody
    case .notFound: return Strings.errorNotFoundBody
    case .offlineBundle(let code): return offlineBundleMessage(code)
    case .badRequest, .unknown: return Strings.errorGenericBody
    }
  }

  /// A retryable failure gets a Retry button; a bad request / not-found does not.
  private var showsRetry: Bool {
    guard onRetry != nil else { return false }
    switch failure {
    case .badRequest, .notFound: return false
    default: return true
    }
  }

  private func offlineBundleTitle(_ code: String?) -> String {
    switch code {
    case "AREA_TOO_LARGE", "PAYLOAD_TOO_LARGE": return Strings.offlineErrorTooLarge
    case "STORE_FULL": return Strings.errorGenericTitle
    default: return Strings.offlineErrorInvalidArea
    }
  }

  private func offlineBundleMessage(_ code: String?) -> String {
    switch code {
    case "AREA_TOO_LARGE", "PAYLOAD_TOO_LARGE": return Strings.offlineErrorTooLarge
    case "STORE_FULL": return Strings.errorGenericBody
    default: return Strings.offlineErrorInvalidArea
    }
  }

  var body: some View {
    // Deployment target is iOS 17 (project.yml), so ContentUnavailableView is
    // always available — the app's "fancy", system-native empty/error state.
    ContentUnavailableView {
      Label(title, systemImage: symbol)
    } description: {
      Text(message)
    } actions: {
      if showsRetry, let onRetry {
        Button(Strings.actionRetry, action: onRetry)
          .buttonStyle(.borderedProminent)
      }
    }
  }
}
