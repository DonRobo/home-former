package at.robert.provider

import at.robert.Diff
import at.robert.util.jsonObjectMapper
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.prefs.Preferences

data class DummyState(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val state: String? = "state1"
)

data class DummyStateProviderConfig(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    val name: String?,
)

class DummyStateProvider(
    private val mocked: Boolean = false
) : Provider<DummyState> {
    override fun toString(): String {
        return "DummyStateProvider($stateVariable)"
    }

    private var currentState: DummyState = DummyState("state1")
        get() {
            if (mocked) {
                return field
            } else {
                val state = Preferences.userNodeForPackage(DummyStateProvider::class.java).get(stateVariable, null)
                return try {
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
        set(value) {
            if (mocked) {
                field = value
            } else {
                Preferences.userNodeForPackage(DummyStateProvider::class.java)
                    .put(stateVariable, jsonObjectMapper.writeValueAsString(value))
            }
        }

    override var config: Any = DummyStateProviderConfig(null)
        set(value) {
            if (value is DummyStateProviderConfig) {
                field = value
                return
            }

            val jsonValue = (value as? JsonNode) ?: jsonObjectMapper.valueToTree(value)

            field = jsonObjectMapper.readerForUpdating(field).readValue<DummyStateProviderConfig>(jsonValue)
        }
    private val castConfig get() = config as DummyStateProviderConfig

    private val stateVariable get() = castConfig.name ?: "state"

    override fun currentState(): DummyState {
        return currentState
    }

    override fun applyDiff(diff: Diff) {
        diff.changes.forEach { change ->
            if (change.path == listOf("state")) {
                currentState = currentState.copy(state = change.newValue?.textValue())
            } else {
                error("Can't apply diff $diff to ${currentState()}")
            }
        }
    }
}
