@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import travel.roame.takehome.CalendarDate
import travel.roame.takehome.DataBounds
import travel.roame.takehome.DateRange
import travel.roame.takehome.FiltersUi
import travel.roame.takehome.FlightExplorerStore
import travel.roame.takehome.formatDateRange
import travel.roame.takehome.formatFlightCount
import travel.roame.takehome.formatPoints
import kotlin.math.roundToInt

@Composable
fun FilterSheet(
    filters: FiltersUi,
    bounds: DataBounds,
    resultCount: Int,
    store: FlightExplorerStore,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDates by remember { mutableStateOf(false) }
    val range = filters.dateRange ?: bounds.dateRange

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)

            Section("Departure dates") {
                OutlinedButton(onClick = { showDates = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Text(formatDateRange(range), Modifier.padding(start = Spacing.s))
                }
            }

            Section("Cabin") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    FilterChip(selected = filters.cabin == null, onClick = { store.setCabin(null) }, label = { Text("Any") })
                    bounds.cabins.forEach { cabin ->
                        FilterChip(selected = filters.cabin == cabin, onClick = { store.setCabin(cabin) }, label = { Text(cabin) })
                    }
                }
            }

            Section("Maximum points") { PointsSlider(filters, bounds, store) }

            Section("Loyalty programs") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    bounds.programs.forEach { program ->
                        FilterChip(
                            selected = program in filters.programs,
                            onClick = { store.toggleProgram(program) },
                            label = { Text(program) },
                        )
                    }
                }
                Text(
                    if (filters.programs.isEmpty()) "Any program" else "Flights from any selected program",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onReset) { Text("Reset all") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onDismiss) { Text("Show ${formatFlightCount(resultCount)}") }
            }
        }
    }

    if (showDates) {
        DateRangeDialog(
            current = range,
            bounds = bounds,
            onConfirm = { start, end ->
                store.setDateRange(start, end)
                showDates = false
            },
            onDismiss = { showDates = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

/** Steps of 500 points, so every fixture price can be chosen exactly; the far right is "no limit". */
@Composable
private fun PointsSlider(filters: FiltersUi, bounds: DataBounds, store: FlightExplorerStore) {
    val min = bounds.minPoints
    val max = bounds.maxPoints
    var value by remember(filters.maxPoints, min, max) { mutableFloatStateOf(filters.maxPoints.coerceIn(min, max).toFloat()) }
    val points = value.roundToInt()

    Text(
        if (points >= max) "No limit" else "Up to ${formatPoints(points)}",
        style = MaterialTheme.typography.bodyLarge,
    )
    Slider(
        value = value,
        onValueChange = {
            value = it
            store.setMaxPoints(it.roundToInt())
        },
        valueRange = min.toFloat()..max.toFloat(),
        steps = ((max - min) / filters.pointsStep - 1).coerceAtLeast(0),
    )
}

@Composable
private fun DateRangeDialog(
    current: DateRange,
    bounds: DataBounds,
    onConfirm: (CalendarDate, CalendarDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // DateRangePicker works in UTC-midnight milliseconds; CalendarDate converts without time zones.
    val first = bounds.firstDate.toUtcMillis()
    val last = bounds.lastDate.toUtcMillis()
    val years = bounds.firstDate.year..bounds.lastDate.year
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = current.start.toUtcMillis(),
        initialSelectedEndDateMillis = current.end.toUtcMillis(),
        initialDisplayedMonthMillis = current.start.toUtcMillis(),
        yearRange = years,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in first..last
            override fun isSelectableYear(year: Int) = year in years
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = picker.selectedStartDateMillis != null,
                onClick = {
                    val start = picker.selectedStartDateMillis ?: return@TextButton
                    val end = picker.selectedEndDateMillis ?: start
                    onConfirm(CalendarDate.fromUtcMillis(start), CalendarDate.fromUtcMillis(end))
                },
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = picker,
            modifier = Modifier.weight(1f),
            title = { Text("Departure dates", Modifier.padding(start = Spacing.xl, top = Spacing.l)) },
        )
    }
}
