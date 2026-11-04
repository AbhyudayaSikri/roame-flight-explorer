package travel.roame.takehome

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Points along the great circle from [from] to [to]. Longitudes are unwrapped (they may go past
 * ±180) so consecutive points never jump by more than 180°; 68 of the fixture's 192 routes cross
 * the antimeridian, and a wrapped path would draw a line across the whole map.
 */
internal fun greatCirclePath(from: GeoPoint, to: GeoPoint, segments: Int = 64): List<GeoPoint> {
    val lat1 = from.latitude.toRadians()
    val lon1 = from.longitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val lon2 = to.longitude.toRadians()
    val angle = 2 * asin(
        sqrt(sin((lat2 - lat1) / 2).squared() + cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).squared()),
    )
    if (abs(sin(angle)) < 1e-12) return listOf(from, to)

    var previousLongitude = from.longitude
    return (0..segments).map { step ->
        val fraction = step.toDouble() / segments
        val a = sin((1 - fraction) * angle) / sin(angle)
        val b = sin(fraction * angle) / sin(angle)
        val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
        val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
        val z = a * sin(lat1) + b * sin(lat2)
        var longitude = atan2(y, x).toDegrees()
        while (longitude - previousLongitude > 180) longitude -= 360
        while (longitude - previousLongitude < -180) longitude += 360
        previousLongitude = longitude
        GeoPoint(latitude = atan2(z, sqrt(x * x + y * y)).toDegrees(), longitude = longitude)
    }
}

private fun Double.toRadians() = this * PI / 180

private fun Double.toDegrees() = this * 180 / PI

private fun Double.squared() = this * this
