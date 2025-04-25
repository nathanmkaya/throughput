package com.throughput.client.model

import com.throughput.common.util.Constants
import io.ktor.client.engine.*

/**
 * Configuration for the ThroughputClient
 */
data class ClientConfig(
    val protocol: String = "http",
    val host: String = "localhost",
    val port: Int = Constants.DEFAULT_PORT,
    val apiVersion: String = Constants.API_VERSION,
    val connectionTimeoutMs: Long = Constants.DEFAULT_TIMEOUT_MS,
    val socketTimeoutMs: Long = Constants.DEFAULT_TIMEOUT_MS,
    val requestTimeoutMs: Long = Constants.DEFAULT_TIMEOUT_MS,
    val engine: HttpClientEngineFactory<*>? = null
) {
    /**
     * Get the base URL for API requests
     */
    val baseUrl: String
        get() = "$protocol://$host:$port/$apiVersion"
        
    /**
     * Get the download endpoint URL for REST API requests
     */
    val downloadUrl: String
        get() = "$baseUrl${Constants.DOWNLOAD_ENDPOINT}"
        
    /**
     * Get the upload endpoint URL
     */
    val uploadUrl: String
        get() = "$baseUrl${Constants.UPLOAD_ENDPOINT}"
}
