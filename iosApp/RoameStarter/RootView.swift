import SwiftUI
@preconcurrency import Shared

struct RootView: View {
    let model: ExplorerModel

    var body: some View {
        TabView {
            ExploreView(model: model)
                .tabItem { Label("Explore", systemImage: "magnifyingglass") }
            SavedView(model: model)
                .tabItem { Label("Saved", systemImage: "heart") }
                .badge(model.saved.count)
        }
        .overlay(alignment: .top) {
            if let message = model.message {
                MessageBanner(message: message, store: model.store)
            }
        }
        // Saves and removes show in savedIds at once (optimistic), so the feedback comes with the tap.
        .sensoryFeedback(trigger: model.savedIds) { old, new in
            new.count > old.count ? .success : (new.count < old.count ? .impact(weight: .light) : nil)
        }
        .sensoryFeedback(.selection, trigger: model.selected?.id) { _, new in new != nil }
        .sensoryFeedback(.error, trigger: model.message?.id) { _, new in new != nil }
    }
}

/// One-shot messages such as "Couldn't save…"; dismissed after a few seconds or on tap.
private struct MessageBanner: View {
    let message: UserMessage
    let store: FlightExplorerStore

    var body: some View {
        Text(message.text)
            .font(.subheadline.weight(.medium))
            .foregroundStyle(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Capsule().fill(Color.red.opacity(0.92)))
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .accessibilityIdentifier(TestIds.shared.message)
            .onTapGesture { store.messageShown(messageId: message.id) }
            .task(id: message.id) {
                try? await Task.sleep(for: .seconds(4))
                store.messageShown(messageId: message.id)
            }
    }
}
