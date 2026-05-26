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
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// --- Local Parsing result models for Moshi ---
@JsonClass(generateAdapter = true)
data class ParsedItem(
    @Json(name = "name") val name: String,
    @Json(name = "brand") val brand: String = "",
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "unitPrice") val unitPrice: Double = 0.0,
    @Json(name = "category") val category: String = "Dispensa",
    @Json(name = "isShared") val isShared: Boolean = true,
    @Json(name = "barcode") val barcode: String = "",
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "pricePerKg") val pricePerKg: Double? = null,
    @Json(name = "confidence") val confidence: Double = 0.95
)

@JsonClass(generateAdapter = true)
data class ParsingReceiptResult(
    @Json(name = "storeName") val storeName: String,
    @Json(name = "items") val items: List<ParsedItem>,
    @Json(name = "totalAmount") val totalAmount: Double,
    @Json(name = "vatNumber") val vatNumber: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "receiptDate") val receiptDate: String? = null, // format "YYYY-MM-DD" or similar
    @Json(name = "receiptTime") val receiptTime: String? = null // format "HH:mm" or similar
)

// --- Robust intermediary JSON classes to handle nullable and missing values from Gemini API resiliently ---
@JsonClass(generateAdapter = true)
data class ParsedItemJson(
    @Json(name = "name") val name: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "unitPrice") val unitPrice: Double? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "isShared") val isShared: Boolean? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "pricePerKg") val pricePerKg: Double? = null,
    @Json(name = "confidence") val confidence: Double? = null
) {
    fun toParsedItem() = ParsedItem(
        name = name ?: "Articolo",
        brand = brand ?: "",
        price = price ?: 0.0,
        unitPrice = unitPrice ?: price ?: 0.0,
        category = category ?: "Dispensa",
        isShared = isShared ?: true,
        barcode = barcode ?: "",
        weight = weight,
        pricePerKg = pricePerKg,
        confidence = confidence ?: 0.95
    )
}

@JsonClass(generateAdapter = true)
data class ParsingReceiptResultJson(
    @Json(name = "storeName") val storeName: String? = null,
    @Json(name = "items") val items: List<ParsedItemJson>? = null,
    @Json(name = "totalAmount") val totalAmount: Double? = null,
    @Json(name = "vatNumber") val vatNumber: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "receiptDate") val receiptDate: String? = null,
    @Json(name = "receiptTime") val receiptTime: String? = null
) {
    fun toParsingReceiptResult(): ParsingReceiptResult {
        val mappedItems = items?.map { it.toParsedItem() } ?: emptyList()
        return ParsingReceiptResult(
            storeName = storeName ?: "Supermercato",
            items = mappedItems,
            totalAmount = totalAmount ?: mappedItems.sumOf { it.price },
            vatNumber = vatNumber,
            address = address,
            phone = phone,
            receiptDate = receiptDate,
            receiptTime = receiptTime
        )
    }
}

object GeminiServiceClient {
    private const val TAG = "GeminiServiceClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
    private val LOCAL_BACKEND_URL = "http://${BuildConfig.LOCAL_BACKEND_IP}:8000/api/v1/scan"
    
