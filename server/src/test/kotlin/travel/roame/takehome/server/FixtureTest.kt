package travel.roame.takehome.server

import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class FixtureTest {
    private val fixtureFile = File(System.getProperty("fixture.path"))

    @Test
    fun theEmbeddedFixtureIsTheRepositorysFixtureByteForByte() {
        assertContentEquals(fixtureFile.readBytes(), loadFixture(path = null))
    }

    @Test
    fun aFixturePathOverridesTheEmbeddedCopy() {
        val other = createTempFile(suffix = ".json").apply { writeText("""{"flights":[]}""") }
        assertContentEquals("""{"flights":[]}""".encodeToByteArray(), loadFixture(other.toString()))
    }

    @Test
    fun aMissingFixtureFileIsAClearError() {
        assertFailsWith<IllegalArgumentException> { loadFixture("/no/such/flights.json") }
    }
}
