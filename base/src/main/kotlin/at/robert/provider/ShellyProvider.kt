package at.robert.provider

import at.robert.Diff
import at.robert.shelly.ShellyClient
import at.robert.shelly.client.schema.`in`.ShellyCoverInMode
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
    val covers: Map<Int, ShellyCoverState>,
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

data class ShellyCoverState(
    val name: String,
    val invertDirection: Boolean,
    val inMode: ShellyCoverInMode,
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
        val covers = async { shellyClient.getCovers() }
        val inputConfigs = inputs.await().associate {
            it.id to async { shellyClient.getInputConfig(it.id) }
        }
        val outputConfigs = outputs.await().associate {
            it.id to async { shellyClient.getSwitchConfig(it.id) }
        }
        val coverConfigs = covers.await().associate {
            it.id to async { shellyClient.getCoverConfig(it.id) }
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
            covers = coverConfigs.mapValues { (id, d) ->
                val c = d.await()
                ShellyCoverState(
                    name = c.name,
                    invertDirection = c.invertDirections,
                    inMode = c.inMode,
                )
            }
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
        val coverChanges =
            diff.changes.filter { it.path.first() == "covers" }.groupBy { it.path[1].toInt() }.mapValues {
                it.value.associate { it.path[2] to it.newValue }.toMutableMap()
            }

        require(diff.changes.all { it.path.first() in setOf("name", "inputs", "outputs", "covers") }) {
            "Unsupported changes: ${diff.changes}"
        }

        require(nameChanges.size <= 1) { "Only one name change is supported: $nameChanges" }
        nameChanges.forEach { change ->
            launch { shellyClient.setName(change.newValue?.asText() ?: "") }
        }
        coroutineScope {
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
//                val state = changesForId.remove("state")?.booleanValue()
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
        coverChanges.forEach { (id, changesForId) ->
            launch {
                val name = changesForId.remove("name")?.asText()
                val invertDirection = changesForId.remove("invertDirection")?.booleanValue()
                val inMode = changesForId.remove("inMode")?.let {
                    jsonObjectMapper.treeToValue<ShellyCoverInMode>(it)
                }
                require(changesForId.isEmpty()) { "Unsupported cover changes: ${changesForId.keys}" }
                shellyClient.setCoverConfig(
                    id,
                    name = name,
                    invertDirections = invertDirection,
                    inMode = inMode,
                )
            }
        }
    }

    override fun toString(): String {
        return "ShellyProvider(host=${castConfig.host})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShellyProvider) return false

        return castConfig == other.castConfig
    }

    override fun hashCode(): Int {
        return castConfig.hashCode()
    }

}
