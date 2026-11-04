import XCTest

/// Run on its own while the server is up: leaves a cache and one saved flight for
/// `OfflineRelaunchTests`, which CI runs next with the server stopped.
final class OfflineSeedTests: UITestCase {
    @MainActor
    func testSeedCacheAndOneSavedFlight() {
        launch()
        waitForAllFlights()
        saveFirstJfkToHaneda()
        app.terminate()
    }
}

/// QA O1–O3, with the server actually stopped.
final class OfflineRelaunchTests: UITestCase {
    @MainActor
    func testCachedAndSavedFlightsWorkWithTheServerStopped() {
        launch(resetData: false)
        waitForAllFlights()
        XCTAssertTrue(element(ID.staleBanner).waitForExistence(timeout: 30))
        screenshot("O2 relaunch offline")

        app.buttons["Refresh"].tap()
        XCTAssertTrue(element(ID.staleBanner).waitForExistence(timeout: 30))
        expectCount("1,000 flights")

        search(ID.origin, "jfk")
        expectCount("113 flights")
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
        savedRemoveButtons.firstMatch.tap()
        XCTAssertTrue(element(ID.savedEmpty).waitForExistence(timeout: 10))
        screenshot("O3 removed offline")
    }
}
