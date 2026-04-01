package com.silentroom.di

import com.silentroom.service.ChatService
import com.silentroom.service.ContactService
import com.silentroom.service.TokenService
import com.silentroom.service.UserService
import io.ktor.server.application.*
import org.koin.dsl.module

val appModule = module {
    single { UserService() }
    single { ContactService() }
    single { ChatService() }
    single { (application: Application) ->
        val config = application.environment.config
        TokenService(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
            expiresIn = config.property("jwt.expiresIn").getString().toLong()
        )
    }
}
