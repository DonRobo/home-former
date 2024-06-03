package at.robert.provider

import at.robert.Diff
import at.robert.shelly.ShellyClient
import at.robert.shelly.client.schema.`in`.ShellyInputType
import at.robert.util.jsonObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class ShellyState(
    val name: String,
    val inputs: Map<Int, ShellyInputState>,
    val outputs: Map<Int, ShellyOutputState>,
)

enum class ShellyInputType {
    SWITCH,
    BUTTON,
}

enum class ShellyInputOutputType {
    TOGGLE,
    MOMENTARY,
    EDGE,
    DETACHED,
    ACTIVATION
}

enum class ShellyActionOnPower {
    TURN_ON,
    TURN_OFF,
    RESTORE_LAST,
    MATCH_INPUT,
}

data class ShellyInputState(
    val enabled: Boolean,
    val name: String,
    val type: ShellyInputType,
)

data class ShellyOutputState(
    val name: String,
    val state: Boolean,
    val outputType: ShellyInputOutputType,
    val actionOnPower: ShellyActionOnPower,
)

data class ShellyProviderConfig(
    val host: String,
)

class ShellyProvider : Provider<ShellyState> {
    override var config: Any = ShellyProviderConfig("")
        set(value) {
            if (value is ShellyProviderConfig) {
                field = value
                return
            }

            val jsonValue = (value as? JsonNode) ?: jsonObjectMapper.valueToTree(value)

            field = jsonObjectMapper.readerForUpdating(field).readValue<ShellyProviderConfig>(jsonValue)
        }
    private val castConfig get() = config as ShellyProviderConfig
    private fun shellyClient(): ShellyClient {
        require(castConfig.host.isNotBlank()) { "Host must not be blank" }
        return ShellyClient(castConfig.host)
    }

    override suspend fun currentState(): ShellyState = coroutineScope {
        val shellyClient = shellyClient()
        val name = async { shellyClient.getName() }
        val inputs = async { shellyClient.getInputs() }
        val outputs = async { shellyClient.getSwitches() }
        val inputConfigs = inputs.await().associate {
            it.id to async { shellyClient.getInputConfig(it.id) }
        }
        val outputConfigs = outputs.await().associate {
            it.id to async { shellyClient.getSwitchConfig(it.id) }
        }
        return@coroutineScope ShellyState(
            name = name.await(),
            inputs = inputConfigs.mapValues { (_, d) ->
                val c = d.await()
                ShellyInputState(
                    name = c.name ?: "",
                    enabled = c.enable,
                    type = ShellyInputType.valueOf(c.type.name),
                )
            },
            outputs = outputConfigs.mapValues { (id, d) ->
                val c = d.await()
                ShellyOutputState(
                    name = c.name ?: "",
                    state = outputs.await().single { it.id == id }.output,
                    outputType = ShellyInputOutputType.valueOf(c.inMode.name),
                    actionOnPower = ShellyActionOnPower.valueOf(c.initialState.name),
                )
            },
        )
    }

    override suspend fun applyDiff(diff: Diff) {
        diff.changes.forEach { change ->
            if (change.path.singleOrNull() == "name") {
                shellyClient().setName(change.newValue!!.asText())
            } else {
                error("Can't apply diff $diff to Shelly")
            }
        }
    }

    override fun toString(): String {
        return "ShellyProvider(host=${castConfig.host})"
    }
}
