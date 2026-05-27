# Architettura, Logiche di Business e Regole Comportamentali di SmartGrocery Manager 📜⛪

> **LA BIBBIA DEL PROGETTO**: Questo documento contiene i requisiti funzionali, architetturali e le regole di business non negoziabili dell'applicazione. Nessun agente o compilatore automatico può alterare i comportamenti descritti di seguito senza un'esplicita richiesta dell'utente.

---

## 🛡️ 1. Validazione Pre-OCR e Rifiuto Foto Non Pertinenti (Anti-Scrivania)
*   **Comportamento**: Prima di inoltrare qualsiasi testo OCR alle API Cloud Gemini o ai server backend locali, il client Android **DEVE** validare il testo rilevato tramite la funzione `isValidReceiptOcrText` in `GroceryViewModel.kt`.
*   **Regole di Validazione**:
    1.  Il testo deve contenere almeno 20 caratteri.
    2.  Deve contenere almeno una cifra numerica.
    3.  Deve includere almeno una parola chiave GDO/Fiscale (es. *totale*, *euro*, *p.iva*, *scontrino*, *fiscale*, *importo*, *pagamento*, *esselunga*, *lidl*, ecc.) oppure un prezzo formattato con decimali (es. `1,39` o `2.50` mediante regex `\d+[,.]\d{2}`).
*   **Outcome**: Se la foto ritrae legno, pavimenti, scatoloni o testo non pertinente, l'app interrompe la scansione all'istante, spegne i caroselli di attesa, e mostra all'utente un messaggio d'errore chiaro: *"La foto scansionata non sembra contenere uno scontrino valido o leggibile. Riprova inquadrando lo scontrino da vicino..."*.
*   **DIVIETO ASSOLUTO**: Non è consentito alcun fallback fittizio (fake/fictitious database data o scontrini inventati tipo "Spesa Mista Local AI") del client. Se l'OCR non convalida lo scontrino, la chiamata API non deve partire.

---

## 🗑️ 2. Integrità della Rimozione Scontrini e Prevenzione Duplicati
*   **Comportamento**: Quando una nota spese o scontrino di contabilità (`LedgerEntry`) viene rimosso dal database (manualmente tramite `deleteLedgerEntry` o ri-computato), tutti gli articoli della spesa ad esso collegati presenti nella tabella `items` (gestiti via Room) **DEVONO** essere rimossi a cascata.
*   **Logica Applicata**:
    1.  La cancellazione invoca `deleteItemsByTimestampAndStore(timestamp, storeName)` nel DAO di Room.
    2.  Questo assicura che cancellando uno scontrino e rifacendo la foto allo stesso identico scontrino successivamente, il database risulti perfettamente "pulito" e non si verifichino duplicazioni fantasma degli articoli nella spesa dei coinquilini.
    3.  Ricalcola asincronamente anche la data `lastSeen` del supermercato basandosi sulle transazioni residue corrette.

---

## 🔀 3. Flusso di Riconciliazione e Logica di Matching Automatico degli Scontrini Duplicati
*   **Trigger**: Il controllo di riconciliazione/deduplica viene calcolato ogni volta che un nuovo scontrino viene scansionato ed è rieseguito reattivamente in tempo reale a ogni modifica manuale della lista articoli (creazione, aggiornamento prezzi, eliminazione).
*   **Regole di Matching (Dettaglio Tecnico)**:
    1.  **Esclusione Partita IVA**: Se sia lo scontrino esistente che quello nuovo hanno una Partita IVA (VAT) compilata e queste non coincidono (`vatMismatch`), lo scontrino non viene considerato un duplicato anche se date e importi coincidono.
    2.  **Calcolo Importo Totale**: L'importo totale dello scontrino registrato viene ricavato sommando puntualmente i prezzi dei singoli articoli presenti in `receiptItemsJson` (se popolato), altrimenti fa fallback sul campo generico `entry.amount`.
    3.  **Tolleranza di Prezzo**: Due scontrini si considerano aventi lo stesso importo se la differenza assoluta tra i totali è strettamente inferiore a **0.15€** (`Math.abs(existingGrandTotal - total) < 0.15`), consentendo di assorbire lievi arrotondamenti o imperfezioni di lettura OCR.
    4.  **Coincidenza Temporale**: I due scontrini devono appartenere allo stesso identico giorno solare (confezionato via metodo `isSameDay(timestampA, timestampB)`).
*   **Impostazioni di Default**:
    1.  Il pulsante **"Sì, Integra"** è pre-selezionato di default (`userDecisionToReconcile` impostato a `true` e `reconciledLedgerEntryId` impostato sull'ID trovato).
    2.  La lista dei singoli prodotti scansionati `showItemsList` rimane sempre **visibile (`true`)** per consentire modifiche puntuali e individuali di classificazione dei nuovi articoli (Spazio Casa vs Privato).
*   **Rendering Web-Safe in Compose**:
    1.  Nella lista degli articoli scansionati, ogni riga di item ha una chiave univoca derivata da nome, prezzo e index (`"${pair.first.name}_${pair.first.price}_$index"`) fornita a `itemsIndexed`.
    2.  I rilevatori di gesture orizzontale (swipe-to-delete) tengono traccia della stabilità dell'oggetto `pItem` anziché dell'indice intero fluttuante.
    3.  Ciò impedisce che la cancellazione manuale di un singolo articolo causi la sparizione visiva errata di altri elementi o dell'intera lista a causa dei cicli di recomposition di Jetpack Compose.

---

## 🛰️ 4. Protocollo di Integrazione e Dynamic IP
*   L'anagrafica IP e di rete per connettere l'app all'ambiente backend locale è salvata rigorosamente in `network_config.json`.
*   Nessun fattore di build o indirizzo statico deve essere codificato nell'ambiente Android al di fuori di quanto generato dallo script `update_ip.py` o iniettato via `BuildConfig`.

---

## 📱 5. Pulizia dell'Interfaccia Scanner e Assenza di Dati Fittizi di Default
*   **Assenza di Dati Democratici / Fittizi**: All'avvio dell'applicazione non deve essere mostrato o inserito alcun dato fittizio, dimostrativo o di simulazione precostituita (quali prodotti demo temporanei o "Scontrini in sospeso fittizi" quali Lidl al "Via Milano, 5" ed Esselunga in "Corso Sempione, 46") a meno che non sia stato esplicitamente inserito o acquisito dall'utente. Un'inizializzazione pulita sul database locale vuoto garantisce l'assoluta conformità con l'ambiente reale.
*   **Pannello "Modalità sviluppatore"**: Tutte le opzioni di test, diagnostiche, auto-trigger, preset di debug degli scontrini OCR (Lidl, Esselunga) ed inserimento testo OCR personalizzato per testare i servizi sono banditi dallo schermo principale o dallo Scanner e devono invece risiedere esclusivamente all'interno delle Impostazioni Globali sotto l'etichetta **"Modalità sviluppatore"** (attivabile da amministratore).
*   **Scanner Screen Semplificato**: La tab Scanner deve escludere HUD scuri di simulazione della fotocamera ("Fotocamera reale" HUD), calcoli intermedi visibili per Laplace ad uso interno o pulsanti di caricamento preset. Al suo posto, devono essere presenti esclusivamente pulsanti prominenti per:
    1.  **Acquisisci Scontrino**: che apre direttamente l'inquadratura reale (`SCONTRINO`) per l'analisi OCR e associazione degli scontrini reali.
    2.  **Acquisisci Etichetta Scaffale**: che apre direttamente l'inquadratura reale (`SCAFFALE`) per aggiornare il catalogo o verificare il costo opportunità.

