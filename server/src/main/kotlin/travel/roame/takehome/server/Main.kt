package travel.roame.takehome.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val config = try {
        ServerConfig.parse(args)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        exitProcess(2)
    }

    val fixtureFile = File(config.fixturePath)
    if (!fixtureFile.isFile) {
        System.err.println("Fixture not found at ${fixtureFile.absolutePath}. Run from the repo root or pass --fixture=<path>.")
        exitProcess(2)
    }

    val control = if (config.allowControl) ", POST /control/mode enabled" else ""
    println("Serving ${fixtureFile.path} at http://0.0.0.0:${config.port}/flights (mode=${config.mode.cliName}$control)")

    embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
        flightsModule(fixtureFile.readBytes(), ServerState(config.mode), config.allowControl)
    }.start(wait = true)
}
