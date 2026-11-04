package travel.roame.takehome.server

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FlightsModuleTest {
    private val fixture = File(System.getProperty("fixture.path")).readBytes()

    private fun serve(
        mode: Mode,
        allowControl: Boolean = false,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application { flightsModule(fixture, ServerState(mode), allowControl, log = {}) }
        block()
    }

    @Test
    fun normalModeServesTheFixtureByteForByte() = serve(Mode.NORMAL) {
        val response = client.get("/flights")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        assertContentEquals(fixture, response.bodyAsBytes())
    }

    @Test
    fun emptyModeServesAnEmptyEnvelope() = serve(Mode.EMPTY) {
        val response = client.get("/flights")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        assertEquals(0, flightsOf(response.bodyAsText()).size)
    }

    @Test
    fun errorModeFailsWith500() = serve(Mode.ERROR) {
        assertEquals(HttpStatusCode.InternalServerError, client.get("/flights").status)
    }

    @Test
    fun updatedModeAddsFiveHundredPointsAndChangesNothingElse() = serve(Mode.UPDATED) {
        val response = client.get("/flights")
        val original = flightsOf(fixture.decodeToString())
        val updated = flightsOf(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1000, updated.size)
        original.zip(updated).forEach { (before, after) ->
            assertEquals(before.getValue("points").jsonPrimitive.int + 500, after.getValue("points").jsonPrimitive.int)
            assertEquals(before - "points", after - "points")
        }
    }

    @Test
    fun healthReturnsOk() = serve(Mode.ERROR) {
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }

    @Test
    fun unknownPathIsNotFound() = serve(Mode.NORMAL) {
        assertEquals(HttpStatusCode.NotFound, client.get("/flight").status)
    }

    @Test
    fun controlEndpointSwitchesTheModeWhenAllowed() = serve(Mode.NORMAL, allowControl = true) {
        assertEquals(HttpStatusCode.OK, client.post("/control/mode?value=empty").status)
        assertEquals(0, flightsOf(client.get("/flights").bodyAsText()).size)

        assertEquals(HttpStatusCode.OK, client.post("/control/mode?value=error").status)
        assertEquals(HttpStatusCode.InternalServerError, client.get("/flights").status)

        assertEquals(HttpStatusCode.OK, client.post("/control/mode?value=normal").status)
        assertContentEquals(fixture, client.get("/flights").bodyAsBytes())
    }

    @Test
    fun controlEndpointRejectsUnknownModes() = serve(Mode.NORMAL, allowControl = true) {
        assertEquals(HttpStatusCode.BadRequest, client.post("/control/mode?value=sideways").status)
        assertContentEquals(fixture, client.get("/flights").bodyAsBytes())
    }

    @Test
    fun controlEndpointDoesNotExistUnlessAllowed() = serve(Mode.NORMAL) {
        assertEquals(HttpStatusCode.NotFound, client.post("/control/mode?value=empty").status)
        assertContentEquals(fixture, client.get("/flights").bodyAsBytes())
    }

    private fun flightsOf(body: String): List<JsonObject> =
        Json.parseToJsonElement(body).jsonObject.getValue("flights").jsonArray.map { it.jsonObject }
}
