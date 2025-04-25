package com.throughput.server.service.impl

import com.throughput.common.util.Constants
import com.throughput.server.service.FileGeneratorService
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory

/**
 * Implementation of FileGeneratorService that creates random binary data with non-blocking IO
 */
class FileGeneratorServiceImpl : FileGeneratorService {
    private val logger = LoggerFactory.getLogger(FileGeneratorServiceImpl::class.java)
    private val random = SecureRandom()
    
    override suspend fun generateRandomData(sizeBytes: Long, channel: ByteWriteChannel) {
        withContext(Dispatchers.IO) {
            try {
                val bufferSize = 8192 // 8KB buffer
                val buffer = ByteArray(bufferSize)
                
                var remainingBytes = sizeBytes
                var bytesGenerated = 0L
                val yieldEvery = 1024 * 1024 // Yield every 1MB
                
                while (remainingBytes > 0) {
                    val bytesToWrite = minOf(bufferSize.toLong(), remainingBytes).toInt()
                    random.nextBytes(buffer)
                    channel.writeFully(buffer, 0, bytesToWrite)
                    
                    remainingBytes -= bytesToWrite
                    bytesGenerated += bytesToWrite
                    
                    // Periodically yield to avoid hogging the thread
                    if (bytesGenerated % yieldEvery < bufferSize) {
                        yield()
                    }
                }
                
                // Ensure all data is written to the channel
                channel.flush()
                logger.info("Generated ${sizeBytes} bytes of random data")
            } catch (e: Exception) {
                logger.error("Error generating random data", e)
                throw e
            } finally {
                // Important: always close the channel when done
                channel.close()
            }
        }
    }
    
    override fun isValidSize(sizeBytes: Long): Boolean {
        return sizeBytes > 0 && sizeBytes <= Constants.MAX_DOWNLOAD_SIZE_BYTES
    }
}
