package travel.roame.takehome.server

import java.io.File

/** Where `fixtures/flights.json` is copied inside the server's jar (see server/build.gradle.kts). */
private const val EMBEDDED_FIXTURE = "/flights.json"

/**
 * The flights to serve: the file at [path], or the copy embedded in the server when [path] is null,
 * so `java -jar` works from any directory. Throws IllegalArgumentException when neither exists.
 */
fun loadFixture(path: String?): ByteArray {
    if (path == null) {
        val embedded = ServerConfig::class.java.getResourceAsStream(EMBEDDED_FIXTURE)
            ?: throw IllegalArgumentException("This build has no embedded fixture; pass --fixture=<path>.")
        return embedded.use { it.readBytes() }
    }
    val file = File(path)
    require(file.isFile) { "Fixture not found at ${file.absolutePath}." }
    return file.readBytes()
}
