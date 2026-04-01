package com.silentroom

import com.silentroom.config.*
import com.silentroom.di.appModule
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    configureSerialization()
    configureCors()
    configureAuth()
    configureWebSockets()
    configureStatusPages()
    configureCallLogging()
    configureDatabase()
    configureRouting()
}
