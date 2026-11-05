package travel.roame.takehome

import kotlinx.coroutines.flow.Flow
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException

internal sealed interface RefreshResult {
    data class Success(val count: Int) : RefreshResult
    data class Failure(val error: RefreshError) : RefreshResult
}

/** The cached flights and when they were fetched, read together so neither is newer than the other. */
internal data class CachedFlights(val flights: List<Flight>, val lastSuccessAtMillis: Long?)

internal interface FlightRepository {
    val cache: Flow<CachedFlights>
    val savedFlights: Flow<List<SavedFlight>>

    suspend fun refresh(baseUrl: String): RefreshResult
    suspend fun save(flight: Flight): Result<Unit>
    suspend fun remove(flightId: String): Result<Unit>
    suspend fun clearAll(): Result<Unit>

    /** The server address chosen in a debug menu, or null for the build default (or when it can't be read). */
    suspend fun savedServerUrl(): String?

    /** Remembers [url]; null forgets it, so the build default applies again. */
    suspend fun saveServerUrl(url: String?): Result<Unit>
}

internal class DefaultFlightRepository(
    private val source: FlightsSource,
    private val database: FlightDatabaseStore,
    private val now: () -> Long,
) : FlightRepository {
    override val cache get() = database.cache
    override val savedFlights get() = database.savedFlights

    override suspend fun refresh(baseUrl: String): RefreshResult {
        val flights = try {
            source.fetchFlights(baseUrl)
        } catch (e: RefreshFailure) {
            return RefreshResult.Failure(e.error)
        }
        return when (attempt { database.replaceCache(flights, now()) }.isSuccess) {
            true -> RefreshResult.Success(flights.size)
            false -> RefreshResult.Failure(RefreshError.Storage)
        }
    }

    override suspend fun save(flight: Flight) = attempt { database.save(flight, now()) }

    override suspend fun remove(flightId: String) = attempt { database.remove(flightId) }

    override suspend fun clearAll() = attempt { database.clearAll() }

    override suspend fun savedServerUrl(): String? = try {
        database.serverUrl()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    override suspend fun saveServerUrl(url: String?) = attempt { database.setServerUrl(url) }
}

/** Like runCatching, but never swallows cancellation. */
internal suspend inline fun attempt(crossinline block: suspend () -> Unit): Result<Unit> = try {
    block()
    Result.success(Unit)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/** Debug-only switches behind DebugOptions and the Android debug menu. */
internal class FaultInjection(failSaves: Boolean, failRemoves: Boolean) {
    @Volatile var failSaves: Boolean = failSaves
    @Volatile var failRemoves: Boolean = failRemoves
}

internal class FaultInjectingRepository(
    private val delegate: FlightRepository,
    private val faults: FaultInjection,
) : FlightRepository by delegate {
    override suspend fun save(flight: Flight): Result<Unit> =
        if (faults.failSaves) Result.failure(IllegalStateException("Simulated save failure")) else delegate.save(flight)

    override suspend fun remove(flightId: String): Result<Unit> =
        if (faults.failRemoves) Result.failure(IllegalStateException("Simulated remove failure")) else delegate.remove(flightId)
}
