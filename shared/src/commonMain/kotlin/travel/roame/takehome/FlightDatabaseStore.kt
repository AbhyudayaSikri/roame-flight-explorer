package travel.roame.takehome

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import travel.roame.takehome.db.FlightDatabase

private const val LAST_SUCCESS_AT = "last_success_at"
private const val SERVER_URL = "server_url"

/** SQLDelight access. Every read and write runs on [io], never on the caller's thread. */
internal class FlightDatabaseStore(driver: SqlDriver, private val io: CoroutineDispatcher) {
    private val database = FlightDatabase(driver)
    private val queries = database.flightsQueries

    private val cachedFlightsQuery = queries
        .selectCachedFlights { id, origin, destination, originCity, destinationCity, departureDate, program, points, cabin,
                               originLatitude, originLongitude, destinationLatitude, destinationLongitude ->
            Flight(
                id = id,
                origin = origin,
                destination = destination,
                originCity = originCity,
                destinationCity = destinationCity,
                departureDate = CalendarDate.parse(departureDate),
                program = program,
                points = points.toInt(),
                cabin = cabin,
                originPoint = GeoPoint(originLatitude, originLongitude),
                destinationPoint = GeoPoint(destinationLatitude, destinationLongitude),
            )
        }
    private val lastSuccessQuery = queries.selectMeta(LAST_SUCCESS_AT)

    /**
     * Re-read after every change to either table, both in one transaction. Two separate reads after
     * [replaceCache] could land in either order, and a new timestamp with the old, empty cache looks
     * like "the server has no flights".
     */
    val cache: Flow<CachedFlights> = combine(cachedFlightsQuery.asFlow(), lastSuccessQuery.asFlow()) { _, _ -> }
        .conflate()
        .map {
            withContext(io) {
                database.transactionWithResult {
                    CachedFlights(cachedFlightsQuery.executeAsList(), lastSuccessQuery.executeAsOneOrNull()?.toLongOrNull())
                }
            }
        }

    val savedFlights: Flow<List<SavedFlight>> = queries
        .selectSavedFlights { id, origin, destination, originCity, destinationCity, departureDate, program, points, cabin,
                              originLatitude, originLongitude, destinationLatitude, destinationLongitude, savedAt ->
            SavedFlight(
                flight = Flight(
                    id = id,
                    origin = origin,
                    destination = destination,
                    originCity = originCity,
                    destinationCity = destinationCity,
                    departureDate = CalendarDate.parse(departureDate),
                    program = program,
                    points = points.toInt(),
                    cabin = cabin,
                    originPoint = GeoPoint(originLatitude, originLongitude),
                    destinationPoint = GeoPoint(destinationLatitude, destinationLongitude),
                ),
                savedAtMillis = savedAt,
            )
        }
        .asFlow()
        .mapToList(io)

    /** The latest successful response replaces the whole cache, even when it's empty. */
    suspend fun replaceCache(flights: List<Flight>, atMillis: Long) = withContext(io) {
        database.transaction {
            queries.deleteCachedFlights()
            flights.forEach { f ->
                queries.insertCachedFlight(
                    f.id, f.origin, f.destination, f.originCity, f.destinationCity, f.departureDate.isoString,
                    f.program, f.points.toLong(), f.cabin, f.originPoint.latitude, f.originPoint.longitude,
                    f.destinationPoint.latitude, f.destinationPoint.longitude,
                )
            }
            queries.upsertMeta(LAST_SUCCESS_AT, atMillis.toString())
        }
    }

    /** Upsert by id: saving the same flight again replaces its snapshot instead of adding a row. */
    suspend fun save(f: Flight, atMillis: Long) = withContext(io) {
        queries.upsertSavedFlight(
            f.id, f.origin, f.destination, f.originCity, f.destinationCity, f.departureDate.isoString,
            f.program, f.points.toLong(), f.cabin, f.originPoint.latitude, f.originPoint.longitude,
            f.destinationPoint.latitude, f.destinationPoint.longitude, atMillis,
        )
    }

    suspend fun remove(flightId: String) = withContext(io) {
        queries.deleteSavedFlight(flightId)
    }

    suspend fun serverUrl(): String? = withContext(io) { queries.selectMeta(SERVER_URL).executeAsOneOrNull() }

    suspend fun setServerUrl(url: String?) = withContext(io) {
        if (url == null) queries.deleteMetaKey(SERVER_URL) else queries.upsertMeta(SERVER_URL, url)
    }

    /** Everything, including the saved server address: like a fresh install. */
    suspend fun clearAll() = withContext(io) {
        database.transaction {
            queries.deleteCachedFlights()
            queries.deleteSavedFlights()
            queries.deleteMeta()
        }
    }
}
