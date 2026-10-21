package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseClasses;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

/** Recupera le release una volta e prepara le associazioni con le relative classi. */
public class ReleaseClassesDAO {
    private final ReleaseDAO releaseDAO;
    private final Path repositoryPath;

    public ReleaseClassesDAO() {
        this(new ReleaseDAO());
    }

    public ReleaseClassesDAO(ReleaseDAO releaseDAO) {
        this(releaseDAO, Path.of("../StormDataset/repositories/storm.git"));
    }

    /** Il percorso deve indicare un repository locale con i tag delle release. */
    public ReleaseClassesDAO(ReleaseDAO releaseDAO, Path repositoryPath) {
        this.releaseDAO = Objects.requireNonNull(releaseDAO, "releaseDAO");
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "repositoryPath").toAbsolutePath();
    }

    /**
     * Crea un oggetto per ogni release del primo 34%, ordinato cronologicamente.
     * Risolve il commit dal tag e legge tutti i percorsi Java del suo albero.
     * La chiamata a Jira avviene prima del ciclo, una sola volta per esecuzione.
     */
    public List<ReleaseClasses> getReleaseClasses(String projectName) throws IOException {
        List<Release> releases = new ArrayList<>(releaseDAO.getReleases(projectName));
        releases.sort(Comparator.comparing(Release::getDate));
        for (int i = 0; i < releases.size(); i++) {
            releases.get(i).setIndex(i + 1);
        }
        int count = (int) ((long) releases.size() * 34 / 100);
        if (count == 0) {
            throw new IllegalArgumentException("Release insufficienti: il 34% per difetto e' zero.");
        }

        List<ReleaseClasses> result = new ArrayList<>();
        Set<String> tags = new LinkedHashSet<>(runGit("tag", "--list").lines().toList());
        for (Release release : releases.subList(0, count)) {
            ReleaseClasses releaseClasses = new ReleaseClasses();
            releaseClasses.setRelease(release);
            releaseClasses.setCommitHash(resolveCommit(release, tags));
            releaseClasses.setClassPaths(getClassPaths(releaseClasses.getCommitHash()));
            result.add(releaseClasses);
        }
        return result;
    }

    /** Risolve anche i tag annotati al commit, senza usare la data come fallback. */
    private String resolveCommit(Release release, Set<String> tags) throws IOException {
        String version = release.getVersionName();
        if (version == null || version.isBlank()) {
            throw new IOException("Nome versione assente per la release " + release.getVersionId());
        }
        Set<String> names = new LinkedHashSet<>();
        names.add(version);
        if (version.endsWith("-incubating")) {
            names.add(version.substring(0, version.length() - "-incubating".length()));
        }
        Set<String> commits = new LinkedHashSet<>();
        for (String name : names) {
            for (String prefix : List.of("", "v", "storm-", "apache-storm-", "release-")) {
                String tag = prefix + name;
                if (tags.contains(tag)) {
                    commits.add(runGit("rev-parse", "--verify", "--end-of-options",
                            "refs/tags/" + tag + "^{commit}").strip());
                }
            }
        }
        if (commits.size() != 1) {
            throw new IOException("Release " + version + " (Jira " + release.getVersionId()
                    + "): " + (commits.isEmpty() ? "nessun tag trovato" : "tag con commit differenti"));
        }
        return commits.iterator().next();
    }

    /** Percorsi relativi al repository, inclusi test e interfacce, senza checkout. */
    private List<String> getClassPaths(String commitHash) throws IOException {
        String tree = runGit("ls-tree", "-r", "-z", "--name-only", commitHash);
        return Arrays.stream(tree.split("\u0000"))
                .filter(path -> path.endsWith(".java"))
                .sorted()
                .toList();
    }

    private String runGit(String... arguments) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "-C", repositoryPath.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                throw new IOException("Comando Git fallito: " + output.strip());
            }
            return output;
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Recupero del commit interrotto", exception);
        } finally {
            process.getInputStream().close();
        }
    }
}
