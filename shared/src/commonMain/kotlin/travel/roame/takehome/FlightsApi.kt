package travel.roame.takehome

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

internal interface FlightsSource {
    /** Fetches `<baseUrl>/flights`. Throws [RefreshFailure] for every expected failure. */
    suspend fun fetchFlights(baseUrl: String): List<Flight>
}

internal class RefreshFailure(val error: RefreshError, cause: Throwable? = null) : Exception(error.toString(), cause)

internal fun HttpClientConfig<*>.flightsClientDefaults() {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 5_000
        requestTimeoutMillis = 10_000
        socketTimeoutMillis = 10_000
    }
}

internal class FlightsApi(private val client: HttpClient) : FlightsSource {
    override suspend fun fetchFlights(baseUrl: String): List<Flight> {
        val body = try {
            val response = client.get(baseUrl.trimEnd('/') + "/flights")
            if (!response.status.isSuccess()) throw RefreshFailure(RefreshError.Http(response.status.value))
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: RefreshFailure) {
            throw e
        } catch (e: Exception) {
            // Connection refused, DNS, TLS and timeouts all mean the API can't be reached right now.
            throw RefreshFailure(RefreshError.Offline, e)
        }
        return try {
            decodeFlights(body)
        } catch (e: IllegalArgumentException) {
            throw RefreshFailure(RefreshError.Malformed, e)
        }
    }
}
