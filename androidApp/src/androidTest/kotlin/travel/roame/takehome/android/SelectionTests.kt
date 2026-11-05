package travel.roame.takehome.android

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/** QA M5, M6 and O7: the selection across List and Map, filters and the activity's lifecycle. */
@RunWith(AndroidJUnit4::class)
class SelectionTests {
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
    fun aFlightSelectedOnTheListStaysSelectedOnTheMapAndBack() {
        compose.search(TestIds.searchOrigin, "sfo")
        compose.search(TestIds.searchDestination, "nrt")
        compose.waitForCount(6)
        compose.selectFirstRow()
        // All six are SFO → NRT, so compare the whole card: date, cabin, points and program.
        val selected = compose.cardTexts()

        compose.onNodeWithTag(TestIds.viewMap).performClick()
        compose.waitForTag(TestIds.card)
        assertEquals("The map shows the flight selected on the list", selected, compose.cardTexts())
        compose.onNodeWithTag(TestIds.viewList).performClick()
        compose.waitForTag(TestIds.card)
        assertEquals("Back on the list, the same flight is selected", selected, compose.cardTexts())
    }

    @Test
    fun aFlightPickedOnTheMapStaysSelectedOnTheList() {
        compose.search(TestIds.searchDestination, "dub")
        compose.waitForCount(29)
        compose.onNodeWithTag(TestIds.viewMap).performClick()
        compose.tapPill(app, "DUB")
        compose.waitForTag(TestIds.destinationSheet)
        compose.onAllNodes(flightRows(container = TestIds.destinationSheet)).onFirst().performClick()
        compose.waitForTag(TestIds.card)
        compose.waitForNoTag(TestIds.destinationSheet)
        val selected = compose.cardTexts()

        compose.onNodeWithTag(TestIds.viewList).performClick()
        compose.waitForTag(TestIds.card)
        assertEquals("The list shows the flight picked on the map", selected, compose.cardTexts())
    }

    @Test
    fun filteringOutTheSelectionClearsItOnBothViews() {
        compose.search(TestIds.searchOrigin, "jfk")
        compose.search(TestIds.searchDestination, "hnd")
        compose.waitForCount(3)
        compose.selectFirstRow()

        compose.search(TestIds.searchDestination, "paris")
        compose.waitForCount(8)
        compose.waitForNoTag(TestIds.card)
        compose.search(TestIds.searchDestination, "")
        compose.waitForCount(113)
        compose.onNodeWithTag(TestIds.card).assertDoesNotExist()
        compose.onNodeWithTag(TestIds.viewMap).performClick()
        compose.onNodeWithTag(TestIds.card).assertDoesNotExist()
    }

    @Test
    fun rotatingAndBackgroundingKeepTheSearchAndTheSelection() {
        compose.search(TestIds.searchOrigin, "jfk")
        compose.waitForCount(113)
        compose.selectFirstRow()
        val selected = compose.cardTexts()

        app.recreate()
        compose.waitForTag(TestIds.card)
        assertEquals("jfk", compose.fieldText(TestIds.searchOrigin))
        compose.waitForCount(113)
        assertEquals(selected, compose.cardTexts())

        app.backgroundAndReturn()
        compose.waitForTag(TestIds.card)
        assertEquals("jfk", compose.fieldText(TestIds.searchOrigin))
        assertEquals(selected, compose.cardTexts())
    }
}
