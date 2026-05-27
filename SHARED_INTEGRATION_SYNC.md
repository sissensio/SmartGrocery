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
| **2026-05-27 09:35** | Antigravity | `README.md`, `.env`, `backend/*` | **Ollama Multi-Model Support & Startup Optimization**: Updated backend to dynamically load any local Ollama model (supporting Qwen, Llama, Phi, etc.). Replaced verbose Llama prompt with a compact Gemini-optimized prompt for Qwen/Llama3.2/Phi. Bypassed Windows httpx async SSL bug with a robust thread-based urllib implementation. Added `OLLAMA_HOST`, `OLLAMA_NUM_THREADS`, and `OLLAMA_PRELOAD_ON_STARTUP` configuration options. | ✅ Sincronizzato & Pushed |


---

## 🎯 4. Prossimi Passi Coordinati & Regole di Sincronizzazione

### ⚠️ Regole di Sincronizzazione
*   **Ambito AI Studio**: AI Studio opera esclusivamente sul repository `SmartGrocery` per l'applicazione Android. Non deve tentare di ricreare file di backend o script Python in questo repository.
*   **Conservazione `network_config.json`**: AI Studio non deve modificare o eliminare `network_config.json` presente alla radice del repository frontend, in quanto esso contiene l'IP dinamico aggiornato da Antigravity per la connettività del client.

### 🚀 Prossimo Passo per Antigravity (Backend Workspace)
Con il repository diviso e pulito, iniziamo lo sviluppo delle logiche intelligenti all'interno di `SmartGrocery-Backend`:
1.  **Geometric OCR Parser (`backend/services.py`)**:
    *   Sviluppare la logica per raggruppare i riquadri di testo (`boundingBox`) in base alla coordinata $Y$ (altezza) per ricostruire le righe originali degli scontrini.
    *   Questo assicurerà che le coppie "Prodotto - Prezzo" rimangano associate correttamente prima di alimentare il prompt di estrazione.
2.  **Integrazione Llama 3**:
    *   Configurare il prompt del modello per processare il testo strutturato estratto e generare l'anagrafica prodotti in output JSON pulito.
3.  **Client Update**:
    *   Successivamente integreremo la chiamata del client `LocalBackendService` verso il backend locale invece che alle API Cloud esterne per completare il cerchio offline/LAN.
