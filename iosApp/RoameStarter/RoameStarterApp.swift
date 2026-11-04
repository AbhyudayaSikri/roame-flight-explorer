import SwiftUI
@preconcurrency import Shared

@main
struct RoameStarterApp: App {
    private let spike = SkieSpike()

    var body: some Scene {
        WindowGroup {
            SpikeView(spike: spike)
        }
    }
}

struct SpikeView: View {
    let spike: SkieSpike
    @State private var message = ""
    @State private var status = ""

    var body: some View {
        VStack(spacing: 16) {
            Text(message)
                .accessibilityIdentifier("spike.message")
            Text(status)
                .accessibilityIdentifier("spike.status")
            Button("Tick") { spike.tick() }
                .accessibilityIdentifier("spike.tick")
        }
        .task {
            for await value in spike.message {
                message = value
            }
        }
        .task {
            for await value in spike.status {
                switch onEnum(of: value) {
                case .idle:
                    status = "idle"
                case .ticked(let ticked):
                    status = "ticked \(ticked.count)"
                }
            }
        }
    }
}
