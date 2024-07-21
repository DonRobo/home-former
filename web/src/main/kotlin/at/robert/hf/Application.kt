package at.robert.hf

import at.robert.hf.config.configureErrors
import at.robert.hf.config.configureStaticContent
import at.robert.hf.page.configureConfigPage
import at.robert.hf.page.configureShellyManager
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureErrors()
    configureStaticContent()
    configureConfigPage()
    configureShellyManager()

//    configureHtmxPage()
}

