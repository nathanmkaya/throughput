package com.throughput.server.api

import com.throughput.common.model.ThroughputException
import com.throughput.common.util.Constants
import com.throughput.server.service.FileGeneratorService
import com.throughput.server.service.UploadService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

/**
 * Configures routes for throughput testing
 */
class ThroughputRoutes(
    private val fileGeneratorService: FileGeneratorService,
    private val uploadService: UploadService
) {
    private val logger = LoggerFactory.getLogger(ThroughputRoutes::class.java)
    
    fun setup(routing: Routing) {
        routing.route("/${Constants.API_VERSION}") {
            downloadRoute()
            uploadRoute()
        }
    }
    
    private fun Route.downloadRoute() {
        // Using GET with path parameter for size is more RESTful
        get("${Constants.DOWNLOAD_ENDPOINT}/{size}") {
            try {
                // Get size from path parameter
                val size = call.parameters["size"]?.toLongOrNull()
                    ?: throw ThroughputException("Missing or invalid size parameter")
                
                logger.info("Download request received for $size bytes")
                
                if (!fileGeneratorService.isValidSize(size)) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid size requested: $size bytes. Maximum allowed: ${Constants.MAX_DOWNLOAD_SIZE_BYTES} bytes")
                    return@get
                }
                
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, 
                        "throughput-test-$size.bin"
                    ).toString()
                )
                
                // Use respondBytesWriter for non-blocking IO
                call.respondBytesWriter(contentType = ContentType.Application.OctetStream) {
                    fileGeneratorService.generateRandomData(size, this)
                }
                
            } catch (e: ThroughputException) {
                logger.warn("Download request validation failed", e)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid download request")
            } catch (e: Exception) {
                logger.error("Error processing download request", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing download request: ${e.message}")
            }
        }
        
        // Keep the POST endpoint for backward compatibility, but mark as deprecated
        post(Constants.DOWNLOAD_ENDPOINT) {
            try {
                // Get size from request body
                val request = call.receive<Map<String, Long>>()
                val size = request["sizeBytes"]
                    ?: throw ThroughputException("Missing sizeBytes parameter")
                
                logger.info("Download request received via POST for $size bytes (deprecated method)")
                
                if (!fileGeneratorService.isValidSize(size)) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid size requested: $size bytes. Maximum allowed: ${Constants.MAX_DOWNLOAD_SIZE_BYTES} bytes")
                    return@post
                }
                
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, 
                        "throughput-test-$size.bin"
                    ).toString()
                )
                
                // Use respondBytesWriter for non-blocking IO
                call.respondBytesWriter(contentType = ContentType.Application.OctetStream) {
                    fileGeneratorService.generateRandomData(size, this)
                }
                
            } catch (e: ThroughputException) {
                logger.warn("Download request validation failed", e)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid download request")
            } catch (e: Exception) {
                logger.error("Error processing download request", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing download request: ${e.message}")
            }
        }
    }
    
    private fun Route.uploadRoute() {
        post(Constants.UPLOAD_ENDPOINT) {
            try {
                val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                    ?: throw ThroughputException("Content-Length header is required for size validation")
                
                logger.info("Upload request received with Content-Length: $contentLength bytes")
                
                if (!uploadService.isValidUploadSize(contentLength)) {
                    call.respond(HttpStatusCode.BadRequest, "Upload size exceeds maximum allowed: ${Constants.MAX_UPLOAD_SIZE_BYTES} bytes")
                    return@post
                }
                
                // Use receiveChannel for non-blocking IO
                val channel = call.receiveChannel()
                val result = uploadService.processUpload(channel, contentLength)
                call.respond(HttpStatusCode.OK, result)
                
            } catch (e: ThroughputException) {
                logger.warn("Upload request validation failed", e)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid upload request")
            } catch (e: Exception) {
                logger.error("Error processing upload", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing upload: ${e.message}")
            }
        }
    }
}
