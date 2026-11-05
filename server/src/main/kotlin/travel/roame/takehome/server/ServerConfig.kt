package travel.roame.takehome.server

enum class Mode(val cliName: String) {
    NORMAL("normal"),
    EMPTY("empty"),
    ERROR("error"),
    UPDATED("updated"),

    /** The normal fixture, 3 s late, so the apps' loading states can be seen. */
    SLOW("slow");

    companion object {
        fun parse(value: String?): Mode? = entries.firstOrNull { it.cliName == value?.trim()?.lowercase() }
    }
}

data class ServerConfig(
    val port: Int = 8080,
    val mode: Mode = Mode.NORMAL,
    val fixturePath: String = "fixtures/flights.json",
    val allowControl: Boolean = false,
) {
    companion object {
        const val USAGE = "Usage: [--port=8080] [--mode=normal|empty|error|updated|slow] [--fixture=<path>] [--allow-control]"

        fun parse(args: Array<String>): ServerConfig = args.fold(ServerConfig()) { config, arg ->
            val value = arg.substringAfter('=')
            when {
                arg.startsWith("--port=") -> config.copy(
                    port = value.toIntOrNull()?.takeIf { it in 1..65535 }
                        ?: throw IllegalArgumentException("--port must be a number from 1 to 65535, got '$value'"),
                )
                arg.startsWith("--mode=") -> config.copy(
                    mode = Mode.parse(value)
                        ?: throw IllegalArgumentException("--mode must be one of ${Mode.entries.joinToString { it.cliName }}, got '$value'"),
                )
                arg.startsWith("--fixture=") -> config.copy(fixturePath = value)
                arg == "--allow-control" -> config.copy(allowControl = true)
                else -> throw IllegalArgumentException("Unknown argument '$arg'. $USAGE")
            }
        }
    }
}
