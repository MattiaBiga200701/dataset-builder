package it.uniroma2.isw2;

import it.uniroma2.isw2.controller.FeatureComputingController;
import it.uniroma2.isw2.model.dao.FeatureComputingDAO;
import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import it.uniroma2.isw2.utils.GitSourceReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Controllo offline dell'associazione CSV e del flusso controller/DAO. */
public class FeatureCsvCheck {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("feature-csv-check-");
        Path base = dir.resolve("base.csv");
        Path output = dir.resolve("Dataset.csv");
        String original = "NR,Class Name,LOC,Release ID,Project Name,Extra,LOC Touched\r\n"
                + "7,\"A,\"\"test\"\".java\",,2,STORM,\"keep\r\nthis\",\r\n"
                + "8,\"A,\"\"test\"\".java\",,1,STORM,preserved,\r\n";
        FeatureComputingDAO dao = new FeatureComputingDAO() {
            @Override public void prepareHistory(List<String> commits) { }
            @Override public void readBaseCsv(Path input, Path target) throws IOException {
                // Isola i file del test senza cambiare i percorsi del controller.
                if (input.equals(Path.of("output", "dataset_base.csv"))) {
                    super.readBaseCsv(base, output);
                } else {
                    super.readBaseCsv(input, target);
                }
            }
            @Override public Map<String, List<int[]>> getChanges(String previous, String current) {
                if (current.equals("commit1") && previous != null) throw new AssertionError(previous);
                if (current.equals("commit2") && !"commit1".equals(previous)) throw new AssertionError(previous);
                return current.equals("commit1")
                        ? Map.of("A,\"test\".java", List.of(new int[]{2, 0}, new int[]{3, 1}))
                        : Map.of();
            }
            @Override public GitSourceReader.Source getSource(String commit, String path) {
                if (!path.equals("A,\"test\".java")) throw new AssertionError(path);
                String text = switch (commit) {
                    case "commit1" -> "class A {}\n// comment";
                    case "commit2", "commit3" -> "class A {\n int n;\n}";
                    default -> throw new AssertionError(commit);
                };
                return new GitSourceReader.Source(commit, text);
            }
        };
        try {
            Files.writeString(base, original);
            DatasetEntry.setProjectName("STORM");
            dao.readBaseCsv(base, output);
            if (!Files.readString(output).equals(original)) throw new AssertionError("Copia iniziale diversa dalla base");
            expectIOException(() -> dao.readBaseCsv(base, base));
            List<ReleaseDataset> datasets = List.of(dataset(1), dataset(2));
            FeatureComputingController controller = new FeatureComputingController(dao);
            controller.createDatasetCsv(datasets);
            String expected = "\"NR\",\"Class Name\",\"LOC\",\"Release ID\",\"Project Name\",\"Extra\",\"LOC Touched\"\r\n"
                    + "\"7\",\"A,\"\"test\"\".java\",\"3\",\"2\",\"STORM\",\"keep\r\nthis\",\"0\"\r\n"
                    + "\"8\",\"A,\"\"test\"\".java\",\"1\",\"1\",\"STORM\",\"preserved\",\"6\"\r\n";
            if (!Files.readString(output).equals(expected)) throw new AssertionError(Files.readString(output));
            if (!Files.readString(base).equals(original)) throw new AssertionError("Base modificata");
            expectIOException(() -> controller.createDatasetCsv(List.of(dataset(1))));
            expectIOException(() -> controller.createDatasetCsv(List.of(dataset(1), dataset(3))));
            Files.writeString(base, original + "8,\"A,\"\"test\"\".java\",,1,STORM,duplicate,\r\n");
            expectIOException(() -> dao.readBaseCsv(base, output));
            Files.writeString(base, "Project Name,Class Name,Release ID\n");
            expectIOException(() -> dao.readBaseCsv(base, output));
            System.out.println("OK: LOC per release, associazione per chiave, ordine, celle preservate, escaping e CSV incoerenti.");
        } finally {
            Files.deleteIfExists(base);
            Files.deleteIfExists(output);
            Files.deleteIfExists(dir);
        }
    }

    private static ReleaseDataset dataset(int index) {
        Release release = new Release();
        release.setIndex(index);
        DatasetEntry entry = new DatasetEntry();
        entry.setRelease(release);
        entry.setClassName("A,\"test\".java");
        ReleaseDataset dataset = new ReleaseDataset();
        dataset.setRelease(release);
        dataset.setCommitHash("commit" + index);
        dataset.setEntries(List.of(entry));
        return dataset;
    }

    private interface Action { void run() throws IOException; }
    private static void expectIOException(Action action) throws IOException {
        try { action.run(); } catch (IOException expected) { return; }
        throw new AssertionError("IOException attesa");
    }
}
