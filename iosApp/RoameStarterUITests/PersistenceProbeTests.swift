import XCTest

// CI runs these two tests in separate `xcodebuild test-without-building` invocations (ios.yml);
// running both in one invocation would always report that the data survived.
final class PersistenceProbeTests: XCTestCase {
    @MainActor
    func testFirstLaunch() {
        XCTAssertEqual(launchCount(arguments: ["-persistenceProbe", "-persistenceProbeReset"]), 1)
    }

    @MainActor
    func testSecondLaunch() {
        let count = launchCount(arguments: ["-persistenceProbe"])
        let line = "PERSISTENCE_PROBE launchCount=\(count.map(String.init) ?? "missing")"
        print(line)
        let attachment = XCTAttachment(string: line)
        attachment.name = "persistence-probe"
        attachment.lifetime = .keepAlways
        add(attachment)
        XCTAssertNotNil(count)
    }

    @MainActor
    private func launchCount(arguments: [String]) -> Int? {
        let app = XCUIApplication()
        app.launchArguments = arguments
        app.launch()
        let label = app.staticTexts["probe.launchCount"]
        guard label.waitForExistence(timeout: 30) else { return nil }
        return Int(label.label.replacingOccurrences(of: "launches: ", with: ""))
    }
}
