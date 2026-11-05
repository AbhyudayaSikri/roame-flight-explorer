package travel.roame.takehome.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import travel.roame.takehome.AndroidExplorer
import travel.roame.takehome.DebugOptions
import travel.roame.takehome.FlightExplorerStore

/**
 * Owns the shared store for the activity's lifetime, across rotations. [launchExtras] holds the
 * activity's intent extras, which debug builds read as [DebugOptions].
 */
class ExplorerViewModel(application: Application, launchExtras: SavedStateHandle) : AndroidViewModel(application) {
    private val debug = debugOptions(launchExtras)

    val store: FlightExplorerStore = AndroidExplorer.createStore(application, BuildConfig.API_BASE_URL, debug)

    var failSaves by mutableStateOf(debug.failSaves)
        private set
    var failRemoves by mutableStateOf(debug.failRemoves)
        private set

    /** Debug menu: makes saves and/or removes fail, to check the rollback and message (QA V6). */
    fun setFaults(failSaves: Boolean, failRemoves: Boolean) {
        this.failSaves = failSaves
        this.failRemoves = failRemoves
        store.setFaultInjection(failSaves, failRemoves)
    }

    override fun onCleared() {
        store.close()
    }

    companion object {
        /**
         * Boolean intent extras, like iOS's `-resetData`, `-failSaves` and `-failRemoves` launch
         * arguments; debug builds only. `adb shell am start -n travel.roame.takehome.android/.MainActivity --ez resetData true`.
         */
        const val EXTRA_RESET_DATA = "resetData"
        const val EXTRA_FAIL_SAVES = "failSaves"
        const val EXTRA_FAIL_REMOVES = "failRemoves"
    }
}

private fun debugOptions(extras: SavedStateHandle): DebugOptions {
    if (!BuildConfig.DEBUG) return DebugOptions()
    return DebugOptions(
        // Removed once read: an activity recreated after process death mustn't wipe the data again.
        resetData = extras.remove<Boolean>(ExplorerViewModel.EXTRA_RESET_DATA) == true,
        failSaves = extras.get<Boolean>(ExplorerViewModel.EXTRA_FAIL_SAVES) == true,
        failRemoves = extras.get<Boolean>(ExplorerViewModel.EXTRA_FAIL_REMOVES) == true,
    )
}
