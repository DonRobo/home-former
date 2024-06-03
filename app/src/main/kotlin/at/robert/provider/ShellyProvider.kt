package at.robert.provider

import at.robert.Diff
import at.robert.shelly.ShellyClient
import at.robert.shelly.client.schema.`in`.ShellyInputType
import at.robert.shelly.client.schema.`in`.ShellySwitchInMode
import at.robert.shelly.client.schema.`in`.ShellySwitchInitialState
import at.robert.util.jsonObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.*

data class ShellyState(
    val name: String,
    val inputs: Map<Int, ShellyInputState>,
    val outputs: Map<Int, ShellyOutputState>,
)

data class ShellyInputState(
    val enabled: Boolean,
    val name: String,
    val type: ShellyInputType,
)

data class ShellyOutputState(
    val name: String,
    //TODO
//    val state:Boolean,
    val outputType: ShellySwitchInMode,
    val actionOnPower: ShellySwitchInitialState,
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
                    type = c.type,
                )
            },
            outputs = outputConfigs.mapValues { (id, d) ->
                val c = d.await()
                ShellyOutputState(
                    name = c.name ?: "",
//                    state = outputs.await().single { it.id == id }.output,
                    outputType = c.inMode,
                    actionOnPower = c.initialState,
                )
            },
        )
    }

    override suspend fun applyDiff(diff: Diff) = withContext(Dispatchers.IO) {
        val shellyClient = shellyClient()
        val nameChanges = diff.changes.filter { it.path.first() == "name" }
        val inputChanges =
            diff.changes.filter { it.path.first() == "inputs" }.groupBy { it.path[1].toInt() }.mapValues {
                it.value.associate { it.path[2] to it.newValue }.toMutableMap()
            }
        val outputChanges =
            diff.changes.filter { it.path.first() == "outputs" }.groupBy { it.path[1].toInt() }.mapValues {
                it.value.associate { it.path[2] to it.newValue }.toMutableMap()
            }

        require(diff.changes.all { it.path.first() in setOf("name", "inputs", "outputs") }) {
            "Unsupported changes: ${diff.changes}"
        }

        require(nameChanges.size <= 1) { "Only one name change is supported: $nameChanges" }
        nameChanges.forEach { change ->
            launch { shellyClient.setName(change.newValue?.asText() ?: "") }
        }
        inputChanges.forEach { (id, changesForId) ->
            launch {
                val name = changesForId.remove("name")?.asText()
                val type = changesForId.remove("type")?.let {
                    jsonObjectMapper.treeToValue<ShellyInputType>(it)
                }
                val enabled = changesForId.remove("enabled")?.asBoolean()
                require(changesForId.isEmpty()) { "Unsupported input changes: ${changesForId.keys}" }
                shellyClient.setInputConfig(
                    id,
                    name = name,
                    type = type,
                    enable = enabled,
                )
            }
        }
        outputChanges.forEach { (id, changesForId) ->
            launch {
                val name = changesForId.remove("name")?.asText()
                val outputType = changesForId.remove("outputType")?.let {
                    jsonObjectMapper.treeToValue<ShellySwitchInMode>(it)
                }
                val actionOnPower = changesForId.remove("actionOnPower")?.let {
                    jsonObjectMapper.treeToValue<ShellySwitchInitialState>(it)
                }
//                val state = changesForId.remove("state")?.asBoolean()
                require(changesForId.isEmpty()) { "Unsupported output changes: ${changesForId.keys}" }
                shellyClient.setSwitchConfig(
                    id,
                    name = name,
                    inMode = outputType,
                    initialState = actionOnPower,
                )
//                shellyClient.setSwitch(id, state)
            }
        }
    }

    override fun toString(): String {
        return "ShellyProvider(host=${castConfig.host})"
    }
}
