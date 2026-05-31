package com.example.api

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
data class UserProfileResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "email") val email: String,
    @Json(name = "full_name") val fullName: String?,
    @Json(name = "profile_code") val profileCode: String?,
    @Json(name = "default_group_id") val defaultGroupId: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "nickname") val nickname: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupMemberResponse(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "full_name") val fullName: String?,
    @Json(name = "email") val email: String,
    @Json(name = "is_admin") val isAdmin: Boolean,
    @Json(name = "joined_at") val joinedAt: String?,
    @Json(name = "nickname") val nickname: String? = null
)

@JsonClass(generateAdapter = true)
data class SpendingGroupResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "created_by_user_id") val createdByUserId: Int,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "members") val members: List<GroupMemberResponse>
)

@JsonClass(generateAdapter = true)
data class ShoppingListItemResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "list_id") val listId: String,
    @Json(name = "name") val name: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "is_checked") val isChecked: Boolean,
    @Json(name = "added_by_user_id") val addedByUserId: Int,
    @Json(name = "added_at") val addedAt: String?
)

@JsonClass(generateAdapter = true)
data class ShoppingListResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "created_by_user_id") val createdByUserId: Int,
    @Json(name = "is_shared") val isShared: Boolean,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "items") val items: List<ShoppingListItemResponse>? = null
)

@JsonClass(generateAdapter = true)
data class LedgerSubmitRequest(
    @Json(name = "storeName") val storeName: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "date") val date: String, // Stringa YYYY-MM-DD
    @Json(name = "paid_by") val paidBy: String = "Io",
    @Json(name = "is_shared") val isShared: Boolean = true,
    @Json(name = "group_id") val groupId: String? = null,
    @Json(name = "paid_by_user_id") val paidByUserId: Int? = null,
    @Json(name = "client_uuid") val clientUuid: String,
    @Json(name = "items") val items: List<LedgerItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "is_blocked") val isBlocked: Boolean,
    @Json(name = "custom_limit") val customLimit: Double? = null
)

@JsonClass(generateAdapter = true)
data class CatalogItemCreate(
    @Json(name = "barcode") val barcode: String,
    @Json(name = "name") val name: String,
    @Json(name = "brand") val brand: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "price") val price: Double?,
    @Json(name = "unit_price") val unitPrice: Double?,
    @Json(name = "weight") val weight: Double?,
    @Json(name = "discount_label") val discountLabel: String?,
    @Json(name = "store_name") val storeName: String?,
    @Json(name = "vat_number") val vatNumber: String?
)

@JsonClass(generateAdapter = true)
data class TelemetryEventCreate(
    @Json(name = "device_uuid") val deviceUuid: String,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "store_name") val storeName: String,
    @Json(name = "dwell_time_seconds") val dwellTimeSeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class ActiveShoppingSessionResponse(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "user_name") val userName: String,
    @Json(name = "store_name") val storeName: String,
    @Json(name = "started_at") val startedAt: String,
    @Json(name = "dwell_time_seconds") val dwellTimeSeconds: Int
)

@JsonClass(generateAdapter = true)
data class CatalogItemResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "barcode") val barcode: String?,
    @Json(name = "name") val name: String,
    @Json(name = "brand") val brand: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "price") val price: Double?,
    @Json(name = "unit_price") val unitPrice: Double?,
    @Json(name = "weight") val weight: Double?,
    @Json(name = "discount_label") val discountLabel: String?,
    @Json(name = "store_id") val storeId: Int?,
    @Json(name = "scanned_by_user_id") val scannedByUserId: Int?,
    @Json(name = "timestamp") val timestamp: String?
)

@JsonClass(generateAdapter = true)
data class CatalogPriceComparisonItem(
    @Json(name = "store_id") val storeId: Int,
    @Json(name = "store_name") val storeName: String,
    @Json(name = "price") val price: Double,
    @Json(name = "unit_price") val unitPrice: Double?,
    @Json(name = "discount_label") val discountLabel: String?,
    @Json(name = "scanned_by") val scannedBy: String,
    @Json(name = "scanned_at") val scannedAt: String
)

@JsonClass(generateAdapter = true)
data class CatalogItemCompareResponse(
    @Json(name = "product_name") val productName: String,
    @Json(name = "barcode") val barcode: String?,
    @Json(name = "brand") val brand: String?,
    @Json(name = "prices") val prices: List<CatalogPriceComparisonItem>
)

