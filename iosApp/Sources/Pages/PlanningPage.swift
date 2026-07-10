import IterCore
import SwiftUI

/// Planning page: from/to card with swap, ranking profile chips, itinerary
/// results. Selecting an itinerary draws it on the map behind the sheet
/// (ADR 0008); tapping the selected one opens the leg detail.
struct PlanningPage: View {
  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var planning: PlanningModel
  @EnvironmentObject private var offline: OfflineModel

  var body: some View {
    VStack(spacing: 0) {
      endpointsCard
        .padding(.horizontal, 16)
        .padding(.top, 6)
      profileChips
        .padding(.vertical, 10)
      resultsList
    }
    .navigationTitle(Strings.planningTitle)
    .navigationBarTitleDisplayMode(.inline)
  }

  private var endpointsCard: some View {
    HStack(spacing: 4) {
      VStack(spacing: 0) {
        EndpointField(label: Strings.planningFrom, endpoint: planning.from) {
          app.push(.planningPicker(fromField: true))
        }
        Divider()
        EndpointField(label: Strings.planningTo, endpoint: planning.to) {
          app.push(.planningPicker(fromField: false))
        }
      }
      Button {
        planning.swap()
      } label: {
        Image(systemName: "arrow.up.arrow.down")
          .frame(width: 40, height: 40)
      }
      .accessibilityLabel(Strings.planningSwap)
    }
    .padding(.horizontal, 12)
    .background(Color(.secondarySystemFill), in: RoundedRectangle(cornerRadius: 16))
  }

  private var profileChips: some View {
    ScrollView(.horizontal, showsIndicators: false) {
      HStack(spacing: 8) {
        ForEach(PlanProfile.allCases, id: \.self) { profile in
          let selected = planning.profile == profile
          Button {
            planning.setProfile(profile)
          } label: {
            Text(profile.label)
              .font(.subheadline.weight(.medium))
              .padding(.horizontal, 14)
              .padding(.vertical, 7)
              .background(
                selected ? Color.accentColor.opacity(0.18) : Color(.secondarySystemFill),
                in: Capsule()
              )
              .foregroundStyle(selected ? Color.accentColor : Color.primary)
          }
          .buttonStyle(.plain)
        }
      }
      .padding(.horizontal, 16)
    }
  }

  @ViewBuilder
  private var resultsList: some View {
    switch planning.state {
    case .idle:
      Spacer(minLength: 0)
    case .loading:
      ProgressView()
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
      Spacer(minLength: 0)
    case .failure(let failure):
      StatusView(
        failure: failure,
        hasOfflineMaps: !offline.areas.isEmpty,
        onRetry: { planning.replan() }
      )
      .padding(16)
      Spacer(minLength: 0)
    case .results(let itineraries):
      if itineraries.isEmpty {
        Text(Strings.planningNoResults)
          .font(.subheadline)
          .foregroundStyle(.secondary)
          .padding(16)
        Spacer(minLength: 0)
      } else {
        ScrollView {
          LazyVStack(spacing: 10) {
            ForEach(Array(itineraries.enumerated()), id: \.offset) { _, itinerary in
              ItineraryCard(itinerary: itinerary, isSelected: planning.selected == itinerary) {
                if planning.selected == itinerary {
                  app.push(.planningDetail)
                } else {
                  planning.selected = itinerary
                }
              }
            }
          }
          .padding(.horizontal, 16)
          .padding(.vertical, 4)
        }
      }
    }
  }
}

private struct EndpointField: View {
  let label: String
  let endpoint: PlanEndpoint?
  let onTap: () -> Void

  var body: some View {
    Button(action: onTap) {
      HStack(spacing: 10) {
        Text(label)
          .font(.caption.weight(.medium))
          .foregroundStyle(.secondary)
          .frame(width: 36, alignment: .leading)
        if let endpoint {
          Text(endpoint.isUserLocation ? Strings.planningMyLocation : endpoint.name)
            .lineLimit(1)
            .foregroundStyle(.primary)
        } else {
          Text(Strings.planningChooseOnSearch)
            .lineLimit(1)
            .foregroundStyle(.secondary)
        }
        Spacer(minLength: 0)
      }
      .padding(.vertical, 13)
      .contentShape(Rectangle())
    }
    .buttonStyle(.plain)
  }
}

