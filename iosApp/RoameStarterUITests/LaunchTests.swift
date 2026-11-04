import XCTest

final class LaunchTests: XCTestCase {
    @MainActor
    func testSharedStateFlowsDriveTheView() {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(text(in: app, "spike.message", equals: "Hello world!"))
        XCTAssertTrue(text(in: app, "spike.status", equals: "idle"))
        attachScreenshot(of: app, named: "skie-initial")

        app.buttons["spike.tick"].tap()

        XCTAssertTrue(text(in: app, "spike.message", equals: "Hello world! (ticks: 1)"))
        XCTAssertTrue(text(in: app, "spike.status", equals: "ticked 1"))
        attachScreenshot(of: app, named: "skie-after-tick")
    }

    @MainActor
    private func text(in app: XCUIApplication, _ identifier: String, equals label: String) -> Bool {
        app.staticTexts
            .matching(NSPredicate(format: "identifier == %@ AND label == %@", identifier, label))
            .firstMatch
            .waitForExistence(timeout: 30)
    }

    @MainActor
    private func attachScreenshot(of app: XCUIApplication, named name: String) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = name
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
