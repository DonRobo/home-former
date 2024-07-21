package at.robert.hf.page

import at.robert.hf.ConfigFiles
import at.robert.hf.respondHtmlBody
import at.robert.shelly.ShellyClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.html.*

fun Application.configureShellyManager() {
    routing {
        get("/shelly") {
            configSelectionPage()
        }
        get("/shelly/{config}") {
            shellySelectionPage(call.parameters["config"]!!)
        }
    }
}

private suspend fun PipelineContext<*, ApplicationCall>.shellySelectionPage(configFileName: String) {
    coroutineScope {
        val config = ConfigFiles.getConfig(configFileName)
        val shellys = config.states.mapNotNull {
            if (it.provider.name != "ShellyProvider") return@mapNotNull null

            it.provider.config?.get("host")?.textValue()?.let { host ->
                host to ShellyClient(host)
            }
        }.toMap()

        val shellyNames = shellys
            .mapValues { (_, shelly) -> async { shelly.getName() } }
            .mapValues { it.value.await() }

        call.respondHtmlBody("Shelly Manager") {
            main {
                h1 { +"Shelly Manager" }
                shellys.forEach { (host, _) ->
                    p {
                        a(href = "shelly/${configFileName.encodeURLPathPart()}/${host.encodeURLPathPart()}") {
                            +"Shelly: ${shellyNames[host]}"
                        }
                        br { }
                        val hostUrl = "http://$host"
                        a(href = hostUrl, classes = "italic") {
                            +hostUrl
                        }
                    }
                }
            }
        }
    }
}

suspend fun PipelineContext<*, ApplicationCall>.configSelectionPage() {
    call.respondHtmlBody("Shelly Manager") {
        main {
            h1 { +"Shelly Manager" }
            p { +"Please select a config" }
            ConfigFiles.getConfigs().forEach {
                p {
                    a(href = "shelly/${it.encodeURLPathPart()}") { +it }
                }
            }
        }
    }
}

private suspend fun PipelineContext<*, ApplicationCall>.shellyManagerAction() {
    val formParams = call.receiveParameters()

    val action = formParams["action"] ?: error("Need action")
    val shellyIp = formParams["shelly"] ?: error("Need shelly host")

    val shellyClient = ShellyClient(shellyIp)
    when (action) {
        "rename" -> {
            shellyClient.setName(formParams["name"] ?: error("Need name"))
        }

        "rename-switch" -> {
            val outputId = formParams["output"]?.toInt() ?: error("Need output")
            val newName = formParams["name"] ?: error("Need name")
            shellyClient.setSwitchConfig(outputId, name = newName)
        }

        else -> {
            error("Invalid action $action")
        }
    }

    call.respondRedirect("/shelly?shelly=$shellyIp")
}
