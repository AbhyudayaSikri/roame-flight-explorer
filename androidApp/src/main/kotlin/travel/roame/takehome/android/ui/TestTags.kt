package travel.roame.takehome.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Shows `testTag`s (the shared `TestIds`) to UiAutomator as resource ids, as in
 * `adb shell uiautomator dump`. Sheets, dialogs and menus are separate windows that don't inherit
 * it, so each sets it again.
 */
fun Modifier.testTagsAsResourceIds(): Modifier = semantics { testTagsAsResourceId = true }
