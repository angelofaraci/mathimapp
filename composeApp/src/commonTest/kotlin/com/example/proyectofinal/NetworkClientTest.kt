package com.example.proyectofinal

import com.example.proyectofinal.di.InMemoryTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkClientTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var tokenStore: InMemoryTokenStore

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tokenStore = InMemoryTokenStore()
    }

    @AfterTest
    fun tearDown() {
        tokenStore.accessToken = null
        Dispatchers.resetMain()
    }

    @Test
    fun `client injects authorization header only when a token exists`() = runTest(dispatcher) {
        val headers = mutableListOf<String?>()
        val client = createHttpClient(tokenStore, MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK)
        })

        client.get("https://example.test/ping")
        tokenStore.accessToken = "session-token"
        client.get("https://example.test/ping")

        assertEquals(listOf(null, "Bearer session-token"), headers)
        client.close()
    }

    @Test
    fun `clearing memory token removes authorization header from later requests`() = runTest(dispatcher) {
        val headers = mutableListOf<String?>()
        tokenStore.accessToken = "session-token"
        val client = createHttpClient(tokenStore, MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK)
        })

        client.get("https://example.test/ping")
        tokenStore.accessToken = null
        client.get("https://example.test/ping")

        assertEquals(listOf("Bearer session-token", null), headers)
        client.close()
    }
}
