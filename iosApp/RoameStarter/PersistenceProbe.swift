import Foundation

/// Debug-only: counts launches in a file inside the app's data container, so CI can tell whether
/// that container survives between separate `xcodebuild test-without-building` runs.
enum PersistenceProbe {
    static func launchCount() -> Int? {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        guard arguments.contains("-persistenceProbe") else { return nil }
        let directory = URL.applicationSupportDirectory
        let file = directory.appending(path: "persistence-probe.txt")
        if arguments.contains("-persistenceProbeReset") {
            try? FileManager.default.removeItem(at: file)
        }
        let previous = (try? String(contentsOf: file, encoding: .utf8)).flatMap { Int($0) } ?? 0
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try? String(previous + 1).write(to: file, atomically: true, encoding: .utf8)
        return previous + 1
        #else
        return nil
        #endif
    }
}
