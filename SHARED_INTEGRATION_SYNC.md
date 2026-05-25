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

---

## 🎯 4. Prossimi Passi Coordinati

A beneficio di entrambi gli ambienti di sviluppo:
- **Client (AI Studio / Android)**: L'integrazione è attiva. `GeminiServiceClient` interroga automaticamente il backend locale (`http://10.0.2.2:8000/api/v1/scan`) se abilitato, ricadendo silenziosamente sul cloud Google AI Studio in caso di errori di rete, garantendo resilienza totale.
- **Backend (Antigravity / FastAPI)**: Lo scheletro e il motore di scansione sono pronti ed eseguibili tramite `backend/run.ps1`.
- **Prossimi Step (Fase 4 & 5)**:
  - Sviluppo del modulo di registrazione e autenticazione utenti sul backend (con database relazionale SQLite/PostgreSQL).
  - Setup dei canali WebSocket sicuri (`wss://`) per lo *Smart-Sync* collaborativo tra partner.
  - Sviluppo dell'interfaccia web di amministrazione per il prompt playground e il monitoraggio budget.
