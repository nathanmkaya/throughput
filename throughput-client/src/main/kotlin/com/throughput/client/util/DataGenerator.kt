package com.throughput.client.util

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import kotlin.math.min
import kotlin.random.Random

/**
 * Utility for generating random data for throughput testing
 */
object DataGenerator {
    // Using Kotlin's Random for better performance when generating large amounts of test data
    val random = Random(System.currentTimeMillis())
    
    // Keep SecureRandom available for cases where it might be needed
    private val secureRandom by lazy { SecureRandom() }
    
    private const val DEFAULT_CHUNK_SIZE = 8192 // 8KB default chunk size
    
    /**
     * Generate random data of the specified size (use only for small amounts)
     * @param sizeBytes Size of data to generate in bytes
     * @param secure Whether to use cryptographically secure random data
     * @return ByteArray containing random data
     */
    fun generateRandomData(sizeBytes: Int, secure: Boolean = false): ByteArray {
        if (sizeBytes > 10 * 1024 * 1024) {
            throw IllegalArgumentException("This method is not suitable for generating large amounts of data (>10MB). Use a streaming method instead.")
        }
        
        val data = ByteArray(sizeBytes)
        if (secure) {
            secureRandom.nextBytes(data)
        } else {
            random.nextBytes(data)
        }
        return data
    }
    
    /**
     * Write random data directly to an output stream
     * @param sizeBytes Size of data to generate in bytes
     * @param outputStream Stream to write data to
     * @param chunkSize Size of chunks to generate at once
     * @param secure Whether to use cryptographically secure random data
     */
    fun writeRandomData(
        sizeBytes: Long, 
        outputStream: OutputStream,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        secure: Boolean = false
    ) {
        val buffer = ByteArray(chunkSize)
        
        var remainingBytes = sizeBytes
        while (remainingBytes > 0) {
            val bytesToWrite = min(chunkSize.toLong(), remainingBytes).toInt()
            if (secure) {
                secureRandom.nextBytes(buffer, 0, bytesToWrite)
            } else {
                random.nextBytes(buffer, 0, bytesToWrite)
            }
            outputStream.write(buffer, 0, bytesToWrite)
            remainingBytes -= bytesToWrite
        }
        outputStream.flush()
    }
    
    /**
     * Generate a flow of random data chunks
     * @param sizeBytes Total size of data to generate
     * @param chunkSize Size of each chunk
     * @param secure Whether to use cryptographically secure random data
     * @return Flow of ByteBuffer chunks
     */
    fun generateRandomDataFlow(
        sizeBytes: Long,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        secure: Boolean = false
    ): Flow<ByteBuffer> = flow {
        val buffer = ByteArray(chunkSize)
        var remainingBytes = sizeBytes
        
        while (remainingBytes > 0) {
            val bytesToWrite = min(chunkSize.toLong(), remainingBytes).toInt()
            if (secure) {
                secureRandom.nextBytes(buffer, 0, bytesToWrite)
            } else {
                random.nextBytes(buffer, 0, bytesToWrite)
            }
            
            // Emit a ByteBuffer view of the filled portion of the array
            emit(ByteBuffer.wrap(buffer, 0, bytesToWrite))
            
            remainingBytes -= bytesToWrite
            
            // Periodically yield to keep the coroutine responsive
            if (remainingBytes % (1024 * 1024) < chunkSize) {
                yield()
            }
        }
    }
    
    /**
     * Write random data to a ByteWriteChannel
     * @param sizeBytes Size of data to generate in bytes
     * @param channel Channel to write data to
     * @param chunkSize Size of chunks to generate at once
     * @param secure Whether to use cryptographically secure random data
     */
    suspend fun writeRandomDataToChannel(
        sizeBytes: Long,
        channel: ByteWriteChannel,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        secure: Boolean = false
    ) {
        val buffer = ByteArray(chunkSize)
        
        var remainingBytes = sizeBytes
        var bytesWritten = 0L
        val yieldEvery = 1024 * 1024 // Yield every 1MB
        var nextYieldPoint = yieldEvery
        
        while (remainingBytes > 0) {
            val bytesToWrite = min(chunkSize.toLong(), remainingBytes).toInt()
            if (secure) {
                secureRandom.nextBytes(buffer, 0, bytesToWrite)
            } else {
                random.nextBytes(buffer, 0, bytesToWrite)
            }
            channel.writeFully(buffer, 0, bytesToWrite)
            
            remainingBytes -= bytesToWrite
            bytesWritten += bytesToWrite
            
            // Yield periodically to avoid hogging the thread
            if (bytesWritten >= nextYieldPoint) {
                yield()
                nextYieldPoint += yieldEvery
            }
        }
        channel.flush()
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

/**
 * Extension function to fill a byte array with random bytes starting at a specific offset
 * This provides the same API as the Random extension for consistency
 */
fun SecureRandom.nextBytes(bytes: ByteArray, offset: Int, length: Int) {
    require(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
        "Invalid offset or length"
    }
    
    // If filling the whole array, use the built-in method
    if (offset == 0 && length == bytes.size) {
        nextBytes(bytes)
        return
    }
    
    // Otherwise, use a temporary array
    val temp = ByteArray(length)
    nextBytes(temp)
    System.arraycopy(temp, 0, bytes, offset, length)
}
