package com.example.api

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OcrElementDto(
    @Json(name = "text") val text: String,
    @Json(name = "x") val x: Double,
    @Json(name = "y") val y: Double,
    @Json(name = "width") val width: Double,
    @Json(name = "height") val height: Double
)

@JsonClass(generateAdapter = true)
data class LocalScanRequest(
    @Json(name = "ocrText") val ocrText: String,
    @Json(name = "elements") val elements: List<OcrElementDto>? = null
)

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "username") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class BiometricRegisterRequest(
    @Json(name = "device_uuid") val deviceUuid: String,
    @Json(name = "public_key_pem") val publicKeyPem: String
)

@JsonClass(generateAdapter = true)
data class BiometricChallengeRequest(
    @Json(name = "device_uuid") val deviceUuid: String
)

@JsonClass(generateAdapter = true)
data class BiometricChallengeResponse(
    @Json(name = "challenge") val challenge: String
)

@JsonClass(generateAdapter = true)
data class BiometricLoginRequest(
    @Json(name = "device_uuid") val deviceUuid: String,
    @Json(name = "challenge") val challenge: String,
    @Json(name = "signature_hex") val signatureHex: String
)

@JsonClass(generateAdapter = true)
data class BackendNotification(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String,
    @Json(name = "type") val type: String, // BROADCAST, GEO, STORE_SPECIFIC
    @Json(name = "target_store_id") val targetStoreId: Int? = null,
    @Json(name = "target_city") val targetCity: String? = null,
    @Json(name = "target_region") val targetRegion: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LedgerItemDto(
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Double,
    @Json(name = "category") val category: String? = null
)

@JsonClass(generateAdapter = true)
data class LedgerSubmitRequest(
    @Json(name = "storeName") val storeName: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String, // Stringa YYYY-MM-DD
    @Json(name = "paid_by") val paidBy: String = "Io",
    @Json(name = "is_shared") val isShared: Boolean = true,
    @Json(name = "client_uuid") val clientUuid: String,
    @Json(name = "items") val items: List<LedgerItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "is_blocked") val isBlocked: Boolean,
    @Json(name = "custom_limit") val customLimit: Double? = null
)

@JsonClass(generateAdapter = true)
data class SyncRequest(
    @Json(name = "device_uuid") val deviceUuid: String,
    @Json(name = "pending_ledger_entries") val pendingLedgerEntries: List<LedgerSubmitRequest>,
    @Json(name = "pending_notification_acks") val pendingNotificationAcks: List<Int>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "server_timestamp") val serverTimestamp: String,
    @Json(name = "synced_ledger_uuids") val syncedLedgerUuids: List<String>,
    @Json(name = "synced_notification_acks") val syncedNotificationAcks: List<Int>,
    @Json(name = "new_notifications") val newNotifications: List<BackendNotification>,
    @Json(name = "device_status") val deviceStatus: DeviceStatusDto
)

object BiometricKeyManager {
    private const val KEY_ALIAS = "SmartGroceryBiometricKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun generateKeyPair(): String? {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .setUserAuthenticationRequired(true)
                .build()

            kpg.initialize(parameterSpec)
            val keyPair = kpg.generateKeyPair()
            
            val pubBytes = keyPair.public.encoded
            val base64Pub = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
            return "-----BEGIN PUBLIC KEY-----\n$base64Pub\n-----END PUBLIC KEY-----"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun isKeyGenerated(): Boolean {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            return keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            return false
        }
    }

    fun getSignatureObject(): Signature? {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            return signature
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}

object LocalBackendServiceClient {
    private const val TAG = "LocalBackendServiceClient"
    
    var lastApiError: String? = null
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
        
    fun isHostConfigured(): Boolean {
        val ip = BuildConfig.LOCAL_BACKEND_IP
        return !ip.isNullOrBlank() && ip != "0.0.0.0"
    }

    private fun getBaseUrl(): String {
        return "http://${BuildConfig.LOCAL_BACKEND_IP}:8000"
    }

