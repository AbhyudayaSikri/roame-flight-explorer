package travel.roame.takehome.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = try {
        ServerConfig.parse(args)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        exitProcess(2)
    }

    val fixture = try {
        loadFixture(config.fixturePath)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        exitProcess(2)
    }

    val source = config.fixturePath ?: "the embedded fixtures/flights.json"
    val control = if (config.allowControl) ", POST /control/mode enabled" else ""
    println("Serving $source at http://0.0.0.0:${config.port}/flights (mode=${config.mode.cliName}$control)")

    embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
        flightsModule(fixture, ServerState(config.mode), config.allowControl)
    }.start(wait = true)
}
