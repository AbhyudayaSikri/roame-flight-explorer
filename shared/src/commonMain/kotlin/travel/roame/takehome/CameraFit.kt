package travel.roame.takehome

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** A Web Mercator camera: [center] goes in the middle of the map view, at [zoom]. */
data class CameraFit(val center: GeoPoint, val zoom: Double)

/**
 * The camera that fits [points] into a [viewWidth] × [viewHeight] map, keeping [padding] clear on
 * every side and leaving out the bottom [bottomInset], where a card covers the map. Longitudes may
 * run past ±180, as `ExplorerState.selectedRoute`'s do, so a route across the antimeridian is
 * framed as one piece. Sizes are in the map's units (dp on Android), and [tileSize] is the world's
 * width at zoom 0 (512 in MapLibre). Null when there are no points.
 */
fun fitCamera(
    points: List<GeoPoint>,
    viewWidth: Double,
    viewHeight: Double,
    bottomInset: Double = 0.0,
    padding: Double = 32.0,
    tileSize: Double = 512.0,
    maxZoom: Double = 8.0,
): CameraFit? {
    if (points.isEmpty()) return null
    val xs = unwrapLongitudes(points.map { it.longitude }).map(::mercatorX)
    val ys = points.map { mercatorY(it.latitude) }
    val spanX = xs.max() - xs.min()
    val spanY = ys.max() - ys.min()
    val width = max(viewWidth - 2 * padding, 1.0)
    val height = max(viewHeight - bottomInset - 2 * padding, 1.0)
    val fittingWorld = min(
        if (spanX > 0) width / spanX else Double.MAX_VALUE,
        if (spanY > 0) height / spanY else Double.MAX_VALUE,
    )
    val zoom = log2(fittingWorld / tileSize).coerceIn(0.0, maxZoom)
    val world = tileSize * 2.0.pow(zoom)
    val centerX = (xs.min() + xs.max()) / 2
    // The view's centre sits half the card's height below the middle of the uncovered area.
    val centerY = (ys.min() + ys.max()) / 2 + bottomInset / 2 / world
    return CameraFit(
        center = GeoPoint(latitude = latitudeAt(centerY), longitude = normalizeLongitude(centerX * 360 - 180)),
        zoom = zoom,
    )
}

private const val MAX_MERCATOR_LATITUDE = 85.05112878

/** 0 at 180° W, 1 at 180° E; unwrapped longitudes go past either end. */
internal fun mercatorX(longitude: Double): Double = (longitude + 180) / 360

/** 0 at the top of the Web Mercator world, 1 at the bottom. */
internal fun mercatorY(latitude: Double): Double {
    val phi = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE) * PI / 180
    return (1 - ln(tan(PI / 4 + phi / 2)) / PI) / 2
}

private fun latitudeAt(y: Double): Double = atan(sinh(PI * (1 - 2 * y))) * 180 / PI

/** Moves each longitude by whole turns to within 180° of the one before it. */
internal fun unwrapLongitudes(longitudes: List<Double>): List<Double> {
    var previous = longitudes.firstOrNull() ?: return emptyList()
    return longitudes.map { longitude ->
        var unwrapped = longitude
        while (unwrapped - previous > 180) unwrapped -= 360
        while (unwrapped - previous < -180) unwrapped += 360
        previous = unwrapped
        unwrapped
    }
}

internal fun normalizeLongitude(longitude: Double): Double {
    val shifted = (longitude + 180) % 360
    return (if (shifted < 0) shifted + 360 else shifted) - 180
}
