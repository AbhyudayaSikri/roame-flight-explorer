package travel.roame.takehome.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerConfigTest {
    @Test
    fun defaultsServeTheEmbeddedFixtureNormallyOnPort8080() {
        assertEquals(
            ServerConfig(port = 8080, mode = Mode.NORMAL, fixturePath = null, allowControl = false),
            ServerConfig.parse(emptyArray()),
        )
    }

    @Test
    fun parsesEveryFlag() {
        assertEquals(
            ServerConfig(port = 9090, mode = Mode.UPDATED, fixturePath = "/tmp/f.json", allowControl = true),
            ServerConfig.parse(arrayOf("--port=9090", "--mode=updated", "--fixture=/tmp/f.json", "--allow-control")),
        )
    }

    @Test
    fun modeNamesAreCaseInsensitive() {
        assertEquals(Mode.EMPTY, ServerConfig.parse(arrayOf("--mode=EMPTY")).mode)
    }

    @Test
    fun parsesSlowMode() {
        assertEquals(Mode.SLOW, ServerConfig.parse(arrayOf("--mode=slow")).mode)
    }

    @Test
    fun rejectsBadInput() {
        assertFailsWith<IllegalArgumentException> { ServerConfig.parse(arrayOf("--port=abc")) }
        assertFailsWith<IllegalArgumentException> { ServerConfig.parse(arrayOf("--port=70000")) }
        assertFailsWith<IllegalArgumentException> { ServerConfig.parse(arrayOf("--mode=sideways")) }
        assertFailsWith<IllegalArgumentException> { ServerConfig.parse(arrayOf("--verbose")) }
    }
}
