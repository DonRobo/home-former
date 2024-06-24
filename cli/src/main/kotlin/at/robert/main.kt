package at.robert

import at.robert.util.yamlObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        val config = ConfigEvaluator(yamlObjectMapper.readValue<Config>(configFile))

        runBlocking {
            if (updateConfig) {
                val updatedConfig = yamlObjectMapper.writeValueAsString(config.computeFullConfig())
                if (updatedConfig != configFile.readText()) {
                    println("Updating config file")
                    configFile.writeText(updatedConfig)
                } else {
                    println("No changes to config file")
                }
            }

            if (showDiff) {
                val calculatedChanges = config.calculateChanges()
                if (calculatedChanges.isNotEmpty()) {
                    println("*Changes to apply*")
                    calculatedChanges.forEach { (c, diff) ->
                        val provider = config.getProvider(c.provider)
                        println("Changes for $provider:")
                        diff.changes.forEach {
                            println("  $it")
                        }
                    }
                } else {
                    println("No changes found")
                }
            }
            if (applyConfig) {
                val calculatedChanges = config.calculateChanges()
                if (calculatedChanges.isNotEmpty()) {
                    calculatedChanges.forEach { (configState, diff) ->
                        val provider = config.getProvider(configState.provider)
                        println("Applying changes to $provider:")
                        diff.changes.forEach {
                            println(it)
                        }
                        launch { provider.applyDiff(diff) }
                    }
                } else {
                    println("No changes to apply")
                }
            }
        }

        if (!updateConfig && !showDiff && !applyConfig) {
            println("No action specified, but config appears valid")
        }

        return 0
    }

}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(HomeFormer()).execute(*args))
