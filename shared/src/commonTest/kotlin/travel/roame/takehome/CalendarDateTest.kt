package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CalendarDateTest {
    @Test
    fun parsesAndPrintsIsoDates() {
        assertEquals(CalendarDate(2027, 2, 1), CalendarDate.parse("2027-02-01"))
        assertEquals("2027-02-01", CalendarDate(2027, 2, 1).isoString)
    }

    @Test
    fun rejectsDaysThatDoNotExist() {
        assertFailsWith<IllegalArgumentException> { CalendarDate(2027, 2, 29) }
        assertFailsWith<IllegalArgumentException> { CalendarDate(2027, 13, 1) }
        assertFailsWith<IllegalArgumentException> { CalendarDate.parse("2027-02-30") }
    }

    @Test
    fun ordersChronologically() {
        assertTrue(CalendarDate(2027, 2, 28) < CalendarDate(2027, 3, 1))
        assertTrue(CalendarDate(2026, 12, 31) < CalendarDate(2027, 1, 1))
    }

    @Test
    fun convertsToAndFromUtcMidnightMillis() {
        assertEquals(1_801_440_000_000, CalendarDate(2027, 2, 1).toUtcMillis())
        assertEquals(CalendarDate(2027, 2, 1), CalendarDate.fromUtcMillis(1_801_440_000_000))
        assertEquals(CalendarDate(2027, 2, 1), CalendarDate.fromUtcMillis(1_801_440_000_000 + 86_399_999))
        assertEquals(CalendarDate(1969, 12, 31), CalendarDate.fromUtcMillis(-1))
        fixtureBounds.dateRange.let { range ->
            assertEquals(range.start, CalendarDate.fromUtcMillis(range.start.toUtcMillis()))
            assertEquals(range.end, CalendarDate.fromUtcMillis(range.end.toUtcMillis()))
        }
    }

    @Test
    fun rangesContainBothEnds() {
        val range = DateRange(CalendarDate(2027, 2, 1), CalendarDate(2027, 2, 3))

        assertTrue(CalendarDate(2027, 2, 1) in range)
        assertTrue(CalendarDate(2027, 2, 3) in range)
        assertTrue(CalendarDate(2027, 2, 4) !in range)
        assertFailsWith<IllegalArgumentException> { DateRange(CalendarDate(2027, 2, 3), CalendarDate(2027, 2, 1)) }
    }
}
