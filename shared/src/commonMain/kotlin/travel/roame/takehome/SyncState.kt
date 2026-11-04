package travel.roame.takehome

sealed interface RefreshError {
    /** English text for the UI. */
    val userMessage: String

    data object Offline : RefreshError {
        override val userMessage = "Can't reach the flights server."
    }

    data class Http(val code: Int) : RefreshError {
        override val userMessage get() = "The flights server returned an error ($code)."
    }

    data object Malformed : RefreshError {
        override val userMessage = "The flights server sent data the app can't read."
    }

    data object Storage : RefreshError {
        override val userMessage = "Couldn't store the flights on this device."
    }
}

sealed interface SyncState {
    /** First load: nothing cached yet and no attempt has finished. */
    data object Loading : SyncState

    data class Fresh(val lastSuccessAtMillis: Long) : SyncState

    /** The last refresh failed; the cache from [lastSuccessAtMillis] is still shown. */
    data class Stale(val lastSuccessAtMillis: Long, val error: RefreshError) : SyncState

    /** The server answered 200 with no flights. Not an error. */
    data class EmptySuccess(val lastSuccessAtMillis: Long) : SyncState

    /** Nothing cached and the refresh failed: show the error and Retry. */
    data class FailedNoCache(val error: RefreshError) : SyncState
}

internal sealed interface RefreshStatus {
    data object Idle : RefreshStatus
    data object Running : RefreshStatus
    data class Failed(val error: RefreshError) : RefreshStatus
}

internal fun syncStateOf(hasCachedFlights: Boolean, lastSuccessAtMillis: Long?, status: RefreshStatus): SyncState =
    when {
        status is RefreshStatus.Failed && lastSuccessAtMillis != null -> SyncState.Stale(lastSuccessAtMillis, status.error)
        status is RefreshStatus.Failed -> SyncState.FailedNoCache(status.error)
        lastSuccessAtMillis == null -> SyncState.Loading
        !hasCachedFlights -> SyncState.EmptySuccess(lastSuccessAtMillis)
        else -> SyncState.Fresh(lastSuccessAtMillis)
    }
