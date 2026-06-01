# Integrazione Open Food Facts & Cervello Globale Crowdsourced 🧠🛍️

Questo documento descrive in dettaglio l'architettura tecnica, le scelte di design, i modelli del database, gli endpoint API e le linee guida di integrazione client-server sviluppate per la gestione della conoscenza condivisa di prodotti, codici a barre (EAN-13), e prodotti freschi da bilancia (in-store) in **SmartGrocery Manager**.

---

## 🎯 1. Visione di Piattaforma: Server-Side Crowdsourcing

Per eliminare ogni frizione nell'acquisizione degli scontrini OCR e arricchire automaticamente la base di dati, abbiamo implementato un **motore centralizzato lato server** di mappatura semantica.

### Flusso Concettuale
1. **Scansione OCR**: L'app mobile inquadra uno scontrino e rileva righe di testo grezzo (es. `“PASS.POM.MUTTI 700G”`).
2. **Risoluzione Semantica**: Il server intercetta la riga e cerca un'associazione nota a livello globale.
   - **Latenza Zero**: Se l'associazione è già nota, restituisce immediatamente il codice EAN-13 e le specifiche di Open Food Facts (Nutri-Score, NOVA, allergeni, peso) salvate in cache.
   - **Text Search & Fuzzy Matching**: Se la riga è sconosciuta, il server effettua una ricerca testuale su **Open Food Facts (OFF)**, calcola la somiglianza di stringa (fuzzy matching > 75%) e ne memorizza temporaneamente l'associazione.
3. **Autocorrezione Autonoma (Self-Healing)**: Se l'associazione automatica è errata, gli utenti possono correggerla. La reputazione del match si aggiorna in tempo reale. Se le contestazioni superano le conferme, il server corregge automaticamente il legame globale per tutti gli utenti della piattaforma.

---

## 🗄️ 2. Modelli del Database (FastAPI SQLite)

Nel backend (`models.py`), abbiamo introdotto due tabelle per supportare questo ecosistema:

### A. Mappa Globale OCR - Codici a Barre (`global_ocr_barcode_map`)
Mantiene l'anagrafica delle mappature condivise a livello di intera piattaforma tra righe OCR normalizzate in minuscolo e codici a barre standard EAN-13.
* **`raw_ocr_name`** (VARCHAR, Primary Key): Il nome normalizzato in minuscolo dell'articolo letto dallo scanner OCR.
* **`barcode`** (VARCHAR): Il codice EAN-13 associato.
* **`confirmed_count`** (INTEGER): Conteggio delle conferme d'acquisto o approvazioni manuali degli utenti.
* **`rejected_count`** (INTEGER): Conteggio delle contestazioni (quando un utente associa manualmente un barcode diverso).
* **`last_used`** (DATETIME): Ultimo utilizzo del match per monitoraggio popolarità.

### B. Catalogo Prodotti Locali del Supermercato (`store_local_catalog`)
Gestisce i prodotti freschi o a peso variabile pesati al banco o alla bilancia. Poiché questi codici a barre variano per ciascun supermercato e non esistono su Open Food Facts, vengono isolati in questa tabella.
* **`id`** (INTEGER, Primary Key)
* **`store_id`** (INTEGER, Foreign Key a `stores`): Identificatore del supermercato.
* **`internal_product_code`** (VARCHAR): Codice articolo a 6 cifre estratto dall'EAN prefisso 2.
* **`product_name`** (VARCHAR): Nome del prodotto fresco (es. "Mortadella IGP").
* **`category`** (VARCHAR): Categoria del banco (default: 'Gastronomia').
* **`price_per_kg`** (FLOAT, Opzionale): Prezzo storico al kg per stime di costo.

*Vincolo di Unicità*: La combinazione di `(store_id, internal_product_code)` è unica, consentendo a negozi diversi di usare la stessa codifica interna per prodotti diversi senza collisioni.

---

## ⚖️ 3. Decodifica Codici a Barre da Bilancia (GS1 Prefisso 2)

Per evitare query inutili ad Open Food Facts e supportare i prodotti freschi venduti a peso, il server implementa il parser standard GS1 per i codici a barre nazionali che iniziano con le cifre da `20` a `29`.

### Struttura Standard EAN-13 con Prefisso 2:
```text
  2  [ 0 8 1 2 3 4 ]  [ 0 0 5 6 0 ]  [ 2 ]
  ┬   ──────┬──────    ──────┬──────   ─┬─
  │         │                │          └─ Check Digit (Cifra di controllo)
  │         │                └──────────── Valore Variabile (Peso: 0.560 Kg o Prezzo: 5.60 €)
  │         └───────────────────────────── Codice Articolo Interno del Negozio (6 cifre)
  └─────────────────────────────────────── Identificatore GS1 Uso Interno (Prefisso 20-29)
```

### Logica di Intercettazione nel Server
Se il barcode inviato al server ha lunghezza $\ge 12$ cifre e inizia con cifre comprese tra `20` e `29`:
1. Viene identificato come **in-store barcode** (bilancia).
2. Viene estratto il **codice articolo interno** (cifre alla posizione 2-8).
3. Viene estratto il **valore variabile** (peso in kg, es. `00560` $\rightarrow$ `0.560 kg`).
4. Il server bypassa Open Food Facts e cerca il codice in `store_local_catalog` filtrando per il supermercato specifico (`store_id`).
5. Se trovato, restituisce le informazioni del prodotto locale con il peso effettivo decodificato.

---

## 🔄 4. Specifica dei Contratti API del Backend (FastAPI)

Tutti gli endpoint risiedono nel modulo `scan_router.py` sotto il prefisso `/api/v1/scan`.

