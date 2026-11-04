package travel.roame.takehome

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import travel.roame.takehome.db.FlightDatabase

object AndroidExplorer {
    fun createStore(context: Context, baseUrl: String, debug: DebugOptions = DebugOptions()): FlightExplorerStore =
        createExplorerStore(
            driver = AndroidSqliteDriver(FlightDatabase.Schema, context.applicationContext, "flights.db"),
            httpClient = HttpClient(OkHttp) { flightsClientDefaults() },
            baseUrl = baseUrl,
            debug = debug,
            ioDispatcher = Dispatchers.IO,
            mainDispatcher = Dispatchers.Main.immediate,
        )
}
