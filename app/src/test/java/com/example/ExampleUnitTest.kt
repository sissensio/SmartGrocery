package com.example

import com.example.api.GeminiServiceClient
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun testListModels() = runBlocking {
    val key = System.getenv("GEMINI_API_KEY") ?: System.getenv("gemini_api_key") ?: ""
    println("Using GEMINI_API_KEY length: ${key.length}")
    
    val client = okhttp3.OkHttpClient()
    
    // Test 1: List Models
    println("\n=== TESTING LIST MODELS ===")
    val listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
    val listRequest = okhttp3.Request.Builder().url(listUrl).get().build()
    try {
        client.newCall(listRequest).execute().use { response ->
            println("List Models Code: ${response.code}")
            val body = response.body?.string() ?: ""
            println("List Models response: $body")
        }
    } catch (e: Exception) {
        println("Error listing models: ${e.message}")
    }
    
    // Test 2: List Models on v1
    println("\n=== TESTING LIST MODELS v1 ===")
    val listUrlV1 = "https://generativelanguage.googleapis.com/v1/models?key=$key"
    val listRequestV1 = okhttp3.Request.Builder().url(listUrlV1).get().build()
    try {
        client.newCall(listRequestV1).execute().use { response ->
            println("List Models v1 Code: ${response.code}")
            val body = response.body?.string() ?: ""
            println("List Models v1 response: $body")
        }
    } catch (e: Exception) {
        println("Error listing models v1: ${e.message}")
    }

    // Test 3: GenerateContent on gemini-3.5-flash
    println("\n=== TESTING GENERATE CONTENT gemini-3.5-flash ===")
    val payload = """
        {
          "contents": [
            {
              "parts": [{ "text": "Ciao" }]
            }
          ]
        }
    """.trimIndent()
    val mediaType = "application/json".toMediaType()
    val g35Request = okhttp3.Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key")
        .post(payload.toRequestBody(mediaType))
        .build()
    try {
        client.newCall(g35Request).execute().use { response ->
            println("gemini-3.5-flash v1beta Code: ${response.code}")
            println("Response: ${response.body?.string()}")
        }
    } catch (e: Exception) {
        println("Error on gemini-3.5-flash: ${e.message}")
    }
  }

  @Test
  fun testGeminiApiCall() = runBlocking {
    try {
        val key = System.getenv("GEMINI_API_KEY") ?: System.getenv("gemini_api_key") ?: ""
        println("\n=== TESTING PARSE RECEIPT === ")
        println("Using GEMINI_API_KEY from environment: length = ${key.length}")
        
        // Inject the key into BuildConfig.GEMINI_API_KEY reflectionally
        try {
            val field = com.example.BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            field.isAccessible = true
            
            // Remove final modifier
            try {
                val modifiersField = Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            } catch (e: Exception) {
                println("Could not override final modifier: ${e.message}")
            }
            
            field.set(null, key)
            println("Successfully set BuildConfig.GEMINI_API_KEY reflectionally")
        } catch (e: Exception) {
            println("Could not override BuildConfig via reflection: ${e.message}")
        }

        println("Is Key Configured: ${GeminiServiceClient.isKeyConfigured()}")
        println("BuildConfig.GEMINI_API_KEY is currently: ${com.example.BuildConfig.GEMINI_API_KEY}")
        val res = GeminiServiceClient.parseReceiptText("ESSELUNGA\nMELE FUJI €2.50\nTOTALE €2.50")
        println("Result of parseReceiptText: $res")
        println("Last Error details: ${GeminiServiceClient.lastApiError}")
    } catch (e: Throwable) {
        println("Outer test execution exception:")
        e.printStackTrace()
        throw e
    }
  }
}
