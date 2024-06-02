package at.robert

import at.robert.provider.DummyStateProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class DummyStateProviderTest {

    @Test
    fun applyDiff() {
        val dummyStateProvider = DummyStateProvider()
        val diff = Diff(listOf(Change(listOf("state"), "state1", "state2")))
        dummyStateProvider.applyDiff(diff)
        assertEquals("state2", dummyStateProvider.currentState().state)
    }

    @Test
    fun `generate and apply diff`() {
        val dummyStateProvider = DummyStateProvider()
        val oldState = dummyStateProvider.currentState()
        val newState = oldState.copy(state = "state2")
        val diff = diff(oldState, newState)
        dummyStateProvider.applyDiff(diff)
        assertEquals(newState, dummyStateProvider.currentState())
    }
}
