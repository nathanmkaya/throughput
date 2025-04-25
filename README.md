# Ktor Network Throughput Tester

A client-server application built with Ktor to measure network throughput (upload and download speeds).

## Description

This project provides:

1.  **A Ktor Server (`throughput-server`)**: Exposes HTTP endpoints to facilitate network speed tests.
    * Generates random binary data of a specified size for clients to download.
    * Accepts uploads of binary data from clients and measures the time taken.
2.  **A Ktor Client Library (`throughput-client`)**: A Kotlin library that can be used in other applications to programmatically test network throughput against the server.
    * Provides functions to initiate downloads and uploads of specified sizes.
    * Supports streaming for efficient handling of large data transfers.
    * Offers methods for downloading directly to memory, to a file, or as a reactive Flow.
    * Includes progress reporting capabilities.
3.  **Shared Code (`throughput-common`)**: Contains common data models (like results and exceptions) used by both client and server.

## Features

**Server:**

* **Download Endpoint**: Generates and streams binary data of a specified size (`GET /v1/download/{size}`)[cite: 9, 216, 416]. A legacy POST endpoint is also available but deprecated (`POST /v1/download`).
* **Upload Endpoint**: Receives streamed binary data (`POST /v1/upload`) and returns timing information (`UploadResult`).
* **Size Validation**: Enforces maximum download and upload sizes defined in `Constants.kt`.
* **Non-Blocking I/O**: Uses Ktor's `ByteReadChannel` and `ByteWriteChannel` for efficient, non-blocking stream processing.
* **Dependency Injection**: Uses Koin for managing dependencies.
* **Configuration**: Configured via `application.conf`.
* **Error Handling**: Centralized error handling using Ktor's `StatusPages` plugin.

**Client Library:**

* **Fluent Builder API**: Easy configuration of server address, timeouts, and HTTP engine using `ThroughputClientFactory.builder()`.
* **Asynchronous API**: Provides `suspend` functions for non-blocking operations.
* **Synchronous API (Use with Caution)**: Offers `*Sync` methods that wrap suspend functions using `runBlocking`. Be aware that `runBlocking` blocks the calling thread.
* **Streaming Operations**: Uploads and downloads data efficiently using streams, avoiding loading large files into memory.
* **Multiple Download Modes**:
    * `downloadData`: Discards data after download (for pure speed test).
    * `downloadToFile`: Streams data directly to a specified file.
    * `downloadAsFlow`: Provides a `Flow<DownloadFlowEvent>` for reactive chunk processing.
* **Multiple Upload Modes**:
    * `uploadData`: Generates and streams random data.
    * `uploadFile`: Streams data directly from a specified file.
* **Progress Reporting**: Optional lambda callbacks (`onProgress`) provide updates on bytes transferred.
* **Resource Management**: Implements `Closeable` to release the underlying `HttpClient`.

## Project Structure

The project is a multi-module Gradle project:

```
network-throughput/
├── throughput-server/    # Ktor server application
├── throughput-client/    # Ktor client library
└── throughput-common/    # Shared data models and utilities
```

## Technology Stack

* **Language**: Kotlin
* **Framework**: Ktor (Server & Client)
* **Build Tool**: Gradle (Kotlin DSL)
* **Asynchronous**: Kotlin Coroutines & Flow
* **Dependency Injection**: Koin (Server-side)
* **Serialization**: Kotlinx Serialization (JSON)
* **Logging**: SLF4j (with Logback expected)

## Setup & Running

### Prerequisites

* Java Development Kit (JDK) 11 or higher
* Gradle (usually included via wrapper)

### Running the Server

1.  Navigate to the project root directory.
2.  Build and run the server using Gradle:
    ```bash
    ./gradlew :throughput-server:run
    ```
3.  The server will start, typically on `http://0.0.0.0:8080` (configurable in `throughput-server/src/main/resources/application.conf`). Check the logs for the exact address and port[cite: 6, 213].

### Using the Client Library

1.  **Add Dependency**: Include the `throughput-client` module as a dependency in your Kotlin project's `build.gradle.kts`:
    ```kotlin
    dependencies {
        implementation(project(":throughput-client"))
        // or implementation("com.throughput:throughput-client:1.0.0") // If published
    }
    ```
2.  **Use the Client**: Instantiate and use the client via the `ThroughputClientFactory`. Remember to close the client when done, preferably using `use`.

    ```kotlin
    import com.throughput.client.ThroughputClientFactory
    import kotlinx.coroutines.runBlocking
    import java.io.File

    fun main() = runBlocking {
        // Create a client (defaults to http://localhost:8080)
        // Use builder for custom configuration
        ThroughputClientFactory.createDefault().use { client ->
            println("Starting download test...")
            val downloadResult = try {
                 client.downloadData(100 * 1024 * 1024) { bytes, total -> // 100 MB
                    val progress = (bytes * 100.0 / total).toInt()
                    print("\rDownload Progress: $progress% ($bytes / $total)")
                 }
            } catch (e: Exception) {
                println("\nDownload failed: ${e.message}")
                null
            }
            println() // Newline after progress
            downloadResult?.let { println(it.toDetailedString()) } // [cite: 200, 401]

            println("\nStarting upload test...")
            val uploadResult = try {
                client.uploadData(50 * 1024 * 1024) { bytes, total -> // 50 MB
                    val progress = (bytes * 100.0 / total).toInt()
                    print("\rUpload Progress: $progress% ($bytes / $total)")
                }
            } catch (e: Exception) {
                println("\nUpload failed: ${e.message}")
                null
            }
            println() // Newline after progress
            uploadResult?.let { println(it.toDetailedString()) } // [cite: 206, 407]

            // Example: Download to file
            // val file = File("test.bin")
            // val fileResult = client.downloadToFile(10 * 1024 * 1024, file) { ... }
            // println(fileResult.toSummaryString())

        } // Client is automatically closed here [cite: 183]
    }
    ```

## Server API Endpoints

Base path: `/v1`

* `GET /download/{size}`: Initiates a download of `{size}` bytes. Returns binary data stream.
* `POST /upload`: Accepts a binary data stream upload. Requires `Content-Length` header. Returns `UploadResult` JSON upon completion.
* `POST /download` (Deprecated): Initiates download. Expects JSON body `{"sizeBytes": Long}`. Returns binary data stream.

