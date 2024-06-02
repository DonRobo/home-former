package at.robert.provider

import at.robert.Diff
import at.robert.jsonObjectMapper
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.prefs.Preferences

data class DummyState(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val state: String? = "state1"
)

class DummyStateProvider(private val mocked: Boolean = false) : Provider<DummyState> {
    private var currentState: DummyState

    init {
        if (mocked) {
            currentState = DummyState("state1")
        } else {
            val state = Preferences.userNodeForPackage(DummyStateProvider::class.java).get("state", null)
            currentState = try {
                if (state == null) {
                    DummyState()
                } else {
                    jsonObjectMapper.readValue(state)
                }
            } catch (ex: Exception) {
                DummyState()
            }
        }
    }

    override fun currentState(): DummyState {
        return currentState
    }

    override fun applyDiff(diff: Diff) {
        diff.changes.forEach { change ->
            if (change.path == listOf("state")) {
                currentState = currentState.copy(state = change.newValue?.textValue())
                if (!mocked) {
                    Preferences.userNodeForPackage(DummyStateProvider::class.java)
                        .put("state", jsonObjectMapper.writeValueAsString(currentState))
                }
            } else {
                error("Can't apply diff $diff to ${currentState()}")
            }
        }
    }
}
