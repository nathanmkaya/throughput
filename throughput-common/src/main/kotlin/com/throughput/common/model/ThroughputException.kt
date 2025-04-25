package com.throughput.common.model

/**
 * Exception for throughput-related errors
 */
class ThroughputException(
    message: String, 
    cause: Throwable? = null
) : Exception(message, cause)
