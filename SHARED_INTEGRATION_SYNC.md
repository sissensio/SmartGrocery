# Shared Integration Sync: AI Studio & Antigravity (Local Workspace) 🤝🔄

Questo documento funge da registro di sincronizzazione e protocollo operativo tra **AI Studio** (l'ambiente di sviluppo in cloud/streaming emulation dell'applicazione Android) e **Google Antigravity** (il sistema che gestisce il workspace locale e l'eventuale integrazione backend).

L'obiettivo è mantenere l'assoluto allineamento sulle evoluzioni dell'architettura client-server, della base di codice e delle logiche di business dell'app.

---

## 🛰️ 1. Allineamento Tecnologico e Visione Condivisa

Entrambi gli agenti di sviluppo operano sul medesimo obiettivo di design a **Frizione Zero, Alta Privacy ed Efficienza Economica**:

1. **Client-Side (AI Studio)**: Sviluppo e rifinitura dell'app Android in Jetpack Compose, Material 3 e database Room locale.
   - *Stato Corrente*: Integrazione nativa **Google ML Kit OCR** per la scansione del testo in locale sul terminale. Parsing semantico deterministico temporaneo via API Cloud `gemini-3.5-flash` per convalidare il modello dei dati e la UX.
   - *Ottimizzazione UX*: Implementata una logica di deduplica e riconciliazione avanzata. Se un nuovo scontrino differisce in articoli da uno già registrato con la stessa data e importo (`hasDifferentItemsFromDuplicate == true`), l'interfaccia dell'app mostra all'utente gli elementi scansionati nel pannello inferiore per consentire l'unione manuale senza perdite informative.
2. **Server-Side (Local/Antigravity Integration)**:
   - *Prossimo Step*: Sviluppo del backend in **Python (FastAPI)** ad alte prestazioni.
   - *Inference Engine (Llama 3)*: Ricezione del JSON di coordinate spaziali dei blocchi di testo del client ed estrazione semantica a costo marginale zero.

---

## 🔀 2. Protocollo di Aggiornamento Bidirezionale (GitHub Bridge)

Siccome il progetto è collegato a un repository **GitHub**, la comunicazione delle modifiche tra i due ambienti avviene secondo i seguenti step:

1. **AI Studio (Client/UX/ML Kit Front-end)**:
   - Modifica l'app Android e valida la compilazione con il built-in compiler.
   - Aggiorna i file di documentazione (come `README.md`, `V4Pro_Master_Document.md` e questo file).
   - Esegue la sincronizzazione o il push sul ramo di sviluppo principale.
2. **Antigravity / Local IDE (Backend/Security/Local DB)**:
   - Esegue il `pull` delle ultime modifiche da GitHub prima di iniziare ogni sessione.
   - Implementa o modifica le parti relative al backend o a componenti correlati.
   - Aggiorna questo documento nella sezione **"Registro delle Modifiche Reciproche"** per notificare ad AI Studio quali file sono stati toccati e perché.

---

## 📝 3. Registro delle Modifiche Reciproche (Changelog di Allineamento)

Ogni agente compila questa tabella dopo modifiche rilevanti per evitare regressioni o disallineamenti di contratto d'interfaccia (API).

| Orario (UTC) / Data | Agente Autore | Componente Modificato | Dettaglio e Motivazione (Cosa, Perché, Come) | Stato Integrazione |
| :--- | :--- | :--- | :--- | :--- |
| **2026-05-24 23:40** | AI Studio | `GroceryViewModel.kt` & `ScannerScreen.kt` | Corretto bug del parsing del testo e migliorata la logica di riconciliazione degli scontrini duplicati. Ora l'utente visualizza l'elenco degli articoli scansionati solo se sono differenti da quelli della transazione esistente per unire i prodotti. | ✅ Compilato e funzionante |
| **2026-05-25 08:35** | AI Studio | `V4Pro_Master_Document.md` & `README.md` | Aggiornata la documentazione generale per integrare la specifica del backend Python FastAPI e il modello Llama 3 che riceverà JSON OCR spaziali al posto delle immagini JPG/PNG pesanti. | ✅ Salvato nel Repo |
| **2026-05-25 10:20** | AI Studio | `SHARED_INTEGRATION_SYNC.md` | **Creazione del Documento**. Definizione del protocollo di collaborazione e comunicazione inter-agente per garantire allineamento assoluto durante l'evoluzione ad architettura ibrida. | ✅ Sincronizzato |
| **2026-05-25 10:28** | Antigravity | `backend/` & `GeminiService.kt` | **Inizializzazione del Backend On-Premise** (FastAPI, SQLite, Ollama/Llama3, fallback Gemini server-side e dizionari di backup deterministici) e **redirezione dell'app Android** tramite `GeminiServiceClient` con meccanismo di failover/fallback automatico e trasparente se il server locale è offline. | ✅ Sincronizzato e pronto |
| **2026-05-25 10:35** | Antigravity | `network_config.json`, `update_ip.py`, `app/build.gradle.kts` | **Configurazione Condivisa dell'IP di Rete**. Introduzione del file `network_config.json` per memorizzare l'IP del backend in LAN. Scrittura dello script `update_ip.py` per autodiagnosticare l'IP corrente del PC, aggiornare il file JSON ed eseguire il commit/push automatico. Configurato Gradle per leggere l'IP a build-time ed iniettarlo in `BuildConfig.LOCAL_BACKEND_IP` mantenendo sicura la chiave in `.env` (ignorata da git). | ✅ Configurato e sincronizzato |
| **2026-05-26 09:07** | Antigravity | Radice Progetto | **Ripristino Struttura di Backend**. Eseguito il `revert` del commit `23e26c3` di AI Studio che aveva rimosso la cartella `backend/`, il Gradle wrapper e le impostazioni di IP locale, ristabilendo la piena compatibilità con lo sviluppo ibrido on-premise. | ✅ File ripristinati nel Repo |
| **2026-05-26 09:12** | Antigravity | `backend/` & `SHARED_INTEGRATION_SYNC.md` | **Modulo Registrazione & Autenticazione Utenti**. Implementato il database SQLite locale (SQLAlchemy), cifratura password (bcrypt) e generazione token JWT (scadenza a 30 giorni) con rotte `/register`, `/token` e `/me`. Aggiunta la **Regola Critica di Conservazione dei File** nel protocollo di sincronizzazione per impedire cancellazioni involontarie da parte del cloud agent. | ✅ Implementato e testato |

---

## 🎯 4. Prossimi Passi Coordinati

A beneficio di entrambi gli ambienti di sviluppo:
- **Client (AI Studio / Android)**: L'integrazione è attiva. `GeminiServiceClient` interroga automaticamente il backend locale (`http://${BuildConfig.LOCAL_BACKEND_IP}:8000/api/v1/scan`) caricato a build-time da `network_config.json`, ricadendo sul cloud in caso di server spento.
- **Backend (Antigravity / FastAPI)**: Lo scheletro e il motore di scansione sono pronti ed eseguibili su qualunque interfaccia di rete (host 0.0.0.0 per accettare accessi LAN) tramite `backend/run.ps1`.
- **IP Auto-Update & Sync Tool**: Eseguendo `python update_ip.py` nella radice del progetto, l'IP locale viene risolto, aggiornato in `network_config.json` ed inviato su GitHub in modo automatico.
- **Prossimi Step (Fase 4 & 5)**:
  - Sviluppo del modulo di registrazione e autenticazione utenti sul backend (con database relazionale SQLite/PostgreSQL).
  - Setup dei canali WebSocket sicuri (`wss://`) per lo *Smart-Sync* collaborativo tra partner.
  - Sviluppo dell'interfaccia web di amministrazione per il prompt playground e il monitoraggio budget.

---

## ⚠️ 5. REGOLA CRITICA DI CONSERVAZIONE DEI FILE (ATTENZIONE AI STUDIO!)

> [!WARNING]
> **REGOLA FONDAMENTALE E IMPERATIVA PER GLI AGENTI CLOUD (AI STUDIO):**
> **NON ELIMINARE MAI**, in nessuna circostanza o per finalità di refactoring, pulizia o "standardizzazione", i file e le directory che appartengono al backend on-premise o all'infrastruttura di rete locale.
> 
> Anche se l'ambiente di emulazione cloud di AI Studio non può eseguire direttamente un server Python o non rileva l'uso locale di determinati script, queste risorse **sono fondamentali per lo sviluppo locale sul device fisico del cliente (Google Antigravity / PC locale)**.
> 
> **ELENCO COMPONENTI DA NON TOCCARE / NON CANCELLARE:**
> 1. **La cartella `backend/`** in ogni sua parte (inclusi `main.py`, `requirements.txt`, `services.py`, `schemas.py`, `run.ps1`, `database.py`, `models.py`, `auth.py`, `routers/`, `test_auth.py`, ecc.).
> 2. **I file di configurazione di rete e sincronizzazione IP**: `network_config.json` e lo script helper `update_ip.py`.
> 3. **L'infrastruttura del Gradle Wrapper**: I file `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` e `gradle/wrapper/gradle-wrapper.jar`.
> 
> Qualsiasi rimozione arbitraria di questi file interrompe il build system locale e compromette lo sviluppo ibrido client-server.
