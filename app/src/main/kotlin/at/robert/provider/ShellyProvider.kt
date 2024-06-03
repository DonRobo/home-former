package at.robert.provider

import at.robert.Diff
import at.robert.shelly.ShellyClient
import at.robert.util.jsonObjectMapper
import com.fasterxml.jackson.databind.JsonNode

data class ShellyState(
    val name: String,
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

    override suspend fun currentState(): ShellyState {
        return ShellyState(shellyClient().getName())
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
