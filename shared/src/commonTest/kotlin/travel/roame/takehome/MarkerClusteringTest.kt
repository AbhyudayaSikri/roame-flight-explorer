package travel.roame.takehome

import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MarkerClusteringTest {
    private val markers = aggregateByDestination(fixtureFlights, savedIds = emptySet())
    private val pillWidth = 110.0
    private val pillHeight = 28.0

    /** The whole world's width on screen at MapLibre-style zoom [zoom]. */
    private fun worldAt(zoom: Double) = 512.0 * 2.0.pow(zoom)

    private fun clusters(world: Double, input: List<DestinationMarker> = markers) =
        clusterMarkers(input, worldWidth = world, pillWidth = pillWidth, pillHeight = pillHeight)

    private fun List<MarkerCluster>.containing(airport: String) = single { cluster -> cluster.markers.any { it.airport == airport } }

    private fun marker(code: String, latitude: Double, longitude: Double, count: Int, lowest: Int, saved: Boolean = false) =
        DestinationMarker(
            airport = code,
            city = code,
            point = GeoPoint(latitude = latitude, longitude = longitude),
            count = count,
            lowestPoints = lowest,
            label = "$count · ${formatPointsCompact(lowest)}",
            hasSaved = saved,
        )

    @Test
    fun atWorldZoomNeighboursMergeButDistantAirportsDoNot() {
        val world = clusters(worldAt(0.0))
        assertEquals(world.containing("LHR"), world.containing("CDG"))
        assertEquals(world.containing("HND"), world.containing("NRT"))
        assertNotEquals(world.containing("LHR"), world.containing("JNB"))
        assertNotEquals(world.containing("HND"), world.containing("SYD"))
    }

    @Test
    fun zoomedInEveryAirportIsItsOwnPill() {
        val close = clusters(worldAt(10.0))
        assertEquals(markers.size, close.size)
        assertTrue(close.all { it.isSingle })
    }

    @Test
    fun noTwoPillsOverlapAtAnyZoom() {
        for (step in 0..20) {
            val world = worldAt(step / 2.0)
            val pills = clusters(world)
            for (i in pills.indices) {
                for (j in i + 1 until pills.size) {
                    val a = pills[i].center
                    val b = pills[j].center
                    val dx = abs(mercatorX(a.longitude) - mercatorX(b.longitude)) * world
                    val dy = abs(mercatorY(a.latitude) - mercatorY(b.latitude)) * world
                    assertTrue(
                        minOf(dx, world - dx) >= pillWidth || dy >= pillHeight,
                        "${pills[i].id} and ${pills[j].id} overlap at zoom ${step / 2.0}",
                    )
                }
            }
        }
    }

    @Test
    fun everyAirportLandsInExactlyOnePillAndNoFlightIsLost() {
        for (zoom in listOf(0.0, 2.0, 4.0)) {
            val pills = clusters(worldAt(zoom))
            assertEquals(markers.map { it.airport }.sorted(), pills.flatMap { pill -> pill.markers.map { it.airport } }.sorted())
            assertEquals(fixtureFlights.size, pills.sumOf { it.flightCount })
            pills.forEach { pill -> assertEquals(pill.markers.minOf { it.lowestPoints }, pill.lowestPoints) }
        }
    }

    @Test
    fun aSinglePillKeepsTheMarkersTitleAndLabel() {
        val haneda = clusters(worldAt(10.0)).single { it.id == "HND" }
        assertEquals("HND", haneda.title)
        assertEquals("55 · 16.5k", haneda.label)
        assertEquals(55, haneda.flightCount)
    }

    @Test
    fun aClusterSaysHowManyAirportsWithTheirFlightsAndLowestPoints() {
        val tokyo = listOf(
            marker("NRT", 35.77, 140.39, count = 55, lowest = 19_500, saved = true),
            marker("HND", 35.55, 139.79, count = 55, lowest = 16_500),
        )
        val pill = clusters(worldAt(0.0), tokyo).single()
        assertEquals("HND", pill.id, "ties go to the first code, so ids are stable")
        assertEquals("2 airports", pill.title)
        assertEquals("110 · 16.5k", pill.label)
        assertEquals(listOf("HND", "NRT"), pill.markers.map { it.airport })
        assertTrue(pill.hasSaved)
    }

    @Test
    fun neighboursAcrossTheAntimeridianMerge() {
        val pacific = listOf(
            marker("EAST", 0.0, 179.8, count = 3, lowest = 10_000),
            marker("WEST", 0.0, -179.8, count = 2, lowest = 12_000),
        )
        assertEquals(1, clusters(worldAt(0.0), pacific).size)
    }

    @Test
    fun anUnknownZoomClustersNothing() {
        assertEquals(markers.size, clusters(0.0).size)
        assertEquals(emptyList(), clusters(worldAt(0.0), emptyList()))
    }

    @Test
    fun theSameZoomAlwaysGivesTheSamePills() {
        assertEquals(clusters(worldAt(1.5)), clusters(worldAt(1.5)))
    }
}
