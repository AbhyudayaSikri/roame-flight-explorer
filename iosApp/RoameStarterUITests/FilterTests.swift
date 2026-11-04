import XCTest

/// QA F3, F4, F5, F7, F8, F10. Date ranges (F1, F2) and the combined F6 case are covered by the
/// shared `FilterTest`, which the store uses unchanged.
final class FilterTests: UITestCase {
    @MainActor
    func testCabinCountsUpdateLiveAndShowAChip() {
        launchWithAllFlights()
        openFilters()
        selectCabin("Business")
        expectDone("Show 344 flights")
        selectCabin("Economy")
        expectDone("Show 467 flights")
        closeFilters()
        expectCount("467 flights")
        XCTAssertTrue(app.buttons[ID.chip("cabin")].exists)
        screenshot("F7 cabin chip")
    }

    @MainActor
    func testProgramsCombineWithOrAndDeselectingRestoresAll() {
        launchWithAllFlights()
        openFilters()
        XCTAssertTrue(app.switches[ID.program("Mileage Plan")].waitForExistence(timeout: 10))
        XCTAssertTrue(app.switches[ID.program("MileagePlus")].exists)
        toggleProgram("Avios")
        toggleProgram("Aeroplan")
        expectDone("Show 386 flights")
        screenshot("F5 programs")
        toggleProgram("Avios")
        toggleProgram("Aeroplan")
        expectDone("Show 1,000 flights")
        closeFilters()
        expectCount("1,000 flights")
    }

    @MainActor
    func testLowestPointsLimitLeavesTheSingleCheapestFlight() {
        launchWithAllFlights()
        openFilters()
        app.sliders[ID.points].adjust(toNormalizedSliderPosition: 0)
        expectDone("Show 1 flight")
        closeFilters()
        expectCount("1 flight")
        let row = flightRows.firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        XCTAssertTrue(row.label.contains("LAX → MEX"), row.label)
        XCTAssertTrue(row.label.contains("9,000 pts"), row.label)
    }

    @MainActor
    func testResetClearsSearchAndEveryFilter() {
        launchWithAllFlights()
        search(ID.origin, "jfk")
        openFilters()
        selectCabin("Business")
        closeFilters()
        XCTAssertTrue(app.buttons[ID.chip("origin")].exists)
        XCTAssertTrue(app.buttons[ID.chip("cabin")].exists)

        app.buttons[ID.filtersReset].tap()
        waitForAllFlights()
        XCTAssertFalse(app.buttons[ID.clear(ID.origin)].exists, "The origin field is empty again")
        XCTAssertFalse(app.buttons[ID.filtersReset].exists)
    }
}
