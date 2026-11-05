@file:OptIn(ExperimentalMaterial3Api::class)

package travel.roame.takehome.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import travel.roame.takehome.ExplorerState
import travel.roame.takehome.TestIds
import travel.roame.takehome.android.BuildConfig
import travel.roame.takehome.android.ExplorerViewModel

private enum class Tab { EXPLORE, SAVED }

@Composable
fun FlightExplorerApp(viewModel: ExplorerViewModel = viewModel()) {
    val store = viewModel.store
    val state by store.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.EXPLORE) }
    val snackbar = remember { SnackbarHostState() }

    val message = state.message
    LaunchedEffect(message?.id) {
        if (message != null) {
            snackbar.showSnackbar(message.text, withDismissAction = true)
            store.messageShown(message.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == Tab.EXPLORE) "Explore flights" else "Saved flights") },
                actions = {
                    IconButton(onClick = store::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                    if (BuildConfig.DEBUG) DebugMenu(viewModel, state)
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.EXPLORE,
                    onClick = { tab = Tab.EXPLORE },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Explore") },
                )
                NavigationBarItem(
                    selected = tab == Tab.SAVED,
                    onClick = { tab = Tab.SAVED },
                    icon = {
                        BadgedBox(badge = { if (state.saved.isNotEmpty()) Badge { Text("${state.saved.size}") } }) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                        }
                    },
                    label = { Text("Saved") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.EXPLORE -> ExploreScreen(state, store)
                Tab.SAVED -> SavedScreen(state, store)
            }
        }
    }
}

@Composable
private fun DebugMenu(viewModel: ExplorerViewModel, state: ExplorerState) {
    var open by remember { mutableStateOf(false) }
    var editingServerUrl by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Debug options") }
    if (editingServerUrl) ServerUrlDialog(state, viewModel.store, onDismiss = { editingServerUrl = false })
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Server URL…") },
            onClick = {
                open = false
                editingServerUrl = true
            },
            modifier = Modifier.testTag(TestIds.debugOpen),
        )
        DropdownMenuItem(
            text = { Text("Fail saves") },
            onClick = { viewModel.setFaults(!viewModel.failSaves, viewModel.failRemoves) },
            trailingIcon = { Checkbox(checked = viewModel.failSaves, onCheckedChange = null) },
        )
        DropdownMenuItem(
            text = { Text("Fail removes") },
            onClick = { viewModel.setFaults(viewModel.failSaves, !viewModel.failRemoves) },
            trailingIcon = { Checkbox(checked = viewModel.failRemoves, onCheckedChange = null) },
        )
    }
}
