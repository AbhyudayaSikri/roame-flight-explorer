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
 * The Android demo video, like iOS's DemoWalkthrough. CI records the emulator while this runs, and
 * while `OfflineRelaunchTest` follows it with the API stopped; it also leaves the cache and the one
 * saved flight that run relies on. Runs only with `-e demo true`, because it's slow on purpose.
 */
@RunWith(AndroidJUnit4::class)
class DemoWalkthrough {
    @get:Rule(order = 0)
    val serverMode = ServerModeReset()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @get:Rule(order = 2)
    val app = ExplorerApp()

    @get:Rule(order = 3)
    val screenshots = ScreenshotOnFailure()

    @Test
    fun searchFilterMapPillCardSaveAndSaved() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("demo") == "true")
        // A 3 s first load, so the video shows the skeleton rows.
        ServerControl.setMode("slow")
        app.launch()
        compose.waitForTag(TestIds.loading)
        compose.waitForCount(1000, LOAD_TIMEOUT)
        ServerControl.setMode("normal")
        pace()

        compose.typeSlowly(TestIds.searchOrigin, "new york")
        compose.waitForCount(113)
        app.hideKeyboard()
        pace()
        compose.typeSlowly(TestIds.searchDestination, "tokyo")
        compose.waitForCount(7)
        app.hideKeyboard()
        pace()

        compose.openFilters()
        pace()
        compose.tapInSheet(TestIds.cabinOption("Business"))
        pace()
        compose.tapInSheet(TestIds.cabinOption(null))
        pace(1.0)
        compose.closeFilters()

        compose.onNodeWithTag(TestIds.viewMap).performClick()
        compose.waitUntil(LOAD_TIMEOUT) { app.pillsOnScreen().containsAll(listOf("HND", "NRT")) }
        pace(2.0)
        compose.tapPill(app, "NRT")
        compose.waitForTag(TestIds.destinationSheet)
        pace()
        compose.onAllNodes(flightRows(container = TestIds.destinationSheet)).onFirst().performClick()
        compose.waitForTag(TestIds.card)
        pace(2.0)

        compose.onNodeWithTag(TestIds.cardSave).performClick()
        compose.waitForTag(TestIds.cardRemove)
        pace()
        compose.onNodeWithTag(TestIds.tabSaved).performClick()
        compose.waitUntil(UI_TIMEOUT) { compose.onAllNodes(savedRows).fetchSemanticsNodes().size == 1 }
        pace(2.0)
    }
}
