import IterCore
import SwiftUI

/// Place card: name, address, primary Directions action; train stations
/// offer the live board.
struct PlaceDetailPage: View {
  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var planning: PlanningModel
  @EnvironmentObject private var place: PlaceModel

  var body: some View {
    if let selected = app.selectedPlace {
      content(selected)
        .navigationTitle(selected.name)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { place.load(selected) }
        .onDisappear { place.clear() }
    } else {
      Color.clear
    }
  }

  private func content(_ selected: SearchResult) -> some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 12) {
        if let detail = selected.detail {
          Text(detail)
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }

        Button {
          planning.directionsTo(name: selected.name, lat: selected.point.lat, lon: selected.point.lon)
          app.push(.planning)
        } label: {
          Label(Strings.placeDirections, systemImage: "arrow.triangle.turn.up.right.diamond.fill")
            .frame(maxWidth: .infinity)
            .frame(height: 40)
        }
        .buttonStyle(.borderedProminent)

        if selected.isTrainStation {
          Button {
            app.push(.boards(initialQuery: selected.name))
          } label: {
            Label(Strings.placeTrainBoard, systemImage: "tram.fill")
              .frame(maxWidth: .infinity)
              .frame(height: 32)
          }
          .buttonStyle(.bordered)
        }

        if let info = place.enriched {
          enrichment(info)
        }
      }
      .padding(.horizontal, 20)
      .padding(.top, 8)
    }
  }

  @ViewBuilder
  private func enrichment(_ info: Place) -> some View {
    if let url = place.imageURL(for: info) {
      // Attribution shows only once the image has actually loaded.
      AsyncImage(url: url) { phase in
        switch phase {
        case .success(let image):
          VStack(alignment: .leading, spacing: 4) {
            image.resizable().scaledToFill()
              .frame(maxWidth: .infinity)
              .frame(height: 180)
              .clipped()
              .clipShape(RoundedRectangle(cornerRadius: 12))
            if let attribution = info.image?.attribution {
              Text(Strings.placePhotoAttribution(attribution))
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
          }
        default:
          EmptyView()
        }
      }
    }
    if let summary = info.summary {
      Text(summary)
        .font(.subheadline)
        .padding(.top, 4)
    }
    if let facets = info.facets {
      VStack(alignment: .leading, spacing: 8) {
        facetRow("globe", facets.website)
        facetRow("phone.fill", facets.phone)
        facetRow("clock.fill", facets.openingHours)
        facetRow("figure.roll", facets.wheelchair)
      }
    }
  }

  @ViewBuilder
  private func facetRow(_ symbol: String, _ value: String?) -> some View {
    if let value {
      Label(value, systemImage: symbol)
        .font(.subheadline)
        .labelStyle(.titleAndIcon)
    }
  }
}
