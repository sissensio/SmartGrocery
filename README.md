<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# SmartGrocery Manager (V4Pro)

Benvenuto nel repository di **SmartGrocery Manager**, un assistente silenzioso a "Frizione Zero" incentrato sulla privacy domestica e sulla contabilità condivisa per la famiglia.

Questo progetto adotta un approccio **ibrido e cooperativo**, permettendo lo sviluppo sinergico sia tramite **Google AI Studio** (per modifiche rapide all'interfaccia utente o test del client) sia tramite **Antigravity** (per l'implementazione del codice di backend, logiche offline-first avanzate e blindatura di sicurezza).

---

## 1. La Visione del Progetto
SmartGrocery Manager è progettato per eliminare l'attrito quotidiano della catalogazione manuale della spesa.
*   **Privacy On-Premise Totale:** I dati personali e le transazioni non vengono mai condivisi con server o API cloud commerciali soggetti a profilazione. L'AI gira interamente in locale.
*   **Computer Vision & OCR Locale:** Estrazione geometrica dei testi degli scontrini sul client (Google ML Kit) ed elaborazione semantica sul backend tramite un LLM locale (**Llama 3 8B Instruct** su Ollama).
*   **Sincronizzazione Real-Time (Smart-Sync):** Liste spesa aggiornate istantaneamente tra tutti i membri dello stesso nucleo familiare tramite WebSocket.
*   **Ledger e Smart-Split:** Smistamento selettivo degli acquisti (privato vs condiviso) e calcolo dinamico del saldo netto dei debiti per semplificare la contabilità domestica.

---

## 2. Struttura del Progetto e Documentazione
La documentazione è stata suddivisa per facilitare lo sviluppo parallelo ed evitare ridondanze. Consulta i seguenti file all'interno del repository per approfondire le specifiche tecniche:

*   **[SHARED_INTEGRATION_SYNC.md](SHARED_INTEGRATION_SYNC.md):** **Il ponte di coesistenza tra AI Studio e Antigravity.** Leggi questo file all'inizio di ogni sessione per allinearti sullo stato del codice, consultare il log delle modifiche di ciascuna piattaforma e tracciare la roadmap comune senza creare conflitti.
*   **[V4Pro_Client_Document.md](V4Pro_Client_Document.md):** Documento tecnico verticale focalizzato sul **Client Android** (Jetpack Compose, Room SQLite, crittografia SQLCipher con Keystore, integrazione OCR Google ML Kit, e algoritmi predittivi on-device come la regola dei 3-Strikes e il Daily Need).
*   **[V4Pro_Backend_Document.md](V4Pro_Backend_Document.md):** Documento tecnico verticale focalizzato sul **Backend On-Premise e sulla Web Dashboard** (FastAPI, SQLite/PostgreSQL, integrazione locale Ollama, WebSocket Server, hashing Argon2id e specifiche dell'interfaccia web di amministrazione).

---

## 3. Roadmap di Sviluppo (Le Intenzioni e Come Procederemo)

Le attività di sviluppo sono pianificate in 5 macro-fasi sequenziali, descritte in dettaglio e monitorate all'interno del file [SHARED_INTEGRATION_SYNC.md](SHARED_INTEGRATION_SYNC.md):

1.  **Fase 1: Riorganizzazione Documentale (Completata):** Suddivisione dei requisiti in specifiche client/backend e attivazione del registro di sincronizzazione comune.
2.  **Fase 2: Inizializzazione Backend FastAPI (Pianificata):** Creazione del server locale in Python, setup del database e sviluppo dei moduli sicuri di registrazione utenti, login e associazione Household (nucleo familiare).
3.  **Fase 3: Integrazione OCR & LLM Locale (Pianificata):** Collegamento del backend a Ollama (Llama 3 8B). Migrazione dell'app Android a Google ML Kit OCR nativo con invio geometrico delle bounding boxes per la strutturazione semantica degli scontrini.
4.  **Fase 4: WebSocket Real-Time (Pianificata):** Attivazione dei canali di comunicazione persistenti e bidirezionali per sincronizzare all'istante le modifiche tra i partner e implementazione dei merge offline CRDT sul backend.
5.  **Fase 5: Web Admin Dashboard & Hardening (Pianificata):** Sviluppo del pannello di controllo web per l'amministratore (playground prompt, grafici budget e anagrafica prodotti) e blindatura di sicurezza OWASP.

---

## 4. Come Eseguire il Progetto in Locale (Run Locally)

### Prerequisiti
*   [Android Studio](https://developer.android.com/studio) installato.
*   (Opzionale per sviluppo transitorio) Una chiave API di Gemini se desideri testare il fallback provvisorio sul client.

### Istruzioni
1.  Apri **Android Studio**.
2.  Seleziona **Open** e scegli la cartella principale contenente questo progetto.
3.  Attendi che Gradle importi il progetto e consenti ad Android Studio di risolvere eventuali incompatibilità.
4.  Crea un file chiamato `.env` nella directory principale del progetto e definisci `GEMINI_API_KEY` (vedi `.env.example` come traccia).
5.  Rimuovi la seguente riga dal file `build.gradle.kts` dell'app per abilitare la firma di debug sul tuo emulatore:
    `signingConfig = signingConfigs.getByName("debugConfig")`
6.  Avvia l'app su un emulatore o su un dispositivo Android fisico collegato.
