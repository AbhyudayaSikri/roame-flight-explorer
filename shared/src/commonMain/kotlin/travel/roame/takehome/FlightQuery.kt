package travel.roame.takehome

internal data class FlightQuery(
    val originText: String = "",
    val destinationText: String = "",
    /** null = no date constraint, which is the full range of the data. */
    val dateRange: DateRange? = null,
    /** Single-select; null = any cabin. */
    val cabin: String? = null,
    /** Inclusive; null = no limit. */
    val maxPoints: Int? = null,
    /** OR within the set; empty = every program. */
    val programs: Set<String> = emptySet(),
)

/** What the cached data contains: drives the default query and the filter controls. */
data class DataBounds(
    val firstDate: CalendarDate,
    val lastDate: CalendarDate,
    val minPoints: Int,
    val maxPoints: Int,
    val cabins: List<String>,
    val programs: List<String>,
    val airportCodes: Set<String>,
) {
    val dateRange: DateRange get() = DateRange(firstDate, lastDate)

    internal companion object {
        fun of(flights: List<Flight>): DataBounds? {
            if (flights.isEmpty()) return null
            return DataBounds(
                firstDate = flights.minOf { it.departureDate },
                lastDate = flights.maxOf { it.departureDate },
                minPoints = flights.minOf { it.points },
                maxPoints = flights.maxOf { it.points },
                cabins = flights.mapTo(mutableSetOf()) { it.cabin }.sortedWith(cabinOrder),
                programs = flights.mapTo(mutableSetOf()) { it.program }.sorted(),
                airportCodes = flights.flatMapTo(mutableSetOf()) { listOf(it.origin, it.destination) },
            )
        }
    }
}

private val knownCabins = listOf("Economy", "Premium Economy", "Business", "First")

/** Known cabins from cheapest to most premium, then any new cabin names alphabetically. */
private val cabinOrder: Comparator<String> =
    compareBy<String> { knownCabins.indexOf(it).let { index -> if (index < 0) knownCabins.size else index } }
        .thenBy { it }

/** Departure date, then points, then id, so the order is stable for equal flights. */
internal val flightOrder: Comparator<Flight> =
    compareBy<Flight> { it.departureDate }.thenBy { it.points }.thenBy { it.id }

/** AND across filter types; empty filters match everything. Results come back in [flightOrder]. */
internal fun filterFlights(flights: List<Flight>, query: FlightQuery, knownCodes: Set<String>): List<Flight> {
    val origin = AirportMatcher(query.originText, knownCodes)
    val destination = AirportMatcher(query.destinationText, knownCodes)
    return flights
        .filter { flight ->
            origin.matches(flight.origin, flight.originCityKey) &&
                destination.matches(flight.destination, flight.destinationCityKey) &&
                (query.dateRange == null || flight.departureDate in query.dateRange) &&
                (query.cabin == null || flight.cabin == query.cabin) &&
                (query.maxPoints == null || flight.points <= query.maxPoints) &&
                (query.programs.isEmpty() || flight.program in query.programs)
        }
        .sortedWith(flightOrder)
}
