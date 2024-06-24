package at.robert

import at.robert.provider.Provider
import at.robert.provider.getProvider
import at.robert.util.SimpleCache
import at.robert.util.jsonObjectMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.time.Duration

class ConfigEvaluator(
    private val config: Config,
) {
    private val cache = SimpleCache(defaultLifetime = Duration.ofSeconds(5))

    private val providers: Map<ProviderConfig, Provider<*>> =
        config.states
            .asSequence()
            .map {
                it.provider
            }
            .distinct()
            .associate {
                try {
                    it to getProvider(it.name, it.config)
                } catch (ex: Exception) {
                    println("Error loading provider ${it.name}: ${ex.message} (${ex.javaClass.simpleName})")
                    throw ex
                }
            }

    private suspend fun fetchCurrentStatesPerProvider(): Map<Provider<*>, Any?> = cache.cached("currentStates") {
        return@cached config.states.map { st ->
            async {
                val provider = providers.getValue(st.provider)
                provider to provider.currentState()
            }
        }.awaitAll().toMap()
    }

    private suspend fun computeFullConfiguredStates() = cache.cached("fullConfiguredState") {
        val currentStates = fetchCurrentStatesPerProvider()
        return@cached config.states.map { st ->
            val provider = providers.getValue(st.provider)
            val currentState = currentStates.getValue(provider)
            st.copy(
                provider = st.provider.copy(
                    config = jsonObjectMapper.valueToTree(provider.config)
                ),
                state = jsonObjectMapper.readerForUpdating(jsonObjectMapper.valueToTree(currentState))
                    .readValue(st.state)
            )
        }
    }

    suspend fun computeFullConfig(): Config {
        return config.copy(
            states = computeFullConfiguredStates()
                .sortedWith(
                    compareBy(
                        { it.provider.name },
                        { it.provider.config.toString() },
                        { it.state.toString() })
                )
        )
    }

    suspend fun calculateChanges(): List<Pair<ConfigState, Diff>> = cache.cached("diff") {
        val currentStates = fetchCurrentStatesPerProvider()
        val fullConfiguredStates = computeFullConfiguredStates()
        val statePairs = fullConfiguredStates.map {
            currentStates.getValue(getProvider(it.provider.name, it.provider.config)) to it
        }
        return@cached statePairs
            .map { (current, configState) ->
                configState to diff(current, configState.state)
            }
            .filter { it.second.changes.isNotEmpty() }
    }

    fun getProvider(provider: ProviderConfig): Provider<*> {
        return providers.getValue(provider)
    }

}
