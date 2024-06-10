package at.robert

import at.robert.util.jsonObjectMapper
import com.fasterxml.jackson.databind.JsonNode

data class Config(
    val states: List<ConfigState>
)

data class ProviderConfig(
    val name: String,
    val config: JsonNode?,
)

data class ConfigState(
    val provider: ProviderConfig,
    val state: JsonNode = jsonObjectMapper.createObjectNode(),
)
