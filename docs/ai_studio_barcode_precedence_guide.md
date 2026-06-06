# Handover Guide: Real Manufacturer Barcode Precedence over Internal Store Barcodes

This guide outlines the changes required in `GroceryViewModel.kt` to ensure that when scanning shelf labels, a real manufacturer EAN-13 barcode parsed from the OCR text (via heuristics, backend, or Gemini) takes precedence over an internal store barcode (starting with local prefixes `20-29`) read by the graphical barcode scanner.

---

## 🛠️ Proposed Changes

### Component: Android Application Frontend

#### [MODIFY] [GroceryViewModel.kt](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/app/src/main/java/com/example/ui/viewmodel/GroceryViewModel.kt)

Update the `processShelfLabelScan(ocrRawText: String, barcodeFromScanner: String?)` method to:
1. **Identify Internal Barcodes**: Check if a barcode is an internal store barcode (starts with local prefixes `20-29` under the GS1 prefix rules).
2. **Identify Real Barcodes**: Check if a barcode is a valid manufacturer EAN-13 barcode (matches `\d{8,13}` and does not start with the local prefixes `20-29`).
3. **Prefer Real Barcode**: If the scanner reads an internal barcode but the OCR parses a real manufacturer barcode, use the real barcode for downstream lookup (Open Food Facts) and result saving.
4. **Prevent Overwriting**: Avoid overwriting a real barcode parsed by downstream services (FastAPI/Gemini) with an internal barcode from the scanner at the end of the processing flow.

---

### Implementation Details

Replace the current implementation of `processShelfLabelScan` with the following corrected logic:

