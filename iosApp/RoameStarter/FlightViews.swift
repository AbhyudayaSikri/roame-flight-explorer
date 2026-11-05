import SwiftUI
import UIKit
@preconcurrency import Shared

struct FlightRowView: View {
    let flight: Flight
    let saved: Bool
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        // At accessibility text sizes the points go under the route instead of squeezing beside it.
        let stacked = typeSize.isAccessibilitySize
        let layout = stacked ? AnyLayout(VStackLayout(alignment: .leading, spacing: 6)) : AnyLayout(HStackLayout(alignment: .top, spacing: 12))
        layout {
            VStack(alignment: .leading, spacing: 3) {
                Text("\(flight.origin) → \(flight.destination)").font(.headline)
                Text("\(flight.originCity) → \(flight.destinationCity)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text("\(flight.dateLabel) · \(flight.cabin) · \(flight.program)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            if !stacked { Spacer(minLength: 8) }
            VStack(alignment: stacked ? .leading : .trailing, spacing: 4) {
                Text(flight.pointsLabel)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.accentColor)
                    .fixedSize()
                if saved {
                    Image(systemName: "heart.fill")
                        .foregroundStyle(.orange)
                        .accessibilityLabel("Saved")
                }
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }
}

/// The selected flight with every field the brief asks for, plus the action for its save status.
struct FlightCardView: View {
    let flight: Flight
    let status: SaveStatus
    let onSave: () -> Void
    let onRemove: () -> Void
    let onClose: () -> Void
    var footnote: String? = nil
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(flight.origin) → \(flight.destination)").font(.title2.weight(.bold))
                    Text("\(flight.originCity) to \(flight.destinationCity)").foregroundStyle(.secondary)
                }
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill").font(.title2).foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
                .accessibilityIdentifier(TestIds.shared.cardClose)
            }
            Divider()
            detail("Departs", flight.dateLabel)
            detail("Cabin", flight.cabin)
            detail("Points", flight.pointsLabel)
            detail("Program", flight.program)
            if let footnote {
                Text(footnote).font(.footnote).foregroundStyle(.secondary)
            }
            actions
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color(.systemBackground)))
        .shadow(color: .black.opacity(0.18), radius: 10, y: 3)
        .padding(12)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(TestIds.shared.card)
    }

    // The screen updates at once (optimistic), so the feedback comes with the tap.
    private func save() {
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        onSave()
    }

    private func remove() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        onRemove()
    }

    /// Label and value side by side, or stacked at accessibility text sizes so neither truncates.
    private func detail(_ label: String, _ value: String) -> some View {
        let stacked = typeSize.isAccessibilitySize
        let layout = stacked ? AnyLayout(VStackLayout(alignment: .leading, spacing: 2)) : AnyLayout(HStackLayout())
        return layout {
            Text(label).foregroundStyle(.secondary)
            if !stacked { Spacer() }
            Text(value).fontWeight(.medium)
        }
    }

    @ViewBuilder
    private var actions: some View {
        switch status {
        case .notSaved:
            Button(action: save) {
                Label("Save", systemImage: "heart").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier(TestIds.shared.cardSave)
        case .saved:
            Button(role: .destructive, action: remove) {
                Label("Saved · Remove", systemImage: "heart.fill").frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .accessibilityIdentifier(TestIds.shared.cardRemove)
        case .outdated:
            HStack {
                Button(action: save) { Text("Update saved").frame(maxWidth: .infinity) }
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier(TestIds.shared.cardUpdate)
                Button(role: .destructive, action: remove) { Text("Remove").frame(maxWidth: .infinity) }
                    .buttonStyle(.bordered)
                    .accessibilityIdentifier(TestIds.shared.cardRemove)
            }
        case .inProgress:
            HStack(spacing: 8) {
                ProgressView()
                Text("Updating…")
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
        }
    }
}

/// A tapped map marker: that destination's flights; picking one selects it.
struct DestinationSheetView: View {
    let model: ExplorerModel

    var body: some View {
        let savedIds = model.savedIds
        NavigationStack {
            List(model.focusedFlights, id: \.id) { flight in
                Button {
                    model.store.select(flightId: flight.id)
                    model.store.focusDestination(airportCode: nil)
                } label: {
                    FlightRowView(flight: flight, saved: savedIds.contains(flight.id))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier(TestIds.shared.flightRow(flightId: flight.id))
            }
            .listStyle(.plain)
            .navigationTitle(title(model.focusedDestination))
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium, .large])
        .accessibilityIdentifier(TestIds.shared.destinationSheet)
    }

    private func title(_ marker: DestinationMarker?) -> String {
        guard let marker else { return "" }
        return "\(marker.city) (\(marker.airport)) · \(marker.count) flights"
    }
}
