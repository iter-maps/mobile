import IterCore
import SwiftUI

/// Live departures/arrivals: station autocomplete first, then a segmented
/// board polled at the contract cadence while visible.
struct BoardsPage: View {
  let initialQuery: String?

  @EnvironmentObject private var boards: BoardsModel

  var body: some View {
    Group {
      if boards.selectedStation != nil {
        boardContent
      } else {
        stationSearch
      }
    }
    .navigationTitle(boards.selectedStation?.name ?? Strings.trainsTitle)
    .navigationBarTitleDisplayMode(.inline)
    .toolbar {
      if boards.selectedStation != nil {
        ToolbarItem(placement: .topBarTrailing) {
          Button {
            boards.selectedStation = nil
          } label: {
            Image(systemName: "magnifyingglass")
          }
          .accessibilityLabel(Strings.trainsChangeStation)
        }
      }
    }
    .onAppear {
      if let initialQuery, boards.selectedStation == nil, boards.stationQuery.isEmpty {
        boards.stationQuery = initialQuery
      }
    }
    .onDisappear { boards.reset() }
  }

  private var stationSearch: some View {
    VStack(spacing: 0) {
      TextField(Strings.trainsSearchHint, text: $boards.stationQuery)
        .textFieldStyle(.plain)
        .autocorrectionDisabled()
        .padding(.horizontal, 16)
        .frame(height: 44)
        .background(Color(.tertiarySystemFill), in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 16)
        .padding(.top, 6)
      if let failure = boards.stationsFailure, boards.stations.isEmpty {
        // A network error must never look like an empty station list.
        StatusView(failure: failure) { boards.retryStationSearch() }
        Spacer(minLength: 0)
      } else {
        List(boards.stations) { station in
          Button {
            boards.selectedStation = station
          } label: {
            Text(station.name)
          }
          .buttonStyle(.plain)
        }
        .listStyle(.plain)
      }
    }
  }

  @ViewBuilder
  private var boardContent: some View {
    VStack(spacing: 0) {
      Picker("", selection: $boards.tab) {
        Text(Strings.trainsDepartures).tag(BoardTab.departures)
        Text(Strings.trainsArrivals).tag(BoardTab.arrivals)
      }
      .pickerStyle(.segmented)
      .padding(.horizontal, 16)
      .padding(.vertical, 6)

      switch boards.board {
      case .idle:
        Spacer(minLength: 0)
      case .loading:
        ProgressView()
          .frame(maxWidth: .infinity)
          .padding(.vertical, 32)
        Spacer(minLength: 0)
      case .failure(let failure):
        StatusView(failure: failure) { boards.retryBoard() }
        Spacer(minLength: 0)
      case .loaded(let entries, let stale):
        if entries.isEmpty {
          Text(Strings.trainsEmpty)
            .foregroundStyle(.secondary)
            .padding(16)
          Spacer(minLength: 0)
        } else {
          if stale {
            // Keep the last good board visible, flagged as not live.
            Label(Strings.trainsStale, systemImage: "wifi.slash")
              .font(.caption)
              .foregroundStyle(.secondary)
              .frame(maxWidth: .infinity, alignment: .leading)
              .padding(.horizontal, 16)
              .padding(.bottom, 2)
          }
          List {
            ForEach(Array(entries.enumerated()), id: \.offset) { _, entry in
              BoardRow(entry: entry, showOrigin: boards.tab == .arrivals)
            }
          }
          .listStyle(.plain)
        }
      }
    }
  }
}

private struct BoardRow: View {
  let entry: BoardEntry
  let showOrigin: Bool

  var body: some View {
    HStack(spacing: 8) {
      Text(entry.scheduledTime)
        .font(.headline)
        .monospacedDigit()
        .frame(width: 56, alignment: .leading)
      VStack(alignment: .leading, spacing: 2) {
        Text(showOrigin ? (entry.origin ?? "—") : entry.destination)
          .lineLimit(1)
        HStack(spacing: 0) {
          Text(entry.trainNumber)
            .font(.footnote)
            .foregroundStyle(.secondary)
          if let platform = entry.platform {
            Text("  ·  \(Strings.trainsPlatform(platform))")
              .font(.footnote)
              .foregroundStyle(.secondary)
          }
        }
      }
      Spacer(minLength: 0)
      delayLabel
    }
  }

  @ViewBuilder
  private var delayLabel: some View {
    let minutes = entry.delayMinutes
    if minutes > 0 {
      Text("+\(minutes) min")
        .font(.subheadline.weight(.semibold))
        .foregroundStyle(DelayColors.forMinutes(minutes))
    } else if minutes < 0 {
      Text("−\(-minutes) min")
        .font(.subheadline.weight(.semibold))
        .foregroundStyle(DelayColors.early)
    } else {
      Text(Strings.trainsOnTime)
        .font(.subheadline.weight(.semibold))
        .foregroundStyle(DelayColors.onTime)
    }
  }
}
