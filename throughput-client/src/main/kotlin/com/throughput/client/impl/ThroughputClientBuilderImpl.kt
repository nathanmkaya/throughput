package com.throughput.client.impl

import com.throughput.client.api.ThroughputClient
import com.throughput.client.api.ThroughputClientBuilder
import com.throughput.client.model.ClientConfig
import com.throughput.common.util.Constants
import io.ktor.client.engine.*

/**
 * Implementation of ThroughputClientBuilder with fluent API
 */
class ThroughputClientBuilderImpl : ThroughputClientBuilder {
    private var host: String = "localhost"
    private var port: Int = Constants.DEFAULT_PORT
    private var secure: Boolean = false
    private var connectionTimeoutMs: Long = Constants.DEFAULT_TIMEOUT_MS
    private var socketTimeoutMs: Long = Constants.DEFAULT_TIMEOUT_MS
    private var requestTimeoutMs: Long = Constants.DEFAULT_TIMEOUT_MS
    private var engine: HttpClientEngineFactory<*>? = null
    
    override fun host(host: String): ThroughputClientBuilder {
        this.host = host
        return this
    }
    
    override fun port(port: Int): ThroughputClientBuilder {
        this.port = port
        return this
    }
    
    override fun secure(secure: Boolean): ThroughputClientBuilder {
        this.secure = secure
        return this
    }
    
    override fun connectionTimeout(timeoutMs: Long): ThroughputClientBuilder {
        this.connectionTimeoutMs = timeoutMs
        return this
    }
    
    override fun requestTimeout(timeoutMs: Long): ThroughputClientBuilder {
        this.requestTimeoutMs = timeoutMs
        return this
    }
    
    override fun socketTimeout(timeoutMs: Long): ThroughputClientBuilder {
        this.socketTimeoutMs = timeoutMs
        return this
    }
    
    override fun engine(engineFactory: HttpClientEngineFactory<*>): ThroughputClientBuilder {
        this.engine = engineFactory
        return this
    }
    
    override fun build(): ThroughputClient {
        val config = ClientConfig(
            protocol = if (secure) "https" else "http",
            host = host,
            port = port,
            connectionTimeoutMs = connectionTimeoutMs,
            socketTimeoutMs = socketTimeoutMs,
            requestTimeoutMs = requestTimeoutMs,
            engine = engine
        )
        
        return ThroughputClientImpl(config)
    }
}
