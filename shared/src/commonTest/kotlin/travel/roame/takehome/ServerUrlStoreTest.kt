package travel.roame.takehome

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ServerUrlStoreTest {
    private val buildDefault = "http://10.0.2.2:8080"
    private val lan = "http://192.168.1.20:8080"

    @Test
    fun theBuildDefaultIsUsedUntilAnotherAddressIsSet() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()

        assertEquals(buildDefault, store.state.value.serverUrl)
        assertEquals(buildDefault, store.state.value.defaultServerUrl)
        assertTrue(store.state.value.isDefaultServerUrl)
        assertEquals(listOf(buildDefault), harness.source.baseUrls)
    }

    @Test
    fun aNewAddressIsFetchedAtOnceAndKeptAcrossARestart() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()

        store.setServerUrl(" 192.168.1.20:8080/ ")
        advanceUntilIdle()
        assertEquals(lan, store.state.value.serverUrl)
        assertFalse(store.state.value.isDefaultServerUrl)
        assertEquals(listOf(buildDefault, lan), harness.source.baseUrls)
        assertEquals(SyncState.Fresh(harness.clock), store.state.value.sync)

        store.close()
        advanceUntilIdle()
        val restarted = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()
        assertEquals(lan, restarted.state.value.serverUrl)
        assertEquals(listOf(buildDefault, lan, lan), harness.source.baseUrls)
    }

    @Test
    fun anInvalidAddressChangesNothingAndSaysWhy() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()

        store.setServerUrl("ftp://nas.local")
        advanceUntilIdle()
        assertEquals("Use an http:// or https:// address.", store.state.value.message?.text)
        assertEquals(buildDefault, store.state.value.serverUrl)
        assertEquals(listOf(buildDefault), harness.source.baseUrls)
    }

    @Test
    fun goingBackToTheDefaultForgetsTheSavedAddress() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()
        store.setServerUrl(lan)
        advanceUntilIdle()

        store.useDefaultServerUrl()
        advanceUntilIdle()
        assertEquals(buildDefault, store.state.value.serverUrl)
        assertTrue(store.state.value.isDefaultServerUrl)
        assertEquals(buildDefault, harness.source.baseUrls.last())

        store.close()
        advanceUntilIdle()
        // A different build default after an update is picked up, because nothing was saved.
        val updated = harness.newStore(defaultServerUrl = "http://localhost:8080")
        advanceUntilIdle()
        assertEquals("http://localhost:8080", updated.state.value.serverUrl)
    }

    @Test
    fun changingTheAddressReplacesARefreshStillInFlight() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        harness.source.gate = gate
        store.refresh()
        runCurrent()
        assertTrue(store.state.value.isRefreshing)

        store.setServerUrl(lan)
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(buildDefault, buildDefault, lan), harness.source.baseUrls)
        assertFalse(store.state.value.isRefreshing)
        assertEquals(SyncState.Fresh(harness.clock), store.state.value.sync)
    }

    @Test
    fun resettingLocalDataForgetsTheSavedAddress() = runTest {
        val harness = StoreHarness(this)
        val store = harness.newStore(defaultServerUrl = buildDefault)
        advanceUntilIdle()
        store.setServerUrl(lan)
        advanceUntilIdle()
        store.close()
        advanceUntilIdle()

        val fresh = harness.newStore(resetData = true, defaultServerUrl = buildDefault)
        advanceUntilIdle()
        assertEquals(buildDefault, fresh.state.value.serverUrl)
        assertEquals(buildDefault, harness.source.baseUrls.last())
    }
}
