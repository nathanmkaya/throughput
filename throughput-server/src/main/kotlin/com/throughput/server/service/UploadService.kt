package com.throughput.server.service

import com.throughput.common.model.UploadResult
import io.ktor.utils.io.ByteReadChannel

/**
 * Interface for handling uploads and measuring performance using non-blocking IO
 */
interface UploadService {
    /**
     * Process an upload channel and calculate performance metrics
     * @param channel The ByteReadChannel containing uploaded data
     * @param contentLength Expected length of the upload in bytes
     * @return Upload result with timing information
     */
    suspend fun processUpload(channel: ByteReadChannel, contentLength: Long): UploadResult
    
    /**
     * Check if the upload size is within allowed limits
     * @param contentLength Size of the upload in bytes
     * @return True if size is acceptable, false otherwise
     */
    fun isValidUploadSize(contentLength: Long): Boolean
}
