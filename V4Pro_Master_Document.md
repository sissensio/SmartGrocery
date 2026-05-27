# Specifica Architetturale e Tecnica Definitiva: SmartGrocery Manager (V4Pro - Master Document)

Questo documento (V4Pro) rappresenta l'ingegnerizzazione olistica, profonda e definitiva per l'applicativo **SmartGrocery Manager**. Definisce le fondamenta teoriche, l'architettura dei moduli, i flussi di dati e l'esperienza d'uso per consentire lo sviluppo federato di un'applicazione mobile a "Frizione Zero" incentrata sulla privacy.

---

## 1. Visione Olistica e Paradigma "Frizione Zero"

Il successo di SmartGrocery Manager risiede nella sua **invisibilità**. L'applicazione adotta il concetto di *Assistente Silenzioso*. Invece di richiedere costanti inserimenti manuali che portano all'abbandono dell'utente, l'applicazione si nutre passivamente dei comportamenti naturali per dedurre intenti, abitudini e ottimizzare il budget familiare. 

### Principi Cardine:
*   **Interazione Minimalista**: Massimo due toscchi (tap) per completare qualsiasi flusso quotidiano primario.
*   **Contestualizzazione Intelligente**: L'applicazione reagisce al tempo, alla posizione geografica e allo stato delle scorte domestiche in modo invisibile.

---

## 2. Architettura di Sicurezza, Autenticazione e Cifratura

Trattando metadati di geolocalizzazione sensibili ed informazioni di natura finanziaria/contabile, l'applicazione adotta standard crittografici di livello bancario e militare.

```
       [ Client App ] --( TLS 1.3 Pinning )--> [ Cloud API Gateway ]
             |                                        |
      ( SQLCipher AES-256 )                     ( JSON Web Tokens )
             |                                        |
     [ Secure Enclave / ]                             v
     [ Android Keystore ]                     [ Zero-Knowledge Storage ]
```

### 2.1 Pipeline di Autenticazione Ibrida
1.  **Cloud Identity (Livello 1)**: Autenticazione OAuth 2.0 (Google SSO come canale primario a bassa frizione) o Email/Password tradizionale con hashing bcrypt (cost factor 12) lato server. Rilascio di JWT Access Token a breve scadenza e Refresh Token.
2.  **Gatekeeper Biometrico Locale (Livello 2)**: L'accesso frequente sul dispositivo sfrutta le API native `BiometricPrompt` (Android) / `LocalAuthentication` (iOS) per sbloccare l'applicazione con FaceID o impronta digitale senza interazioni cloud.
3.  **Fallback Discreto (Codice PIN / Gesture Sequenziale)**: Qualora la biometria fallisca o l'utente indossi guanti/mascherina al supermercato, lo sblocco può avvenire tramite una **Gesture sequenziale su griglia 3x3** (estremamente discreta al riparo da sguardi indiscreti nelle code alla cassa) o un PIN a 6 cifre.
4.  **Inibizione Automatica per Sessione Inattiva**: Quando l'applicazione passa in secondo piano (background), una macchina a stati temporizzata mantiene la sessione decifrata per un massimo di 5 minuti. Superata questa soglia o in seguito al blocco dello schermo, lo stato crittografico viene sigillato chiamando la funzione interna `lockKeystore()`, richiedendo una nuova autenticazione locale biometria/gesture.

### 2.2 Blindatura del Dispositivo (Anti-Tampering)
*   **Archivio Cifrato Locale**: Database locale (Room/SQLite) integralmente cifrato tramite **SQLCipher (AES-256-CBC con PBKDF2)**. La chiave è conservata in hardware protetto (TEE - Trusted Execution Environment / Keystore su Android, Secure Enclave su iOS).
*   **In-Transit Security**: Comunicazioni con il cloud unicamente su protocollo TLS 1.3 con **Certificate Pinning** statico nel codice client. Qualsiasi attacco MITM su Wi-Fi pubblici dei centri commerciali comporta l'interruzione immediata delle chiamate ed il passaggio alla modalità Offline Sincrona.
*   **Hardware Attestation**: Interrogazione attiva delle API `Play Integrity` (Android) / `App Attest` (iOS). L'utente con dispositivo modificato (Root, Jailbreak, Bootloader sbloccato o hooking attivo via Frida) subisce il blocco delle funzionalità finanziarie locali e l'autodistruzione della chiave volatile in RAM.

