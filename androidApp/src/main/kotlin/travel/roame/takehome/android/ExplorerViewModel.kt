package travel.roame.takehome.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import travel.roame.takehome.AndroidExplorer
import travel.roame.takehome.FlightExplorerStore

/** Owns the shared store for the activity's lifetime, across rotations. */
class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    val store: FlightExplorerStore = AndroidExplorer.createStore(application, BuildConfig.API_BASE_URL)

    var failSaves by mutableStateOf(false)
        private set
    var failRemoves by mutableStateOf(false)
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
}
