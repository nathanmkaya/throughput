package com.throughput.client.util

import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import kotlin.math.min

/**
 * Utility for generating random data for throughput testing
 */
object DataGenerator {
    val random = SecureRandom()
    private const val DEFAULT_CHUNK_SIZE = 8192 // 8KB default chunk size
    
    /**
     * Generate random data of the specified size (use only for small amounts)
     * @param sizeBytes Size of data to generate in bytes
     * @return ByteArray containing random data
     */
    fun generateRandomData(sizeBytes: Int): ByteArray {
        if (sizeBytes > 10 * 1024 * 1024) {
            throw IllegalArgumentException("This method is not suitable for generating large amounts of data (>10MB). Use a streaming method instead.")
        }
        
        val data = ByteArray(sizeBytes)
        random.nextBytes(data)
        return data
    }
    
    /**
     * Write random data directly to an output stream
     * @param sizeBytes Size of data to generate in bytes
     * @param outputStream Stream to write data to
     * @param chunkSize Size of chunks to generate at once
     */
    fun writeRandomData(
        sizeBytes: Long, 
        outputStream: OutputStream,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ) {
        val buffer = ByteArray(chunkSize)
        
        var remainingBytes = sizeBytes
        while (remainingBytes > 0) {
            val bytesToWrite = min(chunkSize.toLong(), remainingBytes).toInt()
            random.nextBytes(buffer)
            outputStream.write(buffer, 0, bytesToWrite)
            remainingBytes -= bytesToWrite
        }
        outputStream.flush()
    }
    
    /**
     * Generate a flow of random data chunks
     * @param sizeBytes Total size of data to generate
     * @param chunkSize Size of each chunk
     * @return Flow of ByteBuffer chunks
     */
    fun generateRandomDataFlow(
        sizeBytes: Long,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Flow<ByteBuffer> = flow {
        val buffer = ByteArray(chunkSize)
        var remainingBytes = sizeBytes
        
        while (remainingBytes > 0) {
            val bytesToWrite = min(chunkSize.toLong(), remainingBytes).toInt()
            random.nextBytes(buffer)
            
            // Emit a ByteBuffer view of the filled portion of the array
            emit(ByteBuffer.wrap(buffer, 0, bytesToWrite))
            
            remainingBytes -= bytesToWrite
        }
    }
    
    /**
     * Write random data to a ByteWriteChannel
     * @param sizeBytes Size of data to generate in bytes
     * @param channel Channel to write data to
     * @param chunkSize Size of chunks to generate at once
     */
    suspend fun writeRandomDataToChannel(
        sizeBytes: Long,
        channel: ByteWriteChannel,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ) {
        val buffer = ByteArray(chunkSize)
        
        var remainingBytes = sizeBytes
        while (remainingBytes > 0) {
            val bytesToWrite = min(chunkSize.toLong(), remainingBytes).toInt()
            random.nextBytes(buffer)
            channel.writeFully(buffer, 0, bytesToWrite)
            remainingBytes -= bytesToWrite
        }
        channel.flush()
    }
}
