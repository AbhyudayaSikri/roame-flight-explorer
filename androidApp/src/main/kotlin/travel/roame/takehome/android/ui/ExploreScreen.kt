@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import travel.roame.takehome.ExplorerState
import travel.roame.takehome.FlightExplorerStore
import travel.roame.takehome.SyncState
import travel.roame.takehome.formatFlightCount

private enum class ViewMode { LIST, MAP }

@Composable
fun ExploreScreen(state: ExplorerState, store: FlightExplorerStore) {
    var mode by rememberSaveable { mutableStateOf(ViewMode.LIST) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    // The fields own their text so typing never waits on the store; Reset clears both.
    var origin by rememberSaveable { mutableStateOf(state.filters.originText) }
    var destination by rememberSaveable { mutableStateOf(state.filters.destinationText) }
    val reset = {
        origin = ""
        destination = ""
        store.resetFilters()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            SearchField("From", "City or airport", origin, Modifier.weight(1f)) {
                origin = it
                store.setOriginText(it)
            }
            SearchField("To", "City or airport", destination, Modifier.weight(1f)) {
                destination = it
                store.setDestinationText(it)
            }
        }
        SummaryRow(state, onOpenFilters = { showFilters = true }, onReset = reset)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.xs)) {
            ViewMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = mode == item,
                    onClick = { mode = item },
                    shape = SegmentedButtonDefaults.itemShape(index, ViewMode.entries.size),
                ) { Text(if (item == ViewMode.LIST) "List" else "Map") }
            }
        }
        if (state.isRefreshing && state.totalCount > 0) LinearProgressIndicator(Modifier.fillMaxWidth())
        (state.sync as? SyncState.Stale)?.let { StaleBanner(it) }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val sync = state.sync
            when {
                sync is SyncState.Loading && state.totalCount == 0 -> LoadingView()
                sync is SyncState.FailedNoCache -> ErrorView(sync.error.userMessage, onRetry = store::refresh)
                state.totalCount == 0 -> EmptyView(
                    title = "No flights available",
                    body = "The server has no flights right now. Pull down or tap refresh to check again.",
                    onRefresh = store::refresh,
                )
                state.resultCount == 0 -> NoMatchesView(onReset = reset)
                mode == ViewMode.LIST -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = store::refresh) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = if (state.selected != null) 280.dp else Spacing.l),
                    ) {
                        items(state.results, key = { it.id }) { flight ->
                            FlightRow(
                                flight = flight,
                                saved = flight.id in state.savedIds,
                                selected = flight.id == state.selected?.id,
                                onClick = { store.select(flight.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
                else -> FlightMap(
                    markers = state.markers,
                    route = state.selectedRoute,
                    onMarkerTap = store::focusDestination,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.selected?.let { flight ->
                FlightCard(
                    flight = flight,
                    status = state.selectedSaveStatus,
                    onSave = { store.save(flight.id) },
                    onRemove = { store.remove(flight.id) },
                    onClose = { store.select(null) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    val bounds = state.filters.bounds
    if (showFilters && bounds != null) {
        FilterSheet(
            filters = state.filters,
            bounds = bounds,
            resultCount = state.resultCount,
            store = store,
            onReset = reset,
            onDismiss = { showFilters = false },
        )
    }
    state.focusedDestination?.let { marker ->
        DestinationSheet(
            marker = marker,
            flights = state.focusedFlights,
            savedIds = state.savedIds,
            onPick = { id ->
                store.select(id)
                store.focusDestination(null)
            },
            onDismiss = { store.focusDestination(null) },
        )
    }
}

@Composable
private fun SearchField(label: String, placeholder: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Clear, contentDescription = "Clear $label") }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun SummaryRow(state: ExplorerState, onOpenFilters: () -> Unit, onReset: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = Spacing.l, end = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatFlightCount(state.resultCount),
            style = MaterialTheme.typography.titleSmall,
        )
        LazyRow(
            Modifier.weight(1f).padding(horizontal = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            items(state.activeFilters, key = { it.kind.name }) { filter ->
                AssistChip(onClick = onOpenFilters, label = { Text(filter.label, maxLines = 1) })
            }
        }
        if (state.activeFilters.isNotEmpty()) TextButton(onClick = onReset) { Text("Reset") }
        IconButton(onClick = onOpenFilters, enabled = state.filters.bounds != null) {
            Icon(Icons.Default.Settings, contentDescription = "Filters")
        }
    }
}
