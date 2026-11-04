import XCTest

/// QA F1, F3, F4, F5, F7, F8, F10. F2 and the combined F6 case are covered by the shared
/// `FilterTest`, which the store uses unchanged.
final class FilterTests: UITestCase {
    @MainActor
    func testASingleDayRangeIncludesThatDay() {
        launchWithAllFlights()
        openFilters()

        // The compact "To" picker opens a calendar on April 2027, the last date in the data.
        let end = element(ID.dateEnd)
        XCTAssertTrue(end.waitForExistence(timeout: 10))
        let dateButton = end.buttons.firstMatch
        if dateButton.exists {
            dateButton.tap()
        } else {
            end.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)).tap()
        }
        let previousMonth = app.buttons["Previous Month"]
        XCTAssertTrue(previousMonth.waitForExistence(timeout: 10), "Calendar didn't open")
        previousMonth.tap()
        previousMonth.tap()

        // Day buttons read like "Monday, February 1"; the pattern skips February 10–19.
        let firstOfFebruary = app.buttons.matching(NSPredicate(format: "label MATCHES %@", ".*February 1(,.*)?")).firstMatch
        XCTAssertTrue(firstOfFebruary.waitForExistence(timeout: 10), "No February 1 in the calendar")
        firstOfFebruary.tap()

        // A tap outside the calendar closes it; the calendar floats near the top of the sheet.
        let dismiss = app.buttons["PopoverDismissRegion"]
        if dismiss.exists {
            dismiss.tap()
        } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.95)).tap()
        }
        expectDone("Show 17 flights")
        screenshot("F1 Feb 1 to Feb 1")
        closeFilters()
        expectCount("17 flights")
        XCTAssertTrue(app.buttons[ID.chip("dates")].exists)
    }

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
}
