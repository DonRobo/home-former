package at.robert.hf.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureErrors() {
    install(StatusPages) {
        status(
            HttpStatusCode.NotFound,
            HttpStatusCode.MethodNotAllowed,
            HttpStatusCode.InternalServerError
        ) { call, status ->
            call.respondText(text = "${status.value}: ${status.description}", status = status)
        }
    }
}
