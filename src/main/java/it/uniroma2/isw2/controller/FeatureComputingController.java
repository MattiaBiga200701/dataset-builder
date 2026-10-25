package it.uniroma2.isw2.controller;

import it.uniroma2.isw2.model.dao.FeatureComputingDAO;
import it.uniroma2.isw2.metrics.LocCalculator;
import it.uniroma2.isw2.metrics.LocTouchedCalculator;
import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import it.uniroma2.isw2.utils.GitSourceReader;

public class FeatureComputingController {
    private final FeatureComputingDAO featureComputingDAO;
    private final LocCalculator locCalculator;
    private final LocTouchedCalculator locTouchedCalculator;

    public FeatureComputingController() {
        this(new FeatureComputingDAO());
    }

    public FeatureComputingController(FeatureComputingDAO featureComputingDAO) {
        this.featureComputingDAO = Objects.requireNonNull(featureComputingDAO, "featureComputingDAO");
        this.locCalculator = new LocCalculator();
        this.locTouchedCalculator = new LocTouchedCalculator();
    }

    /** Arricchisce il CSV di base mantenendo ordine e dati preesistenti. */
    public Path createDatasetCsv(List<ReleaseDataset> datasets) throws IOException {
        Path baseCsv = Path.of("output", "dataset_base.csv");
        Path output = Path.of("output", "Dataset.csv");
        Objects.requireNonNull(datasets, "datasets");
        try (FeatureComputingDAO dao = featureComputingDAO) {
            dao.readBaseCsv(baseCsv, output);
            dao.prepareHistory(datasets.stream().map(ReleaseDataset::getCommitHash).toList());
            Map<String, Integer> locByBlob = new HashMap<>();
            String previousCommit = null;
            for (ReleaseDataset dataset : datasets) {
                Map<String, List<int[]>> changesByFile = dao.getChanges(previousCommit, dataset.getCommitHash());
                for (DatasetEntry entry : dataset.getEntries()) {
                    locComputing(dataset, entry, locByBlob);
                    locTouchedComputing(entry, changesByFile);
                    dao.updateEntry(entry);
                }
                previousCommit = dataset.getCommitHash();
            }
            dao.writeDatasetCsv();
        }
        return output.toAbsolutePath();
    }

    private void locComputing(ReleaseDataset dataset, DatasetEntry entry,
                              Map<String, Integer> locByBlob) throws IOException {
        GitSourceReader.Source source = featureComputingDAO.getSource(dataset.getCommitHash(), entry.getClassName());
        entry.setLOC(locByBlob.computeIfAbsent(source.blob(), blob -> locCalculator.calculate(source.text())));
    }

    private void locTouchedComputing(DatasetEntry entry, Map<String, List<int[]>> changesByFile) {
        entry.setLocTouched(locTouchedCalculator.calculate(
                changesByFile.getOrDefault(entry.getClassName(), List.of())));
    }
}
