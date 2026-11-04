package travel.roame.takehome.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.application.install
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val EMPTY_ENVELOPE = """{"flights":[]}"""
private const val ERROR_BODY = """{"error":"simulated server failure (--mode=error)"}"""

class ServerState(initialMode: Mode) {
    @Volatile
    var mode: Mode = initialMode
}

fun Application.flightsModule(
    fixture: ByteArray,
    state: ServerState,
    allowControl: Boolean,
    log: (String) -> Unit = ::println,
) {
    val updatedFixture by lazy { withUpdatedPoints(fixture) }

    install(
        createApplicationPlugin("RequestLog") {
            on(ResponseSent) { call ->
                val status = call.response.status()?.value
                log("${call.request.httpMethod.value} ${call.request.uri} mode=${state.mode.cliName} -> $status")
            }
        },
    )

    routing {
        get("/health") { call.respondText("ok") }

        get("/flights") {
            when (state.mode) {
                Mode.NORMAL -> call.respondBytes(fixture, ContentType.Application.Json)
                Mode.EMPTY -> call.respondText(EMPTY_ENVELOPE, ContentType.Application.Json)
                Mode.ERROR -> call.respondText(ERROR_BODY, ContentType.Application.Json, HttpStatusCode.InternalServerError)
                Mode.UPDATED -> call.respondBytes(updatedFixture, ContentType.Application.Json)
            }
        }

        if (allowControl) {
            post("/control/mode") {
                val mode = Mode.parse(call.request.queryParameters["value"])
                if (mode == null) {
                    call.respondText("Unknown mode. ${ServerConfig.USAGE}", status = HttpStatusCode.BadRequest)
                } else {
                    state.mode = mode
                    call.respondText(mode.cliName)
                }
            }
        }
    }
}

/** The fixture with every flight's points raised by [delta]; ids and all other fields unchanged. */
internal fun withUpdatedPoints(fixture: ByteArray, delta: Int = 500): ByteArray {
    val root = Json.parseToJsonElement(fixture.decodeToString()).jsonObject
    val flights = root.getValue("flights").jsonArray.map { element ->
        val flight = element.jsonObject
        JsonObject(flight + ("points" to JsonPrimitive(flight.getValue("points").jsonPrimitive.int + delta)))
    }
    return JsonObject(root + ("flights" to JsonArray(flights))).toString().encodeToByteArray()
}
