# Documento Tecnico Backend & Web Admin: SmartGrocery Manager (V4Pro - Backend Spec)

Questo documento definisce le specifiche ingegneristiche dell'infrastruttura **Backend On-Premise** e dell'interfaccia **Web Administration** di **SmartGrocery Manager**.

---

## 1. Visione del Backend e Sovranità dei Dati
Il backend è progettato per essere eseguito **on-premise** (in locale su un server domestico, PC fisso o Raspberry Pi). Questo garantisce la totale riservatezza dei dati di spesa familiari, ed esclude dipendenze da server o API cloud commerciali soggetti ad abbonamento o a profilazione commerciale.

---

## 2. Stack Tecnologico di Riferimento
*   **API Framework:** Python con **FastAPI** (veloce, asincrono, auto-documentato con OpenAPI/Swagger).
*   **Database:** **PostgreSQL** per produzione, o **SQLite** per una configurazione zero-install a file singolo locale.
*   **Real-time engine:** Gestione dei WebSocket nativa in FastAPI per comunicazioni broadcast a bassissima latenza.
*   **Local LLM Host:** **Ollama** o **llama.cpp** in esecuzione locale per caricare ed eseguire l'inferenza di **Llama 3 (8B Instruct)**.

---

## 3. Gestione Utenti e Nucleo Familiare (Identity Module)

### Stato dell'Arte Attuale:
*   *Non ancora implementato* (Inizio sviluppo da zero).

### Specifiche Target (Desiderata):
1.  **Registrazione e Login (`/api/auth/register`, `/api/auth/login`):**
    *   Le password vengono cifrate in modo irreversibile tramite hashing **Argon2id** o **bcrypt** (cost factor 12) prima della scrittura su DB.
    *   Gestione delle sessioni con coppie di JWT (Access Token con validità 15 minuti e Refresh Token memorizzato in un cookie cifrato HTTP-Only, SameSite=Strict).
2.  **Sistema Household (Associazione Multi-User):**
    *   *Creazione Casa:* L'utente genera un `household_id` e ottiene un codice d'invito crittografato e firmato digitalmente dal backend.
    *   *Associazione Casa:* Inserendo il codice d'invito su un secondo smartphone, l'utente partner viene associato allo stesso `household_id` nel database, abilitando la visualizzazione e la modifica delle risorse comuni.

---

## 4. OCR & Local LLM Integration (AI Parser)
Riceve il testo estratto in locale da Google ML Kit sul client e lo struttura semanticamente tramite Llama 3.

```
[ Client Android ] --( JSON Geometrico )--> [ FastAPI API ]
                                                   |
 ( Formattazione Prompt & JSON Schema ) <----------+
  |
  v
[ Local Ollama / Llama 3 8B ] --( JSON Strutturato )--> [ Client Android ]
```

### Specifiche Target (Desiderata):
*   **Endpoint di Analisi (`/api/receipts/analyze`):** Riceve il payload JSON geometrico dal client.
*   **Ricostruttore Righe:** Il backend riordina spazialmente i blocchi di testo basandosi sulle coordinate bounding box ($x, y$) per compensare disallineamenti di stampa.
*   **Llama 3 JSON Mode:** Il backend interroga Ollama abilitando la modalità JSON e vincolando l'output ad uno schema rigido.
*   **Schema Output Atteso (`ParsingReceiptResult`):**
    ```json
    {
      "storeName": "Ragione Sociale normalizzata (es: Esselunga)",
      "vatNumber": "Partita IVA (11 cifre) o null",
      "address": "Indirizzo del negozio o null",
      "phone": "Telefono del negozio o null",
      "receiptDate": "Data scontrino in formato YYYY-MM-DD",
      "receiptTime": "Ora scontrino in formato HH:mm",
      "items": [
        {
          "name": "Descrizione prodotto pulita da abbreviazioni",
          "brand": "Marca del prodotto o vuoto",
          "price": 0.0,
          "unitPrice": 0.0,
          "category": "Latticini/Dispensa/Frutta e Verdura/Bevande/ecc.",
          "isShared": true,
          "barcode": "EAN associato o vuoto",
          "weight": null,
          "pricePerKg": null,
          "confidence": 0.95
        }
      ],
      "totalAmount": 0.0
    }
    ```

---

## 5. Hub di Sincronizzazione Real-Time (Smart-Sync)
Garantisce la coerenza dei dati tra i diversi membri dello stesso Household.

### Specifiche Target (Desiderata):
*   **WebSocket Handler (`/api/sync/ws`):** Canali WebSocket persistenti protetti da JWT. All'interno del server, le connessioni attive sono suddivise in gruppi per `household_id`.
*   **Message Broadcasting:** Quando un utente spunta un articolo o aggiorna uno scontrino, il server inoltra istantaneamente la notifica con l'evento a tutti gli altri membri del gruppo associati alla stessa Casa.
*   **CRDT Data Merger:** In fase di sincronizzazione post-offline, il backend riceve il delta delle modifiche locali dal client, risolve eventuali conflitti logici tramite algoritmi CRDT (Conflict-Free Replicated Data Types) e memorizza lo stato finale unificato sul database, spingendolo poi in broadcast.

---

## 6. Ledger Finanziario & Costo Opportunità
*   **Engine di Split Spesa:** Il backend calcola i debiti e crediti del nucleo familiare in base agli scontrini condivisi registrati nel Ledger, producendo un saldo netto compensato per evitare scambi di denaro continui.
*   **Logistica Prezzi:** Mappatura dei listini prezzi crowdsourcing locali per ciascuna Partita IVA negozio. All'invio della lista della spesa dal client, l'algoritmo valuta la deviazione dei prezzi storici e calcola la convenienza di fare la spesa in più tappe (Teorema del Costo Opportunità) tenendo conto dei costi di carburante e del tempo di percorrenza stimato.

---

## 7. Interfaccia Web di Amministrazione (Web Admin)
Una splendida dashboard web ad alte prestazioni, con design premium in modalità scura, per la gestione e la diagnostica del sistema.

### Moduli del Pannello Web:
1.  **Dashboard Finanziaria:** Visualizzazione grafica dei budget familiari, statistiche di spesa mensili per categoria e andamento storico dell'inflazione sui prodotti preferiti.
2.  **LLM Prompt Playground:** Interfaccia per testare e sintonizzare il prompt di sistema di Llama 3. Permette all'amministratore di incollare testo grezzo di scontrini complessi, visualizzare in tempo reale l'output strutturato dall'LLM ed eventualmente affinare il prompt.
3.  **Gestione Household & Devices:** Visualizzazione dei membri associati alla casa, generazione di nuovi codici di invito e revoca delle sessioni di dispositivi non più autorizzati.
4.  **Catalogo Anagrafiche:** Modifica e normalizzazione delle coppie di Jaro-Winkler (descrizioni scontrino -> prodotti reali) e gestione del registro dei punti vendita fisici.
5.  **Sicurezza Hardened (OWASP):** Protezione totale da CSRF tramite token di sessione, restrizioni CORS rigorose sul dominio locale, ed eventuale attivazione del Multi-Factor Authentication (MFA) tramite TOTP (es. Google Authenticator).
