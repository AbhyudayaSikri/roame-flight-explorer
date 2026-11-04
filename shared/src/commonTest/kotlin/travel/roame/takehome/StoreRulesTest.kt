package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals

class StoreRulesTest {
    private val a = fixtureFlights[0]
    private val b = fixtureFlights[1]
    private val c = fixtureFlights[2]

    @Test
    fun syncStateCoversEveryCombination() {
        val failed = RefreshStatus.Failed(RefreshError.Offline)

        assertEquals(SyncState.Loading, syncStateOf(hasCachedFlights = false, lastSuccessAtMillis = null, RefreshStatus.Running))
        assertEquals(SyncState.Loading, syncStateOf(false, null, RefreshStatus.Idle))
        assertEquals(SyncState.Fresh(5), syncStateOf(true, 5, RefreshStatus.Idle))
        assertEquals(SyncState.Fresh(5), syncStateOf(true, 5, RefreshStatus.Running))
        assertEquals(SyncState.EmptySuccess(5), syncStateOf(false, 5, RefreshStatus.Idle))
        assertEquals(SyncState.Stale(5, RefreshError.Offline), syncStateOf(true, 5, failed))
        assertEquals(SyncState.Stale(5, RefreshError.Offline), syncStateOf(false, 5, failed))
        assertEquals(SyncState.FailedNoCache(RefreshError.Offline), syncStateOf(false, null, failed))
    }

    @Test
    fun pendingSavesShowFirstAndPendingRemovalsDisappear() {
        val stored = listOf(SavedFlight(a, 20), SavedFlight(b, 10))
        val pending = mapOf(
            c.id to PendingChange(PendingKind.SAVE, c, startedAtMillis = 30),
            b.id to PendingChange(PendingKind.REMOVE, b, startedAtMillis = 30),
        )

        assertEquals(listOf(c.id, a.id), savedWithPending(stored, pending).map { it.flight.id })
    }

    @Test
    fun aPendingResaveShowsTheNewSnapshotOnce() {
        val updated = a.copy(points = a.points + 500)
        val result = savedWithPending(
            stored = listOf(SavedFlight(a, 20)),
            pending = mapOf(a.id to PendingChange(PendingKind.SAVE, updated, startedAtMillis = 40)),
        )

        assertEquals(listOf(SavedFlight(updated, 40)), result)
    }

    @Test
    fun saveStatusComparesTheSnapshotWithTheCachedFlight() {
        val saved = mapOf(a.id to SavedFlight(a, 1))

        assertEquals(SaveStatus.NOT_SAVED, saveStatusOf(null, saved, emptyMap()))
        assertEquals(SaveStatus.NOT_SAVED, saveStatusOf(b, saved, emptyMap()))
        assertEquals(SaveStatus.SAVED, saveStatusOf(a, saved, emptyMap()))
        assertEquals(SaveStatus.OUTDATED, saveStatusOf(a.copy(points = a.points + 500), saved, emptyMap()))
        assertEquals(
            SaveStatus.IN_PROGRESS,
            saveStatusOf(b, saved, mapOf(b.id to PendingChange(PendingKind.SAVE, b, startedAtMillis = 2))),
        )
        assertEquals(
            SaveStatus.NOT_SAVED,
            saveStatusOf(a, saved, mapOf(a.id to PendingChange(PendingKind.REMOVE, a, startedAtMillis = 2, done = true))),
        )
    }
}
