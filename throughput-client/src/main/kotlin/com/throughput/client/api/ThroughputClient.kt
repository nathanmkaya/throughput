package com.throughput.client.api

import com.throughput.common.model.DownloadResult
import com.throughput.common.model.UploadResult
import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer

/**
 * Interface for the throughput testing client
 */
interface ThroughputClient : Closeable {
    /**
     * Upload data of specified size to the server and measure throughput.
     * The data is generated on-the-fly and streamed to the server.
     * 
     * @param sizeBytes Size of data to upload in bytes
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Upload result with timing and throughput information
     */
    suspend fun uploadData(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): UploadResult
    
    /**
     * Synchronous version of uploadData.
     * NOTE: This method blocks the calling thread until completion.
     * For most applications, the suspend version is preferable.
     * 
     * @param sizeBytes Size of data to upload in bytes
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Upload result with timing and throughput information
     */
    fun uploadDataSync(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): UploadResult
    
    /**
     * Download data of specified size from the server and measure throughput.
     * The data is streamed and discarded after measuring, not stored in memory.
     * 
     * @param sizeBytes Size of data to download in bytes
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Download result with timing and throughput information
     */
    suspend fun downloadData(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadResult
    
    /**
     * Synchronous version of downloadData.
     * NOTE: This method blocks the calling thread until completion.
     * For most applications, the suspend version is preferable.
     * 
     * @param sizeBytes Size of data to download in bytes
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Download result with timing and throughput information
     */
    fun downloadDataSync(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadResult
    
    /**
     * Upload a specific file to the server and measure throughput.
     * The file is streamed efficiently without loading it entirely into memory.
     * 
     * @param file File to upload
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Upload result with timing and throughput information
     */
    suspend fun uploadFile(
        file: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): UploadResult
    
    /**
     * Synchronous version of uploadFile.
     * NOTE: This method blocks the calling thread until completion.
     * For most applications, the suspend version is preferable.
     * 
     * @param file File to upload
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Upload result with timing and throughput information
     */
    fun uploadFileSync(
        file: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): UploadResult
    
    /**
     * Download data of specified size from the server and save to a file.
     * The data is streamed directly to the file without loading it into memory.
     * 
     * @param sizeBytes Size of data to download in bytes
     * @param destinationFile File to save the downloaded data to
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Download result with timing and throughput information
     */
    suspend fun downloadToFile(
        sizeBytes: Long,
        destinationFile: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadResult
    
    /**
     * Synchronous version of downloadToFile.
     * NOTE: This method blocks the calling thread until completion.
     * For most applications, the suspend version is preferable.
     * 
     * @param sizeBytes Size of data to download in bytes
     * @param destinationFile File to save the downloaded data to
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Download result with timing and throughput information
     */
    fun downloadToFileSync(
        sizeBytes: Long,
        destinationFile: File,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadResult
    
    /**
     * Get data chunks as a flow of events (either chunks or the final result)
     * This allows for advanced custom handling of the download stream.
     * 
     * @param sizeBytes Size of data to download in bytes
     * @param onProgress Optional callback for progress updates (bytesTransferred, totalBytes)
     * @return Flow of DownloadFlowEvent (either Chunk or Result)
     */
    suspend fun downloadAsFlow(
        sizeBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    ): Flow<DownloadFlowEvent>
    
    /**
     * Closes resources associated with this client instance.
     * The client cannot be used after closing.
     */
    override fun close()
}

/**
 * Sealed class representing download flow events
 */
sealed class DownloadFlowEvent {
    /**
     * A chunk of downloaded data
     */
    data class Chunk(val buffer: ByteBuffer) : DownloadFlowEvent()
    
    /**
     * The final download result
     */
    data class Result(val result: DownloadResult) : DownloadFlowEvent()
}
