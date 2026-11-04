package travel.roame.takehome

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * The single source of truth for both UIs. Call it from the main thread; it does its own work on
 * background dispatchers. [state] never throws: failures arrive as [ExplorerState.sync] or
 * [ExplorerState.message] (SKIE turns an exception inside a Flow into a Swift crash).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlightExplorerStore internal constructor(
    private val repository: FlightRepository,
    private val scope: CoroutineScope,
    private val computeDispatcher: CoroutineDispatcher,
    private val faults: FaultInjection,
    private val now: () -> Long,
    resetData: Boolean = false,
    private val onClose: () -> Unit = {},
) {
    private val query = MutableStateFlow(FlightQuery())
    private val selection = MutableStateFlow(Selection())
    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    private val pending = MutableStateFlow<Map<String, PendingChange>>(emptyMap())
    private val message = MutableStateFlow<UserMessage?>(null)
    private var messageCount = 0L
    private var cachedById: Map<String, Flight> = emptyMap()
    private var savedById: Map<String, SavedFlight> = emptyMap()

    // Each source starts as null ("not read yet"), so the state, and any storage error message,
    // shows up without waiting for every database read to succeed.
    private val stored: Flow<Stored> = combine(
        repository.cachedFlights.retryOnError("Couldn't read cached flights").nullUntilLoaded(),
        repository.savedFlights.retryOnError("Couldn't read saved flights").nullUntilLoaded(),
        repository.lastSuccessAtMillis.retryOnError("Couldn't read the last sync time").map { Loaded(it) }.nullUntilLoaded(),
    ) { cached, saved, lastSuccessAt -> Stored(cached, saved, lastSuccessAt) }

    private val inputs: Flow<Inputs> = combine(query, selection, refreshStatus, pending, message) { q, s, r, p, m ->
        Inputs(q, s, r, p, m)
    }

    val state: StateFlow<ExplorerState> = combine(stored, inputs) { s, i -> s to i }
        .mapLatest { (s, i) -> withContext(computeDispatcher) { derive(s, i) } }
        .onEach(::applyCorrections)
        .map { it.state }
        .stateIn(scope, SharingStarted.Eagerly, ExplorerState.Initial)

    init {
        scope.launch {
            if (resetData && repository.clearAll().isFailure) postMessage("Couldn't reset local data.")
            refresh()
        }
    }

    fun setOriginText(text: String) = query.update { it.copy(originText = text) }

    fun setDestinationText(text: String) = query.update { it.copy(destinationText = text) }

    /** Inclusive on both ends; a range covering all the data is the same as no date filter. */
    fun setDateRange(start: CalendarDate, end: CalendarDate) {
        val range = if (start <= end) DateRange(start, end) else DateRange(end, start)
        val all = state.value.filters.bounds?.dateRange
        query.update { it.copy(dateRange = range.takeUnless { range == all }) }
    }

    /** null = any cabin. */
    fun setCabin(cabin: String?) = query.update { it.copy(cabin = cabin) }

    /** Inclusive; a limit at or above the highest price is the same as no limit. */
    fun setMaxPoints(points: Int) {
        val highest = state.value.filters.bounds?.maxPoints
        query.update { it.copy(maxPoints = points.takeUnless { highest != null && points >= highest }) }
    }

    fun clearMaxPoints() = query.update { it.copy(maxPoints = null) }

    fun toggleProgram(program: String) = query.update {
        it.copy(programs = if (program in it.programs) it.programs - program else it.programs + program)
    }

    fun clearFilter(kind: FilterKind) = query.update {
        when (kind) {
            FilterKind.ORIGIN -> it.copy(originText = "")
            FilterKind.DESTINATION -> it.copy(destinationText = "")
            FilterKind.DATES -> it.copy(dateRange = null)
            FilterKind.CABIN -> it.copy(cabin = null)
            FilterKind.MAX_POINTS -> it.copy(maxPoints = null)
            FilterKind.PROGRAMS -> it.copy(programs = emptySet())
        }
    }

    /** Back to the default query, including both search fields. */
    fun resetFilters() {
        query.value = FlightQuery()
    }

    fun focusDestination(airportCode: String?) = selection.update { it.copy(focusedAirport = airportCode) }

    fun select(flightId: String?) = selection.update { it.copy(selectedId = flightId) }

    fun selectSaved(flightId: String?) = selection.update { it.copy(selectedSavedId = flightId) }

    /** Saves a snapshot of the cached flight; on an already-saved flight this is "Update saved". */
    fun save(flightId: String) {
        if (pending.value[flightId]?.done == false) return
        val flight = cachedById[flightId]
        if (flight == null) {
            postMessage("That flight is no longer in the latest results, so it can't be saved.")
            return
        }
        pending.update { it + (flightId to PendingChange(PendingKind.SAVE, flight, now())) }
        scope.launch {
            val result = repository.save(flight)
            if (result.isSuccess) {
                markDone(flightId)
            } else {
                pending.update { it - flightId }
                postMessage("Couldn't save ${flight.origin} → ${flight.destination}. Please try again.")
            }
        }
    }

    fun remove(flightId: String) {
        val inFlight = pending.value[flightId]
        if (inFlight?.done == false) return
        val snapshot = savedById[flightId]?.flight ?: inFlight?.flight?.takeIf { inFlight.kind == PendingKind.SAVE } ?: return
        pending.update { it + (flightId to PendingChange(PendingKind.REMOVE, snapshot, now())) }
        scope.launch {
            val result = repository.remove(flightId)
            if (result.isSuccess) {
                markDone(flightId)
            } else {
                pending.update { it - flightId }
                postMessage("Couldn't remove ${snapshot.origin} → ${snapshot.destination}. Please try again.")
            }
        }
    }

    /** Fetches the latest flights; ignored while a refresh is already running. */
    fun refresh() {
        if (refreshStatus.value == RefreshStatus.Running) return
        refreshStatus.value = RefreshStatus.Running
        scope.launch {
            refreshStatus.value = when (val result = repository.refresh()) {
                is RefreshResult.Success -> RefreshStatus.Idle
                is RefreshResult.Failure -> RefreshStatus.Failed(result.error)
            }
        }
    }

    /** Clears [messageId] only, so a newer message that arrived meanwhile isn't lost. */
    fun messageShown(messageId: Long) = message.update { if (it?.id == messageId) null else it }

    /** Debug builds only: makes the next saves and/or removes fail (QA V6). */
    fun setFaultInjection(failSaves: Boolean, failRemoves: Boolean) {
        faults.failSaves = failSaves
        faults.failRemoves = failRemoves
    }

    /** Stops the store. The database and HTTP client close once work already running has finished. */
    fun close() {
        val job = scope.coroutineContext[Job]
        scope.cancel()
        if (job == null) onClose() else job.invokeOnCompletion { onClose() }
    }

    private fun markDone(flightId: String) = pending.update { current ->
        val change = current[flightId] ?: return@update current
        current + (flightId to change.copy(done = true))
    }

    private fun postMessage(text: String) {
        messageCount += 1
        message.value = UserMessage(messageCount, text)
    }

    /** Emits null once before the first value; retries upstream of this don't repeat it. */
    private fun <T : Any> Flow<T>.nullUntilLoaded(): Flow<T?> {
        val values: Flow<T?> = this
        return values.onStart { emit(null) }
    }

    /** Retries with backoff and posts one message per streak of failures; a value ends the streak. */
    private fun <T> Flow<T>.retryOnError(text: String): Flow<T> {
        var failures = 0
        return onEach { failures = 0 }.retryWhen { cause, _ ->
            if (cause is CancellationException) return@retryWhen false
            if (failures == 0) postMessage("$text. Retrying…")
            failures += 1
            delay((1_000L shl (failures - 1).coerceAtMost(5)).coerceAtMost(30_000L))
            true
        }
    }

    /** Pure: everything the UIs show, computed off the main thread. */
    private fun derive(stored: Stored, inputs: Inputs): Derived {
        val cached = stored.cached.orEmpty()
        val bounds = DataBounds.of(cached)
        val q = inputs.query
        val results = filterFlights(cached, q, bounds?.airportCodes.orEmpty())
        val saved = savedWithPending(stored.saved.orEmpty(), inputs.pending)
        val savedIds = saved.mapTo(mutableSetOf()) { it.flight.id }
        val markers = aggregateByDestination(results, savedIds)
        val focused = inputs.selection.focusedAirport?.let { code -> markers.firstOrNull { it.airport == code } }
        val selected = inputs.selection.selectedId?.let { id -> results.firstOrNull { it.id == id } }
        val storedSavedById = stored.saved.orEmpty().associateBy { it.flight.id }
        val sync = if (!stored.isLoaded && inputs.refreshStatus !is RefreshStatus.Failed) {
            SyncState.Loading
        } else {
            syncStateOf(cached.isNotEmpty(), stored.lastSuccessAtMillis?.value, inputs.refreshStatus)
        }

        val state = ExplorerState(
            sync = sync,
            isRefreshing = inputs.refreshStatus == RefreshStatus.Running,
            filters = FiltersUi(
                originText = q.originText,
                destinationText = q.destinationText,
                dateRange = q.dateRange ?: bounds?.dateRange,
                isDateRangeLimited = q.dateRange != null && q.dateRange != bounds?.dateRange,
                cabin = q.cabin,
                maxPoints = q.maxPoints ?: bounds?.maxPoints ?: 0,
                isPointsLimited = q.maxPoints != null && (bounds == null || q.maxPoints < bounds.maxPoints),
                pointsStep = POINTS_STEP,
                programs = q.programs,
                bounds = bounds,
            ),
            results = results,
            resultCount = results.size,
            totalCount = cached.size,
            activeFilters = activeFilters(q, bounds),
            markers = markers,
            focusedDestination = focused,
            focusedFlights = if (focused == null) emptyList() else results.filter { it.destination == focused.airport },
            selected = selected,
            selectedRoute = selected?.let { greatCirclePath(it.originPoint, it.destinationPoint) }.orEmpty(),
            selectedSaveStatus = saveStatusOf(selected, storedSavedById, inputs.pending),
            saved = saved,
            savedIds = savedIds,
            selectedSaved = inputs.selection.selectedSavedId?.let { id -> saved.firstOrNull { it.flight.id == id } },
            message = inputs.message,
        )
        // Until every read has arrived, a missing flight may just not be loaded yet: clear nothing.
        val loaded = stored.isLoaded
        return Derived(
            state = state,
            cachedById = stored.cached?.associateBy { it.id },
            savedById = stored.saved?.let { storedSavedById },
            staleSelection = loaded && inputs.selection.selectedId != null && selected == null,
            staleFocus = loaded && inputs.selection.focusedAirport != null && focused == null,
            staleSavedSelection = loaded && inputs.selection.selectedSavedId != null && state.selectedSaved == null,
            settled = if (stored.saved == null) emptySet() else {
                inputs.pending.filter { (id, change) -> change.done && change.isReflectedIn(storedSavedById[id]) }.keys
            },
        )
    }

    /** Side effects of a new derivation, on the store's own thread. Each one triggers a re-derive. */
    private fun applyCorrections(derived: Derived) {
        derived.cachedById?.let { cachedById = it }
        derived.savedById?.let { savedById = it }
        if (derived.staleSelection || derived.staleFocus || derived.staleSavedSelection) {
            selection.update {
                it.copy(
                    selectedId = if (derived.staleSelection) null else it.selectedId,
                    focusedAirport = if (derived.staleFocus) null else it.focusedAirport,
                    selectedSavedId = if (derived.staleSavedSelection) null else it.selectedSavedId,
                )
            }
        }
        if (derived.settled.isNotEmpty()) pending.update { it - derived.settled }
    }
}

