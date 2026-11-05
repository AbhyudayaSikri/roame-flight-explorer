package travel.roame.takehome.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.geojson.Point
import travel.roame.takehome.FilterKind
import travel.roame.takehome.TestIds
import travel.roame.takehome.android.ui.MARKER_LAYER
import travel.roame.takehome.formatFlightCount
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** The first launch on a cold emulator fetches and stores all 1,000 flights, and loads the map style. */
const val LOAD_TIMEOUT = 60_000L
const val UI_TIMEOUT = 15_000L

/**
 * Launches [MainActivity] the way the iOS UI tests launch the app: with `resetData` unless asked not
 * to, so each test starts from an empty database, and optionally with saves or removes failing.
 * Closes it after each test.
 */
class ExplorerApp : ExternalResource() {
    private var scenario: ActivityScenario<MainActivity>? = null

    fun launch(resetData: Boolean = true, failSaves: Boolean = false, failRemoves: Boolean = false) {
        scenario?.close()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra(ExplorerViewModel.EXTRA_RESET_DATA, resetData)
            .putExtra(ExplorerViewModel.EXTRA_FAIL_SAVES, failSaves)
            .putExtra(ExplorerViewModel.EXTRA_FAIL_REMOVES, failRemoves)
        scenario = ActivityScenario.launch(intent)
    }

    /** Closes the app and opens it again on the same database. */
    fun relaunch() = launch(resetData = false)

    /** Destroys and recreates the activity, as a rotation does. */
    fun recreate() {
        checkNotNull(scenario) { "launch() first" }.recreate()
    }

    /** Closes the soft keyboard that typing opened, so the screen behind it shows. */
    fun hideKeyboard() = onActivity { activity ->
        activity.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
    }

    /** Sends the app to the background and brings it back. */
    fun backgroundAndReturn() {
        val running = checkNotNull(scenario) { "launch() first" }
        running.moveToState(Lifecycle.State.CREATED)
        running.moveToState(Lifecycle.State.RESUMED)
    }

    fun <T> onActivity(block: (MainActivity) -> T): T {
        var result: Result<T>? = null
        checkNotNull(scenario) { "launch() first" }.onActivity { result = runCatching { block(it) } }
        return checkNotNull(result).getOrThrow()
    }

    override fun after() {
        scenario?.close()
        scenario = null
    }
}

/** Switches the local API's mode; CI starts it with `--allow-control`, as ios.yml does. */
object ServerControl {
    fun setMode(mode: String) {
        val connection = URL("${BuildConfig.API_BASE_URL}/control/mode?value=$mode").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(0)
            connection.outputStream.close()
            check(connection.responseCode == 200) { "mode=$mode: HTTP ${connection.responseCode}" }
        } finally {
            connection.disconnect()
        }
    }
}

/** Puts the API back in normal mode after each test, even a failed one. */
class ServerModeReset : ExternalResource() {
    override fun after() {
        runCatching { ServerControl.setMode("normal") }
    }
}

private fun SemanticsNode.texts(): List<String> = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

private fun SemanticsNode.subtree(): Sequence<SemanticsNode> = sequenceOf(this) + children.asSequence().flatMap { it.subtree() }

fun ComposeTestRule.exists(tag: String): Boolean = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

/** Composed and within the visible bounds: a lazy list composes the next item before it scrolls in. */
fun ComposeTestRule.isOnScreen(matcher: SemanticsMatcher): Boolean =
    onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() && onNode(matcher).isDisplayed()

fun ComposeTestRule.waitForTag(tag: String, timeoutMillis: Long = UI_TIMEOUT) =
    waitUntil(timeoutMillis) { exists(tag) }

fun ComposeTestRule.waitForNoTag(tag: String, timeoutMillis: Long = UI_TIMEOUT) =
    waitUntil(timeoutMillis) { !exists(tag) }

/** Waits until the node tagged [tag] reads exactly [text]. */
fun ComposeTestRule.waitForText(tag: String, text: String, timeoutMillis: Long = UI_TIMEOUT) =
    waitUntil(timeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().any { text in it.texts() } }

