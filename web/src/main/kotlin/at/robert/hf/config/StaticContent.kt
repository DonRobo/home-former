package at.robert.hf.config

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureStaticContent() {
    routing {
        staticResources("/", "static")
    }
}
