import Foundation
import Observation
@preconcurrency import Shared

/// Mirrors the shared store's state for SwiftUI, one property per field the views read. A property
/// is assigned only when its value changes, so a view re-renders only when a field it reads does:
/// a new message doesn't redraw the map, and typing a search doesn't redraw the Saved tab.
/// All logic stays in the store.
@MainActor
@Observable
final class ExplorerModel {
    let store: FlightExplorerStore

    private(set) var sync: any SyncState
    private(set) var isRefreshing: Bool
    private(set) var filters: FiltersUi
    private(set) var results: [Flight]
    private(set) var resultCount: Int32
    private(set) var totalCount: Int32
    private(set) var activeFilters: [ActiveFilter]
    private(set) var markers: [DestinationMarker]
    private(set) var focusedDestination: DestinationMarker?
    private(set) var focusedFlights: [Flight]
    private(set) var selected: Flight?
    private(set) var selectedSaveStatus: SaveStatus
    private(set) var saved: [SavedFlight]
    private(set) var savedIds: Set<String>
    private(set) var selectedSaved: SavedFlight?
    private(set) var message: UserMessage?
    private(set) var serverUrl: String
    private(set) var defaultServerUrl: String
    private(set) var isDefaultServerUrl: Bool

    init(store: FlightExplorerStore) {
        self.store = store
        let state = store.state.value
        sync = state.sync
        isRefreshing = state.isRefreshing
        filters = state.filters
        results = state.results
        resultCount = state.resultCount
        totalCount = state.totalCount
        activeFilters = state.activeFilters
        markers = state.markers
        focusedDestination = state.focusedDestination
        focusedFlights = state.focusedFlights
        selected = state.selected
        selectedSaveStatus = state.selectedSaveStatus
        saved = state.saved
        savedIds = state.savedIds
        selectedSaved = state.selectedSaved
        message = state.message
        serverUrl = state.serverUrl
        defaultServerUrl = state.defaultServerUrl
        isDefaultServerUrl = state.isDefaultServerUrl
    }

    /// Runs for the app's lifetime from a `.task`, which cancels it with the view.
    func observe() async {
        for await next in store.state {
            apply(next)
        }
    }

    /// Observation counts every assignment as a change, even of an equal value, so each field is
    /// compared first. Kotlin objects compare with their `equals`.
    private func apply(_ state: ExplorerState) {
        if !(sync as? NSObject == state.sync as? NSObject) { sync = state.sync }
        if isRefreshing != state.isRefreshing { isRefreshing = state.isRefreshing }
        if filters != state.filters { filters = state.filters }
        if results != state.results { results = state.results }
        if resultCount != state.resultCount { resultCount = state.resultCount }
        if totalCount != state.totalCount { totalCount = state.totalCount }
        if activeFilters != state.activeFilters { activeFilters = state.activeFilters }
        if markers != state.markers { markers = state.markers }
        if focusedDestination != state.focusedDestination { focusedDestination = state.focusedDestination }
        if focusedFlights != state.focusedFlights { focusedFlights = state.focusedFlights }
        if selected != state.selected { selected = state.selected }
        if selectedSaveStatus != state.selectedSaveStatus { selectedSaveStatus = state.selectedSaveStatus }
        if saved != state.saved { saved = state.saved }
        if savedIds != state.savedIds { savedIds = state.savedIds }
        if selectedSaved != state.selectedSaved { selectedSaved = state.selectedSaved }
        if message != state.message { message = state.message }
        if serverUrl != state.serverUrl { serverUrl = state.serverUrl }
        if defaultServerUrl != state.defaultServerUrl { defaultServerUrl = state.defaultServerUrl }
        if isDefaultServerUrl != state.isDefaultServerUrl { isDefaultServerUrl = state.isDefaultServerUrl }
    }
}