internal enum class PendingKind { SAVE, REMOVE }

internal data class PendingChange(
    val kind: PendingKind,
    val flight: Flight,
    val startedAtMillis: Long,
    val done: Boolean = false,
) {
    /** True once the database shows what this change wrote, so the optimistic overlay can go. */
    fun isReflectedIn(stored: SavedFlight?): Boolean = when (kind) {
        PendingKind.SAVE -> stored?.flight == flight
        PendingKind.REMOVE -> stored == null
    }
}

private data class Selection(
    val selectedId: String? = null,
    val focusedAirport: String? = null,
    val selectedSavedId: String? = null,
)

/** Wraps a value that may itself be null, so "not read yet" (a null Loaded) stays distinct. */
private class Loaded<T>(val value: T)

/** What the database has shown so far; a null field hasn't been read yet. */
private class Stored(
    val cached: List<Flight>?,
    val saved: List<SavedFlight>?,
    val lastSuccessAtMillis: Loaded<Long?>?,
) {
    val isLoaded: Boolean get() = cached != null && saved != null && lastSuccessAtMillis != null
}

private data class Inputs(
    val query: FlightQuery,
    val selection: Selection,
    val refreshStatus: RefreshStatus,
    val pending: Map<String, PendingChange>,
    val message: UserMessage?,
)

