package com.throughput.client.impl

import com.throughput.client.api.DownloadFlowEvent
import com.throughput.client.api.ThroughputClient
import com.throughput.client.model.ClientConfig
import com.throughput.client.util.DataGenerator
import com.throughput.common.model.DownloadResult
import com.throughput.common.model.ThroughputException
import com.throughput.common.model.UploadResult
import com.throughput.common.util.Constants
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.StandardOpenOption
import kotlin.coroutines.coroutineContext

/**
 * Implementation of ThroughputClient that uses Ktor client for HTTP communication
 * with streaming capabilities for efficient data transfer
 */
class ThroughputClientImpl(private val config: ClientConfig) : ThroughputClient {
    private val logger = LoggerFactory.getLogger(ThroughputClientImpl::class.java)
    
    private val httpClient = HttpClient(config.engine ?: CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        install(HttpTimeout) {
            connectTimeoutMillis = config.connectionTimeoutMs
            socketTimeoutMillis = config.socketTimeoutMs
            requestTimeoutMillis = config.connectionTimeoutMs + config.socketTimeoutMs
        }
        
        expectSuccess = true
    }
    
    override suspend fun uploadData(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        validateUploadSize(sizeBytes)
        logger.info("Preparing to upload ${sizeBytes} bytes of random data")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Use streaming upload with OutgoingContent to avoid loading everything into memory
            val response = httpClient.post(config.uploadUrl) {
                contentType(ContentType.Application.OctetStream)
                
                setBody(object : OutgoingContent.WriteChannelContent() {
                    override val contentLength: Long = sizeBytes
                    
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        var bytesWritten = 0L
                        val bufferSize = 8192
                        val progressUpdateInterval = 64 * 1024 // Update progress every 64KB
                        
                        try {
                            val buffer = ByteArray(bufferSize)
                            
                            while (bytesWritten < sizeBytes) {
                                val bytesToWrite = minOf(bufferSize.toLong(), sizeBytes - bytesWritten).toInt()
                                DataGenerator.random.nextBytes(buffer)
                                channel.writeFully(buffer, 0, bytesToWrite)
                                
                                bytesWritten += bytesToWrite
                                
                                // Report progress periodically to avoid excessive callbacks
                                if (bytesWritten % progressUpdateInterval < bufferSize || bytesWritten == sizeBytes) {
                                    onProgress?.invoke(bytesWritten, sizeBytes)
                                }
                                
                                // Yield periodically to avoid hogging the thread
                                if (bytesWritten % (1024 * 1024) < bufferSize) {
                                    kotlinx.coroutines.yield()
                                }
                            }
                            channel.flush()
                        } catch (e: Exception) {
                            throw IOException("Error generating or writing upload data: ${e.message}", e)
                        }
                    }
                })
            }
            
            val endTime = System.currentTimeMillis()
            
