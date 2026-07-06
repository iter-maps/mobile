package it.iterapp.core.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Tolerant by design: unknown keys are additive server evolution, never an
 * error (ADR 0005).
 */
val IterJson: Json = Json {
  ignoreUnknownKeys = true
  isLenient = true
  explicitNulls = false
}

fun iterHttpClient(engine: HttpClientEngine? = null): HttpClient {
  val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
    expectSuccess = false
    defaultRequest {
      header("x-request-id", randomRequestId())
    }
    install(ContentNegotiation) {
      json(IterJson)
    }
    install(HttpTimeout) {
      requestTimeoutMillis = 30_000
      connectTimeoutMillis = 8_000
      socketTimeoutMillis = 30_000
    }
  }
  return if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
}
