package at.robert.hf.page

import at.robert.hf.htmx.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.html.TagConsumer
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.p

object HtmxTestPage : HtmxPage() {
    object BasicHtmxComponent : HtmxComponent {
        override val id: String
            get() = "comp1"

        override suspend fun render(render: TagConsumer<*>, params: Parameters, hxCtx: HtmxContext) {
            render.apply {
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

    override suspend fun title() = "Htmx Test Page"

    override suspend fun components(): List<HtmxComponent> {
        return listOf(BasicHtmxComponent)
    }

}

fun Application.configureHtmxPage() {
    routing {
        HtmxRenderer.registerPage(this, "/htmx", HtmxTestPage)
    }
}
