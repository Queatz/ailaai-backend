package com.queatz

import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes

class Ai {

    companion object {
        private const val endpoint =
            "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image"
        private val apiKey = secrets.stability.key
        private val defaultStylePreset = "anime"//"cinematic"//null//"anime"//"fantasy-art"
        private const val cfgScale = 20
        private const val height = 832
        private const val width = 1216
        private const val steps = 40
    }

    private val http = HttpClient(CIO) {
        expectSuccess = true
        engine {
            requestTimeout = 10.minutes.inWholeMilliseconds
        }
    }

    suspend fun photo(prefix: String, prompts: List<StabilityTextPrompt>): String {
        val body = json.encodeToString(
            StabilityPrompt(
                prompts = prompts,
                style = defaultStylePreset,
                cfgScale = cfgScale,
                height = height,
                width = width,
                steps = steps,
            )
        )

        Logger.getAnonymousLogger().info("Sending Stability prompt: $body")

        return http.post(endpoint) {
            bearerAuth(apiKey)
            accept(ContentType.Image.PNG)
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(body)
        }.body<ByteArray>().let {
            save(prefix, it)
        }
    }

    private suspend fun save(prefix: String, image: ByteArray): String {
        val folder = "./static/ai"

        if (!File(folder).isDirectory) {
            File(folder).mkdirs()
        }

        val fileName = "$prefix-${Random.nextInt(100_000_000, 999_999_999)}-ai.png"
        val file = File("$folder/$fileName")

        withContext(Dispatchers.IO) {
            file.outputStream().write(image)
        }

        return "${folder.drop(1)}/$fileName"
    }
}

@Serializable
data class StabilityPrompt(
    @SerialName("text_prompts")
    val prompts: List<StabilityTextPrompt>,
    @SerialName("style_preset")
    val style: String?,
    @SerialName("cfg_scale")
    val cfgScale: Int,
    val height: Int,
    val width: Int,
    val steps: Int = 40,
    val samples: Int = 1
)

@Serializable
data class StabilityTextPrompt(
    val text: String,
    val weight: Double = 1.0
)
