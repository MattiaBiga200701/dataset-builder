package it.uniroma2.isw2.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Indicizza una volta Git; ricostruisce intervalli e rinominazioni in memoria. */
public final class GitHistoryReader {
    private final Path repositoryPath;
    private final List<String> commitOrder = new ArrayList<>();
    private final Map<String, List<String>> parents = new HashMap<>();
    private final Map<String, Map<String, int[]>> revisions = new HashMap<>();
    private final Map<String, Map<String, String>> renames = new HashMap<>();
    private final Map<String, Set<String>> additions = new HashMap<>();
    private final Map<String, Set<String>> reachable = new HashMap<>();

    public GitHistoryReader(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /** Un solo log per tutti gli snapshot, inclusi i rami divergenti. */
    public void prepareHistory(List<String> commits) throws IOException {
        for (String commit : commits) validateHash(commit);
        clear();
        if (commits.isEmpty()) return;
        List<String> arguments = new ArrayList<>(List.of("-c", "diff.algorithm=myers",
                "-c", "diff.renameLimit=0", "log", "--topo-order", "--full-history",
                "--format=%x00COMMIT%x00%H%x00%P%x00", "--raw", "--numstat", "-z",
                "--no-abbrev", "--root", "--diff-merges=off", "--find-renames=50%",
                "--find-copies=50%", "--find-copies-harder",
                "--no-ext-diff", "--no-textconv", "--end-of-options"));
        arguments.addAll(new HashSet<>(commits));
        try {
            parseHistory(runGit(arguments));
        } catch (IOException exception) {
            clear();
            throw exception;
        }
    }

    public Map<String, List<int[]>> readChanges(String previousCommit, String currentCommit) throws IOException {
        validateHash(currentCommit);
        if (previousCommit != null) validateHash(previousCommit);
        if (!parents.containsKey(currentCommit)
                || (previousCommit != null && !parents.containsKey(previousCommit))) {
            throw new IOException("Preparare prima la cronologia degli snapshot richiesti");
        }
        Set<String> interval = new HashSet<>(reachableFrom(currentCommit));
        if (previousCommit != null) interval.removeAll(reachableFrom(previousCommit));
        Map<String, List<int[]>> result = new HashMap<>();
        if (interval.isEmpty()) return result;
        String tree = runGit(List.of("ls-tree", "-r", "-z", "--name-only", currentCommit));
        for (String path : tree.split("\u0000")) {
            if (!path.endsWith(".java")) continue;
            List<int[]> changes = followChanges(currentCommit, path, interval);
            if (!changes.isEmpty()) result.put(path, changes);
        }
        return result;
    }

    /** La rinomina su un ramo non modifica i percorsi attraversati sugli altri. */
    private List<int[]> followChanges(String start, String path, Set<String> interval) {
        Map<String, Set<String>> pending = new HashMap<>();
        pending.computeIfAbsent(start, key -> new HashSet<>()).add(path);
        List<int[]> result = new ArrayList<>();
        for (String commit : commitOrder) {
            Set<String> paths = pending.remove(commit);
            if (paths == null || !interval.contains(commit)) continue;
            List<String> ancestors = parents.get(commit);
            for (String currentPath : paths) {
                // Attraversa i merge senza contarli come revisioni.
                if (ancestors.size() <= 1) {
                    int[] change = revisions.getOrDefault(commit, Map.of()).get(currentPath);
                    if (change != null) result.add(change);
                    // Un nuovo file non eredita la storia di un vecchio file omonimo.
                    if (additions.getOrDefault(commit, Set.of()).contains(currentPath)) continue;
                }
                String oldPath = renames.getOrDefault(commit, Map.of()).getOrDefault(currentPath, currentPath);
                for (String parent : ancestors) {
                    if (interval.contains(parent)) {
                        pending.computeIfAbsent(parent, key -> new HashSet<>()).add(oldPath);
                    }
                }
            }
        }
        return result;
    }

    private Set<String> reachableFrom(String start) {
        return reachable.computeIfAbsent(start, key -> {
            Set<String> visited = new HashSet<>();
            ArrayDeque<String> pending = new ArrayDeque<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                String commit = pending.removeLast();
                if (visited.add(commit)) pending.addAll(parents.getOrDefault(commit, List.of()));
            }
            return visited;
        });
    }

    private void parseHistory(String output) throws IOException {
        String[] records = output.split("\u0000", -1);
        String commit = null;
        for (int i = 0; i < records.length; i++) {
            String record = records[i].replaceFirst("^[\\r\\n]+", "");
            if (record.isEmpty()) continue;
            if (record.equals("COMMIT")) {
                if (i + 2 >= records.length) throw new IOException("Intestazione commit incompleta");
                commit = records[++i];
                validateHash(commit);
                String parentText = records[++i];
                commitOrder.add(commit);
                parents.put(commit, parentText.isEmpty() ? List.of() : List.of(parentText.split(" ")));
            } else if (record.startsWith(":")) {
                if (commit == null || ++i >= records.length) throw new IOException("Record raw Git incompleto");
                String oldPath = records[i];
                String[] fields = record.split(" ");
                String status = fields[fields.length - 1];
                if (status.startsWith("R") || status.startsWith("C")) {
                    if (++i >= records.length) throw new IOException("Rinomina Git incompleta");
                    renames.computeIfAbsent(commit, key -> new HashMap<>()).put(records[i], oldPath);
                } else if (status.equals("A")) {
                    additions.computeIfAbsent(commit, key -> new HashSet<>()).add(oldPath);
                }
            } else {
                if (commit == null) throw new IOException("Statistiche senza commit");
                String[] fields = record.split("\t", 3);
                if (fields.length != 3) throw new IOException("Record numstat non valido: " + record);
                String path = fields[2];
                if (path.isEmpty()) {
                    if (i + 2 >= records.length) throw new IOException("Statistiche rinomina incomplete");
                    i++;
                    path = records[++i];
                }
                try {
                    if (fields[0].equals("-") || fields[1].equals("-")) {
                        if (path.endsWith(".java")) throw new IOException("Statistiche binarie per " + path);
                        continue;
                    }
                    int added = Integer.parseInt(fields[0]);
                    int deleted = Integer.parseInt(fields[1]);
                    if (added < 0 || deleted < 0) throw new NumberFormatException("Conteggio negativo");
                    if (added != 0 || deleted != 0) {
                        revisions.computeIfAbsent(commit, key -> new HashMap<>()).put(path, new int[]{added, deleted});
                    }
                } catch (NumberFormatException exception) {
                    throw new IOException("Statistiche non numeriche per " + path, exception);
                }
            }
        }
    }

    public void clear() {
        commitOrder.clear();
        parents.clear();
        revisions.clear();
        renames.clear();
        additions.clear();
        reachable.clear();
    }

    private String runGit(List<String> arguments) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "-C", repositoryPath.toString()));
        command.addAll(arguments);
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
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
        if (hash == null || !hash.matches("[0-9a-fA-F]{40}")) throw new IOException("Hash commit non valido: " + hash);
    }
}
