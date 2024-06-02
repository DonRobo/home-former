package at.robert

import at.robert.provider.DummyStateProvider
import com.fasterxml.jackson.databind.node.TextNode
import kotlin.test.Test
import kotlin.test.assertEquals

class DummyStateProviderTest {

    @Test
    fun applyDiff() {
        val dummyStateProvider = DummyStateProvider(mocked = true)
        val diff = Diff(listOf(Change(listOf("state"), TextNode("state1"), TextNode("state2"))))
        dummyStateProvider.applyDiff(diff)
        assertEquals("state2", dummyStateProvider.currentState().state)
    }

    @Test
    fun `generate and apply diff`() {
        val dummyStateProvider = DummyStateProvider(mocked = true)
        val oldState = dummyStateProvider.currentState()
        val newState = oldState.copy(state = "state2")
        val diff = diff(oldState, newState)
        dummyStateProvider.applyDiff(diff)
        assertEquals(newState, dummyStateProvider.currentState())
    }
}
