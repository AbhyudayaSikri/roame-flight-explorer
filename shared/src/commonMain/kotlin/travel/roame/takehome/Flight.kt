package travel.roame.takehome

data class GeoPoint(val latitude: Double, val longitude: Double)

data class Flight(
    val id: String,
    val origin: String,
    val destination: String,
    val originCity: String,
    val destinationCity: String,
    val departureDate: CalendarDate,
    val program: String,
    val points: Int,
    val cabin: String,
    val originPoint: GeoPoint,
    val destinationPoint: GeoPoint,
) {
    /** "16,500 pts" */
    val pointsLabel: String = formatPoints(points)

    /** "Mon, Feb 1, 2027" */
    val dateLabel: String = formatDate(departureDate)

    internal val originCityKey: String = normalizeSearch(originCity)
    internal val destinationCityKey: String = normalizeSearch(destinationCity)
}

data class SavedFlight(val flight: Flight, val savedAtMillis: Long)
