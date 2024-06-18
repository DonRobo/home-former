package at.robert.hf

import at.robert.hf.plugins.configureConfigPage
import at.robert.hf.plugins.configureErrors
import at.robert.hf.plugins.configureStaticContent
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureErrors()
    configureStaticContent()
    configureConfigPage()
}