/** Waits for the summary row to read [count] flights, formatted as the app does ("1,000 flights"). */
fun ComposeTestRule.waitForCount(count: Int, timeoutMillis: Long = UI_TIMEOUT) =
    waitForText(TestIds.resultCount, formatFlightCount(count), timeoutMillis)

/** Replaces a search field's text, as typing it would. */
fun ComposeTestRule.search(field: String, text: String) {
    onNodeWithTag(field).performTextReplacement(text)
}

/** Types into a search field a key at a time, so a recording shows it being typed. */
fun ComposeTestRule.typeSlowly(field: String, text: String) {
    onNodeWithTag(field).performTextReplacement("")
    text.forEach { key ->
        onNodeWithTag(field).performTextInput(key.toString())
        Thread.sleep(120)
    }
}

/** Slows a recorded test down enough to follow. */
fun pace(seconds: Double = 1.5) = Thread.sleep((seconds * 1000).toLong())

fun ComposeTestRule.fieldText(field: String): String =
    onNodeWithTag(field).fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()

fun hasTagStartingWith(prefix: String) = SemanticsMatcher("testTag starts with \"$prefix\"") {
    it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}

private val rowTagPrefix = TestIds.flightRow("")

/** Flight rows in list order; only those inside [container] when it's given. */
fun flightRows(container: String? = null): SemanticsMatcher {
    val row = hasTagStartingWith(rowTagPrefix)
    return if (container == null) row else row and hasAnyAncestor(hasTestTag(container))
}

/** Any filter chip, or its ×: tags start "filters.chip.". */
val anyFilterChip = hasTagStartingWith(TestIds.filterChip(FilterKind.ORIGIN).substringBeforeLast('.') + ".")

/** Waits for the chip of [kind], scrolling the summary row's chips, which only compose on screen. */
fun ComposeTestRule.waitForChip(kind: FilterKind) {
    val chips = hasScrollToIndexAction() and hasAnyDescendant(anyFilterChip)
    waitUntil(UI_TIMEOUT) { onAllNodes(chips).fetchSemanticsNodes().isNotEmpty() }
    onNode(chips).performScrollToNode(hasTestTag(TestIds.filterChip(kind)))
}

fun SemanticsNodeInteraction.flightId(): String =
    fetchSemanticsNode().config[SemanticsProperties.TestTag].removePrefix(rowTagPrefix)

/** What TalkBack reads for a row: route, cities, date, cabin, program, points and saved state. */
fun SemanticsNodeInteraction.spokenLabel(): String =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()

/**
 * Taps the first result row and waits for the card, then returns the flight's id. Call it only once
 * the count shows the latest search: the store filters asynchronously, so the previous rows can still
 * be on screen right after typing.
 */
fun ComposeTestRule.selectFirstRow(): String {
    val row = onAllNodes(flightRows()).onFirst()
    val id = row.flightId()
    row.performClick()
    waitForTag(TestIds.card)
    return id
}

val savedRows = hasTagStartingWith(TestIds.savedRow(""))

/** Searches JFK → HND (3 flights), selects the first and saves it; returns its id. */
fun ComposeTestRule.saveFirstJfkToHaneda(): String {
    search(TestIds.searchOrigin, "jfk")
    search(TestIds.searchDestination, "hnd")
    waitForCount(3)
    val id = selectFirstRow()
    onNodeWithTag(TestIds.cardSave).performClick()
    waitForTag(TestIds.cardRemove)
    return id
}

/** Pulls the result list down, as a finger does to refresh. */
fun ComposeTestRule.pullToRefresh() {
    onNode(hasScrollToIndexAction() and hasAnyDescendant(flightRows())).performTouchInput { swipeDown() }
}

/** Every text on the flight card, which together identify the selected flight. */
fun ComposeTestRule.cardTexts(): List<String> =
    onNodeWithTag(TestIds.card, useUnmergedTree = true).fetchSemanticsNode().subtree().flatMap { it.texts() }.toList()

fun ComposeTestRule.openFilters() {
    onNodeWithTag(TestIds.filtersOpen).performClick()
    waitForTag(TestIds.filtersDone)
}

fun ComposeTestRule.closeFilters() {
    onNodeWithTag(TestIds.filtersDone).performScrollTo().performClick()
    waitForNoTag(TestIds.filtersDone)
}

