# Guida di Sviluppo per AI Studio: Rilevazione Offline e Fallback Open Food Facts (OFF)

Ciao AI Studio! In qualità di Project Manager del backend e coordinatore del progetto, ho preparato per te le specifiche e i blocchi di codice da implementare nel client Android.

L'obiettivo è rendere la scansione dei cartellini dei prezzi resiliente alle disconnessioni del server locale e consentire l'interrogazione diretta a Open Food Facts (OFF) per i codici a barre reali.

---

## 🛠️ Cosa Devi Fare

Dovrai effettuare due modifiche principali nel codice dell'app:
1. Aggiungere il metodo di ping rapido e il client OFF in `LocalBackendService.kt`.
2. Aggiornare il flusso di smistamento (routing) in `GroceryViewModel.kt`.

---

## 📝 1. Modifiche in `LocalBackendService.kt`

Apri il file `app/src/main/java/com/example/api/LocalBackendService.kt` ed effettua le seguenti aggiunte:

### A. Aggiungi il metodo `pingBackend`
All'interno del `LocalBackendServiceClient` object (subito prima della chiusura `}` dell'oggetto), inserisci questo metodo per effettuare una connessione socket rapida con timeout di 800ms:

```kotlin
    suspend fun pingBackend(): Boolean = withContext(Dispatchers.IO) {
        if (!isHostConfigured()) return@withContext false
        try {
            val socket = java.net.Socket()
            val ip = BuildConfig.LOCAL_BACKEND_IP
            socket.connect(java.net.InetSocketAddress(ip, 8000), 800)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
```

### B. Definisci l'oggetto `OffServiceClient`
In fondo al file `LocalBackendService.kt` (fuori da `LocalBackendServiceClient`), definisci questo nuovo client per interrogare direttamente le API di Open Food Facts ed estrarre i dettagli del prodotto:

```kotlin
object OffServiceClient {
    private const val TAG = "OffServiceClient"

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun queryProductFromOff(barcode: String): CatalogItemCreate? = withContext(Dispatchers.IO) {
        if (barcode.isBlank()) return@withContext null
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SmartGrocery - Android App - Version 1.0")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    Log.w(TAG, "OFF rate limit exceeded (429)")
                    return@withContext null
                }
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null

                val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val mapAdapter = moshi.adapter<Map<String, Any>>(type)
                val resMap = mapAdapter.fromJson(bodyStr) ?: return@withContext null

                val statusVal = resMap["status"]
                val status = when (statusVal) {
                    is Double -> statusVal.toInt()
                    is String -> statusVal.toIntOrNull() ?: 0
                    else -> 0
                }
                if (status != 1) return@withContext null

                val product = resMap["product"] as? Map<*, *> ?: return@withContext null
                val name = product["product_name"] as? String ?: product["product_name_it"] as? String ?: "Prodotto OFF"
                val brand = product["brands"] as? String ?: "Generico"
                val category = (product["categories"] as? String)?.split(",")?.firstOrNull()?.trim() ?: "Dispensa"

                val weightStr = product["quantity"] as? String
                var weightVal: Double? = null
                if (!weightStr.isNullOrBlank()) {
                    val matchWt = Regex("""(\d+[,.]?\d*)\s*(g|kg|l|ml)""").find(weightStr.lowercase())
                    if (matchWt != null) {
                        val valDouble = matchWt.groupValues[1].replace(",", ".").toDoubleOrNull()
                        if (valDouble != null) {
                            val unit = matchWt.groupValues[2]
                            weightVal = if (unit == "g" || unit == "ml") valDouble / 1000.0 else valDouble
                        }
                    }
                }

                return@withContext CatalogItemCreate(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    category = category,
                    price = null,
                    unitPrice = null,
                    weight = weightVal,
                    discountLabel = null,
                    storeName = null,
                    vatNumber = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying OFF directly", e)
            return@withContext null
        }
    }
}
```

---

## 🔀 2. Modifiche in `GroceryViewModel.kt`

Apri il file `app/src/main/java/com/example/ui/viewmodel/GroceryViewModel.kt` ed aggiorna la funzione `processShelfLabelScan` per implementare il flusso di smistamento logico dei codici:

