package com.throughput.server.service.impl

import com.throughput.common.model.UploadResult
import com.throughput.common.util.Constants
import com.throughput.server.service.UploadService
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory

/**
 * Implementation of UploadService for handling file uploads with non-blocking IO
 */
class UploadServiceImpl : UploadService {
    private val logger = LoggerFactory.getLogger(UploadServiceImpl::class.java)
    
    override suspend fun processUpload(channel: ByteReadChannel, contentLength: Long): UploadResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var bytesRead: Long = 0
            
            try {
                // Read data efficiently using ByteReadChannel.
                // We don't need to store the data, consume it and count bytes
                val bufferSize = 8192 // 8KB buffer
                val buffer = ByteArray(bufferSize)
                val yieldEvery = 1024 * 1024 // Yield every 1MB
                
                while (!channel.isClosedForRead) {
                    val chunk = channel.readAvailable(buffer, 0, bufferSize)
                    if (chunk <= 0) break
                    
                    bytesRead += chunk
                    
                    // Periodically yield to avoid hogging the thread
                    if (bytesRead % yieldEvery < bufferSize) {
                        yield()
                    }
                }
                
                val endTime = System.currentTimeMillis()
                logger.info("Upload complete: $bytesRead bytes in ${endTime - startTime}ms")
                
                UploadResult(
                    startTimeMillis = startTime,
                    endTimeMillis = endTime,
                    sizeBytes = bytesRead
                )
            } catch (e: Exception) {
                logger.error("Error processing upload", e)
                throw e
            }
        }
    }
    
    override fun isValidUploadSize(contentLength: Long): Boolean {
        return contentLength > 0 && contentLength <= Constants.MAX_UPLOAD_SIZE_BYTES
    }
}
