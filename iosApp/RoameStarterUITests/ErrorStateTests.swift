import XCTest

/// QA O4, O5 on a fresh install, with the server's mode switched from the test.
final class ErrorStateTests: UITestCase {
    override func tearDownWithError() throws {
        _ = ServerControl.setMode("normal")
        try super.tearDownWithError()
    }

    @MainActor
    func testNoCacheErrorOffersRetryThatLoadsOnceTheServerRecovers() {
        XCTAssertTrue(ServerControl.setMode("error"))
        launch()
        let retry = app.buttons[ID.errorRetry]
        XCTAssertTrue(retry.waitForExistence(timeout: 60))
        screenshot("O4 error with no cache")
        XCTAssertTrue(ServerControl.setMode("normal"))
        retry.tap()
        waitForAllFlights()
    }

    @MainActor
    func testEmptyResponseOnAFreshInstallSaysNoFlightsAvailable() {
        XCTAssertTrue(ServerControl.setMode("empty"))
        launch()
        XCTAssertTrue(element(ID.emptyState).waitForExistence(timeout: 60))
        XCTAssertFalse(app.buttons[ID.errorRetry].exists)
        screenshot("O5 empty response")
    }
}
