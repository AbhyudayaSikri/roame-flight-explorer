import SwiftUI
@preconcurrency import Shared

private enum ViewMode: Hashable { case list, map }

struct ExploreView: View {
    let model: ExplorerModel
    // The fields own their text so typing never waits on the store; Reset clears both.
    @State private var origin = ""
    @State private var destination = ""
    @State private var mode: ViewMode = .list
    @State private var showFilters = false

    private var state: ExplorerState { model.state }
    private var store: FlightExplorerStore { model.store }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    SearchField(title: "From", text: $origin, identifier: TestIds.shared.searchOrigin) {
                        store.setOriginText(text: $0)
                    }
                    SearchField(title: "To", text: $destination, identifier: TestIds.shared.searchDestination) {
                        store.setDestinationText(text: $0)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                summaryRow
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)

                Picker("View", selection: $mode) {
                    Text("List").tag(ViewMode.list)
                    Text("Map").tag(ViewMode.map)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
                .accessibilityIdentifier("view.mode")

                if state.isRefreshing && state.totalCount > 0 {
                    ProgressView().progressViewStyle(.linear)
                }
                if case .stale(let stale) = onEnum(of: state.sync) {
                    StaleBanner(stale: stale)
                }

                ZStack(alignment: .bottom) {
                    content
                    if let flight = state.selected {
                        FlightCardView(
                            flight: flight,
                            status: state.selectedSaveStatus,
                            onSave: { store.save(flightId: flight.id) },
                            onRemove: { store.remove(flightId: flight.id) },
                            onClose: { store.select(flightId: nil) }
                        )
                    }
                }
                .frame(maxHeight: .infinity)
                .sheet(isPresented: destinationSheetShown) {
                    DestinationSheetView(model: model)
                }
            }
            .navigationTitle("Explore flights")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { store.refresh() } label: { Image(systemName: "arrow.clockwise") }
                        .accessibilityLabel("Refresh")
                }
            }
            .sheet(isPresented: $showFilters) {
                FilterSheetView(model: model, onReset: reset)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        let sync = onEnum(of: state.sync)
        if case .failedNoCache(let failed) = sync {
            ErrorStateView(message: failed.error.userMessage) { store.refresh() }
        } else if case .loading = sync, state.totalCount == 0 {
            LoadingStateView()
        } else if state.totalCount == 0 {
            EmptyStateView(
                title: "No flights available",
                message: "The server has no flights right now. Tap Refresh to check again.",
                identifier: TestIds.shared.emptyState
            ) { store.refresh() }
        } else if state.resultCount == 0 {
            NoMatchesView(onReset: reset)
        } else if mode == .list {
            FlightListView(model: model)
        } else {
            FlightMapView(model: model, bottomInset: state.selected == nil ? 0 : 300)
        }
    }

    private var summaryRow: some View {
        HStack(spacing: 8) {
            // With no data at all, the loading, error or empty state below says what's going on.
            if state.totalCount > 0 {
                Text(verbatim: FormattingKt.formatFlightCount(count: state.resultCount))
                    .font(.subheadline.weight(.semibold))
                    .accessibilityIdentifier(TestIds.shared.resultCount)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(state.activeFilters, id: \.label) { filter in
                        Button { showFilters = true } label: {
                            Text(filter.label)
                                .font(.caption.weight(.medium))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Capsule().fill(Color.accentColor.opacity(0.12)))
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier(TestIds.shared.filterChip(kind: filter.kind))
                    }
                }
            }
            if !state.activeFilters.isEmpty {
                Button("Reset", action: reset)
                    .font(.subheadline)
                    .accessibilityIdentifier(TestIds.shared.filtersReset)
            }
            Button { showFilters = true } label: {
                Image(systemName: "line.3.horizontal.decrease.circle").font(.title3)
            }
            .disabled(state.filters.bounds == nil)
            .accessibilityLabel("Filters")
            .accessibilityIdentifier(TestIds.shared.filtersOpen)
        }
    }

    private var destinationSheetShown: Binding<Bool> {
        Binding(
            get: { model.state.focusedDestination != nil },
            set: { shown in if !shown { model.store.focusDestination(airportCode: nil) } }
        )
    }

    private func reset() {
        origin = ""
        destination = ""
        store.resetFilters()
    }
}

private struct SearchField: View {
    let title: String
    @Binding var text: String
    let identifier: String
    let onChange: (String) -> Void

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "mappin.and.ellipse").foregroundStyle(.secondary)
            TextField(title, text: $text)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .accessibilityHint("City or airport code")
                .accessibilityIdentifier(identifier)
            if !text.isEmpty {
                Button { text = "" } label: { Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary) }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Clear \(title)")
                    .accessibilityIdentifier("\(identifier).clear")
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(.secondarySystemBackground)))
        .onChange(of: text) { _, newValue in onChange(newValue) }
    }
}

private struct FlightListView: View {
    let model: ExplorerModel

    var body: some View {
        let state = model.state
        let savedIds = state.savedIds
        List(state.results, id: \.id) { flight in
            Button { model.store.select(flightId: flight.id) } label: {
                FlightRowView(flight: flight, saved: savedIds.contains(flight.id))
            }
            .buttonStyle(.plain)
            .listRowBackground(flight.id == state.selected?.id ? Color.accentColor.opacity(0.12) : Color.clear)
            .accessibilityIdentifier(TestIds.shared.flightRow(flightId: flight.id))
        }
        .listStyle(.plain)
        .refreshable { model.store.refresh() }
        .safeAreaInset(edge: .bottom) {
            if state.selected != nil { Color.clear.frame(height: 280) }
        }
    }
}
