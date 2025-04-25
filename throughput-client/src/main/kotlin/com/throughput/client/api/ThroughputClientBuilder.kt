package com.throughput.client.api

import io.ktor.client.engine.*

/**
 * Builder interface for creating ThroughputClient instances
 */
interface ThroughputClientBuilder {
    /**
     * Set the server host
     * @param host Server hostname or IP address
     * @return Builder instance for method chaining
     */
    fun host(host: String): ThroughputClientBuilder
    
    /**
     * Set the server port
     * @param port Server port
     * @return Builder instance for method chaining
     */
    fun port(port: Int): ThroughputClientBuilder
    
    /**
     * Set whether to use HTTPS
     * @param secure True to use HTTPS, false for HTTP
     * @return Builder instance for method chaining
     */
    fun secure(secure: Boolean): ThroughputClientBuilder
    
    /**
     * Set connection timeout in milliseconds
     * @param timeoutMs Connection timeout in milliseconds
     * @return Builder instance for method chaining
     */
    fun connectionTimeout(timeoutMs: Long): ThroughputClientBuilder
    
    /**
     * Set request timeout in milliseconds (total timeout for the entire request)
     * @param timeoutMs Request timeout in milliseconds
     * @return Builder instance for method chaining
     */
    fun requestTimeout(timeoutMs: Long): ThroughputClientBuilder
    
    /**
     * Set socket read/write timeout in milliseconds
     * @param timeoutMs Socket timeout in milliseconds
     * @return Builder instance for method chaining
     */
    fun socketTimeout(timeoutMs: Long): ThroughputClientBuilder
    
    /**
     * Set custom HTTP client engine factory
     * @param engineFactory Custom HTTP client engine factory (e.g., OkHttp, Apache, CIO)
     * @return Builder instance for method chaining
     */
    fun engine(engineFactory: HttpClientEngineFactory<*>): ThroughputClientBuilder
    
    /**
     * Build the ThroughputClient instance
     * @return Configured ThroughputClient instance
     */
    fun build(): ThroughputClient
}