---

## 3. Struttura del Database Tripartito e della Privacy Differenziale

Per garantire sia l'esigenza collaborativa del nucleo familiare che la privacy indivisibile del singolo utente, lo schema dei dati è rigorosamente segmentato in tre livelli isolati logicamente e crittograficamente.

| Database | Contenuto Principale | Isolamento e Infrastruttura |
| :--- | :--- | :--- |
| **Spazio Privato Locale** | Tabelle: `Users`, `PrivateReceipts`, `PrivateItems`, `ConsumptionHistory`, `PendingReceiptQueue` | Database SQLite locale cifrato con SQLCipher. Backup cloud cifrato end-to-end con chiave derivata dai segreti personali dell'utente (Zero-Knowledge). |
| **Household Pool (Casa)** | Tabelle: `HouseholdGroup`, `SharedList`, `SharedReceipts`, `Ledger` | Spazio sincronizzato tramite WebSocket cifrati. Accesso consentito unicamente ai dispositivi associati mediante "Codice Casa" univoco e firmato crittograficamente. |
| **Database Globale Crowdsourced** | Tabelle: `GlobalProducts`, `StorePrices`, `OCRDictionaries` | Cataloghi condivisi orizzontalmente. Prima dell'invio in crowdsourcing dei prezzi rilevati, il client pulisce qualsiasi metatato personale (UserID, coordinate GPS esatte, timestamp assoluto) applicando la **Privacy Differenziale**. |

### 3.1 Profilazione Etica e On-Device Marketing ML
L'applicazione ripudia il tracciamento dei profili utente sui server remoti. Invece, la profilazione comportamentale e la selezione di coupon/promozioni avvengono **integralmente on-device** tramite un modello leggero *TensorFlow Lite* in locale. L'applicazione scarica periodicamente pacchetti aggregati di inserzioni destinati a macro-cluster (es. "Interessato ad alimenti biologici senza lattosio nella provincia X"). Il modello locale valuta l'appartenenza dell'utente a questo cluster anonimo ed espone le sezioni promozionali idonee.

---

## 4. Ciclo di Vita delle Notifiche di Geofencing e "Coda Scontrini in Sospeso"

Uno dei maggiori attriti nell'uso di app per la spesa è l'obbligo di scansionare lo scontrino nel caos post-pagamento, tra borse ingombranti e fretta delle casse. Il sistema risolve questo attrito con la **Sospensione Asincrona della Scansione**.

```
  [ Supermercato ] ---> ( Rilevazione Geofence EXIT ) ---> [ Notifica Push Locale ]
                                                                   |
          +--------------------------------------------------------+
          v                                                        v
     [ Tap SÌ ]                                               [ Tap DOPO ]
          |                                                        |
   ( Avvia AR Scanner )                                 ( Salva in Pending Queue )
                                                                   |
                                                                   v
                                                        [ Home: Sezione Prominente ]
                                                        "Hai 2 scontrini in sospeso"
```

### 4.1 Logica del Geofencing Passivo
1.  L'applicazione monitora la posizione passivamente tramite cambi di celle radio e Wi-Fi conosciuti, svegliando il GPS ad alta precisione solo al rilevamento del confine virtuale del negozio (Geofence di 50 metri).
2.  All'uscita fisica dal supermercato (**Geofence EXIT**), viene generata una notifica push locale interattiva: `"Sei appena uscito da [Esselunga]. Vuoi scansionare lo scontrino adesso?"`:
    *   **Azione 1 (SÌ)**: Avvia un deep link alla fotocamera pre-compilando i dati del punto vendita rilevato.
    *   **Azione 2 (NO)**: Rifiuta e cancella definitivamente l'evento georilevato.
    *   **Azione 3 (DOPO)**: Salva le informazioni essenziali `{ StoreName, GeofenceLocation, Timestamp }` all'interno della tabella locale `PendingReceiptQueue`.

