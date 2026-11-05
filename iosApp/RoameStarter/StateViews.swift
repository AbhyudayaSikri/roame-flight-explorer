import SwiftUI
@preconcurrency import Shared

/// The first load: grey rows in the shape of the list, so the layout doesn't jump when flights
/// arrive. No pulsing: XCUITest waits for animations to finish before it acts.
struct LoadingStateView: View {
    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<8, id: \.self) { _ in
                PlaceholderRow()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                Divider().padding(.leading, 16)
            }
            Spacer(minLength: 0)
        }
        .redacted(reason: .placeholder)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Loading flights")
        .accessibilityIdentifier(TestIds.shared.loading)
    }
}

/// `FlightRowView`'s layout with sample text; redacted, only its shape shows.
private struct PlaceholderRow: View {
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(verbatim: "JFK → HND").font(.headline)
                Text(verbatim: "New York → Tokyo").font(.subheadline)
                Text(verbatim: "Mon, Feb 1, 2027 · Economy · Avios").font(.caption)
            }
            Spacer(minLength: 8)
            Text(verbatim: "16,500 pts").font(.subheadline.weight(.bold))
        }
        .padding(.vertical, 4)
    }
}

struct ErrorStateView: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("Couldn't load flights", systemImage: "exclamationmark.triangle")
        } description: {
            Text("\(message) Check that the local API is running, then try again.")
        } actions: {
            Button("Retry", action: retry)
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier(TestIds.shared.errorRetry)
        }
    }
}

struct EmptyStateView: View {
    let title: String
    let message: String
    let identifier: String
    let action: (() -> Void)?

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "airplane")
        } description: {
            Text(message)
        } actions: {
            if let action {
                Button("Refresh", action: action).buttonStyle(.bordered)
            }
        }
        .accessibilityIdentifier(identifier)
    }
}

struct NoMatchesView: View {
    let onReset: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("No flights match these filters", systemImage: "magnifyingglass")
        } description: {
            Text("Try a different city or airport, or widen the dates and points.")
        } actions: {
            Button("Reset filters", action: onReset).buttonStyle(.borderedProminent)
        }
        .accessibilityIdentifier(TestIds.shared.noMatches)
    }
}

/// Shown while the last refresh failed but cached flights are still on screen.
struct StaleBanner: View {
    let stale: SyncStateStale

    var body: some View {
        let time = Date(timeIntervalSince1970: Double(stale.lastSuccessAtMillis) / 1000)
            .formatted(date: .omitted, time: .shortened)
        let reason = stale.error is RefreshErrorOffline ? "Offline" : "Couldn't refresh"
        Label("\(reason) · showing results from \(time)", systemImage: "wifi.exclamationmark")
            .font(.footnote.weight(.medium))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
            .background(Color.orange)
            .accessibilityIdentifier(TestIds.shared.staleBanner)
    }
}