/** The filter sheet's Done button reads "Show N flights" and updates live. */
fun ComposeTestRule.expectDone(count: Int) = waitForText(TestIds.filtersDone, "Show ${formatFlightCount(count)}")

/** Taps a control in the filter sheet, scrolling to it first. */
fun ComposeTestRule.tapInSheet(tag: String) {
    onNodeWithTag(tag).performScrollTo().performClick()
}

/** Sets the points limit exactly, as a screen reader's adjust action does; a drag can't land on a step. */
fun ComposeTestRule.setMaxPoints(points: Int) {
    onNodeWithTag(TestIds.filterPoints).performScrollTo()
        .performSemanticsAction(SemanticsActions.SetProgress) { it(points.toFloat()) }
}

/**
 * Picks [day], written as the picker reads it ("February 1, 2027"), as the whole date range. A tap
 * starts a new range at that day, and Apply with no end day picked keeps just that day.
 */
fun ComposeTestRule.pickSingleDay(day: String) {
    tapInSheet(TestIds.filterDates)
    val apply = hasText("Apply") and hasClickAction()
    waitUntil(UI_TIMEOUT) { onAllNodes(apply).fetchSemanticsNodes().isNotEmpty() }
    val cell = (hasText(day, substring = true) or hasContentDescription(day, substring = true)) and hasClickAction()
    // The picker's month list reports no scroll range, so performScrollToNode gives up; go by month.
    val months = onNode(hasScrollToIndexAction() and hasAnyAncestor(isDialog()))
    val monthCount = months.fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
    var month = 0
    while (!isOnScreen(cell) && month < monthCount) months.performScrollToIndex(month++)
    onNode(cell).performClick()
    onNode(apply).performClick()
    waitUntil(UI_TIMEOUT) { onAllNodes(apply).fetchSemanticsNodes().isEmpty() }
}

/**
 * Taps a map pill: the one with [pillId] (a single airport's pill has the airport code), or any pill.
 * Pills are bitmaps that MapLibre draws, not Compose nodes, so the point comes from the map's own
 * projection, and the tap goes through the input system like a finger.
 */
fun ComposeTestRule.tapPill(app: ExplorerApp, pillId: String? = null, timeoutMillis: Long = LOAD_TIMEOUT) {
    var point: PointF? = null
    waitUntil(timeoutMillis) {
        point = app.pillOnScreen(pillId)
        point != null
    }
    val (x, y) = checkNotNull(point)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val down = SystemClock.uptimeMillis()
    instrumentation.sendPointerSync(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0))
    instrumentation.sendPointerSync(MotionEvent.obtain(down, down + 50, MotionEvent.ACTION_UP, x, y, 0))
}

private operator fun PointF.component1() = x
private operator fun PointF.component2() = y

/** Where MapLibre drew the pill, in screen pixels, once the style and the pills have rendered. */
private fun ExplorerApp.pillOnScreen(pillId: String?): PointF? = onActivity { activity ->
    val view = activity.window.decorView.findMapView() ?: return@onActivity null
    var point: PointF? = null
    view.getMapAsync { map ->
        val everything = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
        val pill = map.queryRenderedFeatures(everything, MARKER_LAYER)
            .firstOrNull { pillId == null || it.getStringProperty("pill") == pillId }
        val center = pill?.geometry() as? Point ?: return@getMapAsync
        val inView = map.projection.toScreenLocation(LatLng(center.latitude(), center.longitude()))
        val origin = IntArray(2).also(view::getLocationOnScreen)
        point = PointF(origin[0] + inView.x, origin[1] + inView.y)
    }
    point
}

private fun View.findMapView(): MapView? = when (this) {
    is MapView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findMapView() }
    else -> null
}

/**
 * Saves the screen when a test fails, to the app's external files, where the CI script pulls it
 * from. Put it inside the app rule (a higher `order`), so the activity is still on screen.
 */
class ScreenshotOnFailure : TestWatcher() {
    override fun failed(e: Throwable?, description: Description) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screen = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-test-failures").apply { mkdirs() }
        File(dir, "${description.testClass.simpleName}.${description.methodName}.png").outputStream().use {
            screen.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
