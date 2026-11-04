package travel.roame.takehome.android.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import travel.roame.takehome.RefreshError
import travel.roame.takehome.SyncState
import java.util.Date

@Composable
fun LoadingView() {
    CenteredMessage {
        CircularProgressIndicator()
        Text("Loading flights…", style = MaterialTheme.typography.bodyLarge)
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
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptyView(title: String, body: String, onRefresh: () -> Unit) {
    CenteredMessage {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRefresh) { Text("Refresh") }
    }
}

@Composable
fun NoMatchesView(onReset: () -> Unit) {
    CenteredMessage {
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
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiary).padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        Text("$reason · showing results from $time", color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
