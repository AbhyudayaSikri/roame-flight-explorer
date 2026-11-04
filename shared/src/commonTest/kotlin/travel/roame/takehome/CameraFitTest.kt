package travel.roame.takehome

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraFitTest {
    private val width = 400.0
    private val height = 800.0
    private val card = 300.0
    private val padding = 32.0

    @Test
    fun mercatorMatchesKnownValues() {
        assertEquals(0.5, mercatorX(0.0))
        assertEquals(0.0, mercatorX(-180.0))
        assertEquals(0.5, mercatorY(0.0), 1e-12)
        assertEquals(0.0, mercatorY(85.05112878), 1e-6)
        assertEquals(1.0, mercatorY(-85.05112878), 1e-6)
    }

    @Test
    fun noPointsGiveNoCamera() {
        assertNull(fitCamera(emptyList(), width, height))
    }

    @Test
    fun oneAirportIsCentredAtTheMaximumZoom() {
        val haneda = GeoPoint(latitude = 35.55, longitude = 139.78)
        val fit = fitCamera(listOf(haneda), width, height, maxZoom = 8.0)!!
        assertEquals(8.0, fit.zoom)
        assertEquals(haneda.latitude, fit.center.latitude, 1e-9)
        assertEquals(haneda.longitude, fit.center.longitude, 1e-9)
    }

    @Test
    fun aTransPacificRouteIsCentredOverThePacificNotEurope() {
        val flight = fixtureFlights.first { it.origin == "SFO" && it.destination == "NRT" }
        val fit = fitCamera(greatCirclePath(flight.originPoint, flight.destinationPoint), width, height)!!
        assertTrue(abs(fit.center.longitude) > 150, "centre ${fit.center}")
    }

    @Test
    fun theCardPushesTheRouteHigherOnScreen() {
        val flight = fixtureFlights.first { it.origin == "LAX" && it.destination == "MEX" }
        val path = greatCirclePath(flight.originPoint, flight.destinationPoint)
        val uncovered = fitCamera(path, width, height)!!
        val covered = fitCamera(path, width, height, bottomInset = card)!!
        assertTrue(covered.center.latitude < uncovered.center.latitude, "$covered vs $uncovered")
    }

    @Test
    fun everyFixtureRouteFitsInsideThePaddingAboveTheCard() {
        val routes = fixtureFlights.map { it.originPoint to it.destinationPoint }.distinct()
        assertTrue(routes.size > 100)
        for ((from, to) in routes) {
            val path = greatCirclePath(from, to)
            val fit = fitCamera(path, width, height, bottomInset = card, padding = padding)!!
            val world = 512.0 * 2.0.pow(fit.zoom)
            val centreX = mercatorX(fit.center.longitude)
            val centreY = mercatorY(fit.center.latitude)
            val xs = unwrapLongitudes(path.map { it.longitude }).map(::mercatorX)
            // Same path, shifted by whole turns so it sits next to the (normalized) centre.
            val turns = (centreX - (xs.min() + xs.max()) / 2).roundToInt()
            path.forEachIndexed { index, point ->
                val screenX = (xs[index] + turns - centreX) * world + width / 2
                val screenY = (mercatorY(point.latitude) - centreY) * world + height / 2
                val inside = screenX in (padding - 0.5)..(width - padding + 0.5) &&
                    screenY in (padding - 0.5)..(height - card - padding + 0.5)
                assertTrue(inside, "$from → $to: $point lands at ($screenX, $screenY) with zoom ${fit.zoom}")
            }
        }
    }
}
