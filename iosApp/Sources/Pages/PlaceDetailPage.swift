import IterCore
import SwiftUI

/// Place card: name, address, primary Directions action; train stations
/// offer the live board.
struct PlaceDetailPage: View {
  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var planning: PlanningModel

  var body: some View {
    if let place = app.selectedPlace {
      content(place)
    } else {
      Color.clear
    }
  }

  private func content(_ place: SearchResult) -> some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 12) {
        if let detail = place.detail {
          Text(detail)
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }

        Button {
          planning.directionsTo(name: place.name, lat: place.point.lat, lon: place.point.lon)
          app.push(.planning)
        } label: {
          Label(Strings.placeDirections, systemImage: "arrow.triangle.turn.up.right.diamond.fill")
            .frame(maxWidth: .infinity)
            .frame(height: 40)
        }
        .buttonStyle(.borderedProminent)

        if place.isTrainStation {
          Button {
            app.push(.boards(initialQuery: place.name))
          } label: {
            Label(Strings.placeTrainBoard, systemImage: "tram.fill")
              .frame(maxWidth: .infinity)
              .frame(height: 32)
          }
          .buttonStyle(.bordered)
        }
      }
      .padding(.horizontal, 20)
      .padding(.top, 8)
    }
    .navigationTitle(place.name)
    .navigationBarTitleDisplayMode(.inline)
  }
}
