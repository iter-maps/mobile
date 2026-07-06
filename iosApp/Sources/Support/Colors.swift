import SwiftUI
import UIKit
import IterCore

extension UIColor {
  convenience init(rgb: UInt32) {
    self.init(
      red: CGFloat((rgb >> 16) & 0xFF) / 255,
      green: CGFloat((rgb >> 8) & 0xFF) / 255,
      blue: CGFloat(rgb & 0xFF) / 255,
      alpha: 1
    )
  }

  /// Parses a GTFS hex color (`RRGGBB`, with or without `#`); nil when invalid.
  convenience init?(gtfsHex: String?) {
    guard let hex = gtfsHex?.trimmingCharacters(in: .whitespaces) else { return nil }
    let cleaned = hex.hasPrefix("#") ? String(hex.dropFirst()) : hex
    guard cleaned.count == 6, let value = UInt32(cleaned, radix: 16) else { return nil }
    self.init(rgb: value)
  }

  var isLightColor: Bool {
    var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
    getRed(&red, green: &green, blue: &blue, alpha: &alpha)
    return (0.299 * red + 0.587 * green + 0.114 * blue) > 0.5
  }
}

extension Color {
  init(rgb: UInt32) {
    self.init(uiColor: UIColor(rgb: rgb))
  }
}

/// Transit identity colors — semantic, palette-independent (ADR 0009,
/// docs/design/tokens.md). A GTFS `route_color` always wins; these are
/// category fallbacks matching the Android LineColors.
enum LineColors {
  static let metro = UIColor(rgb: 0x0570B5)
  static let tram = UIColor(rgb: 0x7A9E4E)
  static let bus = UIColor(rgb: 0x3E7CB1)
  static let rail = UIColor(rgb: 0x7B4EA3)
  static let walk = UIColor(rgb: 0x5F6368)

  static func forMode(_ mode: LegMode) -> UIColor {
    if mode == .subway { return metro }
    if mode == .tram { return tram }
    if mode == .bus { return bus }
    if mode == .rail || mode == .ferry || mode == .funicular || mode == .gondola { return rail }
    return walk
  }
}

/// Delay coloring for boards and legs (docs/design/tokens.md).
enum DelayColors {
  static let onTime = Color(rgb: 0x2E9E63)
  static let minor = Color(rgb: 0xC98A2B)
  static let severe = Color(rgb: 0xC94242)
  static let early = Color(rgb: 0x3E7CB1)

  static func forMinutes(_ delayMinutes: Int32) -> Color {
    switch delayMinutes {
    case ..<0: return early
    case ...2: return onTime
    case ...10: return minor
    default: return severe
    }
  }
}

/// Resolves a leg's line color: GTFS color first, category fallback otherwise.
func lineUIColor(routeColor: String?, mode: LegMode) -> UIColor {
  UIColor(gtfsHex: routeColor) ?? LineColors.forMode(mode)
}

func lineColor(routeColor: String?, mode: LegMode) -> Color {
  Color(uiColor: lineUIColor(routeColor: routeColor, mode: mode))
}

/// Readable text color on a line badge, by luminance — not by theme.
func onLineColor(_ background: UIColor) -> Color {
  background.isLightColor ? Color(rgb: 0x1B1D29) : .white
}
