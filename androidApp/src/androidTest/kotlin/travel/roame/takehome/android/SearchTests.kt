package travel.roame.takehome.android

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/** QA S1, S2, S4, S5 and S6; S3 is in [SmokeTest]. */
@RunWith(AndroidJUnit4::class)
class SearchTests {
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
    fun originIgnoresCaseAndSpacesMatchesTheCityAndCombinesWithTheDestination() {
        compose.search(TestIds.searchOrigin, "  JFK ")
        compose.waitForCount(113)
        compose.search(TestIds.searchOrigin, "new york")
        compose.waitForCount(113)
        compose.search(TestIds.searchDestination, "tokyo")
        compose.waitForCount(7)
    }

    @Test
    fun anExactCodeWinsOverCityPrefixes() {
        compose.search(TestIds.searchDestination, "dub")
        compose.waitForCount(29)
        compose.search(TestIds.searchDestination, "du")
        compose.waitForCount(69)

        compose.search(TestIds.searchDestination, "")
        compose.search(TestIds.searchOrigin, "fra")
        compose.waitForTag(TestIds.noMatches)
        compose.search(TestIds.searchOrigin, "fran")
        compose.waitForCount(123)
    }

    @Test
    fun noMatchesIsAnEmptyStateWithResetNotAnError() {
        compose.search(TestIds.searchOrigin, "xyz")
        compose.waitForTag(TestIds.noMatches)
        compose.onNodeWithTag(TestIds.errorRetry).assertDoesNotExist()

        compose.onNode(hasText("Reset filters") and hasClickAction() and hasAnyAncestor(hasTestTag(TestIds.noMatches))).performClick()
        compose.waitForCount(1000)
        assertEquals("", compose.fieldText(TestIds.searchOrigin))
    }
}
