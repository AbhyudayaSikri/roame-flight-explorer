package travel.roame.takehome

import kotlinx.datetime.isoDayNumber

// English only, shared so both apps show identical text (docs/NOTES.md §2.2).
private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** 16500 -> "16,500" */
internal fun formatThousands(value: Int): String {
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (value < 0) "-$grouped" else grouped
}

/** 16500 -> "16,500 pts" */
fun formatPoints(points: Int): String = "${formatThousands(points)} pts"

/**
 * What a screen reader says for a flight: words instead of "→" and "pts", so VoiceOver and TalkBack
 * don't read out "right arrow". "LAX to MEX, Los Angeles to Mexico City, Thu, Feb 18, 2027, Economy,
 * MileagePlus, 9,000 points, saved".
 */
fun spokenFlight(flight: Flight, saved: Boolean): String = buildString {
    append("${spokenRoute(flight)}, ${flight.originCity} to ${flight.destinationCity}, ")
    append("${flight.dateLabel}, ${flight.cabin}, ${flight.program}, ${formatThousands(flight.points)} points")
    if (saved) append(", saved")
}

/** "LAX to MEX" */
fun spokenRoute(flight: Flight): String = "${flight.origin} to ${flight.destination}"

/** 1 -> "1 flight", 1000 -> "1,000 flights" */
fun formatFlightCount(count: Int): String = if (count == 1) "1 flight" else "${formatThousands(count)} flights"

/** Marker-sized points, rounded to one decimal: 16500 -> "16.5k", 9000 -> "9k", 950 -> "950". */
fun formatPointsCompact(points: Int): String {
    if (points < 1000) return points.toString()
    val tenths = (points + 50) / 100
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0) "${formatThousands(whole)}k" else "${formatThousands(whole)}.${fraction}k"
}

/** "Mon, Feb 1, 2027" */
fun formatDate(date: CalendarDate): String {
    val weekday = weekdays[date.toLocalDate().dayOfWeek.isoDayNumber - 1]
    return "$weekday, ${formatDateShort(date)}, ${date.year}"
}

/** "Feb 1" */
fun formatDateShort(date: CalendarDate): String = "${months[date.month - 1]} ${date.day}"

/** "Feb 1 – Feb 28", or "Feb 1" for a single day. */
fun formatDateRange(range: DateRange): String =
    if (range.start == range.end) formatDateShort(range.start)
    else "${formatDateShort(range.start)} – ${formatDateShort(range.end)}"
