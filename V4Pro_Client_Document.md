# Documento Tecnico Client Android: SmartGrocery Manager (V4Pro - Client Spec)

Questo documento definisce le specifiche ingegneristiche dell'applicazione mobile **SmartGrocery Manager**, delineando lo stato dell'arte attuale del codice e la roadmap per il completamento delle funzionalità target a "Frizione Zero".

---

## 1. Visione del Progetto e Filosofia "Frizione Zero"
L'app si comporta come un **Assistente Silenzioso**. Minimizza l'interazione manuale (massimo due tap per completare qualsiasi flusso quotidiano primario), basandosi su geofencing, realtà aumentata e logiche predittive on-device per dedurre i comportamenti dell'utente.

---

## 2. Architettura di Sicurezza del Dispositivo (Device Hardening)
Per proteggere i dati sensibili di spesa e geolocalizzazione, il client adotta standard crittografici rigorosi.

### Stato dell'Arte Attuale:
*   **Edge-to-Edge & Tonal Elevation:** Gestione immersiva dell'interfaccia utente in `MainActivity.kt`.
*   **Crash Detector Safe Pipeline:** Un gestore globale di eccezioni non catturate (`Thread.setDefaultUncaughtExceptionHandler`) in `MainActivity.kt` previene arresti anomali silenziosi del canale IPC.
*   **Stato Offline:** Il ViewModel traccia lo stato offline tramite `isOfflineMode`.

### Target di Sviluppo (Desiderata):
1.  **SQLCipher Database:** Crittografia completa del database Room (`AppDatabase`) tramite AES-256-CBC con chiave hardware derivata da **Android Keystore (TEE)**.
2.  **Gatekeeper Biometrico (BiometricPrompt):** Integrazione nativa dello sblocco biometrico all'avvio o alla riattivazione dell'app.
3.  **Griglia di Fallback Sequenziale 3x3:** Sblocco con sequenza geometrica discreta (per uso rapido e riservato alla cassa del supermercato).
4.  **Auto-Lock a 5 Minuti:** Una macchina a stati temporizzata cancella la chiave crittografica dalla RAM se l'app è inattiva o in background per oltre 5 minuti (`lockKeystore()`).
5.  **Attestazione Hardware:** Integrazione delle API **Google Play Integrity** per verificare l'integrità del dispositivo e bloccare le chiavi sensibili su dispositivi Rootati o manomessi.

---

## 3. Gestione dei Dati e Database Tripartito
Lo schema dei dati client si divide in tre ambiti isolati logicamente e fisicamente:

| Ambito | Contenuto | Sincronizzazione |
| :--- | :--- | :--- |
| **Spazio Privato** | Spese personali, scontrini privati, budget individuali. | Cifrato in SQLCipher. Backup cloud cifrato end-to-end (Zero-Knowledge). |
| **Household Pool** | Lista della spesa condivisa, scontrini della casa, Ledger debiti. | Sincronizzato in tempo reale via WebSocket sicuri (`wss://`) tramite "Codice Casa". |
| **Global Crowdsourced** | Cataloghi prodotti, statistiche prezzi medi, dizionari OCR. | Anonimizzato via Privacy Differenziale prima dell'invio. |

---

## 4. Geofencing Passivo e Coda Scontrini
Elimina la necessità di scansionare lo scontrino nella confusione post-cassa.

### Stato dell'Arte Attuale:
*   **Coda Scontrini in Sospeso:** Gestione dell'entità `PendingReceipt` e caricamento asincrono nella dashboard iniziale in `HomeScreen.kt` ("Hai N scontrini in sospeso").
*   **Geofence Simulation:** Nel ViewModel è presente una variabile `activeGeofenceNotification` per simulare l'attivazione dei confini del negozio.

### Target di Sviluppo (Desiderata):
*   **Geofencing API Nativo:** Monitoraggio passivo basato su celle radio e transizioni di geofence (raggio 50 metri).
*   **Notifiche Push Interattive (Geofence EXIT):** All'uscita dal supermercato viene generata una notifica push locale:
    *   *SÌ:* Deep link che avvia lo scanner pre-compilando il negozio.
    *   *NO:* Rifiuto definitivo dell'evento.
    *   *DOPO:* Inserimento dell'evento nella tabella Room `PendingReceiptQueue` per il batch scanning a mente fredda.

---

## 5. Computer Vision & OCR Client-Side
Interfaccia di scansione intelligente per catalogazione scorte e lettura scontrini.

### Stato dell'Arte Attuale:
*   **Scanner Screen UI (`ScannerScreen.kt`):** Layout per inquadrare scontrini o scaffali con indicatori visivi di stabilità.
*   **Parser Locale Fallback:** Funzione `discernPricingElements` per estrapolare pesi, prezzi unitari e prezzi al kg, inclusa l'euristica per correggere errori OCR (es. `5,32` vs `5,92`).
*   **Date OCR Parser:** Metodo `extractReceiptTimestamp` per dedurre la data dello scontrino anche in presenza di mesi scritti in italiano abbreviato (es. `"MAG"`, `"MGG"`).

### Target di Sviluppo (Desiderata):
1.  **Google ML Kit OCR Integration:** Scansione offline del testo con tracciamento geometrico delle parole.
2.  **Ordinamento Geometrico Spaziale ($x, y$):** Raggruppamento dei blocchi di testo letti da sinistra a destra e dall'alto in basso, compensando le distorsioni prospettiche e formattando il testo in righe coerenti da inviare al backend.
3.  **Blur Variance Auto-Trigger:** Algoritmo basato sulla varianza di Laplace che cattura il frame automaticamente senza costringere l'utente a fare tap sullo schermo (evitando foto sfocate).
4.  **AR Overlay (HUD):** Reticolo blu attorno ai prezzi dell'etichetta dello scaffale e reticolo verde sopra i codici a barre (EAN) in tempo reale.

---

## 6. Algoritmi Predittivi e Dispensa Domestica

### Stato dell'Arte Attuale:
*   **Semaforo "Daily Need" (Urgenza):** Calcolato nel ViewModel e visualizzato nella lista. Classifica i prodotti in:
    *   *Rosso (Esaurito):* Giorni trascorsi dall'ultimo acquisto $\geq \mu$ (Delta Medio).
    *   *Giallo (Esaurimento imminente nelle 48h):* Giorni trascorsi $\geq (\mu \times 0.8)$.
    *   *Verde (Sufficiente).*
*   **Regola dei "3-Strikes" (`applyThreeStrikesRule`):** Rileva le deviazioni di acquisto di marca. Se un utente acquista una marca alternativa consecutivamente per 3 volte, il sistema aggiorna silenziosamente la marca preferita nello Smart-Lookup.

---

## 7. Collaborazione, Smart-Split & Real-Time Sync

### Stato dell'Arte Attuale:
*   **Smart-Split UI:** Consente di selezionare gli articoli scansionati, assegnandoli con un tocco allo *Spazio Privato* (nascosto) o allo *Spazio Casa* (condiviso).
*   **Ledger Integrato:** Visualizza i saldi in `LedgerScreen.kt` e permette l'aggiornamento dinamico degli articoli.
*   **WebSocket Simulation:** Ricezione di messaggi toast simulati quando avvengono sincronizzazioni.

### Target di Sviluppo (Desiderata):
*   **Connessione WebSocket Reale:** Integrazione con il backend FastAPI per riflettere all'istante l'acquisto di un articolo sugli smartphone di tutti i membri dell'Household, prevenendo doppi acquisti nei corridoi.
