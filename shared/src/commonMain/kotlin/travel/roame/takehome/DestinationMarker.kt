package travel.roame.takehome

data class DestinationMarker(
    val airport: String,
    val city: String,
    val point: GeoPoint,
    val count: Int,
    val lowestPoints: Int,
    /** "55 · 16.5k" */
    val label: String,
    val hasSaved: Boolean,
)

/** One marker per destination airport code (Tokyo is HND and NRT), sorted by code. */
internal fun aggregateByDestination(results: List<Flight>, savedIds: Set<String>): List<DestinationMarker> =
    results
        .groupBy { it.destination }
        .map { (airport, flights) ->
            val lowest = flights.minOf { it.points }
            DestinationMarker(
                airport = airport,
                city = flights.first().destinationCity,
                point = flights.first().destinationPoint,
                count = flights.size,
                lowestPoints = lowest,
                label = "${flights.size} · ${formatPointsCompact(lowest)}",
                hasSaved = flights.any { it.id in savedIds },
            )
        }
        .sortedBy { it.airport }
