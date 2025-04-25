package com.throughput.server.api

import com.throughput.common.model.ThroughputException
import com.throughput.common.model.UploadResult
import com.throughput.common.util.Constants
import com.throughput.server.service.FileGeneratorService
import com.throughput.server.service.UploadService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.io.IOException

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
        // RESTful GET endpoint with path parameter
        get("${Constants.DOWNLOAD_ENDPOINT}/{size}") {
            try {
                // Get size from path parameter
                val size = call.parameters["size"]?.toLongOrNull()
                    ?: throw ThroughputException("Missing or invalid size parameter")
                
                logger.info("Download request received via GET for $size bytes")
                call.handleDownloadRequest(size)
            } catch (e: ThroughputException) {
                logger.warn("Download request validation failed", e)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid download request")
            } catch (e: IOException) {
                logger.error("I/O error processing download request", e)
                call.respond(HttpStatusCode.InternalServerError, "I/O error processing download request: ${e.message}")
            } catch (e: Exception) {
                logger.error("Error processing download request", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing download request: ${e.message}")
            }
        }
        
        // Backwards-compatible POST endpoint
        post(Constants.DOWNLOAD_ENDPOINT) {
            try {
                // Get size from request body
                val request = call.receive<Map<String, Long>>()
                val size = request["sizeBytes"]
                    ?: throw ThroughputException("Missing sizeBytes parameter")
                
                logger.info("Download request received via POST for $size bytes (deprecated method)")
                call.handleDownloadRequest(size)
            } catch (e: ThroughputException) {
                logger.warn("Download request validation failed", e)
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid download request")
            } catch (e: ContentTransformationException) {
                logger.warn("Content transformation error", e)
                call.respond(HttpStatusCode.BadRequest, "Invalid request format: ${e.message}")
            } catch (e: IOException) {
                logger.error("I/O error processing download request", e)
                call.respond(HttpStatusCode.InternalServerError, "I/O error processing download request: ${e.message}")
            } catch (e: Exception) {
                logger.error("Error processing download request", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing download request: ${e.message}")
            }
        }
    }
    
    /**
     * Common handler for download requests
     * @param size Size of data to download in bytes
     */
    private suspend fun ApplicationCall.handleDownloadRequest(size: Long) {
        if (!fileGeneratorService.isValidSize(size)) {
            respond(HttpStatusCode.BadRequest, "Invalid size requested: $size bytes. Maximum allowed: ${Constants.MAX_DOWNLOAD_SIZE_BYTES} bytes")
            return
        }
        
        response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName, 
                "throughput-test-$size.bin"
            ).toString()
        )
        
        // Use respondBytesWriter for non-blocking IO
        respondBytesWriter(contentType = ContentType.Application.OctetStream) {
            fileGeneratorService.generateRandomData(size, this)
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
            } catch (e: IOException) {
                logger.error("I/O error during upload", e)
                call.respond(HttpStatusCode.InternalServerError, "I/O error during upload: ${e.message}")
            } catch (e: Exception) {
                logger.error("Error processing upload", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing upload: ${e.message}")
            }
        }
    }
}
