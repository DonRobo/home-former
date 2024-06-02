package at.robert

import at.robert.provider.DummyState

fun main() {
    val config = Config(
        listOf(
            ConfigState(
                ProviderConfig("DummyStateProvider", null),
                jsonObjectMapper.valueToTree(DummyState(null))
            )
        )
    )
    val json = jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config)
    println(json)
    println("----")
    val yaml = yamlObjectMapper.writeValueAsString(config)
    println(yaml)
}
