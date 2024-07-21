package at.robert.hf.htmx

import io.ktor.http.*
import kotlinx.html.TagConsumer

interface HtmxComponent {
    val id: String

    suspend fun render(render: TagConsumer<*>, params: Parameters, hxCtx: HtmxContext)
}