    var useLocalBackend: Boolean = true
    var lastApiError: String? = null

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the Gemini API key is validly configured.
     */
    fun isKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "Checking GEMINI_API_KEY: length=${key?.length ?: 0}, prefix=${key?.take(5) ?: "null"}")
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY" && key != "placeholder"
    }

    /**
     * Sends raw OCR receipt rows or shelf label text to Gemini and gets a structured clean JSON response.
     */
    suspend fun parseReceiptText(ocrText: String): ParsingReceiptResult? = withContext(Dispatchers.IO) {
        lastApiError = null

        // Try local FastAPI backend first if enabled
        if (useLocalBackend) {
            try {
                Log.d(TAG, "Attempting local backend scan parsing: $LOCAL_BACKEND_URL")
                val scanMap = mapOf("ocrText" to ocrText)
                val jsonPayload = moshi.adapter(Map::class.java).toJson(scanMap)
                val request = Request.Builder()
                    .url(LOCAL_BACKEND_URL)
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawResponse = response.body?.string()
                        if (rawResponse != null) {
                            val resultAdapter = moshi.adapter(ParsingReceiptResultJson::class.java)
                            val parsed = resultAdapter.fromJson(rawResponse)?.toParsingReceiptResult()
                            if (parsed != null) {
                                Log.i(TAG, "Successfully parsed OCR text via local backend API.")
                                return@withContext parsed
                            }
                        }
                    } else {
                        Log.w(TAG, "Local backend returned error status: ${response.code} ${response.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Local backend connection failed: ${e.localizedMessage}. Falling back to Gemini Cloud API.")
            }
        }

        // Gemini Cloud API Fallback Pipeline
        if (!isKeyConfigured()) {
            Log.w(TAG, "Gemini API key is missing or is set to placeholder value. Falling back to local pattern parser.")
            lastApiError = "Chiave non configurata o valore placeholder."
            return@withContext null
        }

        val systemPrompt = """
            Sei l'assistente OCR intelligente di SmartGrocery Manager per i supermercati e negozi Italiani (9Agrifarm, Lidl, Coop, Esselunga, Conad, Carrefour, etc).
            Prendi il testo grezzo e confuso estratto da uno scontrino o da un'etichetta di scaffale, interpretalo con estrema precisione e restituisci un JSON pulito e ordinato.

            REGOLA FONDAMENTALE STRUTTURALE (SCONTRINI ORTOFRUTTA / AGRICOLI - ESEMPIO 9AGRIFARM):
            In molti scontrini per frutta e verdura o scontrini agricoli, le informazioni di ciascun articolo sono divise su più righe consecutive di testo:
            - Riga A (Intestazione articolo): Nome stampato in lettere maiuscole da solo (es: 'ZUCCHINE', 'BIETA DA TAGLIO', 'INSALATA MIX', 'PORRI', 'CAPUCCIO', 'CIPOLLE MIX').
            - Riga B o righe successive (Misure/Dettagli della pesata): Contengono SOLO dati numerici relativi alla pesata, formattati in tre colonne separate, ad esempio:
              '1,348  4,50  6,07'
              Significato preciso: Peso in kg (1.348), Prezzo al kg (4.50), Importo totale (6.07).
              Questi dettagli numerici APPARTENGONO ALL'ARTICOLO STAMPATO SULLA RIGA IMMEDIATAMENTE SOPRA (in questo caso 'Zucchine').

            GESTIONE BATTUTE CONSECUTIVE DELLO STESSO ARTICOLO (MOLTO IMPORTANTE):
            Se sotto una riga di testo dell'articolo (es: 'ZUCCHINE' o 'CAPUCCIO') ci sono DUE o più righe numeriche consecutive di pesate (es:
            'ZUCCHINE'
            '1,348  4,50  6,07'
            '1,156  2,50  2,89'),
            significa che sono state effettuate due pesate distinte dello STESSO articolo 'Zucchine' (una da 1.348 kg a 4.50 €/kg per un totale di 6.07 € e una da 1.156 kg a 2.50 €/kg per un totale di 2.89 €).
            In questo caso, DEVI creare DUE elementi separati nell'array 'items' (uno con nome 'Zucchine' per la prima pesata da 6.07 € e uno con nome 'Zucchine' per la seconda pesata da 2.89 €).
            NON associare MAI la seconda riga numerica con l'articolo successivo nell'elenco! Questo causerebbe un errore a catena sfasando tutti quanti i prodotti successivi dello scontrino.

            RILEVAMENTO DETTAGLI NEGOZIO (INSEGNA ED ESTREMI FISCALI):
            - Estrai il nome esatto dell'insegna in cima allo scontrino (es: '9AGRIFARM SOC AGRICOLA' o '9AGRIFARM' diventa '9Agrifarm Soc Agricola'). Non storpiare i caratteri iniziali!
            - Rileva la Partita IVA d'impresa (numero di 11 cifre, spesso preceduto da P.IVA, Partita IVA, PI o simile).
            - Rileva l'indirizzo del negozio (se rilevabile) e il numero di telefono del negozio (se rilevabile).
            - Rileva la data effettiva dello scontrino scritta nel testo (es. "22 05 2026" o "22/05/2026" o "22-05-2026") e decodificala in formato "YYYY-MM-DD" (es: "2026-05-22"). Se non la trovi, lascia null.
            - Rileva l'ora dello scontrino scritta nel testo (es. "07 39" o "07:39" o "07:39:12") e decodificala in formato "HH:mm" (es: "07:39"). Se non la trovi, lascia null.

            RICEVUTE DI ORTOFRUTTA / AGRICOLI:
            Se gli articoli appartengono a verdure, piante, ortaggi o frutti (es. zucchine, bieta, porri, insalata, cappuccio/cavolo, cipolle), imposta SEMPRE come categoria 'Frutta e Verdura'.

            CORREZIONE ERRORE OCR SPECIALE:
            Gli scanner OCR convertono spesso il prezzo "5,92" in "5,32". Se vedi nel testo "5,32" o simile, valuta se fa pensare a questo errore o se la confidence dev'essere impostata a 0.45 per far scattare l'alert visivo.

            RESTITUISCI UN JSON VALIDO CHE RISPETTI ESATTAMENTE QUESTO SCHEMA:
            {
              "storeName": "Nome Supermercato (es: 9Agrifarm Soc Agricola)",
              "vatNumber": "Partita IVA (11 cifre) o null",
              "address": "Indirizzo del negozio o null",
              "phone": "Numero di telefono del negozio o null",
              "receiptDate": "Data scontrino in formato YYYY-MM-DD o null",
              "receiptTime": "Ora scontrino in formato HH:mm o null",
              "items": [
                {
                  "name": "Nome Prodotto Pulito (es: Zucchine, Bieta da Taglio, Porri, Cavolo Cappuccio, Cipolle Mix)",
                  "brand": "Marca estratta o vuoto",
                  "price": 0.0, // Prezzo finale del prodotto come double (es: 6.07, 2.89)
                  "unitPrice": 0.0, // Prezzo unitario al kg se presente (es: 4.50, 2.50)
                  "category": "Una tra: Latticini, Dispensa, Frutta e Verdura, Macelleria, Bevande, Igiene e Casa, Colazione, Surgelati, Spuntini",
                  "isShared": true,
                  "barcode": "Un eventuale codice numerico associato se presente, altrimenti vuoto",
                  "weight": null, // peso in kg (Double) se dedotto (es: 1.348, 1.156) o null
                  "pricePerKg": null, // prezzo al kg (Double) se rilevato (es: 4.50, 2.50) o null
                  "confidence": 0.95
                }
              ],
              "totalAmount": 0.0 // Somma stampata/totale dello scontrino (es: 22.50)
            }
            Genera SOLO puro JSON. Nessun markdown block (come ```json) o testo introduttivo/conclusivo.
        """.trimIndent()

        val requestBodyMoshi = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = ocrText)))),
            generationConfig = GeminiConfig(
                responseMimeType = "application/json",
                temperature = 0.1
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        val adapter = moshi.adapter(GeminiRequest::class.java)
        val jsonRequest = adapter.toJson(requestBodyMoshi)

        val url = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"
        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    lastApiError = "HTTP ${response.code}: $errBody"
                    Log.e(TAG, "HTTP Error: ${response.code} ${response.message} - $errBody")
                    return@withContext null
                }
                val rawResponse = response.body?.string() ?: return@withContext null
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val geminiRes = responseAdapter.fromJson(rawResponse)
                
                val resultText = geminiRes?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (resultText == null) {
                    lastApiError = "La risposta Gemini non contiene candidati di testo validi. Risposta grezza: $rawResponse"
                    return@withContext null
                }
                
                Log.d(TAG, "Gemini parsing output: $resultText")

                val resultAdapter = moshi.adapter(ParsingReceiptResultJson::class.java)
                return@withContext resultAdapter.fromJson(resultText)?.toParsingReceiptResult()
            }
        } catch (e: IOException) {
            lastApiError = "Errore di rete (IOException): ${e.localizedMessage}"
            Log.e(TAG, "Network exception during Gemini API call", e)
            return@withContext null
        } catch (e: Exception) {
            lastApiError = "Errore di elaborazione/parsing: ${e.localizedMessage}"
            Log.e(TAG, "JSON parsing error on Gemini response", e)
            return@withContext null
        }
    }
}
