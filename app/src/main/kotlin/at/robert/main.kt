package at.robert

import at.robert.provider.getProvider
import com.fasterxml.jackson.module.kotlin.readValue
import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(name = "hf", mixinStandardHelpOptions = true)
class HomeFormer : Callable<Int> {
    @Parameters(paramLabel = "config file", description = ["The configuration file to use"])
    lateinit var configFile: File

    @Option(names = ["-u", "--update"], description = ["Update the configuration file in-place"])
    var updateConfig: Boolean = false

    @Option(
        names = ["-d", "--diff", "--dry-run", "-dr"],
        description = ["Show what actions would be taken to apply the configuration"]
    )
    var showDiff: Boolean = false

    @Option(names = ["-a", "--apply"], description = ["Apply the configuration to the system"])
    var applyConfig: Boolean = false

    override fun call(): Int {
        if (!configFile.exists()) {
            println("Config file does not exist")
            return 1
        }
        if (!configFile.isFile) {
            println("Config file is not a file")
            return 1
        }

        val config = yamlObjectMapper.readValue<Config>(configFile)
        val providers = config.states.asSequence().map {
            it.provider
        }.distinct().associate {
            try {
                it to getProvider(it.name, it.config)
            } catch (ex: Exception) {
                println("Error loading provider ${it.name}: ${ex.message} (${ex.javaClass.simpleName})")
                return 1
            }
        }

        val states = config.states.map { st ->
            val provider = providers.getValue(st.provider)
            val currentState = provider.currentState()
            st.copy(
                state = mergeStates(jsonObjectMapper.valueToTree(currentState), st.state)
            )
        }

        if (updateConfig) {
            val updatedConfig = yamlObjectMapper.writeValueAsString(
                config.copy(
                    states = states
                )
            )
            if (updatedConfig != configFile.readText()) {
                println("Updating config file")
                configFile.writeText(updatedConfig)
            } else {
                println("No changes to config file")
            }
        }
        if (showDiff) {
            TODO()
        }
        if (applyConfig) {
            TODO()
        }

        if (!updateConfig && !showDiff && !applyConfig) {
            println("No action specified, but config appears valid")
        }

        return 0
    }

}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(HomeFormer()).execute(*args))
