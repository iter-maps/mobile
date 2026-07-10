import SwiftUI
import UIKit

struct SettingsPage: View {
  @EnvironmentObject private var app: AppModel
  @EnvironmentObject private var settings: SettingsModel
  @EnvironmentObject private var location: LocationProvider
  @State private var originField = ""

  var body: some View {
    Form {
      // Appearance — kept FIRST so Theme needs no scroll.
      Section(Strings.settingsAppearance) {
        Picker(Strings.settingsTheme, selection: $settings.themeMode) {
          Text(Strings.settingsThemeSystem).tag("SYSTEM")
          Text(Strings.settingsThemeLight).tag("LIGHT")
          Text(Strings.settingsThemeDark).tag("DARK")
        }
        .pickerStyle(.segmented)
        .labelsHidden()
      }

      Section(Strings.settingsMapLocation) {
        Picker(Strings.settingsMapStyle, selection: $settings.mapMode) {
          Text(Strings.settingsMapStandard).tag("STANDARD")
          Text(Strings.settingsMapTransit).tag("TRANSIT")
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        Button {
          app.push(.offline)
        } label: {
          SettingsRow(title: Strings.settingsOffline, systemImage: "arrow.down.circle")
        }
        .buttonStyle(.plain)
      }

      Section(Strings.settingsAbout) {
        LabeledContent(Strings.settingsVersion, value: settings.appVersion)
        Button {
          app.push(.attribution)
        } label: {
          SettingsRow(title: Strings.settingsAttribution, systemImage: "map")
        }
        .buttonStyle(.plain)
        Button {
          app.push(.attribution)
        } label: {
          SettingsRow(title: Strings.settingsLicenses, systemImage: "doc.text")
        }
        .buttonStyle(.plain)
      }

      Section(Strings.settingsHelp) {
        Button {
          settings.replayOnboarding()
        } label: {
          SettingsRow(title: Strings.settingsReplayOnboarding, systemImage: "arrow.counterclockwise")
        }
        .buttonStyle(.plain)
        locationPermissionRow
      }

      Section {
        TextField(Strings.settingsServerOrigin, text: $originField)
          .keyboardType(.URL)
          .textInputAutocapitalization(.never)
          .autocorrectionDisabled()
        Button(Strings.settingsServerSave) {
          settings.applyGatewayOrigin(originField)
        }
        .disabled(cleanedOrigin == settings.gatewayOrigin)
      } header: {
        Text(Strings.settingsAdvanced)
      } footer: {
        Text(Strings.settingsServerDesc)
      }
    }
    .navigationTitle(Strings.settingsTitle)
    .navigationBarTitleDisplayMode(.inline)
    .onAppear { originField = settings.gatewayOrigin }
  }

  /// Passive "Granted" state when authorized; when denied, deep-links to the
  /// iOS Settings app so the user can recover from a denied prompt.
  @ViewBuilder
  private var locationPermissionRow: some View {
    if location.isAuthorized {
      HStack {
        Label(Strings.settingsLocationPermission, systemImage: "location.fill")
        Spacer()
        Text(Strings.settingsLocationGranted)
          .foregroundStyle(.secondary)
      }
    } else {
      Button {
        if let url = URL(string: UIApplication.openSettingsURLString) {
          UIApplication.shared.open(url)
        }
      } label: {
        HStack {
          Label(Strings.settingsLocationPermission, systemImage: "location.slash")
          Spacer()
          Text(Strings.settingsLocationDenied)
            .foregroundStyle(.secondary)
        }
      }
      .buttonStyle(.plain)
    }
  }

  private var cleanedOrigin: String {
    var cleaned = originField.trimmingCharacters(in: .whitespaces)
    while cleaned.hasSuffix("/") { cleaned.removeLast() }
    return cleaned
  }
}

/// A tappable settings row: leading icon + title, trailing chevron.
private struct SettingsRow: View {
  let title: String
  let systemImage: String

  var body: some View {
    HStack {
      Label(title, systemImage: systemImage)
        .foregroundStyle(.primary)
      Spacer()
      Image(systemName: "chevron.right")
        .font(.caption.weight(.semibold))
        .foregroundStyle(.tertiary)
    }
    .contentShape(Rectangle())
  }
}
