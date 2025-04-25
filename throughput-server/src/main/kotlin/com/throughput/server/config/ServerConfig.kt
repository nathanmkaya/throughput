package com.throughput.server.config

import com.throughput.common.util.Constants
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * Server configuration parameters
 */
data class ServerConfig(
    val host: String = Constants.DEFAULT_HOST,
    val port: Int = Constants.DEFAULT_PORT,
    val timeoutMillis: Long = Constants.DEFAULT_TIMEOUT_MS
)
