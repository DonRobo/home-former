package at.robert

import com.fasterxml.jackson.module.kotlin.readValue
import java.util.prefs.Preferences

data class DummyState(
    val state: String? = "state1"
)

class DummyStateProvider : Provider<DummyState> {
    private var currentState: DummyState

    init {
        val state = Preferences.userNodeForPackage(DummyStateProvider::class.java).get("state", null)
        currentState = try {
            if (state == null) {
                DummyState()
            } else {
                objectMapper.readValue(state)
            }
        } catch (ex: Exception) {
            DummyState()
        }
    }

    override fun currentState(): DummyState {
        return currentState
    }

    override fun applyDiff(diff: Diff) {
        diff.changes.forEach { change ->
            if (change.path == listOf("state")) {
                currentState = currentState.copy(state = change.newValue as String?)
                Preferences.userNodeForPackage(DummyStateProvider::class.java)
                    .put("state", objectMapper.writeValueAsString(currentState))
            } else {
                error("Can't apply diff $diff to ${currentState()}")
            }
        }
    }
}