### 4.2 La Dashboard "Scontrini in Sospeso"
*   Se l'utente preme "DOPO" o ignora l'avviso, nella schermata iniziale dell'app (Home Screen) compare un modulo visivo elegante ma non ostruttivo con l'avviso `"Hai N scontrini in sospeso"`.
*   Quando l'utente si rilassa a casa, può toccare l'avviso per visualizzare la coda. Selezionando un elemento (es. `"Lidl - Oggi ore 19:15"`), la fotocamera si apre pre-valorizzando data e insegna, consentendo una sessione rapida di **Batch Scanning** e catalogazione cumulativa a mente fredda.

---

## 5. Moduli di Computer Vision: Assistente allo Scaffale e Scanner Scontrini

### 5.1 Assistente allo Scaffale (Etichette in Realtà Aumentata con Auto-Trigger)
Questo sottomodulo introduce una fotocamera assistita da Realtà Aumentata (AR) per catalogare scorte e prezzi senza l'uso di scontrini.
*   **AR Overlay (HUD)**: Sul flusso video live, il sistema visualizza in tempo reale un reticolo blu attorno ai blocchi testuali dei prezzi e un reticolo verde sopra i codici a barre (EAN/UPC) con indicatori di stabilità del puntamento.
*   **Blur Variance Auto-Trigger (Laplace-based)**: Per azzerare lo sfocamento generato dal tocco dello schermo, non esiste il pulsante fisco di scatto. L'algoritmo calcola dinamicamente la varianza del gradiente di Laplace sulla sequenza di frame pixel-by-pixel. Quando la varianza supera una determinata soglia di nitidezza (`Varianza > Soglia_Nitidezza`) e il decoder EAN estrae un codice a barre valido, lo scatto viene effettuato **automaticamente**.
*   **Estrazione Dati**: Il motore OCR locale estrae cinque dati essenziali: Descrizione, Marca, Peso/Volume (es. 500g, 1L), Prezzo unitario articolo e Prezzo per kg/litro. Qualora le metriche siano parziali o la foto sfocata, restituisce una vibrazione doppia di errore richiedendo il riposizionamento.

### 5.2 Scanner Scontrini GDO con Image Stitching e Jaro-Winkler Matching
La scansione degli scontrini cartacei affronta la criticità dei layout caotici e delle descrizioni abbreviate tipici dei registratori di cassa.
1.  **Image Stitching (Panning Continuo)**: Sotto scontrini di notevoli dimensioni, l'utente effettua uno scorrimento verticale. Il sistema applica algoritmi di feature matching (ORB/SIFT) per cucire i frame in un'unica immagine piana e corretta prospetticamente.
2.  **Riconoscimento Insegna (Mappe GDO)**: L'OCR individua nell'intestazione la catena (es. *Coop*, *Lidl*, *Esselunga*) ed applica una maschera geometrica personalizzata per quella catena che restringe l'area di estrazione alle colonne di quantità, sconti e importo lordo, riducendo gli errori sui decimali.
3.  **Pattern Matching Traduttivo (Jaro-Winkler)**: Per decifrare stringhe ostiche quali `"PR CR S.DAN"` o `"CHOC NOIR 80"`, il client effettua un calcolo basato sulla distanza di Jaro-Winkler rispetto al database globale crowdsourced. Il sistema associa con precisione percentuale la stringa abbreviata al prodotto reale, in questo caso `"Prosciutto Crudo San Daniele"` o `"Cioccolato Nero 80%"`.
4.  **Correzione Asincrona Collaborativa**: In caso di fallimento della traduzione, l'articolo viene provvisoriamente inserito come `"Prodotto Sconosciuto"`. L'applicazione non disturba l'utente al supermercato; in seguito, gli mostrerà un invito silenzioso per aiutarlo a battezzare il prodotto, salvando l'associazione in locale e arricchendo anonimamente il database globale in cloud.

