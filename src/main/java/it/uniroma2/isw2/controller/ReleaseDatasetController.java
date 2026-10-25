package it.uniroma2.isw2.controller;

import it.uniroma2.isw2.model.dao.ReleaseDatasetDAO;
import it.uniroma2.isw2.model.dao.ReleaseDAO;
import it.uniroma2.isw2.model.dao.DatasetEntryDAO;
import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Coordina recupero Jira, selezione delle release e recupero degli snapshot Git. */
public class ReleaseDatasetController {
    private final DatasetEntryDAO datasetEntryDAO;
    private final ReleaseDAO releaseDAO;
    private final ReleaseDatasetDAO releaseDatasetDAO;

    public ReleaseDatasetController() {
        this(new ReleaseDAO(), new ReleaseDatasetDAO());
    }

    public ReleaseDatasetController(ReleaseDAO releaseDAO,
                                    ReleaseDatasetDAO releaseDatasetDAO) {
        this(releaseDAO, releaseDatasetDAO, new DatasetEntryDAO(releaseDatasetDAO.getRepositoryPath()));
    }

    public ReleaseDatasetController(ReleaseDAO releaseDAO, ReleaseDatasetDAO releaseDatasetDAO,
                                    DatasetEntryDAO datasetEntryDAO) {
        this.datasetEntryDAO = Objects.requireNonNull(datasetEntryDAO, "datasetEntryDAO");
        this.releaseDAO = Objects.requireNonNull(releaseDAO, "releaseDAO");
        this.releaseDatasetDAO = Objects.requireNonNull(releaseDatasetDAO, "releaseDatasetDAO");
    }



    public List<ReleaseDataset> getReleaseDataset() throws IOException {
        String projectName = "STORM";
        List<Release> releases = getSelectedReleases(projectName);
        datasetEntryDAO.setProjectName(projectName);
        List<ReleaseDataset> result = new ArrayList<>();
        for (Release release : releases) {
            String commitHash = releaseDatasetDAO.getCommitHash(release);
            result.add(releaseDatasetDAO.createReleaseDataset(release, commitHash,
                    datasetEntryDAO.getEntries(release, commitHash)));
        }
        return result;
    }

    public Path createBaseCsv(List<ReleaseDataset> datasets) throws IOException {
        return releaseDatasetDAO.createBaseCsv(datasets);
    }

    /** Mantiene il primo 34%, arrotondato per difetto, con gli indici originali. */
    public List<Release> getSelectedReleases(String projectName) throws IOException {
        List<Release> releases = releaseDAO.getReleases(projectName);
        int count = (int) ((long) releases.size() * 34 / 100);
        if (count == 0) {
            throw new IllegalArgumentException("Release insufficienti: il 34% per difetto e' zero.");
        }
        return List.copyOf(releases.subList(0, count));
    }

}
