package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.LocTouchedCalculator;
import it.uniroma2.isw2.utils.GitHistoryReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Test offline con cronologia reale: root, modifica e annullamento, rinomina, merge. */
public class LocTouchedCheck {
    public static void main(String[] args) throws Exception {
        Path repo = Files.createTempDirectory("loc-touched-check-");
        try {
            git(repo, "init", "-b", "main");
            String name = "A, sample.java";
            Files.writeString(repo.resolve(name), "alpha\nbeta\n");
            Files.writeString(repo.resolve("B.java"), "stable\n");
            Files.writeString(repo.resolve("note.txt"), "ignored\n");
            String root = commit(repo);
            Files.writeString(repo.resolve(name), "alpha\ngamma\n");
            commit(repo);
            Files.writeString(repo.resolve(name), "alpha\nbeta\n");
            String reverted = commit(repo);
            GitHistoryReader reader = new GitHistoryReader(repo);
            LocTouchedCalculator calculator = new LocTouchedCalculator();
            var initial = reader.readChanges(null, root);
            check(calculator.calculate(initial.get(name)), 2);
            check(calculator.calculate(initial.get("B.java")), 1);
            if (initial.containsKey("note.txt")) throw new AssertionError("File non Java incluso");
            var changes = reader.readChanges(root, reverted);
            check(calculator.calculate(changes.get(name)), 4);
            check(calculator.calculate(changes.getOrDefault("B.java", List.of())), 0);
            if (!reader.readChanges(reverted, reverted).isEmpty()) throw new AssertionError("Intervallo vuoto");
            Files.move(repo.resolve(name), repo.resolve("Renamed.java"));
            String renamed = commit(repo);
            var rename = reader.readChanges(reverted, renamed);
            check(calculator.calculate(rename.get(name)), 2);
            check(calculator.calculate(rename.get("Renamed.java")), 2);
            git(repo, "checkout", "-b", "side");
            Files.writeString(repo.resolve("C.java"), "side\n");
            commit(repo);
            git(repo, "checkout", "main");
            Files.writeString(repo.resolve("B.java"), "stable\nmain\n");
            commit(repo);
            git(repo, "merge", "--no-ff", "side", "-m", "merge");
            String merged = git(repo, "rev-parse", "HEAD").strip();
            var merge = reader.readChanges(renamed, merged);
            check(calculator.calculate(merge.get("B.java")), 1);
            check(calculator.calculate(merge.get("C.java")), 1);
            System.out.println("OK: root, intervalli, somma revisioni, file invariati, rinomina e merge senza doppio conteggio.");
        } finally {
            try (var paths = Files.walk(repo)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    path.toFile().setWritable(true);
                    Files.delete(path);
                }
            }
        }
    }

    private static String commit(Path repo) throws Exception {
        git(repo, "add", ".");
        git(repo, "commit", "-m", "fixture");
        return git(repo, "rev-parse", "HEAD").strip();
    }

    private static String git(Path repo, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", repo.toString(),
                "-c", "user.name=Fixture", "-c", "user.email=fixture@example.test",
                "-c", "commit.gpgsign=false", "-c", "core.autocrlf=false"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new AssertionError(output);
        return output;
    }

    private static void check(int actual, int expected) {
        if (actual != expected) throw new AssertionError("Atteso " + expected + ", ottenuto " + actual);
    }
}
