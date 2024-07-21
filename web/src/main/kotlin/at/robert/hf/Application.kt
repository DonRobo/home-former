package at.robert.hf

import at.robert.hf.page.configureConfigPage
import at.robert.hf.page.configureErrors
import at.robert.hf.page.configureShellyManager
import at.robert.hf.page.configureStaticContent
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureErrors()
    configureStaticContent()
    configureConfigPage()
    configureShellyManager()
}

