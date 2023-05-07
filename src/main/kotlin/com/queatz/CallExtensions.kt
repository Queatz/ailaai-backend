package com.queatz

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

suspend fun ApplicationCall.receiveFiles(param: String, prefix: String, onFileNames: suspend (fileNames: List<String>) -> Unit) {
    val parts = receiveMultipart().readAllParts()

    val match = "$param\\[(\\d+)]".toRegex()
    val fileItems = parts
        .filter { match.matches(it.name ?: "") }
        .mapNotNull { it as? PartData.FileItem  }

    if (fileItems.isEmpty()) {
        HttpStatusCode.BadRequest.description("Missing '$param[0]'")
    } else {
        val folder = "./static/$param"
        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }


        val urls = withContext(Dispatchers.IO) {
            fileItems.map { fileItem ->
                val fileName = "$prefix-${Random.nextInt(10000000, 99999999)}-${fileItem.originalFileName}"
                val file = File("$folder/$fileName")
                file.outputStream().write(fileItem.streamProvider().readBytes())
                "${folder.drop(1)}/$fileName"
            }
        }

        onFileNames(urls)

        HttpStatusCode.NoContent
    }
}

suspend fun ApplicationCall.receiveFile(param: String, prefix: String, onFileName: suspend (fileName: String) -> Unit) {
    val parts = receiveMultipart().readAllParts()

    val fileItem = parts.find { it.name == param } as? PartData.FileItem

    if (fileItem == null) {
        HttpStatusCode.BadRequest.description("Missing '$param'")
    } else {
        val folder = "./static/$param"
        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-${fileItem.originalFileName}"
        val file = File("$folder/$fileName")

        withContext(Dispatchers.IO) {
            file.outputStream().write(fileItem.streamProvider().readBytes())
        }

        onFileName("${folder.drop(1)}/$fileName")

        HttpStatusCode.NoContent
    }
}
