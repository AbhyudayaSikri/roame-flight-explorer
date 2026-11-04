import SwiftUI
@preconcurrency import Shared

struct LoadingStateView: View {
    var body: some View {
        ProgressView("Loading flights…")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityIdentifier(TestIds.shared.loading)
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
