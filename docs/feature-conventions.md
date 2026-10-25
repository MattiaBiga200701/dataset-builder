# Convenzioni di calcolo

## LOC

Righe fisiche contenenti codice, escludendo righe vuote e soli commenti. Il sorgente viene letto al commit della release, con cache del risultato per blob ID.

## LOC Touched

Somma delle righe fisiche aggiunte e cancellate in tutte le revisioni considerate, secondo `git log --numstat`. Comprende modifiche a commenti e righe vuote: non applica il filtro lessicale del calcolo LOC al diff.

- La cronologia degli snapshot selezionati viene indicizzata una volta per elaborazione: parent dei commit, aggiunte/cancellazioni e cambi di percorso. Non vengono lanciati log separati per file o metrica. Per release si legge soltanto l'elenco dei file dello snapshot, poi si elaborano le revisioni in memoria.
- Prima release: tutti i commit raggiungibili dal suo commit, inclusa la creazione iniziale dei file.
- Release successive: commit raggiungibili dal commit corrente ma non da quello precedente (`precedente..corrente`). La lista ricevuta dal controller deve essere in ordine di release e completa dall'inizio del dataset. Non e' una finestra basata sulle date; su rami divergenti l'intervallo segue la raggiungibilita' Git.
- Merge esclusi. Le modifiche dei commit non-merge raggiungibili vengono conteggiate; eventuali risoluzioni introdotte solo nel merge non vengono conteggiate.
- Rinomine: si segue nel grafo dei commit la storia dei percorsi precedenti, entro la finestra. Una rinomina pura contribuisce zero; con modifiche si sommano le sole righe aggiunte e cancellate. Si indicizzano rinomine e copie riconosciute da Git con similarita' 50%, includendo le sorgenti non modificate (`--find-copies-harder`), per ricostruire anche le origini che `git log --follow` riconosce come copie. File simili possono avere un'origine ambigua: il riconoscimento rimane euristico come quello di Git.
- Percorso senza modifiche nell'intervallo: valore zero.
- Le coppie `int[]` hanno sempre due elementi: indice 0 = aggiunte, indice 1 = cancellazioni. Sono raggruppate per percorso, senza entity aggiuntive.
- File Java rilevati da Git come binari: errore esplicito, senza inventare un conteggio.

Il DAO copia il CSV di base nel file di output e legge la copia una sola volta. Righe e indici sono attributi del DAO, senza una classe interna CsvDataset. Il controller calcola le metriche e il DAO aggiorna le celle per progetto, percorso e release, poi salva la copia. Il file finale conserva ordine delle righe e celle non aggiornate. In caso di errore durante il calcolo, la copia resta con i valori di base; il file originale non viene modificato.

`GitHistoryCheck` confronta un campione distribuito sulle 12 release dello snapshot locale con il precedente `git log --follow`. Con `--fixture` verifica intervalli e rinomine su un repository temporaneo; con `--pipeline` verifica tutte le 9.864 entry, preservazione del CSV e LOC contro StormDataset, scrivendo soltanto un output temporaneo. Il mapping del test STORM e' specifico dello snapshot corrente (0.10.0 non inclusa nel dataset base).
