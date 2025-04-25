package com.throughput.server.service.impl

import com.throughput.common.util.Constants
import com.throughput.server.service.FileGeneratorService
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.random.Random

/**
 * Implementation of FileGeneratorService that creates random binary data with non-blocking IO
 */
class FileGeneratorServiceImpl : FileGeneratorService {
    private val logger = LoggerFactory.getLogger(FileGeneratorServiceImpl::class.java)
    
    // Using Kotlin's Random implementation instead of SecureRandom for better performance
    // When generating large amounts of test data, cryptographic security is unnecessary
    private val random = Random(System.currentTimeMillis())
    
    override suspend fun generateRandomData(sizeBytes: Long, channel: ByteWriteChannel) {
        withContext(Dispatchers.IO) {
            try {
                val bufferSize = 8192 // 8KB buffer
                val buffer = ByteArray(bufferSize)
                
                var bytesGenerated = 0L
                val yieldEvery = 1024 * 1024 // 1MB
                var nextYieldPoint = yieldEvery
                
                while (bytesGenerated < sizeBytes) {
                    val bytesToWrite = minOf(bufferSize.toLong(), sizeBytes - bytesGenerated).toInt()
                    
                    // Fill exactly bytesToWrite bytes (not the whole buffer if it's the last chunk)
                    random.nextBytes(buffer, 0, bytesToWrite)
                    channel.writeFully(buffer, 0, bytesToWrite)
                    
                    bytesGenerated += bytesToWrite
                    
                    // Yield periodically to avoid hogging the thread - simplified logic
                    if (bytesGenerated >= nextYieldPoint) {
                        yield()
                        nextYieldPoint += yieldEvery
                    }
                }
                
                // Ensure all data is written to the channel
                channel.flush()
                logger.info("Generated ${sizeBytes} bytes of random data")
            } catch (e: IOException) {
                logger.error("I/O error generating random data", e)
                throw e
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

/**
 * Extension function to fill a byte array with random bytes starting at a specific offset
 * This mimics the behavior of SecureRandom.nextBytes but for Kotlin's Random
 */
fun Random.nextBytes(bytes: ByteArray, offset: Int, length: Int) {
    require(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
        "Invalid offset or length"
    }
    
    for (i in offset until offset + length) {
        bytes[i] = nextInt(256).toByte()
    }
}
