package com.throughput.server.service

import io.ktor.utils.io.ByteWriteChannel

/**
 * Interface for generating files of specified sizes using non-blocking IO
 */
interface FileGeneratorService {
    /**
     * Generate random binary data of requested size directly to the output channel
     * @param sizeBytes Size of data to generate in bytes
     * @param channel Channel to write the data to
     */
    suspend fun generateRandomData(sizeBytes: Long, channel: ByteWriteChannel)
    
    /**
     * Check if the requested size is valid within system constraints
     * @param sizeBytes Size to validate
     * @return True if size is valid, false otherwise
     */
    fun isValidSize(sizeBytes: Long): Boolean
}
