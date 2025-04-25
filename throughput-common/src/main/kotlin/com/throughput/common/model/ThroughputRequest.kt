package com.throughput.common.model

import kotlinx.serialization.Serializable

/**
 * Request to specify the size of data to transfer in bytes
 */
@Serializable
data class ThroughputRequest(
    val sizeBytes: Long
)