### A. Risoluzione Semantica Articolo
Risolve una riga OCR o un codice a barre interrogando in cascata: catalogo locale bilancia, cache locale, mappa globale, ed infine Open Food Facts.
* **Rotta**: `POST /api/v1/scan/catalog/resolve`
* **Payload Richiesta**:
  ```json
  {
    "raw_ocr_name": "scamorza aff.",
    "barcode": "20081234005602",
    "store_id": 1
  }
  ```
* **Risposta (Esempio Prodotto Fresco da Bilancia)**:
  ```json
  {
    "source": "LOCAL_CATALOG",
    "barcode": "20081234005602",
    "product_name": "Scamorza Affumicata",
    "brand": "Banco Fresco",
    "weight": 0.56,
    "category": "Gastronomia",
    "nutriscore": null,
    "nova_group": null,
    "allergens": [],
    "is_fresh_item": true,
    "confidence": 1.0
  }
  ```
* **Risposta (Esempio Prodotto Standard da Open Food Facts)**:
  ```json
  {
    "source": "OPEN_FOOD_FACTS",
    "barcode": "8002200148858",
    "product_name": "Passata di pomodoro classica Mutti 700g",
    "brand": "Mutti",
    "weight": 0.7,
    "category": "Dispensa",
    "nutriscore": "a",
    "nova_group": 1,
    "allergens": ["Gluten", "Lactose"],
    "is_fresh_item": false,
    "confidence": 0.85
  }
  ```

### B. Registrazione Feedback Utente (Reputazione)
Invia la conferma o la correzione manuale da parte dell'utente per aggiornare l'algoritmo di auto-apprendimento.
* **Rotta**: `POST /api/v1/scan/catalog/feedback`
* **Payload Richiesta**:
  ```json
  {
    "raw_ocr_name": "pass.pom.mutti 700g",
    "barcode": "8002200148858",
    "user_confirmed": true
  }
  ```
* **Meccanismo di Autocorrezione**:
  * Se `user_confirmed` è `true`, incrementa `confirmed_count`.
  * Se l'utente associa un barcode differente per lo stesso nome OCR, incrementa `rejected_count`.
  * Se `rejected_count > confirmed_count`, l'associazione corrente viene sovrascritta con il nuovo codice a barre inserito dall'utente. La reputazione si resetta con `confirmed_count = 1` e `rejected_count = 0`.

### C. Registrazione Prodotto Fresco (Banco/Bilancia)
Consente agli utenti (o amministratori) di mappare un codice articolo da bilancia (a 6 cifre) al nome del rispettivo prodotto per quel supermercato.
* **Rotta**: `POST /api/v1/scan/catalog/register_local`
* **Parametri Query**:
  * `store_id` (int): ID del supermercato
  * `internal_product_code` (str): Codice di esattamente 6 cifre (es. "081234")
  * `product_name` (str): Nome del prodotto (es. "Scamorza Affumicata")
  * `category` (str, opzionale): Categoria (default: "Gastronomia")
  * `price_per_kg` (float, opzionale): Prezzo storico al kg
* **Risposta**:
  ```json
  {
    "message": "Prodotto da banco registrato con successo!"
  }
  ```

---

## 🧪 5. Suite di Test di Integrità e Verifica

Abbiamo scritto una suite di test completa in `test_catalog_resolution.py` ed integrata nel test runner generale `run_all_tests.py` (Suite 7) che valida:
1. **Correttezza del Decoder GS1**: Verifica che il prefisso `2` sia isolato correttamente e che il peso del prodotto fresco sia decodificato con precisione (es. `20081234005602` $\rightarrow$ `0.560 kg`, codice `081234`).
2. **Registrazione Locale**: Verifica che la rotta `/register_local` inserisca correttamente l'anagrafica del supermercato e che `/resolve` la ritrovi istantaneamente con sorgente `LOCAL_CATALOG`.
3. **Mappatura e Feedback**: Verifica che i match semantici vengano memorizzati in `global_ocr_barcode_map`.
4. **Self-Healing**: Simula scenari di crowdsourcing in cui una serie di contestazioni da parte degli utenti ribalta un accoppiamento errato, allineando automaticamente il database globale al valore corretto.

---

## 📱 6. Guida per l'Integrazione in AI Studio (Frontend Android)

### A. Integrazione nel Modulo Scanner
Quando l'app Android invia lo scontrino per l'analisi OCR spaziale biometrica e riceve l'elenco degli articoli, per ciascun articolo rilevato:
1. Chiama `POST /api/v1/scan/catalog/resolve` passando il nome grezzo rilevato dall'OCR ed il `store_id` corrente.
2. Riceve la risposta contenente le proprietà nutrizionali (Nutri-Score, NOVA) ed eventuali allergeni.
3. Se l'utente edita l'articolo o scansiona un codice a barre manuale sovrascrivendo l'associazione, invoca immediatamente in background `POST /api/v1/scan/catalog/feedback` per aggiornare il Cervello Globale.

### B. Gestione Badge in Jetpack Compose
* Se `confidence` restituita da `/resolve` è inferiore a `0.8` (o `source` è `NONE`), mostra un piccolo badge discreto affianco alla riga articolo in Compose (es. *"Associa Codice a Barre"* o un'icona punto interrogativo arancione).
* Al click, l'app apre la fotocamera per scansionare il codice a barre reale della confezione. Una volta letto, invoca `/feedback` con `user_confirmed = true`. Questo pulirà l'interfaccia e insegnerà al server il match corretto per tutti gli scontrini futuri.

---

> [!NOTE]
> Questa architettura centralizzata non solo velocizza l'inserimento quotidiano della spesa per i coinquilini, ma crea una solida base per report nutrizionali e alert allergeni integrati nel co-housing.
