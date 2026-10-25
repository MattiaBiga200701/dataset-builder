package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.ReleaseDataset;
import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.utils.Feature;
import java.io.BufferedWriter;
import it.uniroma2.isw2.utils.CsvFormat;
import it.uniroma2.isw2.utils.GitSourceReader;
import it.uniroma2.isw2.utils.GitHistoryReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeatureComputingDAO implements AutoCloseable {

    private final Path repositoryPath;
    private GitSourceReader sourceReader;
    private final GitHistoryReader historyReader;
    private ReleaseDataset releaseDataset;
    private List<List<String>> rows = List.of();
    private final Map<String, Integer> columns = new HashMap<>();
    private final Map<EntryKey, List<String>> entries = new HashMap<>();
    private final Set<EntryKey> updated = new HashSet<>();
    private Path outputPath;

    public FeatureComputingDAO() {
        this(Path.of("../StormDataset/repositories/storm.git"));
    }

    public FeatureComputingDAO(Path repositoryPath) {
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "repositoryPath").toAbsolutePath();
        this.historyReader = new GitHistoryReader(this.repositoryPath);
    }

    /** Chiave composta che identifica una riga del dataset. */
    private record EntryKey(String project, String className, String releaseId) { }

    /** Copia il CSV di base e legge la copia una sola volta per elaborazione. */
    public void readBaseCsv(Path input, Path output) throws IOException {

        Path source = input.toAbsolutePath().normalize();
        Path target = output.toAbsolutePath().normalize();
        if (source.equals(target) || (Files.exists(target) && Files.isSameFile(source, target))) {
            throw new IOException("Il file di output deve essere diverso dal CSV di base");
        }
        rows = List.of();
        columns.clear();
        entries.clear();
        updated.clear();
        outputPath = null;
        Files.createDirectories(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        rows = CsvFormat.parseCsv(Files.readString(target, StandardCharsets.UTF_8));
        if (rows.isEmpty()) throw new IOException("CSV di base vuoto");
        List<String> header = rows.getFirst();
        for (int i = 0; i < header.size(); i++) {
            if (columns.put(header.get(i), i) != null) {
                throw new IOException("Colonna duplicata: " + header.get(i));
            }
        }
        for (String name : List.of("Project Name", "Class Name", "Release ID",
                Feature.LOC.getColumnName(), Feature.LOC_TOUCHED.getColumnName())) {
            if (!columns.containsKey(name)) throw new IOException("Colonna mancante: " + name);
        }
        for (List<String> row : rows.subList(1, rows.size())) {
            if (row.size() != header.size()) throw new IOException("Numero di celle non valido nel CSV di base");
            EntryKey key = new EntryKey(row.get(columns.get("Project Name")),
                    row.get(columns.get("Class Name")), row.get(columns.get("Release ID")));
            if (entries.put(key, row) != null) throw new IOException("Entry duplicata nel CSV: " + key);
        }
        outputPath = target;
    }


    public void updateEntry(DatasetEntry entry) throws IOException {

        if (outputPath == null) throw new IOException("Caricare prima la copia del CSV di base");

        EntryKey key = new EntryKey(DatasetEntry.getProjectName(), entry.getClassName(),
                entry.getRelease().getIndex().toString());
        List<String> row = entries.get(key);

        if (row == null) throw new IOException("Entry assente dal CSV di base: " + key);

        if (!updated.add(key)) throw new IOException("Entry calcolata piu' volte: " + key);

        row.set(columns.get(Feature.LOC.getColumnName()), Objects.requireNonNull(entry.getLOC(), "LOC").toString());
        row.set(columns.get(Feature.LOC_TOUCHED.getColumnName()),
                Objects.requireNonNull(entry.getLocTouched(), "LOC Touched").toString());
    }

    public Map<String, List<int[]>> getChanges(String previousCommit, String currentCommit) throws IOException {
        return historyReader.readChanges(previousCommit, currentCommit);
    }

    /** Mantiene ordine delle righe, intestazione e celle non aggiornate. */
    public void writeDatasetCsv() throws IOException {
        if (outputPath == null) throw new IOException("Caricare prima la copia del CSV di base");
        if (updated.size() != entries.size()) {
            throw new IOException("Il CSV di base contiene entry senza LOC calcolati");
        }
        
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) writer.write(",");
                    writer.write(CsvFormat.csvValue(row.get(i)));
                }
                writer.write("\r\n");
            }
        }
    }

    /** Legge il sorgente dal commit indicato senza checkout. */
    public GitSourceReader.Source getSource(String commitHash, String classPath) throws IOException {
        Objects.requireNonNull(commitHash, "commitHash");
        Objects.requireNonNull(classPath, "classPath");
        if (sourceReader == null) sourceReader = new GitSourceReader(repositoryPath);
        return sourceReader.read(commitHash, classPath);
    }

    @Override
    public void close() throws IOException {
        if (sourceReader != null) {
            try { sourceReader.close(); }
            finally { sourceReader = null; }
        }
    }

    public ReleaseDataset getReleaseDataset() {
        return releaseDataset;
    }

    public void setReleaseDataset(ReleaseDataset releaseDataset) {
        this.releaseDataset = releaseDataset;
    }
}
