import XCTest

/// QA M1, M3–M7. M8 (the trans-Pacific arc) is judged from the screenshot.
final class MapSelectionTests: UITestCase {
    @MainActor
    func testMarkersShowCountAndLowestPointsPerAirport() {
        launchWithAllFlights()
        showMap()
        screenshot("M1 map, every destination")
        search(ID.destination, "tokyo")
        expectCount("110 flights")
        let haneda = app.buttons[ID.marker("HND")]
        XCTAssertTrue(haneda.waitForExistence(timeout: 15))
        XCTAssertEqual(haneda.label, "HND 55 · 16.5k")
        XCTAssertEqual(app.buttons[ID.marker("NRT")].label, "NRT 55 · 19.5k")
        XCTAssertEqual(markers.count, 2, "55 + 55 markers add up to the 110 results")
        screenshot("M1 map, Tokyo")
    }

    @MainActor
    func testTappingAMergedPillZoomsInUntilItsAirportsSeparate() {
        launchWithAllFlights()
        showMap()
        let merged = clusters.firstMatch
        XCTAssertTrue(merged.waitForExistence(timeout: 15), "At world zoom, nearby airports share a pill")
        let airports = (merged.value as? String ?? "").split(separator: " ").map(String.init)
        XCTAssertGreaterThan(airports.count, 1, merged.debugDescription)
        screenshot("Map, merged pills at world zoom")

        merged.tap()
        let separated = airports.map { app.buttons[ID.marker($0)] }
        let deadline = Date().addingTimeInterval(15)
        while !separated.contains(where: { $0.exists }) && Date() < deadline {
            Thread.sleep(forTimeInterval: 0.5)
        }
        XCTAssertTrue(separated.contains { $0.exists }, "None of \(airports) got its own pill after zooming in")
        screenshot("Map, zoomed into a merged pill")
    }

    @MainActor
    func testMarkerSheetPicksAFlightAndShowsTheCard() {
        launchWithAllFlights()
        search(ID.destination, "hnd")
        expectCount("55 flights")
        showMap()
        let haneda = app.buttons[ID.marker("HND")]
        XCTAssertTrue(haneda.waitForExistence(timeout: 15))
        XCTAssertEqual(markers.count, 1)
        haneda.tap()

        let row = flightRows.firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        screenshot("M3 destination sheet")
        row.tap()
        let card = element(ID.card)
        XCTAssertTrue(card.waitForExistence(timeout: 10))
        for label in ["Departs", "Cabin", "Points", "Program"] {
            XCTAssertTrue(card.staticTexts[label].exists, "Card shows \(label)")
        }
        XCTAssertTrue(app.buttons[ID.cardSave].exists)
        screenshot("M3 route and card")
    }

    @MainActor
    func testSelectionIsSharedBetweenListAndMap() {
        launchWithAllFlights()
        search(ID.origin, "sfo")
        search(ID.destination, "nrt")
        expectCount("6 flights")
        selectFirstRow()
        // All six flights are SFO → NRT, so compare the whole card: date, cabin, points and program.
        let selected = cardTexts()
        showMap()
        XCTAssertTrue(element(ID.card).waitForExistence(timeout: 5))
        XCTAssertEqual(cardTexts(), selected, "The map shows the flight selected on the list")
        screenshot("M5 M8 SFO to NRT route on the map")
        showList()
        XCTAssertTrue(element(ID.card).waitForExistence(timeout: 5))
        XCTAssertEqual(cardTexts(), selected, "Back on the list, the same flight is still selected")
    }

    @MainActor
    func testFilteringOutTheSelectionClearsIt() {
        launchWithAllFlights()
        search(ID.origin, "jfk")
        search(ID.destination, "hnd")
        expectCount("3 flights")
        selectFirstRow()
        search(ID.destination, "paris")
        XCTAssertTrue(element(ID.card).waitForNonExistence(timeout: 10))
        clearSearch(ID.destination)
        expectCount("113 flights")
        XCTAssertFalse(element(ID.card).exists)
    }
}