private class Derived(
    val state: ExplorerState,
    val cachedById: Map<String, Flight>?,
    val savedById: Map<String, SavedFlight>?,
    val staleSelection: Boolean,
    val staleFocus: Boolean,
    val staleSavedSelection: Boolean,
    val settled: Set<String>,
)

/** Saved flights as the user should see them: the database plus saves and removals in flight. */
internal fun savedWithPending(stored: List<SavedFlight>, pending: Map<String, PendingChange>): List<SavedFlight> {
    if (pending.isEmpty()) return stored
    val storedIds = stored.mapTo(mutableSetOf()) { it.flight.id }
    val kept = stored.mapNotNull { saved ->
        val change = pending[saved.flight.id]
        when (change?.kind) {
            null -> saved
            PendingKind.REMOVE -> null
            PendingKind.SAVE -> SavedFlight(change.flight, change.startedAtMillis)
        }
    }
    val added = pending.values
        .filter { it.kind == PendingKind.SAVE && it.flight.id !in storedIds }
        .map { SavedFlight(it.flight, it.startedAtMillis) }
    return (added + kept).sortedWith(compareByDescending<SavedFlight> { it.savedAtMillis }.thenBy { it.flight.id })
}

internal fun saveStatusOf(
    selected: Flight?,
    storedSaved: Map<String, SavedFlight>,
    pending: Map<String, PendingChange>,
): SaveStatus {
    if (selected == null) return SaveStatus.NOT_SAVED
    val change = pending[selected.id]
    return when {
        change != null && !change.done -> SaveStatus.IN_PROGRESS
        change?.kind == PendingKind.REMOVE -> SaveStatus.NOT_SAVED
        change?.kind == PendingKind.SAVE -> if (change.flight == selected) SaveStatus.SAVED else SaveStatus.OUTDATED
        else -> when (val saved = storedSaved[selected.id]) {
            null -> SaveStatus.NOT_SAVED
            else -> if (saved.flight == selected) SaveStatus.SAVED else SaveStatus.OUTDATED
        }
    }
}
