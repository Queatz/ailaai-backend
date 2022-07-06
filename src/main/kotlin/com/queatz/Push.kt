package com.queatz

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.queatz.db.*
import com.queatz.plugins.InstantTypeConverter
import com.queatz.plugins.secrets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Push {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantTypeConverter())
        .create()


    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {
                registerTypeAdapter(Instant::class.java, InstantTypeConverter())
            }
        }
    }

    companion object {
        private val hmsOAuthEndpoint = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
        private val hmsPushEndpoint = "https://push-api.cloud.huawei.com/v1/${secrets.hms.appId}/messages:send"
        private val gmsPushEndpoint = "https://fcm.googleapis.com/v1/projects/${secrets.gms.appId}/messages:send"
    }

    private var hmsToken: String? = null

    suspend fun start() {
        while (Thread.currentThread().isAlive) {
            try {
                val response = http.post(hmsOAuthEndpoint) {
                    header("Host", "oauth-login.cloud.huawei.com")
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(FormDataContent(Parameters.build {
                        append("grant_type", "client_credentials")
                        append("client_id", secrets.hms.clientId)
                        append("client_secret", secrets.hms.clientSecret)
                    }))
                }

                println(response.toString())

                if (response.status.isSuccess()) {
                    response.body<HmsOAuthResponse>().let {
                        hmsToken = it.access_token
                        delay((it.expires_in ?: 1.hours.inWholeSeconds).seconds.minus(30.seconds))
                    }
                } else {
                    delay(1.minutes)
                }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                delay(15.seconds)
            }
        }
    }

    suspend fun sendPush(device: Device, pushData: PushData) {
        when (device.type!!) {
            DeviceType.Hms -> {
                try {
                    http.post(hmsPushEndpoint) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $hmsToken")
                        setBody(
                            PushBody(
                                message = PushBodyMessage(
                                    data = gson.toJson(pushData),
                                    token = listOf(device.token!!)
                                )
                            )
                        )
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            DeviceType.Gms -> {
                try {
                    http.post(gmsPushEndpoint) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "key=${secrets.gms.apiKey}")
                        setBody(
                            PushBody(
                                message = PushBodyMessage(
                                    data = gson.toJson(pushData),
                                    token = listOf(device.token!!)
                                )
                            )
                        )
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
        }
    }
}

enum class PushAction {
    Message
}

data class PushData(
    val action: PushAction? = null,
    val data: Any? = null
)

data class MessagePushData(
    val group: Group,
    val person: Person,
    val message: Message
)

data class HmsOAuthResponse(
    val access_token: String? = null,
    val expires_in: Long? = null
)

data class PushBody(
    @SerializedName("validate_only") var validateOnly: Boolean = false,
    @SerializedName("message") var message: PushBodyMessage? = PushBodyMessage()
)

data class PushBodyMessage(
    @SerializedName("data") var data: String? = null,
    @SerializedName("priority") var priority: String? = "high",
    @SerializedName("token") var token: List<String>? = null
)
