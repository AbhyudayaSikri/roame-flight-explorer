import XCTest

/// QA V1–V6. V2 and V5 switch the server's mode, so `tearDown` puts it back to normal.
final class SavedTests: UITestCase {
    override func tearDownWithError() throws {
        _ = ServerControl.setMode("normal")
        try super.tearDownWithError()
    }

    @MainActor
    func testSavedFlightShowsEverywhereIgnoresFiltersAndRemovesFromTheCard() {
        launch()
        waitForAllFlights()
        saveFirstJfkToHaneda()
        XCTAssertTrue(flightRows.firstMatch.label.hasSuffix(", saved"), "VoiceOver hears the saved state: \(flightRows.firstMatch.label)")

        // Wait for JFK → Paris to apply, so "Saved ignores the filters" is checked against them.
        search(ID.destination, "paris")
        expectCount("8 flights")
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
        screenshot("V1 V3 saved tab")

        openExplore()
        search(ID.destination, "hnd")
        expectCount("3 flights")
        selectFirstRow()
        app.buttons[ID.cardRemove].tap()
        XCTAssertTrue(app.buttons[ID.cardSave].waitForExistence(timeout: 10))
        openSaved()
        XCTAssertTrue(element(ID.savedEmpty).waitForExistence(timeout: 10))
    }

    @MainActor
    func testRemoveFromTheSavedTab() {
        launch()
        waitForAllFlights()
        saveFirstJfkToHaneda()
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
        savedRemoveButtons.firstMatch.tap()
        XCTAssertTrue(element(ID.savedEmpty).waitForExistence(timeout: 10))
    }

    @MainActor
    func testFailedSaveRollsBackWithAMessage() {
        launch(extra: ["-failSaves"])
        waitForAllFlights()
        search(ID.origin, "jfk")
        expectCount("113 flights")
        selectFirstRow()
        app.buttons[ID.cardSave].tap()
        XCTAssertTrue(element(ID.message).waitForExistence(timeout: 10))
        screenshot("V6 save failed")
        XCTAssertTrue(app.buttons[ID.cardSave].waitForExistence(timeout: 10))
        openSaved()
        XCTAssertTrue(element(ID.savedEmpty).waitForExistence(timeout: 10))
    }

    @MainActor
    func testFailedRemoveRollsBackWithAMessage() {
        launch(extra: ["-failRemoves"])
        waitForAllFlights()
        saveFirstJfkToHaneda()
        app.buttons[ID.cardRemove].tap()
        XCTAssertTrue(element(ID.message).waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons[ID.cardRemove].waitForExistence(timeout: 10))
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
    }

    @MainActor
    func testSavingAgainAfterAPriceChangeUpdatesWithoutDuplicates() {
        launch()
        waitForAllFlights()
        saveFirstJfkToHaneda()
        XCTAssertTrue(ServerControl.setMode("updated"))

        relaunch()
        waitForAllFlights()
        search(ID.origin, "jfk")
        search(ID.destination, "hnd")
        expectCount("3 flights")
        selectFirstRow()
        let update = app.buttons[ID.cardUpdate]
        XCTAssertTrue(update.waitForExistence(timeout: 30))
        screenshot("V2 price changed")
        update.tap()
        XCTAssertTrue(app.buttons[ID.cardRemove].waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons[ID.cardUpdate].exists)
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
    }

    @MainActor
    func testSavedFlightSurvivesAnEmptyResponse() {
        launch()
        waitForAllFlights()
        saveFirstJfkToHaneda()
        XCTAssertTrue(ServerControl.setMode("empty"))
        app.buttons["Refresh"].tap()
        XCTAssertTrue(element(ID.emptyState).waitForExistence(timeout: 30))
        openSaved()
        XCTAssertEqual(savedRows.count, 1)
        screenshot("V5 saved after empty response")
    }
}
