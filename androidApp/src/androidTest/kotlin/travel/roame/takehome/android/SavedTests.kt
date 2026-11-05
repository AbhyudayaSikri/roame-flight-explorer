package travel.roame.takehome.android

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds
import travel.roame.takehome.formatPoints

/** QA V2, V4, V5 and V6. V2 and V5 switch the API's mode; [serverMode] puts it back. */
@RunWith(AndroidJUnit4::class)
class SavedTests {
    @get:Rule(order = 0)
    val serverMode = ServerModeReset()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @get:Rule(order = 2)
    val app = ExplorerApp()

    @get:Rule(order = 3)
    val screenshots = ScreenshotOnFailure()

    private fun launchWithAllFlights(failSaves: Boolean = false, failRemoves: Boolean = false) {
        app.launch(failSaves = failSaves, failRemoves = failRemoves)
        compose.waitForCount(1000, LOAD_TIMEOUT)
    }

    private fun openSaved() = compose.onNodeWithTag(TestIds.tabSaved).performClick()

    private fun savedCount() = compose.onAllNodes(savedRows).fetchSemanticsNodes().size

    @Test
    fun removeWorksFromTheCardAndFromTheSavedTab() {
        launchWithAllFlights()
        val id = compose.saveFirstJfkToHaneda()
        compose.onNodeWithTag(TestIds.cardRemove).performClick()
        compose.waitForTag(TestIds.cardSave)
        openSaved()
        compose.waitForTag(TestIds.savedEmpty)

        compose.onNodeWithTag(TestIds.tabExplore).performClick()
        compose.onNodeWithTag(TestIds.cardSave).performClick()
        compose.waitForTag(TestIds.cardRemove)
        openSaved()
        compose.onNodeWithTag(TestIds.savedRemove(id)).performClick()
        compose.waitForTag(TestIds.savedEmpty)
    }

    @Test
    fun aFailedSaveRollsBackWithAMessage() {
        launchWithAllFlights(failSaves = true)
        compose.search(TestIds.searchOrigin, "jfk")
        compose.waitForCount(113)
        compose.selectFirstRow()
        compose.onNodeWithTag(TestIds.cardSave).performClick()
        compose.waitForTag(TestIds.message)
        compose.waitForTag(TestIds.cardSave)
        openSaved()
        compose.waitForTag(TestIds.savedEmpty)
    }

    @Test
    fun aFailedRemoveRollsBackWithAMessage() {
        launchWithAllFlights(failRemoves = true)
        compose.saveFirstJfkToHaneda()
        compose.onNodeWithTag(TestIds.cardRemove).performClick()
        compose.waitForTag(TestIds.message)
        compose.waitForTag(TestIds.cardRemove)
        openSaved()
        compose.waitUntil(UI_TIMEOUT) { savedCount() == 1 }
    }

    @Test
    fun savingAgainAfterAPriceChangeUpdatesWithoutDuplicates() {
        launchWithAllFlights()
        val id = compose.saveFirstJfkToHaneda()
        val points = compose.cardTexts().firstNotNullOf { Regex("^([0-9,]+) pts$").find(it)?.groupValues?.get(1) }
            .replace(",", "").toInt()
        ServerControl.setMode("updated")

        app.relaunch()
        compose.waitForCount(1000, LOAD_TIMEOUT)
        compose.search(TestIds.searchOrigin, "jfk")
        compose.search(TestIds.searchDestination, "hnd")
        compose.waitForCount(3)
        compose.onNodeWithTag(TestIds.flightRow(id)).performClick()
        compose.waitForTag(TestIds.cardUpdate, LOAD_TIMEOUT)
        compose.onNodeWithTag(TestIds.cardUpdate).performClick()
        compose.waitForTag(TestIds.cardRemove)
        compose.onNodeWithTag(TestIds.cardUpdate).assertDoesNotExist()

        openSaved()
        compose.waitUntil(UI_TIMEOUT) { savedCount() == 1 }
        val updated = formatPoints(points + 500).removeSuffix(" pts") + " points"
        val saved = compose.onNodeWithTag(TestIds.savedRow(id)).spokenLabel()
        assertTrue("Saved shows $updated: $saved", updated in saved)
        assertEquals(1, savedCount())
    }

    @Test
    fun aSavedFlightSurvivesAnEmptyResponse() {
        launchWithAllFlights()
        compose.saveFirstJfkToHaneda()
        ServerControl.setMode("empty")
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.waitForTag(TestIds.emptyState, LOAD_TIMEOUT)
        openSaved()
        compose.waitUntil(UI_TIMEOUT) { savedCount() == 1 }
    }
}
