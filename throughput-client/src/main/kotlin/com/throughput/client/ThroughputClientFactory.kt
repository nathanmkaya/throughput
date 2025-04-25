package com.throughput.client

import com.throughput.client.api.DownloadFlowEvent
import com.throughput.client.api.ThroughputClient
import com.throughput.client.api.ThroughputClientBuilder
import com.throughput.client.impl.ThroughputClientBuilderImpl
import io.ktor.client.engine.cio.CIO
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Factory class to create ThroughputClient instances
 */
object ThroughputClientFactory {
    /**
     * Create a new client builder for configuring a ThroughputClient instance
     * @return A new ThroughputClientBuilder instance
     */
    fun builder(): ThroughputClientBuilder {
        return ThroughputClientBuilderImpl()
    }

    /**
     * Create a default ThroughputClient instance with default settings
     * @return A new ThroughputClient instance
     */
    fun createDefault(): ThroughputClient {
        return builder().build()
    }

    /**
     * Create a new ThroughputClient instance with specific host and port
     * @param host Server hostname or IP address
     * @param port Server port
     * @param secure Whether to use HTTPS
     * @return A new ThroughputClient instance
     */
    fun create(host: String, port: Int, secure: Boolean = false): ThroughputClient {
        return builder()
            .host(host)
            .port(port)
            .secure(secure)
            .build()
    }

    /**
     * Create a new ThroughputClient instance optimized for high performance
     * @param host Server hostname or IP address
     * @param port Server port
     * @return A new ThroughputClient instance with performance-optimized settings
     */
    fun createHighPerformance(host: String = "localhost", port: Int = 8080): ThroughputClient {
        return builder()
            .host(host)
            .port(port)
            .engine(CIO) // CIO engine is good for high throughput
            .connectionTimeout(5000)
            .socketTimeout(60000)
            .requestTimeout(120000)
            .build()
    }

    /**
     * Create a new ThroughputClient instance using OkHttp engine
     * @param host Server hostname or IP address
     * @param port Server port
     * @return A new ThroughputClient instance using OkHttp
     */
    fun createWithOkHttp(host: String = "localhost", port: Int = 8080): ThroughputClient {
        return builder()
            .host(host)
            .port(port)
            .engine(CIO)
            .build()
    }
}

// Example client usage code:

/**
 * The following is an example of how to use the ThroughputClient:
 *
 * ```kotlin
 * import com.throughput.client.ThroughputClientFactory
 * import kotlinx.coroutines.runBlocking
 * import java.io.File
 *
 * fun main() = runBlocking {
 *     // Create a client with progress reporting
 *     ThroughputClientFactory.createDefault().use { client ->
 *         // Test download with progress reporting
 *         val downloadResult = client.downloadData(10 * 1024 * 1024) { bytesTransferred, totalBytes ->
 *             val progressPercent = (bytesTransferred * 100.0 / totalBytes).toInt()
 *             println("Download progress: $progressPercent% (${bytesTransferred / 1024} KB / ${totalBytes / 1024} KB)")
 *         }
 *         println(downloadResult.toDetailedString())
 *
 *         // Test upload with progress reporting
 *         val uploadResult = client.uploadData(5 * 1024 * 1024) { bytesTransferred, totalBytes ->
 *             val progressPercent = (bytesTransferred * 100.0 / totalBytes).toInt()
 *             println("Upload progress: $progressPercent% (${bytesTransferred / 1024} KB / ${totalBytes / 1024} KB)")
 *         }
 *         println(uploadResult.toDetailedString())
 *
 *         // Download to file
 *         val file = File("download-test.bin")
 *         val downloadToFileResult = client.downloadToFile(2 * 1024 * 1024, file) { bytesTransferred, totalBytes ->
 *             val progressPercent = (bytesTransferred * 100.0 / totalBytes).toInt()
 *             println("File download progress: $progressPercent%")
 *         }
 *
 *         // Get download as a flow
 *         val flow = client.downloadAsFlow(1024 * 1024)
 *         flow.collect { event ->
 *             when (event) {
 *                 is DownloadFlowEvent.Chunk -> {
 *                     // Process each chunk as it arrives
 *                     println("Received chunk: ${event.buffer.remaining()} bytes")
 *                 }
 *                 is DownloadFlowEvent.Result -> {
 *                     // Final result
 *                     println("Download complete: ${event.result.toSummaryString()}")
 *                 }
 *             }
 *         }
 *     } // Client is automatically closed here
 * }
 * ```
 */

fun main() = runBlocking {
    /// Create a client with progress reporting
        ThroughputClientFactory.createHighPerformance().use { client ->
            // Test download with progress reporting
            val downloadResult = client.downloadData(10 * 1024 * 1024) { bytesTransferred, totalBytes ->
                val progressPercent = (bytesTransferred * 100.0 / totalBytes).toInt()
                println("Download progress: $progressPercent% (${bytesTransferred / 1024} KB / ${totalBytes / 1024} KB)")
            }
            println(downloadResult.toDetailedString())

            // Test upload with progress reporting
            val uploadResult = client.uploadData(5 * 1024 * 1024) { bytesTransferred, totalBytes ->
                val progressPercent = (bytesTransferred * 100.0 / totalBytes).toInt()
                println("Upload progress: $progressPercent% (${bytesTransferred / 1024} KB / ${totalBytes / 1024} KB)")
            }
            println(uploadResult.toDetailedString())

            // Download to file
            val file = File("download-test.bin")
            val downloadToFileResult = client.downloadToFile(2 * 1024 * 1024, file) { bytesTransferred, totalBytes ->
                val progressPercent = (bytesTransferred * 100.0 / totalBytes).toInt()
                println("File download progress: $progressPercent%")
            }

            // Get download as a flow
            val flow = client.downloadAsFlow(1024 * 1024)
            flow.collect { event ->
                when (event) {
                    is DownloadFlowEvent.Chunk -> {
                        // Process each chunk as it arrives
                        println("Received chunk: ${event.buffer.remaining()} bytes")
                    }

                    is DownloadFlowEvent.Result -> {
                        // Final result
                        println("Download complete: ${event.result.toSummaryString()}")
                    }
                }
            }
        } // Client is automatically closed here
}