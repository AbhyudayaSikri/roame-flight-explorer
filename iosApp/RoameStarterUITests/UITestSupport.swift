import Network
import XCTest

/// Accessibility identifiers from shared `TestIds` (the test bundle doesn't link the Shared framework).
enum ID {
    static let origin = "search.origin"
    static let destination = "search.destination"
    static let resultCount = "results.count"
    static let filtersOpen = "filters.open"
    static let filtersReset = "filters.reset"
    static let filtersDone = "filters.done"
    static let points = "filters.points"
    static let card = "card"
    static let cardSave = "card.save"
    static let cardRemove = "card.remove"
    static let cardUpdate = "card.update"
    static let staleBanner = "banner.stale"
    static let errorRetry = "error.retry"
    static let emptyState = "state.empty"
    static let noMatches = "state.noMatches"
    static let loading = "state.loading"
    static let message = "message"
    static let savedEmpty = "saved.empty"
    static let debugOpen = "debug.open"
    static let debugServerUrl = "debug.serverUrl"
    static let debugServerUrlProblem = "debug.serverUrl.problem"
    static let debugServerUrlSave = "debug.serverUrl.save"
    static let debugServerUrlDefault = "debug.serverUrl.default"
    static func clear(_ field: String) -> String { "\(field).clear" }
    static func chip(_ kind: String) -> String { "filters.chip.\(kind)" }
    static func chipRemove(_ kind: String) -> String { "filters.chip.\(kind).remove" }
    static func cabin(_ name: String?) -> String { "filters.cabin.\(name ?? "any")" }
    static func program(_ name: String) -> String { "filters.program.\(name)" }
    static func marker(_ airport: String) -> String { "marker.\(airport)" }
    static func cluster(_ id: String) -> String { "cluster.\(id)" }
}

/// Switches the local API's mode (server started with `--allow-control`). Uses a raw TCP
/// connection because App Transport Security blocks plain-HTTP URLSession calls from the runner.
enum ServerControl {
    private final class Outcome: @unchecked Sendable {
        var succeeded = false
    }

    static func setMode(_ mode: String) -> Bool {
        let done = DispatchSemaphore(value: 0)
        let outcome = Outcome()
        let connection = NWConnection(host: "127.0.0.1", port: 8080, using: .tcp)
        let request = "POST /control/mode?value=\(mode) HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
        connection.stateUpdateHandler = { state in
            switch state {
            case .ready:
                connection.send(content: Data(request.utf8), completion: .contentProcessed { _ in
                    connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) { data, _, _, _ in
                        outcome.succeeded = data.map { String(decoding: $0, as: UTF8.self).hasPrefix("HTTP/1.1 200") } ?? false
                        connection.cancel()
                        done.signal()
                    }
                })
            case .failed, .waiting:
                connection.cancel()
                done.signal()
            default:
                break
            }
        }
        connection.start(queue: .global())
        return done.wait(timeout: .now() + 10) == .success && outcome.succeeded
    }
}

/// Helpers are main-actor isolated like XCUIApplication; test methods are annotated `@MainActor` too.
class UITestCase: XCTestCase {
    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    /// Fresh-install launches pass `-resetData`, so each test starts from an empty database.
    @MainActor
    func launch(resetData: Bool = true, extra: [String] = [], apiBaseUrl: String = "http://localhost:8080") {
        let app = XCUIApplication()
        app.launchArguments = ["-apiBaseUrl", apiBaseUrl] + (resetData ? ["-resetData"] : []) + extra
        app.launch()
        self.app = app
    }

    @MainActor
    func launchWithAllFlights(extra: [String] = []) {
        launch(extra: extra)
        waitForAllFlights()
    }

    @MainActor
    func relaunch() {
        app.terminate()
        launch(resetData: false)
    }

