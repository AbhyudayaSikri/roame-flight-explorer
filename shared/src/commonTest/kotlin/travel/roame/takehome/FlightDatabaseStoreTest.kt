package travel.roame.takehome

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FlightDatabaseStoreTest {
    private fun TestScope.database() = FlightDatabaseStore(createTestDriver(), StandardTestDispatcher(testScheduler))

    @Test
    fun theCacheRoundTripsEveryField() = runTest {
        val db = database()

        db.replaceCache(fixtureFlights, atMillis = 42)

        assertEquals(fixtureFlights.sortedBy { it.id }, db.cachedFlights.first().sortedBy { it.id })
        assertEquals(42, db.lastSuccessAtMillis.first())
    }

    @Test
    fun aRefreshReplacesTheWholeCacheEvenWithNothing() = runTest {
        val db = database()
        db.replaceCache(fixtureFlights, atMillis = 1)

        db.replaceCache(fixtureFlights.take(3), atMillis = 2)
        assertEquals(fixtureFlights.take(3).map { it.id }.toSet(), db.cachedFlights.first().map { it.id }.toSet())

        db.replaceCache(emptyList(), atMillis = 3)
        assertEquals(emptyList(), db.cachedFlights.first())
        assertEquals(3, db.lastSuccessAtMillis.first())
    }

    @Test
    fun savingTheSameFlightAgainUpdatesItInsteadOfAddingARow() = runTest {
        val db = database()
        val flight = fixtureFlights.first()

        db.save(flight, atMillis = 10)
        db.save(flight.copy(points = flight.points + 500), atMillis = 20)

        assertEquals(listOf(SavedFlight(flight.copy(points = flight.points + 500), 20)), db.savedFlights.first())
    }

    @Test
    fun savedFlightsAreNewestFirstAndUntouchedByRefreshes() = runTest {
        val db = database()
        val (a, b) = fixtureFlights.take(2)
        db.save(a, atMillis = 10)
        db.save(b, atMillis = 20)

        db.replaceCache(emptyList(), atMillis = 30)

        assertEquals(listOf(b.id, a.id), db.savedFlights.first().map { it.flight.id })
    }

    @Test
    fun removeDeletesOnlyThatFlight() = runTest {
        val db = database()
        val (a, b) = fixtureFlights.take(2)
        db.save(a, atMillis = 10)
        db.save(b, atMillis = 20)

        db.remove(a.id)

        assertEquals(listOf(b.id), db.savedFlights.first().map { it.flight.id })
    }

    @Test
    fun clearAllEmptiesEverything() = runTest {
        val db = database()
        db.replaceCache(fixtureFlights, atMillis = 1)
        db.save(fixtureFlights.first(), atMillis = 2)

        db.clearAll()

        assertEquals(emptyList(), db.cachedFlights.first())
        assertEquals(emptyList(), db.savedFlights.first())
        assertEquals(null, db.lastSuccessAtMillis.first())
    }
}
