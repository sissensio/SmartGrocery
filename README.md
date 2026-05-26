# SmartGrocery Manager 🛒📱

Benvenuto nel repository di **SmartGrocery Manager**, l'applicazione mobile a "Frizione Zero" progettata per supportare la gestione intelligente delle spese, l'ottimizzazione del bilancio familiare e la tracciabilità delle scorte alimentari salvaguardando integralmente la privacy dell'utente.

---

## 📌 Visione del Progetto
SmartGrocery Manager è pensato come un **Assistente Silenzioso**. Minimizza l'interazione manuale raccogliendo passivamente dati dai comportamenti di spesa effettivi dell'utente (scansione scontrini, geofencing all'uscita dei punti vendita, e monitoraggio delle scorte).

- **Interazione Rapida**: Solo 2 tap per i flussi primari.
- **Privacy End-to-End**: Crittografia dei dati locali tramite SQLCipher (AES-256-CBC) e database locale isolato.
- **Sincronizzazione Familiare**: Condivisione asincrona in un pool domestico (Household Pool) per bilanci coordinati e riduzione di articoli doppioni.

---

## 🏗️ Architettura & Stato dell'Arte

Il progetto sta affrontando un'evoluzione tecnica guidata dalla sostenibilità economica e dalla tutela della privacy:

1. **Stato Corrente (On-Device OCR + Generative Parsing)**:
   - Acquisizione locale tramite **Google ML Kit OCR** per la massima performance offline.
   - Parsing semantico transitorio via API REST **Gemini 3.5 Flash** con formattazione JSON deterministica.
   - Sottosistema di deduplica e riconciliazione transazioni (`GroceryViewModel.kt`) con confronto profondo degli elenchi articoli (`areItemListsSubstantiallySame`) e gestione intelligente dell'interfaccia di merge (`ScannerScreen.kt`).

2. **Evoluzione Futura (Sistema Ibrido Android + Python FastAPI + Llama 3)**:
   - **Client**: ML Kit OCR locale estrae sia il testo sia le coordinate spaziali dei riquadri (`boundingBox`), generando un payload JSON ultra-leggero (<15KB). Nessuna immagine viene caricata in cloud.
   - **Backend**: Microservizio asincrono in **Python (FastAPI)** ad alte prestazioni ed elevata concorrenza.
   - **Inference**: Esecuzione on-premise/self-hosted di **Llama 3** guidata dai vincoli geometrico-spaziali dei blocchi OCR per estrapolare con precisione a costo zero:
     - Anagrafica punto vendita.
     - Articoli, marchi, pesi/volumi e categorie con risoluzione semantica delle abbreviazioni.
     - Aliquote IVA, sconti applicati e contabilità generale.

*Per i dettagli completi, leggi il master document di progettazione:* [**V4Pro Master Document (Dettaglio Tecnico)**](./V4Pro_Master_Document.md).

---

## 🚀 Come Compilare e Sviluppare (Android)

L'applicazione è sviluppata in **Kotlin** sfruttando **Jetpack Compose** per l'interfaccia utente dichiarativa e **Material Design 3 (M3)** per lo stile e i componenti visuali.

### Requisiti
- **Android Studio** (versione Koala o superiore consigliata)
- **Java Development Kit (JDK)**: JDK 17
- **Gradle**: Kotlin DSL (`.gradle.kts`)

### Comandi Gradle Comuni (da riga di comando)
*Nota: si raccomanda l'uso di `gradle` standard senza wrapper locale.*

- **Compila l'applicazione**:
  ```bash
  gradle assembleDebug
  ```
- **Esegui i Test Unitari e Robolectric**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Verifica i Test di Screenshot (Roborazzi)**:
  ```bash
  gradle :app:verifyRoborazziDebug
  ```
- **Registra nuovi Screenshot di riferimento**:
  ```bash
  gradle :app:recordRoborazziDebug
  ```

---

## 🐍 🖥️ Setup del Backend & Configurazione LAN IP Dinamica

Per abilitare il funzionamento sincrono dell'applicazione mobile (sia su emulatore che su dispositivi fisici) con il backend Python FastAPI in rete locale, il progetto condivide un file di configurazione di rete.

### 🛰️ 1. Configurazione Condivisa (`network_config.json`)
Il file `network_config.json` nella radice del progetto definisce l'IP del server:
```json
{
  "LOCAL_BACKEND_IP": "192.168.1.154"
}
```
* **Android**: Legge questo file a build-time in `app/build.gradle.kts` e inietta l'IP in `BuildConfig.LOCAL_BACKEND_IP`. Il client punta a `http://${BuildConfig.LOCAL_BACKEND_IP}:8000/api/v1/scan`.
* **Backend**: Espone l'IP configurato nelle API di diagnostica (root `/`) e si mette in ascolto su `0.0.0.0` per poter ricevere connessioni da qualsiasi dispositivo sulla stessa rete Wi-Fi (LAN).

### 🚀 2. Avvio del Backend Python
Per lanciare il server FastAPI in locale su Windows, esegui lo script PowerShell:
```powershell
.\backend\run.ps1
```
Lo script si occupa di inizializzare l'ambiente virtuale (`.venv`), installare le dipendenze da `requirements.txt` e lanciare il server FastAPI in ascolto su `0.0.0.0:8000` (accettando connessioni da qualsiasi IP della LAN).

### 🔄 3. Aggiornamento Automatico IP (`update_ip.py`)
Quando cambi rete o riavvii il router, il tuo IP locale potrebbe cambiare. Per evitare configurazioni manuali noiose, usa lo script helper Python nella radice:
```bash
python update_ip.py
```
**Cosa fa lo script:**
1. Rileva automaticamente l'indirizzo IP LAN attivo della tua macchina.
2. Aggiorna `network_config.json` solo se l'IP è cambiato.
3. Se aggiornato, esegue automaticamente `git add`, `git commit` e `git push` su GitHub, allineando sia l'ambiente Antigravity locale sia l'emulatore cloud di AI Studio!

---

## 🔐 Configurazione dei Segreti
Le chiavi API sensibili (come la chiave transitoria `GEMINI_API_KEY`) sono iniettate tramite il Gradle Secrets Plugin.
1. Inserisci i tuoi segreti personali direttamente nel pannello **Secrets** di Google AI Studio.
2. In locale (durante l'esportazione su Android Studio), i parametri verranno caricati tramite file `.env` senza mai esporre o hardcodare le chiavi nel codice sorgente o nei file di configurazione controllati da Git.