private struct ItineraryCard: View {
  let itinerary: Itinerary
  let isSelected: Bool
  let onTap: () -> Void

  var body: some View {
    Button(action: onTap) {
      VStack(alignment: .leading, spacing: 8) {
        HStack {
          Text("\(Formatters.clock(itinerary.startMs)) – \(Formatters.clock(itinerary.endMs))")
            .font(.headline)
            .monospacedDigit()
          Spacer()
          Text(Formatters.duration(itinerary.durationSeconds))
            .font(.headline.weight(.bold))
        }
        ItinerarySegments(itinerary: itinerary)
        HStack(spacing: 12) {
          Text(transfersLabel)
            .font(.footnote)
            .foregroundStyle(.secondary)
          Text(Strings.planningWalkDistance(Formatters.distance(itinerary.walkDistanceMeters)))
            .font(.footnote)
            .foregroundStyle(.secondary)
          reliabilityHint
        }
      }
      .padding(14)
      .background(
        isSelected ? Color.accentColor.opacity(0.15) : Color(.secondarySystemFill),
        in: RoundedRectangle(cornerRadius: 16)
      )
    }
    .buttonStyle(.plain)
  }

  private var transfersLabel: String {
    switch itinerary.numberOfTransfers {
    case 0: return Strings.planningTransfersNone
    case 1: return Strings.planningTransfersOne
    default: return Strings.planningTransfers(Int(itinerary.numberOfTransfers))
    }
  }

  @ViewBuilder
  private var reliabilityHint: some View {
    if let ranking = itinerary.ranking {
      if ranking.reliabilityScore >= 0.8 {
        Text(Strings.planningReliabilityHigh)
          .font(.footnote)
          .foregroundStyle(DelayColors.onTime)
      } else if ranking.reliabilityScore <= 0.35 {
        Text(Strings.planningReliabilityLow)
          .font(.footnote)
          .foregroundStyle(DelayColors.severe)
      }
    }
  }
}

/// Proportional segment strip: each leg gets width by duration (with a small
/// floor so short walks stay visible), walk legs as translucent gray, transit
/// legs in their line color with the badge inside when it fits.
struct ItinerarySegments: View {
  let itinerary: Itinerary

  var body: some View {
    let legs = itinerary.legs
    let total = max(legs.reduce(Int64(0)) { $0 + $1.durationSeconds }, 1)
    let rawWeights = legs.map { max(Double($0.durationSeconds) / Double(total), 0.08) }
    let weightSum = rawWeights.reduce(0, +)

    GeometryReader { geo in
      let spacing: CGFloat = 3
      let available = geo.size.width - spacing * CGFloat(max(legs.count - 1, 0))
      HStack(spacing: spacing) {
        ForEach(Array(legs.enumerated()), id: \.offset) { index, leg in
          let weight = rawWeights[index] / weightSum
          segment(leg, weight: weight)
            .frame(width: max(available * CGFloat(weight), 0))
        }
      }
    }
    .frame(height: 26)
  }

  @ViewBuilder
  private func segment(_ leg: Leg, weight: Double) -> some View {
    let color = lineUIColor(routeColor: leg.routeColor, mode: leg.mode)
    ZStack {
      RoundedRectangle(cornerRadius: 6)
        .fill(leg.isTransit ? Color(uiColor: color) : Color(uiColor: LineColors.walk).opacity(0.25))
      if leg.isTransit && weight > 0.14 {
        Text(leg.routeShortName ?? "")
          .font(.caption2.weight(.bold))
          .foregroundStyle(onLineColor(color))
          .lineLimit(1)
          .padding(.horizontal, 2)
      } else if !leg.isTransit {
        Image(systemName: "figure.walk")
          .font(.system(size: 11))
          .foregroundStyle(.secondary)
      }
    }
  }
}
