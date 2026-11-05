import XCTest

/// QA P3 (dark mode): screenshots for review. `-forceDarkMode` sets the app's appearance, so the
/// simulator's own setting doesn't matter.
final class AppearanceTests: UITestCase {
    @MainActor
    func testDarkModeScreenshots() {
        launchWithAllFlights(extra: ["-forceDarkMode"])
        screenshot("P3 dark list")
        selectFirstRow()
        screenshot("P3 dark card")
        showMap()
        XCTAssertTrue(element(ID.card).waitForExistence(timeout: 5))
        screenshot("P3 dark map")
        openFilters()
        screenshot("P3 dark filters")
        closeFilters()
        openSaved()
        screenshot("P3 dark saved")
    }
}
