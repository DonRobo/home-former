package at.robert.hf

import at.robert.hf.plugins.configureDatabases
import at.robert.hf.plugins.configureRouting
import at.robert.hf.plugins.configureTemplating
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabases()
    configureTemplating()
    configureRouting()
}
