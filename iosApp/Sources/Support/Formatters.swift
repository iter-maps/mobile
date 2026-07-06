import Foundation

enum Formatters {
  private static let clockFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "HH:mm"
    return formatter
  }()

  /// `14:05` from epoch milliseconds (matching the Android 24h clock).
  static func clock(_ epochMs: Int64) -> String {
    clockFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000))
  }

  /// `7 min` / `1 h 12 min`.
  static func duration(_ seconds: Int64) -> String {
    let minutes = Int(seconds / 60)
    if minutes < 60 { return "\(minutes) min" }
    return "\(minutes / 60) h \(minutes % 60) min"
  }

  /// `350 m` / `2.4 km` style distance.
  static func distance(_ meters: Double) -> String {
    if meters < 950 {
      return "\(Int((meters / 10).rounded()) * 10) m"
    }
    return String(format: "%.1f km", locale: .current, meters / 1000)
  }

  /// Seconds → compact `3 min` / `45 s` delay label.
  static func delay(_ seconds: Double) -> String {
    let rounded = Int(seconds.rounded())
    if rounded >= 90 { return "\(Int((Double(rounded) / 60).rounded())) min" }
    return "\(rounded) s"
  }

  /// `312 KB` / `4.2 MB` download progress.
  static func bytes(_ bytes: Int64) -> String {
    if bytes < 1024 * 1024 { return "\(bytes / 1024) KB" }
    return String(format: "%.1f MB", locale: .current, Double(bytes) / (1024 * 1024))
  }
}
