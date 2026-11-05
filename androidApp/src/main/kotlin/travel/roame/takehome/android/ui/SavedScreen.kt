package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import travel.roame.takehome.ExplorerState
import travel.roame.takehome.FlightExplorerStore
import travel.roame.takehome.SaveStatus
import travel.roame.takehome.TestIds

/** Saved snapshots, newest first. Independent of Explore's filters, and works offline. */
@Composable
fun SavedScreen(state: ExplorerState, store: FlightExplorerStore) {
    if (state.saved.isEmpty()) {
        EmptyView(
            title = "No saved flights yet",
            body = "Save a flight from Explore and it stays here, even offline.",
            testTag = TestIds.savedEmpty,
            onRefresh = store::refresh,
        )
        return
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (state.selectedSaved != null) 300.dp else Spacing.l),
        ) {
            items(state.saved, key = { it.flight.id }) { saved ->
                Column {
                    FlightRow(
                        flight = saved.flight,
                        saved = true,
                        selected = saved.flight.id == state.selectedSaved?.flight?.id,
                        onClick = { store.selectSaved(saved.flight.id) },
                        modifier = Modifier.testTag(TestIds.savedRow(saved.flight.id)),
                    )
                    Row(Modifier.fillMaxWidth().padding(end = Spacing.s), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = { store.remove(saved.flight.id) },
                            modifier = Modifier.testTag(TestIds.savedRemove(saved.flight.id)),
                        ) { Text("Remove") }
                    }
                    HorizontalDivider()
                }
            }
        }
        state.selectedSaved?.let { saved ->
            FlightCard(
                flight = saved.flight,
                status = SaveStatus.SAVED,
                onSave = {},
                onRemove = { store.remove(saved.flight.id) },
                onClose = { store.selectSaved(null) },
                modifier = Modifier.align(Alignment.BottomCenter),
                footnote = "Saved snapshot: details as they were when you saved it.",
            )
        }
    }
}
