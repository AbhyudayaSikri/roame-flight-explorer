package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import travel.roame.takehome.ExplorerState
import travel.roame.takehome.FlightExplorerStore
import travel.roame.takehome.TestIds
import travel.roame.takehome.serverUrlProblem

/** Debug menu: point the app at another flights server, e.g. a computer on the same Wi-Fi. */
@Composable
fun ServerUrlDialog(state: ExplorerState, store: FlightExplorerStore, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(state.serverUrl) }
    val problem = serverUrlProblem(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server URL") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = problem != null,
                    label = { Text("Flights server") },
                    supportingText = { Text(problem ?: "The build's default is ${state.defaultServerUrl}.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag(TestIds.debugServerUrl),
                )
                Text(
                    "Use the computer's address on this network, such as http://192.168.1.20:8080. Saving fetches the flights again.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    store.setServerUrl(text)
                    onDismiss()
                },
                enabled = problem == null,
                modifier = Modifier.testTag(TestIds.debugServerUrlSave),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        store.useDefaultServerUrl()
                        onDismiss()
                    },
                    enabled = !state.isDefaultServerUrl,
                    modifier = Modifier.testTag(TestIds.debugServerUrlDefault),
                ) { Text("Use default") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
