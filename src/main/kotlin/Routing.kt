package com.sev4ikwasd

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import io.ktor.server.plugins.statuspages.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

private val logger = LoggerFactory.getLogger("MonitorLogger")

@Serializable
data class CheckRequest(val urls: List<String>)

@Serializable
data class CheckResult(val url: String, val statusCode: Int, val responseTimeMs: Long, val success: Boolean)

@Serializable
data class ErrorResponse(val error: String)

fun Application.configureRouting() {
    val client = HttpClient(CIO) {
        engine {
            requestTimeout = 10_000
        }
    }

    install(StatusPages) {
        exception<ContentTransformationException> { call, cause ->
            logger.error("Failed to parse request: ${cause.message}", cause)
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid JSON: ${cause.message}"))
        }
        exception<Throwable> { call, cause ->
            logger.error("Unexpected error: ${cause.message}", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    routing {
        post("/api/monitor/check") {
            val request = call.receive<CheckRequest>()
            logger.info("Received check request for URLs: ${request.urls}")

            val results = withContext(Dispatchers.IO) {
                request.urls.map { url ->
                    async { checkUrl(client, url) }
                }.awaitAll()
            }

            call.respond(results)
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, "Healthy")
        }
    }
}

suspend fun checkUrl(client: HttpClient, url: String): CheckResult {
    return try {
        val start = Instant.now()
        val response: HttpResponse = client.get(url)
        val duration = Duration.between(start, Instant.now()).toMillis()
        logger.info("Successful check: $url (status: ${response.status}, time: ${duration}ms)")
        CheckResult(url, response.status.value, duration, true)
    } catch (e: Exception) {
        logger.error("Failed check: $url - ${e.message}")
        CheckResult(url, -1, 0, false)
    }
}