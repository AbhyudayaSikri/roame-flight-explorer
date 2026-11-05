package travel.roame.takehome

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class FlightsApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private class Api(private val api: FlightsApi) {
        suspend fun fetchFlights() = api.fetchFlights("http://localhost:8080/")
    }

    private fun api(engine: MockEngine) =
        Api(FlightsApi(HttpClient(engine) { flightsClientDefaults() }, Dispatchers.Unconfined))

    /**
     * Knows whether it is running one of its own blocks, so a test can ask where code ran. On
     * Native the engine resumes from its own thread, so every block goes through the test's
     * scheduler and runs on the test thread: [isRunning] is never read mid-block from another thread.
     */
    private class RecordingDispatcher(private val scheduled: CoroutineDispatcher) : CoroutineDispatcher() {
        var isRunning = false
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            scheduled.dispatch(context, Runnable {
                isRunning = true
                try {
                    block.run()
                } finally {
                    isRunning = false
                }
            })
        }
    }

    @Test
    fun theBodyIsDecodedOnTheWorkDispatcherNotTheCallers() = runTest {
        val work = RecordingDispatcher(StandardTestDispatcher(testScheduler))
        var decodedOnWork: Boolean? = null
        val engine = MockEngine { respond(FIXTURE_JSON, HttpStatusCode.OK, json) }
        val api = FlightsApi(HttpClient(engine) { flightsClientDefaults() }, work) { body ->
            decodedOnWork = work.isRunning
            decodeFlights(body)
        }

        assertEquals(fixtureFlights, api.fetchFlights("http://localhost:8080"))
        assertEquals(true, decodedOnWork)
        assertFalse(work.isRunning)
    }

    @Test
    fun fetchesAndDecodesTheFlights() = runTest {
        val engine = MockEngine { request ->
            assertEquals("http://localhost:8080/flights", request.url.toString())
            respond(FIXTURE_JSON, HttpStatusCode.OK, json)
        }

        assertEquals(fixtureFlights, api(engine).fetchFlights())
    }

    @Test
    fun anEmptyEnvelopeIsASuccess() = runTest {
        assertEquals(emptyList(), api(MockEngine { respond("""{"flights":[]}""", HttpStatusCode.OK, json) }).fetchFlights())
    }

    @Test
    fun aServerErrorIsReportedWithItsStatus() = runTest {
        val failure = assertFailsWith<RefreshFailure> {
            api(MockEngine { respond("boom", HttpStatusCode.InternalServerError) }).fetchFlights()
        }

        assertEquals(RefreshError.Http(500), failure.error)
    }

    @Test
    fun anUnreadableBodyIsMalformed() = runTest {
        val failure = assertFailsWith<RefreshFailure> {
            api(MockEngine { respond("""{"flights":[{"id":"x"}]}""", HttpStatusCode.OK, json) }).fetchFlights()
        }

        assertEquals(RefreshError.Malformed, failure.error)
    }

    @Test
    fun aConnectionFailureMeansOffline() = runTest {
        val failure = assertFailsWith<RefreshFailure> {
            api(MockEngine { throw IllegalStateException("Connection refused") }).fetchFlights()
        }

        assertEquals(RefreshError.Offline, failure.error)
    }
}
