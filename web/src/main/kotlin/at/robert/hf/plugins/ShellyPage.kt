package at.robert.hf.plugins

import at.robert.hf.respondHtmlBody
import at.robert.shelly.ShellyClient
import at.robert.shelly.client.schema.`in`.ShellyCover
import at.robert.shelly.client.schema.`in`.ShellyCoverConfig
import at.robert.shelly.client.schema.`in`.ShellyOutput
import at.robert.shelly.client.schema.`in`.ShellySwitchConfig
import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.html.*

suspend fun PipelineContext<*, ApplicationCall>.shellyPage(shellyHost: String) {
    val shellyClient = ShellyClient(shellyHost)
    val name: String
//    val inputs: List<ShellyInput>
    val outputs: List<Pair<ShellyOutput, ShellySwitchConfig>>
    val covers: List<Pair<ShellyCover, ShellyCoverConfig>>

    coroutineScope {
        val nameD = async { shellyClient.getName() }
//        val inputsD = async { shellyClient.getInputs() }
        val outputsD = async {
            shellyClient.getSwitches().map {
                it to async { shellyClient.getSwitchConfig(it.id) }
            }.map { it.first to it.second.await() }
        }
        val coversD = async {
            shellyClient.getCovers().map {
                it to async { shellyClient.getCoverConfig(it.id) }
            }.map { it.first to it.second.await() }
        }

        name = nameD.await()
//        inputs = inputsD.await()
        outputs = outputsD.await()
        covers = coversD.await()
    }

    call.respondHtmlBody("Shelly - $name - $shellyHost", includeHtmx = true) {
        main {
            h1 { +name }
            p {
                val shellyUrl = "http://$shellyHost"
                a(href = shellyUrl) {
                    +shellyUrl
                }
            }

            shellyActionForm(
                shellyHost = shellyHost,
                action = "rename",
                actionLabel = "Rename"
            ) {
                basicFormInput(
                    paramName = "name",
                    label = "Name",
                    defaultValue = name
                )
            }

            if (outputs.isNotEmpty()) {
                h2 { +"Switches" }
                outputs.forEach { (output, config) ->
                    div("box") {
                        p { +"Switch ${output.id}" }
                        shellyActionForm(
                            shellyHost = shellyHost,
                            action = "rename-switch",
                            actionLabel = "Rename"
                        ) {
                            hidden("output", output.id)
                            basicFormInput(
                                paramName = "name",
                                label = "Name",
                                defaultValue = config.name,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.shellyActionForm(
    shellyHost: String,
    action: String,
    actionLabel: String,
    block: FORM.() -> Unit
) {
    form(
        action = "shelly",
        method = FormMethod.post
    ) {
        block()
        input(type = InputType.hidden, name = "action") {
            value = action
        }
        input(type = InputType.hidden, name = "shelly") {
            value = shellyHost
        }
        input(type = InputType.submit) {
            value = actionLabel
        }
    }
}

private fun FORM.basicFormInput(paramName: String, label: String, defaultValue: String?) {
    label {
        htmlFor = paramName
        +label
    }
    input(type = InputType.text, name = paramName) {
        defaultValue?.let { value = it }
        id = paramName
    }
}

private fun FORM.hidden(paramName: String, value: Any) {
    input(type = InputType.hidden, name = paramName) {
        this.value = value.toString()
    }
}
