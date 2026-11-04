import XCTest

/// The demo video: CI (ios.yml) records the simulator while this runs and while
/// `OfflineRelaunchTests` runs after it with the server stopped. It also leaves the cache and one
/// saved flight that the offline run relies on.
final class DemoWalkthrough: UITestCase {
    @MainActor
    func testSearchFilterMapSelectSave() {
        launchWithAllFlights()
        pace()
        search(ID.origin, "new york")
        expectCount("113 flights")
        pace()
        search(ID.destination, "tokyo")
        expectCount("7 flights")
        pace()

        openFilters()
        pace()
        selectCabin("Business")
        pace()
        selectCabin("Any")
        closeFilters()

        search(ID.destination, "hnd")
        expectCount("3 flights")
        showMap()
        let haneda = app.buttons[ID.marker("HND")]
        XCTAssertTrue(haneda.waitForExistence(timeout: 15))
        pace()
        haneda.tap()
        let row = flightRows.firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        pace()
        row.tap()
        XCTAssertTrue(element(ID.card).waitForExistence(timeout: 10))
        pace(2)
        app.buttons[ID.cardSave].tap()
        XCTAssertTrue(app.buttons[ID.cardRemove].waitForExistence(timeout: 10))
        pace()

        openSaved()
        XCTAssertEqual(savedRows.count, 1)
        pace(2)
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
        pace()

        app.buttons["Refresh"].tap()
        XCTAssertTrue(element(ID.staleBanner).waitForExistence(timeout: 30))
        expectCount("1,000 flights")

        search(ID.origin, "jfk")
        expectCount("113 flights")
        pace()
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
        pace()
        savedRemoveButtons.firstMatch.tap()
        XCTAssertTrue(element(ID.savedEmpty).waitForExistence(timeout: 10))
        screenshot("O3 removed offline")
        pace()
    }
}
