package travel.roame.takehome.android

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.FilterKind
import travel.roame.takehome.TestIds

/**
 * QA F1–F8 and F10–F12. A UI test sets the date range and the points limit exactly here, so F1, F2
 * and F6 are checked end to end on Android; iOS covers them with the shared tests.
 */
@RunWith(AndroidJUnit4::class)
class FilterTests {
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
    fun aSingleDayRangeIncludesThatDay() {
        compose.openFilters()
        compose.pickSingleDay("February 1, 2027")
        compose.expectDone(17)
        compose.pickSingleDay("April 1, 2027")
        compose.expectDone(21)
        compose.closeFilters()
        compose.waitForCount(21)
        compose.waitForChip(FilterKind.DATES)
    }

    @Test
    fun theLowestPointsLimitLeavesTheSingleCheapestFlight() {
        compose.openFilters()
        compose.setMaxPoints(9_000)
        compose.expectDone(1)
        compose.closeFilters()
        compose.waitForCount(1)
        val flight = compose.onAllNodes(flightRows()).onFirst().spokenLabel()
        assertTrue(flight, flight.startsWith("LAX to MEX, Los Angeles to Mexico City"))
        assertTrue(flight, "9,000 points" in flight)
    }

    @Test
    fun cabinCountsUpdateLiveAndCombineWithAProgramAndAnExactLimit() {
        compose.openFilters()
        compose.tapInSheet(TestIds.cabinOption("Business"))
        compose.expectDone(344)
        compose.tapInSheet(TestIds.cabinOption("Economy"))
        compose.expectDone(467)

        compose.tapInSheet(TestIds.cabinOption("Business"))
        compose.tapInSheet(TestIds.programOption("Avios"))
        compose.setMaxPoints(60_000)
        compose.expectDone(32)
        compose.closeFilters()
        compose.waitForCount(32)
        listOf(FilterKind.CABIN, FilterKind.PROGRAMS, FilterKind.MAX_POINTS).forEach { compose.waitForChip(it) }
    }

    @Test
    fun programsCombineWithOrAndDeselectingRestoresAll() {
        compose.openFilters()
        compose.onNodeWithTag(TestIds.programOption("Mileage Plan")).performScrollTo()
        compose.onNodeWithTag(TestIds.programOption("MileagePlus")).performScrollTo()
        compose.tapInSheet(TestIds.programOption("Avios"))
        compose.tapInSheet(TestIds.programOption("Aeroplan"))
        compose.expectDone(386)

        compose.tapInSheet(TestIds.programOption("Avios"))
        compose.tapInSheet(TestIds.programOption("Aeroplan"))
        compose.expectDone(1000)
        compose.closeFilters()
        compose.waitForCount(1000)
    }

    @Test
    fun resetClearsSearchAndEveryFilter() {
        compose.search(TestIds.searchOrigin, "jfk")
        compose.waitForCount(113)
        compose.openFilters()
        compose.tapInSheet(TestIds.cabinOption("Business"))
        compose.setMaxPoints(60_000)
        compose.closeFilters()
        compose.waitForChip(FilterKind.ORIGIN)
        compose.waitForChip(FilterKind.CABIN)

        compose.onNodeWithTag(TestIds.filtersReset).performClick()
        compose.waitForCount(1000)
        assertEquals("", compose.fieldText(TestIds.searchOrigin))
        assertEquals(0, compose.onAllNodes(anyFilterChip).fetchSemanticsNodes().size)
        compose.onNodeWithTag(TestIds.filtersReset).assertDoesNotExist()
    }

    @Test
    fun eachChipRemovesOnlyItsOwnFilter() {
        compose.search(TestIds.searchOrigin, "jfk")
        compose.waitForCount(113)
        compose.openFilters()
        compose.tapInSheet(TestIds.cabinOption("Business"))
        compose.closeFilters()
        compose.waitForCount(35)

        compose.onNodeWithTag(TestIds.filterChipRemove(FilterKind.CABIN)).performClick()
        compose.waitForCount(113)
        compose.waitForNoTag(TestIds.filterChip(FilterKind.CABIN))
        assertEquals("The From field still says jfk", "jfk", compose.fieldText(TestIds.searchOrigin))

        compose.onNodeWithTag(TestIds.filterChipRemove(FilterKind.ORIGIN)).performClick()
        compose.waitForCount(1000)
        assertEquals("The From field is empty again", "", compose.fieldText(TestIds.searchOrigin))
        compose.onNodeWithTag(TestIds.filtersReset).assertDoesNotExist()
    }

    @Test
    fun minusAndPlusStepThePointsLimit() {
        compose.openFilters()
        compose.waitForText(TestIds.filterPointsValue, "No limit")
        compose.tapInSheet(TestIds.filterPointsDecrease)
        compose.waitForText(TestIds.filterPointsValue, "Up to 132,500 pts")
        compose.expectDone(999)

        compose.tapInSheet(TestIds.filterPointsIncrease)
        compose.waitForText(TestIds.filterPointsValue, "No limit")
        compose.expectDone(1000)

        compose.setMaxPoints(9_000)
        compose.waitForText(TestIds.filterPointsValue, "Up to 9,000 pts")
        compose.onNodeWithTag(TestIds.filterPointsDecrease).assertIsNotEnabled()
        compose.closeFilters()
    }
}
