package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTest {
    private fun origins(text: String) = filter(FlightQuery(originText = text))

    private fun destinations(text: String) = filter(FlightQuery(destinationText = text))

    @Test
    fun originCodeIgnoresCaseAndSurroundingWhitespace() {
        val results = origins("  jfk ")

        assertEquals(113, results.size)
        assertTrue(results.all { it.origin == "JFK" })
    }

    @Test
    fun originCityMatchesTheSameFlights() {
        assertEquals(origins("JFK"), origins("new york"))
        assertEquals(origins("JFK"), origins("  New   YORK  "))
    }

    @Test
    fun destinationCityMatchesEveryAirportInIt() {
        val results = destinations("TOKYO")

        assertEquals(110, results.size)
        assertEquals(setOf("HND", "NRT"), results.map { it.destination }.toSet())
    }

    @Test
    fun originAndDestinationCombineWithAnd() {
        val results = filter(FlightQuery(originText = "jfk", destinationText = "tokyo"))

        assertEquals(7, results.size)
        assertTrue(results.all { it.origin == "JFK" && it.destination in setOf("HND", "NRT") })
    }

    @Test
    fun anExactAirportCodeWinsOverCityPrefixes() {
        assertEquals(setOf("DUB"), destinations("dub").map { it.destination }.toSet())
        assertEquals(29, destinations("dub").size)
        assertEquals(0, origins("fra").size)
    }

    @Test
    fun otherTextMatchesCodePrefixesAndCityWordPrefixes() {
        assertEquals(69, destinations("du").size)
        assertEquals(setOf("DUB", "DXB"), destinations("du").map { it.destination }.toSet())
        assertEquals(origins("JFK"), origins("jf"))
        assertEquals(123, origins("fran").size)
        assertTrue(origins("fran").all { it.origin == "SFO" })
        assertEquals(origins("JFK"), origins("york"))
        assertEquals(origins("JFK"), origins("new y"))
        assertEquals(setOf("LAX"), origins("an").map { it.origin }.toSet())
        assertEquals(110, destinations("tok").size)
    }

    @Test
    fun textInsideAWordDoesNotMatch() {
        assertEquals(0, origins("ork").size)
        assertEquals(0, origins("fk").size)
        assertEquals(0, destinations("ao").size)
    }

    @Test
    fun noMatchGivesNoResults() {
        assertEquals(0, origins("xyz").size)
    }

    @Test
    fun blankTextIsNoConstraint() {
        assertEquals(1000, origins("   ").size)
        assertEquals(1000, filter(FlightQuery()).size)
    }
}
