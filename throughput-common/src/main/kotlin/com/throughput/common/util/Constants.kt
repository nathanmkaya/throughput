package com.throughput.common.util

/**
 * Application-wide constants
 */
object Constants {
    const val API_VERSION = "v1"
    const val DOWNLOAD_ENDPOINT = "/download"
    const val UPLOAD_ENDPOINT = "/upload"
    const val DEFAULT_PORT = 8080
    const val DEFAULT_HOST = "0.0.0.0"
    const val DEFAULT_TIMEOUT_MS = 60000L
    
    // Limits
    const val MAX_UPLOAD_SIZE_BYTES = 1024L * 1024 * 1024 // 1 GB
    const val MAX_DOWNLOAD_SIZE_BYTES = 1024L * 1024 * 1024 // 1 GB
}
