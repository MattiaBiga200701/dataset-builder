# Dataset Builder — Class diagram MVC

Diagramma ricostruito dal codice Java attuale. Sono riportati gli attributi e i metodi principali; costruttori, getter/setter delle entità e metodi privati di supporto sono omessi.

```mermaid
classDiagram
direction TB

namespace Avvio {
    class Main {
        +main(String[] args)$ void
    }
}
namespace View {
    class DatasetCLI {
        +start() void
    }
}
namespace Controller {
    class FeatureComputingController {
        -FeatureComputingDAO featureComputingDAO
        -LocCalculator locCalculator
        -LocTouchedCalculator locTouchedCalculator
        -NrCalculator nrCalculator
        -NfixCalculator nfixCalculator
        -NauthCalculator nauthCalculator
        +createDatasetCsv(List~ReleaseDataset~ datasets) Path
    }
    class BuildDatasetController {
        -DatasetEntryDAO datasetEntryDAO
        -ReleaseDAO releaseDAO
        -ReleaseDatasetDAO releaseDatasetDAO
        +getReleaseDataset() List~ReleaseDataset~
        +getReleaseDataset(String projectName) List~ReleaseDataset~
        +getSelectedReleases(String projectName) List~Release~
        +createBaseCsv(List~ReleaseDataset~ datasets) Path
    }
}
namespace Model_DAO {
    class ReleaseDAO {
        +getReleases(String projName) List~Release~
    }
    class ReleaseDatasetDAO {
        -Path repositoryPath
        +getRepositoryPath() Path
        +getCommitHash(Release release) String
        +createReleaseDataset(Release release, String commitHash, List~DatasetEntry~ entries) ReleaseDataset
        +createBaseCsv(List~ReleaseDataset~ datasets) Path
    }
    class DatasetEntryDAO {
        -Path repositoryPath
        +setProjectName(String projectName) void
        +getEntries(Release release, String commitHash) List~DatasetEntry~
    }
    class FeatureComputingDAO {
        +getTicketIDs(String projName) List~String~
        -GitSourceReader sourceReader
        -GitHistoryReader historyReader
        +readBaseCsv(Path input, Path output) void
        +prepareHistory(List~String~ commits) void
        +getChanges(String previousCommit, String currentCommit) Map
        +getSource(String commitHash, String classPath) Source
        +updateEntry(DatasetEntry entry) void
        +writeDatasetCsv() void
        +close() void
    }
}
namespace Model_Entity {
    class Release {
        -Integer index
        -String versionId
        -String versionName
        -LocalDateTime date
    }
    class ReleaseDataset {
        -Release release
        -String commitHash
        -List~DatasetEntry~ entries
    }
    class DatasetEntry {
        -String projectName$
        -String className
        -Release release
        -Integer loc
        -Integer locTouched
        -Integer nr
        -Integer nfix
        -Integer nauth
        -Integer locAdded
        -Integer maxLocAdded
        -Double averageLocAdded
        -Integer churn
        -Integer maxChurn
        -Double averageChurn
        -Integer changeSetSize
        -Integer maxChangeSet
        -Double averageChangeSet
        -Double age
        -Double weightedAge
        -Double meanNs
        -Double meanLa
        -Double meanLd
        -Double meanNd
        -Integer nSmells
        -String bugginess
    }
}
namespace Model_Calcolo {
    class LocCalculator {
        +calculate(String source) int
    }
    class LocTouchedCalculator {
        +calculate(List~Revision~ revisions) int
    }
    class NauthCalculator {
        +calculate(List~String~ authorEmails) int
    }
    class NfixCalculator {
        +calculate(Map~String,String~ messagesByCommit, Set~String~ fixedBugKeys) int
    }
    class NrCalculator {
        +calculate(List~Revision~ revisions) int
    }
}
namespace Utils {
    class Revision {
        <<record>>
        +String commitHash
        +String message
        +String authorEmail
        +int added
        +int deleted
    }
    class GitSourceReader {
        +read(String commit, String path) Source
        +close() void
    }
    class GitHistoryReader {
        +prepareHistory(List~String~ commits) void
        +readChanges(String previousCommit, String currentCommit) Map
        +clear() void
    }
    class Feature {
        <<enumeration>>
        -String columnName
        +getColumnName() String
    }
}

Main ..> DatasetCLI : crea e avvia
DatasetCLI ..> BuildDatasetController : crea e invoca
DatasetCLI ..> ReleaseDataset : legge risultati
BuildDatasetController --> "1" ReleaseDAO : usa
BuildDatasetController --> "1" ReleaseDatasetDAO : usa
BuildDatasetController --> "1" DatasetEntryDAO : usa
BuildDatasetController ..> Release : seleziona primo 34%
BuildDatasetController ..> ReleaseDataset : restituisce
ReleaseDAO ..> Release : crea da Jira
ReleaseDatasetDAO ..> Release : risolve tag Git
ReleaseDatasetDAO ..> ReleaseDataset : crea ed esporta
ReleaseDatasetDAO ..> DatasetEntry : legge per CSV
ReleaseDatasetDAO ..> Feature : ricava colonne CSV
DatasetEntryDAO ..> DatasetEntry : crea da snapshot Git
DatasetEntryDAO ..> Release : assegna alle entry
ReleaseDataset --> "0..1" Release : release
ReleaseDataset --> "0..*" DatasetEntry : entries
DatasetEntry --> "0..1" Release : release
DatasetCLI ..> FeatureComputingController : crea e invoca
FeatureComputingController --> "1" FeatureComputingDAO : accesso ai dati
FeatureComputingController --> "1" LocCalculator : delega LOC
FeatureComputingController --> "1" LocTouchedCalculator : delega LOC Touched
FeatureComputingController --> "1" NrCalculator : delega NR
FeatureComputingController --> "1" NfixCalculator : delega Nfix
FeatureComputingController --> "1" NauthCalculator : delega Nauth
GitHistoryReader ..> Revision : crea
FeatureComputingController ..> ReleaseDataset : itera snapshot
FeatureComputingController ..> DatasetEntry : valorizza metriche
FeatureComputingDAO --> "0..1" GitSourceReader : legge sorgenti
FeatureComputingDAO --> "1" GitHistoryReader : legge revisioni
FeatureComputingDAO ..> DatasetEntry : aggiorna CSV
FeatureComputingDAO ..> Feature : identifica colonne
```

