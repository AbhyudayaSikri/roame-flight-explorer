package travel.roame.takehome.android

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/** QA M11: the map frames the airports a search leaves. */
@RunWith(AndroidJUnit4::class)
class MapTests {
    @get:Rule(order = 0)
    val compose = createEmptyComposeRule()

    @get:Rule(order = 1)
    val app = ExplorerApp()

    @get:Rule(order = 2)
    val screenshots = ScreenshotOnFailure()

    @Before
    fun launchWithAllFlights() {
        app.launch()
        compose.waitForCount(1000, LOAD_TIMEOUT)
    }

    @Test
    fun searchingTokyoFramesBothAirportsAsSeparatePills() {
        compose.onNodeWithTag(TestIds.viewMap).performClick()
        compose.waitUntil(LOAD_TIMEOUT) { app.pillsOnScreen().isNotEmpty() }
        assertFalse("The opening world view leaves Japan off the screen", app.pillsOnScreen().containsAll(tokyo))

        compose.search(TestIds.searchDestination, "tokyo")
        compose.waitForCount(110)
        compose.waitUntil(UI_TIMEOUT) { app.pillsOnScreen().containsAll(tokyo) }
    }

    private val tokyo = listOf("HND", "NRT")
}
