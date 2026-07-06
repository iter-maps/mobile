import SwiftUI

/// Home page: search pill + quick-action grid, sized for the peek detent.
struct HomePage: View {
  @EnvironmentObject private var app: AppModel

  var body: some View {
    VStack(spacing: 14) {
      searchPill
      HStack(spacing: 10) {
        QuickAction(icon: "arrow.triangle.turn.up.right.diamond.fill", label: Strings.homeQuickPlanning) {
          app.push(.planning)
        }
        QuickAction(icon: "tram.fill", label: Strings.homeQuickBoards) {
          app.push(.boards(initialQuery: nil))
        }
        QuickAction(icon: "arrow.down.circle.fill", label: Strings.homeQuickOffline) {
          app.push(.offline)
        }
        QuickAction(icon: "gearshape.fill", label: Strings.homeQuickSettings) {
          app.push(.settings)
        }
      }
      Spacer(minLength: 0)
    }
    .padding(.horizontal, 16)
    .padding(.top, 14)
    .toolbar(.hidden, for: .navigationBar)
  }

  private var searchPill: some View {
    Button {
      app.push(.search)
    } label: {
      HStack(spacing: 12) {
        Image(systemName: "magnifyingglass")
          .foregroundStyle(.secondary)
        Text(Strings.homeSearchHint)
          .foregroundStyle(.secondary)
        Spacer(minLength: 0)
      }
      .padding(.horizontal, 16)
      .frame(height: 52)
      .background(Color(.tertiarySystemFill), in: Capsule())
    }
    .buttonStyle(.plain)
  }
}

private struct QuickAction: View {
  let icon: String
  let label: String
  let action: () -> Void

  var body: some View {
    Button(action: action) {
      VStack(spacing: 6) {
        Image(systemName: icon)
          .font(.system(size: 20))
          .foregroundStyle(.tint)
        Text(label)
          .font(.caption.weight(.medium))
          .lineLimit(1)
          .foregroundStyle(.primary)
      }
      .frame(maxWidth: .infinity)
      .padding(.vertical, 12)
      .background(Color(.secondarySystemFill), in: RoundedRectangle(cornerRadius: 12))
    }
    .buttonStyle(.plain)
  }
}
