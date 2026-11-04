package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveFiltersTest {
    @Test
    fun theDefaultQueryHasNoChips() {
        assertEquals(emptyList(), activeFilters(FlightQuery(), fixtureBounds))
    }

    @Test
    fun filtersThatDoNotNarrowTheDataHaveNoChip() {
        val query = FlightQuery(originText = "   ", dateRange = fixtureBounds.dateRange, maxPoints = fixtureBounds.maxPoints)

        assertEquals(emptyList(), activeFilters(query, fixtureBounds))
    }

    @Test
    fun oneChipPerActiveFilterInAFixedOrder() {
        val query = FlightQuery(
            originText = " jfk ",
            destinationText = "Tokyo",
            dateRange = DateRange(CalendarDate(2027, 2, 1), CalendarDate(2027, 2, 28)),
            cabin = "Business",
            maxPoints = 60_000,
            programs = setOf("Avios", "Aeroplan"),
        )

        assertEquals(
            listOf(
                ActiveFilter(FilterKind.ORIGIN, "From: jfk"),
                ActiveFilter(FilterKind.DESTINATION, "To: Tokyo"),
                ActiveFilter(FilterKind.DATES, "Feb 1 – Feb 28"),
                ActiveFilter(FilterKind.CABIN, "Business"),
                ActiveFilter(FilterKind.MAX_POINTS, "≤ 60,000 pts"),
                ActiveFilter(FilterKind.PROGRAMS, "Aeroplan, Avios"),
            ),
            activeFilters(query, fixtureBounds),
        )
    }
}
