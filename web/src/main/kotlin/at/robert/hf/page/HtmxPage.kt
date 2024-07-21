package at.robert.hf.page

import at.robert.hf.respondHtmlBody
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

interface HtmxComponent {
    val id: String

    fun render(flowContent: FlowContent, params: Parameters, hxCtx: HtmxContext)
}

private val objectMapper = jacksonObjectMapper()

private fun Tag.hxPost(hxCtx: HtmxContext, vararg params: Pair<String, Any>) {
    attributes["hx-post"] = hxCtx.route
    attributes["hx-target"] = "#${hxCtx.component}"
    attributes["hx-swap"] = "outerHTML"
    attributes["hx-vals"] = params.toMap().let { objectMapper.writeValueAsString(it) }
}

data class HtmxContext(
    val route: String,
    val component: String,
)


abstract class HtmxPage(
    private val title: String
) {

    open fun renderHeader(flowContent: FlowContent) {
        flowContent.apply {
            h1 {
                +this@HtmxPage.title
            }
        }
    }

    abstract fun components(): List<HtmxComponent>
    open fun getComponent(id: String): HtmxComponent? = components().find { it.id == id }

    fun registerRoutes(routing: Route, route: String) {
        routing.get(route) {
            call.respondHtmlBody(title, includeHtmx = true) {
                main {
                    renderHeader(this)
                    components().forEach { component ->
                        div {
                            id = component.id
                            component.render(this, Parameters.Empty, HtmxContext(route, component.id))
                        }
                    }
                }
            }
        }
        routing.post(route) {
            require(call.request.header("HX-Request") == "true")
            val compId = call.request.header("HX-Target")!!
            val params = call.receiveParameters()
            val text = buildString {
                appendHTML().apply {
                    val component = getComponent(compId)!!
                    div {
                        id = component.id
                        component.render(this, params, HtmxContext(route, component.id))
                    }
                }
            }
            call.respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8)))
        }
    }
}

object HtmxTestPage : HtmxPage("Htmx Test Page") {
    object BasicHtmxComponent : HtmxComponent {
        override val id: String
            get() = "comp1"

        override fun render(
            flowContent: FlowContent,
            params: Parameters,
            hxCtx: HtmxContext
        ) {
            flowContent.apply {
                p { +"This is a component" }
                params.names().forEach {
                    div {
                        +"$it: ${params[it]}"
                    }
                }
                button {
                    hxPost(
                        hxCtx,
                        "greeting" to "Hello hxPost!",
                    )
                    +"Click me"
                }
            }
        }
    }

    override fun components(): List<HtmxComponent> {
        return listOf(BasicHtmxComponent)
    }

}

fun Application.configureHtmxPage() {
    routing {
        HtmxTestPage.registerRoutes(this, "/htmx")
    }
}
