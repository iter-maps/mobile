import SwiftUI

struct SettingsPage: View {
  @EnvironmentObject private var settings: SettingsModel
  @State private var originField = ""

  var body: some View {
    Form {
      Section(Strings.settingsTheme) {
        Picker(Strings.settingsTheme, selection: $settings.themeMode) {
          Text(Strings.settingsThemeSystem).tag("SYSTEM")
          Text(Strings.settingsThemeLight).tag("LIGHT")
          Text(Strings.settingsThemeDark).tag("DARK")
        }
        .pickerStyle(.segmented)
        .labelsHidden()
      }

      Section(Strings.settingsMapStyle) {
        Picker(Strings.settingsMapStyle, selection: $settings.mapMode) {
          Text(Strings.settingsMapStandard).tag("STANDARD")
          Text(Strings.settingsMapTransit).tag("TRANSIT")
        }
        .pickerStyle(.segmented)
        .labelsHidden()
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
        Text(Strings.settingsServer)
      } footer: {
        Text(Strings.settingsServerDesc)
      }
    }
    .navigationTitle(Strings.settingsTitle)
    .navigationBarTitleDisplayMode(.inline)
    .onAppear { originField = settings.gatewayOrigin }
  }

  private var cleanedOrigin: String {
    var cleaned = originField.trimmingCharacters(in: .whitespaces)
    while cleaned.hasSuffix("/") { cleaned.removeLast() }
    return cleaned
  }
}
