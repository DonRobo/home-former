package at.robert.hf.plugins

import at.robert.ConfigEvaluator
import at.robert.hf.ConfigFiles
import at.robert.hf.basePath
import at.robert.hf.respondHtmlBody
import at.robert.util.yamlObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

private fun BODY.configList() {
    main {
        h1 {
            +"Available Configs"
        }
        val configs = ConfigFiles.getConfigs()
        if (configs.isEmpty()) {
            +"No configs available"
        } else {
            ul {
                configs.forEach { config ->
                    li {
                        a(href = "config/$config") {
                            +config
                        }
                    }
                }
            }
        }
        form(action = "config", method = FormMethod.post, encType = FormEncType.multipartFormData) {
            h2 {
                +"Upload Config"
            }
            input(type = InputType.file, name = "file") {
                accept = "text/yaml"
            }
            input(type = InputType.submit) {
                value = "Upload"
            }

        }
    }
}

private suspend fun uploadConfigFile(call: ApplicationCall) {
    val formParams = call.receiveMultipart()
    formParams.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                ConfigFiles.uploadConfig(
                    part.originalFileName!!,
                    part.streamProvider().bufferedReader().use { it.readText() }
                )
            }

            else -> {
                println("Unexpected part type: ${part.javaClass}")
            }
        }
        part.dispose()
    }
    call.respondRedirect("${call.basePath}config")
}

private suspend fun specificConfigPage(call: ApplicationCall) {
    val name = call.parameters["name"] ?: return call.respondText(
        "Missing name",
        status = HttpStatusCode.BadRequest
    )

    val config = ConfigFiles.getConfig(name)
    val configEvaluator = ConfigEvaluator(config)
    val diff = configEvaluator.calculateChanges()
    val fullConfig = configEvaluator.computeFullConfig()

    call.respondHtmlBody("Config - $name") {
        main {
            h1 {
                +"Config $name"
            }
            div {
                config.states.forEach { state ->
                    h2 {
                        +"${state.provider.name} - ${state.provider.config}"
                    }
                    renderJson(state.state)
                }
            }
            div {
                h2 {
                    +"Diff"
                }
                diff.forEach { (configState, diff) ->
                    h3 {
                        +"${configState.provider.name} - ${configState.provider.config}"
                    }
                    diff.changes.forEach {
                        div {
                            +it.toString()
                        }
                    }
                }
            }
            div {
                h2 {
                    +"Full Config"
                }
                pre {
                    +yamlObjectMapper.writeValueAsString(fullConfig)
                }
            }
        }
    }
}

private fun FlowContent.renderJson(json: JsonNode) {
    when (json) {
        is ObjectNode -> {
            details {
                summary {
                    +"Show"
                }
                span {
                    json.fieldNames().forEach {
                        val fieldName = it
                        val fieldValue = json.get(fieldName)
                        div {
                            attributes["style"] = "margin-left: 1rem;"
                            h3 {
                                +fieldName
                            }
                            renderJson(fieldValue)
                        }
                    }
                }
            }
        }

        is IntNode -> div {
            attributes["style"] = "margin-left: 1rem;"
            +json.intValue().toString()
        }

        is TextNode -> div {
            attributes["style"] = "margin-left: 1rem;"
            +json.textValue()
        }

        else -> div {
            attributes["style"] = "margin-left: 1rem;"
            +"Unknown type: ${json.javaClass.simpleName}"
            br
            +json.toString()
        }
    }
}

fun Application.configureConfigPage() {
    routing {
        get("/config") {
            call.respondHtmlBody("Config", block = BODY::configList)
        }
        post("/config") {
            uploadConfigFile(call)
        }
        get("/config/{name}") {
            specificConfigPage(call)
        }
    }
}
