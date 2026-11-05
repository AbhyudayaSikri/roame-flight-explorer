package travel.roame.takehome.android

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/**
 * QA O1, O4 and O5, with the API's mode switched from the test, as iOS's `ErrorStateTests` do.
 * `OfflineRelaunchTest` covers the same with the API process stopped.
 */
@RunWith(AndroidJUnit4::class)
class ServerStateTests {
    @get:Rule(order = 0)
    val serverMode = ServerModeReset()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @get:Rule(order = 2)
    val app = ExplorerApp()

    @get:Rule(order = 3)
    val screenshots = ScreenshotOnFailure()

    @Test
    fun aFailedRefreshKeepsTheCacheAndMarksItStale() {
        app.launch()
        compose.waitForCount(1000, LOAD_TIMEOUT)
        ServerControl.setMode("error")
        compose.pullToRefresh()
        compose.waitForTag(TestIds.staleBanner, LOAD_TIMEOUT)
        compose.waitForCount(1000)
    }

    @Test
    fun withNoCacheAnErrorOffersRetryThatLoadsOnceTheServerRecovers() {
        ServerControl.setMode("error")
        app.launch()
        compose.waitForTag(TestIds.errorRetry, LOAD_TIMEOUT)
        ServerControl.setMode("normal")
        compose.onNodeWithTag(TestIds.errorRetry).performClick()
        compose.waitForCount(1000, LOAD_TIMEOUT)
    }

    @Test
    fun anEmptyResponseOnAFreshInstallSaysNoFlightsAvailable() {
        ServerControl.setMode("empty")
        app.launch()
        compose.waitForTag(TestIds.emptyState, LOAD_TIMEOUT)
        compose.onNodeWithTag(TestIds.errorRetry).assertDoesNotExist()
    }
}
