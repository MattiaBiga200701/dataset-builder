# Convenzioni di calcolo

## LOC

Righe fisiche contenenti codice, escludendo righe vuote e soli commenti. Il sorgente viene letto al commit della release, con cache del risultato per blob ID.


Una classe nel dataset può avere LOC pari a un numero x e LOC touched pari a 0, poichè significa che magari in quella release la classe già esisteva e non è stata toccata.

Quindi mantenere La colonna LOC touched anche se ha molti 0 può essere utile perchè indica che una classe non è stata toccata quindi se fosse buggata in una release ed ha LOC touched pari a 0 continua ad essere buggata
## NR (Number of Revisions)

Numero di commit distinti che revisionano la classe nell'intervallo della release. La milestone 1 (pagina 4) ammette sia il calcolo nella release sia quello dalla release 0; qui si usa la stessa finestra di LOC Touched: tutta la storia raggiungibile per la prima release, `precedente..corrente` per le successive.

- Il commit di creazione conta una revisione. Un file non revisionato nell'intervallo ha NR = 0.
- Ogni commit conta una sola volta per classe, indipendentemente da righe, blocchi modificati e percorsi attraversati nel grafo. Non si filtrano i commit per ticket o bug fix.
- I merge sono attraversati ma esclusi dal conteggio, come per LOC Touched.
- Le rinomine pure e le altre variazioni registrate da Git con numstat con zero aggiunte e zero cancellazioni contano una revisione e contribuiscono zero a LOC Touched. Rinomine e copie seguono le stesse euristiche della cronologia condivisa.
- Il reader conserva una `Revision` per commit distinto; `NrCalculator` ne conta gli elementi. Il controller invoca il calcolatore e valorizza `DatasetEntry.nr`; il DAO verifica la colonna `NR` e salva il valore. Non si eseguono nuove scansioni Git per NR.
- Queste regole su merge, creazione e rinomine sono convenzioni del progetto, non dettagli prescritti dalla milestone.

## LOC Touched

Somma delle righe fisiche aggiunte e cancellate in tutte le revisioni considerate, secondo `git log --numstat`. Comprende modifiche a commenti e righe vuote: non applica il filtro lessicale del calcolo LOC al diff.

- La cronologia degli snapshot selezionati viene indicizzata una volta per elaborazione: parent dei commit, aggiunte/cancellazioni e cambi di percorso. Non vengono lanciati log separati per file o metrica. Per release si legge soltanto l'elenco dei file dello snapshot, poi si elaborano le revisioni in memoria.
- Prima release: tutti i commit raggiungibili dal suo commit, inclusa la creazione iniziale dei file.
- Release successive: commit raggiungibili dal commit corrente ma non da quello precedente (`precedente..corrente`). La lista ricevuta dal controller deve essere in ordine di release e completa dall'inizio del dataset. Non e' una finestra basata sulle date; su rami divergenti l'intervallo segue la raggiungibilita' Git.
- Merge esclusi. Le modifiche dei commit non-merge raggiungibili vengono conteggiate; eventuali risoluzioni introdotte solo nel merge non vengono conteggiate.
- Rinomine: si segue nel grafo dei commit la storia dei percorsi precedenti, entro la finestra. Una rinomina pura contribuisce zero; con modifiche si sommano le sole righe aggiunte e cancellate. Si indicizzano rinomine e copie riconosciute da Git con similarita' 50%, includendo le sorgenti non modificate (`--find-copies-harder`), per ricostruire anche le origini che `git log --follow` riconosce come copie. File simili possono avere un'origine ambigua: il riconoscimento rimane euristico come quello di Git.
- Percorso senza modifiche nell'intervallo: valore zero.
- Ogni `Revision` rappresenta un commit distinto nella storia della classe e conserva hash, messaggio completo, righe aggiunte e cancellate. Se piu' percorsi convergono nello stesso commit si aggregano le righe dei percorsi distinti in una sola revisione, preservando LOC Touched. I conteggi non possono essere negativi.
- File Java rilevati da Git come binari: errore esplicito, senza inventare un conteggio.

Scelta all'interno della stessa release.

Il DAO copia il CSV di base nel file di output e legge la copia una sola volta. Righe e indici sono attributi del DAO, senza una classe interna CsvDataset. Il controller calcola LOC, LOC Touched, NR, Nfix e Nauth e il DAO aggiorna le celle per progetto, percorso e release, poi salva la copia. Il file finale conserva ordine delle righe e celle non aggiornate. In caso di errore durante il calcolo, la copia resta con i valori di base; il file originale non viene modificato.

`GitHistoryCheck` confronta un campione distribuito sulle 12 release dello snapshot locale con il precedente `git log --follow`. Con `--fixture` verifica intervalli, rinomine pure, creazione, file invariati, NR e merge su un repository temporaneo; con `--pipeline` verifica tutte le 9.864 entry, preservazione del CSV e LOC contro StormDataset, scrivendo soltanto un output temporaneo. Il mapping del test STORM e' specifico dello snapshot corrente (0.10.0 non inclusa nel dataset base).

`FeatureCsvCheck` verifica anche la scrittura di NR con colonne e righe riordinate, una revisione `[0, 0]`, NR = 0 e il rifiuto di un CSV senza colonna NR. I controlli scrivono solo file temporanei; non rigenerano `output/Dataset.csv`.

## Nfix (Number of defect fixes)

