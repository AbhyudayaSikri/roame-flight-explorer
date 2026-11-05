import XCTest

/// QA F3, F4, F5, F7, F8, F10, F11, F12. Date ranges (F1, F2) and the combined F6 case are covered by the
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
        reveal(app.switches[ID.program("MileagePlus")])
        XCTAssertTrue(app.switches[ID.program("Mileage Plan")].exists)
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
        // adjust(toNormalizedSliderPosition:) lands imprecisely on SwiftUI sliders, so drag the
        // thumb (at "no limit", the right end) past the left end instead.
        let slider = app.sliders[ID.points]
        XCTAssertTrue(slider.waitForExistence(timeout: 10))
        slider.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5))
            .press(forDuration: 0.3, thenDragTo: slider.coordinate(withNormalizedOffset: CGVector(dx: -0.3, dy: 0.5)))
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
        expectCount("113 flights")
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

    /// F12: −/+ move the points limit one step; back up to the highest price is no limit again.
    @MainActor
    func testMinusAndPlusStepThePointsLimit() {
        launchWithAllFlights()
        openFilters()
        let stepper = app.steppers[ID.pointsStepper]
        reveal(stepper)
        let value = app.staticTexts[ID.pointsValue]
        expectLabel(value, "No limit")

        stepper.buttons.element(boundBy: 0).tap()
        expectLabel(value, "Up to 132,500 pts")
        expectDone("Show 999 flights")
        screenshot("F12 one step below the highest price")

        stepper.buttons.element(boundBy: 1).tap()
        expectLabel(value, "No limit")
        expectDone("Show 1,000 flights")
        closeFilters()
    }

    /// F11: a chip's × removes only its own filter; removing From also empties the From field.
    @MainActor
    func testEachChipRemovesOnlyItsOwnFilter() {
        launchWithAllFlights()
        search(ID.origin, "jfk")
        expectCount("113 flights")
        openFilters()
        selectCabin("Business")
        closeFilters()
        expectCount("35 flights")

        app.buttons[ID.chipRemove("cabin")].tap()
        expectCount("113 flights")
        XCTAssertTrue(app.buttons[ID.chip("cabin")].waitForNonExistence(timeout: 10))
        XCTAssertTrue(app.buttons[ID.clear(ID.origin)].exists, "The From field still says jfk")
        screenshot("F11 cabin chip removed")

        app.buttons[ID.chipRemove("origin")].tap()
        waitForAllFlights()
        XCTAssertFalse(app.buttons[ID.clear(ID.origin)].exists, "The From field is empty again")
        XCTAssertFalse(app.buttons[ID.filtersReset].exists)
    }
}
