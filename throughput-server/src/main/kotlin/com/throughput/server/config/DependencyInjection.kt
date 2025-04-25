package com.throughput.server.config

import com.throughput.server.api.ThroughputRoutes
import com.throughput.server.service.FileGeneratorService
import com.throughput.server.service.UploadService
import com.throughput.server.service.impl.FileGeneratorServiceImpl
import com.throughput.server.service.impl.UploadServiceImpl
import org.koin.dsl.module

/**
 * Dependency injection configuration using Koin
 */
val serverModule = module {
    // Services
    single<FileGeneratorService> { FileGeneratorServiceImpl() }
    single<UploadService> { UploadServiceImpl() }
    
    // Routes
    single { ThroughputRoutes(get(), get()) }
    
    // Config
    single { ServerConfig() }
}
