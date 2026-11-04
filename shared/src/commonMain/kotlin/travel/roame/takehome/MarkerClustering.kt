package travel.roame.takehome

import kotlin.math.abs
import kotlin.math.min

/**
 * One pill on the map: a single destination, or several whose pills would overlap at the current
 * zoom. [title] is the airport code or "6 airports", and [label] is "flights · lowest points".
 * [id] is the code of the busiest destination in it.
 */
data class MarkerCluster(
    val id: String,
    val center: GeoPoint,
    val markers: List<DestinationMarker>,
    val title: String,
    val label: String,
    val flightCount: Int,
    val lowestPoints: Int,
    val hasSaved: Boolean,
) {
    val isSingle: Boolean get() = markers.size == 1
}

/**
 * Merges destination markers whose pills, [pillWidth] × [pillHeight] centred on the airport, would
 * overlap when the whole world is [worldWidth] wide on screen (same units as the pill). The
 * busiest destinations claim their neighbours first, so a zoom always gives the same pills and no
 * two of them overlap. A [worldWidth] of zero or less (zoom not known yet) merges nothing.
 */
fun clusterMarkers(
    markers: List<DestinationMarker>,
    worldWidth: Double,
    pillWidth: Double = 110.0,
    pillHeight: Double = 28.0,
): List<MarkerCluster> {
    val merging = worldWidth.isFinite() && worldWidth > 0
    val groups = mutableListOf<MutableList<DestinationMarker>>()
    val busiestFirst = markers.sortedWith(compareByDescending<DestinationMarker> { it.count }.thenBy { it.airport })
    for (marker in busiestFirst) {
        val group = if (merging) {
            groups.firstOrNull { pillsOverlap(it.first().point, marker.point, worldWidth, pillWidth, pillHeight) }
        } else {
            null
        }
        if (group != null) group += marker else groups += mutableListOf(marker)
    }
    return groups.map(::clusterOf).sortedBy { it.id }
}

private fun pillsOverlap(a: GeoPoint, b: GeoPoint, world: Double, width: Double, height: Double): Boolean {
    val dx = abs(mercatorX(a.longitude) - mercatorX(b.longitude)) * world
    val dy = abs(mercatorY(a.latitude) - mercatorY(b.latitude)) * world
    return min(dx, world - dx) < width && dy < height
}

private fun clusterOf(members: List<DestinationMarker>): MarkerCluster {
    val seed = members.first()
    val flights = members.sumOf { it.count }
    val lowest = members.minOf { it.lowestPoints }
    return MarkerCluster(
        id = seed.airport,
        center = seed.point,
        markers = members.sortedBy { it.airport },
        title = if (members.size == 1) seed.airport else "${members.size} airports",
        label = if (members.size == 1) seed.label else "$flights · ${formatPointsCompact(lowest)}",
        flightCount = flights,
        lowestPoints = lowest,
        hasSaved = members.any { it.hasSaved },
    )
}
