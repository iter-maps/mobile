import IterCore
import SwiftUI

/// Full search page: searchable results list. Also reused by the planning
/// endpoint picker, which pops back with the chosen endpoint instead of
/// opening the place detail.
struct SearchPage: View {
  enum Mode: Hashable {
    case explore
    case picker(fromField: Bool)
  }

  let mode: Mode

  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var search: SearchModel
  @EnvironmentObject private var planning: PlanningModel
  @EnvironmentObject private var location: LocationProvider
  @EnvironmentObject private var offline: OfflineModel
  @State private var searchPresented = true

  var body: some View {
    List {
      if case .picker(let fromField) = mode, location.lastLocation != nil {
        Button {
          planning.useMyLocation(fromField: fromField)
          app.pop()
        } label: {
          Label(Strings.planningMyLocation, systemImage: "location.fill")
        }
      }
      ForEach(search.results) { result in
        Button {
          pick(result)
        } label: {
          SearchResultRow(result: result)
        }
        .buttonStyle(.plain)
      }
      if let failure = search.failure, search.results.isEmpty, !search.isSearching {
        // A network error must never look like "Nothing found".
        StatusView(
          failure: failure,
          hasOfflineMaps: !offline.areas.isEmpty,
          onRetry: { search.retry() }
        )
        .frame(maxWidth: .infinity)
        .listRowSeparator(.hidden)
      } else if search.results.isEmpty && search.query.count >= 2 && !search.isSearching {
        Text(Strings.searchNoResults)
          .foregroundStyle(.secondary)
      }
    }
    .listStyle(.plain)
    .searchable(
      text: $search.query,
      isPresented: $searchPresented,
      placement: .navigationBarDrawer(displayMode: .always),
      prompt: Text(Strings.searchPlaceholder)
    )
    .navigationBarTitleDisplayMode(.inline)
    .onAppear { offline.refresh() }
    .onDisappear { search.reset() }
  }

  private func pick(_ result: SearchResult) {
    switch mode {
    case .explore:
      app.selectedPlace = result
      app.push(.placeDetail)
    case .picker(let fromField):
      planning.setEndpoint(
        fromField: fromField,
        name: result.name,
        lat: result.point.lat,
        lon: result.point.lon
      )
      app.pop()
    }
  }
}

private struct SearchResultRow: View {
  let result: SearchResult

  var body: some View {
    HStack(spacing: 14) {
      Image(systemName: result.isTrainStation ? "tram.fill" : "mappin.circle.fill")
        .font(.system(size: 18))
        .foregroundStyle(.secondary)
        .frame(width: 40, height: 40)
        .background(Color(.secondarySystemFill), in: Circle())
      VStack(alignment: .leading, spacing: 2) {
        Text(result.name)
          .lineLimit(1)
        if let detail = result.detail {
          Text(detail)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .lineLimit(1)
        }
      }
    }
  }
}