```kotlin
    fun processShelfLabelScan(ocrRawText: String, barcodeFromScanner: String?) {
        viewModelScope.launch {
            isProcessingShelfScan.value = true
            try {
                val localItem = parseShelfLabelHeuristicsFallback(ocrRawText)
                
                // Verifichiamo se il barcode dello scanner è un codice interno locale (prefissi 20-29)
                val isScannerBarcodeInternal = barcodeFromScanner != null && 
                        barcodeFromScanner.startsWith("2") && 
                        barcodeFromScanner.length >= 2 && 
                        barcodeFromScanner[1] in '0'..'9'
                
                // Verifichiamo se l'OCR ha rilevato un barcode di fabbrica reale
                val isOcrBarcodeReal = localItem.barcode.matches(Regex("""\d{8,13}""")) && 
                        !(localItem.barcode.startsWith("2") && 
                          localItem.barcode.length >= 2 && 
                          localItem.barcode[1] in '0'..'9')

                var finalBarcode = barcodeFromScanner?.trim() ?: ""
                
                // Precedenza al codice reale di fabbrica se lo scanner ha letto quello interno
                if (isScannerBarcodeInternal && isOcrBarcodeReal) {
                    finalBarcode = localItem.barcode
                } else if (finalBarcode.isBlank()) {
                    finalBarcode = localItem.barcode
                }

                var parsedItem: com.example.api.CatalogItemCreate? = null

                // 1. Priorità: Interrogazione diretta a Open Food Facts per barcode "reali"
                val isInternalBarcode = finalBarcode.startsWith("2") && finalBarcode.length >= 2 && finalBarcode[1] in '0'..'9'
                val isValidEan = finalBarcode.isNotEmpty() && finalBarcode.matches(Regex("""\d{8,13}""")) && !isInternalBarcode
                if (isValidEan) {
                    try {
                        val offItem = com.example.api.OffServiceClient.getProductDetails(finalBarcode)
                        if (offItem != null) {
                            // Uniamo i dati ad alta precisione di OFF con i prezzi e sconti reali letti dall'OCR locale
                            parsedItem = offItem.copy(
                                price = localItem.price,
                                unitPrice = localItem.unitPrice,
                                discountLabel = localItem.discountLabel ?: offItem.discountLabel,
                                storeName = localItem.storeName ?: offItem.storeName
                            )
                        }
                    } catch (e: Exception) {
                        // OFF fallback silente alla chiamata successiva
                    }
                }

                // 2. Controllo e routing: se non trovato in OFF, interroghiamo backend o Gemini
                if (parsedItem == null) {
                    val useLocalServer = com.example.BuildConfig.SEND_OCR_TO_BACKEND
                    if (useLocalServer) {
                        // Verifica preventiva di connessione/ping rapido al backend locale (timeout a 800ms)
                        val isBackendAlive = com.example.api.LocalBackendServiceClient.pingBackend()
                        if (isBackendAlive) {
                            parsedItem = com.example.api.LocalBackendServiceClient.parseShelfLabel(ocrRawText)
                        }
                    } else {
                        parsedItem = com.example.api.GeminiServiceClient.parseShelfLabelText(ocrRawText)
                    }
                }

                // 3. Fallback finale offline: se tutti i servizi falliscono o non sono raggiungibili, usa euristiche locali
                if (parsedItem == null) {
                    parsedItem = localItem
                }

                // Copiamo finalBarcode nel risultato finale solo se finalBarcode è reale o se parsedItem non contiene già un barcode reale
                if (!finalBarcode.isNullOrBlank()) {
                    val isFinalInternal = finalBarcode.startsWith("2") && finalBarcode.length >= 2 && finalBarcode[1] in '0'..'9'
                    val isParsedReal = parsedItem.barcode.matches(Regex("""\d{8,13}""")) && 
                            !(parsedItem.barcode.startsWith("2") && 
                              parsedItem.barcode.length >= 2 && 
                              parsedItem.barcode[1] in '0'..'9')
                    
                    if (!(isFinalInternal && isParsedReal)) {
                        parsedItem = parsedItem.copy(barcode = finalBarcode)
                    }
                }

                parsedShelfLabelScanResult.value = parsedItem
            } catch (e: Exception) {
                var currentHeuristic = parseShelfLabelHeuristicsFallback(ocrRawText)
                val isScannerInternal = barcodeFromScanner != null && 
                        barcodeFromScanner.startsWith("2") && 
                        barcodeFromScanner.length >= 2 && 
                        barcodeFromScanner[1] in '0'..'9'
                val isHeuristicReal = currentHeuristic.barcode.matches(Regex("""\d{8,13}""")) && 
                        !(currentHeuristic.barcode.startsWith("2") && 
                          currentHeuristic.barcode.length >= 2 && 
                          currentHeuristic.barcode[1] in '0'..'9')
                
                var fallbackBarcode = barcodeFromScanner?.trim() ?: ""
                if (isScannerInternal && isHeuristicReal) {
                    // Mantiene currentHeuristic.barcode reale estratto dall'OCR
                } else if (fallbackBarcode.isNotBlank()) {
                    currentHeuristic = currentHeuristic.copy(barcode = fallbackBarcode)
                }
                parsedShelfLabelScanResult.value = currentHeuristic
            } finally {
                isProcessingShelfScan.value = false
            }
        }
    }
```

---

## 🧪 Verification Plan

### Test Case: Store Internal Barcode with Real Manufacturer OCR Barcode
1. Input:
   - `ocrRawText` contains:
     ```text
     Esselunga SpA
     PASTA DI SEMOLA SPAGHETTI N.5 BARILLA 500G
     € 1,15
     8002270014901
     ```
   - `barcodeFromScanner` = `"2000043824906"` (L'EAN interno stampato sul cartellino dello scaffale)
2. Expected Behavior:
   - `localItem.barcode` is parsed as `"8002270014901"` (EAN reale di fabbrica Barilla).
   - `isScannerBarcodeInternal` is evaluated as `true`.
   - `isOcrBarcodeReal` is evaluated as `true`.
   - `finalBarcode` is resolved to `"8002270014901"`.
   - The app queries Open Food Facts with EAN `"8002270014901"`, successfully retrieves the product details (Spaghetti Barilla N.5), and stores the real EAN-13 in the catalog.
