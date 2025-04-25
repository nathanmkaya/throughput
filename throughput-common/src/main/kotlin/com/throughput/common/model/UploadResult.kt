package com.throughput.common.model

import com.throughput.common.util.ByteUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Result of an upload operation containing timing information
 */
@Serializable
data class UploadResult(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val sizeBytes: Long,
) {
    /**
     * Duration as a Kotlin Duration object (not serialized)
     */
    @Transient 
    val duration: Duration get() = (endTimeMillis - startTimeMillis).milliseconds
    
    /**
     * Duration of the upload in milliseconds
     */
    val durationMillis: Long get() = duration.inWholeMilliseconds
    
    /**
     * Throughput in bytes per second
     */
    val throughputBytesPerSecond: Double get() = 
        if (durationMillis > 0) (sizeBytes.toDouble() / duration.inWholeMilliseconds) * 1000 else 0.0
    
    /**
     * Throughput in bits per second (network standard measure)
     */
    val throughputBitsPerSecond: Double get() = throughputBytesPerSecond * 8
    
    /**
     * Throughput in megabits per second (SI standard - 1,000,000 bits)
     */
    val throughputMbps: Double get() = throughputBitsPerSecond / 1_000_000.0
    
    /**
     * Throughput in mebibits per second (binary standard - 1,048,576 bits)
     */
    val throughputMibps: Double get() = throughputBitsPerSecond / (1024.0 * 1024.0)
    
    /**
     * Get a human-readable summary of the upload result
     */
    fun toSummaryString(): String {
        return "Upload: ${ByteUtils.formatBytes(sizeBytes)} in ${duration}, " +
                "Speed: ${ByteUtils.formatThroughput(throughputBytesPerSecond)} " +
                "(${String.format("%.2f", throughputMbps)} Mbps)"
    }
    
    /**
     * Get a detailed string with all throughput measurements
     */
    fun toDetailedString(): String {
        return "Upload: ${ByteUtils.formatBytes(sizeBytes)} in ${duration}\n" +
                "Throughput: ${ByteUtils.formatThroughput(throughputBytesPerSecond)}\n" +
                "           ${ByteUtils.formatNetworkThroughput(throughputBitsPerSecond)}\n" +
                "           ${String.format("%.2f", throughputMbps)} Mbps (SI standard)\n" +
                "           ${String.format("%.2f", throughputMibps)} Mibps (binary standard)"
    }
}
