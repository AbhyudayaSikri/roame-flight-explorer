package travel.roame.takehome.android

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import travel.roame.takehome.TestIds

/**
 * Launch, search, map selection and save, against the real local API (`10.0.2.2:8080` from the
 * emulator). CI then stops the API and runs [OfflineRelaunchTest] on the cache these leave.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule(order = 0)
    val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 1)
    val screenshots = ScreenshotOnFailure()

    @Test
    fun launchLoadsAllFlights() {
        rule.waitForCount(1000, LOAD_TIMEOUT)
        rule.onNodeWithTag(TestIds.staleBanner).assertDoesNotExist()
    }

    @Test
    fun searchingTokyoFindsBothAirports() {
        rule.waitForCount(1000, LOAD_TIMEOUT)
        rule.onNodeWithTag(TestIds.searchDestination).performTextInput("tokyo")
        rule.waitForCount(110)
    }

    @Test
    fun aFlightPickedFromAMapPillShowsItsCard() {
        rule.waitForCount(1000, LOAD_TIMEOUT)
        rule.onNodeWithTag(TestIds.searchDestination).performTextInput("dub")
        rule.waitForCount(29)
        rule.onNodeWithTag(TestIds.viewMap).performClick()

        rule.tapPill("DUB")
        rule.waitForTag(TestIds.destinationSheet)
        rule.onAllNodes(flightRows(container = TestIds.destinationSheet)).onFirst().performClick()

        rule.waitForTag(TestIds.card)
        rule.waitForNoTag(TestIds.destinationSheet)
        rule.onNodeWithTag(TestIds.card).assert(hasAnyDescendant(hasText("→ DUB", substring = true)))
    }

    @Test
    fun aSavedFlightAppearsInSaved() {
        rule.waitForCount(1000, LOAD_TIMEOUT)
        rule.onNodeWithTag(TestIds.searchDestination).performTextInput("dub")
        rule.waitForCount(29)
        val row = rule.onAllNodes(flightRows()).onFirst()
        val id = row.flightId()
        row.performClick()
        rule.waitForTag(TestIds.card)
        // A rerun on the same emulator finds it saved already.
        if (rule.onAllNodesWithTag(TestIds.cardRemove).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag(TestIds.cardRemove).performClick()
            rule.waitForTag(TestIds.cardSave)
        }

        rule.onNodeWithTag(TestIds.cardSave).performClick()
        rule.waitForTag(TestIds.cardRemove)
        rule.onNodeWithTag(TestIds.tabSaved).performClick()
        rule.waitForTag(TestIds.savedRow(id))
    }
}
