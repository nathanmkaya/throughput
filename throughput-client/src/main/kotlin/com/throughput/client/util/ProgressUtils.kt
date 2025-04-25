package com.throughput.client.util

/**
 * Utility functions for handling progress reporting during data transfers
 */
object ProgressUtils {
    /**
     * Default size between progress updates (64KB)
     */
    const val DEFAULT_PROGRESS_INTERVAL = 64 * 1024L
    
    /**
     * Default size between yields to prevent thread hogging (1MB)
     */
    const val DEFAULT_YIELD_INTERVAL = 1024 * 1024L
    
    /**
     * Report progress if needed based on a periodic interval
     * 
     * @param bytesTransferred Current bytes transferred
     * @param totalBytes Total bytes to transfer
     * @param onProgress Progress callback function
     * @param lastReportedPosition Last position where progress was reported
     * @param progressInterval Minimum bytes between progress reports
     * @param forceReport Whether to force a progress report regardless of interval
     * @return New last reported position if reported, or original position if not
     */
    fun reportProgressIfNeeded(
        bytesTransferred: Long,
        totalBytes: Long,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)?,
        lastReportedPosition: Long,
        progressInterval: Long = DEFAULT_PROGRESS_INTERVAL,
        forceReport: Boolean = false
    ): Long {
        // Report progress if:
        // 1. We've transferred at least progressInterval more bytes since last report, or
        // 2. This is the end of the transfer, or
        // 3. Force report is requested
        val shouldReport = onProgress != null && (
            bytesTransferred - lastReportedPosition >= progressInterval ||
            bytesTransferred == totalBytes ||
            forceReport
        )
        
        if (shouldReport) {
            onProgress?.invoke(bytesTransferred, totalBytes)
            return bytesTransferred
        }
        
        return lastReportedPosition
    }
    
    /**
     * Determine if we should yield based on a periodic interval
     * 
     * @param bytesTransferred Current bytes transferred
     * @param nextYieldPosition Next position where yield should occur
     * @param yieldInterval Minimum bytes between yields
     * @return Pair of (shouldYield, newNextYieldPosition)
     */
    fun shouldYield(
        bytesTransferred: Long,
        nextYieldPosition: Long,
        yieldInterval: Long = DEFAULT_YIELD_INTERVAL
    ): Pair<Boolean, Long> {
        if (bytesTransferred >= nextYieldPosition) {
            return Pair(true, nextYieldPosition + yieldInterval)
        }
        return Pair(false, nextYieldPosition)
    }
}
