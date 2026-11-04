import Observation
@preconcurrency import Shared

/// Mirrors the shared store's state for SwiftUI. All logic stays in the store.
@MainActor
@Observable
final class ExplorerModel {
    let store: FlightExplorerStore
    private(set) var state: ExplorerState

    init(store: FlightExplorerStore) {
        self.store = store
        self.state = store.state.value
    }

    /// Runs for the app's lifetime from a `.task`, which cancels it with the view.
    func observe() async {
        for await next in store.state {
            state = next
        }
    }
}
