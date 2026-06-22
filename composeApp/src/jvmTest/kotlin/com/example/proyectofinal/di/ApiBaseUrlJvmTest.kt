package com.example.proyectofinal.di

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiBaseUrlJvmTest {
    private var originalApiBaseUrlProperty: String? = null

    @BeforeTest
    fun setUp() {
        originalApiBaseUrlProperty = System.getProperty(API_BASE_URL_PROPERTY)
        System.clearProperty(API_BASE_URL_PROPERTY)
    }

    @AfterTest
    fun tearDown() {
        if (originalApiBaseUrlProperty == null) {
            System.clearProperty(API_BASE_URL_PROPERTY)
        } else {
            System.setProperty(API_BASE_URL_PROPERTY, originalApiBaseUrlProperty!!)
        }
    }

    @Test
    fun `resolver uses environment override when present or localhost fallback otherwise`() {
        val environmentValue = System.getenv(API_BASE_URL_ENVIRONMENT)
        val expected = environmentValue ?: DEFAULT_JVM_API_BASE_URL

        assertEquals(
            expected,
            getApiBaseUrl(),
            "When $API_BASE_URL_ENVIRONMENT is defined for the JVM test process, it is the active default; localhost fallback only applies when that environment variable is absent.",
        )
    }

    @Test
    fun `resolver prefers system property override over environment and localhost defaults`() {
        System.setProperty(API_BASE_URL_PROPERTY, "http://192.168.1.42:8080")

        assertEquals("http://192.168.1.42:8080", getApiBaseUrl())
    }

    private companion object {
        const val API_BASE_URL_PROPERTY = "api.base.url"
        const val API_BASE_URL_ENVIRONMENT = "API_BASE_URL"
        const val DEFAULT_JVM_API_BASE_URL = "http://localhost:8080"
    }
}
