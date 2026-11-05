package travel.roame.takehome.android.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import travel.roame.takehome.RefreshError
import travel.roame.takehome.SyncState
import travel.roame.takehome.TestIds
import java.util.Date

@Composable
fun LoadingView() {
    // The first load: grey rows in the shape of the list, so the layout doesn't jump when the
    // flights arrive.
    Column(
        Modifier
            .fillMaxSize()
            .testTag(TestIds.loading)
            .clearAndSetSemantics { contentDescription = "Loading flights" },
    ) {
        repeat(8) {
            PlaceholderRow()
            HorizontalDivider()
        }
    }
}

/** `FlightRow`'s shape: route, cities and details on the left, points on the right. */
@Composable
private fun PlaceholderRow() {
    val shade = MaterialTheme.colorScheme.surfaceVariant
    @Composable
    fun bar(width: Int, height: Int) =
        Box(Modifier.size(width.dp, height.dp).background(shade, RoundedCornerShape(4.dp)))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            bar(width = 110, height = 16)
            bar(width = 170, height = 12)
            bar(width = 210, height = 10)
        }
        bar(width = 64, height = 16)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    CenteredMessage {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Text("Couldn't load flights", style = MaterialTheme.typography.titleMedium)
        Text(
            "$message Check that the local API is running, then try again.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry, modifier = Modifier.testTag(TestIds.errorRetry)) { Text("Retry") }
    }
}

@Composable
fun EmptyView(title: String, body: String, testTag: String, onRefresh: () -> Unit) {
    CenteredMessage(Modifier.testTag(testTag)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRefresh) { Text("Refresh") }
    }
}

@Composable
fun NoMatchesView(onReset: () -> Unit) {
    CenteredMessage(Modifier.testTag(TestIds.noMatches)) {
        Text("No flights match these filters", style = MaterialTheme.typography.titleMedium)
        Text(
            "Try a different city or airport, or widen the dates and points.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onReset) { Text("Reset filters") }
    }
}

/** Shown while the last refresh failed but cached flights are still on screen. */
@Composable
fun StaleBanner(sync: SyncState.Stale) {
    val time = DateFormat.getTimeFormat(LocalContext.current).format(Date(sync.lastSuccessAtMillis))
    val reason = if (sync.error == RefreshError.Offline) "Offline" else "Couldn't refresh"
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = Spacing.l, vertical = Spacing.s)
            .testTag(TestIds.staleBanner),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        Text("$reason · showing results from $time", color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
