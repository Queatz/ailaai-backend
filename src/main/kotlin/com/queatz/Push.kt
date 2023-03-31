package com.queatz

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.queatz.db.*
import com.queatz.plugins.InstantTypeConverter
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
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
        private val gmsOAuthEndpoint = "https://oauth2.googleapis.com/token"
        private val hmsPushEndpoint = "https://push-api.cloud.huawei.com/v1/${secrets.hms.appId}/messages:send"
        private val gmsPushEndpoint = "https://fcm.googleapis.com/v1/projects/${secrets.gms.appId}/messages:send"
    }

    private var hmsToken: String? = null
    private var gmsToken: String? = null

    private lateinit var coroutineScope: CoroutineScope

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        this.coroutineScope.launch {
            start()
        }
    }

    private suspend fun start() {
        withContext(Dispatchers.IO) {
            launch {
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
                            response.body<OAuthResponse>().let {
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

            launch {
                while (Thread.currentThread().isAlive) {
                    try {
                        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(secrets.gms.privateKey))
                        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
                        val token = JWT.create()
                            .withAudience(gmsOAuthEndpoint)
                            .withIssuer(secrets.gms.clientEmail)
                            .withKeyId(secrets.gms.privateKeyId)
                            .withSubject(secrets.gms.clientEmail)
                            .withIssuedAt(Clock.System.now().toJavaInstant().toGMTDate().toJvmDate())
                            .withExpiresAt(Clock.System.now().plus(1.hours).toJavaInstant().toGMTDate().toJvmDate())
                            .withClaim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                            .withClaim("type", "service_account")
                            .sign(Algorithm.RSA256(null, privateKey as RSAPrivateKey))

                        val response = http.post(gmsOAuthEndpoint) { // https://oauth2.googleapis.com/token ??
                            contentType(ContentType.Application.FormUrlEncoded)
                            setBody(FormDataContent(Parameters.build {
                                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                append("assertion", token)
                            }))
                        }

                        println(response.toString())

                        if (response.status.isSuccess()) {
                            response.body<OAuthResponse>().let {
                                gmsToken = it.access_token
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
        }
    }

    fun sendPush(device: Device, pushData: PushData) {
        coroutineScope.launch(Dispatchers.IO) {
            // Todo: Add to queue, retry, persist, etc.
            doSendPush(device, pushData)
        }
    }

    private suspend fun doSendPush(device: Device, pushData: PushData) {
        println("Sending push to ${json.toJson(device)}:")
        println(json.toJson(pushData))
        when (device.type!!) {
            DeviceType.Hms -> {
                try {
                    val response = http.post(hmsPushEndpoint) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $hmsToken")
                        setBody(
                            HmsPushBody(
                                HmsPushBodyMessage(
                                    data = gson.toJson(pushData),
                                    token = listOf(device.token!!)
                                )
                            )
                        )
                    }
                    println(response.bodyAsText())
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }

            DeviceType.Gms -> {
                try {
                    val response = http.post(gmsPushEndpoint) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $gmsToken")
                        setBody(
                            GmsPushBody(
                                GmsPushBodyMessage(
                                    data = mapOf(
                                        "action" to pushData.action!!.name,
                                        "data" to gson.toJson(pushData.data)
                                    ),
                                    token = device.token!!
                                )
                            )
                        )
                    }
                    if (response.status == HttpStatusCode.NotFound) {
                        onDeviceUnregistered(device)
                    }
                    println("FCM response: ${response.status} ${response.bodyAsText()}")
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            else -> {
                println("Push notifications for $device are not supported")
            }
        }
    }

    private fun onDeviceUnregistered(device: Device) {
        db.deleteDevice(device.type!!, device.token!!)
    }
}

enum class PushAction {
    Message,
    Collaboration
}

data class PushData(
    val action: PushAction? = null,
    val data: Any? = null,
)

data class MessagePushData(
    val group: Group,
    val person: Person,
    val message: Message,
)

enum class CollaborationEvent {
    AddedPerson,
    RemovedPerson,
    AddedCard,
    RemovedCard,
    UpdatedCard,
}

enum class CollaborationEventDataDetails {
    Photo,
    Conversation,
    Name,
    Location,
}

data class CollaborationEventData (
    val card: Card? = null,
    val person: Person? = null,
    val details: CollaborationEventDataDetails? = null
)

data class CollaborationPushData(
    val person: Person,
    val card: Card,
    val event: CollaborationEvent,
    val data: CollaborationEventData,
)

data class OAuthResponse(
    val access_token: String? = null,
    val expires_in: Long? = null,
)

data class HmsPushBody(
    @SerializedName("message") var message: HmsPushBodyMessage? = HmsPushBodyMessage(),
)

data class HmsPushBodyMessage(
    @SerializedName("data") var data: String? = null,
    @SerializedName("token") var token: List<String>? = null,
)

data class GmsPushBody(
    @SerializedName("message") var message: GmsPushBodyMessage? = GmsPushBodyMessage(),
)

data class GmsPushBodyMessage(
    @SerializedName("data") var data: Map<String, String>? = null,
    @SerializedName("token") var token: String? = null,
)
