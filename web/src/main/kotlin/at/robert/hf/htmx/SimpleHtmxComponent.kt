package at.robert.hf.htmx

import io.ktor.http.*
import kotlinx.html.TagConsumer

class SimpleHtmxComponent(
    override val id: String,
    private val block: (TagConsumer<*>) -> Unit
) : HtmxComponent {
    override suspend fun render(render: TagConsumer<*>, params: Parameters, hxCtx: HtmxContext) {
        block(render)
    }
}

fun simpleComponent(id: String, block: TagConsumer<*>.() -> Unit) = SimpleHtmxComponent(id, block)
