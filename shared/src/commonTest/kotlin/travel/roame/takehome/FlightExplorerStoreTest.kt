package travel.roame.takehome

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlightExplorerStoreTest {
    private val jfkToHaneda = fixtureFlights.first { it.origin == "JFK" && it.destination == "HND" }

    private fun TestScope.loadedStore(harness: StoreHarness = StoreHarness(this)): Pair<StoreHarness, FlightExplorerStore> {
        val store = harness.newStore()
        advanceUntilIdle()
        return harness to store
    }

    @Test
    fun theFirstLaunchShowsEveryFlightWithNoFiltersActive() = runTest {
        val (harness, store) = loadedStore()
        val state = store.state.value

        assertEquals(SyncState.Fresh(harness.clock), state.sync)
        assertFalse(state.isRefreshing)
        assertEquals(1000, state.resultCount)
        assertEquals(1000, state.totalCount)
        assertEquals(24, state.markers.size)
        assertEquals(emptyList(), state.activeFilters)
        assertEquals(DateRange(date("2027-02-01"), date("2027-04-01")), state.filters.dateRange)
        assertEquals(133_000, state.filters.maxPoints)
        assertFalse(state.filters.isPointsLimited)
    }

    @Test
    fun theFirstLoadShowsLoadingUntilTheResponseArrives() = runTest {
        val harness = StoreHarness(this)
        val gate = CompletableDeferred<Unit>().also { harness.source.gate = it }
        val store = harness.newStore()
        runCurrent()

        assertEquals(SyncState.Loading, store.state.value.sync)
        assertTrue(store.state.value.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1000, store.state.value.resultCount)
        assertFalse(store.state.value.isRefreshing)
    }

    @Test
    fun aFailedRefreshKeepsTheCacheAndMarksItStale() = runTest {
        val (harness, store) = loadedStore()
        val loadedAt = harness.clock

        harness.clock += 60_000
        harness.source.fail(RefreshError.Offline)
        store.refresh()
        advanceUntilIdle()

        assertEquals(SyncState.Stale(loadedAt, RefreshError.Offline), store.state.value.sync)
        assertEquals(1000, store.state.value.resultCount)
    }

    @Test
    fun aRetryKeepsTheCacheMarkedStaleUntilItSucceeds() = runTest {
        val (harness, store) = loadedStore()
        val loadedAt = harness.clock
        harness.source.fail(RefreshError.Offline)
        store.refresh()
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>().also { harness.source.gate = it }
        store.refresh()
        runCurrent()
        assertTrue(store.state.value.isRefreshing)
        assertEquals(SyncState.Stale(loadedAt, RefreshError.Offline), store.state.value.sync)

        harness.clock += 60_000
        harness.source.serve(fixtureFlights)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(SyncState.Fresh(harness.clock), store.state.value.sync)
    }

    @Test
    fun aRefreshThatFailsBeforeTheCacheIsReadDoesNotClaimThereIsNoCache() = runTest {
        val harness = StoreHarness(this)
        harness.database.replaceCache(fixtureFlights, atMillis = 5)
        harness.source.fail(RefreshError.Offline)
        val cacheRead = CompletableDeferred<Unit>()
        val store = harness.newStore(repository = SlowCacheRepository(harness.defaultRepository(), cacheRead))
        advanceUntilIdle()

        // The refresh has failed and the cache hasn't been read: that's still loading.
        assertEquals(SyncState.Loading, store.state.value.sync)

        cacheRead.complete(Unit)
        advanceUntilIdle()
        assertEquals(SyncState.Stale(5, RefreshError.Offline), store.state.value.sync)
        assertEquals(1000, store.state.value.totalCount)
    }

    @Test
    fun anEmptySuccessIsNotAFailureAndReplacesTheCache() = runTest {
        val (harness, store) = loadedStore()

        harness.source.serve(emptyList())
        store.refresh()
        advanceUntilIdle()

        assertEquals(SyncState.EmptySuccess(harness.clock), store.state.value.sync)
        assertEquals(0, store.state.value.totalCount)
    }

    @Test
    fun withNoCacheAFailureNeedsARetryThatThenWorks() = runTest {
        val harness = StoreHarness(this)
        harness.source.fail(RefreshError.Http(500))
        val store = harness.newStore()
        advanceUntilIdle()

        assertEquals(SyncState.FailedNoCache(RefreshError.Http(500)), store.state.value.sync)

        harness.source.serve(fixtureFlights)
        val gate = CompletableDeferred<Unit>().also { harness.source.gate = it }
        store.refresh()
        runCurrent()
        assertEquals(SyncState.Loading, store.state.value.sync)
        assertTrue(store.state.value.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(SyncState.Fresh(harness.clock), store.state.value.sync)
        assertEquals(1000, store.state.value.resultCount)
    }

    @Test
    fun aMalformedFirstResponseIsAnErrorWithNothingCached() = runTest {
        val harness = StoreHarness(this)
        harness.source.fail(RefreshError.Malformed)
        val store = harness.newStore()
        advanceUntilIdle()

        assertEquals(SyncState.FailedNoCache(RefreshError.Malformed), store.state.value.sync)
        assertEquals(0, store.state.value.totalCount)
    }

    @Test
    fun aBadResponseOverACacheKeepsTheCacheAndMarksItStale() = runTest {
        listOf(RefreshError.Malformed, RefreshError.Http(500)).forEach { error ->
            val (harness, store) = loadedStore()
            val loadedAt = harness.clock
            harness.clock += 60_000
            harness.source.fail(error)
            store.refresh()
            advanceUntilIdle()

            assertEquals(SyncState.Stale(loadedAt, error), store.state.value.sync, "$error")
            assertEquals(1000, store.state.value.totalCount, "$error")
            store.close()
        }
    }

    @Test
    fun cachedAndSavedFlightsSurviveARestartWhileOffline() = runTest {
        val (harness, first) = loadedStore()
        first.save(jfkToHaneda.id)
        advanceUntilIdle()
        first.close()
        advanceUntilIdle()
        assertTrue(harness.closed)

        harness.source.fail(RefreshError.Offline)
        val second = harness.newStore()
        advanceUntilIdle()

        assertEquals(1000, second.state.value.resultCount)
        assertEquals(listOf(jfkToHaneda.id), second.state.value.saved.map { it.flight.id })
        assertTrue(second.state.value.sync is SyncState.Stale)
    }

    @Test
    fun savedFlightsSurviveARefreshThatNoLongerIncludesThem() = runTest {
        val (harness, store) = loadedStore()
        store.save(jfkToHaneda.id)
        advanceUntilIdle()

        harness.source.serve(fixtureFlights.filterNot { it.id == jfkToHaneda.id })
        store.refresh()
        advanceUntilIdle()

        assertEquals(999, store.state.value.totalCount)
        assertEquals(listOf(jfkToHaneda), store.state.value.saved.map { it.flight })
        assertTrue(jfkToHaneda.id in store.state.value.savedIds)
    }

    @Test
    fun savingAgainUpdatesTheSnapshotWithoutADuplicate() = runTest {
        val (harness, store) = loadedStore()
        store.select(jfkToHaneda.id)
        store.save(jfkToHaneda.id)
        store.save(jfkToHaneda.id)
        advanceUntilIdle()
        assertEquals(SaveStatus.SAVED, store.state.value.selectedSaveStatus)

        harness.source.serve(fixtureFlights.map { it.copy(points = it.points + 500) })
        store.refresh()
        advanceUntilIdle()
        assertEquals(SaveStatus.OUTDATED, store.state.value.selectedSaveStatus)

        store.save(jfkToHaneda.id)
        advanceUntilIdle()
        assertEquals(SaveStatus.SAVED, store.state.value.selectedSaveStatus)
        assertEquals(listOf(jfkToHaneda.points + 500), store.state.value.saved.map { it.flight.points })
    }

    @Test
    fun aFailedSaveIsShownOptimisticallyThenRolledBackWithAMessage() = runTest {
        val harness = StoreHarness(this)
        val repository = GatedRepository(harness.defaultRepository())
        val store = harness.newStore(repository = repository)
        advanceUntilIdle()
        harness.faults.failSaves = true
        store.select(jfkToHaneda.id)

        val gate = CompletableDeferred<Unit>().also { repository.writeGate = it }
        store.save(jfkToHaneda.id)
        runCurrent()
        assertTrue(jfkToHaneda.id in store.state.value.savedIds)
        assertEquals(SaveStatus.IN_PROGRESS, store.state.value.selectedSaveStatus)
        assertEquals(listOf(jfkToHaneda.id), store.state.value.saved.map { it.flight.id })

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(jfkToHaneda.id in store.state.value.savedIds)
        assertEquals(SaveStatus.NOT_SAVED, store.state.value.selectedSaveStatus)
        assertTrue(store.state.value.message!!.text.startsWith("Couldn't save JFK → HND"))

        store.messageShown(store.state.value.message!!.id)
        advanceUntilIdle()
        assertNull(store.state.value.message)
    }

    @Test
    fun dismissingAnOlderMessageKeepsANewerOne() = runTest {
        val (harness, store) = loadedStore()
        harness.faults.failSaves = true
        val (a, b) = fixtureFlights.take(2)

        store.save(a.id)
        advanceUntilIdle()
        val first = store.state.value.message!!
        store.save(b.id)
        advanceUntilIdle()
        val second = store.state.value.message!!

        store.messageShown(first.id)
        advanceUntilIdle()
        assertEquals(second, store.state.value.message)
    }

    @Test
    fun aFailedRemoveIsRolledBackWithAMessage() = runTest {
        val harness = StoreHarness(this)
        val repository = GatedRepository(harness.defaultRepository())
        val store = harness.newStore(repository = repository)
        advanceUntilIdle()
        store.save(jfkToHaneda.id)
        advanceUntilIdle()
        harness.faults.failRemoves = true

        val gate = CompletableDeferred<Unit>().also { repository.writeGate = it }
        store.remove(jfkToHaneda.id)
        runCurrent()
        assertFalse(jfkToHaneda.id in store.state.value.savedIds)
        assertEquals(emptyList(), store.state.value.saved)

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(jfkToHaneda.id in store.state.value.savedIds)
        assertTrue(store.state.value.message!!.text.startsWith("Couldn't remove JFK → HND"))
    }

    @Test
    fun searchFiltersAndRemoveAllWorkOffline() = runTest {
        val (harness, store) = loadedStore()
        store.save(jfkToHaneda.id)
        advanceUntilIdle()
        harness.source.fail(RefreshError.Offline)
        store.refresh()
        advanceUntilIdle()

        store.setOriginText("jfk")
        store.setCabin("Business")
        advanceUntilIdle()
        assertTrue(store.state.value.sync is SyncState.Stale)
        assertEquals(fixtureFlights.count { it.origin == "JFK" && it.cabin == "Business" }, store.state.value.resultCount)

        store.remove(jfkToHaneda.id)
        advanceUntilIdle()
        assertEquals(emptyList(), store.state.value.saved)
        assertNull(store.state.value.message)
    }

    @Test
    fun theSelectionSurvivesFilterChangesThatKeepItAndClearsWhenFilteredOut() = runTest {
        val (_, store) = loadedStore()
        store.select(jfkToHaneda.id)
        store.setDestinationText("tokyo")
        advanceUntilIdle()
        assertEquals(jfkToHaneda, store.state.value.selected)
        assertEquals(jfkToHaneda.originPoint, store.state.value.selectedRoute.first())

        val otherCabin = fixtureBounds.cabins.first { it != jfkToHaneda.cabin }
        store.setCabin(otherCabin)
        advanceUntilIdle()
        assertNull(store.state.value.selected)
        assertEquals(emptyList(), store.state.value.selectedRoute)

        store.setCabin(null)
        advanceUntilIdle()
        assertNull(store.state.value.selected, "a cleared selection must not come back")
    }

    @Test
    fun aFocusedMarkerListsItsFlightsAndClearsWhenItDisappears() = runTest {
        val (_, store) = loadedStore()

        store.focusDestination("HND")
        advanceUntilIdle()
        assertEquals("HND", store.state.value.focusedDestination?.airport)
        assertEquals(55, store.state.value.focusedFlights.size)
        assertTrue(store.state.value.focusedFlights.all { it.destination == "HND" })

        store.setDestinationText("paris")
        advanceUntilIdle()
        assertNull(store.state.value.focusedDestination)
        assertEquals(emptyList(), store.state.value.focusedFlights)
    }

    @Test
    fun savedFlightsIgnoreDiscoveryFilters() = runTest {
        val (_, store) = loadedStore()
        store.save(jfkToHaneda.id)
        advanceUntilIdle()

        store.setOriginText("xyz")
        store.selectSaved(jfkToHaneda.id)
        advanceUntilIdle()

        assertEquals(0, store.state.value.resultCount)
        assertEquals(listOf(jfkToHaneda.id), store.state.value.saved.map { it.flight.id })
        assertEquals(jfkToHaneda, store.state.value.selectedSaved?.flight)
    }

    @Test
    fun rapidQueryChangesEndWithTheLastQuery() = runTest {
        val (_, store) = loadedStore()

        listOf("j", "jf", "jfk", "new", "new york", "  sea ").forEach { store.setOriginText(it) }
        advanceUntilIdle()

        assertEquals("  sea ", store.state.value.filters.originText)
        assertTrue(store.state.value.results.all { it.origin == "SEA" })
        assertEquals(fixtureFlights.count { it.origin == "SEA" }, store.state.value.resultCount)
    }

    @Test
    fun anOutdatedQueryStillBeingComputedNeverReachesTheState() = runTest {
        val compute = ManualDispatcher()
        val store = StoreHarness(this).newStore(computeDispatcher = compute)
        fun settle() = repeat(10) {
            advanceUntilIdle()
            compute.runAll()
        }
        settle()
        assertEquals(1000, store.state.value.resultCount)
        val shown = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.state.collect { shown += it.resultCount } }

        // "jfk" (113 flights) is still being computed when "sea" arrives.
        store.setOriginText("jfk")
        advanceUntilIdle()
        assertTrue(compute.hasPendingWork)
        store.setOriginText("sea")
        settle()

        val sea = filter(FlightQuery(originText = "sea")).size
        assertNotEquals(113, sea)
        assertEquals(sea, store.state.value.resultCount)
        assertFalse(113 in shown, "jfk's results were shown after a newer query: $shown")
    }

    @Test
    fun markersAlwaysAddUpToTheResults() = runTest {
        val (_, store) = loadedStore()
        val queries = listOf<FlightExplorerStore.() -> Unit>(
            { setDestinationText("tokyo") },
            { setCabin("Business") },
            { toggleProgram("Avios") },
            { setMaxPoints(60_000) },
            { resetFilters() },
        )

        queries.forEach { change ->
            store.change()
            advanceUntilIdle()
            assertEquals(store.state.value.resultCount, store.state.value.markers.sumOf { it.count })
        }
    }

    @Test
    fun limitsThatCoverAllTheDataAreNoFilter() = runTest {
        val (_, store) = loadedStore()

        store.setMaxPoints(200_000)
        store.setDateRange(date("2027-04-01"), date("2027-02-01"))
        advanceUntilIdle()
        assertFalse(store.state.value.filters.isPointsLimited)
        assertFalse(store.state.value.filters.isDateRangeLimited)
        assertEquals(emptyList(), store.state.value.activeFilters)

        store.setMaxPoints(60_000)
        store.setDateRange(date("2027-02-10"), date("2027-02-01"))
        advanceUntilIdle()
        assertTrue(store.state.value.filters.isPointsLimited)
        assertEquals(DateRange(date("2027-02-01"), date("2027-02-10")), store.state.value.filters.dateRange)
        assertEquals(listOf(FilterKind.DATES, FilterKind.MAX_POINTS), store.state.value.activeFilters.map { it.kind })
    }

    @Test
    fun movingOnlyTheEndDateToTheStartGivesThatSingleDay() = runTest {
        // What the iOS sheet does for QA F1: the start stays at the data's first day.
        val (_, store) = loadedStore()
        val first = store.state.value.filters.bounds!!.firstDate
        store.setDateRange(first, date("2027-02-01"))
        advanceUntilIdle()
        assertEquals(DateRange(date("2027-02-01"), date("2027-02-01")), store.state.value.filters.dateRange)
        assertEquals(17, store.state.value.resultCount)
        assertEquals(listOf("Feb 1"), store.state.value.activeFilters.map { it.label })

        // Moving the end back to the last day restores "no date filter"; the pickers then show the
        // data's full range.
        store.setDateRange(store.state.value.filters.dateRange!!.start, date("2027-04-01"))
        advanceUntilIdle()
        assertFalse(store.state.value.filters.isDateRangeLimited)
        assertEquals(DateRange(date("2027-02-01"), date("2027-04-01")), store.state.value.filters.dateRange)
        assertEquals(1000, store.state.value.resultCount)
        assertEquals(emptyList(), store.state.value.activeFilters)
    }

    @Test
    fun anEndBeforeTheStartIsTreatedAsTheSameTwoDays() = runTest {
        val (_, store) = loadedStore()
        store.setDateRange(date("2027-02-02"), date("2027-02-01"))
        advanceUntilIdle()
        assertEquals(DateRange(date("2027-02-01"), date("2027-02-02")), store.state.value.filters.dateRange)
        assertEquals(filter(FlightQuery(dateRange = DateRange(date("2027-02-01"), date("2027-02-02")))).size, store.state.value.resultCount)
    }

    @Test
    fun resetClearsSearchAndEveryFilter() = runTest {
        val (_, store) = loadedStore()
        store.setOriginText("jfk")
        store.setDestinationText("tokyo")
        store.setCabin("Business")
        store.toggleProgram("Avios")
        store.setMaxPoints(60_000)
        store.setDateRange(date("2027-02-01"), date("2027-02-10"))
        advanceUntilIdle()
        assertEquals(6, store.state.value.activeFilters.size)

        store.clearFilter(FilterKind.CABIN)
        advanceUntilIdle()
        assertEquals(5, store.state.value.activeFilters.size)

        store.resetFilters()
        advanceUntilIdle()
        assertEquals(1000, store.state.value.resultCount)
        assertEquals(emptyList(), store.state.value.activeFilters)
        assertEquals("", store.state.value.filters.originText)
    }

    @Test
    fun removingOneChipClearsOnlyThatFilter() = runTest {
        val (_, store) = loadedStore()
        val all = FlightQuery(
            originText = "yyz",
            destinationText = "tokyo",
            dateRange = DateRange(date("2027-02-01"), date("2027-03-15")),
            cabin = "Business",
            maxPoints = 80_000,
            programs = setOf("Flying Blue", "Mileage Plan", "MileagePlus"),
        )
        // Every filter narrows these results, so removing any one of them must widen them.
        assertEquals(2, filter(all).size)

        FilterKind.entries.forEach { kind ->
            store.resetFilters()
            store.setOriginText(all.originText)
            store.setDestinationText(all.destinationText)
            store.setDateRange(all.dateRange!!.start, all.dateRange.end)
            store.setCabin(all.cabin)
            store.setMaxPoints(all.maxPoints!!)
            all.programs.forEach(store::toggleProgram)
            advanceUntilIdle()
            assertEquals(FilterKind.entries.toSet(), store.state.value.activeFilters.map { it.kind }.toSet())

            store.clearFilter(kind)
            advanceUntilIdle()
            val state = store.state.value
            assertEquals(FilterKind.entries.toSet() - kind, state.activeFilters.map { it.kind }.toSet(), "$kind")
            val without = when (kind) {
                FilterKind.ORIGIN -> all.copy(originText = "")
                FilterKind.DESTINATION -> all.copy(destinationText = "")
                FilterKind.DATES -> all.copy(dateRange = null)
                FilterKind.CABIN -> all.copy(cabin = null)
                FilterKind.MAX_POINTS -> all.copy(maxPoints = null)
                FilterKind.PROGRAMS -> all.copy(programs = emptySet())
            }
            assertTrue(state.resultCount > 2, "$kind")
            assertEquals(filter(without).size, state.resultCount, "$kind")
        }
    }

    @Test
    fun aBrokenStorageReadBecomesAMessageAndRecovers() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(repository = FlakyCacheRepository(harness.defaultRepository()))
        runCurrent()

        assertEquals("Couldn't read cached flights. Retrying…", store.state.value.message?.text)

        advanceUntilIdle()
        assertEquals(1000, store.state.value.resultCount)
    }

    @Test
    fun eachNewStreakOfStorageErrorsPostsAMessage() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(repository = TwoFailureStreaksRepository(harness.defaultRepository()))
        runCurrent()
        val first = store.state.value.message!!

        advanceTimeBy(1_001)
        runCurrent()
        val second = store.state.value.message!!
        assertTrue(second.id > first.id)
        assertEquals("Couldn't read cached flights. Retrying…", second.text)

        advanceUntilIdle()
        assertEquals(1000, store.state.value.resultCount)
    }

    @Test
    fun closeReleasesResourcesOnlyAfterWorkInFlightFinishes() = runTest {
        val harness = StoreHarness(this)
        val repository = GatedRepository(harness.defaultRepository()).apply { ignoreCancellation = true }
        val store = harness.newStore(repository = repository)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>().also { repository.writeGate = it }
        store.save(jfkToHaneda.id)
        runCurrent()

        store.close()
        runCurrent()
        assertFalse(harness.closed, "the database must stay open while a write is running")

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(harness.closed)
    }

    @Test
    fun resetDataClearsEverythingBeforeTheFirstRefresh() = runTest {
        val (harness, first) = loadedStore()
        first.save(jfkToHaneda.id)
        advanceUntilIdle()
        first.close()

        harness.source.fail(RefreshError.Offline)
        val second = harness.newStore(resetData = true)
        advanceUntilIdle()

        assertEquals(SyncState.FailedNoCache(RefreshError.Offline), second.state.value.sync)
        assertEquals(emptyList(), second.state.value.saved)
    }

    @Test
    fun closeStopsTheStore() = runTest {
        val (harness, store) = loadedStore()

        store.close()
        store.setOriginText("jfk")
        advanceUntilIdle()

        assertTrue(harness.closed)
        assertEquals("", store.state.value.filters.originText)
    }

    @Test
    fun savingAFlightThatIsNoLongerCachedSaysSo() = runTest {
        val (_, store) = loadedStore()

        store.save("not-a-flight")
        advanceUntilIdle()

        assertNotNull(store.state.value.message)
        assertEquals(emptyList(), store.state.value.saved)
    }
}
