package com.queatz

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

suspend fun ApplicationCall.receivePhotos(prefix: String, onFileNames: suspend (fileNames: List<String>) -> Unit) {
    val parts = receiveMultipart().readAllParts()

    val match = "photo\\[(\\d+)]".toRegex()
    val photos = parts
        .filter { match.matches(it.name ?: "") }
        .mapNotNull { it as? PartData.FileItem  }

    if (photos.isEmpty()) {
        HttpStatusCode.BadRequest.description("Missing 'photo[0]'")
    } else {
        if (!File("./static/photos").isDirectory) {
            File("./static/photos").mkdirs()
        }


        val photosUrls = withContext(Dispatchers.IO) {
            photos.map { photo ->
                val fileName = "$prefix-${Random.nextInt(10000000, 99999999)}-${photo.originalFileName}"
                val file = File("./static/photos/${fileName}")
                file.outputStream().write(photo.streamProvider().readBytes())
                "/static/photos/${fileName}"
            }
        }

        onFileNames(photosUrls)

        HttpStatusCode.NoContent
    }
}

suspend fun ApplicationCall.receivePhoto(prefix: String, onFileName: suspend (fileName: String) -> Unit) {
    val parts = receiveMultipart().readAllParts()

    val photo = parts.find { it.name == "photo" } as? PartData.FileItem

    if (photo == null) {
        HttpStatusCode.BadRequest.description("Missing 'photo'")
    } else {
        if (!File("./static/photos").isDirectory) {
            File("./static/photos").mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-${photo.originalFileName}"
        val file = File("./static/photos/${fileName}")

        withContext(Dispatchers.IO) {
            file.outputStream().write(photo.streamProvider().readBytes())
        }

        val photoUrl = "/static/photos/${fileName}"
        onFileName(photoUrl)

        HttpStatusCode.NoContent
    }
}
