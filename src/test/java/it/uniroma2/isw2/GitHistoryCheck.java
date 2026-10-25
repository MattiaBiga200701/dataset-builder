package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.LocTouchedCalculator;
import it.uniroma2.isw2.utils.CsvFormat;
import it.uniroma2.isw2.utils.GitHistoryReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import it.uniroma2.isw2.controller.FeatureComputingController;
import it.uniroma2.isw2.model.dao.FeatureComputingDAO;
import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import java.io.IOException;

/** Verifica sul clone locale; non modifica i CSV di output. */
public class GitHistoryCheck {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--fixture")) {
            checkFixture();
            return;
        }
        Path repo = Path.of("../StormDataset/repositories/storm.git");
        var mapping = CsvFormat.parseCsv(Files.readString(Path.of("../StormDataset/output/selected_release_commits.csv")));
        var base = CsvFormat.parseCsv(Files.readString(Path.of("output/dataset_base.csv")));
        // Mapping dello snapshot corrente: il dataset base accorpa la data di 0.10.0 e 0.9.6.
        List<String> commits = new ArrayList<>();
        for (int i = 1; i < mapping.size(); i++) if (i != 7) commits.add(mapping.get(i).get(5));
        if (args.length > 0 && args[0].equals("--pipeline")) {
            checkPipeline(repo, base, commits);
            return;
        }
        GitHistoryReader reader = new GitHistoryReader(repo);
        LocTouchedCalculator calculator = new LocTouchedCalculator();
        long start = System.nanoTime();
        reader.prepareHistory(commits);
        List<Map<String, List<int[]>>> results = new ArrayList<>();
        String previous = null;
        for (String commit : commits) {
            results.add(reader.readChanges(previous, commit));
            previous = commit;
        }
        System.out.printf("Indicizzazione e intervalli: %.3f s%n", (System.nanoTime() - start) / 1e9);
        int checked = 0;
        int differences = 0;
        for (int release = 1; release <= commits.size(); release++) {
            int index = release;
            var rows = base.subList(1, base.size()).stream().filter(row -> row.get(2).equals("" + index)).toList();
            int step = Math.max(1, rows.size() / 8);
            for (int i = 0; i < rows.size(); i += step) {
                String path = rows.get(i).get(1);
                int actual = calculator.calculate(results.get(release - 1).getOrDefault(path, List.of()));
                int expected = reference(repo, release == 1 ? null : commits.get(release - 2), commits.get(release - 1), path);
                checked++;
                if (actual != expected) {
                    differences++;
                    System.out.printf("DIFF release=%d %s indexed=%d follow=%d%n", release, path, actual, expected);
                }
            }
        }
        int config = calculator.calculate(results.getFirst().get("storm-core/src/jvm/backtype/storm/Config.java"));
        if (config != 1848) throw new AssertionError("Config.java: " + config);
        System.out.printf("Confronti --follow: %d, differenze: %d; Config.java: %d%n", checked, differences, config);
        if (differences != 0) throw new AssertionError("Conteggi da verificare");
    }

    private static int reference(Path repo, String previous, String current, String path) throws Exception {
        Process process = new ProcessBuilder("git", "-C", repo.toString(), "-c", "diff.algorithm=myers",
                "-c", "diff.renameLimit=0", "log", "--follow", "--format=", "--numstat", "-z", "--root",
                "--no-merges", "--find-renames=50%", "--no-ext-diff", "--no-textconv",
                previous == null ? current : previous + ".." + current, "--", path)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new AssertionError("Git reference fallito");
        int count = 0;
        for (String record : output.split("\u0000")) {
            String[] fields = record.stripLeading().split("\t", 3);
            if (fields.length == 3 && fields[0].matches("[0-9]+") && fields[1].matches("[0-9]+")) {
                count += Integer.parseInt(fields[0]) + Integer.parseInt(fields[1]);
            }
        }
        return count;
    }

    private static void checkPipeline(Path repo, List<List<String>> base, List<String> commits) throws Exception {
        Path output = Files.createTempFile("dataset-pipeline-check-", ".csv");
        Path original = Path.of("output", "dataset_base.csv");
        byte[] originalBytes = Files.readAllBytes(original);
        var reference = CsvFormat.parseCsv(Files.readString(Path.of("../StormDataset/output/dataset_calculated.csv")));
        Map<String, String> expectedLoc = new java.util.HashMap<>();
        for (var row : reference.subList(1, reference.size())) {
            expectedLoc.put(row.get(2) + ":" + row.get(1), row.get(3));
        }
        List<ReleaseDataset> datasets = new ArrayList<>();
        for (int i = 0; i < commits.size(); i++) {
            Release release = new Release();
            release.setIndex(i + 1);
            ReleaseDataset dataset = new ReleaseDataset();
            dataset.setRelease(release);
            dataset.setCommitHash(commits.get(i));
            dataset.setEntries(new ArrayList<>());
            datasets.add(dataset);
        }
        DatasetEntry.setProjectName("STORM");
        for (var row : base.subList(1, base.size())) {
            ReleaseDataset dataset = datasets.get(Integer.parseInt(row.get(2)) - 1);
            DatasetEntry entry = new DatasetEntry();
            entry.setClassName(row.get(1));
            entry.setRelease(dataset.getRelease());
            dataset.getEntries().add(entry);
        }
        FeatureComputingDAO dao = new FeatureComputingDAO(repo) {
            @Override public void readBaseCsv(Path input, Path target) throws IOException {
                super.readBaseCsv(original, output);
            }
        };
        try {
            long start = System.nanoTime();
            new FeatureComputingController(dao).createDatasetCsv(datasets);
            double seconds = (System.nanoTime() - start) / 1e9;
            var actual = CsvFormat.parseCsv(Files.readString(output));
            if (actual.size() != base.size()) throw new AssertionError("Righe perse");
            for (int i = 1; i < actual.size(); i++) {
                var row = actual.get(i);
                int release = Integer.parseInt(row.get(2));
                String key = (release <= 6 ? release : release + 1) + ":" + row.get(1);
                if (!row.get(3).equals(expectedLoc.get(key))) throw new AssertionError("LOC cambiati: " + key);
                if (row.get(4).isBlank() || Integer.parseInt(row.get(4)) < 0) throw new AssertionError("LOC Touched mancanti");
                for (int col = 0; col < row.size(); col++) {
                    if (col != 3 && col != 4 && !row.get(col).equals(base.get(i).get(col))) {
                        throw new AssertionError("Cella non calcolata modificata");
                    }
                }
            }
            if (!java.util.Arrays.equals(originalBytes, Files.readAllBytes(original))) throw new AssertionError("Base modificata");
            if (!actual.get(1).get(4).equals("1848")) throw new AssertionError("Config.java LOC Touched");
            System.out.printf("OK: pipeline %d entry in %.3f s; LOC identici al riferimento, LOC Touched valorizzati, base invariata.%n",
                    actual.size() - 1, seconds);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static void checkFixture() throws Exception {
        Path repo = Files.createTempDirectory("history-index-check-");
        try {
            git(repo, "init", "-b", "main");
            String original = "alpha\nbeta\ngamma\ndelta\nepsilon\nzeta\neta\ntheta\n";
            Files.writeString(repo.resolve("Original.java"), original);
            Files.writeString(repo.resolve("Stable.java"), "stable\n");
            String root = commit(repo);
            Files.writeString(repo.resolve("Original.java"), original.replace("beta", "changed"));
            commit(repo);
            Files.writeString(repo.resolve("Original.java"), original);
            String reverted = commit(repo);
            Files.move(repo.resolve("Original.java"), repo.resolve("Moved.java"));
            String moved = commit(repo);
            Files.move(repo.resolve("Moved.java"), repo.resolve("Final.java"));
            Files.writeString(repo.resolve("Final.java"), original + "extra\n");
            String renamed = commit(repo);
            git(repo, "checkout", "-b", "side", root);
            Files.writeString(repo.resolve("Original.java"), original.replace("gamma", "side"));
            String side = commit(repo);
            git(repo, "checkout", "main");
            git(repo, "merge", "--no-ff", "side", "-m", "merge");
            String merged = git(repo, "rev-parse", "HEAD").strip();
            GitHistoryReader reader = new GitHistoryReader(repo);
            reader.prepareHistory(List.of(root, reverted, moved, renamed, side, merged));
            LocTouchedCalculator calculator = new LocTouchedCalculator();
            checkValue(reader, calculator, null, root, "Original.java", 8);
            checkValue(reader, calculator, root, reverted, "Original.java", 4);
            checkValue(reader, calculator, reverted, moved, "Moved.java", 0);
            checkValue(reader, calculator, null, moved, "Moved.java", 12);
            checkValue(reader, calculator, moved, renamed, "Final.java", 1);
            checkValue(reader, calculator, root, renamed, "Final.java", 5);
            checkValue(reader, calculator, root, renamed, "Stable.java", 0);
            checkValue(reader, calculator, renamed, side, "Original.java", 2);
            int reference = reference(repo, root, merged, "Final.java");
            checkValue(reader, calculator, root, merged, "Final.java", reference);
            if (!reader.readChanges(root, root).isEmpty()) throw new AssertionError("Intervallo vuoto");
            System.out.println("OK: intervalli, modifiche annullate, catene di rinomine, rami divergenti e merge.");
        } finally {
            try (var paths = Files.walk(repo)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    path.toFile().setWritable(true);
                    Files.delete(path);
                }
            }
        }
    }

    private static void checkValue(GitHistoryReader reader, LocTouchedCalculator calculator,
                                   String previous, String current, String path, int expected) throws Exception {
        int actual = calculator.calculate(reader.readChanges(previous, current).getOrDefault(path, List.of()));
        if (actual != expected) throw new AssertionError(path + ": atteso " + expected + ", ottenuto " + actual);
    }

    private static String commit(Path repo) throws Exception {
        git(repo, "add", ".");
        git(repo, "commit", "-m", "fixture");
        return git(repo, "rev-parse", "HEAD").strip();
    }

    private static String git(Path repo, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toString(),
                "-c", "user.name=Fixture", "-c", "user.email=fixture@example.test",
                "-c", "commit.gpgsign=false", "-c", "core.autocrlf=false"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new AssertionError(output);
        return output;
    }
}
