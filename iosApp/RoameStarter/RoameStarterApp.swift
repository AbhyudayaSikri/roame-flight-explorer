import SwiftUI
@preconcurrency import Shared

@main
struct RoameStarterApp: App {
    private let spike = SkieSpike()
    private let probeLaunchCount = PersistenceProbe.launchCount()

    var body: some Scene {
        WindowGroup {
            SpikeView(spike: spike, probeLaunchCount: probeLaunchCount)
        }
    }
}

struct SpikeView: View {
    let spike: SkieSpike
    let probeLaunchCount: Int?
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
            if let probeLaunchCount {
                Text("launches: \(probeLaunchCount)")
                    .accessibilityIdentifier("probe.launchCount")
            }
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