```kotlin
    fun processShelfLabelScan(ocrRawText: String, barcodeFromScanner: String?) {
        viewModelScope.launch {
            isProcessingShelfScan.value = true
            try {
                // 1. Estrae il codice a barre attivo dallo scanner o dal testo OCR
                var activeBarcode = barcodeFromScanner ?: ""
                if (activeBarcode.isBlank()) {
                    val lines = ocrRawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val barcodePattern = Regex("""\b\d{8,13}\b""")
                    for (line in lines) {
                        val matchBar = barcodePattern.find(line)
                        if (matchBar != null) {
                            activeBarcode = matchBar.value
                            break
                        } else if (line.contains("*")) {
                            Regex("""\*(\d+)\*""").find(line)?.let {
                                activeBarcode = it.groupValues[1]
                            }
                            if (activeBarcode.isNotBlank()) break
                        }
                    }
                }

                val cleanBarcode = activeBarcode.trim()
                
                // 2. Verifica se è un EAN reale (non vuoto e non inizia con prefissi locali 20-29)
                val isRealBarcode = cleanBarcode.length >= 8 && !(
                    cleanBarcode.startsWith("20") || cleanBarcode.startsWith("21") || cleanBarcode.startsWith("22") ||
                    cleanBarcode.startsWith("23") || cleanBarcode.startsWith("24") || cleanBarcode.startsWith("25") ||
                    cleanBarcode.startsWith("26") || cleanBarcode.startsWith("27") || cleanBarcode.startsWith("28") ||
                    cleanBarcode.startsWith("29")
                )

                var parsedItem: com.example.api.CatalogItemCreate? = null

                // 3. Se il codice è reale, prova a interrogare prima Open Food Facts direttamente dall'app
                if (isRealBarcode) {
                    val offProduct = com.example.api.OffServiceClient.queryProductFromOff(cleanBarcode)
                    if (offProduct != null) {
                        // Unisce le anagrafiche OFF con i prezzi ricavati dall'OCR tramite euristica
                        val localHeuristics = parseShelfLabelHeuristicsFallback(ocrRawText)
                        parsedItem = offProduct.copy(
                            price = localHeuristics.price,
                            unitPrice = localHeuristics.unitPrice,
                            discountLabel = localHeuristics.discountLabel,
                            storeName = localHeuristics.storeName
                        )
                    }
                }

                // 4. Fallback al backend se la ricerca OFF non è andata a buon fine (o è stata saltata)
                if (parsedItem == null) {
                    val isOnline = com.example.api.LocalBackendServiceClient.pingBackend()
                    if (isOnline) {
                        val useLocalServer = com.example.BuildConfig.SEND_OCR_TO_BACKEND
                        if (useLocalServer) {
                            parsedItem = com.example.api.LocalBackendServiceClient.parseShelfLabel(ocrRawText)
                        } else {
                            parsedItem = com.example.api.GeminiServiceClient.parseShelfLabelText(ocrRawText)
                        }
                    }
                }

                // 5. Fallback finale all'euristica pura locale (in caso di server offline o errori di rete)
                if (parsedItem == null) {
                    parsedItem = parseShelfLabelHeuristicsFallback(ocrRawText)
                }

                // Assicura che il barcode sia popolato
                if (parsedItem.barcode.isBlank() && cleanBarcode.isNotBlank()) {
                    parsedItem = parsedItem.copy(barcode = cleanBarcode)
                }

                parsedShelfLabelScanResult.value = parsedItem
            } catch (e: Exception) {
                var currentHeuristic = parseShelfLabelHeuristicsFallback(ocrRawText)
                val cleanBarcode = (barcodeFromScanner ?: "").trim()
                if (cleanBarcode.isNotBlank()) {
                    currentHeuristic = currentHeuristic.copy(barcode = cleanBarcode)
                }
                parsedShelfLabelScanResult.value = currentHeuristic
            } finally {
                isProcessingShelfScan.value = false
            }
        }
    }
```

Una volta terminata la scrittura, compila l'applicazione per verificare la correttezza del codice ed esegui i test manuali. Buon lavoro!
