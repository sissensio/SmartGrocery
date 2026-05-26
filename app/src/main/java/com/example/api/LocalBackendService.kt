package com.example.api

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
        
    /**
     * Checks if the dynamic dynamic host IP is set and has a valid format.
     */
    fun isHostConfigured(): Boolean {
        val ip = BuildConfig.LOCAL_BACKEND_IP
        return !ip.isNullOrBlank() && ip != "0.0.0.0"
    }

    /**
     * Sends the OCR text and dynamic bounding box coordinate elements to the local FastAPI backend.
     */
    suspend fun scanReceipt(ocrText: String, elements: List<OcrElementDto>?): ParsingReceiptResult? = withContext(Dispatchers.IO) {
        lastApiError = null
        if (!isHostConfigured()) {
            lastApiError = "IP host non configurato correttamente in network_config.json."
            Log.e(TAG, "Dynamic IP is invalid or 0.0.0.0: ${BuildConfig.LOCAL_BACKEND_IP}")
            return@withContext null
        }
        
        val backendIp = BuildConfig.LOCAL_BACKEND_IP
        val url = "http://$backendIp:8000/api/v1/scan"
        
        Log.i(TAG, "Sending spatial OCR payload to local backend server at $url...")
        
        val requestBodyMoshi = LocalScanRequest(
            ocrText = ocrText,
            elements = elements
        )
        
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
                    Log.e(TAG, "Local Backend HTTP Error: ${response.code} ${response.message} - $errBody")
                    return@withContext null
                }
                val rawResponse = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Local Backend Response length: ${rawResponse.length}")
                
                val responseAdapter = moshi.adapter(ParsingReceiptResultJson::class.java)
                return@withContext responseAdapter.fromJson(rawResponse)?.toParsingReceiptResult()
            }
        } catch (e: IOException) {
            lastApiError = "Impossibile connettersi al server FastAPI (${e.localizedMessage}). Verifica che il server sia acceso e sulla stessa rete LAN."
            Log.e(TAG, "Network exception during Local Backend API call at $url", e)
            return@withContext null
        } catch (e: Exception) {
            lastApiError = "Errore nel parsing della risposta JSON: ${e.localizedMessage}"
            Log.e(TAG, "JSON parsing error on Local Backend response", e)
            return@withContext null
        }
    }
}
