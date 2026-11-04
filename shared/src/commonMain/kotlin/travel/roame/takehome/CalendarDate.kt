package travel.roame.takehome

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private const val MILLIS_PER_DAY = 86_400_000L

/** A day on the calendar with no time zone, e.g. a flight's departure date. */
data class CalendarDate(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDate> {
    init {
        requireNotNull(LocalDate.orNull(year, month, day)) { "Not a calendar date: $year-$month-$day" }
    }

    override fun compareTo(other: CalendarDate): Int =
        compareValuesBy(this, other, CalendarDate::year, CalendarDate::month, CalendarDate::day)

    /** "2027-02-01" */
    val isoString: String
        get() = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    /** Midnight UTC of this day, the representation Android's DateRangePicker uses. */
    fun toUtcMillis(): Long = toLocalDate().toEpochDays() * MILLIS_PER_DAY

    companion object {
        fun fromUtcMillis(utcMillis: Long): CalendarDate =
            LocalDate.fromEpochDays(utcMillis.floorDiv(MILLIS_PER_DAY)).toCalendarDate()

        fun parse(isoString: String): CalendarDate = LocalDate.parse(isoString).toCalendarDate()
    }
}

internal fun CalendarDate.toLocalDate(): LocalDate = LocalDate(year = year, month = month, day = day)

internal fun LocalDate.toCalendarDate(): CalendarDate = CalendarDate(year, month.number, day)

data class DateRange(val start: CalendarDate, val end: CalendarDate) {
    init {
        require(start <= end) { "Date range starts after it ends: $start..$end" }
    }

    operator fun contains(date: CalendarDate): Boolean = date >= start && date <= end
}
