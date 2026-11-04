package travel.roame.takehome

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import travel.roame.takehome.db.FlightDatabase

object IosExplorer {
    fun createStore(baseUrl: String, debug: DebugOptions): FlightExplorerStore =
        createExplorerStore(
            driver = NativeSqliteDriver(FlightDatabase.Schema, "flights.db"),
            httpClient = HttpClient(Darwin) { flightsClientDefaults() },
            baseUrl = baseUrl,
            debug = debug,
            ioDispatcher = Dispatchers.IO,
            mainDispatcher = Dispatchers.Main.immediate,
        )
}
