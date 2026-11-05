@file:OptIn(ExperimentalMaterial3Api::class)

package travel.roame.takehome.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import travel.roame.takehome.DestinationMarker
import travel.roame.takehome.Flight
import travel.roame.takehome.SaveStatus
import travel.roame.takehome.TestIds
import travel.roame.takehome.formatPoints
import travel.roame.takehome.spokenFlight
import travel.roame.takehome.spokenRoute

@Composable
fun FlightRow(flight: Flight, saved: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        // TalkBack reads this instead of the row's texts, so it hears words, not "→" and "pts".
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = spokenFlight(flight, saved) },
        colors = if (selected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        headlineContent = {
            Text("${flight.origin} → ${flight.destination}", fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Column {
                Text("${flight.originCity} → ${flight.destinationCity}")
                Text(
                    "${flight.dateLabel} · ${flight.cabin} · ${flight.program}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(flight.pointsLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (saved) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Saved",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
    )
}

/** The selected flight: every field the brief lists, plus the save action for its current status. */
@Composable
fun FlightCard(
    flight: Flight,
    status: SaveStatus,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    ElevatedCard(modifier.fillMaxWidth().padding(Spacing.m).testTag(TestIds.card)) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${flight.origin} → ${flight.destination}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { contentDescription = spokenRoute(flight) },
                    )
                    Text("${flight.originCity} to ${flight.destinationCity}", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onClose, modifier = Modifier.testTag(TestIds.cardClose)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            HorizontalDivider()
            Detail("Departs", flight.dateLabel)
            Detail("Cabin", flight.cabin)
            Detail("Points", flight.pointsLabel)
            Detail("Program", flight.program)
            footnote?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SaveActions(status, onSave, onRemove)
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SaveActions(status: SaveStatus, onSave: () -> Unit, onRemove: () -> Unit) {
    val wide = Modifier.fillMaxWidth()
    // The screen updates at once (optimistic), so the feedback comes with the tap.
    val haptics = LocalHapticFeedback.current
    val save = {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onSave()
    }
    val remove = {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        onRemove()
    }
    when (status) {
        SaveStatus.NOT_SAVED -> Button(onClick = save, modifier = wide.testTag(TestIds.cardSave)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null)
            Text("Save", Modifier.padding(start = Spacing.s))
        }
        SaveStatus.SAVED -> OutlinedButton(onClick = remove, modifier = wide.testTag(TestIds.cardRemove)) {
            Icon(Icons.Default.Favorite, contentDescription = null)
            Text("Saved · Remove", Modifier.padding(start = Spacing.s))
        }
        SaveStatus.OUTDATED -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(onClick = save, modifier = Modifier.weight(1f).testTag(TestIds.cardUpdate)) { Text("Update saved") }
            OutlinedButton(onClick = remove, modifier = Modifier.weight(1f).testTag(TestIds.cardRemove)) { Text("Remove") }
        }
        SaveStatus.IN_PROGRESS -> Button(onClick = {}, enabled = false, modifier = wide) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Updating…", Modifier.padding(start = Spacing.s))
        }
    }
}

/** A tapped map marker: that destination's flights, tap one to select it. */
@Composable
fun DestinationSheet(
    marker: DestinationMarker,
    flights: List<Flight>,
    savedIds: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestIds.destinationSheet).testTagsAsResourceIds(),
    ) {
        Column(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.s)) {
            Text("${marker.city} (${marker.airport})", style = MaterialTheme.typography.titleLarge)
            Text(
                "${marker.count} flights · from ${formatPoints(marker.lowestPoints)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn {
            items(flights, key = { it.id }) { flight ->
                FlightRow(
                    flight,
                    saved = flight.id in savedIds,
                    selected = false,
                    onClick = { onPick(flight.id) },
                    modifier = Modifier.testTag(TestIds.flightRow(flight.id)),
                )
                HorizontalDivider()
            }
        }
    }
}
