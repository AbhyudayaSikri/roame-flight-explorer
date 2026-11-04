package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterTest {
    @Test
    fun aSingleDayRangeIncludesBothEnds() {
        val firstDay = filter(FlightQuery(dateRange = DateRange(date("2027-02-01"), date("2027-02-01"))))
        val lastDay = filter(FlightQuery(dateRange = DateRange(date("2027-04-01"), date("2027-04-01"))))

        assertEquals(17, firstDay.size)
        assertTrue(firstDay.all { it.departureDate == date("2027-02-01") })
        assertEquals(21, lastDay.size)
        assertTrue(lastDay.all { it.departureDate == date("2027-04-01") })
    }

    @Test
    fun rangeBoundariesAreInclusive() {
        val february = filter(FlightQuery(dateRange = DateRange(date("2027-02-01"), date("2027-02-28"))))
        val expected = fixtureFlights.count { it.departureDate.month == 2 }

        assertEquals(expected, february.size)
        assertTrue(february.any { it.departureDate == date("2027-02-01") })
        assertTrue(february.any { it.departureDate == date("2027-02-28") })
    }

    @Test
    fun aRangeOutsideTheDataIsEmptyAndTheFullRangeIsEverything() {
        assertEquals(0, filter(FlightQuery(dateRange = DateRange(date("2027-05-01"), date("2027-05-31")))).size)
        assertEquals(1000, filter(FlightQuery(dateRange = fixtureBounds.dateRange)).size)
    }

    @Test
    fun maxPointsIncludesTheLimit() {
        val atCheapest = filter(FlightQuery(maxPoints = 9000))

        assertEquals(listOf("fx-LAX-MEX-20270218"), atCheapest.map { it.id })
        assertEquals(0, filter(FlightQuery(maxPoints = 8500)).size)
        assertEquals(999, filter(FlightQuery(maxPoints = 132_999)).size)
        assertEquals(1000, filter(FlightQuery(maxPoints = 133_000)).size)
        assertEquals(1000, filter(FlightQuery(maxPoints = null)).size)
    }

    @Test
    fun cabinMatchesExactly() {
        val economy = filter(FlightQuery(cabin = "Economy"))

        assertEquals(467, economy.size)
        assertTrue(economy.none { it.cabin == "Premium Economy" })
        assertEquals(344, filter(FlightQuery(cabin = "Business")).size)
        assertEquals(189, filter(FlightQuery(cabin = "Premium Economy")).size)
    }

    @Test
    fun programsCombineWithOrAndMatchExactly() {
        val mileagePlan = filter(FlightQuery(programs = setOf("Mileage Plan")))
        val two = filter(FlightQuery(programs = setOf("Avios", "Aeroplan")))

        assertEquals(203, mileagePlan.size)
        assertTrue(mileagePlan.none { it.program == "MileagePlus" })
        assertEquals(386, two.size)
        assertTrue(two.all { it.program == "Avios" || it.program == "Aeroplan" })
        assertEquals(1000, filter(FlightQuery(programs = emptySet())).size)
    }

    @Test
    fun differentFilterTypesCombineWithAnd() {
        val results = filter(FlightQuery(cabin = "Business", programs = setOf("Avios"), maxPoints = 60_000))

        assertEquals(32, results.size)
        assertTrue(results.all { it.cabin == "Business" && it.program == "Avios" && it.points <= 60_000 })
    }

    @Test
    fun resultsAreOrderedByDateThenPointsThenId() {
        val results = filter(FlightQuery())

        assertEquals(results.sortedWith(flightOrder), results)
        results.zipWithNext().forEach { (a, b) ->
            assertTrue(a.departureDate < b.departureDate || (a.departureDate == b.departureDate && a.points <= b.points))
        }
    }

    @Test
    fun boundsDescribeTheFixture() {
        assertEquals(date("2027-02-01"), fixtureBounds.firstDate)
        assertEquals(date("2027-04-01"), fixtureBounds.lastDate)
        assertEquals(9000, fixtureBounds.minPoints)
        assertEquals(133_000, fixtureBounds.maxPoints)
        assertEquals(listOf("Economy", "Premium Economy", "Business"), fixtureBounds.cabins)
        assertEquals(listOf("Aeroplan", "Avios", "Flying Blue", "Mileage Plan", "MileagePlus"), fixtureBounds.programs)
        assertEquals(32, fixtureBounds.airportCodes.size)
        assertEquals(null, DataBounds.of(emptyList()))
    }
}
