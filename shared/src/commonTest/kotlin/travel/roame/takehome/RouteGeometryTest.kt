package travel.roame.takehome

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteGeometryTest {
    private val jfk = GeoPoint(40.63945, -73.77932)
    private val hnd = GeoPoint(35.54968, 139.78696)
    private val lhr = GeoPoint(51.47075, -0.45991)
    private val sfo = GeoPoint(37.61981, -122.37482)
    private val syd = GeoPoint(-33.9461, 151.177)

    @Test
    fun aTransPacificPathIsContinuousAcrossTheAntimeridian() {
        val path = greatCirclePath(sfo, syd)

        assertEquals(65, path.size)
        assertEquals(sfo, path.first())
        assertClose(syd.latitude, path.last().latitude)
        assertClose(syd.longitude - 360, path.last().longitude)
        assertNoJumps(path)
    }

    @Test
    fun jfkToTokyoCurvesOverTheArctic() {
        val path = greatCirclePath(jfk, hnd)

        assertNoJumps(path)
        assertTrue(path.maxOf { it.latitude } > 60)
        assertClose(hnd.latitude, path.last().latitude)
        assertClose(0.0, angularDistance(path.last().longitude, hnd.longitude))
    }

    @Test
    fun aPathThatDoesNotCrossTheAntimeridianStaysInRange() {
        val path = greatCirclePath(jfk, lhr)

        assertNoJumps(path)
        assertTrue(path.all { it.longitude in -180.0..180.0 })
        assertClose(lhr.longitude, path.last().longitude)
    }

    @Test
    fun identicalPointsGiveATrivialPath() {
        assertEquals(listOf(jfk, jfk), greatCirclePath(jfk, jfk))
    }

    @Test
    fun everyFixtureRouteIsContinuousAndEndsAtItsDestination() {
        fixtureFlights.distinctBy { it.origin to it.destination }.forEach { flight ->
            val path = greatCirclePath(flight.originPoint, flight.destinationPoint)
            // Six routes pass within 4° of the North Pole, where longitude really does swing fast.
            val polar = path.maxOf { abs(it.latitude) } >= 80
            assertNoJumps(path, limitDegrees = if (polar) 180.0 else 30.0)
            assertClose(flight.destinationPoint.latitude, path.last().latitude)
            assertClose(0.0, angularDistance(path.last().longitude, flight.destinationPoint.longitude))
        }
    }

    private fun assertNoJumps(path: List<GeoPoint>, limitDegrees: Double = 30.0) {
        path.zipWithNext().forEach { (a, b) ->
            assertTrue(abs(b.longitude - a.longitude) < limitDegrees, "jump $a -> $b")
        }
    }

    /** Smallest difference between two longitudes, in 0..180. */
    private fun angularDistance(a: Double, b: Double): Double = abs(((a - b) % 360 + 540) % 360 - 180)

    private fun assertClose(expected: Double, actual: Double) {
        assertTrue(abs(expected - actual) < 1e-6, "expected $expected, got $actual")
    }
}