@JsonClass(generateAdapter = true)
data class ShrinkflationAlertResponse(
    @Json(name = "itemName") val itemName: String,
    @Json(name = "brand") val brand: String,
    @Json(name = "originalWeight") val originalWeight: Double,
    @Json(name = "newWeight") val newWeight: Double,
    @Json(name = "weightReductionPercent") val weightReductionPercent: Double,
    @Json(name = "originalPrice") val originalPrice: Double,
    @Json(name = "newPrice") val newPrice: Double,
    @Json(name = "priceIncreasePercent") val priceIncreasePercent: Double,
    @Json(name = "detectedAt") val detectedAt: String,
    @Json(name = "store") val store: String,
    @Json(name = "barcode") val barcode: String?
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

@JsonClass(generateAdapter = true)
data class ItemResponseDto(
    @Json(name = "name") val name: String,
    @Json(name = "brand") val brand: String? = "",
    @Json(name = "category") val category: String,
    @Json(name = "price") val price: Double,
    @Json(name = "unitPrice") val unitPrice: Double? = 0.0,
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "barcode") val barcode: String? = ""
)

@JsonClass(generateAdapter = true)
data class LedgerDetailedResponseDto(
    @Json(name = "id") val id: Int,
    @Json(name = "description") val description: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "category") val category: String?,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "paid_by") val paidBy: String,
    @Json(name = "is_shared") val isShared: Boolean,
    @Json(name = "group_id") val groupId: String?,
    @Json(name = "paid_by_user_id") val paidByUserId: Int?,
    @Json(name = "client_uuid") val clientUuid: String?,
    @Json(name = "store_name") val storeName: String,
    @Json(name = "store_vat") val storeVat: String?,
    @Json(name = "store_address") val storeAddress: String?,
    @Json(name = "items") val items: List<ItemResponseDto>
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
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

    suspend fun getUserProfile(token: String?): UserProfileResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext null
        val url = "${getBaseUrl()}/api/v1/auth/me"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(UserProfileResponse::class.java).fromJson(raw)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun getSpendingGroups(token: String?): List<SpendingGroupResponse> = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext emptyList()
        val url = "${getBaseUrl()}/api/v1/groups"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext emptyList()
                    val type = Types.newParameterizedType(List::class.java, SpendingGroupResponse::class.java)
                    return@withContext moshi.adapter<List<SpendingGroupResponse>>(type).fromJson(raw) ?: emptyList()
                }
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun createSpendingGroup(token: String?, name: String): SpendingGroupResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext null
        val url = "${getBaseUrl()}/api/v1/groups"
        val json = """{"name":"$name"}"""
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(SpendingGroupResponse::class.java).fromJson(raw)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun addMemberToGroup(token: String?, groupId: String, profileCode: String): GroupMemberResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext null
        val url = "${getBaseUrl()}/api/v1/groups/$groupId/members"
        val json = """{"profile_code":"$profileCode"}"""
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(GroupMemberResponse::class.java).fromJson(raw)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun setDefaultGroup(token: String?, groupId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext false
        val url = "${getBaseUrl()}/api/v1/groups/default"
        val json = """{"group_id":"$groupId"}"""
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .put(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun removeMemberFromGroup(token: String?, groupId: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext false
        val url = "${getBaseUrl()}/api/v1/groups/$groupId/members/$userId"
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .delete().build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun getShoppingLists(token: String?): List<ShoppingListResponse> = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext emptyList()
        val url = "${getBaseUrl()}/api/v1/lists"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext emptyList()
                    val type = Types.newParameterizedType(List::class.java, ShoppingListResponse::class.java)
                    return@withContext moshi.adapter<List<ShoppingListResponse>>(type).fromJson(raw) ?: emptyList()
                }
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun createShoppingList(token: String?, name: String): ShoppingListResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext null
        val url = "${getBaseUrl()}/api/v1/lists"
        val json = """{"name":"$name"}"""
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(ShoppingListResponse::class.java).fromJson(raw)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun shareShoppingList(token: String?, listId: String, profileCode: String?, groupId: String?): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || token == null) return@withContext false
        val url = "${getBaseUrl()}/api/v1/lists/$listId/share"
        val json = if (profileCode != null) """{"profile_code":"$profileCode"}""" else """{"group_id":"$groupId"}"""
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun deleteLedgerEntry(token: String?, entryId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/ledger/$entryId"
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.delete().build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            return@withContext false
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
        
        // Converte il timestamp in formato completo
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date(timestamp))
        
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

    suspend fun updateLedgerEntry(token: String?, deviceUuid: String, entryId: Int, storeName: String, amount: Double, timestamp: Long, items: List<LedgerItemDto>?, clientUuid: String): Int = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext 250
        val url = "${getBaseUrl()}/api/v1/ledger/$entryId"
        
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date(timestamp))
        
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
        val request = reqBuilder.put(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.code
            }
        } catch (e: Exception) {
            return@withContext 500
        }
    }
    
    suspend fun submitShelfLabel(token: String?, deviceUuid: String, item: CatalogItemCreate): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/scan/shelflabel"
        val json = moshi.adapter(CatalogItemCreate::class.java).toJson(item)
        
        val reqBuilder = Request.Builder().url(url).header("X-Device-ID", deviceUuid)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { return@withContext it.isSuccessful }
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    suspend fun submitTelemetry(token: String?, event: TelemetryEventCreate): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        val url = "${getBaseUrl()}/api/v1/scan/telemetry"
        val json = moshi.adapter(TelemetryEventCreate::class.java).toJson(event)
        
        val reqBuilder = Request.Builder().url(url).header("X-Device-ID", event.deviceUuid)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.post(json.toRequestBody("application/json".toMediaType())).build()
        try {
            client.newCall(request).execute().use { return@withContext it.isSuccessful }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun getActiveShoppingSessions(token: String?): List<ActiveShoppingSessionResponse> = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext emptyList()
        val url = "${getBaseUrl()}/api/v1/scan/active_sessions"
        
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val type = Types.newParameterizedType(List::class.java, ActiveShoppingSessionResponse::class.java)
                val adapter = moshi.adapter<List<ActiveShoppingSessionResponse>>(type)
                return@withContext adapter.fromJson(bodyStr) ?: emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun searchCatalog(token: String?, query: String): List<CatalogItemResponse> = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || query.isBlank()) return@withContext emptyList()
        // url encode query
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${getBaseUrl()}/api/v1/scan/catalog/search?q=$encodedQuery"
        
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val type = Types.newParameterizedType(List::class.java, CatalogItemResponse::class.java)
                val adapter = moshi.adapter<List<CatalogItemResponse>>(type)
                return@withContext adapter.fromJson(bodyStr) ?: emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun compareCatalogPrices(token: String?, barcode: String?, name: String?): CatalogItemCompareResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext null
        if (barcode.isNullOrBlank() && name.isNullOrBlank()) return@withContext null
        
        val urlBuilder = "${getBaseUrl()}/api/v1/scan/catalog/compare".toHttpUrlOrNull()?.newBuilder() ?: return@withContext null
        if (!barcode.isNullOrBlank()) {
            urlBuilder.addQueryParameter("barcode", barcode)
        } else if (!name.isNullOrBlank()) {
            urlBuilder.addQueryParameter("name", name)
        }
        
        val reqBuilder = Request.Builder().url(urlBuilder.build())
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val adapter = moshi.adapter(CatalogItemCompareResponse::class.java)
                return@withContext adapter.fromJson(bodyStr)
            }
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun getShrinkflationAlerts(token: String?): List<ShrinkflationAlertResponse> = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext emptyList()
        val url = "${getBaseUrl()}/api/v1/scan/catalog/shrinkflation"
        
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val type = Types.newParameterizedType(List::class.java, ShrinkflationAlertResponse::class.java)
                val adapter = moshi.adapter<List<ShrinkflationAlertResponse>>(type)
                return@withContext adapter.fromJson(bodyStr) ?: emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun checkSingleShrinkflation(token: String?, barcode: String): ShrinkflationAlertResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured() || barcode.isBlank()) return@withContext null
        val url = "${getBaseUrl()}/api/v1/scan/catalog/$barcode/shrinkflation"
        
        val reqBuilder = Request.Builder().url(url)
        if (token != null) reqBuilder.header("Authorization", "Bearer $token")
        
        val request = reqBuilder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val adapter = moshi.adapter(ShrinkflationAlertResponse::class.java)
                return@withContext adapter.fromJson(bodyStr)
            }
        } catch (e: Exception) {
            return@withContext null
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

    suspend fun updateNickname(token: String, nickname: String): UserProfileResponse? = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext null
        val url = "${getBaseUrl()}/api/v1/auth/nickname"
        val payload = "{\"nickname\":\"$nickname\"}"
        val reqBuilder = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .put(payload.toRequestBody("application/json".toMediaType()))
        val request = reqBuilder.build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext null
                    return@withContext moshi.adapter(UserProfileResponse::class.java).fromJson(raw)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("LocalBackendService", "Errore aggiornamento nickname", e)
            return@withContext null
        }
    }

    suspend fun fetchGroupLedgerEntries(token: String?): List<LedgerDetailedResponseDto> = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext emptyList()
        val url = "${getBaseUrl()}/api/v1/ledger"
        
        val reqBuilder = Request.Builder().url(url)
        if (token != null) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        val request = reqBuilder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string() ?: return@withContext emptyList()
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, LedgerDetailedResponseDto::class.java)
                    return@withContext moshi.adapter<List<LedgerDetailedResponseDto>>(type).fromJson(raw) ?: emptyList()
                }
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("LocalBackendService", "Errore nel recupero degli scontrini di gruppo", e)
            return@withContext emptyList()
        }
    }
}
