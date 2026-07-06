import IterCore
import SwiftUI

/// Offline areas: download the current viewport as a bundle, list and delete
/// installed areas.
struct OfflinePage: View {
  /// Supplies the map viewport as the wire bbox string, nil before readiness.
  let currentBBox: () -> String?

  @EnvironmentObject private var offline: OfflineModel

  var body: some View {
    List {
      Section {
        downloadRow
      }
      Section {
        if offline.areas.isEmpty {
          Text(Strings.offlineEmpty)
            .foregroundStyle(.secondary)
        } else {
          ForEach(offline.areas, id: \.id) { area in
            AreaRow(area: area) {
              offline.delete(areaId: area.id)
            }
          }
        }
      }
    }
    .navigationTitle(Strings.offlineTitle)
    .navigationBarTitleDisplayMode(.inline)
    .onAppear { offline.refresh() }
  }

  @ViewBuilder
  private var downloadRow: some View {
    switch offline.download {
    case .downloading(let read, let total):
      VStack(alignment: .leading, spacing: 8) {
        if let total, total > 0 {
          ProgressView(value: Double(read), total: Double(total))
        } else {
          ProgressView()
        }
        Text(Strings.offlineDownloading(Formatters.bytes(read)))
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
    case .installing:
      VStack(alignment: .leading, spacing: 8) {
        ProgressView()
        Text(Strings.offlineInstalling)
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
    case .failed(let code):
      VStack(alignment: .leading, spacing: 8) {
        Text(failureMessage(code))
          .font(.subheadline)
          .foregroundStyle(.red)
        // Retry the download rather than merely clearing the error.
        downloadButton(Strings.actionRetry)
      }
    case .idle:
      downloadButton(Strings.offlineDownloadCurrent)
    }
  }

  private func downloadButton(_ title: String) -> some View {
    Button {
      if let bbox = currentBBox() {
        offline.downloadArea(bbox: bbox)
      }
    } label: {
      Label(title, systemImage: "arrow.down.circle.fill")
    }
  }

  private func failureMessage(_ code: String?) -> String {
    if OfflineModel.isTooLarge(code) { return Strings.offlineErrorTooLarge }
    if OfflineModel.isInvalidArea(code) { return Strings.offlineErrorInvalidArea }
    if OfflineModel.isBusy(code) { return Strings.offlineErrorBusy }
    return Strings.errorGeneric
  }
}

private struct AreaRow: View {
  let area: OfflineArea
  let onDelete: () -> Void

  var body: some View {
    HStack {
      VStack(alignment: .leading, spacing: 2) {
        Text(label)
        Text("\(Strings.offlineAreaZoom(Int(area.manifest.maxzoom)))  ·  \(String(area.manifest.createdAt.prefix(10)))")
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
      Spacer()
      Button(role: .destructive, action: onDelete) {
        Image(systemName: "trash")
      }
      .buttonStyle(.borderless)
      .accessibilityLabel(Strings.actionDelete)
    }
  }

  private var label: String {
    // Manifest bbox is the JSON 4-tuple [minLon, minLat, maxLon, maxLat].
    let bbox = area.manifest.bbox.map { $0.doubleValue }
    guard bbox.count == 4 else { return area.id }
    return String(format: "%.2f, %.2f → %.2f, %.2f", bbox[1], bbox[0], bbox[3], bbox[2])
  }
}
