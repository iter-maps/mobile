import IterCore
import SwiftUI

/// Leg-by-leg detail of the selected itinerary: colored rail timeline, times,
/// line badges, stop counts and the historical-delay hint.
struct PlanningDetailPage: View {
  @EnvironmentObject private var planning: PlanningModel

  var body: some View {
    ScrollView {
      if let itinerary = planning.selected {
        VStack(alignment: .leading, spacing: 0) {
          HStack {
            Text("\(Formatters.clock(itinerary.startMs)) – \(Formatters.clock(itinerary.endMs))")
              .font(.title3.weight(.semibold))
              .monospacedDigit()
            Spacer()
            Text(Formatters.duration(itinerary.durationSeconds))
              .font(.title3.weight(.bold))
              .foregroundStyle(.tint)
          }
          .padding(.bottom, 16)
          LegTimeline(itinerary: itinerary)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
      }
    }
    .navigationTitle(Strings.planningDetailTitle)
    .navigationBarTitleDisplayMode(.inline)
  }
}

/// Vertical leg timeline: colored rail (translucent for walks), times,
/// badges, headsigns and per-leg hints, ending with the destination row.
struct LegTimeline: View {
  let itinerary: Itinerary

  var body: some View {
    let legs = itinerary.legs
    VStack(alignment: .leading, spacing: 0) {
      ForEach(Array(legs.enumerated()), id: \.offset) { _, leg in
        LegRow(leg: leg)
      }
      if let last = legs.last {
        destinationRow(last)
      }
    }
  }

  private func destinationRow(_ leg: Leg) -> some View {
    HStack(alignment: .center, spacing: 12) {
      Text(Formatters.clock(leg.endMs))
        .font(.footnote.weight(.semibold))
        .monospacedDigit()
        .frame(width: 48, alignment: .trailing)
      Circle()
        .fill(railColor(leg))
        .frame(width: 10, height: 10)
      Text(leg.to.name)
        .font(.body.weight(.medium))
        .lineLimit(1)
    }
  }
}

private func railColor(_ leg: Leg) -> Color {
  leg.isTransit
    ? lineColor(routeColor: leg.routeColor, mode: leg.mode)
    : Color(uiColor: LineColors.walk)
}

private struct LegRow: View {
  let leg: Leg

  var body: some View {
    HStack(alignment: .top, spacing: 12) {
      Text(Formatters.clock(leg.startMs))
        .font(.footnote.weight(.semibold))
        .monospacedDigit()
        .frame(width: 48, alignment: .trailing)
        .padding(.top, 1)
      legBody
        .padding(.leading, 22)
        .padding(.bottom, 18)
        .background(alignment: .topLeading) { rail }
    }
  }

  /// The rail is a background of the leg body so it stretches to its height.
  private var rail: some View {
    let color = railColor(leg)
    return VStack(spacing: 0) {
      Circle()
        .fill(color)
        .frame(width: 10, height: 10)
      Rectangle()
        .fill(leg.isTransit ? color : color.opacity(0.35))
        .frame(width: 4)
        .frame(maxHeight: .infinity)
    }
    .frame(width: 10)
  }

  private var legBody: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(leg.from.name)
        .font(.body.weight(.medium))
        .lineLimit(1)
      HStack(spacing: 8) {
        if leg.isTransit {
          LineBadge(leg: leg)
          if let headsign = leg.headsign {
            Text("» \(headsign)")
              .font(.footnote)
              .foregroundStyle(.secondary)
              .lineLimit(1)
          }
        } else {
          Image(systemName: "figure.walk")
            .font(.system(size: 14))
            .foregroundStyle(.secondary)
          Text(Formatters.distance(leg.distanceMeters))
            .font(.footnote)
            .foregroundStyle(.secondary)
        }
        Text(Formatters.duration(leg.durationSeconds))
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
      if leg.isTransit && !leg.intermediateStops.isEmpty {
        Text(Strings.planningStopsCount(leg.intermediateStops.count + 1))
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
      if let delay = leg.predictedDelay, delay.p85Seconds >= 60 {
        Text(Strings.planningUsuallyDelayed(Formatters.delay(delay.p85Seconds)))
          .font(.footnote)
          .foregroundStyle(DelayColors.minor)
      }
    }
  }
}

/// Line identity badge: GTFS route color first, category fallback otherwise;
/// text color by luminance, never by theme (ADR 0009).
struct LineBadge: View {
  let leg: Leg

  var body: some View {
    let background = lineUIColor(routeColor: leg.routeColor, mode: leg.mode)
    Text(leg.routeShortName ?? leg.routeLongName ?? "•")
      .font(.caption.weight(.bold))
      .lineLimit(1)
      .padding(.horizontal, 8)
      .padding(.vertical, 3)
      .background(Color(uiColor: background), in: RoundedRectangle(cornerRadius: 6))
      .foregroundStyle(onLineColor(background))
  }
}
