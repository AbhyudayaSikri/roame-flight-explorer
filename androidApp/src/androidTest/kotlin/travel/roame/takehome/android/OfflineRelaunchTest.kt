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
 * QA O1–O3 with the API process stopped, in two runs that CI makes with `-e offline before` and, once
 * it has stopped the API, `-e offline after`. Without the argument both tests skip, so a plain run
 * of every class works.
 */
@RunWith(AndroidJUnit4::class)
class OfflineRelaunchTest {
    @get:Rule(order = 0)
    val compose = createEmptyComposeRule()

    @get:Rule(order = 1)
    val app = ExplorerApp()

    @get:Rule(order = 2)
    val screenshots = ScreenshotOnFailure()

    private val phase: String? = InstrumentationRegistry.getArguments().getString("offline")

    @Test
    fun beforeTheApiStopsCacheTheFlightsAndSaveOne() {
        assumeTrue(phase == "before")
        app.launch()
        compose.waitForCount(1000, LOAD_TIMEOUT)
        compose.search(TestIds.searchDestination, "dub")
        compose.waitForCount(29)
        compose.selectFirstRow()
        compose.onNodeWithTag(TestIds.cardSave).performClick()
        compose.waitForTag(TestIds.cardRemove)
    }

    @Test
    fun afterTheApiStopsARelaunchShowsTheCacheMarkedStaleAndTheSavedFlight() {
        assumeTrue(phase == "after")
        app.launch(resetData = false)
        compose.waitForTag(TestIds.staleBanner, LOAD_TIMEOUT)
        compose.waitForCount(1000)

        // O1: pulling to refresh with the API down keeps the cache, still marked stale.
        compose.pullToRefresh()
        compose.waitForTag(TestIds.staleBanner)
        compose.waitForCount(1000)

        // O3: search the cache offline, then remove the saved flight offline.
        compose.search(TestIds.searchDestination, "dub")
        compose.waitForCount(29)
        compose.onNodeWithTag(TestIds.tabSaved).performClick()
        compose.waitUntil(UI_TIMEOUT) { compose.onAllNodes(savedRows).fetchSemanticsNodes().size == 1 }
        compose.onAllNodes(hasTagStartingWith(TestIds.savedRemove(""))).onFirst().performClick()
        compose.waitForTag(TestIds.savedEmpty)
    }
}
