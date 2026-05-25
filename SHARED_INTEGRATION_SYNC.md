# Registro di Integrazione e Sincronizzazione: AI Studio & Antigravity (SmartGrocery Shared State)

> [!IMPORTANT]
> **PROTOCOLLO DI SINCRONIZZAZIONE E COESISTENZA (MANDATORIO):**
> Questo file è il canale di comunicazione condiviso tra **Google AI Studio** e **Antigravity**. 
> *   **REGOLA D'ORO:** Ogni volta che viene richiesto un allineamento o un "sync" sul progetto, l'agente o la piattaforma di sviluppo (AI Studio o Antigravity) **DEVE obbligatoriamente come primissimo passo leggere questo file e i due documenti tecnici di specifica** (`V4Pro_Client_Document.md` e `V4Pro_Backend_Document.md`).
> *   **ANALISI DELLO STATO:** L'ambiente di sviluppo deve comprendere chi ha effettuato le ultime modifiche e lo stato dell'arte attuale dell'intero ecosistema (Frontend Android e Backend Python) prima di scrivere qualsiasi riga di codice.
> *   **PREVENZIONE DEI CONFLITTI:** Questo previene "derive architetturali" (prendere strade diverse o non compatibili) e garantisce che lo sviluppo prosegua fedelmente rispetto alla roadmap sinergica approvata.
> *   **AGGIORNAMENTO DEL REGISTRO:** Al completamento di modifiche strutturali, aggiorna tempestivamente le tabelle di log qui sotto e la checklist di sviluppo.

---

## 1. Stato di Sincronizzazione Attuale

| Parametro | Valore Corrente | Data Ultimo Allineamento | Riferimento Commit / Stato |
| :--- | :--- | :--- | :--- |
| **Piattaforma Attiva** | Antigravity | 2026-05-25 | Allineato post sdoppiamento documentazione |
| **Versione Client Android** | Jetpack Compose (5 Tabs) | 2026-05-25 | Room Database attivo, ViewModel con mocks operativi |
| **Versione Backend** | Da Sviluppare | 2026-05-25 | Inizializzazione pianificata in Python/FastAPI |
| **Modello LLM On-Premise**| Llama 3 (8B Instruct) | 2026-05-25 | Configurazione pianificata tramite Ollama locale |

---

## 2. Registro Modifiche (Change Log)

### A. Modifiche apportate da ANTIGRAVITY

| Data | Modulo Coinvolto | Descrizione della Modifica | File Modificati / Nuovi |
| :--- | :--- | :--- | :--- |
| **2026-05-25** | Documentazione | Sdoppiata la specifica tecnica cumulativa `V4Pro_Master_Document.md` in due specifiche verticali separate per facilitare lo sviluppo parallelo client/backend. Creato questo file di sincronizzazione comune. | [V4Pro_Client_Document.md](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/V4Pro_Client_Document.md), [V4Pro_Backend_Document.md](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/V4Pro_Backend_Document.md), [SHARED_INTEGRATION_SYNC.md](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/SHARED_INTEGRATION_SYNC.md), [V4Pro_Master_Document.md](file:///g:/Il%20mio%20Drive/Android%20Apps/SmartGrocery/V4Pro_Master_Document.md) (Rimosso) |

### B. Modifiche apportate da GOOGLE AI STUDIO
*(Sezione riservata agli aggiornamenti eseguiti tramite l'interfaccia o l'agente di Google AI Studio)*

| Data | Modulo Coinvolto | Descrizione della Modifica | File Modificati / Nuovi |
| :--- | :--- | :--- | :--- |
| *(In attesa)* | | *Nessuna modifica registrata da questa piattaforma.* | |

---

## 3. Roadmap e Checklist di Sviluppo Sinergica

Usa questa griglia per tracciare lo stato delle macro-funzionalità, specificando chi se ne sta occupando.

### FASE 1: Riorganizzazione e Documentazione (Completata)
- [x] Sdoppiamento della documentazione master in Client e Backend (*Antigravity* - Fatto)
- [x] Istituzione del file di sincronizzazione comune `SHARED_INTEGRATION_SYNC.md` (*Antigravity* - Fatto)

### FASE 2: Inizializzazione Backend On-Premise (Pianificata)
- [ ] Creazione della cartella `backend/` nella radice del progetto (*Pianificato*)
- [ ] Inizializzazione del progetto Python con FastAPI e configurazione di base (*Pianificato*)
- [ ] Configurazione del Database locale SQLite/PostgreSQL con SQLAlchemy/SQLModel (*Pianificato*)
- [ ] Sviluppo degli endpoint di Autenticazione (Registrazione, Login, Refresh JWT, Hashing password con Argon2id) (*Pianificato*)
- [ ] Creazione del modulo Household (Generazione ed associazione con Codici d'Invito cifrati) (*Pianificato*)

### FASE 3: Integrazione OCR & Local LLM (Pianificata)
- [ ] Sviluppo del client di inferenza locale in FastAPI per connettersi ad Ollama (Llama 3 8B Instruct) (*Pianificato*)
- [ ] Endpoint `/api/receipts/analyze` che riceve i dati di posizionamento geometrico e li struttura via JSON Mode (*Pianificato*)
- [ ] Migrazione del client Android dall'OCR mock all'integrazione di Google ML Kit locale (*Pianificato*)
- [ ] Sviluppo dell'algoritmo Android per calcolare l'ordinamento spaziale delle righe ($x, y$ bounding boxes) prima dell'invio (*Pianificato*)
- [ ] Sostituzione delle chiamate mock nel ViewModel con chiamate reali all'endpoint del backend (*Pianificato*)

### FASE 4: Sincronizzazione WebSocket & Ledger (Pianificata)
- [ ] Sviluppo del server WebSocket in FastAPI con canali isolati per `household_id` (*Pianificato*)
- [ ] Connessione del client Android al canale WebSocket per il broadcast real-time delle modifiche della lista spesa e del Ledger (*Pianificato*)
- [ ] Sviluppo dell'algoritmo di merge delle modifiche offline (CRDT) sul backend (*Pianificato*)

### FASE 5: Pannello Web di Amministrazione e Hardening (Pianificata)
- [ ] Realizzazione della Dashboard Web Admin in FastAPI (HTML, CSS scuro personalizzato, Javascript reattivo) (*Pianificato*)
- [ ] Implementazione del Playground di diagnostica dei prompt per affinare il comportamento di Llama 3 (*Pianificato*)
- [ ] Blindatura del sistema: CSP, rate limiting, mitigazione CSRF e CORS (*Pianificato*)
