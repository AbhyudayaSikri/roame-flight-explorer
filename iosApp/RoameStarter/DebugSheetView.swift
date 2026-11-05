import SwiftUI
@preconcurrency import Shared

/// Debug builds: point the app at another flights server, such as a Mac on the same Wi-Fi.
struct DebugSheetView: View {
    let model: ExplorerModel
    @Environment(\.dismiss) private var dismiss
    @State private var text = ""

    var body: some View {
        let problem = ServerUrlKt.serverUrlProblem(text: text)
        NavigationStack {
            Form {
                Section {
                    TextField("http://192.168.1.20:8080", text: $text)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .accessibilityIdentifier(TestIds.shared.debugServerUrl)
                    if let problem {
                        Text(problem)
                            .font(.footnote)
                            .foregroundStyle(.red)
                            .accessibilityIdentifier(TestIds.shared.debugServerUrlProblem)
                    }
                    Button("Save and refresh") {
                        model.store.setServerUrl(text: text)
                        dismiss()
                    }
                    .disabled(problem != nil)
                    .accessibilityIdentifier(TestIds.shared.debugServerUrlSave)
                    Button("Use the default (\(model.defaultServerUrl))") {
                        model.store.useDefaultServerUrl()
                        dismiss()
                    }
                    .disabled(model.isDefaultServerUrl)
                    .accessibilityIdentifier(TestIds.shared.debugServerUrlDefault)
                } header: {
                    Text("Server URL")
                } footer: {
                    Text("Now using \(model.serverUrl). On a phone, use the Mac's address on the same network, such as http://192.168.1.20:8080 or http://your-mac.local:8080. It's kept until you change it or reset the app's data.")
                }
            }
            .navigationTitle("Debug")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .accessibilityIdentifier(TestIds.shared.debugDone)
                }
            }
            .onAppear { text = model.serverUrl }
        }
    }
}
