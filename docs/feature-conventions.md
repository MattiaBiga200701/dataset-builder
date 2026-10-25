# Convenzioni di calcolo

## LOC

Righe fisiche contenenti codice, escludendo righe vuote e soli commenti. Il sorgente viene letto al commit della release, con cache del risultato per blob ID.

## LOC Touched

Somma delle righe fisiche aggiunte e cancellate in tutte le revisioni considerate, secondo `git log --numstat`. Comprende modifiche a commenti e righe vuote: non applica il filtro lessicale del calcolo LOC al diff.

- Una sola lettura della cronologia per release, condivisibile con altre metriche.
- Prima release: tutti i commit raggiungibili dal suo commit, inclusa la creazione iniziale dei file.
- Release successive: commit raggiungibili dal commit corrente ma non da quello precedente (`precedente..corrente`). La lista ricevuta dal controller deve essere in ordine di release e completa dall'inizio del dataset. Non e' una finestra basata sulle date; su rami divergenti l'intervallo segue la raggiungibilita' Git.
- Merge esclusi. Le modifiche dei commit non-merge raggiungibili vengono conteggiate; eventuali risoluzioni introdotte solo nel merge non vengono conteggiate.
- Rinomine: rilevamento disattivato; considerate cancellazione del vecchio percorso e aggiunta del nuovo. Non si trasferisce al nuovo percorso la storia del vecchio.
- Percorso senza modifiche nell'intervallo: valore zero.
- Le coppie `int[]` hanno sempre due elementi: indice 0 = aggiunte, indice 1 = cancellazioni. Sono raggruppate per percorso, senza entity aggiuntive.
- File Java rilevati da Git come binari: errore esplicito, senza inventare un conteggio.

Il DAO copia il CSV di base nel file di output e legge la copia una sola volta. Righe e indici sono attributi del DAO, senza una classe interna CsvDataset. Il controller calcola le metriche e il DAO aggiorna le celle per progetto, percorso e release, poi salva la copia. Il file finale conserva ordine delle righe e celle non aggiornate. In caso di errore durante il calcolo, la copia resta con i valori di base; il file originale non viene modificato.
