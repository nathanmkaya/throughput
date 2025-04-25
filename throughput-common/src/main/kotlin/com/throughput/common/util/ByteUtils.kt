package com.throughput.common.util

import java.text.DecimalFormat
import kotlin.time.Duration

/**
 * Utility functions for byte calculations and formatting
 */
object ByteUtils {
    private val df = DecimalFormat("#.##")
    
    /**
     * Formats bytes into a human-readable string with appropriate unit
     */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kilobytes = bytes / 1024.0
        if (kilobytes < 1024) return "${df.format(kilobytes)} KB"
        val megabytes = kilobytes / 1024.0
        if (megabytes < 1024) return "${df.format(megabytes)} MB"
        val gigabytes = megabytes / 1024.0
        return "${df.format(gigabytes)} GB"
    }
    
    /**
     * Formats throughput into a human-readable string with appropriate unit
     */
    fun formatThroughput(bytesPerSecond: Double): String {
        if (bytesPerSecond < 1024) return "${df.format(bytesPerSecond)} B/s"
        val kilobytesPerSecond = bytesPerSecond / 1024.0
        if (kilobytesPerSecond < 1024) return "${df.format(kilobytesPerSecond)} KB/s"
        val megabytesPerSecond = kilobytesPerSecond / 1024.0
        if (megabytesPerSecond < 1024) return "${df.format(megabytesPerSecond)} MB/s"
        val gigabytesPerSecond = megabytesPerSecond / 1024.0
        return "${df.format(gigabytesPerSecond)} GB/s"
    }
    
    /**
     * Formats throughput in bits per second into a human-readable string
     */
    fun formatNetworkThroughput(bitsPerSecond: Double): String {
        if (bitsPerSecond < 1000) return "${df.format(bitsPerSecond)} bps"
        val kilobitsPerSecond = bitsPerSecond / 1000.0
        if (kilobitsPerSecond < 1000) return "${df.format(kilobitsPerSecond)} Kbps"
        val megabitsPerSecond = kilobitsPerSecond / 1000.0
        if (megabitsPerSecond < 1000) return "${df.format(megabitsPerSecond)} Mbps"
        val gigabitsPerSecond = megabitsPerSecond / 1000.0
        return "${df.format(gigabitsPerSecond)} Gbps"
    }
    
    /**
     * Calculates the time it would take to transfer a given amount of data at a constant rate
     * @param sizeBytes Size of data in bytes
     * @param speedBytesPerSecond Transfer speed in bytes per second
     * @return Estimated duration of the transfer
     */
    fun calculateTransferTime(sizeBytes: Long, speedBytesPerSecond: Double): Duration {
        if (speedBytesPerSecond <= 0) {
            throw IllegalArgumentException("Speed must be greater than zero")
        }
        
        val durationSeconds = sizeBytes.toDouble() / speedBytesPerSecond
        return Duration.parse("PT${durationSeconds}S")
    }
}
