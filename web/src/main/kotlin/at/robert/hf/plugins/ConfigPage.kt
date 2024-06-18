package at.robert.hf.plugins

import at.robert.hf.ConfigFiles
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Application.configureConfigPage() {
    routing {
        get("/config") {
            call.respondHtmlBody("Config") {
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
                                +config
                            }
                        }
                    }
                }
                form(action = "/config", method = FormMethod.post, encType = FormEncType.multipartFormData) {
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
        post("/config") {
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
            call.respondRedirect("/config")
        }
    }
}

private suspend fun ApplicationCall.respondHtmlBody(title: String, block: BODY.() -> Unit) {
    this.respondHtml {
        head {
            title(title)
        }
        body {
            block()
        }
    }
}
