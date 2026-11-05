package travel.roame.takehome

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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

    /**
     * The instant this day starts in [timeZoneId] (an IANA id such as "Asia/Tokyo"), as epoch
     * millis: what iOS's DatePicker shows for this day in the device's zone. Unknown ids count as UTC.
     */
    @OptIn(ExperimentalTime::class)
    fun startOfDayMillis(timeZoneId: String): Long =
        toLocalDate().atStartOfDayIn(zoneOrUtc(timeZoneId)).toEpochMilliseconds()

    companion object {
        fun fromUtcMillis(utcMillis: Long): CalendarDate =
            LocalDate.fromEpochDays(utcMillis.floorDiv(MILLIS_PER_DAY)).toCalendarDate()

        /** The calendar day that [epochMillis] falls on in [timeZoneId]; unknown ids count as UTC. */
        @OptIn(ExperimentalTime::class)
        fun fromEpochMillis(epochMillis: Long, timeZoneId: String): CalendarDate =
            Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zoneOrUtc(timeZoneId)).date.toCalendarDate()

        fun parse(isoString: String): CalendarDate = LocalDate.parse(isoString).toCalendarDate()
    }
}

// Never throws: an exception crossing into Swift would crash the app.
private fun zoneOrUtc(timeZoneId: String): TimeZone = try {
    TimeZone.of(timeZoneId)
} catch (_: IllegalTimeZoneException) {
    TimeZone.UTC
}

internal fun CalendarDate.toLocalDate(): LocalDate = LocalDate(year = year, month = month, day = day)

internal fun LocalDate.toCalendarDate(): CalendarDate = CalendarDate(year, month.number, day)

data class DateRange(val start: CalendarDate, val end: CalendarDate) {
    init {
        require(start <= end) { "Date range starts after it ends: $start..$end" }
    }

    operator fun contains(date: CalendarDate): Boolean = date >= start && date <= end
}