    suspend fun scanReceipt(ocrText: String, elements: List<OcrElementDto>?): ParsingReceiptResult? = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) {
            lastApiError = "IP host non configurato in network_config.json."
            return@withContext null
        }
        val url = "${getBaseUrl()}/api/v1/scan"
        val requestBodyMoshi = LocalScanRequest(ocrText = ocrText, elements = elements)
        val adapter = moshi.adapter(LocalScanRequest::class.java)
        val jsonRequest = adapter.toJson(requestBodyMoshi)
        
        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toRequestBody("application/json".toMediaType()))
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    lastApiError = "HTTP ${response.code}: $errBody"
                    return@withContext null
                }
                val rawResponse = response.body?.string() ?: return@withContext null
                val responseAdapter = moshi.adapter(ParsingReceiptResultJson::class.java)
                return@withContext responseAdapter.fromJson(rawResponse)?.toParsingReceiptResult()
            }
        } catch (e: Exception) {
            lastApiError = e.localizedMessage
            return@withContext null
        }
    }

    suspend fun registerUser(email: String, authKey: String): Boolean = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/auth/register"
        
        // Genera un nome utente e un nome famiglia eleganti partendo dal prefisso dell'email
        val namePrefix = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        
        val json = """
            {
                "email": "$email",
                "password": "$authKey",
                "full_name": "$namePrefix",
                "household_name": "Famiglia $namePrefix"
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    lastApiError = response.body?.string() ?: "Errore server: ${response.code}"
                    return@withContext false
                }
                return@withContext true
            }
        } catch (e: Exception) {
            lastApiError = e.localizedMessage
            return@withContext false
        }
    }

    suspend fun loginUser(email: String, authKey: String): AuthResponse? = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) return@withContext null
        val url = "${getBaseUrl()}/api/v1/auth/token"
        val requestBody = okhttp3.FormBody.Builder()
            .add("username", email)
            .add("password", authKey)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(AuthResponse::class.java).fromJson(raw)
                } else {
                    lastApiError = "Credenziali non valide (HTTP ${response.code})"
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            lastApiError = e.localizedMessage
            return@withContext null
        }
    }

    suspend fun registerBiometricKey(token: String, deviceUuid: String, publicKeyPem: String): Boolean = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/auth/biometric/register"
        val req = BiometricRegisterRequest(deviceUuid, publicKeyPem)
        val json = moshi.adapter(BiometricRegisterRequest::class.java).toJson(req)
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun getBiometricChallenge(deviceUuid: String): String? = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) return@withContext null
        val url = "${getBaseUrl()}/api/v1/auth/biometric/challenge"
        val req = BiometricChallengeRequest(deviceUuid)
        val json = moshi.adapter(BiometricChallengeRequest::class.java).toJson(req)
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(BiometricChallengeResponse::class.java).fromJson(raw)?.challenge
                }
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun loginBiometric(deviceUuid: String, challenge: String, signatureHex: String): AuthResponse? = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) return@withContext null
        val url = "${getBaseUrl()}/api/v1/auth/biometric/login"
        val req = BiometricLoginRequest(deviceUuid, challenge, signatureHex)
        val json = moshi.adapter(BiometricLoginRequest::class.java).toJson(req)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(AuthResponse::class.java).fromJson(raw)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun getUnreadNotifications(token: String?, deviceUuid: String): List<BackendNotification> = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext emptyList()
        val url = "${getBaseUrl()}/api/v1/notifications/unread?device_uuid=$deviceUuid"
        val reqBuilder = Request.Builder().url(url)
            .header("X-Device-ID", deviceUuid)
        if (token != null) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        val request = reqBuilder.build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext emptyList()
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, BackendNotification::class.java)
                    return@withContext moshi.adapter<List<BackendNotification>>(type).fromJson(raw) ?: emptyList()
                }
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun acknowledgeNotification(token: String?, id: Int, deviceUuid: String): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/notifications/$id/ack"
        val json = """{"device_uuid":"$deviceUuid"}"""
        val reqBuilder = Request.Builder().url(url)
            .header("X-Device-ID", deviceUuid)
        if (token != null) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        val request = reqBuilder.post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun submitLedgerEntry(token: String?, deviceUuid: String, storeName: String, amount: Double, timestamp: Long, items: List<LedgerItemDto>?, clientUuid: String): Int = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext 250
        val url = "${getBaseUrl()}/api/v1/ledger"
        
        // Converte il timestamp in formato yyyy-MM-dd
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestamp))
        
        val req = LedgerSubmitRequest(
            storeName = storeName,
            amount = amount,
            date = dateStr,
            paidBy = "Io",
            isShared = true,
            clientUuid = clientUuid,
            items = items
        )
        
        val json = moshi.adapter(LedgerSubmitRequest::class.java).toJson(req)
        val reqBuilder = Request.Builder().url(url)
            .header("X-Device-ID", deviceUuid)
        if (token != null) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        val request = reqBuilder.post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // Ritorna un codice di successo convenzionale (es. 201)
                    return@withContext response.code
                }
                return@withContext response.code
            }
        } catch (e: Exception) {
            return@withContext 500
        }
    }

    suspend fun performUnifiedSync(token: String?, requestBody: SyncRequest): SyncResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext null
        val url = "${getBaseUrl()}/api/v1/sync"
        val json = moshi.adapter(SyncRequest::class.java).toJson(requestBody)
        val reqBuilder = Request.Builder().url(url)
            .header("X-Device-ID", requestBody.deviceUuid)
        if (token != null) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        val request = reqBuilder.post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(SyncResponse::class.java).fromJson(raw)
                } else {
                    lastApiError = "Errore sync: HTTP ${response.code}"
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            lastApiError = e.localizedMessage
            return@withContext null
        }
    }
}
