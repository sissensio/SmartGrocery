# Shared Integration Sync: AI Studio & Antigravity (Local Workspace) 🤝🔄

Questo documento funge da registro di sincronizzazione e protocollo operativo tra **AI Studio** (l'ambiente di sviluppo in cloud/streaming emulation dell'applicazione Android) e **Google Antigravity** (il sistema che gestisce il workspace locale e l'integrazione backend).

L'obiettivo è mantenere l'assoluto allineamento sulle evoluzioni dell'architettura client-server, della base di codice e delle logiche di business dell'app.

---

## 🛰️ 1. Architettura a Doppio Repository (Repository Split)

Per prevenire conflitti di sincronizzazione e sovrascritture accidentali da parte dei meccanismi di cache cloud di AI Studio, il progetto è stato definitivamente diviso in **due repository indipendenti**:

1.  **Frontend (Android Client)**:
    *   **Repository GitHub**: [SmartGrocery](https://github.com/sissensio/SmartGrocery)
    *   **Tecnologie**: Kotlin, Jetpack Compose, Material 3, Room Database, Google ML Kit OCR.
    *   **Sito di Lavoro (AI Studio)**: Opera esclusivamente all'interno di questo workspace. Non vede e non può interferire con i file del backend.
    *   **File di Configurazione Chiave**: `network_config.json` alla radice, che contiene l'indirizzo IP del server locale ed è letto da Gradle per iniettare l'IP in `BuildConfig.LOCAL_BACKEND_IP`.

2.  **Backend (Python FastAPI Server & AI Inference)**:
    *   **Repository GitHub**: [SmartGrocery-Backend](https://github.com/sissensio/SmartGrocery-Backend)
    *   **Tecnologie**: Python, FastAPI, Uvicorn, SQLAlchemy (SQLite), Llama 3 (via Ollama o libreria locale).
    *   **Gestione Antigravity (Local IDE)**: Gestito localmente da Antigravity, protetto da qualsiasi operazione cloud.

---

## 🔀 2. Protocollo di Sincronizzazione Dynamic IP (Dual-Sync IP)

Poiché il backend gira in locale sulla rete LAN e l'emulatore/dispositivo Android ha bisogno di connettersi ad esso, è cruciale mantenere aggiornato l'indirizzo IP locale nel client.

Il flusso è automatizzato tramite lo script `update_ip.py` posizionato nel repository del backend:
1.  **Rilevamento automatico**: Lo script identifica l'IP LAN attivo del PC host (es. `192.168.0.101`).
2.  **Scrittura Duale**: Aggiorna simultaneamente il file `network_config.json` in entrambi i workspace locali (`SmartGrocery` e `SmartGrocery Backend`).
3.  **Push Automatico**: Esegue automaticamente uno staging, commit e push del file `network_config.json` sul repository frontend, assicurando che AI Studio riceva istantaneamente l'IP corretto al primo `pull` o allineamento.

---

## 📝 3. Registro delle Modifiche Reciproche (Changelog di Allineamento)

Ogni agente compila questa tabella dopo modifiche rilevanti per evitare regressioni o disallineamenti di contratto d'interfaccia (API).

| Orario (UTC) / Data | Agente Autore | Componente Modificato | Dettaglio e Motivazione (Cosa, Perché, Come) | Stato Integrazione |
| :--- | :--- | :--- | :--- | :--- |
| **2026-05-24 23:40** | AI Studio | `GroceryViewModel.kt` & `ScannerScreen.kt` | Corretto bug del parsing del testo e migliorata la logica di riconciliazione degli scontrini duplicati. Ora l'utente visualizza l'elenco degli articoli scansionati solo se sono differenti da quelli della transazione esistente per unire i prodotti. | ✅ Compilato e funzionante |
| **2026-05-25 08:35** | AI Studio | `V4Pro_Master_Document.md` & `README.md` | Aggiornata la documentazione generale per integrare la specifica del backend Python FastAPI e il modello Llama 3 che riceverà JSON OCR spaziali al posto delle immagini JPG/PNG pesanti. | ✅ Salvato nel Repo |
| **2026-05-25 10:20** | AI Studio | `SHARED_INTEGRATION_SYNC.md` | **Creazione del Documento**. Definizione del protocollo di collaborazione e comunicazione inter-agente per garantire allineamento assoluto durante l'evoluzione ad architettura ibrida. | ✅ Sincronizzato |
| **2026-05-26 07:30** | AI Studio | `SHARED_INTEGRATION_SYNC.md` | Aggiornato il piano di lavoro e formalizzate le regole di conservazione dei file extra-Android creati da Antigravity.| ✅ Sincronizzato |
| **2026-05-26 09:40** | Antigravity | `SHARED_INTEGRATION_SYNC.md` & Repo Structure | **Repository Split**. Il codice backend è stato spostato in un repository separato (`SmartGrocery-Backend`). Rimosso ogni file Python e Gradle extra dal repository Android frontend per prevenire conflitti con AI Studio. Configurato script dynamic IP `update_ip.py`. | ✅ Sincronizzato & Pushed |
| **2026-05-26 10:25** | AI Studio | Full Workspace Alignment | **Eseguito Git Pull con successo**. Allineato il client di AI Studio all'ultimo commit del frontend con l'IP LAN (`192.168.0.101`) integrato in `network_config.json`, l'OCR spaziale biometrico e il client `LocalBackendService` nel modulo Android. | ✅ Allineato & Compilato |
| **2026-05-27 09:35** | Antigravity | `README.md`, `.env`, `backend/*` | **Ollama Multi-Model & DeepSeek-R1 Support**: Updated backend to dynamically load any local model (supporting Qwen, Llama, Phi, DeepSeek, etc.). Added native DeepSeek-R1 `<think>` block extraction, console logging, and JSON isolation. Bypassed Windows httpx async SSL bug with a robust thread-based urllib implementation. Added `OLLAMA_HOST`, `OLLAMA_NUM_THREADS`, and `OLLAMA_PRELOAD_ON_STARTUP` configuration options. | ✅ Sincronizzato & Pushed |
| **2026-05-27 12:30** | AI Studio | `GroceryViewModel.kt`, `ScannerScreen.kt`, `Database.kt`, `Repository.kt` | **OCR Validation & Deletion Integrity**: Added strict client-side evaluation of OCR text to reject non-receipt images (like desks, plain backgrounds) before doing any API/local server request. Completely prohibited and removed fictitious item fallbacks. When a LedgerEntry is deleted, associated items in the local Room database (`items` table) matching timestamp & store are deleted to prevent leftovers and duplicates on subsequent scans. Default integration option ("Sì, Integra") is preselected by default when a duplicate is found, while always keeping the list of scanned items visible and stable using custom Compose itemsIndexed keys. | ✅ Allineato & Compilato |
| **2026-05-27 16:00** | Antigravity | `backend/tests/*`, `backend/routers/*`, `backend/main.py`, `backend/schemas.py`, `SHARED_INTEGRATION_SYNC.md` | **Backend Phase Complete**: Implemented traditional/Google/biometric logins, automated store VAT resolution and name normalization, price history tracking with analytics (average, min/max, geo-stats), shrinkflation alerts, and advanced targeted notifications (broadcast, geo, store-specific) with offline sync support. Organized tests under `backend/tests/` package and created a unified test runner (`run_all_tests.py`) verifying 100% green and deterministic local flows. | ✅ Sincronizzato & Pushed |
| **2026-05-27 19:40** | AI Studio | `SyncNotificationAcksWorker.kt`, `GroceryViewModel.kt`, `LocalBackendService.kt`, `HomeScreen.kt`, `build.gradle.kts` | **Frontend Integration - Notifications & Limits**: Implementato `SyncNotificationAcksWorker` con Android WorkManager per gestire la sincronizzazione differita e in background degli acknowledge delle notifiche lettate in modalità offline verso il backend locale. Inserite chiamate di polling automatico e queue del worker all'avvio. Esteso `LocalBackendService` per includere correttamente l'header `X-Device-ID` e gestire lo state flow dell'app. Aggiunto `AlertDialog` in `HomeScreen` per la notifica tempestiva dei superamenti del limite transazione (HTTP 403). Risolti problemi di test (NPE nei Robolectric suites) anticipando le inizializzazioni dello stato offline. | ✅ Allineato & Compilato |
| **2026-05-27 19:55** | AI Studio | `network_config.json`, `BiometricKeyManager`, `GroceryViewModel.kt`, `GlobalSettingsScreen.kt`, `MainActivity.kt` | **IP Update & Biometric RSA Integration**: Aggiornato IP LAN a `192.168.111.101`. Completata implementazione del flusso 4.1 "Registrazione & Login Biometrico": aggiunto `androidx.biometric:biometric` al progetto, convertito `MainActivity` a `FragmentActivity`, aggiunto `.setUserAuthenticationRequired(true)` in `KeyGenParameterSpec` e implementato l'intero ciclo interattivo asincrono con `androidx.biometric.BiometricPrompt` nella UI (`GlobalSettingsScreen.kt`) per firmare in locale il challenge (nonce) del backend tramite chiavi RSA hardware sicure. | ✅ Allineato & Compilato |
| **2026-05-27 21:08** | AI Studio | `LocalBackendService.kt` | **Fix Payload 422 `submitLedgerEntry`**: Corretto JSON payload verso l'endpoint `POST /api/v1/ledger`. Sistemata incompatibilità del DTO `LedgerSubmitRequest` ("store_name" in "storeName" e "timestamp" in "date" stringata YYYY-MM-DD), per allineamento esatto allo schema Pydantic atteso. | ✅ Allineato & Compilato |
| **2026-05-28 10:23** | AI Studio & Antigravity | Full Integration Sync | **Integrazione Mobile & Backend Completata**: Cablato l'endpoint `PUT` per la de-duplica atomica. Relocate le opzioni sviluppatore, simulatori di geofence e preset OCR sotto il pannello di debug visibile solo per `role == ADMIN`. Attivate notifiche nativa OS con DeepLink per inquadrare scontrini in sospeso, e chiamate OkHttp per la telemetria (`/telemetry` per alert tempo reale coinquilini) e catalogazione prezzi a scaffale (`/shelflabel`). | ✅ Integrazione Completata |
| **2026-05-28 10:33** | AI Studio & Antigravity | Step 3: Active Sessions API | **Deciso Target Primario (Step 3)**: Proposta ed approvata l'architettura ibrida per visualizzare in tempo reale i coinquilini che stanno facendo la spesa. Implementato e testato lato backend l'endpoint `GET /api/v1/scan/active_sessions` che calcola dinamicamente il dwell time e pulisce le sessioni appese oltre le 2 ore. AI Studio procede alla realizzazione del banner animato premium nella HomeScreen con polling intelligente in foreground e trigger istantaneo al click della notifica push locale. | ✅ Sviluppo in Corso |

---

## 🎯 4. Prossimi Passi Coordinati & Contratto API per AI Studio (Frontend)

Con il backend ed i test d'unità completamente verificati, verdi e pushed su GitHub, **Google AI Studio** può procedere all'integrazione del client Android. Di seguito sono elencati i contratti degli endpoint definiti ed il flusso logico da implementare sul frontend.

### 🔑 4.1 Registrazione & Login Biometrico (Android Keystore API)
L'app Android deve implementare la crittografia asimmetrica RSA per consentire il login rapido tramite impronta digitale:
1. **Generazione Coppia Chiavi**:
   * Generare una chiave `RSA 2048` protetta tramite `AndroidKeyStore` con requisiti di autenticazione biometrica (es. `BiometricPrompt`).
   * Estrarre la chiave pubblica in formato **X.509 PEM** (Public Key).
2. **Registrazione della Chiave pubblica**:
   * Chiamare `POST /api/v1/auth/biometric/register` (Richiede Bearer Token JWT dell'utente autenticato tradizionalmente o via Google).
   * **Payload**:
     ```json
     {
       "device_uuid": "hardware_uuid_del_telefono",
       "public_key_pem": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
     }
     ```
3. **Flusso di Login Crittografico (Biometric Sign-In)**:
   * **Richiesta Sfida**: Richiedere un codice casuale al server chiamando `POST /api/v1/auth/biometric/challenge` inviando `{ "device_uuid": "..." }`. Riceverà un codice nonce esadecimale `{ "challenge": "nonce_esadecimale_32_byte" }`.
   * **Firma Locale**: Mostrare il popup `BiometricPrompt`. Una volta sbloccato dall'utente con l'impronta, firmare la stringa `challenge` (codificata in UTF-8 bytes) usando la chiave privata RSA locale con algoritmo di firma `SHA256withRSA`.
   * **Invio al Backend**: Codificare la firma in esadecimale e inviarla a `POST /api/v1/auth/biometric/login`:
     ```json
     {
       "device_uuid": "...",
       "challenge": "nonce_esadecimale_32_byte",
       "signature_hex": "firma_esadecimale_generata"
     }
     ```
   * **Risposta**: Riceverà il JWT token definitivo `{ "access_token": "...", "token_type": "bearer", "role": "USER" }` per le sessioni successive.

### 🔔 4.2 Notifiche Avanzate & Sincronizzazione Offline (Room & WorkManager)
Il backend supporta notifiche mirate in base alle abitudini d'acquisto dell'utente ed il tracciamento delle letture (Acknowledge) in modalità offline:
1. **Recupero Notifiche Non Lette**:
   * Chiamare `GET /api/v1/notifications/unread`.
   * **Headers**: `Authorization: Bearer <token>` e `X-Device-ID: <device_uuid>` (o come parametro query `?device_uuid=...`).
   * **Risposta**: Una lista di notifiche mirate per l'utente (che includono BROADCAST, notifiche geografiche della città in cui ha fatto spese, e notifiche specifiche dei supermercati in cui ha acquistato):
     ```json
     [
       {
         "id": 1,
         "title": "Sconto Speciale",
         "body": "Zucchine scontate al 10% da 9Agrifarm!",
         "type": "STORE_SPECIFIC",
         "target_store_id": 2,
         "target_city": null,
         "target_region": null,
         "created_at": "2026-05-27T18:00:00"
       }
     ]
     ```
2. **Conferma di Lettura (Acknowledge)**:
   * Quando l'utente visualizza o cancella una notifica, chiamare `POST /api/v1/notifications/{id}/ack` con payload `{ "device_uuid": "..." }`.
   * **Offline Mode (Room Cache)**: Se il telefono è offline, salvare la notifica letta nella tabella Room locale `notification_acks`. Impostare un `CoroutineWorker` con Android `WorkManager` (configurato con vincolo `NetworkType.CONNECTED`) che provveda a sincronizzare in batch le ricevute di ritorno pendenti non appena la connettività di rete viene ripristinata.

### 🛍️ 4.3 Registrazione Spesa (Ledger) & Limiti Dispositivo
Quando l'utente finalizza ed integra uno scontrino scansionato:
* Chiamare `POST /api/v1/ledger` inviando l'anagrafica negozio e la lista articoli.
* **Headers**: `Authorization: Bearer <token>` e `X-Device-ID: <device_uuid>`.
* Il server verificherà lo stato del dispositivo ed applicherà eventuali limiti di transazione ad-hoc configurati dall'amministratore (restituendo un HTTP 403 in caso di superamento limiti), registrando asincronamente lo storico dei prezzi degli articoli per analizzare l'andamento del mercato e calcolare gli alert di sgrammatura (Shrinkflation).

---

> [!TIP]
> **Consiglio per AI Studio (Frontend)**:
> Tutti gli endpoint sopra indicati sono pronti e auto-documentati nel pannello interattivo Swagger UI all'indirizzo `http://<LOCAL_BACKEND_IP>:8000/docs` una volta avviato il backend locale. AI Studio può avviare l'emulatore Android, effettuare il `git pull` per aggiornare `network_config.json` contenente l'IP dinamico locale del server, e avviare l'integrazione Kotlin con i router ed i contratti definiti.

### 🛍️ 4.4 Riconciliazione Avanzata Scontrini Duplicati (PUT)
Quando il client Android rileva e de-duplica gli articoli di uno scontrino (es. merge di una scansione POS ed uno scontrino cartaceo), deve inviare l'aggiornamento consolidato al server:
* **Endpoint**: `PUT /api/v1/ledger/{entry_id}`
* **Payload**: Lo stesso JSON di `LedgerCreate` (lista articoli puliti e de-duplicati in `items`, totale spesa corretto in `amount`).
* **Logica**: Il server eliminerà in modo atomico i vecchi prodotti relazionati da `ItemPriceHistory` per quella spesa e li sostituirà con il nuovo breakdown de-duplicato, ricalcolando le statistiche.

### 🏷️ 4.5 Scansione Avanzata Etichette Scaffale (ShelfLabel Catalog)
Il backend supporta la raccolta crowdsourced dei prezzi a scaffale per arricchire il database storico dei supermercati:
* **Endpoint**: `POST /api/v1/scan/shelflabel`
* **Headers**: `Authorization: Bearer <token>`
* **Payload (`CatalogItemCreate`)**:
```json
{
  "barcode": "8001234567890",
  "name": "Salsa Pronta di Ciliegino",
  "brand": "Mutti",
  "category": "Dispensa",
  "price": 1.89,
  "unitPrice": 5.40,
  "weight": 0.35,
  "discountLabel": "Sconto 15%",
  "storeName": "Coop",
  "vatNumber": "55566677788"
}
```

### 🛰️ 4.6 Telemetria Geofencing & Alert Coinquilini in Tempo Reale
Per tenere aggiornata la casa sulle abitudini di spesa dei coinquilini e permettere check-in immediati:
* **Endpoint**: `POST /api/v1/scan/telemetry`
* **Headers**: `Authorization: Bearer <token>`
* **Payload (`TelemetryEventCreate`)**:
```json
{
  "device_uuid": "string (UUID hardware)",
  "event_type": "CHECK_IN", // o "CHECK_OUT"
  "store_name": "Esselunga",
  "dwell_time_seconds": null // opzionale, valorizzato su CHECK_OUT
}
```
* **Comportamento Check-In**: Quando il client rileva l'ingresso in un geofence ed invia un evento `CHECK_IN`, il backend genera **automaticamente ed istantaneamente una notifica push Broadcast** per informare gli altri coinquilini: *"Spesa in Corso! 🛒 Mario Rossi è appena entrato da Esselunga! Ti serve qualcosa? Chiediglielo al volo!"*.

### 🛒 4.7 Polling di Stato: Sessioni di Spesa Attive in Tempo Reale
Per popolare il banner animato premium nella HomeScreen dei coinquilini e visualizzare chi sta facendo la spesa in questo momento:
* **Endpoint**: `GET /api/v1/scan/active_sessions`
* **Headers**: `Authorization: Bearer <token>`
* **Risposta JSON (`List[ActiveShoppingSessionResponse]`)**:
```json
[
  {
    "user_id": 1,
    "user_name": "Mario Rossi",
    "store_name": "Esselunga",
    "started_at": "2026-05-28T10:30:00.123Z",
    "dwell_time_seconds": 180
  }
]
```
* **Logica Server**: L'endpoint analizza l'ultimo evento telemetrico dei coinquilini dello stesso gruppo familiare (`household_id`). Se l'ultimo evento è un `CHECK_IN` ed è avvenuto nelle ultime 2 ore, restituisce la sessione calcolando dinamicamente il tempo di permanenza trascorso. Se l'utente esegue un `CHECK_OUT` o passano più di 2 ore, la sessione viene rimossa.
