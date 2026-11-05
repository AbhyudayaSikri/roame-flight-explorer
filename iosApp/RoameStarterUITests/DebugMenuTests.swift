import XCTest

/// The debug menu's Server URL, against the API on the runner's LAN address (CI passes it as
/// TEST_RUNNER_LAN_SERVER_URL). A plain-HTTP IP address also proves the App Transport Security setup.
final class DebugMenuTests: UITestCase {
    /// Nothing listens here, so the build default fails until the debug menu points elsewhere.
    private let deadDefault = "http://localhost:9"

    @MainActor
    func testAServerUrlFromTheDebugMenuLoadsFlightsAndIsKept() throws {
        guard let lanUrl = ProcessInfo.processInfo.environment["LAN_SERVER_URL"], !lanUrl.isEmpty else {
            throw XCTSkip("LAN_SERVER_URL isn't set (CI sets TEST_RUNNER_LAN_SERVER_URL)")
        }
        launch(apiBaseUrl: deadDefault)
        XCTAssertTrue(app.buttons[ID.errorRetry].waitForExistence(timeout: 30))

        openDebugMenu()
        let field = app.textFields[ID.debugServerUrl]
        field.tap()
        field.clearAndType(lanUrl)
        app.buttons[ID.debugServerUrlSave].tap()
        waitForAllFlights()
        screenshot("Debug server URL, LAN address")

        // Kept across launches. The cache shows 1,000 flights either way; the refresh succeeding
        // (no stale banner) shows it went to the saved address, not the dead default.
        relaunch(apiBaseUrl: deadDefault)
        waitForAllFlights()
        XCTAssertFalse(element(ID.staleBanner).waitForExistence(timeout: 8), "The launch refresh used the saved address")

        openDebugMenu()
        app.buttons[ID.debugServerUrlDefault].tap()
        XCTAssertTrue(element(ID.staleBanner).waitForExistence(timeout: 30), "Back on the dead default: cached flights, stale banner")
    }

    @MainActor
    func testAnAddressThatIsntAServerCantBeSaved() {
        launchWithAllFlights()
        openDebugMenu()
        let field = app.textFields[ID.debugServerUrl]
        field.tap()
        field.clearAndType("ftp://nas.local")
        XCTAssertTrue(element(ID.debugServerUrlProblem).waitForExistence(timeout: 5))
        XCTAssertEqual(element(ID.debugServerUrlProblem).label, "Use an http:// or https:// address.")
        XCTAssertFalse(app.buttons[ID.debugServerUrlSave].isEnabled)
    }

    @MainActor
    private func openDebugMenu() {
        let open = app.buttons[ID.debugOpen]
        XCTAssertTrue(open.waitForExistence(timeout: 10))
        open.tap()
        XCTAssertTrue(app.textFields[ID.debugServerUrl].waitForExistence(timeout: 10))
    }

    @MainActor
    private func relaunch(apiBaseUrl: String) {
        app.terminate()
        launch(resetData: false, apiBaseUrl: apiBaseUrl)
    }
}

extension XCUIElement {
    /// Replaces whatever the field holds.
    @MainActor
    func clearAndType(_ text: String) {
        let current = value as? String ?? ""
        if !current.isEmpty {
            typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: current.count))
        }
        typeText(text)
    }
}
