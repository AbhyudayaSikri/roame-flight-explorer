import XCTest

/// QA P3 (dark mode) and P4 (largest text): screenshots for review. `-forceDarkMode` and UIKit's
/// `-UIPreferredContentSizeCategoryName` set them per launch, whatever the simulator's settings.
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

    /// The largest size in Settings, then the largest with Larger Accessibility Sizes on.
    @MainActor
    func testLargestTextScreenshots() {
        let sizes = [("P4 largest text", "UICTContentSizeCategoryXXXL"), ("P4 largest accessibility text", "UICTContentSizeCategoryAccessibilityXXXL")]
        for (name, category) in sizes {
            launchWithAllFlights(extra: ["-UIPreferredContentSizeCategoryName", category])
            screenshot("\(name) list")
            selectFirstRow()
            screenshot("\(name) card")
            app.terminate()
        }
    }
}