---

## 6. Logica Algoritmica Predittiva e la Dispensa Domestica

Il ripopolamento della dispensa si fonda su calcoli dinamici delle frequenze d'acquisto per eliminare le classiche check-list manuali.

### 6.1 Il Semaforo del "Daily Need" (Urgenza)
Calcolo asincrono del tempo di esaurimento stimato per ciascun prodotto consumabile. Il sistema memorizza gli intervalli (Delta Giorni $\Delta G$) intercorsi tra l'inserimento dello stesso articolo nell'archivio storico scontrini.
*   $$\text{Delta Medio} (\mu) = \frac{1}{N}\sum_{i=1}^{N}(Timestamp_i - Timestamp_{i-1})$$
*   Il semaforo di urgenza classifica i prodotti:
    *   **Rosso (Esaurito)**: Giorni trascorsi dall'ultimo acquisto $\geq \mu$. Richiede acquisto immediato.
    *   **Giallo (Esaurimento Imminente nelle 48h)**: Giorni trascorsi $\geq (\mu \times 0.8)$.
    *   **Verde (Sufficiente)**: Giorni trascorsi inferiore alla soglia di sicurezza.
*   **Flessibilità Temporale**: L'utente nella Home può impostare un orizzonte temporale rapido ("Vista Giornaliera" per la spesa al rientro vs "Vista Settimanale/Mensile" per l'approvvigionamento cumulativo).

### 6.2 Regola dei "3 Strikes" (Aggiornamento Passivo della Marca Preferita)
Per l'autocompletamento assistito delle liste spesa (*Smart-Lookup*), il sistema deduce passivamente le transizioni tra le marche dei consumabili.

```
                  [ Preferito: Granarolo ]
                             |
                   ( Compra Coop - Strike 1 )
                             |
                   ( Compra Coop - Strike 2 )
                             |
                   ( Compra Coop - Strike 3 )
                             |
                             v
                  [ Preferito: Coop (Aggiornato) ]
```

Se la famiglia sostituisce temporaneamente una marca (es. a causa di esaurimento stock o promozioni temporanee), il sistema mantiene la marca storica indicizzata nello Smart-Lookup. Tuttavia, se l'anomalia si ripete per **tre volte consecutive (3 Strikes Rule)**, il sistema effettua in modo autonomo e silente la transizione del prodotto preferito sulla nuova marca ("Coop"), azzerando il contatore e mantenendo l'autocompletamento aggiornato alle reali preferenze d'uso.

---

## 7. Collaborazione Familiare, Contabilità Condivisa e Sincronizzazione Real-Time

### 7.1 Lo Smart-Split della Spesa
Al momento dell'acquisizione di uno scontrino, il sistema elenca le singole voci abilitando un sistema di split selettivo:
*   Con un singolo tocco, l'utente smista gli acquisti personali (es. beni intimi, regali privati, spese aziendali) nello **Spazio Privato** e i beni comuni (es. pane, frutta, sgrassatore) nello **Spazio Casa/Condiviso**.
*   I beni nello spazio privato rimangono permanentemente nascosti e crittografati sul dispositivo, invisibili al partner o al gruppo familiare, garantendo la sovranità personale.
*   L'utente può configurare un profilo predittivo globale per categoria o insegna (es. `"Invia sempre tutto a Spazio Casa"`) per completare il flusso con un tap rapido ("OK").

