import XCTest

final class LaunchTests: XCTestCase {
    @MainActor
    func testLaunchShowsSharedGreeting() {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.staticTexts["Hello world!"].waitForExistence(timeout: 60))

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "launch"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
