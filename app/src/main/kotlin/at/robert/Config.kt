package at.robert

data class Config(
    val states: List<ConfigState>
)

data class ProviderConfig(
    val name: String,
    val config: Any?,
)

data class ConfigState(
    val provider: ProviderConfig,
    val state: Any,
)