### 7.2 Ledger Finanziario Integrato e Smart-Sync WebSocket
*   **Compensazione delle Spese (Ledger)**: All'indicazione del pagatore dello scontrino condiviso, l'algoritmo calcola i bilanci incrociati degli acquisti domestici. Piuttosto che scambiare decine di piccoli pagamenti singoli, il ledger calcola i totali a fine mese ed espone una compensazione lineare netta e trasparente: `"L'utente A deve effettuare un bonifico di 30,00€ a favore dell'utente B"`.
*   **Sincronizzazione Unificata (Smart-Sync)**: Utilizzando un'infrastruttura WebSocket asincrona, nel momento in cui un familiare effettua una spesa spuntando l'articolo "Latte" o scansionando uno scontrino contenente tale voce, l'articolo viene **istantaneamente eliminato o aggiornato** su tutti i dispositivi dei membri dello stesso Household Pool, impedendo doppioni in corsia.

---

## 8. Logistica Intelligente, Mappe Inverse e Costo Opportunità

SmartGrocery Manager funge da vero e proprio navigatore economico-domestico.

### 8.1 Lista Inversa e Corrispondenza Geografica
L'utente immette i prodotti desiderati indipendentemente dal negozio. Quando è in movimento, l'applicazione interroga il database silenziosamente:
*   Al rilevamento dell'ingresso nel Geofence (50m) del supermercato, l'applicazione riordina la lista spesa disponendo i prodotti secondo la disposizione fisica stimata dei corridoi di quel punto vendita, pre-popolando i prezzi attivi rilevati dal crowdsourcing globale.
*   **Semaforo scostamento prezzi storico**: A schermo compaiono flag di avviso: colore **Verde** se il prezzo è allineato/inferiore, colore **Rosso** se il prezzo del prodotto nel negozio attuale presenta un ricarico inflazionistico o indice di shrinkflation superiore del 10% rispetto alla media degli scontrini accumulati in archivio.

### 8.2 Teorema del Costo Opportunità e Soglia di Indifferenza
Nel momento di pianificare la spesa, l'algoritmo analizza se convenga o meno frazionare gli acquisti tra più punti vendita vicini tenendo conto del tempo di tragitto extra (tempo stimato, traffico) e del consumo di carburante dell'auto.

```
  Soglia di Indifferenza impostata: €2.00
  LISTA: Latte, Caffè, Detersivo, Biscotti
  --------------------------------------------------------------------------
  OPZIONE A (Massimo Risparmio - 2 Tappe: Eurospin + Conad)
  Risparmio teorico sugli articoli: €3.40
  Costo carburante per 5.2 km extra: €0.85
  Valutazione Opportunità: €3.40 - €0.85 = €2.55 (Risparmio Netto)
  * Risparmio Netto (€2.55) > Soglia di Indifferenza (€2.00) ---> CONSIGLIATA!

  OPZIONE B (Massimo Comfort - 1 Tappa: Solo Conad)
  Sovrapprezzo articoli: €2.10
  * Il costo del tempo di viaggio compensa il sovrapprezzo ---> Mostra confronto.
```

L'app non impone alcuna decisione ma mostra una scheda di confronto pulita ed asimmetrica per lasciare all'utente la scelta strategica finale.

### 8.3 Rilevazione Micro-Retailers per Prossimità (Zero Scan)
Nelle spese tradizionali di rione (panificio, macelleria, fruttivendolo rionale), scontrini dettagliati non sono quasi mai disponibili per OCR o mancano codici a barre. L'applicazione rileva l'uscita da questi geofence di rione ed invia una notifica discreta: `"Sei appena uscito dal Fornaio di quartiere? Inserisci il totale della spesa"`. L'utente digita un importo rapido (es. `"4.50€"`) con un solo tocco; l'app imputa il totale al budget mensile escludendo la catalogazione dei singoli articoli, garantendo l'integrità del computo monetario domestico.

---

## 9. Resilienza Offline-First, UX dei Widget e Export GDPR

