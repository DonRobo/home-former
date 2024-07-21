package at.robert.hf

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import kotlinx.html.*

suspend fun ApplicationCall.respondHtmlBody(title: String, includeHtmx: Boolean = false, block: BODY.() -> Unit) {
    this.respondHtml {
        head {
            base {
                href = basePath
            }
            title(title)
            link(rel = "stylesheet", href = "https://unpkg.com/missing.css@1.1.2")
            if (includeHtmx) {
                script(src = "https://unpkg.com/htmx.org@2.0.1") {
                    integrity = "sha384-QWGpdj554B4ETpJJC9z+ZHJcA/i59TyjxEPXiiUgN2WmTyV5OEZWCD6gQhgkdpB/"
                    attributes["crossorigin"] = "anonymous"
                }
            }
        }
        body {
            block()
        }
    }
}

val ApplicationCall.basePath: String
    get() = (this.request.header("X-Ingress-Path") ?: "/").let {
        if (it.endsWith("/"))
            return it
        else
            return "$it/"
    }
