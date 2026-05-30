# Aggiornamento UI e Refactoring Locale - Sync per Backend

Ciao Antigravity. Ti scrivo per farti un resoconto del lavoro svolto finora sul client locale e per capire quali sono i prossimi passi per portare avanti l'integrazione del nuovo refactoring.
Ho provveduto a fare un pull del repository con l'ultima versione stabile e ho impostato il nuovo IP.
Ti prego di **effettuare tu stesso un `git pull`** del repository di frontend in modo da allinearti alle mie ultime modifiche.

## Cosa abbiamo portato a termine finora:
1. **Centro Notifiche & Push**:
   - Spostato tutto il centro notifiche in un elegante `ModalBottomSheet` accessibile tramite la "campanella" in Home.
   - Implementato `BadgedBox` per visualizzare in tempo reale le notifiche non lette. Le notifiche lette adesso godono di un marcato differenziale estetico (`alpha = 0.5f`).
   - Alzata a `IMPORTANCE_HIGH` la priority del channel delle notifiche push per avere i pop-up heads-up in app.
   - Reso reattivo in tempo reale con polling a 20s che richiama il `MasterSyncWorker`.
2. **Struttura Dati per Gruppi & Liste Condivise**:
   - Create le entity `SpendingGroup` (con UUID, nome e id creatore) e `ShoppingList`.
   - Modificato `LedgerEntry` (lo scontrino) aggiungendo `groupId` e `paidByUserId` per poter assegnare la spesa dinamicamente ai gruppi e a singoli utenti.
   - Aggiunto `listId` a `GroceryItem` per associare l'articolo scansionato ad un eventuale carrello separato.
   - Creati appositi `TypeConverters` nel database Room per serializzare/deserializzare agilmente le liste generiche utili alle condivisioni (es. `sharedWithGroupIds`).
3. **Draft delle Strutture UI**:
   - Iniziato il design base dei dialoghi e delle drop-down per lo `ScannerScreen` per selezionare a che Gruppo/Membro agganciare la lettura scontrino.
   - Pulito l'interfaccia scanner rimuovendo le icone delle impostazioni che portavano a confusione.

## Domande e Prossimi Passaggi per te (Backend):
- Quali sono le specifiche esatte e le URL finali per gli endpoint per la gestione del `profile_code` e dei `gruppi`? Puoi fornirmi lo schema di Response dettagliato del GET /api/v1/profile e POST /api/v1/groups?
- Come verranno trasmessi i `GroupMember` in risposta? Contiene già il `profile_code` del membro per potergli fare match con eventuali rubriche, oppure i membri visualizzeranno solo il loro UUID lato backend?
- Sei pronto con la base dati backend (SQLite) o dobbiamo testare con qualche stub su Postman preliminarmente?

Fammi sapere quando hai finito i deploy delle API e iniziamo l'integrazione del frontend service Layer!
