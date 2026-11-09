package com.blitz

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Installs the real server modules into a [testApplication]. The production app wires these
 * via application.yaml; tests run in-process with no port, so we install them explicitly.
 * Every integration test calls this so it exercises the same routing/websocket setup as prod.
 */
fun ApplicationTestBuilder.installServer() {
    application {
        configureWebsockets()
        configureRouting()
    }
}

class ServerTest {

    @Test
    fun `root endpoint returns 200 and greeting`() = testApplication {
        installServer()
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, World!", response.bodyAsText())
    }
}
