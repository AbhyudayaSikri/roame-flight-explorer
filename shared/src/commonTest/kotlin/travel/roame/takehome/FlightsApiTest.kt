package travel.roame.takehome

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlightsApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) = FlightsApi(HttpClient(engine) { flightsClientDefaults() }, "http://localhost:8080/")

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
