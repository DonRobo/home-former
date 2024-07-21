package at.robert.hf.htmx

import kotlinx.html.TagConsumer
import kotlinx.html.h1

abstract class HtmxPage {
    open suspend fun renderHeader(render: TagConsumer<*>) {
        val title = title()
        render.h1 {
            +title
        }
    }

    abstract suspend fun title(): String

    abstract suspend fun components(): List<HtmxComponent>
    open suspend fun getComponent(id: String): HtmxComponent? = components().find { it.id == id }
}
