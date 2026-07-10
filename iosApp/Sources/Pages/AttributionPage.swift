import SwiftUI
import UIKit

/// About-the-map / attribution screen (item 4). The basemap is OpenStreetMap
/// data via OpenMapTiles (ODbL), so the full credits live here and the on-map
/// credit chip opens this page. Also carries the open-source licenses entry.
struct AttributionPage: View {
  var body: some View {
    Form {
      Section {
        VStack(alignment: .leading, spacing: 4) {
          Text(Strings.attributionOsm)
            .font(.body.weight(.medium))
          Text(Strings.attributionOsmDesc)
            .font(.footnote)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
        Link(Strings.attributionOpenOsm, destination: osmCopyrightURL)
      } header: {
        Text(Strings.attributionOsm)
      }

      Section {
        VStack(alignment: .leading, spacing: 4) {
          Text(Strings.attributionOpenMapTiles)
            .font(.body.weight(.medium))
          Text(Strings.attributionOpenMapTilesDesc)
            .font(.footnote)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
      } header: {
        Text(Strings.attributionOpenMapTiles)
      }

      Section {
        Text(Strings.attributionLicensesPlaceholder)
          .font(.footnote)
          .foregroundStyle(.secondary)
      } header: {
        Text(Strings.settingsLicenses)
      }
    }
    .navigationTitle(Strings.attributionTitle)
    .navigationBarTitleDisplayMode(.inline)
  }

  private var osmCopyrightURL: URL {
    URL(string: "https://www.openstreetmap.org/copyright")!
  }
}