            if (response.status.isSuccess()) {
                return response.body<UploadResult>()
            } else {
                throw ThroughputException("Upload failed with status: ${response.status}")
            }
        } catch (e: ClientRequestException) {
            logger.error("Client request error during data upload: ${e.response.status}", e)
            throw ThroughputException("Client error during data upload: ${e.response.status.description}", e)
        } catch (e: ServerResponseException) {
            logger.error("Server error during data upload: ${e.response.status}", e)
            throw ThroughputException("Server error during data upload: ${e.response.status.description}", e)
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout during data upload", e)
            throw ThroughputException("Timeout during data upload", e)
        } catch (e: IOException) {
            logger.error("I/O error during data upload", e)
            throw ThroughputException("I/O error during data upload: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error during data upload", e)
            throw ThroughputException("Error during data upload: ${e.message}", e)
        }
    }
    
    override fun uploadDataSync(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): UploadResult = runBlocking {
        uploadData(sizeBytes, onProgress)
    }
    
    override suspend fun downloadData(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): DownloadResult {
        validateDownloadSize(sizeBytes)
        logger.info("Preparing to download ${sizeBytes} bytes of data")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Use GET with path parameter for the RESTful approach
            val response = httpClient.get("${config.downloadUrl}/$sizeBytes") {
                contentType(ContentType.Application.Json)
            }
            
            // Stream the data without storing it in memory
            val bytesRead = consumeResponseBody(response, sizeBytes, onProgress)
            
            val endTime = System.currentTimeMillis()
            
            if (bytesRead != sizeBytes) {
                logger.warn("Downloaded size (${bytesRead}) does not match requested size (${sizeBytes})")
            }
            
            return DownloadResult(
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                sizeBytes = bytesRead
            )
        } catch (e: ClientRequestException) {
            logger.error("Client request error during data download: ${e.response.status}", e)
            throw ThroughputException("Client error during data download: ${e.response.status.description}", e)
        } catch (e: ServerResponseException) {
            logger.error("Server error during data download: ${e.response.status}", e)
            throw ThroughputException("Server error during data download: ${e.response.status.description}", e)
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout during data download", e)
            throw ThroughputException("Timeout during data download", e)
        } catch (e: IOException) {
            logger.error("I/O error during data download", e)
            throw ThroughputException("I/O error during data download: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error during data download", e)
            throw ThroughputException("Error during data download: ${e.message}", e)
        }
    }
    
    override fun downloadDataSync(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): DownloadResult = runBlocking {
        downloadData(sizeBytes, onProgress)
    }
    
    override suspend fun uploadFile(
        file: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): UploadResult {
        if (!file.exists() || !file.isFile) {
            throw ThroughputException("File does not exist or is not a regular file: ${file.absolutePath}")
        }
        
        val fileSize = file.length()
        validateUploadSize(fileSize)
        logger.info("Preparing to upload file ${file.name} (${fileSize} bytes)")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Use streaming upload with OutgoingContent to avoid loading everything into memory
            val response = httpClient.post(config.uploadUrl) {
                contentType(ContentType.Application.OctetStream)
                
                setBody(object : OutgoingContent.WriteChannelContent() {
                    override val contentLength: Long = fileSize
                    
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        // Stream file contents directly to the channel
                        val fileChannel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)
                        var position = 0L
                        val buffer = ByteBuffer.allocate(8192)
                        val progressUpdateInterval = 64 * 1024 // Update progress every 64KB
                        
                        try {
                            while (position < fileSize) {
                                buffer.clear()
                                val bytesRead = fileChannel.read(buffer, position).await()
                                if (bytesRead <= 0) break
                                
                                buffer.flip()
                                channel.writeFully(buffer)
                                position += bytesRead
                                
                                // Report progress periodically to avoid excessive callbacks
                                if (position % progressUpdateInterval < buffer.capacity() || position == fileSize) {
                                    onProgress?.invoke(position, fileSize)
                                }
                                
                                // Yield periodically to avoid hogging the thread
                                if (position % (1024 * 1024) < buffer.capacity()) {
                                    kotlinx.coroutines.yield()
                                }
                            }
                            channel.flush()
                        } catch (e: Exception) {
                            throw IOException("Error reading from file or writing to upload channel: ${e.message}", e)
                        } finally {
                            fileChannel.close()
                        }
                    }
                })
            }
            
            val endTime = System.currentTimeMillis()
            
            if (response.status.isSuccess()) {
                return response.body<UploadResult>()
            } else {
                throw ThroughputException("Upload failed with status: ${response.status}")
            }
        } catch (e: ClientRequestException) {
            logger.error("Client request error during file upload: ${e.response.status}", e)
            throw ThroughputException("Client error during file upload: ${e.response.status.description}", e)
        } catch (e: ServerResponseException) {
            logger.error("Server error during file upload: ${e.response.status}", e)
            throw ThroughputException("Server error during file upload: ${e.response.status.description}", e)
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout during file upload", e)
            throw ThroughputException("Timeout during file upload", e)
        } catch (e: IOException) {
            logger.error("I/O error during file upload", e)
            throw ThroughputException("I/O error during file upload: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error during file upload", e)
            throw ThroughputException("Error during file upload: ${e.message}", e)
        }
    }
    
    override fun uploadFileSync(
        file: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): UploadResult = runBlocking {
        uploadFile(file, onProgress)
    }
    
    override suspend fun downloadToFile(
        sizeBytes: Long, 
        destinationFile: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): DownloadResult {
        validateDownloadSize(sizeBytes)
        logger.info("Preparing to download ${sizeBytes} bytes to file ${destinationFile.absolutePath}")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Use GET with path parameter for the RESTful approach
            val response = httpClient.get("${config.downloadUrl}/$sizeBytes") {
                contentType(ContentType.Application.Json)
            }
            
            // Stream directly to file
            val fileChannel = AsynchronousFileChannel.open(
                destinationFile.toPath(),
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.TRUNCATE_EXISTING
            )
            
            val bytesRead = try {
                var position = 0L
                val channel = response.bodyAsChannel()
                val buffer = ByteBuffer.allocate(8192)
                val progressUpdateInterval = 64 * 1024 // Update progress every 64KB
                
                while (!channel.isClosedForRead) {
                    buffer.clear()
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead <= 0) break
                    
                    buffer.flip()
                    while (buffer.hasRemaining()) {
                        val written = fileChannel.write(buffer, position).await()
                        position += written
                    }
                    
                    // Report progress periodically to avoid excessive callbacks
                    if (position % progressUpdateInterval < buffer.capacity() || position == sizeBytes) {
                        onProgress?.invoke(position, sizeBytes)
                    }
                    
                    // Yield periodically to avoid hogging the thread
                    if (position % (1024 * 1024) < buffer.capacity()) {
                        kotlinx.coroutines.yield()
                    }
                }
                position
            } catch (e: Exception) {
                throw IOException("Error writing downloaded data to file: ${e.message}", e)
            } finally {
                fileChannel.close()
            }
            
            val endTime = System.currentTimeMillis()
            
            if (bytesRead != sizeBytes) {
                logger.warn("Downloaded size (${bytesRead}) does not match requested size (${sizeBytes})")
            }
            
            return DownloadResult(
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                sizeBytes = bytesRead
            )
        } catch (e: ClientRequestException) {
            logger.error("Client request error during download to file: ${e.response.status}", e)
            throw ThroughputException("Client error during download to file: ${e.response.status.description}", e)
        } catch (e: ServerResponseException) {
            logger.error("Server error during download to file: ${e.response.status}", e)
            throw ThroughputException("Server error during download to file: ${e.response.status.description}", e)
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout during download to file", e)
            throw ThroughputException("Timeout during download to file", e)
        } catch (e: IOException) {
            logger.error("I/O error during download to file", e)
            throw ThroughputException("I/O error during download to file: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error during download to file", e)
            throw ThroughputException("Error during download to file: ${e.message}", e)
        }
    }
    
    override fun downloadToFileSync(
        sizeBytes: Long, 
        destinationFile: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): DownloadResult = runBlocking {
        downloadToFile(sizeBytes, destinationFile, onProgress)
    }
    
    override suspend fun downloadAsFlow(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): Flow<DownloadFlowEvent> = flow {
        validateDownloadSize(sizeBytes)
        logger.info("Preparing to download ${sizeBytes} bytes as a flow")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Use GET with path parameter for the RESTful approach
            val response = httpClient.get("${config.downloadUrl}/$sizeBytes") {
                contentType(ContentType.Application.Json)
            }
            
            var totalBytesRead = 0L
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            val progressUpdateInterval = 64 * 1024 // Update progress every 64KB
            
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead <= 0) break
                
                totalBytesRead += bytesRead
                
                // Emit chunk event
                emit(DownloadFlowEvent.Chunk(ByteBuffer.wrap(buffer.copyOf(bytesRead))))
                
                // Report progress periodically to avoid excessive callbacks
                if (totalBytesRead % progressUpdateInterval < buffer.size || totalBytesRead == sizeBytes) {
                    onProgress?.invoke(totalBytesRead, sizeBytes)
                }
                
                // Yield periodically to avoid hogging the thread
                if (totalBytesRead % (1024 * 1024) < buffer.size) {
                    kotlinx.coroutines.yield()
                }
            }
            
            val endTime = System.currentTimeMillis()
            val result = DownloadResult(
                startTimeMillis = startTime,
                endTimeMillis = endTime,
                sizeBytes = totalBytesRead
            )
            
            if (totalBytesRead != sizeBytes) {
                logger.warn("Downloaded size (${totalBytesRead}) does not match requested size (${sizeBytes})")
            }
            
            // Emit final result event
            emit(DownloadFlowEvent.Result(result))
            
        } catch (e: ClientRequestException) {
            logger.error("Client request error during download flow: ${e.response.status}", e)
            throw ThroughputException("Client error during download flow: ${e.response.status.description}", e)
        } catch (e: ServerResponseException) {
            logger.error("Server error during download flow: ${e.response.status}", e)
            throw ThroughputException("Server error during download flow: ${e.response.status.description}", e)
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout during download flow", e)
            throw ThroughputException("Timeout during download flow", e)
        } catch (e: IOException) {
            logger.error("I/O error during download flow", e)
            throw ThroughputException("I/O error during download flow: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error during download flow", e)
            throw ThroughputException("Error during download flow: ${e.message}", e)
        }
    }
    
    private suspend fun consumeResponseBody(
        response: HttpResponse, 
        totalSize: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?
    ): Long {
        var bytesRead = 0L
        val buffer = ByteArray(8192)
        val channel = response.bodyAsChannel()
        val progressUpdateInterval = 64 * 1024 // Update progress every 64KB
        
        try {
            while (!channel.isClosedForRead) {
                val chunk = channel.readAvailable(buffer)
                if (chunk <= 0) break
                
                bytesRead += chunk
                
                // Report progress periodically to avoid excessive callbacks
                if (bytesRead % progressUpdateInterval < buffer.size || bytesRead == totalSize) {
                    onProgress?.invoke(bytesRead, totalSize)
                }
                
                // Yield periodically to avoid hogging the thread
                if (bytesRead % (1024 * 1024) < buffer.size) {
                    kotlinx.coroutines.yield()
                }
            }
        } catch (e: Exception) {
            throw IOException("Error reading response body: ${e.message}", e)
        }
        
        return bytesRead
    }
    
    private fun validateUploadSize(sizeBytes: Long) {
        if (sizeBytes <= 0) {
            throw ThroughputException("Upload size must be greater than 0")
        }
        if (sizeBytes > Constants.MAX_UPLOAD_SIZE_BYTES) {
            throw ThroughputException("Upload size exceeds maximum allowed: ${Constants.MAX_UPLOAD_SIZE_BYTES} bytes")
        }
    }
    
    private fun validateDownloadSize(sizeBytes: Long) {
        if (sizeBytes <= 0) {
            throw ThroughputException("Download size must be greater than 0")
        }
        if (sizeBytes > Constants.MAX_DOWNLOAD_SIZE_BYTES) {
            throw ThroughputException("Download size exceeds maximum allowed: ${Constants.MAX_DOWNLOAD_SIZE_BYTES} bytes")
        }
    }
    
    override fun close() {
        try {
            httpClient.close()
            logger.info("Throughput client resources released.")
        } catch (e: Exception) {
            logger.warn("Error closing the HTTP client: ${e.message}")
        }
    }
}

/**
 * Extension function to await the completion of an AsynchronousFileChannel operation
 */
private suspend fun java.util.concurrent.Future<Int>.await(): Int {
    while (!isDone) {
        kotlinx.coroutines.yield()
    }
    return get()
}
