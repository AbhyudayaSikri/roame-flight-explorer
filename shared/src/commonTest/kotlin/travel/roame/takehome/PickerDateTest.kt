package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * iOS's DatePicker hands over the instant of local midnight in the device's time zone; these
 * conversions turn it into the flight's calendar date and back.
 */
class PickerDateTest {
    private val zones = listOf(
        "UTC",
        "America/New_York",
        "America/Los_Angeles",
        "America/St_Johns", // -3:30
        "Asia/Tokyo",
        "Asia/Kathmandu", // +5:45
        "Australia/Lord_Howe", // +10:30, half-hour daylight saving
        "Pacific/Kiritimati", // +14
        "Pacific/Pago_Pago", // -11
        "Europe/London",
    )

    private val days: List<CalendarDate> = buildList {
        var day = fixtureBounds.firstDate
        while (day <= fixtureBounds.lastDate) {
            add(day)
            day = CalendarDate.fromUtcMillis(day.toUtcMillis() + 86_400_000)
        }
        // Daylight-saving changes: US Mar 14, Europe Mar 28, Lord Howe Apr 4 (all 2027).
        add(date("2027-03-14"))
        add(date("2027-03-28"))
        add(date("2027-04-04"))
    }

    @Test
    fun everyDayRoundTripsInEveryZone() {
        for (zone in zones) {
            for (day in days) {
                assertEquals(day, CalendarDate.fromEpochMillis(day.startOfDayMillis(zone), zone), "$day in $zone")
            }
        }
    }

    @Test
    fun theLastMillisecondOfADayIsStillThatDay() {
        for (zone in zones) {
            for (day in days.dropLast(3)) {
                val next = CalendarDate.fromUtcMillis(day.toUtcMillis() + 86_400_000)
                assertEquals(day, CalendarDate.fromEpochMillis(next.startOfDayMillis(zone) - 1, zone), "end of $day in $zone")
            }
        }
    }

    @Test
    fun localMidnightEastOfUtcIsNotTheDayBefore() {
        // Feb 1 00:00 in Tokyo is Jan 31 15:00 UTC: reading it as UTC would be off by one day.
        val tokyoMidnight = date("2027-02-01").startOfDayMillis("Asia/Tokyo")
        assertEquals(date("2027-01-31"), CalendarDate.fromUtcMillis(tokyoMidnight))
        assertEquals(date("2027-02-01"), CalendarDate.fromEpochMillis(tokyoMidnight, "Asia/Tokyo"))
    }

    @Test
    fun localMidnightWestOfUtcIsNotTheDayAfter() {
        // Apr 1 00:00 in Pago Pago is Apr 1 11:00 UTC, and 23:59 there is already Apr 2 in UTC.
        val lastMoment = date("2027-04-02").startOfDayMillis("Pacific/Pago_Pago") - 1
        assertEquals(date("2027-04-02"), CalendarDate.fromUtcMillis(lastMoment))
        assertEquals(date("2027-04-01"), CalendarDate.fromEpochMillis(lastMoment, "Pacific/Pago_Pago"))
    }

    @Test
    fun anUnknownZoneFallsBackToUtc() {
        val utcMidnight = date("2027-02-01").toUtcMillis()
        assertEquals(utcMidnight, date("2027-02-01").startOfDayMillis("Not/A_Zone"))
        assertEquals(date("2027-02-01"), CalendarDate.fromEpochMillis(utcMidnight, "Not/A_Zone"))
    }
}
