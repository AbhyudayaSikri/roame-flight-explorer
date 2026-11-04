import SwiftUI
@preconcurrency import Shared

@main
struct RoameStarterApp: App {
    @State private var model = ExplorerModel(
        store: IosExplorer.shared.createStore(baseUrl: AppConfig.baseURL, debug: AppConfig.debugOptions)
    )

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
                .task { await model.observe() }
        }
    }
}
