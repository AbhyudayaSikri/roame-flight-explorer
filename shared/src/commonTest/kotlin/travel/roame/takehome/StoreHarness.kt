package travel.roame.takehome

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext

/** A FlightsSource that answers with whatever the test sets, optionally held open by [gate]. */
internal class FakeFlightsSource : FlightsSource {
    private var response: () -> List<Flight> = { fixtureFlights }
    var gate: CompletableDeferred<Unit>? = null
    var calls = 0
        private set

    /** The server each fetch went to, in order. */
    val baseUrls = mutableListOf<String>()

    fun serve(flights: List<Flight>) {
        response = { flights }
    }

    fun fail(error: RefreshError) {
        response = { throw RefreshFailure(error) }
    }

    override suspend fun fetchFlights(baseUrl: String): List<Flight> {
        calls += 1
        baseUrls += baseUrl
        gate?.await()
        return response()
    }
}

/** Real repository and in-memory SQLite, with every dispatcher on the test scheduler. */
internal class StoreHarness(
    private val test: TestScope,
    val source: FakeFlightsSource = FakeFlightsSource(),
    val driver: SqlDriver = createTestDriver(),
) {
    var clock = 1_000L
    val dispatcher = StandardTestDispatcher(test.testScheduler)
    val database = FlightDatabaseStore(driver, dispatcher)
    val faults = FaultInjection(failSaves = false, failRemoves = false)
    var closed = false
        private set

    fun newStore(
        resetData: Boolean = false,
        repository: FlightRepository = defaultRepository(),
        defaultServerUrl: String = "http://localhost:8080",
        computeDispatcher: CoroutineDispatcher = dispatcher,
    ): FlightExplorerStore {
        // Parented to backgroundScope so it's cancelled when the test ends, but without its context:
        // advanceUntilIdle() skips work marked as background, and the store's work must run.
        val scope = CoroutineScope(SupervisorJob(test.backgroundScope.coroutineContext.job) + dispatcher)
        return FlightExplorerStore(
            repository = repository,
            scope = scope,
            computeDispatcher = computeDispatcher,
            faults = faults,
            now = { clock },
            defaultServerUrl = defaultServerUrl,
            resetData = resetData,
            onClose = { closed = true },
        )
    }

    fun defaultRepository(): FlightRepository =
        FaultInjectingRepository(DefaultFlightRepository(source, database) { clock }, faults)
}

/** Holds every task until the test calls [runAll], so a derivation can be caught half-way. */
internal class ManualDispatcher : CoroutineDispatcher() {
    private val queue = ArrayDeque<Runnable>()
    val hasPendingWork get() = queue.isNotEmpty()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.addLast(block)
    }

    fun runAll() {
        while (queue.isNotEmpty()) queue.removeFirst().run()
    }
}

/** Holds saves and removes open until the test completes [writeGate], so in-flight states are observable. */
internal class GatedRepository(private val delegate: FlightRepository) : FlightRepository by delegate {
    var writeGate: CompletableDeferred<Unit>? = null

    /** Like a blocking database write: cancelling the store doesn't interrupt it. */
    var ignoreCancellation = false

    override suspend fun save(flight: Flight): Result<Unit> {
        awaitGate()
        return delegate.save(flight)
    }

    override suspend fun remove(flightId: String): Result<Unit> {
        awaitGate()
        return delegate.remove(flightId)
    }

    private suspend fun awaitGate() {
        val gate = writeGate ?: return
        if (ignoreCancellation) withContext(NonCancellable) { gate.await() } else gate.await()
    }
}

/** Fails, recovers with one value, fails again, then works: two separate streaks of errors. */
internal class TwoFailureStreaksRepository(private val delegate: FlightRepository) : FlightRepository by delegate {
    private var collections = 0

    override val cache: Flow<CachedFlights> = flow {
        collections += 1
        when (collections) {
            1 -> throw IllegalStateException("disk I/O error")
            2 -> {
                emit(CachedFlights(fixtureFlights, lastSuccessAtMillis = 1))
                throw IllegalStateException("disk I/O error again")
            }
            else -> emitAll(delegate.cache)
        }
    }
}

/** The cache read waits for [gate], like a slow first read of 1,000 rows. */
internal class SlowCacheRepository(
    private val delegate: FlightRepository,
    private val gate: CompletableDeferred<Unit>,
) : FlightRepository by delegate {
    override val cache: Flow<CachedFlights> = flow {
        gate.await()
        emitAll(delegate.cache)
    }
}

/** Delegates to [delegate], except that the first collection of the cache fails. */
internal class FlakyCacheRepository(private val delegate: FlightRepository) : FlightRepository by delegate {
    private var failed = false

    override val cache: Flow<CachedFlights> = flow {
        if (!failed) {
            failed = true
            throw IllegalStateException("disk I/O error")
        }
        emitAll(delegate.cache)
    }
}
