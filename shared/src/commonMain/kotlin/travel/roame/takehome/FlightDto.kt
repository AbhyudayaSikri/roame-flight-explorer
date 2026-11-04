package travel.roame.takehome

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class FlightsEnvelope(val flights: List<FlightDto>)

@Serializable
internal data class FlightDto(
    val id: String,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val program: String,
    val points: Int,
    val cabin: String,
    val originCity: String,
    val destinationCity: String,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
)

internal fun FlightDto.toFlight(): Flight = Flight(
    id = id,
    origin = origin,
    destination = destination,
    originCity = originCity,
    destinationCity = destinationCity,
    departureDate = CalendarDate.parse(departureDate),
    program = program,
    points = points,
    cabin = cabin,
    originPoint = GeoPoint(originLatitude, originLongitude),
    destinationPoint = GeoPoint(destinationLatitude, destinationLongitude),
)

private val flightsJson = Json { ignoreUnknownKeys = true }

/**
 * Decodes the `GET /flights` envelope. One invalid record fails the whole response, so a partial
 * list never replaces the cache. Throws [IllegalArgumentException] (including SerializationException).
 */
internal fun decodeFlights(json: String): List<Flight> =
    flightsJson.decodeFromString<FlightsEnvelope>(json).flights.map { it.toFlight() }
