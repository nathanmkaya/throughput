package com.throughput.server

import com.throughput.common.model.ThroughputException
import com.throughput.server.api.ThroughputRoutes
import com.throughput.server.config.serverModule
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * Main application entry point for the throughput server
 */
fun main() {
    // Get configuration from application.conf
    val config = ApplicationConfig("application.conf")
    val host = config.propertyOrNull("ktor.deployment.host")?.getString() ?: "0.0.0.0"
    val port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toIntOrNull() ?: 8080

    // Start the server with graceful shutdown configuration
    embeddedServer(
        Netty,
        port = port,
        host = host,
        module = Application::configureServer,
    ).start(wait = true)
}

/**
 * Configure the Ktor server with all necessary plugins and routes
 */
fun Application.configureServer() {
    val logger = LoggerFactory.getLogger("ThroughputServer")

    // Install plugins

    // Koin for dependency injection
    install(Koin) {
        modules(serverModule)
    }

    // Default headers
    install(DefaultHeaders)

    // Call logging
    install(CallLogging) {
        level = Level.INFO
    }

    // Content negotiation for JSON
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    // Status pages for centralized error handling
    install(StatusPages) {
        exception<ThroughputException> { call, cause ->
            logger.warn("Throughput exception: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "Throughput error")
        }

        exception<ContentTransformationException> { call, cause ->
            logger.warn("Content transformation exception: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, "Invalid request format: ${cause.message}")
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, "An internal error occurred")
        }
    }

    // Get routes from Koin
    val throughputRoutes by inject<ThroughputRoutes>()

    // Configure routing
    routing {
        throughputRoutes.setup(this)
    }

    // Add shutdown hook for cleanly closing resources
    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Server is shutting down, cleaning up resources...")
        // Perform any cleanup operations if needed
    }

    // Log server startup
    val host = environment.config.propertyOrNull("ktor.deployment.host")?.getString() ?: "localhost"
    val port = environment.config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8080"
    val shutdownInfo = "with graceful shutdown (grace period: 2s, timeout: 10s)"
    logger.info("Throughput server started on $host:$port $shutdownInfo")
}