    @MainActor
    func element(_ id: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: id).firstMatch
    }

    @MainActor
    func expectLabel(_ element: XCUIElement, _ label: String, timeout: TimeInterval = 20,
                     file: StaticString = #filePath, line: UInt = #line) {
        let expectation = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label == %@", label), object: element)
        if XCTWaiter().wait(for: [expectation], timeout: timeout) != .completed {
            let actual = element.exists ? element.label : "no element"
            XCTFail("Expected \"\(label)\", found \"\(actual)\"", file: file, line: line)
        }
    }

    @MainActor
    func expectCount(_ label: String, timeout: TimeInterval = 20, file: StaticString = #filePath, line: UInt = #line) {
        expectLabel(app.staticTexts[ID.resultCount], label, timeout: timeout, file: file, line: line)
    }

    /// The filter sheet's Done button reads "Show N flights" and updates live.
    @MainActor
    func expectDone(_ label: String, file: StaticString = #filePath, line: UInt = #line) {
        expectLabel(app.buttons[ID.filtersDone], label, file: file, line: line)
    }

    @MainActor
    func waitForAllFlights(file: StaticString = #filePath, line: UInt = #line) {
        expectCount("1,000 flights", timeout: 60, file: file, line: line)
    }

    @MainActor
    func search(_ fieldId: String, _ text: String) {
        let field = app.textFields[fieldId]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        let clear = app.buttons[ID.clear(fieldId)]
        if clear.exists { clear.tap() }
        field.tap()
        field.typeText(text + "\n")
    }

    @MainActor
    func clearSearch(_ fieldId: String) {
        app.buttons[ID.clear(fieldId)].tap()
    }

    @MainActor
    var flightRows: XCUIElementQuery {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'flight.row.'"))
    }

    @MainActor
    var savedRows: XCUIElementQuery {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'saved.row.'"))
    }

    @MainActor
    var savedRemoveButtons: XCUIElementQuery {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'saved.remove.'"))
    }

    @MainActor
    var markers: XCUIElementQuery {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'marker.'"))
    }

    @MainActor
    var clusters: XCUIElementQuery {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'cluster.'"))
    }

    /// Taps the first result row and waits for the flight card. Call it only after `expectCount`
    /// confirms the latest search: the store filters asynchronously, so rows from the previous
    /// query can still be on screen right after typing.
    @MainActor
    func selectFirstRow(file: StaticString = #filePath, line: UInt = #line) {
        let row = flightRows.firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "No result rows", file: file, line: line)
        row.tap()
        XCTAssertTrue(element(ID.card).waitForExistence(timeout: 10), "No flight card", file: file, line: line)
    }

    /// Every text on the flight card, which together identify the selected flight.
    @MainActor
    func cardTexts() -> [String] {
        element(ID.card).staticTexts.allElementsBoundByIndex.map(\.label)
    }

    /// Searches JFK → HND, selects the first flight and saves it.
    @MainActor
    func saveFirstJfkToHaneda() {
        search(ID.origin, "jfk")
        search(ID.destination, "hnd")
        expectCount("3 flights")
        selectFirstRow()
        app.buttons[ID.cardSave].tap()
        XCTAssertTrue(app.buttons[ID.cardRemove].waitForExistence(timeout: 10))
    }

    @MainActor func showMap() { app.segmentedControls.buttons["Map"].tap() }
    @MainActor func showList() { app.segmentedControls.buttons["List"].tap() }
    @MainActor func openExplore() { app.tabBars.buttons.element(boundBy: 0).tap() }
    @MainActor func openSaved() { app.tabBars.buttons.element(boundBy: 1).tap() }

    @MainActor
    func openFilters() {
        app.buttons[ID.filtersOpen].tap()
        XCTAssertTrue(app.buttons[ID.filtersDone].waitForExistence(timeout: 10))
    }

    @MainActor
    func closeFilters() {
        app.buttons[ID.filtersDone].tap()
        XCTAssertTrue(app.buttons[ID.filtersDone].waitForNonExistence(timeout: 10))
    }

    /// Scrolls the filter sheet until [element] is on screen; rows below the fold may not exist yet.
    @MainActor
    func reveal(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        var swipes = 0
        while !(element.exists && element.isHittable) && swipes < 6 {
            app.swipeUp()
            swipes += 1
        }
        XCTAssertTrue(element.exists && element.isHittable, "Couldn't scroll to \(element)", file: file, line: line)
    }

    /// "Any", "Economy", "Premium Economy" or "Business".
    @MainActor
    func selectCabin(_ cabin: String) {
        let option = app.buttons[ID.cabin(cabin == "Any" ? nil : cabin)]
        reveal(option)
        option.tap()
    }

    @MainActor
    func toggleProgram(_ name: String) {
        let toggle = app.switches[ID.program(name)]
        reveal(toggle)
        let before = toggle.value as? String ?? ""
        toggle.coordinate(withNormalizedOffset: CGVector(dx: 0.92, dy: 0.5)).tap()
        let flipped = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value != %@", before), object: toggle)
        XCTAssertEqual(XCTWaiter().wait(for: [flipped], timeout: 5), .completed, "\(name) toggled")
    }

    /// Slows the recorded demo down enough to follow.
    func pace(_ seconds: TimeInterval = 1.5) {
        Thread.sleep(forTimeInterval: seconds)
    }

    @MainActor
    /// Review evidence, not an assertion: on a loaded CI runner XCTest can time out capturing the
    /// screen ("Timed out while requesting screenshot"), which mustn't fail the test.
    func screenshot(_ name: String) {
        XCTExpectFailure("Capturing a screenshot can time out on a busy runner", options: .nonStrict()) {
            let attachment = XCTAttachment(screenshot: app.screenshot())
            attachment.name = name
            attachment.lifetime = .keepAlways
            add(attachment)
        }
    }
}
