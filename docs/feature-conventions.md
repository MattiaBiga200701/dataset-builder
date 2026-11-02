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
- Le rinomine pure e le altre variazioni registrate da Git con numstat `[0, 0]` contano una revisione e contribuiscono zero a LOC Touched. Rinomine e copie seguono le stesse euristiche della cronologia condivisa.
- Il reader conserva una coppia per commit distinto; `NrCalculator` ne conta gli elementi. Il controller invoca il calcolatore e valorizza `DatasetEntry.nr`; il DAO verifica la colonna `NR` e salva il valore. Non si eseguono nuove scansioni Git per NR.
- Queste regole su merge, creazione e rinomine sono convenzioni del progetto, non dettagli prescritti dalla milestone.

## LOC Touched

Somma delle righe fisiche aggiunte e cancellate in tutte le revisioni considerate, secondo `git log --numstat`. Comprende modifiche a commenti e righe vuote: non applica il filtro lessicale del calcolo LOC al diff.

- La cronologia degli snapshot selezionati viene indicizzata una volta per elaborazione: parent dei commit, aggiunte/cancellazioni e cambi di percorso. Non vengono lanciati log separati per file o metrica. Per release si legge soltanto l'elenco dei file dello snapshot, poi si elaborano le revisioni in memoria.
- Prima release: tutti i commit raggiungibili dal suo commit, inclusa la creazione iniziale dei file.
- Release successive: commit raggiungibili dal commit corrente ma non da quello precedente (`precedente..corrente`). La lista ricevuta dal controller deve essere in ordine di release e completa dall'inizio del dataset. Non e' una finestra basata sulle date; su rami divergenti l'intervallo segue la raggiungibilita' Git.
- Merge esclusi. Le modifiche dei commit non-merge raggiungibili vengono conteggiate; eventuali risoluzioni introdotte solo nel merge non vengono conteggiate.
- Rinomine: si segue nel grafo dei commit la storia dei percorsi precedenti, entro la finestra. Una rinomina pura contribuisce zero; con modifiche si sommano le sole righe aggiunte e cancellate. Si indicizzano rinomine e copie riconosciute da Git con similarita' 50%, includendo le sorgenti non modificate (`--find-copies-harder`), per ricostruire anche le origini che `git log --follow` riconosce come copie. File simili possono avere un'origine ambigua: il riconoscimento rimane euristico come quello di Git.
- Percorso senza modifiche nell'intervallo: valore zero.
- Ogni coppia rappresenta un commit distinto nella storia della classe. Se piu' percorsi seguiti convergono nello stesso commit, si aggregano le righe dei percorsi distinti in una sola coppia, preservando LOC Touched. Le coppie `int[]` hanno sempre due elementi: indice 0 = aggiunte, indice 1 = cancellazioni. Sono raggruppate per percorso, senza entity aggiuntive.
- File Java rilevati da Git come binari: errore esplicito, senza inventare un conteggio.

Scelta all'interno della stessa release.

Il DAO copia il CSV di base nel file di output e legge la copia una sola volta. Righe e indici sono attributi del DAO, senza una classe interna CsvDataset. Il controller calcola LOC, LOC Touched e NR e il DAO aggiorna le celle per progetto, percorso e release, poi salva la copia. Il file finale conserva ordine delle righe e celle non aggiornate. In caso di errore durante il calcolo, la copia resta con i valori di base; il file originale non viene modificato.

`GitHistoryCheck` confronta un campione distribuito sulle 12 release dello snapshot locale con il precedente `git log --follow`. Con `--fixture` verifica intervalli, rinomine pure, creazione, file invariati, NR e merge su un repository temporaneo; con `--pipeline` verifica tutte le 9.864 entry, preservazione del CSV e LOC contro StormDataset, scrivendo soltanto un output temporaneo. Il mapping del test STORM e' specifico dello snapshot corrente (0.10.0 non inclusa nel dataset base).

`FeatureCsvCheck` verifica anche la scrittura di NR con colonne e righe riordinate, una revisione `[0, 0]`, NR = 0 e il rifiuto di un CSV senza colonna NR. I controlli scrivono solo file temporanei; non rigenerano `output/Dataset.csv`.
