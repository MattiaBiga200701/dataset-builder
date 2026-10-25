package it.uniroma2.isw2.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Lettura condivisa per intervallo, con follow mirato dei percorsi rinominati. */
public final class GitHistoryReader {
    private final Path repositoryPath;

    public GitHistoryReader(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public Map<String, List<int[]>> readChanges(String previousCommit, String currentCommit) throws IOException {
        validateHash(currentCommit);
        if (previousCommit != null) validateHash(previousCommit);
        String range = previousCommit == null ? currentCommit : previousCommit + ".." + currentCommit;
        // Legge tutti i percorsi per riconoscere anche cambi di estensione verso .java.
        String output = runGit(logArguments(range));
        Map<String, List<int[]>> changes = new HashMap<>();
        Set<String> renamedPaths = new HashSet<>();
        parseChanges(output, changes, renamedPaths, true);
        if (!renamedPaths.isEmpty()) {
            // Solo i file ancora presenti nello snapshot hanno una entry da calcolare.
            String tree = runGit(List.of("ls-tree", "-r", "-z", "--name-only", currentCommit));
            for (String path : tree.split("\u0000")) {
                if (!path.endsWith(".java") || !renamedPaths.contains(path)) continue;
                List<String> arguments = logArguments(range);
                arguments.add(arguments.indexOf("--end-of-options"), "--follow");
                arguments.add("--");
                arguments.add(path);
                Map<String, List<int[]>> followed = new HashMap<>();
                parseChanges(runGit(arguments), followed, new HashSet<>(), false);
                List<int[]> revisions = new ArrayList<>();
                for (List<int[]> values : followed.values()) revisions.addAll(values);
                changes.put(path, revisions);
            }
        }
        return changes;
    }

    private List<String> logArguments(String range) {
        return new ArrayList<>(List.of("-c", "diff.algorithm=myers", "-c", "diff.renameLimit=0",
                "log", "--format=", "--numstat", "-z", "--root", "--no-merges",
                "--find-renames=50%", "--no-ext-diff", "--no-textconv", "--end-of-options", range));
    }

    private void parseChanges(String output, Map<String, List<int[]>> changes,
                              Set<String> renamedPaths, boolean javaOnly) throws IOException {
        String[] records = output.split("\u0000", -1);
        for (int i = 0; i < records.length; i++) {
            String record = records[i].replaceFirst("^[\\r\\n]+", "");
            if (record.isEmpty()) continue;
            String[] fields = record.split("\t", 3);
            if (fields.length != 3) throw new IOException("Record numstat non valido: " + record);
            String path = fields[2];
            if (path.isEmpty()) {
                // Formato -z delle rinomine: conteggi TAB NUL vecchio NUL nuovo NUL.
                if (i + 2 >= records.length) throw new IOException("Rinomina Git incompleta");
                i++; // Vecchio percorso: --follow ricostruira' la sua storia.
                path = records[++i];
                renamedPaths.add(path);
            }
            if (javaOnly && !path.endsWith(".java")) continue;
            try {
                int added = Integer.parseInt(fields[0]);
                int deleted = Integer.parseInt(fields[1]);
                if (added < 0 || deleted < 0) throw new NumberFormatException("Conteggio negativo");
                // Una rinomina pura non aggiunge e non cancella righe.
                if (added != 0 || deleted != 0) {
                    changes.computeIfAbsent(path, key -> new ArrayList<>()).add(new int[]{added, deleted});
                }
            } catch (NumberFormatException exception) {
                throw new IOException("Statistiche non numeriche per " + path, exception);
            }
        }
    }

    private String runGit(List<String> arguments) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "-C", repositoryPath.toString()));
        command.addAll(arguments);
        Process process = new ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) throw new IOException("Lettura cronologia Git fallita");
            return output;
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
