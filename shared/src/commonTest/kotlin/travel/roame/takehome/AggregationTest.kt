package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AggregationTest {
    @Test
    fun oneMarkerPerDestinationAirportWithCountAndLowestPoints() {
        val markers = aggregateByDestination(fixtureFlights, emptySet())

        assertEquals(24, markers.size)
        assertEquals(1000, markers.sumOf { it.count })
        assertEquals(markers.sortedBy { it.airport }, markers)
        val hnd = markers.single { it.airport == "HND" }
        val nrt = markers.single { it.airport == "NRT" }
        assertEquals(listOf(55, 16_500), listOf(hnd.count, hnd.lowestPoints))
        assertEquals(listOf(55, 19_500), listOf(nrt.count, nrt.lowestPoints))
        assertEquals("55 · 16.5k", hnd.label)
        assertEquals("Tokyo", hnd.city)
        assertEquals(GeoPoint(35.54968, 139.78696), hnd.point)
    }

    @Test
    fun markersFollowTheFilteredResults() {
        val results = filter(FlightQuery(destinationText = "tokyo", cabin = "Business"))
        val markers = aggregateByDestination(results, emptySet())

        assertEquals(setOf("HND", "NRT"), markers.map { it.airport }.toSet())
        assertEquals(results.size, markers.sumOf { it.count })
        markers.forEach { marker ->
            assertEquals(results.filter { it.destination == marker.airport }.minOf { it.points }, marker.lowestPoints)
        }
    }

    @Test
    fun noResultsMeansNoMarkers() {
        assertEquals(emptyList(), aggregateByDestination(emptyList(), emptySet()))
    }

    @Test
    fun markersShowWhetherAnyOfTheirFlightsIsSaved() {
        val savedHnd = fixtureFlights.first { it.destination == "HND" }.id
        val markers = aggregateByDestination(fixtureFlights, setOf(savedHnd))

        assertTrue(markers.single { it.airport == "HND" }.hasSaved)
        assertTrue(markers.filter { it.airport != "HND" }.none { it.hasSaved })
    }
}
