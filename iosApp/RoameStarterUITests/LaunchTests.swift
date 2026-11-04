import XCTest

/// QA L1, L2, O7.
final class LaunchTests: UITestCase {
    @MainActor
    func testFreshInstallLoadsEveryFlightWithNoFiltersActive() {
        launchWithAllFlights()
        XCTAssertFalse(app.buttons[ID.filtersReset].exists)
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'filters.chip.'")).count, 0)
        screenshot("L1 launch")

        openFilters()
        expectDone("Show 1,000 flights")
        screenshot("L2 default filters")
        closeFilters()
    }

    @MainActor
    func testRotatingAndBackgroundingKeepsSearchAndSelection() {
        launchWithAllFlights()
        search(ID.origin, "jfk")
        expectCount("113 flights")
        selectFirstRow()

        XCUIDevice.shared.orientation = .landscapeLeft
        expectCount("113 flights")
        XCTAssertTrue(element(ID.card).exists)
        screenshot("O7 landscape")
        XCUIDevice.shared.orientation = .portrait

        XCUIDevice.shared.press(.home)
        app.activate()
        expectCount("113 flights")
        XCTAssertTrue(element(ID.card).waitForExistence(timeout: 10))
    }
}
