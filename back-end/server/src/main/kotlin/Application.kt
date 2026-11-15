package it.inkorporate

import io.ktor.server.application.Application

fun Application.rootModule() {
    configureRouting()
    configureSockets()
}
