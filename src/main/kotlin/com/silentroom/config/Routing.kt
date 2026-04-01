package com.silentroom.config

import com.silentroom.routing.*
import com.silentroom.service.WebSocketService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module

fun Application.configureRouting() {
    val koin = org.koin.core.context.GlobalContext.get()
    koin.loadModules(listOf(org.koin.dsl.module { single { WebSocketService() } }))

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "The Silent Room"))
        }

        userRoutes()
        tokenRoutes()
        contactRoutes()
        chatRoutes()
        webSocketRoutes()
    }
}