Conta i commit distinti della classe collegati ad almeno uno degli ID restituiti da `FeatureComputingDAO`. La finestra e' la stessa di NR: storia raggiungibile nella prima release, `precedente..corrente` nelle successive. Non si usa la data di risoluzione Jira per assegnare un fix alla release.

- Il metodo `FeatureComputingDAO.getTicketIDs` deriva dallo script `SLIDES FALESSI/OneDrive_1_17-10-2026/18 - Milestone 1 - The Dataset/Using Jira and Git/GetTicketsID/GetTicketID/src/main/java/RetrieveTicketsID.java`.
- Come per `ReleaseDAO`, il progetto diventa parametro del metodo `getTicketIDs(String projName)` e la stampa diventa restituzione di una lista. Query, campi richiesti e ciclo `i`, `j`, `total` sono conservati. `readJsonFromUrl` e' un metodo protected di istanza per consentire verifiche offline.
- La query seleziona Bug, Closed oppure Resolved, risoluzione Fixed. Richiede anche resolutiondate, versions e created, ma lo script estrae soltanto key. La paginazione originale presume pagine da 1.000 elementi e usa `i % 1000`: non e' stata riscritta. Eventuali incompatibilita' del server vanno segnalate prima di modificarla.
- I ticket vengono recuperati una sola volta per elaborazione. Un errore HTTP/JSON interrompe il calcolo, senza produrre zeri fittizi.
- Il messaggio completo del commit viene confrontato con gli ID, senza distinzione tra maiuscole/minuscole e con delimitatori: STORM-12 non corrisponde a STORM-123 o XSTORM-12. Sono riconosciuti anche ID tra parentesi o nei link Jira.
- Un commit con piu' ID validi conta una volta. Commit distinti dello stesso ticket contano separatamente. Non si contano ticket distinti, righe o semplici occorrenze della parola fix.
- Merge esclusi; creazione e rinomine pure sono ammesse se collegate a un ticket valido, come le altre revisioni di NR. Nessun fix riconosciuto significa Nfix = 0; sempre 0 <= Nfix <= NR.
- Il matching e' una convenzione operativa: non riconosce fix privi di riferimenti e non distingue automaticamente una citazione accessoria o un revert da una correzione. Lo stato Jira e' quello al momento del recupero, non uno snapshot storico.
- Il calcolatore e' nel Model; il controller coordina il recupero da FeatureComputingDAO, il calcolo e la scrittura attraverso FeatureComputingDAO. I messaggi vengono letti nello stesso log Git delle altre metriche.

### Verifiche Nfix

`NfixCheck` verifica offline query e paginazione originali su 1.001 ticket simulati, propagazione degli errori, matching e unicita' dei commit. Con `--live` legge realmente gli ID da Jira senza scrivere file. `GitHistoryCheck --fixture` verifica anche messaggi multilinea, rinomine, intervalli, esclusione dei merge e percorsi convergenti. `FeatureCsvCheck` verifica valori Nfix, colonne riordinate e colonna mancante. `GitHistoryCheck --pipeline` usa ticket vuoti simulati; `--pipeline-live` usa FeatureComputingDAO reale e controlla 0 <= Nfix <= NR su tutto il dataset. Entrambe le pipeline producono soltanto un CSV temporaneo.

`Revision` indica il record annidato `GitHistoryReader.Revision`, nel package `utils`, coerente con `GitSourceReader.Source`. Trasporta i dati Git ai calcolatori e non appartiene a `model.entity`. La collocazione non cambia le regole di LOC Touched, NR o Nfix.

Per Nfix il flusso e' `FeatureComputingDAO -> FeatureComputingController -> NfixCalculator`: il controller estrae una `Map<String, String>` (hash del commit -> messaggio) dalle revisioni e passa anche il `Set<String>` degli ID. Il calcolatore applica matching e conteggio senza importare DAO o tipi del reader. La mappa garantisce una sola voce per commit.

## Nauth (Number of authors)

Numero di email Author distinte nelle revisioni della classe all'interno della release, usando la stessa finestra e storia di NR. Per la prima release si considera tutta la storia raggiungibile; nelle successive `precedente..corrente`. Merge esclusi, creazione e rinomine pure incluse. Nessuna revisione significa Nauth = 0.

- Si usa `%ae` (email Author originale), non il Committer, il nome o i trailer Co-authored-by. Non si applica mailmap: email diverse restano identita' distinte.
- Il calcolatore rimuove spazi iniziali/finali e normalizza in minuscolo con `Locale.ROOT`, poi inserisce le email in un `HashSet`: la stessa email conta una volta anche se appare in molti commit. La normalizzazione e' una convenzione del progetto.
- Un'email vuota o nulla causa un errore esplicito: non si inventano identita' o conteggi. Con dati validi, 0 <= Nauth <= NR e NR > 0 implica Nauth >= 1.
- Il reader recupera l'email nello stesso log gia' utilizzato; `Revision` resta un record tecnico annidato. Il flusso e' DAO -> controller -> NauthCalculator: il calcolatore riceve solo una lista di stringhe, senza dipendere dai tipi del reader. Il risultato passa attraverso `DatasetEntry.setNauth` alla scrittura del CSV.
- `NauthCheck` verifica duplicati, normalizzazione, identita' diverse, lista vuota ed email mancante. La fixture Git verifica anche Author diverso dal Committer, intervalli e rinomine; il test CSV verifica scrittura e colonna mancante. La pipeline controlla i vincoli su tutte le righe, usando output temporaneo.


ANCORA DA VERIFICARE SE CALCOLARLO NELLA RELEASE O DA RELEASE 0