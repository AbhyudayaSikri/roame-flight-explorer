package travel.roame.takehome.android

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/**
 * Launch, search, map selection and save, against the real local API (`10.0.2.2:8080` from the
 * emulator), each from a fresh install. QA L1, L2, S3, M3, V1.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
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
    fun launchLoadsAllFlightsWithNoFiltersActive() {
        compose.onNodeWithTag(TestIds.staleBanner).assertDoesNotExist()
        assertFalse("No filter chips", compose.onAllNodes(anyFilterChip).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun searchingTokyoFindsBothAirports() {
        compose.search(TestIds.searchDestination, "tokyo")
        compose.waitForCount(110)
    }

    @Test
    fun aFlightPickedFromAMapPillShowsItsCard() {
        compose.search(TestIds.searchDestination, "dub")
        compose.waitForCount(29)
        compose.onNodeWithTag(TestIds.viewMap).performClick()

        compose.tapPill(app, "DUB")
        compose.waitForTag(TestIds.destinationSheet)
        compose.onAllNodes(flightRows(container = TestIds.destinationSheet)).onFirst().performClick()

        compose.waitForTag(TestIds.card)
        compose.waitForNoTag(TestIds.destinationSheet)
        compose.onNodeWithTag(TestIds.card).assert(hasAnyDescendant(hasText("→ DUB", substring = true)))
    }

    @Test
    fun aSavedFlightAppearsInSaved() {
        compose.search(TestIds.searchDestination, "dub")
        compose.waitForCount(29)
        val id = compose.selectFirstRow()

        compose.onNodeWithTag(TestIds.cardSave).performClick()
        compose.waitForTag(TestIds.cardRemove)
        compose.onNodeWithTag(TestIds.tabSaved).performClick()
        compose.waitForTag(TestIds.savedRow(id))
    }
}
