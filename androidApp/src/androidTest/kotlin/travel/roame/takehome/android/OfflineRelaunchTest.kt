package travel.roame.takehome.android

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/**
 * QA O1–O3 with the API process stopped. CI runs it with `-e offline true` after [DemoWalkthrough]
 * has filled the cache and saved a flight, and the API has been stopped; the recording of both is
 * `android-demo.mp4`. Without the argument it skips, so a plain run of every class works.
 */
@RunWith(AndroidJUnit4::class)
class OfflineRelaunchTest {
    @get:Rule(order = 0)
    val compose = createEmptyComposeRule()

    @get:Rule(order = 1)
    val app = ExplorerApp()

    @get:Rule(order = 2)
    val screenshots = ScreenshotOnFailure()

    @Test
    fun aRelaunchWithTheApiStoppedShowsTheCacheMarkedStaleAndTheSavedFlight() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("offline") == "true")
        app.launch(resetData = false)
        compose.waitForTag(TestIds.staleBanner, LOAD_TIMEOUT)
        compose.waitForCount(1000)
        pace(2.0)

        // O1: pulling to refresh with the API down keeps the cache, still marked stale.
        compose.pullToRefresh()
        compose.waitForTag(TestIds.staleBanner)
        compose.waitForCount(1000)

        // O3: search the cache offline, then remove the saved flight offline.
        compose.typeSlowly(TestIds.searchOrigin, "jfk")
        compose.waitForCount(113)
        app.hideKeyboard()
        pace()
        compose.onNodeWithTag(TestIds.tabSaved).performClick()
        compose.waitUntil(UI_TIMEOUT) { compose.onAllNodes(savedRows).fetchSemanticsNodes().size == 1 }
        pace()
        compose.onAllNodes(hasTagStartingWith(TestIds.savedRemove(""))).onFirst().performClick()
        compose.waitForTag(TestIds.savedEmpty)
        pace()
    }
}
