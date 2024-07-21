package at.robert.hf.page

import at.robert.hf.Once
import at.robert.hf.basicFormInput
import at.robert.hf.htmx.*
import at.robert.shelly.ShellyClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.html.*

class ShellyPage(
    private val shellyHost: String,
) : HtmxPage() {
    private val shellyClient = ShellyClient(shellyHost)
    private val name = Once { shellyClient.getName() }

    override suspend fun title(): String {
        return "Shelly - ${name.get()} - $shellyHost"
    }

    override suspend fun renderHeader(render: TagConsumer<*>) {
        name.get().let { name ->
            render.apply {
                h1 { +name }
                p {
                    val shellyUrl = "http://$shellyHost"
                    a(href = shellyUrl) {
                        +shellyUrl
                    }
                }
            }
        }
    }

    inner class RenameComponent : HtmxComponent {
        override val id: String
            get() = "rename-shelly"

        override suspend fun render(
            render: TagConsumer<*>,
            params: Parameters,
            hxCtx: HtmxContext
        ) {
            val name = if (params.contains("name")) {
                shellyClient.setName(params["name"]!!)
                params["name"]!!
            } else {
                name.get()
            }
            render.apply {
                div("box") {
                    hxActionForm(
                        hxCtx = hxCtx,
                        actionLabel = "Rename"
                    ) {
                        basicFormInput(
                            paramName = "name",
                            label = "Name",
                            defaultValue = name
                        )
                    }
                }
            }
        }
    }

    inner class SwitchComponent(
        private val switchId: Int
    ) : HtmxComponent {
        override val id: String
            get() = "switch-$switchId"

        override suspend fun render(render: TagConsumer<*>, params: Parameters, hxCtx: HtmxContext) {
            if (params.contains("toggle")) {
                shellyClient.toggleSwitch(switchId)
            }
            val name = if (params.contains("name")) {
                shellyClient.setSwitchConfig(switchId, params["name"]!!)
                params["name"]!!
            } else {
                shellyClient.getSwitchConfig(switchId).name
            }
            render.div("box") {
                h1 { +"Switch $switchId" }
                hxActionForm(
                    hxCtx = hxCtx,
                    actionLabel = "Rename"
                ) {
                    basicFormInput(
                        paramName = "name",
                        label = "Name",
                        defaultValue = name
                    )
                }
                button {
                    hxPost(hxCtx, "toggle" to true)
                    +"Toggle"
                }
            }
        }
    }

    inner class InputComponent(
        private val inputId: Int,
    ) : HtmxComponent {
        override val id: String
            get() = "input-$inputId"

        override suspend fun render(render: TagConsumer<*>, params: Parameters, hxCtx: HtmxContext) {
            val name = if (params.contains("name")) {
                shellyClient.setInputConfig(inputId, params["name"]!!)
                params["name"]!!
            } else {
                shellyClient.getInputConfig(inputId).name
            }
            render.div("box") {
                h1 { +"Input $inputId" }
                hxActionForm(
                    hxCtx = hxCtx,
                    actionLabel = "Rename"
                ) {
                    basicFormInput(
                        paramName = "name",
                        label = "Name",
                        defaultValue = name
                    )
                }
            }
        }
    }

    override suspend fun components(): List<HtmxComponent> =
        coroutineScope {
            val inputsD = async { shellyClient.getInputs() }
            val outputsD = async { shellyClient.getSwitches() }
//            val coversD = async {
//                shellyClient.getCovers().map {
//                    it to async { shellyClient.getCoverConfig(it.id) }
//                }.map { it.first to it.second.await() }
//            }

            return@coroutineScope buildList {
                add(RenameComponent())
                add(simpleComponent("inputHeader") { h2 { +"Inputs" } })
                inputsD.await().forEach { input ->
                    add(InputComponent(input.id))
                }
                add(simpleComponent("outputHeader") { h2 { +"Switches" } })
                outputsD.await().forEach { output ->
                    add(SwitchComponent(output.id))
                }
            }
        }

    override suspend fun getComponent(id: String): HtmxComponent =
        when {
            id == "rename-shelly" -> RenameComponent()
            id.startsWith("input-") -> InputComponent(id.substringAfter("input-").toInt())
            id.startsWith("switch-") -> SwitchComponent(id.substringAfter("switch-").toInt())
            else -> TODO()
        }

}

fun Application.registerShellyPage() {
    routing {
        HtmxRenderer.registerPage(this, "/shelly/{config}/{host}") { parameters ->
            val host = parameters["host"]!!
            ShellyPage(host)
        }
    }
}
