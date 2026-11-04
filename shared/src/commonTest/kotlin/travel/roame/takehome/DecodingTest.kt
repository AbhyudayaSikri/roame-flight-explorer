package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecodingTest {
    @Test
    fun decodesEveryFixtureRecordWithUniqueIds() {
        assertEquals(1000, fixtureFlights.size)
        assertEquals(1000, fixtureFlights.map { it.id }.toSet().size)
    }

    @Test
    fun mapsEveryField() {
        assertEquals(
            Flight(
                id = "fx-BOS-BKK-20270201",
                origin = "BOS",
                destination = "BKK",
                originCity = "Boston",
                destinationCity = "Bangkok",
                departureDate = CalendarDate(2027, 2, 1),
                program = "Mileage Plan",
                points = 39000,
                cabin = "Economy",
                originPoint = GeoPoint(42.36197, -71.0079),
                destinationPoint = GeoPoint(13.6811, 100.747),
            ),
            fixtureFlights.first { it.id == "fx-BOS-BKK-20270201" },
        )
    }

    @Test
    fun idsThatBreakThePatternDecodeLikeAnyOther() {
        val odd = fixtureFlights.filterNot { it.id.startsWith("fx-") }

        assertEquals(listOf("f101", "f102", "f103", "f104", "f105", "f106"), odd.map { it.id }.sorted())
        val f101 = odd.first { it.id == "f101" }
        assertEquals(listOf("SFO", "NRT", "Tokyo"), listOf(f101.origin, f101.destination, f101.destinationCity))
        assertEquals(CalendarDate(2027, 2, 12), f101.departureDate)
    }

    @Test
    fun ignoresUnknownKeys() {
        val flights = decodeFlights("""{"version":2,"flights":[${record(extra = ""","seats":4""")}]}""")

        assertEquals(listOf("x1"), flights.map { it.id })
    }

    @Test
    fun decodesAnEmptyEnvelope() {
        assertEquals(emptyList(), decodeFlights("""{"flights":[]}"""))
    }

    @Test
    fun rejectsMalformedJson() {
        assertFailsWith<IllegalArgumentException> { decodeFlights("""{"flights":[""") }
        assertFailsWith<IllegalArgumentException> { decodeFlights("<html>500</html>") }
    }

    @Test
    fun oneInvalidRecordFailsTheWholeResponse() {
        assertFailsWith<IllegalArgumentException> {
            decodeFlights("""{"flights":[${record()},${record(date = "2027-02-30")}]}""")
        }
        assertFailsWith<IllegalArgumentException> {
            decodeFlights("""{"flights":[${record().replace(""""points":9000,""", "")}]}""")
        }
    }

    private fun record(date: String = "2027-02-01", extra: String = "") =
        """{"id":"x1","origin":"JFK","destination":"LHR","departureDate":"$date","program":"Avios",""" +
            """"points":9000,"cabin":"Economy","originCity":"New York","destinationCity":"London",""" +
            """"originLatitude":40.6,"originLongitude":-73.8,"destinationLatitude":51.5,"destinationLongitude":-0.5$extra}"""
}
