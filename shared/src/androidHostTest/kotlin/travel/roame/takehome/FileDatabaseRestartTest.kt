package travel.roame.takehome

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import travel.roame.takehome.db.FlightDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A real restart: the database file is closed and opened again, not shared in memory. */
class FileDatabaseRestartTest {
    @Test
    fun cachedAndSavedFlightsSurviveClosingAndReopeningTheDatabaseFile() = runTest {
        val file = File.createTempFile("flights", ".db").apply { deleteOnExit() }
        val url = "jdbc:sqlite:${file.absolutePath}"
        val saved = fixtureFlights.first { it.origin == "JFK" && it.destination == "HND" }

        val firstDriver = JdbcSqliteDriver(url).also { FlightDatabase.Schema.create(it) }
        val first = StoreHarness(this, driver = firstDriver).newStore()
        advanceUntilIdle()
        first.save(saved.id)
        advanceUntilIdle()
        first.close()
        advanceUntilIdle()
        firstDriver.close()

        val secondDriver = JdbcSqliteDriver(url)
        val harness = StoreHarness(this, driver = secondDriver)
        harness.source.fail(RefreshError.Offline)
        val second = harness.newStore()
        advanceUntilIdle()

        assertEquals(1000, second.state.value.totalCount)
        assertEquals(listOf(saved.id), second.state.value.saved.map { it.flight.id })
        assertTrue(second.state.value.sync is SyncState.Stale)
        second.close()
        advanceUntilIdle()
        secondDriver.close()
    }
}
