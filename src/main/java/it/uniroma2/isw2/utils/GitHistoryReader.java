package it.uniroma2.isw2.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Una lettura per intervallo; per ogni file conserva coppie [aggiunte, cancellate]. */
public final class GitHistoryReader {
    private final Path repositoryPath;

    public GitHistoryReader(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public Map<String, List<int[]>> readChanges(String previousCommit, String currentCommit) throws IOException {
        validateHash(currentCommit);
        if (previousCommit != null) validateHash(previousCommit);
        String range = previousCommit == null ? currentCommit : previousCommit + ".." + currentCommit;
        Process process = new ProcessBuilder("git", "-C", repositoryPath.toString(),
                "-c", "diff.algorithm=myers", "log", "--format=", "--numstat", "-z",
                "--root", "--no-merges", "--no-renames", "--no-ext-diff", "--no-textconv",
                "--end-of-options", range, "--", "*.java")
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) throw new IOException("Lettura cronologia Git fallita: " + range);
            Map<String, List<int[]>> changes = new HashMap<>();
            for (String record : output.split("\u0000")) {
                record = record.replaceFirst("^[\\r\\n]+", "");
                if (record.isEmpty()) continue;
                String[] fields = record.split("\t", 3);
                if (fields.length != 3) throw new IOException("Record numstat non valido: " + record);
                try {
                    int added = Integer.parseInt(fields[0]);
                    int deleted = Integer.parseInt(fields[1]);
                    if (added < 0 || deleted < 0) throw new NumberFormatException("Conteggio negativo");
                    changes.computeIfAbsent(fields[2], path -> new ArrayList<>()).add(new int[]{added, deleted});
                } catch (NumberFormatException exception) {
                    throw new IOException("Statistiche non numeriche per " + fields[2], exception);
                }
            }
            return changes;
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Lettura cronologia interrotta", exception);
        } finally {
            process.getInputStream().close();
        }
    }

    private void validateHash(String hash) throws IOException {
        if (hash == null || !hash.matches("[0-9a-fA-F]{40}")) {
            throw new IOException("Hash commit non valido: " + hash);
        }
    }
}
