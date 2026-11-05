package travel.roame.takehome.android

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.geojson.Point
import travel.roame.takehome.TestIds
import travel.roame.takehome.android.ui.MARKER_LAYER
import travel.roame.takehome.formatFlightCount
import java.io.File

/** The first launch on a cold emulator fetches and stores all 1,000 flights, and loads the map style. */
const val LOAD_TIMEOUT = 60_000L
const val UI_TIMEOUT = 15_000L

typealias ExplorerRule = AndroidComposeTestRule<*, MainActivity>

/** Waits for the summary row to read [count] flights, formatted as the app does ("1,000 flights"). */
fun ExplorerRule.waitForCount(count: Int, timeoutMillis: Long = UI_TIMEOUT) {
    val label = formatFlightCount(count)
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(TestIds.resultCount).fetchSemanticsNodes().any { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == label }
        }
    }
}

fun ExplorerRule.waitForTag(tag: String, timeoutMillis: Long = UI_TIMEOUT) {
    waitUntil(timeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
}

fun ExplorerRule.waitForNoTag(tag: String, timeoutMillis: Long = UI_TIMEOUT) {
    waitUntil(timeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
}

private val rowTagPrefix = TestIds.flightRow("")

fun hasTagStartingWith(prefix: String) = SemanticsMatcher("testTag starts with \"$prefix\"") {
    it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}

/** Flight rows in list order; only those inside [container] when it's given. */
fun flightRows(container: String? = null): SemanticsMatcher {
    val row = hasTagStartingWith(rowTagPrefix)
    return if (container == null) row else row and hasAnyAncestor(hasTestTag(container))
}

fun SemanticsNodeInteraction.flightId(): String =
    fetchSemanticsNode().config[SemanticsProperties.TestTag].removePrefix(rowTagPrefix)

/**
 * Taps the map pill with this id; a single airport's pill has the airport code. Pills are bitmaps
 * that MapLibre draws, not Compose nodes, so the point comes from the map's own projection, and the
 * tap goes through the input system like a finger.
 */
fun ExplorerRule.tapPill(pillId: String, timeoutMillis: Long = LOAD_TIMEOUT) {
    var point: PointF? = null
    waitUntil(timeoutMillis) {
        point = pillOnScreen(pillId)
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
private fun ExplorerRule.pillOnScreen(pillId: String): PointF? {
    var point: PointF? = null
    runOnUiThread {
        val view = activity.window.decorView.findMapView() ?: return@runOnUiThread
        view.getMapAsync { map ->
            val everything = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
            val pill = map.queryRenderedFeatures(everything, MARKER_LAYER).firstOrNull { it.getStringProperty("pill") == pillId }
            val center = pill?.geometry() as? Point ?: return@getMapAsync
            val inView = map.projection.toScreenLocation(LatLng(center.latitude(), center.longitude()))
            val origin = IntArray(2).also(view::getLocationOnScreen)
            point = PointF(origin[0] + inView.x, origin[1] + inView.y)
        }
    }
    return point
}

private fun View.findMapView(): MapView? = when (this) {
    is MapView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findMapView() }
    else -> null
}

/**
 * Saves the screen when a test fails, to the app's external files, where the CI script pulls it
 * from. Put it inside the compose rule (a higher `order`), so the activity is still on screen.
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
