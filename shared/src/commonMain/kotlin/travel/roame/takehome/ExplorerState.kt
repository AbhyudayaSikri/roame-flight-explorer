package travel.roame.takehome

enum class SaveStatus { NOT_SAVED, SAVED, OUTDATED, IN_PROGRESS }

/** A one-shot message for a snackbar or alert; clear it with FlightExplorerStore.messageShown(). */
data class UserMessage(val id: Long, val text: String)

data class FiltersUi(
    val originText: String,
    val destinationText: String,
    /** The effective range: the user's choice, or the full data range. null only when there's no data. */
    val dateRange: DateRange?,
    val isDateRangeLimited: Boolean,
    /** null = any cabin. */
    val cabin: String?,
    /** The slider value: the user's limit, or the highest price when there's no limit. */
    val maxPoints: Int,
    val isPointsLimited: Boolean,
    val pointsStep: Int,
    val programs: Set<String>,
    /** What the cached data contains; null until there is data. */
    val bounds: DataBounds?,
)

data class ExplorerState(
    val sync: SyncState,
    val isRefreshing: Boolean,
    val filters: FiltersUi,
    val results: List<Flight>,
    val resultCount: Int,
    /** Cached flights before filtering: 0 means "no flights available", not "no matches". */
    val totalCount: Int,
    val activeFilters: List<ActiveFilter>,
    val markers: List<DestinationMarker>,
    /** The tapped map marker and the flights behind it. */
    val focusedDestination: DestinationMarker?,
    val focusedFlights: List<Flight>,
    /** Selection in Explore: always one of [results], or null. */
    val selected: Flight?,
    /** Great-circle path for [selected], longitudes unwrapped across the antimeridian. */
    val selectedRoute: List<GeoPoint>,
    val selectedSaveStatus: SaveStatus,
    /** Newest first; includes saves still in flight and hides removals still in flight. */
    val saved: List<SavedFlight>,
    val savedIds: Set<String>,
    /** Selection in the Saved tab, independent of discovery filters. */
    val selectedSaved: SavedFlight?,
    val message: UserMessage?,
    /** Where flights come from: the build's address unless a debug menu chose another. */
    val serverUrl: String,
    val defaultServerUrl: String,
    val isDefaultServerUrl: Boolean,
) {
    internal companion object {
        fun initial(defaultServerUrl: String) = ExplorerState(
            sync = SyncState.Loading,
            isRefreshing = false,
            filters = FiltersUi(
                originText = "",
                destinationText = "",
                dateRange = null,
                isDateRangeLimited = false,
                cabin = null,
                maxPoints = 0,
                isPointsLimited = false,
                pointsStep = POINTS_STEP,
                programs = emptySet(),
                bounds = null,
            ),
            results = emptyList(),
            resultCount = 0,
            totalCount = 0,
            activeFilters = emptyList(),
            markers = emptyList(),
            focusedDestination = null,
            focusedFlights = emptyList(),
            selected = null,
            selectedRoute = emptyList(),
            selectedSaveStatus = SaveStatus.NOT_SAVED,
            saved = emptyList(),
            savedIds = emptySet(),
            selectedSaved = null,
            message = null,
            serverUrl = defaultServerUrl,
            defaultServerUrl = defaultServerUrl,
            isDefaultServerUrl = true,
        )
    }
}

/** Every fixture price is a multiple of 500, so the points slider can reach each one exactly. */
internal const val POINTS_STEP = 500

/** Debug-only behaviour, set from launch arguments (iOS) or the debug menu (Android). */
data class DebugOptions(
    val resetData: Boolean = false,
    val failSaves: Boolean = false,
    val failRemoves: Boolean = false,
)

/** Accessibility identifiers shared by both UIs; the iOS UI tests find controls by these. */
object TestIds {
    val searchOrigin = "search.origin"
    val searchDestination = "search.destination"
    val resultCount = "results.count"
    val filtersOpen = "filters.open"
    val filtersReset = "filters.reset"
    val filtersDone = "filters.done"
    val filterDateStart = "filters.dateStart"
    val filterDateEnd = "filters.dateEnd"
    /** Android's single button for the date range dialog; iOS has the two pickers above. */
    val filterDates = "filters.dates"
    val filterPoints = "filters.points"
    val filterPointsValue = "filters.pointsValue"
    val viewList = "view.list"
    val viewMap = "view.map"
    val tabExplore = "tab.explore"
    val tabSaved = "tab.saved"
    val card = "card"
    val cardSave = "card.save"
    val cardRemove = "card.remove"
    val cardUpdate = "card.update"
    val cardClose = "card.close"
    val destinationSheet = "sheet.destination"
    val staleBanner = "banner.stale"
    val errorRetry = "error.retry"
    val emptyState = "state.empty"
    val noMatches = "state.noMatches"
    val savedEmpty = "saved.empty"
    val loading = "state.loading"
    val message = "message"
    val debugOpen = "debug.open"
    val debugServerUrl = "debug.serverUrl"
    val debugServerUrlProblem = "debug.serverUrl.problem"
    val debugServerUrlSave = "debug.serverUrl.save"
    val debugServerUrlDefault = "debug.serverUrl.default"
    val debugDone = "debug.done"

    fun filterChip(kind: FilterKind) = "filters.chip.${kind.name.lowercase()}"
    fun cabinOption(cabin: String?) = "filters.cabin.${cabin ?: "any"}"
    fun programOption(program: String) = "filters.program.$program"
    fun flightRow(flightId: String) = "flight.row.$flightId"
    fun marker(airport: String) = "marker.$airport"
    fun markerCluster(id: String) = "cluster.$id"
    fun savedRow(flightId: String) = "saved.row.$flightId"
    fun savedRemove(flightId: String) = "saved.remove.$flightId"
}