## Responsabilità MVC

| Livello | Classi | Responsabilità |
| --- | --- | --- |
| View | `DatasetCLI` | Gestisce menu, input, messaggi, errori e visualizzazione del risultato. |
| Controller | `BuildDatasetController` | Coordina i DAO, seleziona il primo 34% delle release e avvia la costruzione/esportazione del dataset. |
| Model — DAO | `ReleaseDAO`, `ReleaseDatasetDAO`, `DatasetEntryDAO` | Recuperano release da Jira, commit e percorsi Java da Git e scrivono il CSV. |
| Model — entità | `Release`, `ReleaseDataset`, `DatasetEntry` | Rappresentano release, snapshot del dataset e righe con metriche. |
| Controller | `FeatureComputingController` | Coordina lettura, calcolo delle feature e salvataggio. |
| Model: accesso ai dati | `FeatureComputingDAO`, `GitSourceReader`, `GitHistoryReader` | Gestiscono ticket Jira, CSV, sorgenti e cronologia condivisa. |
| Model: calcolo | `LocCalculator`, `LocTouchedCalculator`, `NrCalculator`, `NfixCalculator`, `NauthCalculator` | Calcolano le metriche senza dipendenze dalla View. |
| Supporto | `Main`, `Feature` | Avvio dell'applicazione e definizione dei nomi delle colonne. |


## Lettura delle relazioni

- Le frecce tratteggiate indicano dipendenze: creazione, invocazione o uso temporaneo di oggetti.
- Le frecce continue indicano riferimenti conservati negli attributi. Non viene usata la composizione perché il codice non impone proprietà esclusiva o gestione del ciclo di vita delle entità.
- `0..1` riflette i riferimenti alle entità inizialmente null e assegnabili tramite setter. Nel flusso di costruzione completato ogni dataset e ogni entry ricevono una release.
- `projectName` è statico: è condiviso da tutte le istanze di `DatasetEntry`.
- La View legge le entità per contare le entry; i dati vengono quindi restituiti alla View tramite il Controller, senza un meccanismo di notifica/Observer.
- Il primo caso d'uso produce `dataset_base.csv` con le feature vuote; il secondo produce `Dataset.csv` con LOC, LOC Touched, NR, Nfix e Nauth valorizzati.
- `metrics` appartiene logicamente al Model. I lettori Git sono nel package `utils`; `Map` indica `Map<String, List<Revision>>`, con una `Revision` per commit distinto (hash, messaggio, aggiunte e cancellazioni), incluse revisioni a zero righe.
- Sono omessi gli attributi interni del DAO per indicizzare il CSV e il campo legacy `releaseDataset`, estraneo al flusso di calcolo.
- Il dettaglio del secondo caso d'uso e' in [class-diagram-uc2-mvc.md](class-diagram-uc2-mvc.md); le regole delle metriche sono in [feature-conventions.md](feature-conventions.md).

## Ticket e Nfix

Il metodo `FeatureComputingDAO.getTicketIDs` adatta lo script originale `Using Jira and Git/GetTicketsID/GetTicketID/src/main/java/RetrieveTicketsID.java`, come `ReleaseDAO` adatta lo script delle release: progetto parametrico e lista restituita al controller. Query, campi e paginazione originali rimangono invariati. Non viene introdotta un'entita Ticket, perche' per Nfix servono soltanto gli ID.

Il controller riceve dal DAO gli ID e le revisioni della classe nella release. Converte gli ID in un insieme ed estrae dalle revisioni una mappa `hash -> messaggio`, con una voce per commit, poi passa questi soli dati a `NfixCalculator`. Il calcolatore non dipende dal DAO, dal reader o dal tipo `Revision`. La lettura HTTP e' nel DAO; matching dei ticket e conteggio sono nel calcolatore. `FeatureComputingDAO` scrive Nfix dal campo gia' esistente nell'entry. `Revision` sostituisce le coppie di interi per conservare anche hash e messaggio, senza ulteriori scansioni Git. Gli errori Jira risalgono alla CLI: non vengono tradotti in zero fix.

`Revision` nel diagramma abbrevia `GitHistoryReader.Revision`: e' un record annidato nel reader, come `GitSourceReader.Source`. E' un contenitore tecnico immutabile dei dati Git, non una entity del dataset; le entity conservano i loro getter e setter.

Per Nauth, `GitHistoryReader` legge l'email Author nello stesso log Git e la conserva in `Revision.authorEmail`. Il DAO restituisce le revisioni al controller, che estrae una `List<String>` e la passa a `NauthCalculator`. Il calcolatore non dipende da DAO o reader; conta le email distinte normalizzate. Il DAO scrive `DatasetEntry.nauth` nella colonna `Nauth`.
