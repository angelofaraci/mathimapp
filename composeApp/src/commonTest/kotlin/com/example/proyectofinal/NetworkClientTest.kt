package com.example.proyectofinal

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkClientTest {
    @BeforeTest
    fun setUp() {
        TokenHolder.accessToken = null
    }

    @AfterTest
    fun tearDown() {
        TokenHolder.accessToken = null
    }

    @Test
    fun `client injects authorization header only when a token exists`() = runTest {
        val headers = mutableListOf<String?>()
        val client = createHttpClient(MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK)
        })

        client.get("https://example.test/ping")
        TokenHolder.accessToken = "session-token"
        client.get("https://example.test/ping")

        assertEquals(listOf(null, "Bearer session-token"), headers)
        client.close()
    }

    @Test
    fun `clearing memory token removes authorization header from later requests`() = runTest {
        val headers = mutableListOf<String?>()
        TokenHolder.accessToken = "session-token"
        val client = createHttpClient(MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK)
        })

        client.get("https://example.test/ping")
        TokenHolder.accessToken = null
        client.get("https://example.test/ping")

        assertEquals(listOf("Bearer session-token", null), headers)
        client.close()
    }
}
