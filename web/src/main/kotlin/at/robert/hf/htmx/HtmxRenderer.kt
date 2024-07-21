package at.robert.hf.htmx

import at.robert.hf.respondHtmlBody
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.main
import kotlinx.html.stream.appendHTML
import kotlinx.html.unsafe

object HtmxRenderer {
    private suspend fun renderHtmxPage(
        page: HtmxPage,
        currentRoute: String,
        call: ApplicationCall
    ) = coroutineScope {
        val title = page.title()

        val headerD = async {
            buildString {
                page.renderHeader(appendHTML())
            }
        }

        val renderedComponentsD = page.components().associateWith { comp ->
            async {
                buildString {
                    comp.render(appendHTML(), Parameters.Empty, HtmxContext(currentRoute, comp.id))
                }
            }
        }

        val header = headerD.await()
        val renderedComponents = renderedComponentsD.mapValues { it.value.await() }

        call.respondHtmlBody(title, includeHtmx = true) {
            main {
                unsafe {
                    raw(header)
                }
                renderedComponents.forEach { (component, content) ->
                    div {
                        id = component.id
                        unsafe {
                            raw(content)
                        }
                    }
                }
            }
        }
    }

    private suspend fun renderHtmxComponent(
        page: HtmxPage,
        compId: String,
        currentRoute: String,
        call: ApplicationCall
    ) = coroutineScope {
        val paramsD = async { call.receiveParameters() }
        val componentD = async { page.getComponent(compId)!! }
        val text = buildString {
            val component = componentD.await()
            component.render(appendHTML(), paramsD.await(), HtmxContext(currentRoute, component.id))
        }
        call.respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8)))
    }

    fun registerPage(routing: Route, route: String, page: HtmxPage) {
        registerPage(routing, route) { page }
    }

    fun registerPage(routing: Route, route: String, block: (Parameters) -> HtmxPage) {
        routing.get(route) {
            val page = block(call.parameters)
            renderHtmxPage(page, call.request.path(), call)
        }
        routing.post(route) {
            val page = block(call.parameters)
            require(call.request.header("HX-Request") == "true")
            val compId = call.request.header("HX-Target")!!
            renderHtmxComponent(page, compId, call.request.path(), call)
        }
    }
}
