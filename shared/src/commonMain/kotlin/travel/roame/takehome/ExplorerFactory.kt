package travel.roame.takehome

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Wires the store; AndroidExplorer and IosExplorer supply the platform driver, engine and dispatchers. */
internal fun createExplorerStore(
    driver: SqlDriver,
    httpClient: HttpClient,
    baseUrl: String,
    debug: DebugOptions,
    ioDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
): FlightExplorerStore {
    val faults = FaultInjection(debug.failSaves, debug.failRemoves)
    val repository = FaultInjectingRepository(
        delegate = DefaultFlightRepository(
            source = FlightsApi(httpClient, baseUrl),
            database = FlightDatabaseStore(driver, ioDispatcher),
            now = ::currentTimeMillis,
        ),
        faults = faults,
    )
    return FlightExplorerStore(
        repository = repository,
        scope = CoroutineScope(SupervisorJob() + mainDispatcher),
        computeDispatcher = Dispatchers.Default,
        faults = faults,
        now = ::currentTimeMillis,
        resetData = debug.resetData,
        onClose = {
            httpClient.close()
            driver.close()
        },
    )
}
