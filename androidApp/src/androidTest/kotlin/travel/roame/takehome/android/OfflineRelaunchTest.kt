package travel.roame.takehome.android

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/**
 * Runs only with `-e offline true`, which CI passes after [SmokeTest] has filled the cache, saved a
 * flight, and the API has been stopped. The relaunch must show that cache, marked stale, and the
 * saved flight (QA O2).
 */
@RunWith(AndroidJUnit4::class)
class OfflineRelaunchTest {
    @get:Rule(order = 0)
    val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 1)
    val screenshots = ScreenshotOnFailure()

    @Before
    fun onlyWithTheApiStopped() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("offline") == "true")
    }

    @Test
    fun aRelaunchWithTheApiStoppedShowsTheCacheMarkedStaleAndTheSavedFlight() {
        rule.waitForTag(TestIds.staleBanner, LOAD_TIMEOUT)
        rule.waitForCount(1000)

        rule.onNodeWithTag(TestIds.tabSaved).performClick()
        rule.waitUntil(UI_TIMEOUT) {
            rule.onAllNodes(hasTagStartingWith(TestIds.savedRow(""))).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
