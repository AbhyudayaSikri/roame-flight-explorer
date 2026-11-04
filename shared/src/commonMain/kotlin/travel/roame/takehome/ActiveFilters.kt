package travel.roame.takehome

enum class FilterKind { ORIGIN, DESTINATION, DATES, CABIN, MAX_POINTS, PROGRAMS }

data class ActiveFilter(val kind: FilterKind, val label: String)

/**
 * One chip per filter that actually narrows the results. A date range covering the whole data
 * range, or a points limit at (or above) the highest price, counts as no filter.
 */
internal fun activeFilters(query: FlightQuery, bounds: DataBounds?): List<ActiveFilter> = buildList {
    val origin = query.originText.trim()
    if (origin.isNotEmpty()) add(ActiveFilter(FilterKind.ORIGIN, "From: $origin"))
    val destination = query.destinationText.trim()
    if (destination.isNotEmpty()) add(ActiveFilter(FilterKind.DESTINATION, "To: $destination"))
    query.dateRange?.takeIf { bounds == null || it != bounds.dateRange }?.let {
        add(ActiveFilter(FilterKind.DATES, formatDateRange(it)))
    }
    query.cabin?.let { add(ActiveFilter(FilterKind.CABIN, it)) }
    query.maxPoints?.takeIf { bounds == null || it < bounds.maxPoints }?.let {
        add(ActiveFilter(FilterKind.MAX_POINTS, "≤ ${formatPoints(it)}"))
    }
    if (query.programs.isNotEmpty()) add(ActiveFilter(FilterKind.PROGRAMS, query.programs.sorted().joinToString(", ")))
}
