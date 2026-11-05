import XCTest

/// QA S1–S6.
final class SearchTests: UITestCase {
    @MainActor
    func testOriginIgnoresCaseAndSurroundingSpaces() {
        launchWithAllFlights()
        search(ID.origin, "  jfk ")
        expectCount("113 flights")
    }

    @MainActor
    func testOriginMatchesTheCityName() {
        launchWithAllFlights()
        search(ID.origin, "new york")
        expectCount("113 flights")
    }

    @MainActor
    func testDestinationCityCoversBothTokyoAirports() {
        launchWithAllFlights()
        search(ID.destination, "TOKYO")
        expectCount("110 flights")
        showMap()
        XCTAssertTrue(app.buttons[ID.marker("HND")].waitForExistence(timeout: 15))
        XCTAssertTrue(app.buttons[ID.marker("NRT")].exists)
        XCTAssertEqual(markers.count, 2)
    }

    @MainActor
    func testOriginAndDestinationCombine() {
        launchWithAllFlights()
        search(ID.origin, "jfk")
        search(ID.destination, "tokyo")
        expectCount("7 flights")
        // QA P5: rows are read with words, not "→" and "pts".
        let label = flightRows.firstMatch.label
        XCTAssertTrue(label.hasPrefix("JFK to ") && label.contains(" points"), label)
        screenshot("S4 jfk to tokyo")
    }

    @MainActor
    func testNoMatchesIsAnEmptyStateWithResetNotAnError() {
        launchWithAllFlights()
        search(ID.origin, "xyz")
        expectCount("0 flights")
        XCTAssertTrue(element(ID.noMatches).waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons[ID.errorRetry].exists)
        screenshot("S5 no matches")
        app.buttons["Reset filters"].tap()
        waitForAllFlights()
    }

    /// An exact airport code wins over city prefixes: "dub" is Dublin, not Dubai, and "fra" is
    /// Frankfurt (never an origin), not San Francisco. Typing on falls back to prefixes.
    @MainActor
    func testAnExactCodeWinsOverCityPrefixes() {
        launchWithAllFlights()
        search(ID.destination, "dub")
        expectCount("29 flights")
        search(ID.destination, "du")
        expectCount("69 flights")
        clearSearch(ID.destination)
        waitForAllFlights()
        search(ID.origin, "fra")
        expectCount("0 flights")
        XCTAssertTrue(element(ID.noMatches).waitForExistence(timeout: 10))
        search(ID.origin, "fran")
        expectCount("123 flights")
    }
}
