import SwiftUI
import UIKit
@preconcurrency import Shared

/// Saved snapshots, newest first. Independent of Explore's filters, and works offline.
struct SavedView: View {
    let model: ExplorerModel

    var body: some View {
        let store = model.store
        NavigationStack {
            Group {
                if model.saved.isEmpty {
                    EmptyStateView(
                        title: "No saved flights yet",
                        message: "Save a flight from Explore and it stays here, even offline.",
                        identifier: "saved.empty",
                        action: nil
                    )
                } else {
                    ZStack(alignment: .bottom) {
                        List(model.saved, id: \.flight.id) { saved in
                            HStack {
                                Button { store.selectSaved(flightId: saved.flight.id) } label: {
                                    FlightRowView(flight: saved.flight, saved: true)
                                }
                                .buttonStyle(.plain)
                                .accessibilityIdentifier(TestIds.shared.savedRow(flightId: saved.flight.id))
                                Button("Remove", role: .destructive) {
                                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                    store.remove(flightId: saved.flight.id)
                                }
                                    .buttonStyle(.borderless)
                                    .font(.subheadline)
                                    .accessibilityIdentifier(TestIds.shared.savedRemove(flightId: saved.flight.id))
                            }
                        }
                        .listStyle(.plain)
                        .safeAreaInset(edge: .bottom) {
                            if model.selectedSaved != nil { Color.clear.frame(height: 300) }
                        }
                        if let selected = model.selectedSaved {
                            FlightCardView(
                                flight: selected.flight,
                                status: .saved,
                                onSave: {},
                                onRemove: { store.remove(flightId: selected.flight.id) },
                                onClose: { store.selectSaved(flightId: nil) },
                                footnote: "Saved snapshot: details as they were when you saved it."
                            )
                        }
                    }
                }
            }
            .navigationTitle("Saved flights")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
