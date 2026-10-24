package it.uniroma2.isw2.controller;

import it.uniroma2.isw2.model.dao.ReleaseDatasetDAO;
import it.uniroma2.isw2.model.dao.ReleaseDAO;
import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.utils.Feature;
import java.io.IOException;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Coordina recupero Jira, selezione delle release e recupero degli snapshot Git. */
public class ReleaseDatasetController {
    private final ReleaseDAO releaseDAO;
    private final ReleaseDatasetDAO releaseDatasetDAO;

    public ReleaseDatasetController() {
        this(new ReleaseDAO(), new ReleaseDatasetDAO());
    }

    public ReleaseDatasetController(ReleaseDAO releaseDAO,
                                    ReleaseDatasetDAO releaseDatasetDAO) {
        this.releaseDAO = Objects.requireNonNull(releaseDAO, "releaseDAO");
        this.releaseDatasetDAO = Objects.requireNonNull(releaseDatasetDAO, "releaseDatasetDAO");
    }

    /** Recupera le release da Jira una sola volta prima del ciclo su Git. */
    public List<ReleaseDataset> getReleaseDataset() throws IOException {
        return getReleaseDataset("STORM");
    }

    public List<ReleaseDataset> getReleaseDataset(String projectName) throws IOException {
        List<Release> releases = getSelectedReleases(projectName);
        DatasetEntry.setProjectName(projectName);
        List<ReleaseDataset> result = new ArrayList<>();
        for (Release release : releases) {
            result.add(releaseDatasetDAO.getReleaseDataset(release));
        }
        return result;
    }

    public Path createBaseCsv(List<ReleaseDataset> datasets) throws IOException {
        Path output = Path.of("output", "dataset_base.csv").toAbsolutePath();
        Files.createDirectories(output.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("Project Name,Class Name,Release ID");
            for (Feature feature : Feature.values()) {
                writer.write("," + feature.getColumnName());
            }
            writer.write("\r\n");
            for (ReleaseDataset dataset : datasets) {
                for (DatasetEntry entry : dataset.getEntries()) {
                    writer.write(csvValue(DatasetEntry.getProjectName()));
                    writer.write("," + csvValue(entry.getClassName()));
                    writer.write("," + entry.getRelease().getIndex());
                    writer.write(",".repeat(Feature.values().length));
                    writer.write("\r\n");
                }
            }
        }
        return output;
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Mantiene il primo 34%, arrotondato per difetto, con gli indici originali. */
    public List<Release> getSelectedReleases(String projectName) throws IOException {
        List<Release> releases = getReleases(projectName);
        int count = (int) ((long) releases.size() * 34 / 100);
        if (count == 0) {
            throw new IllegalArgumentException("Release insufficienti: il 34% per difetto e' zero.");
        }
        return List.copyOf(releases.subList(0, count));
    }

    /** Recupera le release da Jira, le ordina per data e assegna indici da 1. */
    public List<Release> getReleases(String projectName) throws IOException {
        List<Release> releases = new ArrayList<>(releaseDAO.getReleases(projectName));
        releases.sort(Comparator.comparing(Release::getDate));
        for (int i = 0; i < releases.size(); i++) {
            releases.get(i).setIndex(i + 1);
        }
        return releases;
    }
}
