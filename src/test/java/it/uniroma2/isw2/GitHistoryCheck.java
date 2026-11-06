package it.uniroma2.isw2;

import it.uniroma2.isw2.utils.GitHistoryReader.Revision;

import it.uniroma2.isw2.metrics.LocTouchedCalculator;
import it.uniroma2.isw2.metrics.NrCalculator;
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
import it.uniroma2.isw2.metrics.NfixCalculator;
import it.uniroma2.isw2.metrics.NauthCalculator;
import java.util.Set;

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
        if (args.length > 0 && (args[0].equals("--pipeline") || args[0].equals("--pipeline-live"))) {
            checkPipeline(repo, base, commits, args[0].equals("--pipeline-live"));
            return;
        }
        GitHistoryReader reader = new GitHistoryReader(repo);
        LocTouchedCalculator calculator = new LocTouchedCalculator();
        long start = System.nanoTime();
        reader.prepareHistory(commits);
        List<Map<String, List<Revision>>> results = new ArrayList<>();
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

    private static void checkPipeline(Path repo, List<List<String>> base, List<String> commits, boolean live) throws Exception {
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
            @Override public List<String> getTicketIDs(String project) throws IOException {
                return live ? super.getTicketIDs(project) : List.of();
            }
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
                if (row.get(5).isBlank() || Integer.parseInt(row.get(5)) < 0) throw new AssertionError("NR mancanti");
                int added = Integer.parseInt(row.get(8));
                if (added < 0 || added > Integer.parseInt(row.get(4))) throw new AssertionError("LOC Added fuori intervallo");
                int maxAdded = Integer.parseInt(row.get(9));
                if (maxAdded < 0 || maxAdded > added || (added > 0 && maxAdded == 0)) {
                    throw new AssertionError("Max LOC Added fuori intervallo");
                }
                if (Integer.parseInt(row.get(10)) < 0) throw new AssertionError("WMC negativa");
                if (!row.get(11).isBlank() && Integer.parseInt(row.get(11)) < 0) throw new AssertionError("DIT negativa");
                int nauth = Integer.parseInt(row.get(7));
                int nr = Integer.parseInt(row.get(5));
                if (nauth < 0 || nauth > nr || (nr > 0 && nauth == 0)) throw new AssertionError("Nauth fuori intervallo");
                int nfix = Integer.parseInt(row.get(6));
                if (nfix < 0 || nfix > Integer.parseInt(row.get(5))) throw new AssertionError("Nfix fuori intervallo");
                if (!live && nfix != 0) throw new AssertionError("Nfix con ticket vuoti");
                for (int col = 0; col < row.size(); col++) {
                    if (col != 3 && col != 4 && col != 5 && col != 6 && col != 7 && col != 8 && col != 9 && col != 10 && col != 11 && !row.get(col).equals(base.get(i).get(col))) {
                        throw new AssertionError("Cella non calcolata modificata");
                    }
                }
            }
            if (!java.util.Arrays.equals(originalBytes, Files.readAllBytes(original))) throw new AssertionError("Base modificata");
            if (!actual.get(1).get(4).equals("1848")) throw new AssertionError("Config.java LOC Touched");
            System.out.printf("OK: pipeline %d entry in %.3f s; LOC identici al riferimento, LOC Touched, LOC Added, Max LOC Added, NR, Nfix, Nauth e WMC valorizzati, base invariata.%n",
                    actual.size() - 1, seconds);
            System.out.println("DIT non disponibili: " + actual.subList(1, actual.size()).stream().filter(row -> row.get(11).isBlank()).count());
            System.out.println("Righe con Nfix > 0: " + actual.subList(1, actual.size()).stream()
                    .filter(row -> Integer.parseInt(row.get(6)) > 0).count());
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
            String reverted = commit(repo, "Fix\n\n[STORM-12] STORM-123", "Second <second@example.test>");
            Files.move(repo.resolve("Original.java"), repo.resolve("Moved.java"));
            String moved = commit(repo, "STORM-12 rename");
            Files.move(repo.resolve("Moved.java"), repo.resolve("Final.java"));
            Files.writeString(repo.resolve("Final.java"), original + "extra\n");
            String renamed = commit(repo);
            git(repo, "checkout", "-b", "side", root);
            Files.writeString(repo.resolve("Original.java"), original.replace("gamma", "side"));
            String side = commit(repo);
            git(repo, "checkout", "main");
            git(repo, "merge", "--no-ff", "side", "-m", "STORM-12 merge");
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
            checkNauth(reader, null, root, "Original.java", 1);
            checkNauth(reader, root, reverted, "Original.java", 2);
            checkNauth(reader, reverted, moved, "Moved.java", 1);
            checkNauth(reader, null, moved, "Moved.java", 2);
            checkNauth(reader, root, renamed, "Stable.java", 0);
            checkNauth(reader, renamed, merged, "Stable.java", 0);
            checkNr(reader, null, root, "Original.java", 1);
            checkNr(reader, root, reverted, "Original.java", 2);
            checkNr(reader, reverted, moved, "Moved.java", 1);
            checkNr(reader, null, moved, "Moved.java", 4);
            checkNr(reader, moved, renamed, "Final.java", 1);
            checkNr(reader, root, renamed, "Final.java", 4);
            checkNr(reader, root, renamed, "Stable.java", 0);
            checkNr(reader, renamed, side, "Original.java", 1);
            checkNr(reader, null, merged, "Stable.java", 1);
            checkNr(reader, renamed, merged, "Stable.java", 0);
            checkNfix(reader, null, root, "Original.java", 0);
            checkNfix(reader, root, reverted, "Original.java", 1);
            checkNfix(reader, reverted, moved, "Moved.java", 1);
            checkNfix(reader, moved, renamed, "Final.java", 0);
            checkNfix(reader, null, moved, "Moved.java", 2);
            checkNfix(reader, renamed, merged, "Stable.java", 0);
            if (!reader.readChanges(root, root).isEmpty()) throw new AssertionError("Intervallo vuoto");
            // Due storie di percorso convergono nello stesso commit: NR deve contarlo una volta.
            Files.writeString(repo.resolve("One.java"), "unique-one-two\nshared-origin\n");
            Files.writeString(repo.resolve("Two.java"), "unique-one-two\nshared-origin\n");
            String origins = commit(repo, "STORM-12");
            git(repo, "checkout", "-b", "rename-left");
            Files.move(repo.resolve("One.java"), repo.resolve("Joined.java"));
            commit(repo);
            git(repo, "checkout", "-b", "rename-right", origins);
            Files.move(repo.resolve("Two.java"), repo.resolve("Joined.java"));
            commit(repo);
            git(repo, "merge", "--no-ff", "rename-left", "-m", "join paths");
            String joined = git(repo, "rev-parse", "HEAD").strip();
            reader.prepareHistory(List.of(origins, joined));
            checkNr(reader, null, joined, "Joined.java", 3);
            checkNr(reader, origins, joined, "Joined.java", 2);
            checkValue(reader, calculator, null, joined, "Joined.java", 4);
            checkNfix(reader, null, joined, "Joined.java", 1);
            System.out.println("OK: intervalli, rinomine, NR, Nfix, rami divergenti, merge e unicita' per commit.");
        } finally {
            try (var paths = Files.walk(repo)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    path.toFile().setWritable(true);
                    Files.delete(path);
                }
            }
        }
    }

    private static void checkNauth(GitHistoryReader reader, String previous, String current,
                                   String path, int expected) throws Exception {
        var emails = reader.readChanges(previous, current).getOrDefault(path, List.of())
                .stream().map(Revision::authorEmail).toList();
        int actual = new NauthCalculator().calculate(emails);
        if (actual != expected) throw new AssertionError("Nauth " + path + ": " + actual + " != " + expected);
    }

    private static void checkNfix(GitHistoryReader reader, String previous, String current,
                                  String path, int expected) throws Exception {
        Map<String, String> messagesByCommit = new java.util.HashMap<>();
        for (Revision revision : reader.readChanges(previous, current).getOrDefault(path, List.of())) {
            messagesByCommit.put(revision.commitHash(), revision.message());
        }
        int actual = new NfixCalculator().calculate(messagesByCommit, Set.of("STORM-12", "STORM-123"));
        if (actual != expected) throw new AssertionError("Nfix " + path + ": " + actual + " != " + expected);
    }

    private static void checkNr(GitHistoryReader reader, String previous, String current,
                                String path, int expected) throws Exception {
        int actual = new NrCalculator().calculate(reader.readChanges(previous, current).getOrDefault(path, List.of()));
        if (actual != expected) throw new AssertionError("NR " + path + ": atteso " + expected + ", ottenuto " + actual);
    }

    private static void checkValue(GitHistoryReader reader, LocTouchedCalculator calculator,
                                   String previous, String current, String path, int expected) throws Exception {
        int actual = calculator.calculate(reader.readChanges(previous, current).getOrDefault(path, List.of()));
        if (actual != expected) throw new AssertionError(path + ": atteso " + expected + ", ottenuto " + actual);
    }

    private static String commit(Path repo) throws Exception {
        return commit(repo, "fixture");
    }

    private static String commit(Path repo, String message) throws Exception {
        return commit(repo, message, "Fixture <fixture@example.test>");
    }

    private static String commit(Path repo, String message, String author) throws Exception {
        git(repo, "add", ".");
        git(repo, "commit", "--author", author, "-m", message);
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
