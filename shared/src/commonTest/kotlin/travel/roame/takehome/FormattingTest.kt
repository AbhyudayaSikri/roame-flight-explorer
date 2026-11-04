package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattingTest {
    @Test
    fun fullPointsGroupThousands() {
        assertEquals("9,000 pts", formatPoints(9000))
        assertEquals("16,500 pts", formatPoints(16_500))
        assertEquals("133,000 pts", formatPoints(133_000))
        assertEquals("500 pts", formatPoints(500))
    }

    @Test
    fun compactPointsKeepHalfThousands() {
        assertEquals("16.5k", formatPointsCompact(16_500))
        assertEquals("19.5k", formatPointsCompact(19_500))
        assertEquals("9k", formatPointsCompact(9000))
        assertEquals("133k", formatPointsCompact(133_000))
        assertEquals("950", formatPointsCompact(950))
        assertEquals("1,250k", formatPointsCompact(1_250_000))
    }

    @Test
    fun datesUseShortEnglishNames() {
        assertEquals("Mon, Feb 1, 2027", formatDate(CalendarDate(2027, 2, 1)))
        assertEquals("Thu, Apr 1, 2027", formatDate(CalendarDate(2027, 4, 1)))
        assertEquals("Feb 1", formatDateRange(DateRange(CalendarDate(2027, 2, 1), CalendarDate(2027, 2, 1))))
        assertEquals("Feb 1 – Mar 31", formatDateRange(DateRange(CalendarDate(2027, 2, 1), CalendarDate(2027, 3, 31))))
    }

    @Test
    fun flightsCarryTheirLabels() {
        val flight = fixtureFlights.first { it.id == "fx-LAX-MEX-20270218" }

        assertEquals("9,000 pts", flight.pointsLabel)
        assertEquals("Thu, Feb 18, 2027", flight.dateLabel)
    }
}