*   **Conflict-Free Replicated Data Types (CRDT)**: L'architettura esclude la dipendenza dalla connettività in tempo reale all'interno dei locali schermati o sotterranei. Le liste spesa e lo stato degli articoli utilizzano algoritmi CRDT per evitare conflitti di modifica. Non appena il segnale radio (4G/Wi-Fi dei negozi) viene recuperato, la coda di sincronizzazione locale asincrona effettua il merge con il database cloud.
*   **Widget Home ad Accesso Rapido (Jetpack Glance)**: Widget essenziale con pulsante allargato di scatto rapido scontrino (conduce direttamente alla fotocamera, bypassando l'autenticazione completa) ed esposizione rapida della visualizzazione di urgenza del "Daily Need".
*   **Esportazione Dati CSV/PDF (GDPR)**: Sezione impostazioni delegata all'esportazione completa a tutela dell'utente ed a riprova dell'assenza di vendor lock-in e conservazione trasparente delle informazioni personali.

---

## 10. Flusso Operativo Integrato e Definitivo

1.  **Fase 1 (Pianificazione Silenziosa)**: L'algoritmo predittivo colora in rosso il Latte (Daily Need). L'utente digita `"Fet"`, lo Smart-Lookup autocompila `"Fette Biscottate Misura"` (3-Strikes Rule). L'app valuta il Costo Opportunità e suggerisce il supermercato Y come tappa unica.
2.  **Fase 2 (Acquisto Offline)**: L'utente si reca fisicamente al supermercato Y. L'applicazione va offline. Utilizza la scorciatoia sul Widget per inquadrare con l'AR l'etichetta di un nuovo caffè in offerta (l'Auto-Trigger cattura lo scatto nitido non appena rileva la stabilità e la presenza dei prezzi). Lo aggiunge alla lista. Nota sul display che le fette biscottate mostrano un prezzo rosso superiore rispetto alla media storica e decide di non acquistarle.
3.  **Fase 3 (Sospensione e Sincronizzazione)**: L'utente paga ed esce. Il geofence rileva l'allontanamento e notifica: `"Vuoi caricare lo scontrino?"`. Avendo le mani occupate, l'utente clicca `"DOPO"`. Lo scontrino finisce nella coda `"Scontrini in sospeso"`. Al ripescaggio del segnale di rete, la lista sul server si aggiorna via WebSocket. A casa, l'utente estrapola lo scontrino in sospeso, avvia lo scan batch, l'OCR decodifica gli articoli correlando le stringhe con Jaro-Winkler e lo Smart-Split separa le spese private da quelle di cassa, chiudendo il cerchio contabile domestico in totale sicurezza.

---

## 11. Stato dell'Arte Corrente e Piano di Transizione ad Architettura Ibrida

Il sistema è attualmente configurato con un'architettura transitoria scalabile, pronta per evolvere verso una piattaforma enterprise decentralizzata. Di seguito sono dettagliati lo stato corrente implementato e le specifiche tecniche della futura infrastruttura back-end.

### 11.1 Stato di Implementazione Attuale (Client-Side)
L'applicazione SmartGrocery Manager adotta una solida logica di elaborazione locale assistita da API generative basandosi sulle seguenti soluzioni software:
1.  **OCR Locale (Google ML Kit)**: L'applicazione esegue l'acquisizione dello scontrino in locale sul dispositivo dell'utente ed elabora il rilevamento del testo tramite il motore on-device di *Google ML Kit*. Questo garantisce il funzionamento offline e l'assenza di invii pesanti di file multimediali sulla rete.
2.  **Validazione Pre-Filtro OCR e Rifiuto Immagini Vuote (Anti-Scrivania)**: Prima di trasmettere il testo alle API, il client convalida robustamente i contenuti per evitare falsi caricamenti causati da inquadrature casuali (come il legno di un tavolo). Se il testo non supera i controlli di lunghezza minima, presenza di numeri e parole chiave della GDO/fiscale, la transazione viene interrotta istantaneamente informando l'utente. I fallback di dati fittizi/inventati sono totalmente rimossi per garantire la massima veridicità storica dei dati.
3.  **Cloud Parsing Transitorio (Gemini 3.5 Flash)**: Il testo strutturato risultante dall'OCR viene convogliato al client interno del dispositivo (`GeminiServiceClient` in `GeminiService.kt`) che lo invia alla famiglia di modelli di default `gemini-3.5-flash` per la scomposizione semantica e la categorizzazione in formato JSON rigido.
4.  **Integrità del Database su Rimozione Spese (Deduplica Totale)**: Quando un utente cancella manualmente o ri-elabora una contabilità esistente, il sistema cancella a cascata nel database locale Room (`items` table) tutti gli articoli ad essa associati tramite timestamp e intestazione del negozio. Questo assicura che reinquadrare lo stesso scontrino carichi solo l'ultima transazione pulita, azzerando pericoli di duplicazione fantasma degli articoli.
5.  **Algoritmo di Riconciliazione e Deduplica (`GroceryViewModel.kt`)**: Al caricamento di un nuovo scontrino, il sistema interroga l'archivio locale per rilevare transazioni potenzialmente duplicate (stesso totale e stessa data d'acquisto). Invece di chiedere genericamente all'utente di sovrascrivere o eliminare la transazione precedente, l'algoritmo effettua una comparazione approfondita:
    *   **Confronto Semantico-Fiscale (`areItemListsSubstantiallySame`)**: Converte i set di articoli in liste ordinate per nome (lowercase/trimmed) e confronta i prezzi al livello di decimali e tolleranza ($|prezzo_A - prezzo_B| > 0.01$).
    *   **UX Condizionale Pre-Selezionata (`ScannerScreen.kt` + `showItemsList`)**: Il pulsante di riconciliazione "Sì, Integra" è pre-selezionato di default per velocizzare l'azione a due tocchi ("Frizione Zero"). La lista degli articoli scansionati rimane sempre visibile e navigabile (grazie a chiavi stabili assegnate a ciascun elemento in Compose) per consentire all'utente di modificare lo split di distribuzione spaziale privato o condiviso per ciascun nuovo articolo prima dell'invio. Si evita la scomparsa simultanea di righe non correlate negli elenchi visualizzati.

### 11.2 Piano Architetturale: Sistema Ibrido Android-Python-Llama3
Per abbattere i costi delle API Cloud a consumo (pay-per-token), mantenere un controllo totale sulla privacy e unificare la logica di business e la sincronizzazione globale degli account, l'architettura migrerà verso una configurazione ibrida Client-Server ad alte prestazioni.

```
+-------------------------------------+
| Android App (Client-Side)           |
| 1. ML Kit -> OCR Testo              |
| 2. Estrazione coordinate (X, Y)     |
| 3. Generazione e invio JSON Spaziale|
+------------------+------------------+
                   |
                   | (HTTPS Payload Leggero: No JPG/PNG)
                   v
+------------------+------------------+
| Python Back-end (FastAPI / Flask)   |
| 1. Ricezione JSON Spaziale          |
| 2. Formattazione Prompt Line-by-Line|
+------------------+------------------+
                   |
                   | (Host Local / Unix Socket)
                   v
+------------------+------------------+
| On-Premise LLM Engine (Llama 3)     |
| 1. Interpretazione allineamenti     |
| 2. Estrazione Entità e Metadati     |
| 3. Generazione Strutturata JSON     |
+-------------------------------------+
```

#### A. Acquisizione e Pre-elaborazione (Client-Side - Android)
*   Anziché inviare file d'immagine pesanti (es. foto scattate a 12 Megapixel con peso superiore a 5MB):
*   L'applicazione Android esegue la rilevazione dei blocchi testuali tramite **Google ML Kit OCR** in locale.
*   Ogni singola riga o parola rilevata viene mappata spazialmente calcolandone le coordinate geometriche relative: il rettangolo di delimitazione Bounding Box (proprietà native di ML Kit `boundingBox: Rect`).
*   L'app converte il tutto in un payload JSON standard compatto (dimensione media < 15KB) strutturato in nodi spaziali:
    ```json
    {
      "metadata": { "imageWidth": 1080, "imageHeight": 2400 },
      "elements": [
        { "text": "BIRRA MENABREA 66CL", "x": 45, "y": 280, "w": 400, "h": 35 },
        { "text": "1.89", "x": 890, "y": 280, "w": 80, "h": 35 }
      ]
    }
    ```
*   **Vantaggio cost/efficiency**: Zero consumo di banda cellulare e latenza di caricamento istantanea.

#### B. Back-end Server-Side in Python (FastAPI o Flask)
La scelta d'elezione per lo sviluppo del back-end è un servizio web **FastAPI** scritto in Python per i seguenti vantaggi:
1.  **Asincronia Nativa (ASGI)**: Consente di gestire contemporaneamente migliaia di richieste di scansione concorrenti e comunicazioni WebSocket real-time con minima impronta di memoria.
2.  **Validazione Rigida (Pydantic)**: Consente la definizione di schemi di input e output sicuri a compile-time e runtime, rifiutando richieste strutturate male.
3.  **Auto-Documentazione OpenAPI (Swagger)**: Esposizione nativa dell'UI di test del backend, azzerando i disallineamenti di contratto tra sviluppatori client e server.

#### C. Intelligenza Semantica e Spaziale (Llama 3 On-Premise)
Il server Python riceve il JSON spaziale e lo elabora per generare il prompt contestuale per **Llama 3** (es. servito via Ollama, vLLM, o API locali dedicate):
*   **Gestione dell'allineamento spaziale**: L'algoritmo del back-end può raggruppare i blocchi di testo che condividono la stessa coordinata $y$ (allineandoli orizzontalmente) per ricreare fedelmente le colonne dello scontrino (es. Associando `"BIRRA MENABREA"` con `"1.89"`).
*   **Strutturazione tramite Llama 3**: Llama 3 riceve il testo ordinato e le indicazioni spaziali ed esegue un'estrazione deterministica in formato JSON strutturato, comprendendo le abbreviazioni del supermercato ed effettuando le seguenti mappature:
    *   **Dati Punto Vendita**: Nome catena (es. "Coop"), Indirizzo esatto (via, città, CAP), Partita IVA (PIVA), contatti telefonici.
    *   **Line-Item Merceologia**: Descrizione "esplosa" (es. `"PR CR S.DAN"` diviene `"Prosciutto Crudo San Daniele"`), categorizzazione merceologica (es. `"Salumi"`), marca riconosciuta.
    *   **Metatesto Quantitativo**: Rilevazione del peso o volume (es. gr, cl, litri), prezzo al chilogrammo/litro, quantità acquistate e unità base.
    *   **Contabilità & Budget**: Aliquote IVA per singola riga di spesa, sconti assoluti o percentuali, totale complessivo pagato, timestamp completo.

#### D. Risultati e Vantaggi Strategici del Systema Ibrido
*   **Economia di Esercizio**: Raggiungimento di costo marginale **pari a zero** per l'inferenza LLM. Nessun pedaggio a consumo a OpenAI o Google su base mensile.
*   **Privacy & Sovranità dei Dati**: Trattandosi di un'instanza locale o privata self-hosted Llama 3, nessun dettaglio sugli acquisti del nucleo familiare (abitudini alimentari, brand preferiti, orari di spesa) viene esposto a player pubblicitari terzi.
*   **Accuratezza Elevata**: Il modello neurale di Llama 3 è in grado di correggere asincronamente sfasamenti di riga causati da pieghe del scontrino o imperfezioni della fotocamera, interpretando il contesto del documento per inferire i decimali mancanti.
